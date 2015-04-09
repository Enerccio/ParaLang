package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.plang.types.NoValue;
import cz.upol.vanusanik.paralang.plang.types.Pointer;

public class PLangList extends ObjectBase implements Serializable {
	private static final long serialVersionUID = -2150162854532454209L;
	private List<PLangObject> innerList = new ArrayList<PLangObject>();

	public PLangList(){
	}
	
	public PLangList(PLangObject... args){
		
	}
	
	public PLangObject appendAt(PLangObject o, Int ix, Pointer p){
		PLangList newList = p.getPointer();
		newList.innerList.addAll(innerList);
		newList.innerList.add((int) ix.getValue(), o);
		return o;
	}
	
	public PLangObject append(PLangObject o, Pointer p){
		PLangList newList = p.getPointer();
		newList.innerList.addAll(innerList);
		return BooleanValue.fromBoolean(newList.innerList.add(o));
	}
	
	public PLangObject insert(PLangObject o, Int ix){
		innerList.add((int) ix.getValue(), o);
		return o;
	}
	
	public PLangObject add(PLangObject o){
		return BooleanValue.fromBoolean(innerList.add(o));
	}
	
	public PLangObject remove(PLangObject o){
		return BooleanValue.fromBoolean(innerList.remove(o));
	}
	
	public PLangObject removeAt(Int ix){		
		return innerList.remove((int) ix.getValue());
	}
	
	public PLangObject setAt(Int ix, PLangObject v){
		return innerList.set((int) ix.getValue(), v);
	}
	
	public PLangObject get(Int ix){
		return innerList.get((int) ix.getValue());
	}
	
	public PLangObject find(PLangObject o){
		return new Int(innerList.indexOf(o));
	}
	
	public PLangObject clear(){
		innerList.clear();
		return NoValue.NOVALUE;
	}
	
	public PLangObject size(){
		return new Int(innerList.size());
	}
	
	@Override
	public String doToString(){
		return innerList.toString();
	}

	public List<PLangObject> ___innerList() {
		return innerList;
	}
}
