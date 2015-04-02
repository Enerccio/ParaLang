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
	protected Map<String, PLangObject> __fieldsAndMethods;
	private Map<String, Long> __fieldModificationMap = new HashMap<String, Long>();
	private Map<PLangObject, Set<String>> __reverseMapLookup = new HashMap<PLangObject, Set<String>>(){

		private static final long serialVersionUID = 455190760365806731L;

		@Override
		public synchronized Set<String> get(Object key) {
			if (!containsKey(key))
				put((PLangObject) key, new HashSet<String>());
			return super.get(key);
		}
		
	};
	private Set<BaseCompiledStub> __containers = new HashSet<BaseCompiledStub>();
	private final long objectId;
	protected boolean __restrictedOverride = false;
	
	protected BaseCompiledStub(){
		objectId = PLRuntime.getRuntime().registerObject(this);
	}
	
	public long __getObjectId(){
		return objectId;
	}
	
	public void __decouple(BaseCompiledStub coupler){
		__containers.remove(coupler);
	}
	
	public void __couple(BaseCompiledStub coupler){
		__containers.add(coupler);
	}
	
	protected PLRuntime __get_runtime(){
		return PLRuntime.getRuntime();
	}
	
	public void __init_class(){
		__fieldsAndMethods = new HashMap<String, PLangObject>();
		__isInited = true;
		__restrictedOverride = true;
		__init_internal_datafields();
		__restrictedOverride = false;
	}
	
	protected boolean __isInited;
	
	protected abstract void __init_internal_datafields();
	
	public PLangObject __getkey(String key){
		if (!__isInited){
			__init_class();
		}
		
		if (!__fieldsAndMethods.containsKey(key))
			throw new RuntimeException("Unknown field or method");
		
		PLangObject data = __fieldsAndMethods.get(key);
		return data;
	}
	
	public PLangObject __getThis(){
		return this;
	}
	
	public void __setkey(String key, PLangObject var){
		if (!__isInited)
			__init_class();
		
		if (!__restrictedOverride)
			PLRuntime.getRuntime().checkRestrictedAccess();
		
		if (__fieldsAndMethods.containsKey(key)){
			PLangObject oldValue = __fieldsAndMethods.remove(key);
			if (oldValue instanceof BaseCompiledStub)
				((BaseCompiledStub) oldValue).__decouple(this);
			__reverseMapLookup.get(oldValue).remove(key);
			if (__reverseMapLookup.get(oldValue).size() == 0)
				__reverseMapLookup.remove(oldValue);
		}
		__fieldsAndMethods.put(key, var);
		if (var instanceof BaseCompiledStub)
			((BaseCompiledStub) var).__couple(this);
		__reverseMapLookup.get(var).add(key);
		
		__markModified(key);
		__propagateChanges();
	}

	private void __markModified(String key) {
		if (__fieldModificationMap.containsKey(key))
			__fieldModificationMap.remove(key);
		__fieldModificationMap.put(key, new Long(new Date().getTime()));
	}

	private void __update(PLangObject changed) {
		for (String key : __reverseMapLookup.get(changed)){
			__markModified(key);
		}
		__propagateChanges();
	}

	private static final ThreadLocal<Set<Object>> traversalChainSet = new ThreadLocal<Set<Object>>(){

		@Override
		protected Set<Object> initialValue() {
			return new HashSet<Object>();
		}
		
	};
	
	private void __propagateChanges() {
		if (traversalChainSet.get().contains(this))
			return;
		traversalChainSet.get().add(this);
		for (BaseCompiledStub owner : __containers)
			owner.__update(this);
		traversalChainSet.get().remove(this);
	}
	
	@Override
	public JsonValue __sys_m_toObject(long previousTime) {
		PLRuntime runtime = PLRuntime.getRuntime();
		JsonObject metaData = new JsonObject().add("metaObjectType", __sys_m_getType().toString());
		if (runtime.isAlreadySerialized(this)){
			metaData.add("link", true)
					.add("linkId", __getObjectId());
		} else {
			runtime.setAsAlreadySerialized(this);
			metaData.add("isBaseClass", false)
					.add("link", false)
					.add("isInited", __isInited)
					.add("modifiedFrom", previousTime)
					.add("thisLink", __getObjectId())
					.add("className", getClass().getSimpleName())
					.add("modifiedFromFields", getDeltaFields(previousTime));
		}
		return metaData;
	}

	private JsonArray getDeltaFields(long previousTime) {
		JsonArray array = new JsonArray();
		for (String field : __fieldModificationMap.keySet()){
			if (__fieldModificationMap.get(field) > previousTime){
				JsonObject f = new JsonObject();
				
				f.add("fieldName", field);
				f.add("fieldValue", __fieldsAndMethods.get(field).__sys_m_toObject(previousTime));
				
				array.add(f);
			}
		}
		return array;
	}
	
	@Override
	public boolean __sys_m_isNumber() {
		return false;
	}

	@Override
	public Float __sys_m_getNumber(PLangObject self) {
		return null;
	}
	
	@Override
	public boolean eq(PLangObject self, PLangObject b) {
		return self == b;
	}
	
	protected PLangObject __convertBoolean(boolean b){
		return BooleanValue.fromBoolean(b);
	}
	
	@Override
	public String toString(){
		if (__fieldsAndMethods.containsKey("__str")){
			PLangObject str = __getkey("__str");
			if (str instanceof FunctionWrapper){
				return PLRuntime.getRuntime().run(str, (BaseCompiledStub)this.__getThis()).toString();
			}
		}
		return super.toString();
	}

	public BaseCompiledStub __getLowestClassInstance() {
		return null;
	}

	public BaseCompiledStub __getParent() {
		if (!__isInited)
			__init_class();
		return (BaseCompiledStub) __fieldsAndMethods.get(PLClass.__superKey);
	}
}
