package cz.upol.vanusanik.paralang.runtime;

import java.io.Serializable;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.NoValue;

public class Function  extends PLClass implements Serializable {
	private static final long serialVersionUID = -499503904346523234L;
	public static final String __applyMethod = "__apply";
	
	@Override
	protected void __init_internal_datafields() {
		this.__restrictedOverride = true;
		
		__setkey(BaseClass.__superKey, new BaseClass());
		__setkey(__applyMethod, new FunctionWrapper(__applyMethod, this, true));
		
		this.__restrictedOverride = false;
	}

	
	public PLangObject __apply(PLangObject self, PLangObject... args){
		return NoValue.NOVALUE;
	}
	
}
