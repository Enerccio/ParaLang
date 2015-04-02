package cz.upol.vanusanik.paralang.runtime;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.plang.types.NoValue;

public class BaseInteger extends BaseNumber {
	private static final long serialVersionUID = 2544264163945283955L;
	
	public BaseInteger(){
		
	}
	
	@Override
	public PLangObject __init_superclass(PLangObject self, PLangObject iv){
		if (!iv.__sys_m_isNumber()){
			throw new RuntimeException("Value " + iv + " is not a number!");
		}
		__setkey(__valKey, new Int(iv.__sys_m_getNumber(iv).intValue()));
		return NoValue.NOVALUE;
	}

}
