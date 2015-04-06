package cz.upol.vanusanik.paralang.runtime;

import java.io.Serializable;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.NoValue;
import cz.upol.vanusanik.paralang.plang.types.Str;

public class BaseException extends PLClass implements Serializable {
	private static final long serialVersionUID = 8923942805591451790L;
	static final String __messageField = "__message";
	static final String __messageGetter = "get_message";

	public BaseException(){
		
	}

	@Override
	protected void ___init_internal_datafields() {
		this.___restrictedOverride = true;
		
		___setkey(BaseClass.___superKey, new BaseClass());
		___setkey("init", new FunctionWrapper("__init__base", this, true));
		___setkey(__messageGetter, new FunctionWrapper(__messageGetter, this, true));
		___setkey("__str", new FunctionWrapper("__str", this, true));
		
		___setkey(__messageField, NoValue.NOVALUE);
		
		
		this.___restrictedOverride = false;
	}

	public PLangObject __init__base(PLangObject self, PLangObject message){
		// run super init
		BaseClass bc = (BaseClass)___getkey(BaseClass.___superKey);
		PLRuntime.getRuntime().run(bc.___getkey("init"), bc);
		
		___setkey(__messageField, message);
		return NoValue.NOVALUE;
	}
	
	public PLangObject get_message(BaseCompiledStub self){
		return self.___getkey(__messageField);
	}
	
	public PLangObject __str(BaseCompiledStub self){
		return new Str(PLRuntime.getRuntime().run(self.___getkey(__messageGetter), self).toString());
	}

	@Override
	public String getMessage() {
		return toString();
	}
	
	
	
}

