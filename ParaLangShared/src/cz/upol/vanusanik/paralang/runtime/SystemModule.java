package cz.upol.vanusanik.paralang.runtime;

import java.io.Serializable;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.plang.types.NoValue;

public class SystemModule extends PLModule implements Serializable {
	private static final long serialVersionUID = -499503904346523233L;

	@Override
	protected void ___init_internal_datafields(BaseCompiledStub self) {
		this.___restrictedOverride = true;
		
		___setkey("init", new FunctionWrapper("__init", this, false));
		___setkey("current_time", new FunctionWrapper("currentTime", this, false));
		
		this.___restrictedOverride = false;
	}

	public PLangObject __init(){
		return NoValue.NOVALUE;
	}
	
	public PLangObject currentTime(){
		return new Int(System.currentTimeMillis());
	}
}
