package cz.upol.vanusanik.paralang.runtime;

import java.io.Serializable;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.NoValue;
import cz.upol.vanusanik.paralang.plang.types.Str;

public class BaseException extends PLClass implements Serializable {
	private static final long serialVersionUID = 8923942805591451790L;
	private static final String __messageField = "__message";
	private static final String __messageGetter = "get_message";

	public BaseException(){
		
	}

	@Override
	protected void __init_internal_datafields() {
		this.__restrictedOverride = true;
		
		__setkey(BaseClass.__superKey, new BaseClass());
		__setkey("init", new FunctionWrapper("__init__base", this, true));
		__setkey(__messageGetter, new FunctionWrapper(__messageGetter, this, true));
		__setkey("__str", new FunctionWrapper("__str", this, true));
		
		__setkey(__messageField, NoValue.NOVALUE);
		
		
		this.__restrictedOverride = false;
	}

	public PLangObject __init__base(PLangObject self, PLangObject message){
		// run super init
		BaseClass bc = (BaseClass)__getkey(BaseClass.__superKey);
		PLRuntime.getRuntime().run(bc.__getkey("init"), bc);
		
		__setkey(__messageField, message);
		return NoValue.NOVALUE;
	}
	
	public PLangObject get_message(BaseCompiledStub self){
		return self.__getkey(__messageField);
	}
	
	public PLangObject __str(BaseCompiledStub self){
		return new Str(PLRuntime.getRuntime().run(self.__getkey(__messageGetter), self).toString());
	}

	@Override
	public String getMessage() {
		return toString();
	}
	
	
	
}

