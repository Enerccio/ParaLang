package edu.upol.vanusanik.node;

public class NodeCluster {

	private Node[] nodes;

	public NodeCluster(int wtc){
		nodes = new Node[wtc];
		for (int i=0; i<wtc; i++){
			nodes[i] = new Node(this, i);
			new Thread(nodes[i], "Worker Thread " + i + ";").start();
		}
	}
	
	public Node[] getNodes(){
		return nodes;
	}
	
	int rrindex = 0;
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
