package edu.upol.vanusanik.node;

import org.apache.log4j.Logger;

/**
 * Node is running thread that handles the execution of ParaLang code.
 * @author Enerccio
 *
 */
public class Node implements Runnable {
	private static final Logger log = Logger.getLogger(Node.class);
	
	/** Id of this Node */
	private int id;
	/** Whether this node is reserved for some client or not */
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
	
	/** Payload that will be run asap */
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
	
	/** Sets the new payload. This method does not check whether there is payload already running and 
	 * setting new payload while payload is running results in indefinite behavior.
	 * @param payload to be run
	 */
	public synchronized void setNewPayload(Runnable payload){
		this.payload = payload;
	}

	/**
	 * Reserves this node
	 */
	public synchronized void reserve() {
		reserved = true;
	}

	/**
	 * Releases this node, if it was previously reserved. Does not unblocks the thread or node if it is still computing!
	 */
	public void release() {
		reserved = false;
	}
}
