package cz.upol.vanusanik.paralang.runtime;

import java.io.Serializable;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.NoValue;
import cz.upol.vanusanik.paralang.plang.types.Str;

/**
 * System.BaseException
 * 
 * @author Enerccio
 *
 */
public class BaseException extends PLClass implements Serializable {
	private static final long serialVersionUID = 8923942805591451790L;
	static final String __messageField = "_message";
	static final String __messageGetter = "get_message";

	public BaseException() {

	}

	@Override
	protected void ___init_internal_datafields(BaseCompiledStub self) {
		this.___restrictedOverride = true;

		___setkey(PLClass.___superKey, new BaseClass());
		___setkey("init", new FunctionWrapper("__init__base", this, true));
		___setkey(__messageGetter, new FunctionWrapper(__messageGetter, this,
				true));
		___setkey("_str", new FunctionWrapper("__str", this, true));

		___setkey(__messageField, NoValue.NOVALUE);

		this.___restrictedOverride = false;
	}

	public PLangObject __init__base(PLangObject self, PLangObject message) {
		// run super init
		___restrictedOverride = true;
		BaseClass bc = (BaseClass) ___getkey(PLClass.___superKey, true);
		PLRuntime.getRuntime().run(bc.___getkey("init", true), bc);

		___setkey(__messageField, message);
		___restrictedOverride = false;
		return NoValue.NOVALUE;
	}

	public PLangObject get_message(BaseCompiledStub self) {
		return self.___getkey(__messageField, false);
	}

	public PLangObject __str(BaseCompiledStub self) {
		return new Str(PLRuntime.getRuntime()
				.run(self.___getkey(__messageGetter, false), self).toString());
	}

	@Override
	public String getMessage() {
		return toString();
	}
}
