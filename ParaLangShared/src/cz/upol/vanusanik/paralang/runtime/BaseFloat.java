package cz.upol.vanusanik.paralang.runtime;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.Flt;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.plang.types.NoValue;

/**
 * System.BaseFloat
 * 
 * @author Enerccio
 *
 */
public class BaseFloat extends BaseNumber {
	private static final long serialVersionUID = 9204688374246651301L;

	public BaseFloat() {

	}

	@Override
	public PLangObject __init_superclass(PLangObject self, PLangObject iv) {
		if (!iv.___isNumber()) {
			throw new RuntimeException("Value " + iv + " is not a number!");
		}
		this.___restrictedOverride = true;
		___setkey(__valKey, new Flt(iv.___getNumber(iv).floatValue()));
		___setkey("sqrt", new FunctionWrapper("sqrt", this, true));
		___setkey(__toInt, new FunctionWrapper("__toInt", this, true));
		this.___restrictedOverride = false;
		return NoValue.NOVALUE;
	}

	@Override
	protected PLangObject asObject(PLangObject o) {
		return PLRuntime.getRuntime().newInstance("System.Float", o);
	}

	public PLangObject sqrt(PLangObject self) {
		Flt value = (Flt) ___getkey(__valKey);
		Flt result = new Flt((float) Math.sqrt(value.___getNumber(value)));
		return asObject(result);
	}

	public PLangObject toInt(PLangObject self) {
		Flt value = (Flt) ___getkey(__valKey);
		return new Int(value.___getNumber(value).longValue());
	}
}
