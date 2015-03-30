package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.util.HashSet;
import java.util.Set;

public abstract class ObjectBase {
	
	private static ThreadLocal<Set<Object>> printChain = new ThreadLocal<Set<Object>>(){

		@Override
		protected Set<Object> initialValue() {
			return new HashSet<Object>();
		}
		
	};
	
	@Override
	public final String toString(){
		if (printChain.get().contains(this))
			return "...";
		printChain.get().add(this);
		String str = doToString();
		printChain.get().remove(this);
		return str;
	}

	protected abstract String doToString();

}
