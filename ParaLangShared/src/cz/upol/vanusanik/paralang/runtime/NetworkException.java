package cz.upol.vanusanik.paralang.runtime;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.NoValue;

public class NetworkException extends PLClass{
	private static final long serialVersionUID = -1870114368385241652L;
	
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
		
		
		this.___restrictedOverride = false;
	}

	public PLangObject __init__base(PLangObject self, PLangObject message){
		// run super init
		BaseException bc = (BaseException)___getkey(BaseClass.___superKey);
		PLRuntime.getRuntime().run(bc.___getkey("init"), bc);
		
		___setkey(BaseException.__messageField, message);
		return NoValue.NOVALUE;
	}	
	
}
