package edu.upol.vanusanik.paralang.node;

import java.lang.ref.SoftReference;
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

/**
 * NodeController is main class of the ParaLang Node. 
 * 
 * NodeController controls the runtime of ParaLang Node, providing services to whomever connects to it.
 * The communication is handled by extended json protocol, which is 0x1e <4 bytes> <json message>. 
 * NodeController can be modified via settings in NodeOptions. 
 * @author Enerccio
 *
 */
public class NodeController {
	private static final Logger log = Logger.getLogger(NodeController.class);

	public static final void main(String[] args) throws Exception{
		NodeOptions no = new NodeOptions();
		new JCommander(no, args);
		
		new NodeController().start(no);
	}
	
	/**
	 * service is thread pool that handles incoming requests.
	 */
	private ExecutorService service;
	/**
	 * cluster contains para lang nodes. You can retrieve free nodes from it.
	 */
	private NodeCluster cluster;

	/**
	 * RuntimeStoreContainer is simple data class containing the sole reference point to 
	 * particular client runtime this node was running previously. If this instance is gc'ed, 
	 * runtime will disappear.
	 * @author Enerccio
	 *
	 */
	public class RuntimeStoreContainer implements RuntimeAccessListener {
		/** Last access time. Used by RuntimeMemoryCleaningThread to determine whether the cache should be purged or not */
		public volatile long lastAccessTime;
		/** This runtime's last modification time, as per client's specifications. Used to send over when requesting delta of the runtime changes.*/
		public volatile long lastModificationTime;
		
		/** Source files before compilation are stored here */
		public List<StringDesignator> sources = new ArrayList<StringDesignator>();
		/** Actual runtime handle is stored here */
		public PLRuntime runtime;
		
		private boolean invalidated = false;
		@Override
		public synchronized void wasAccessed() {
			if (invalidated) return;
			lastAccessTime = System.currentTimeMillis();
		}
		
		/**
		 * Sets the lastAccessTime to 0 which in turn will make this available for reclamation in memory cleaning thread
		 */
		public synchronized void invalidate(){
			invalidated = true;
			lastAccessTime = 0;
		}
	}
	
	
	/**
	 * Contains soft references to RuntimeStoreContainer for particular uuid hash. Might disappear when gc'ed.
	 */
	private HashMap<String, SoftReference<RuntimeStoreContainer>> cache = new HashMap<String, SoftReference<RuntimeStoreContainer>>();

	/**
	 * Starts this node controller instance server, will start to accept the incoming requests.
	 * @param no
	 * @throws Exception
	 */
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
	
	/**
	 * Request Data storage class. Every client that has open request stream will have one assigned. 
	 * Used to store intermediate data for client on this server.
	 * @author Enerccio
	 *
	 */
	private class NodeLocalStorage {
		/** Node that this client has reserved, if any */
		public Node reservedNode;
	}
	
	/** NodeLocalStorage is stored in this thread local */
	private ThreadLocal<NodeLocalStorage> localStorage = new ThreadLocal<NodeLocalStorage>();

	/**
	 * Resolves request from client. Will not close connection (only client can do that or timeout).
	 * @param s communication bound socket
	 * @return whether the communication was closed or not
	 * @throws Exception if communication fails
	 */
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

	/**
	 * Resolve reserve spot request. Will reserve a node for this client or fails, and reports back the success or failure
	 * via Protocol.RESERVE_SPOT_RESPONSE message.
	 * @param s communication bound socket
	 * @param m original message
	 * @throws Exception on any failure
	 */
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

	/**
	 * Sends information about this node to the client
	 * via Protocol.GET_STATUS_RESPONSE message.
	 * @param s communication bound socket
	 * @param m original message
	 * @throws Exception on any failure
	 */
	private void resolveStatusRequest(Socket s, JsonObject m) throws Exception {
		JsonObject payload = new JsonObject()
			.add("header", Protocol.GET_STATUS_RESPONSE)
			.add("payload", new JsonObject()
				.add("workerThreads", options.threadCount)
				.add("workerThreadStatus", generateWorkerThreadUsage()));
		Protocol.send(s.getOutputStream(), payload);
	}
	
	/** Helper method to compute worker thread usage message */
	private JsonObject generateWorkerThreadUsage() {
		JsonObject o = new JsonObject();
		
		for (Node n : cluster.getNodes()){
			o.add("node" + n.getId(), n.isBusy());
		}
		
		return o;
	}

	private NodeOptions options;
	
	/**
	 * Initializes this NodeController via NodeOptions. Also creates the handling thread pools and 
	 * RuntimeMemoryCleaningThread cleaning thread that is immediatelly started.
	 * @param no initialization values.
	 * @throws Exception
	 */
	private void initialize(NodeOptions no) throws Exception {
		service = Executors.newCachedThreadPool();
		options = no;
		cluster = new NodeCluster(no.threadCount);
	}
	
}
