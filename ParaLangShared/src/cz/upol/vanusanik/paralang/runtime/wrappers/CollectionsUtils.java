package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.util.List;

import cz.upol.vanusanik.paralang.plang.PLangObject;

public class CollectionsUtils {

	public static PLangObject listRemoveAt(List<PLangObject> list, int idx){
		return list.remove(idx);
	}
	
}
