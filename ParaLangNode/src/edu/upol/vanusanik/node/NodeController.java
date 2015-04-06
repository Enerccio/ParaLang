package edu.upol.vanusanik.node;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.beust.jcommander.JCommander;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;

import cz.upol.vanusanik.paralang.connector.Protocol;

public class NodeController {
	private static final Logger log = Logger.getLogger(NodeController.class);

	public static final void main(String[] args) throws Exception{
		NodeOptions no = new NodeOptions();
		new JCommander(no, args);
		
		new NodeController().start(no);
	}
	
	private ExecutorService service;

	private void start(NodeOptions no) throws Exception {
		log.info("Starting ParaLang Node Controller at port " + no.portNumber + ", number of working threads: " + no.threadCount);
		
		initialize(no);
		@SuppressWarnings("resource")
		ServerSocket server = new ServerSocket(no.portNumber);
		
		while (true){
			final Socket s = server.accept();
			service.execute(new Runnable(){

				@Override
				public void run() {
					try {
						while (!s.isClosed())
							resolveRequest(s);
					} catch (Exception e) {
						log.error(e);
						log.debug(e, e);
					}
				}
				
			});
		}
	}

	protected void resolveRequest(Socket s) throws Exception {
		JsonObject m = Protocol.load(s.getInputStream());
		log.info("Request " + m.get("header") + " from " + s.getLocalAddress());
		
		if (m.getString("header", "").equals(Protocol.GET_STATUS_REQUEST))
			resolveStatusRequest(s, m);
	}

	private void resolveStatusRequest(Socket s, JsonObject m) throws Exception {
		JsonObject payload = new JsonObject()
			.add("header", Protocol.GET_STATUS_RESPONSE)
			.add("payload", new JsonObject()
				.add("workerThreads", options.threadCount));
		IOUtils.write(payload.toString(WriterConfig.PRETTY_PRINT), s.getOutputStream());
	}
	
	private NodeOptions options;
	private void initialize(NodeOptions no) throws Exception {
		service = Executors.newCachedThreadPool();
		options = no;
	}
	
}
