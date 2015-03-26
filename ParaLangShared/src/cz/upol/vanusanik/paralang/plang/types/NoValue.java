package cz.upol.vanusanik.paralang.plang.types;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;

public class NoValue extends PLangObject {
	
	private NoValue(){
		
	}
	
	public static final NoValue NOVALUE = new NoValue();

	@Override
	public PlangObjectType getType() {
		return PlangObjectType.NOVALUE;
	}

	@Override
	public String toString(){
		return "NoValue";
	}
}
