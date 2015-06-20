package cz.upol.vanusanik.paralang.runtime;

import java.io.Serializable;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.NoValue;
import cz.upol.vanusanik.paralang.plang.types.Str;
import cz.upol.vanusanik.paralang.plang.types.TypeOperations.Operator;

/**
 * System.BaseClass class definition
 * 
 * @author Enerccio
 *
 */
public class BaseClass extends PLClass implements Serializable {
	private static final long serialVersionUID = -499503904346523232L;

	public BaseClass() {

	}

	@Override
	protected void ___init_internal_datafields(BaseCompiledStub self) {
		this.___restrictedOverride = true;

		___setkey("init", new FunctionWrapper("__init", this, true));
		___setkey("_str", new FunctionWrapper("__str_base", this, true));
		___setkey(Operator.EQ.classMethod, new FunctionWrapper("__eq__base",
				this, true));
		___setkey(Operator.NEQ.classMethod, new FunctionWrapper("__neq__base",
				this, true));

		this.___restrictedOverride = false;
	}

	public PLangObject __init(PLangObject self) {
		return NoValue.NOVALUE;
	}

	public PLangObject __eq__base(PLangObject self, PLangObject other) {
		return BooleanValue.fromBoolean(self == other);
	}

	public PLangObject __neq__base(PLangObject self, PLangObject other) {
		return BooleanValue.fromBoolean(self != other);
	}

	public PLangObject ___str_base(PLangObject self) {
		return new Str("BaseClass");
	}
}
