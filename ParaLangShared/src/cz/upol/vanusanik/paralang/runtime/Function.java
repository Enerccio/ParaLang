package cz.upol.vanusanik.paralang.runtime;

import java.io.Serializable;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.NoValue;

public class Function  extends PLClass implements Serializable {
	private static final long serialVersionUID = -499503904346523210L;
	public static final String __applyMethod = "__apply";
	
	public Function(){
		
	}
	
	@Override
	protected void __init_internal_datafields() {
		this.__restrictedOverride = true;
		
		__setkey(BaseClass.__superKey, new BaseClass());
		__setkey(__applyMethod, new FunctionWrapper("__apply__base", this, true));
		
		this.__restrictedOverride = false;
	}

	
	public PLangObject __apply__base(PLangObject self, PLangObject... args){
		return NoValue.NOVALUE;
	}
	
}
