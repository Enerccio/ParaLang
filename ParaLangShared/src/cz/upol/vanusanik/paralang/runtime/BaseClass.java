package cz.upol.vanusanik.paralang.runtime;

import java.io.Serializable;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.NoValue;
import cz.upol.vanusanik.paralang.plang.types.Str;
import cz.upol.vanusanik.paralang.plang.types.TypeOperations.Operator;

public class BaseClass extends PLClass implements Serializable {
	private static final long serialVersionUID = -499503904346523232L;
	public static final String __hasKey = "__has";
	
	public BaseClass(){
		
	}

	@Override
	protected void __init_internal_datafields() {
		this.__restrictedOverride = true;
		
		__setkey("init", new FunctionWrapper("__init", this, true));
		__setkey("__str", new FunctionWrapper("__str_base", this, true));
		__setkey(Operator.EQ.classMethod, new FunctionWrapper("__eq__base", this, true));
		__setkey(Operator.NEQ.classMethod, new FunctionWrapper("__neq__base", this, true));
		
		this.__restrictedOverride = false;
	}

	public PLangObject __init(PLangObject self){
		return NoValue.NOVALUE;
	}
	
	public PLangObject __eq__base(PLangObject self, PLangObject other){
		return BooleanValue.fromBoolean(self == other);
	}
	
	public PLangObject __neq__base(PLangObject self, PLangObject other){
		return BooleanValue.fromBoolean(self != other);
	}
	
	public PLangObject __str_base(PLangObject self){
		return new Str("BaseClass");
	}
}
