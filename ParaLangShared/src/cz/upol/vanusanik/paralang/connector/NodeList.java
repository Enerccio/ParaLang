package cz.upol.vanusanik.paralang.connector;

import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.io.IOUtils;

import com.eclipsesource.json.JsonObject;

public class NodeList {

	private NodeList() {

	}

	private static Set<Node> nodes = new HashSet<Node>();
	private static List<Node> nList = new ArrayList<Node>();

	public static void loadFile(InputStream file) throws Exception {
		List<String> lines = IOUtils.readLines(file);
		for (String line : lines) {
			String[] ldata = line.split(":");
			Node n = new Node(ldata[0], Integer.parseInt(ldata[1]));
			nodes.add(n);
			nList.add(n);
		}
	}

	public static void addNode(String address, int port) {
		Node n = new Node(address, port);
		nodes.add(n);
		nList.add(n);
	}

	private static class NodeComparator implements Comparable<NodeComparator> {
		public Node n;
		public int val;

		@Override
		public int compareTo(NodeComparator arg0) {
			return Integer.compare(val, arg0.val);
		}

	}

	public static synchronized List<Node> getBestLoadNodes(int reqNodeNum) {
		List<NodeComparator> ncl = new ArrayList<NodeComparator>();
		for (Node n : nodes) {
			NodeComparator nc = new NodeComparator();
			nc.n = n;
			try {
				nc.val = getFreeNodes(n);
			} catch (Exception e) {
				continue;
			}
			ncl.add(nc);
		}

		List<Node> nodeList = new ArrayList<Node>(reqNodeNum);

		try {
			for (int i = 0; i < reqNodeNum; i++) {
				Collections.sort(ncl);

				nodeList.add(ncl.get(ncl.size() - 1).n);
				--ncl.get(ncl.size() - 1).val;
			}
		} catch (Exception e) {

		}
		return nodeList;
	}

	private static final Random r = new Random();

	public static synchronized Node getRandomNode() {
		boolean hasFreeNodes = false;
		Node n;
		do {
			try {
				Thread.sleep(0, 10);
			} catch (InterruptedException e1) {

			}
			n = nList.get(r.nextInt(nList.size()));
			try {
				hasFreeNodes = getFreeNodes(n) > 0;
			} catch (Exception e) {
				continue;
			}
		} while (hasFreeNodes == false);
		return n;
	}

	private static int getFreeNodes(Node n) throws Exception {
		Socket s = SSLSocketFactory.getDefault().createSocket(n.getAddress(),
				n.getPort());

		JsonObject o = new JsonObject();
		o.add("header", Protocol.GET_STATUS_REQUEST);
		Protocol.send(s.getOutputStream(), o);

		o = Protocol.receive(s.getInputStream());
		s.close();
		if (!o.getString("header", "").equals(Protocol.GET_STATUS_RESPONSE))
			throw new Exception("Wrong reply from the server");

		int wtc = o.get("payload").asObject().get("workerThreads").asInt();
		int c = 0;

		for (int i = 0; i < wtc; i++) {
			if (!o.get("payload").asObject().get("workerThreadStatus")
					.asObject().getBoolean("node" + i, false))
				++c;
		}

		return c;
	}

	public static synchronized int expectNumberOfNodes() {
		int sum = 0;
		for (Node n : nList)
			try {
				sum += getFreeNodes(n);
			} catch (Exception e) {

			}
		return sum;
	}
}
