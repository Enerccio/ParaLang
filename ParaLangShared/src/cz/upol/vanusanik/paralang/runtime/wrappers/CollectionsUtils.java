package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.util.List;

import cz.upol.vanusanik.paralang.plang.PLangObject;

/**
 * Utility class used in plang system library
 * 
 * @author Enerccio
 *
 */
public class CollectionsUtils {

	/**
	 * Removes object at that index
	 * 
	 * @param list
	 * @param idx
	 * @return
	 */
	public static PLangObject listRemoveAt(List<PLangObject> list, int idx) {
		return list.remove(idx);
	}

}
