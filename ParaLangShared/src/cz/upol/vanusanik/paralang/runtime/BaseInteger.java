package cz.upol.vanusanik.paralang.runtime;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.Flt;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.plang.types.NoValue;
import cz.upol.vanusanik.paralang.plang.types.TypeOperations;
import cz.upol.vanusanik.paralang.plang.types.TypeOperations.Operator;

public class BaseInteger extends BaseNumber {
	private static final long serialVersionUID = 2544264163945283955L;

	public BaseInteger() {

	}

	@Override
	public PLangObject __init_superclass(PLangObject self, PLangObject iv) {
		if (!iv.___isNumber()) {
			throw new RuntimeException("Value " + iv + " is not a number!");
		}
		this.___restrictedOverride = true;
		___setkey(__valKey, new Int(iv.___getNumber(iv).intValue()));
		___setkey(__toInt, new FunctionWrapper("__toInt", this, true));
		___setkey(Operator.UBINNEG.classMethod, new FunctionWrapper(
				"__ubn_base", this, true));
		___setkey("sqrt", new FunctionWrapper("sqrt", this, true));
		this.___restrictedOverride = false;

		return NoValue.NOVALUE;
	}

	public PLangObject __ubn_base(PLangObject self) {
		return asObject(TypeOperations.ubneg(((PLClass) self)
				.___getkey(__valKey)));
	}

	@Override
	protected PLangObject asObject(PLangObject o) {
		return PLRuntime.getRuntime().newInstance("System.Integer", o);
	}

	public PLangObject sqrt(PLangObject self) {
		Int value = (Int) ___getkey(__valKey);
		Flt result = new Flt((float) Math.sqrt(value.getValue()));
		return asObject(result);
	}

	public PLangObject toInt(PLangObject self) {
		Int value = (Int) ___getkey(__valKey);
		return value;
	}
}
