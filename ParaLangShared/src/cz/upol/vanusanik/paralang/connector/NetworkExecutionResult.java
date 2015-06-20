package cz.upol.vanusanik.paralang.connector;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.NoValue;

/**
 * Holds the result of network execution, either exceptions or results are non
 * null
 * 
 * @author Enerccio
 *
 */
public class NetworkExecutionResult {
	public PLangObject[] exceptions;
	public PLangObject[] results;

	/**
	 * Returns true if exception happened
	 * 
	 * @return
	 */
	public boolean hasExceptions() {
		if (exceptions == null)
			return false;
		for (PLangObject e : exceptions)
			if (e != NoValue.NOVALUE)
				return true;
		return false;
	}
}
