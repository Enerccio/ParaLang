package cz.upol.vanusanik.paralang.runtime;

import java.util.HashMap;
import java.util.Map;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.utils.Utils;

/**
 * BaseCompiledStub is base class for all PLang classes/modules.
 * 
 * @author Enerccio
 *
 */
public abstract class BaseCompiledStub extends RuntimeException implements
		PLangObject {
	private static final long serialVersionUID = -2885702496818908285L;
	/** fields and methods are stored here */
	protected Map<String, PLangObject> ___fieldsAndMethods;
	/** id used to serialize/deserialize object */
	public long ___objectId;
	/** whether restricted check is ignored or not */
	protected boolean ___restrictedOverride = false;

	protected BaseCompiledStub() {
		___objectId = PLRuntime.getRuntime().registerObject(this);
	}

	/**
	 * Returns object id of this object
	 * 
	 * @return
	 */
	public long ___getObjectId() {
		return ___objectId;
	}

	/**
	 * Returns current runtime
	 * 
	 * @return
	 */
	protected PLRuntime ___get_runtime() {
		return PLRuntime.getRuntime();
	}

	/**
	 * Initializes this class, called only once
	 */
	public void ___init_class() {
		___fieldsAndMethods = new HashMap<String, PLangObject>();
		___isInited = true;
		___restrictedOverride = true;
		___init_internal_datafields(this);
		___restrictedOverride = false;
	}

	/** Whether this object was initialized or not */
	protected boolean ___isInited;

	/**
	 * Internal init method that is added by the PLCompiler
	 * 
	 * @param self
	 */
	protected abstract void ___init_internal_datafields(BaseCompiledStub self);

	/**
	 * Returns value for key in fields/methods
	 * 
	 * @param key
	 * @return
	 */
	public PLangObject ___getkey(String key, boolean askedParent) {
		if (!___isInited) {
			___init_class();
		}

		if (!___fieldsAndMethods.containsKey(key))
			throw new RuntimeException("Unknown field or method");

		PLangObject data = ___fieldsAndMethods.get(key);
		return data;
	}

	/**
	 * Returns this
	 * 
	 * @return
	 */
	public PLangObject ___getThis() {
		return this;
	}

	/**
	 * Sets the field/method identified by key to value var
	 * 
	 * @param key
	 * @param var
	 */
	public void ___setkey(String key, PLangObject var) {
		___setkey_internal(key, var);
	}

	protected boolean ___setkey_internal(String key, PLangObject var) {
		if (!___isInited)
			___init_class();

		if (!___restrictedOverride)
			PLRuntime.getRuntime().checkRestrictedAccess(this);

		___fieldsAndMethods.put(key, var);
		return true;
	}

	@Override
	public JsonValue ___toObject() {
		PLRuntime runtime = PLRuntime.getRuntime();
		JsonObject metaData = new JsonObject().add("metaObjectType",
				___getType().toString());
		if (runtime.isAlreadySerialized(this)) {
			metaData.add("link", true).add("linkId", ___getObjectId());
		} else {
			runtime.setAsAlreadySerialized(this);
			metaData.add("isBaseClass", false).add("link", false)
					.add("isInited", ___isInited)
					.add("thisLink", ___getObjectId())
					.add("className", getClass().getSimpleName())
					.add("fields", ___getFields());
		}
		return metaData;
	}

	/**
	 * Serialize all fields
	 * 
	 * @return
	 */
	protected JsonArray ___getFields() {
		JsonArray array = new JsonArray();
		for (String field : ___fieldsAndMethods.keySet()) {
			JsonObject f = new JsonObject();

			f.add("fieldName", field);
			f.add("fieldValue", ___fieldsAndMethods.get(field).___toObject());

			array.add(f);
		}
		return array;
	}

	@Override
	public boolean ___isNumber() {
		return false;
	}

	@Override
	public Float ___getNumber(PLangObject self) {
		return null;
	}

	@Override
	public boolean ___eq(PLangObject self, PLangObject b) {
		return self == b;
	}

	protected PLangObject ___convertBoolean(boolean b) {
		return BooleanValue.fromBoolean(b);
	}

	@Override
	public boolean ___less(PLangObject self, PLangObject other, boolean equals) {
		throw new RuntimeException("Undefined method for this type!");
	}

	@Override
	public boolean ___more(PLangObject self, PLangObject other, boolean equals) {
		throw new RuntimeException("Undefined method for this type!");
	}

	@Override
	public BaseCompiledStub ___getLowestClassdef() {
		return null;
	}

	@Override
	public String toString(PLangObject self) {
		return toString();
	}

	@Override
	public String toString() {
		if (___fieldsAndMethods.containsKey("_str")) {
			PLangObject str = ___getkey("_str", false);
			if (str instanceof FunctionWrapper) {
				return PLRuntime.getRuntime()
						.run(str, (BaseCompiledStub) this.___getThis())
						.toString();
			}
		}
		return super.toString();
	}

	/**
	 * Returns lowest class instance. Ie, if this object is actually part of the
	 * class chain, it returns actually lowest class. Ie, if class Foo extends
	 * Bar, if Bar object bound to that particular Foo is asked for this, it
	 * returns Foo.
	 * 
	 * @return
	 */
	public BaseCompiledStub ___getLowestClassInstance() {
		return null;
	}

	/**
	 * Returns parent of this object
	 * 
	 * @return
	 */
	public BaseCompiledStub ___getParent() {
		if (!___isInited)
			___init_class();
		return (BaseCompiledStub) ___fieldsAndMethods.get(PLClass.___superKey);
	}
	
	public BaseCompiledStub ___rebuildStack(){
		fillInStackTrace();
		setStackTrace(Utils.removeStackElements(getStackTrace(), 1));
		return this;
	}
}
