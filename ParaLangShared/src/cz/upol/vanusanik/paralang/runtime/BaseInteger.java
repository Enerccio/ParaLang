package cz.upol.vanusanik.paralang.runtime;

import java.io.Serializable;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.plang.types.NoValue;
import cz.upol.vanusanik.paralang.plang.types.TypeOperations;
import cz.upol.vanusanik.paralang.plang.types.TypeOperations.Operator;

public class BaseInteger extends PLClass implements Serializable {
	private static final long serialVersionUID = -499503904346523234L;
	private static final String __valKey = "__val";
	
	@Override
	protected void __init_internal_datafields() {
		this.__restrictedOverride = true;
		
		__setkey(BaseClass.__superKey, new BaseClass());
		__setkey("init", new FunctionWrapper("__init", this, true));
		
		__setkey(Operator.EQ.classMethod, new FunctionWrapper("__eq", this, true));
		
		__setkey(__valKey, new Int(0));
		this.__restrictedOverride = false;
	}

	public PLangObject __init(PLangObject self, PLangObject iv){
		if (!iv.isNumber()){
			throw new RuntimeException("Value " + iv + " is not a number!");
		}
		__setkey(__valKey, new Int(iv.getNumber().intValue()));
		return NoValue.NOVALUE;
	}
	
	public PLangObject __eq(PLangObject self, PLangObject me, PLangObject other){
		return TypeOperations.eq(__getkey(__valKey), other);
	}
	
	public boolean less(PLangObject other, boolean equals) {
		if (equals){
			return BooleanValue.toBoolean(TypeOperations.leq(__getkey(__valKey), other));
		} else {
			return BooleanValue.toBoolean(TypeOperations.less(__getkey(__valKey), other));
		}
	}
	
	public boolean more(PLangObject other, boolean equals) {
		if (equals){
			return BooleanValue.toBoolean(TypeOperations.meq(__getkey(__valKey), other));
		} else {
			return BooleanValue.toBoolean(TypeOperations.more(__getkey(__valKey), other));
		}
	}
	
	@Override
	public boolean isNumber(){
		return true;
	}
	
	@Override
	public Float getNumber(){
		return __getkey(__valKey).getNumber();
	}
}
