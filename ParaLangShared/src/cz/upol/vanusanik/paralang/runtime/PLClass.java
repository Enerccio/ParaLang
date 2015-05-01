package cz.upol.vanusanik.paralang.runtime;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.TypeOperations.Operator;

public abstract class PLClass extends BaseCompiledStub {
	private static final long serialVersionUID = 715934816813529044L;
	public static final String ___superKey = "$$__parent__$$";
	public static final String ___derivedKey = "$$__derived__$$";

	@Override
	public PlangObjectType ___getType() {
		return PlangObjectType.CLASS;
	}

	@Override
	public void ___init_class() {
		super.___init_class();
		___fieldsAndMethods.put("inst", this);
	}

	public PLangObject ___getkey(String key) {
		if (!___isInited) {
			___init_class();
		}

		PLClass parent = null;
		if (!___fieldsAndMethods.containsKey(key)
				&& ((parent = ___getSuper()) != null))
			return parent.___getkey(key);
		else if (!___fieldsAndMethods.containsKey(key)) {
			return null;
		}

		PLangObject data = ___fieldsAndMethods.get(key);
		return data;
	}

	public PLClass ___getSuper() {
		if (!___isInited) {
			___init_class();
		}

		if (!___fieldsAndMethods.containsKey(___superKey))
			return null;

		return (PLClass) ___getkey(___superKey);
	}

	public void ___setDerivedClass(PLClass derived) {
		boolean prev = ___restrictedOverride;
		___restrictedOverride = true;
		___setkey(___derivedKey, derived);
		___restrictedOverride = prev;
	}

	@Override
	public boolean ___eq(PLangObject self, PLangObject b) {
		return BooleanValue
				.toBoolean(PLRuntime.getRuntime().run(
						___getkey(Operator.EQ.classMethod),
						(BaseCompiledStub) self, b));
	}

	@Override
	public BaseCompiledStub ___getLowestClassInstance() {
		if (___fieldsAndMethods.containsKey(___derivedKey))
			return (BaseCompiledStub) ___fieldsAndMethods.get(___derivedKey);
		return null;
	}

	@Override
	public String toString(PLangObject self) {
		PLangObject str = ___getkey("__str");
		if (str != null && str instanceof FunctionWrapper) {
			PLangObject o = PLRuntime.getRuntime().run(str,
					(BaseCompiledStub) self);
			return o.toString(o);
		}
		return super.toString();
	}

	@Override
	public Throwable fillInStackTrace() {
		if (___isException(this))
			return super.fillInStackTrace();
		else
			return this;
	}

	protected boolean ___isException(PLangObject self) {
		if (this instanceof BaseException)
			return true;
		PLClass superclass = ___getSuper();
		if (superclass == null)
			return false;
		return superclass.___isException(self);
	}
}
