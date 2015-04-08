package cz.upol.vanusanik.paralang.runtime;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.Flt;
import cz.upol.vanusanik.paralang.plang.types.NoValue;

public class BaseFloat extends BaseNumber {
	private static final long serialVersionUID = 9204688374246651301L;
	
	public BaseFloat(){
		
	}

	@Override
	public PLangObject __init_superclass(PLangObject self, PLangObject iv){
		if (!iv.___isNumber()){
			throw new RuntimeException("Value " + iv + " is not a number!");
		}
		this.___restrictedOverride = true;
		___setkey(__valKey, new Flt(iv.___getNumber(iv).floatValue()));
		this.___restrictedOverride = false;
		return NoValue.NOVALUE;
	}
	
	@Override
	protected PLangObject asObject(PLangObject o) {
		return PLRuntime.getRuntime().newInstance("System.Float", o);
	}
}
