package cz.upol.vanusanik.paralang.runtime;

import java.util.HashMap;
import java.util.Map;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;

public class PLRuntime {
	private static final ThreadLocal<PLRuntime> localRuntime = new ThreadLocal<PLRuntime>();
	
	public static final PLRuntime getRuntime(){
		return localRuntime.get();
	}

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
	
	public PLClass newInstance(String fqname){
		String[] components = fqname.split("\\.");
		if (components.length != 2) throw new RuntimeException("Malformed name of the class!");
		if (!classMap.containsKey(components[0])) throw new RuntimeException("Unknown module!");
		if (!classMap.get(components[0]).containsKey(components[1])) throw new RuntimeException("Unknown class!");
		try {
			PLClass instance = (PLClass) classMap.get(components[0]).get(components[1]).newInstance();
			run(instance.__getkey("init")); // run constructor
			return instance;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void addModlue(String fqName, PLModule module){
		moduleMap.put(fqName, module);
	}
	
	public PLModule getModule(String moduleName){
		return moduleMap.get(moduleName);
	}
	
	public PLangObject run(String module, String runnable){
		PLModule mod = moduleMap.get(module);
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
}
