package edu.upol.vanusanik.node;

import org.apache.log4j.Logger;

public class Node implements Runnable {
	private static final Logger log = Logger.getLogger(Node.class);
	
	private int id;
	private boolean reserved = false;
	
	public Node(NodeCluster nodeCluster, int id) {
		this.setId(id);
	}

	public synchronized boolean isBusy() {
		return payload != null || reserved;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	private volatile Runnable payload = null;

	@Override
	public void run(){
		while (true){
			try {
				while (payload == null)
					Thread.sleep(10);
				payload.run();
				reserved = false;
				payload = null;
			} catch (Exception e){
				log.error(e.getMessage());
			}
		}
	}
	
	public synchronized void setNewPayload(Runnable payload){
		this.payload = payload;
	}

	public void reserve() {
		reserved = true;
	}

	/**
	 * Releases this node, if it was previously reserved. Does not unblocks the thread or node if it is still computing!
	 */
	public void release() {
		reserved = false;
	}
}
