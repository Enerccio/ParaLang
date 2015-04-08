package edu.upol.vanusanik.paralang.node;

/**
 * NodeCluster class contains all the nodes for this Node Controller. 
 * You can get free nodes via getFreeNode() method, which will also reserve such a node.
 * @author Enerccio
 *
 */
public class NodeCluster {

	private Node[] nodes;

	public NodeCluster(int wtc){
		nodes = new Node[wtc];
		for (int i=0; i<wtc; i++){
			nodes[i] = new Node(this, i);
			new Thread(nodes[i], "Worker Thread " + i + ";").start();
		}
	}
	
	/**
	 * Returns all nodes.
	 * @return all nodes
	 */
	public Node[] getNodes(){
		return nodes;
	}
	
	int rrindex = 0;

	/**
	 * Returns free node via round robin queue mechanism. 
	 * May return null if no free nodes exists. 
	 * After 4 * maxAmountOfNodes it will switch to linear queue algorithm for finding free node.
	 * @return free, reserved node or null
	 */
	public synchronized Node getFreeNode(){
		Node n;
		int it = 0;
		do {
			if (it > 4 * nodes.length)
				return getFreeNodeOrFail();
			n = nodes[rrindex];
			rrindex = (rrindex + 1) % nodes.length;
			++it;
		} while (n.isBusy());
		n.reserve();
        return n;
	}

	/**
	 * Linearly finds a first free node or fails and returns null
	 * @return free, reserved node or null
	 */
	private Node getFreeNodeOrFail() {
		for (Node n : nodes){
			if (!n.isBusy()){
				n.reserve();
				return n;
			}
		}
		return null;
	}
}
