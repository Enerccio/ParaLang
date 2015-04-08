package cz.upol.vanusanik.paralang.connector;

import cz.upol.vanusanik.paralang.plang.PLangObject;

public class NetworkExecutionResult {
	public PLangObject[] exceptions;
	public PLangObject[] results;
	
	public boolean hasExceptions() {
		for (PLangObject e : exceptions)
			if (e != null)
				return true;
		return false;
	}
}
