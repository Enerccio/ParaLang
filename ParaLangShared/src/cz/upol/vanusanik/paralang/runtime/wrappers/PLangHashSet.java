package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.io.Serializable;
import java.util.HashSet;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.Pointer;

public class PLangHashSet implements Serializable{
	private static final long serialVersionUID = -7098990803194029764L;
	private HashSet<PLangObject> set = new HashSet<PLangObject>();
	
	public PLangHashSet(){
	}
	
	public PLangHashSet(PLangObject... args){
		
	}

	/**
	 * Returns TRUE/FALSE whether this set contains element
	 * @param args
	 * @return
	 */
	public PLangObject contains(PLangObject... args){
		if (args.length != 1) 
			throw new RuntimeException("Wrong number of parameters, expected 1, got " + args.length);
		
		return BooleanValue.fromBoolean(set.contains(args[0]));
	}
	
	/**
	 * Inserts into the original set, restricted access
	 * @param args
	 * @return
	 */
	public PLangObject insert(PLangObject... args){
		if (args.length != 1) 
			throw new RuntimeException("Wrong number of parameters, expected 1, got " + args.length);
		
		set.add(args[0]);
		return args[0];
	}
	
	/**
	 * Removes from the original set, restricted access
	 * @param args
	 * @return
	 */
	public PLangObject remove(PLangObject... args){
		if (args.length != 1) 
			throw new RuntimeException("Wrong number of parameters, expected 1, got " + args.length);
		
		return BooleanValue.fromBoolean(set.remove(args[0]));
	}
	
	/**
	 * Returns copy of the original set with added element, unrestricted access
	 * @param args
	 * @return
	 */
	public PLangObject push(PLangObject... args){
		if (args.length != 2) 
			throw new RuntimeException("Wrong number of parameters, expected 2, got " + args.length);
		
		Pointer p = (Pointer) args[1];
		PLangHashSet inner = p.getPointer();
		
		inner.set.addAll(set);
		inner.set.add(args[0]);
		
		return p;
	}
	
	@Override
	public String toString(){
		return set.toString();
	}
}
