package cz.upol.vanusanik.paralang.plang.types;

import cz.upol.vanusanik.paralang.plang.PLangObject;

public class TypeOperations {

	public static boolean convertToBoolean(PLangObject object){
		if (object == null) throw new NullPointerException();
		
		if (object == NoValue.NOVALUE || object == BooleanValue.FALSE)
			return false;
		
		return true;
	}
	
}
