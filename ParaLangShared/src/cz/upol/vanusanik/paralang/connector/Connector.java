package cz.upol.vanusanik.paralang.connector;

public interface Connector {

	public NetworkResult executeDistributed(long ___getObjectId,
			String methodName, int tcount);
	
}
