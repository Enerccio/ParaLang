package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.util.HashSet;
import java.util.Set;

/**
 * Base class for objects wrapped by plang system library.
 * 
 * @author Enerccio
 *
 */
public abstract class ObjectBase {

	/** Print chain, used to block recursive prints */
	private static ThreadLocal<Set<Object>> printChain = new ThreadLocal<Set<Object>>() {

		@Override
		protected Set<Object> initialValue() {
			return new HashSet<Object>();
		}

	};

	@Override
	public final String toString() {
		if (printChain.get().contains(this))
			return "...";
		printChain.get().add(this);
		String str = doToString();
		printChain.get().remove(this);
		return str;
	}

	/**
	 * Actually do toString with respect to chain printing
	 * 
	 * @return String representation of this object
	 */
	protected abstract String doToString();

}
