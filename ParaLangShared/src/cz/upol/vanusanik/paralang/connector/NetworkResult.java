package cz.upol.vanusanik.paralang.connector;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.runtime.NetworkException;

public class NetworkResult {
	
	public boolean success;
	public NetworkException exception;
	public PLangObject[] results;

}
