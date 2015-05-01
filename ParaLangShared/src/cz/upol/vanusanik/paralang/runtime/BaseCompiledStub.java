package cz.upol.vanusanik.paralang.runtime;

import java.util.HashMap;
import java.util.Map;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;

public abstract class BaseCompiledStub extends RuntimeException implements
		PLangObject {
	private static final long serialVersionUID = -2885702496818908285L;
	protected Map<String, PLangObject> ___fieldsAndMethods;
	public long ___objectId;
	protected boolean ___restrictedOverride = false;

	protected BaseCompiledStub() {
		___objectId = PLRuntime.getRuntime().registerObject(this);
	}

	public long ___getObjectId() {
		return ___objectId;
	}

	protected PLRuntime ___get_runtime() {
		return PLRuntime.getRuntime();
	}

	public void ___init_class() {
		___fieldsAndMethods = new HashMap<String, PLangObject>();
		___isInited = true;
		___restrictedOverride = true;
		___init_internal_datafields(this);
		___restrictedOverride = false;
	}

	protected boolean ___isInited;

	protected abstract void ___init_internal_datafields(BaseCompiledStub self);

	public PLangObject ___getkey(String key) {
		if (!___isInited) {
			___init_class();
		}

		if (!___fieldsAndMethods.containsKey(key))
			throw new RuntimeException("Unknown field or method");

		PLangObject data = ___fieldsAndMethods.get(key);
		return data;
	}

	public PLangObject ___getThis() {
		return this;
	}

	public void ___setkey(String key, PLangObject var) {
		if (!___isInited)
			___init_class();

		if (!___restrictedOverride)
			PLRuntime.getRuntime().checkRestrictedAccess(this);

		___fieldsAndMethods.put(key, var);

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

	public boolean ___less(PLangObject self, PLangObject other, boolean equals) {
		throw new RuntimeException("Undefined method for this type!");
	}

	public boolean ___more(PLangObject self, PLangObject other, boolean equals) {
		throw new RuntimeException("Undefined method for this type!");
	}

	public BaseCompiledStub ___getLowestClassdef() {
		return null;
	}

	public String toString(PLangObject self) {
		return toString();
	}

	@Override
	public String toString() {
		if (___fieldsAndMethods.containsKey("__str")) {
			PLangObject str = ___getkey("__str");
			if (str instanceof FunctionWrapper) {
				return PLRuntime.getRuntime()
						.run(str, (BaseCompiledStub) this.___getThis())
						.toString();
			}
		}
		return super.toString();
	}

	public BaseCompiledStub ___getLowestClassInstance() {
		return null;
	}

	public BaseCompiledStub ___getParent() {
		if (!___isInited)
			___init_class();
		return (BaseCompiledStub) ___fieldsAndMethods.get(PLClass.___superKey);
	}
}
