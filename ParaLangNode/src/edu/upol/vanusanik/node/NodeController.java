package edu.upol.vanusanik.node;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.beust.jcommander.JCommander;
import com.eclipsesource.json.JsonObject;

import cz.upol.vanusanik.paralang.compiler.StringDesignator;
import cz.upol.vanusanik.paralang.connector.Protocol;
import cz.upol.vanusanik.paralang.runtime.PLRuntime;
import cz.upol.vanusanik.paralang.runtime.PLRuntime.RuntimeAccessListener;

public class NodeController {
	private static final Logger log = Logger.getLogger(NodeController.class);

	public static final void main(String[] args) throws Exception{
		NodeOptions no = new NodeOptions();
		new JCommander(no, args);
		
		new NodeController().start(no);
	}
	
	private ExecutorService service;
	private NodeCluster cluster;

	public class RuntimeStoreContainer implements RuntimeAccessListener {
		public volatile long lastAccessTime;
		public volatile long lastModificationTime;
		
		public List<StringDesignator> sources = new ArrayList<StringDesignator>();
		public PLRuntime runtime;
		
		@Override
		public void wasAccessed() {
			lastAccessTime = System.currentTimeMillis();
		}
	}
	
	private HashMap<String, WeakReference<RuntimeStoreContainer>> cache = new HashMap<String, WeakReference<RuntimeStoreContainer>>();
	private Set<RuntimeStoreContainer> containerSet = new HashSet<RuntimeStoreContainer>();

	private void start(NodeOptions no) throws Exception {
		log.info("Starting ParaLang Node Controller at port " + no.portNumber + ", number of working threads: " + no.threadCount);
		
		initialize(no);
		@SuppressWarnings("resource")
		ServerSocket server = new ServerSocket(no.portNumber);
		
		while (true){
			final Socket s = server.accept();
			s.setKeepAlive(true);
			service.execute(new Runnable(){

				@Override
				public void run() {
					try {
						localStorage.set(new NodeLocalStorage());
						while (!s.isClosed())
							if (resolveRequest(s)) 
								break;
					} catch (Exception e) {
						log.error(e);
						log.debug(e, e);
					} finally {
						NodeLocalStorage local = localStorage.get();
						localStorage.set(null);
						if (local.reservedNode != null)
							local.reservedNode.release();
					}
				}
				
			});
		}
	}
	
	private class NodeLocalStorage {
		public Node reservedNode;
	}
	
	private ThreadLocal<NodeLocalStorage> localStorage = new ThreadLocal<NodeLocalStorage>();

	protected boolean resolveRequest(Socket s) throws Exception {
		s.setSoTimeout(1000 * 120);
		JsonObject m = Protocol.receive(s.getInputStream());
		
		if (m == null) return true;
		
		log.info("Request " + m.get("header") + " from " + s.getLocalAddress());
		
		if (m.getString("header", "").equals(Protocol.GET_STATUS_REQUEST))
			resolveStatusRequest(s, m);
		if (m.getString("header", "").equals(Protocol.RESERVE_SPOT_REQUEST))
			resolveReserveSpotRequest(s, m);
		return false;
	}

	private void resolveReserveSpotRequest(Socket s, JsonObject m) throws Exception {
		JsonObject payload = new JsonObject();
		payload.add("header", Protocol.RESERVE_SPOT_RESPONSE);
		
		NodeLocalStorage storage = localStorage.get();
		
		storage.reservedNode = cluster.getFreeNode();
		if (storage.reservedNode == null){
			payload.add("payload", new JsonObject().add("result", false));
		} else {
			payload.add("payload", new JsonObject().add("result", true));	
		}
		Protocol.send(s.getOutputStream(), payload);
	}

	private void resolveStatusRequest(Socket s, JsonObject m) throws Exception {
		JsonObject payload = new JsonObject()
			.add("header", Protocol.GET_STATUS_RESPONSE)
			.add("payload", new JsonObject()
				.add("workerThreads", options.threadCount)
				.add("workerThreadStatus", generateWorkerThreadUsage()));
		Protocol.send(s.getOutputStream(), payload);
	}
	
	private JsonObject generateWorkerThreadUsage() {
		JsonObject o = new JsonObject();
		
		for (Node n : cluster.getNodes()){
			o.add("node" + n.getId(), n.isBusy());
		}
		
		return o;
	}

	private NodeOptions options;
	private void initialize(NodeOptions no) throws Exception {
		service = Executors.newCachedThreadPool();
		options = no;
		cluster = new NodeCluster(no.threadCount);
		
		new RuntimeMemoryCleaningThread(this, containerSet, no.cacheStoreTime);
	}
	
}
