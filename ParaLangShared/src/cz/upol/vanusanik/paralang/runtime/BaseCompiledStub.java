package cz.upol.vanusanik.paralang.runtime;

import java.util.HashMap;
import java.util.Map;

import cz.upol.vanusanik.paralang.plang.PLangObject;

public abstract class BaseCompiledStub extends PLangObject {
	protected Map<String, PLangObject> __fieldsAndMethods;
	
	protected PLRuntime __get_runtime(){
		return PLRuntime.getRuntime();
	}
	
	public void __init_class(){
		__fieldsAndMethods = new HashMap<String, PLangObject>();
		isInited = true;
		__init_internal_datafields();
	}
	
	protected boolean isInited;
	
	protected abstract void __init_internal_datafields();
	
	public PLangObject __getkey(String key){
		if (!isInited){
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
		if (!isInited)
			__init_class();
		
		if (__fieldsAndMethods.containsKey(key))
			__fieldsAndMethods.remove(key);
		__fieldsAndMethods.put(key, var);
	}
}
