package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.io.Serializable;
import java.util.HashSet;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.plang.types.NoValue;
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
	public PLangObject contains(PLangObject arg){
		return BooleanValue.fromBoolean(set.contains(arg));
	}
	
	/**
	 * Inserts into the original set, restricted access
	 * @param args
	 * @return
	 */
	public PLangObject insert(PLangObject arg){
		set.add(arg);
		return arg;
	}
	
	/**
	 * Removes from the original set, restricted access
	 * @param args
	 * @return
	 */
	public PLangObject remove(PLangObject arg){
		return BooleanValue.fromBoolean(set.remove(arg));
	}
	
	/**
	 * Returns copy of the original set with added element, unrestricted access
	 * @param args
	 * @return
	 */
	public PLangObject push(PLangObject a2, Pointer p){
		
		PLangHashSet inner = p.getPointer();
		
		inner.set.addAll(set);
		inner.set.add(a2);
		
		return p;
	}
	
	/**
	 * Clears inner set
	 * @param args
	 * @return
	 */
	public PLangObject clear(){
		set.clear();
		return NoValue.NOVALUE;
	}
	
	public PLangObject size(){
		return new Int(set.size());
	}
	
	@Override
	public String toString(){
		return "Set=" + set.toString();
	}
}
