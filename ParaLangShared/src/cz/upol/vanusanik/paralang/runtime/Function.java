package cz.upol.vanusanik.paralang.runtime;

import java.io.Serializable;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.NoValue;

/**
 * System.Function
 * 
 * @author Enerccio
 *
 */
public class Function extends PLClass implements Serializable {
	private static final long serialVersionUID = -499503904346523210L;
	public static final String __applyMethod = "_apply";

	public Function() {

	}

	@Override
	protected void ___init_internal_datafields(BaseCompiledStub self) {
		this.___restrictedOverride = true;

		___setkey(PLClass.___superKey, new BaseClass());
		___setkey(__applyMethod, new FunctionWrapper("__apply__base", this,
				true));

		this.___restrictedOverride = false;
	}

	public PLangObject __apply__base(PLangObject self, PLangObject... args) {
		return NoValue.NOVALUE;
	}

}
