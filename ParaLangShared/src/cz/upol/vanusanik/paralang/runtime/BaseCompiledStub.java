package cz.upol.vanusanik.paralang.runtime;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;

public abstract class BaseCompiledStub extends PLangObject{
	private static final long serialVersionUID = -2885702496818908285L;
	protected Map<String, PLangObject> ___fieldsAndMethods;
	private Map<String, Long> ___fieldModificationMap = new HashMap<String, Long>();
	private Map<PLangObject, Set<String>> ___reverseMapLookup = new HashMap<PLangObject, Set<String>>(){

		private static final long serialVersionUID = 455190760365806731L;

		@Override
		public synchronized Set<String> get(Object key) {
			if (!containsKey(key))
				put((PLangObject) key, new HashSet<String>());
			return super.get(key);
		}
		
	};
	private Set<BaseCompiledStub> ___containers = new HashSet<BaseCompiledStub>();
	private final long __objectId;
	protected boolean ___restrictedOverride = false;
	
	protected BaseCompiledStub(){
		__objectId = PLRuntime.getRuntime().registerObject(this);
	}
	
	public long ___getObjectId(){
		return __objectId;
	}
	
	public void ___decouple(BaseCompiledStub coupler){
		___containers.remove(coupler);
	}
	
	public void __couple(BaseCompiledStub coupler){
		___containers.add(coupler);
	}
	
	protected PLRuntime ___get_runtime(){
		return PLRuntime.getRuntime();
	}
	
	public void ___init_class(){
		___fieldsAndMethods = new HashMap<String, PLangObject>();
		___isInited = true;
		___restrictedOverride = true;
		___init_internal_datafields();
		___restrictedOverride = false;
	}
	
	protected boolean ___isInited;
	
	protected abstract void ___init_internal_datafields();
	
	public PLangObject ___getkey(String key){
		if (!___isInited){
			___init_class();
		}
		
		if (!___fieldsAndMethods.containsKey(key))
			throw new RuntimeException("Unknown field or method");
		
		PLangObject data = ___fieldsAndMethods.get(key);
		return data;
	}
	
	public PLangObject ___getThis(){
		return this;
	}
	
	public void ___setkey(String key, PLangObject var){
		if (!___isInited)
			___init_class();
		
		if (!___restrictedOverride)
			PLRuntime.getRuntime().checkRestrictedAccess();
		
		if (___fieldsAndMethods.containsKey(key)){
			PLangObject oldValue = ___fieldsAndMethods.remove(key);
			if (oldValue instanceof BaseCompiledStub)
				((BaseCompiledStub) oldValue).___decouple(this);
			___reverseMapLookup.get(oldValue).remove(key);
			if (___reverseMapLookup.get(oldValue).size() == 0)
				___reverseMapLookup.remove(oldValue);
		}
		___fieldsAndMethods.put(key, var);
		if (var instanceof BaseCompiledStub)
			((BaseCompiledStub) var).__couple(this);
		___reverseMapLookup.get(var).add(key);
		
		___markModified(key);
		___propagateChanges();
	}

	private void ___markModified(String key) {
		if (___fieldModificationMap.containsKey(key))
			___fieldModificationMap.remove(key);
		___fieldModificationMap.put(key, new Long(new Date().getTime()));
	}

	private void __update(PLangObject changed) {
		for (String key : ___reverseMapLookup.get(changed)){
			___markModified(key);
		}
		___propagateChanges();
	}

	private static final ThreadLocal<Set<Object>> traversalChainSet = new ThreadLocal<Set<Object>>(){

		@Override
		protected Set<Object> initialValue() {
			return new HashSet<Object>();
		}
		
	};
	
	private void ___propagateChanges() {
		if (traversalChainSet.get().contains(this))
			return;
		traversalChainSet.get().add(this);
		for (BaseCompiledStub owner : ___containers)
			owner.__update(this);
		traversalChainSet.get().remove(this);
	}
	
	@Override
	public JsonValue ___toObject(long previousTime) {
		PLRuntime runtime = PLRuntime.getRuntime();
		JsonObject metaData = new JsonObject().add("metaObjectType", ___getType().toString());
		if (runtime.isAlreadySerialized(this)){
			metaData.add("link", true)
					.add("linkId", ___getObjectId());
		} else {
			runtime.setAsAlreadySerialized(this);
			metaData.add("isBaseClass", false)
					.add("link", false)
					.add("isInited", ___isInited)
					.add("modifiedFrom", previousTime)
					.add("thisLink", ___getObjectId())
					.add("className", getClass().getSimpleName())
					.add("modifiedFromFields", ___getDeltaFields(previousTime));
		}
		return metaData;
	}

	private JsonArray ___getDeltaFields(long previousTime) {
		JsonArray array = new JsonArray();
		for (String field : ___fieldModificationMap.keySet()){
			if (___fieldModificationMap.get(field) > previousTime){
				JsonObject f = new JsonObject();
				
				f.add("fieldName", field);
				f.add("fieldValue", ___fieldsAndMethods.get(field).___toObject(previousTime));
				
				array.add(f);
			}
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
	
	protected PLangObject ___convertBoolean(boolean b){
		return BooleanValue.fromBoolean(b);
	}
	
	@Override
	public String toString(){
		if (___fieldsAndMethods.containsKey("__str")){
			PLangObject str = ___getkey("__str");
			if (str instanceof FunctionWrapper){
				return PLRuntime.getRuntime().run(str, (BaseCompiledStub)this.___getThis()).toString();
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
		return (BaseCompiledStub) ___fieldsAndMethods.get(PLClass.__superKey);
	}
}
