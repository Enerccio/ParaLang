package cz.upol.vanusanik.paralang.runtime;

import java.io.Serializable;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.NoValue;

public class BaseClass extends PLClass implements Serializable {
	private static final long serialVersionUID = -499503904346523232L;

	@Override
	protected void __init_internal_datafields() {
		this.__restrictedOverride = true;
		
		__setkey("init", new FunctionWrapper("init", this, true));
		
		this.__restrictedOverride = false;
	}

	public PLangObject init(PLangObject self){
		return NoValue.NOVALUE;
	}
}
