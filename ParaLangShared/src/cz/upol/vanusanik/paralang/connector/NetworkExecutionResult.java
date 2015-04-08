package cz.upol.vanusanik.paralang.connector;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.runtime.PLException;

public class NetworkExecutionResult {
	public PLException exception;
	public boolean success;
	public PLangObject[] results;
}
