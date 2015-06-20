package cz.upol.vanusanik.paralang.runtime;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.TypeOperations.Operator;

/**
 * PLang class instance
 * 
 * @author Enerccio
 *
 */
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

	@Override
	public PLangObject ___getkey(String key, boolean askedParent) {
		if (!___isInited) {
			___init_class();
		}

		PLClass parent = null;
		if (!___fieldsAndMethods.containsKey(key)
				&& ((parent = ___getSuper()) != null))
			return parent.___getkey(key, askedParent);
		else if (!___fieldsAndMethods.containsKey(key)) {
			return null;
		}
		
		return ___getkey_internal(key, askedParent);
	}

	private PLangObject ___getkey_internal(String key, boolean parent) {
		if (!___isInited) {
			___init_class();
		}
		
		if (!parent && !key.startsWith("$$") && !key.startsWith("__") && !key.equals("init")){
			if (___fieldsAndMethods.containsKey(___derivedKey)){
				PLangObject value = ((PLClass) ___fieldsAndMethods.get(___derivedKey)).___getkey_internal(key, parent);
				if (value != null)
					return value;
			}
		}
		
		return ___fieldsAndMethods.get(key); 
	}

	@Override
	protected boolean ___setkey_internal(String key, PLangObject var) {
		// we need to check if derived exists and if so, we need to save values there instead of here
		if (!___isInited) {
			___init_class();
		}
		
		if (___fieldsAndMethods.containsKey(___derivedKey)){
			if (!key.startsWith("$$") && !key.startsWith("__") && !key.equals("init"))
				if (((PLClass) ___fieldsAndMethods.get(___derivedKey)).___setkey_internal(key, var))
					return true;
		}
		
		return super.___setkey_internal(key, var);
	}

	/**
	 * Returns super class if this instance has any
	 * 
	 * @return
	 */
	public PLClass ___getSuper() {
		if (!___isInited) {
			___init_class();
		}

		if (!___fieldsAndMethods.containsKey(___superKey))
			return null;

		return (PLClass) ___getkey(___superKey, true);
	}

	/**
	 * Sets the derived class this instance is parent of.
	 * 
	 * @param derived
	 */
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
						___getkey(Operator.EQ.classMethod, false),
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
		PLangObject str = ___getkey("__str", false);
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

	/**
	 * Returns true if this class is exception, ie derives from BaseException.
	 * 
	 * @param self
	 *            this object
	 * @return
	 */
	protected boolean ___isException(PLangObject self) {
		if (this instanceof BaseException)
			return true;
		PLClass superclass = ___getSuper();
		if (superclass == null)
			return false;
		return superclass.___isException(self);
	}
}
