package cz.upol.vanusanik.paralang.connector;

/**
 * Node class holds address and port.
 * 
 * @author Enerccio
 *
 */
public class Node {

	private final String address;
	private final int port;

	public Node(String address, int port) {
		this.address = address;
		this.port = port;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + port;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (port != other.port)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Node [address=" + address + ", port=" + port + "]";
	}

	public String getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}

	private volatile long lastCheckTime;
	private volatile int lastActiveNodes;
	
	private static final long CACHE_CHECK_TIME = 1350000000L; // 1350ms
	public synchronized int checkLastModified(long lm){
		if (lm - lastCheckTime > CACHE_CHECK_TIME)
			return -1;
		return lastActiveNodes;
	}
	
	public synchronized void setLastModified(long lm, int wts){
		lastCheckTime = lm;
		lastActiveNodes = wts;
	}
}
