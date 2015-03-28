package cz.upol.vanusanik.paralang.runtime;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;

public class PLRuntime {
	private static final ThreadLocal<PLRuntime> localRuntime = new ThreadLocal<PLRuntime>();
	
	public static final PLRuntime getRuntime(){
		return localRuntime.get();
	}
	
	private long objectIdCounter = 0;
	
	public long registerObject(BaseCompiledStub object){
		return objectIdCounter++;
	}

	private Map<String, Class<? extends PLModule>> preloadedModuleMap = new HashMap<String, Class<? extends PLModule>>();
	private Map<String, PLModule> moduleMap = new HashMap<String, PLModule>();
	private Map<String, Map<String, Class<?>>> classMap = new HashMap<String, Map<String,Class<?>>>();
	
	private boolean isSafeContext = false;
	private boolean isRestricted = true;
	
	public PLRuntime(){
		localRuntime.set(this);
	}
	
	public void registerClass(String module, String className, Class<?> cls){
		if (!classMap.containsKey(module))
			classMap.put(module, new HashMap<String, Class<?>>());
		classMap.get(module).put(className, cls);
	}
	
	public PLClass newInstance(String fqname, PLangObject... inits){
		String[] components = fqname.split("\\.");
		if (components.length != 2) throw new RuntimeException("Malformed name of the class!");
		if (!classMap.containsKey(components[0])) throw new RuntimeException("Unknown module!");
		if (!classMap.get(components[0]).containsKey(components[1])) throw new RuntimeException("Unknown class!");
		try {
			PLClass instance = (PLClass) classMap.get(components[0]).get(components[1]).newInstance();
			run(instance.__getkey("init"), inits); // run constructor
			return instance;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void addModule(String fqName, Class<? extends PLModule> module){
		preloadedModuleMap.put(fqName, module);
	}
	
	public PLModule getModule(String moduleName){
		if (!moduleMap.containsKey(moduleName)){
			if (!preloadedModuleMap.containsKey(moduleName))
				throw new RuntimeException("Module load failed: No such module " + moduleName);
			try {
				PLModule module = preloadedModuleMap.get(moduleName).newInstance();
				moduleMap.put(moduleName, module);
				run(moduleName, "init");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return moduleMap.get(moduleName);
	}
	
	public PLangObject run(String module, String runnable){
		PLModule mod = getModule(module);
		return run(mod.__getkey(runnable));
	}
	
	public void checkRestrictedAccess(){
		if (isRestricted)
			throw new RuntimeException("Restricted mode");
	}
	
	public PLangObject runUnsafe(PLangObject runner, PLangObject...args){
		if (!isSafeContext)
			throw new RuntimeException("Unsafe context for this call");
		return run(runner, args);
	}
	
	public PLangObject run(PLangObject runner, PLangObject... args){
		if (runner.getType() != PlangObjectType.FUNCTION && runner.getType() != PlangObjectType.JAVAWRAPPER)
			throw new RuntimeException("Field is not callable");
	
		if (runner.getType() == PlangObjectType.FUNCTION){
			FunctionWrapper wrapper = (FunctionWrapper)runner;
			try {
				return wrapper.run(args);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	public void setRestricted(boolean restricted) {
		isRestricted = restricted;
	}

	private Set<Object> serializedObjects = new HashSet<Object>();
	
	public void setAsAlreadySerialized(BaseCompiledStub baseCompiledStub) {
		serializedObjects.add(baseCompiledStub);
	}

	public boolean isAlreadySerialized(BaseCompiledStub baseCompiledStub) {
		return serializedObjects.contains(baseCompiledStub);
	}
	
	/*
	 * Only serializes the actual content, not class definitions
	 */
	public void serializeRuntimeContent(OutputStream os, long previousSerialization) throws Exception {
		serializedObjects.clear();
		
		JsonObject root = new JsonObject();
		JsonArray modules = new JsonArray();
		
		for (String moduleName : moduleMap.keySet()){
			modules.add(moduleMap.get(moduleName).toObject(previousSerialization));
		}
		
		root.add("modules", modules);
		os.write(root.toString(WriterConfig.PRETTY_PRINT).getBytes("utf-8"));
	}
}
