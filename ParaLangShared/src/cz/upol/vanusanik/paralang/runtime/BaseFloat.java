package cz.upol.vanusanik.paralang.runtime;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.Flt;
import cz.upol.vanusanik.paralang.plang.types.NoValue;

public class BaseFloat extends BaseNumber {
	private static final long serialVersionUID = 9204688374246651301L;
	
	public BaseFloat(){
		
	}

	@Override
	public PLangObject __init_superclass(PLangObject self, PLangObject iv){
		if (!iv.__sys_m_isNumber()){
			throw new RuntimeException("Value " + iv + " is not a number!");
		}
		__setkey(__valKey, new Flt(iv.__sys_m_getNumber().floatValue()));
		return NoValue.NOVALUE;
	}
}
