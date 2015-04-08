package cz.upol.vanusanik.paralang.runtime;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.NoValue;

public class NetworkException extends PLClass{
	private static final long serialVersionUID = -1870114368385241652L;
	
	public static final String listKey = "___prevExps";
	public static final String getExceptions = "get_exceptions";
	
	public NetworkException(){
		
	}

	@Override
	protected void ___init_internal_datafields() {
		this.___restrictedOverride = true;
		
		BaseException be = new BaseException();
		be.___init_class();
		___setkey(BaseClass.___superKey, be);
		___setkey("init", new FunctionWrapper("__init__base", this, true));
		___setkey(BaseException.__messageField, NoValue.NOVALUE);
		___setkey(listKey, NoValue.NOVALUE);
		___setkey(getExceptions, new FunctionWrapper("__get_exceptions", this, true));
		
		this.___restrictedOverride = false;
	}
	
	public PLangObject __get_exceptions(PLangObject self){
		return ((PLClass) self).___getkey(listKey); 
	}

	public PLangObject __init__base(PLangObject self, PLangObject message){
		// run super init
		BaseException bc = (BaseException)___getkey(BaseClass.___superKey);
		PLRuntime.getRuntime().run(bc.___getkey("init"), bc);
		
		___setkey(BaseException.__messageField, message);
		return NoValue.NOVALUE;
	}	
	
}
