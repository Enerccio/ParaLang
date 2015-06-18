package cz.upol.vanusanik.paralang.node;

import java.io.FileInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.log4j.Logger;

import com.beust.jcommander.JCommander;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.compiler.StringDesignator;
import cz.upol.vanusanik.paralang.connector.NodeList;
import cz.upol.vanusanik.paralang.connector.Protocol;
import cz.upol.vanusanik.paralang.node.NodeCluster.___TimeoutException;
import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.plang.types.Str;
import cz.upol.vanusanik.paralang.runtime.PLClass;
import cz.upol.vanusanik.paralang.runtime.PLRuntime;
import cz.upol.vanusanik.paralang.runtime.PLRuntime.DeserializationResult;

/**
 * NodeController is main class of the ParaLang Node.
 * 
 * NodeController controls the runtime of ParaLang Node, providing services to
 * whomever connects to it. The communication is handled by extended json
 * protocol, which is 0x1e <4 bytes> <json message>. NodeController can be
 * modified via settings in NodeOptions.
 * 
 * @author Enerccio
 *
 */
public class NodeController {
	private static final Logger log = Logger.getLogger(NodeController.class);

	public static final void main(String[] args) throws Exception {
		NodeOptions no = new NodeOptions();
		new JCommander(no, args);

		NodeController nc = new NodeController();
		try {
			nc.start(no);
		} finally {
			nc.service.shutdown();
		}
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
	 * RuntimeStoreContainer is simple data class containing the sole reference
	 * point to particular client runtime this node was running previously. If
	 * this instance is gc'ed, runtime will disappear.
	 * 
	 * @author Enerccio
	 *
	 */
	public static class RuntimeStoreContainer {
		/** Source files before compilation are stored here */
		public List<StringDesignator> sources = new ArrayList<StringDesignator>();
		/** Actual runtime handle is stored here */
		public PLRuntime runtime;
	}

	/**
	 * Starts this node controller instance server, will start to accept the
	 * incoming requests.
	 * 
	 * @param no
	 * @throws Exception
	 */
	private void start(NodeOptions no) throws Exception {
		log.info("Starting ParaLang Node Controller at port " + no.portNumber
				+ ", number of working threads: " + no.threadCount);

		initialize(no);

		ServerSocket server = createServer(no);

		while (true) {
			final Socket s = server.accept();
			s.setKeepAlive(true);
			service.execute(new Runnable() {

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
						if (local != null && local.reservedNode != null)
							local.reservedNode.release();
					}
				}

			});
		}
	}

	private ServerSocket createServer(NodeOptions no) throws Exception {
		ServerSocket server;
		if (no.useSSL){
			SSLServerSocketFactory sslServerFactory = (SSLServerSocketFactory) SSLServerSocketFactory
					.getDefault();
			server = sslServerFactory
					.createServerSocket(no.portNumber);
			((SSLServerSocket)server).getNeedClientAuth();
			return server;
		} else 
			server = ServerSocketFactory.getDefault().createServerSocket(no.portNumber);
		return server;
	}

	/**
	 * Request Data storage class. Every client that has open request stream
	 * will have one assigned. Used to store intermediate data for client on
	 * this server.
	 * 
	 * @author Enerccio
	 *
	 */
	private class NodeLocalStorage {
		/** Node that this client has reserved, if any */
		public Node reservedNode;
		public RuntimeStoreContainer runtime;
		public PLangObject exception;
	}

	/** NodeLocalStorage is stored in this thread local */
	private ThreadLocal<NodeLocalStorage> localStorage = new ThreadLocal<NodeLocalStorage>();

	/**
	 * Resolves request from client. Will not close connection (only client can
	 * do that or timeout).
	 * 
	 * @param s
	 *            communication bound socket
	 * @return whether the communication was closed or not
	 * @throws Exception
	 *             if communication fails
	 */
	protected boolean resolveRequest(Socket s) throws Exception {
		s.setSoTimeout(1000 * 120);
		JsonObject m = Protocol.receive(s.getInputStream());

		if (m == null)
			return true;

		log.info("Request " + m.get("header") + " from " + s.getLocalAddress());

		if (m.getString("header", "").equals(Protocol.GET_STATUS_REQUEST))
			resolveStatusRequest(s, m);
		if (m.getString("header", "").equals(Protocol.RESERVE_SPOT_REQUEST))
			resolveReserveSpotRequest(s, m);
		if (m.getString("header", "").equals(Protocol.RUN_CODE))
			resolveRunCode(s, m);
		return false;
	}

	private void resolveRunCode(Socket s, JsonObject m) throws Exception {
		JsonObject payload = new JsonObject();
		payload.add("header", Protocol.RETURNED_EXECUTION);

		final NodeLocalStorage storage = localStorage.get();
		if (storage.reservedNode == null) {
			sendError(s, payload, Protocol.ERROR_NO_RESERVED_NODE,
					"No reserved node for this client");
			return;
		}

		final JsonObject input = m.get("payload").asObject();

		storage.runtime = new RuntimeStoreContainer();

		/* Initialize the runtime */
		storage.runtime.runtime = PLRuntime.createEmptyRuntime();
		storage.runtime.runtime.setAsCurrent();
		JsonArray sources = input.get("runtimeFiles").asArray();
		for (JsonValue v : sources) {
			JsonObject data = v.asObject();
			if (data.getString("type", "").equals("module")) {
				storage.runtime.runtime.loadBytecode(
						data.getString("name", ""),
						data.getString("content", ""));
			} else {
				storage.runtime.runtime.loadBytecode(
						data.getString("name", ""),
						data.getString("module", ""),
						data.getString("content", ""));
			}
		}
		storage.runtime.runtime.prepareForDeserialization(input
				.get("runtimeData").asObject().get("serialVersionUIDs")
				.asArray());
		Map<Long, Long> ridxMap;
		PLangObject arg;
		/* Deserializes the json content back to the objects */
		try {
			DeserializationResult r = storage.runtime.runtime.deserialize(input
					.get("runtimeData").asObject().get("modules").asArray(),
					input.get("runtimeData").asObject().get("currentCaller")
							.asObject(), input.get("runtimeData").asObject()
							.get("callerArg").asObject());
			ridxMap = r.ridxMap;
			arg = r.cobject;
		} catch (Exception e) {
			sendError(s, payload, Protocol.ERROR_DESERIALIZATION_FAILURE,
					e.getMessage());
			storage.runtime = null;
			return;
		}

		final Map<Long, Long> transMap = ridxMap;
		final PLangObject farg = arg;
		storage.runtime.runtime.setRestricted(true);
		storage.exception = null;

		/* Runtime is prepared to resume from last call */
		RunnablePayload<PLangObject> run = new RunnablePayload<PLangObject>() {

			@Override
			protected PLangObject executePayload() {

				try {
					long argId = input.getLong("argId", 0);
					// binds to current thread
					storage.runtime.runtime.setAsCurrent();
					// runs the current object and method asked
					return storage.runtime.runtime.runByObjectId(transMap
							.get(input.getLong("runnerId", 0)), input
							.getString("methodName", ""),
							new Int(input.getInt("id", 0)), farg,
							argId > 0 ? transMap.get(argId) : argId);
				} catch (PLClass e) {
					storage.exception = e;
					return null;
				} catch (___TimeoutException te){
					// cancel the thread due to time limit and inform about it
					log.info("Worker thread failed due to timeout");
					storage.exception = PLRuntime.getRuntime().newInstance("System.BaseException", 
							new Str("Timeout, maximum runtime allowed: " + te.getTimeoutValue()));
					finished = true;
					throw te;
				} catch (Throwable t) {
					// cancel unlimited running because throwing will go behind
					// the usual end of run mechanics
					finished = true;
					throw t;
				}
			}

		};

		// Runs the code
		storage.reservedNode.setNewPayload(run);

		// Wait for the payload to finish with active waiting
		while (!run.hasFinished()) {
			Thread.sleep(10);
		}

		PLangObject result = run.getResult();

		// serialize the return value back to the caller
		JsonObject p = new JsonObject();
		if (result != null) {
			p.add("hasResult", true);
			p.add("result", result.___toObject());
		} else {
			p.add("hasResult", false);
			p.add("exception", storage.exception.___toObject());
		}

		payload.add("payload", p);

		localStorage.set(null);

		// send the payload back to the requesting client
		Protocol.send(s.getOutputStream(), payload);
	}

	/**
	 * Sends the error back to the client.
	 * 
	 * This is not used to send back exception, instead it is used to send when
	 * error happened prior to the running of the runtime.
	 * 
	 * @param s
	 * @param payload
	 * @param ecode
	 * @param dmesg
	 * @throws Exception
	 */
	private void sendError(Socket s, JsonObject payload, long ecode,
			String dmesg) throws Exception {
		payload.add("payload",
				new JsonObject().add("error", true).add("errorCode", ecode)
						.add("errorDetails", dmesg));
		Protocol.send(s.getOutputStream(), payload);
	}

	/**
	 * Resolve reserve spot request. Will reserve a node for this client or
	 * fails, and reports back the success or failure via
	 * Protocol.RESERVE_SPOT_RESPONSE message.
	 * 
	 * @param s
	 *            communication bound socket
	 * @param m
	 *            original message
	 * @throws Exception
	 *             on any failure
	 */
	private void resolveReserveSpotRequest(Socket s, JsonObject m)
			throws Exception {
		JsonObject payload = new JsonObject();
		payload.add("header", Protocol.RESERVE_SPOT_RESPONSE);

		NodeLocalStorage storage = localStorage.get();

		storage.reservedNode = cluster.getFreeNode();
		if (storage.reservedNode == null) {
			payload.add("payload", new JsonObject().add("result", false));
		} else {
			payload.add("payload", new JsonObject().add("result", true));
		}
		Protocol.send(s.getOutputStream(), payload);
	}

	/**
	 * Sends information about this node to the client via
	 * Protocol.GET_STATUS_RESPONSE message.
	 * 
	 * @param s
	 *            communication bound socket
	 * @param m
	 *            original message
	 * @throws Exception
	 *             on any failure
	 */
	private void resolveStatusRequest(Socket s, JsonObject m) throws Exception {
		JsonObject payload = new JsonObject().add("header",
				Protocol.GET_STATUS_RESPONSE).add(
				"payload",
				new JsonObject().add("workerThreads", options.threadCount).add(
						"workerThreadStatus", generateWorkerThreadUsage()));
		Protocol.send(s.getOutputStream(), payload);
	}

	/** Helper method to compute worker thread usage message */
	private JsonObject generateWorkerThreadUsage() {
		JsonObject o = new JsonObject();

		for (Node n : cluster.getNodes()) {
			synchronized (cluster){
				o.add("node" + n.getId(), n.isBusy());
			}
		}

		return o;
	}

	private NodeOptions options;

	/**
	 * Initializes this NodeController via NodeOptions. Also creates the
	 * handling thread pools and RuntimeMemoryCleaningThread cleaning thread
	 * that is immediately started.
	 * 
	 * @param no
	 *            initialization values.
	 * @throws Exception
	 */
	private void initialize(NodeOptions no) throws Exception {
		service = Executors.newCachedThreadPool();
		options = no;
		cluster = new NodeCluster(no.threadCount, no.timeout);

		if (no.nodeListFile != null) {
			FileInputStream fis = new FileInputStream(no.nodeListFile);
			NodeList.loadFile(fis);
			fis.close();
		}

		String[] parsedNodes = no.nodes.split(";");
		for (String s : parsedNodes) {
			if (!s.equals("")) {
				String[] datum = s.split(":");
				NodeList.addNode(datum[0], Integer.parseInt(datum[1]));
			}
		}

		if (no.useSSL){
			// Set up the keystores for ssl/tsl communication
			System.setProperty("javax.net.ssl.keyStore", no.keystore);
			System.setProperty("javax.net.ssl.keyStorePassword", no.keystorepass);
		}
		
		NodeList.setUseSSL(no.useSSL);
		
		NodeList.addNode("localhost", no.portNumber);
	}

}
