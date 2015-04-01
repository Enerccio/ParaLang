package cz.upol.vanusanik.paralang.runtime;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Collection;

import org.apache.commons.io.DirectoryWalker;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;

import cz.upol.vanusanik.paralang.compiler.DiskFileDesignator;
import cz.upol.vanusanik.paralang.compiler.PLCompiler;
import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.Pointer;

public class PLRuntime {
	private static final ThreadLocal<PLRuntime> localRuntime = new ThreadLocal<PLRuntime>();
	private static final HashMap<String, Class<? extends PLClass>> __SYSTEM_CLASSES = new HashMap<String, Class<? extends PLClass>>();
	public static final Map<String, Class<? extends PLClass>> SYSTEM_CLASSES = Collections.synchronizedMap(Collections.unmodifiableMap(__SYSTEM_CLASSES));
	
	static {
		__SYSTEM_CLASSES.put("BaseClass", BaseClass.class);
		__SYSTEM_CLASSES.put("Function", Function.class);
		__SYSTEM_CLASSES.put("Integer", BaseInteger.class);
	}
	
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
	private Map<String, Long> uuidMap = new HashMap<String, Long>();
	
	private boolean isSafeContext = false;
	private boolean isRestricted = true;
	
	public PLRuntime(){
		localRuntime.set(this);
		
		initialize();
	}
	
	public void addUuidMap(String fqName, Long uuid){
		uuidMap.put(fqName, uuid);
	}
	
	public PLModule resolveModule(String moduleName){
		return moduleMap.get(moduleName);
	}
	
	private static Random prng;
	static {
		 try {
			prng = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			prng = new Random();
		}
	}
	public Long getUuid(String fqName){
		if (!uuidMap.containsKey(fqName)){
			addUuidMap(fqName, prng.nextLong());
		}
		return uuidMap.get(fqName);
	}
	
	public void initialize(){
		final List<File> fList = new ArrayList<File>();
		final File f = new File("plang//");
		new DirectoryWalker<File>(){
			public void doIt(){
				try {
					walk(f, fList);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			protected boolean handleDirectory(File directory, int depth, Collection<File> results) {
				results.add(directory);
			    return true;
			}
		}.doIt();

		setSafeContext(true);
		setRestricted(false);
		
		addModule("System", SystemModule.class);
		
		for (String cn : SYSTEM_CLASSES.keySet()){
			registerClass("System", cn, SYSTEM_CLASSES.get(cn));	
		}
		
		for (File ffd : fList){
			for (File ff : ffd.listFiles(new FileFilter(){

				@Override
				public boolean accept(File pathname) {
					return pathname.getName().endsWith(".plang");
				}
				
			})){
				PLCompiler c = new PLCompiler();
				c.compile(new DiskFileDesignator(ff));
			}
		}
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
	
	public boolean isSafeContext(){
		return isSafeContext;
	}
	
	public void setSafeContext(boolean sc){
		isSafeContext = sc;
	}
	
	public PLangObject run(PLangObject runner, PLangObject... args){
	
		if (runner.getType() == PlangObjectType.FUNCTION){
			FunctionWrapper wrapper = (FunctionWrapper)runner;
			try {
				return wrapper.run(args);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else if (runner.getType() == PlangObjectType.CLASS){
			PLClass c = (PLClass)runner;
			PLangObject callableMethod = c.__getkey(Function.__applyMethod);
			if (c != null){
				return run(callableMethod, args);
			}
		}
		throw new RuntimeException(runner + " cannot be run!");
	}
	

	public PLangObject runJavaWrapper(Pointer runner, String mname, PLangObject... args){
		try {
			return runner.runMethod(mname, args);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
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
	
	public PLangObject wrapJavaObject(Object object){
		return new Pointer(object);
	}
	
	/*
	 * Only serializes the actual content, not class definitions
	 */
	public void serializeRuntimeContent(OutputStream os, long previousSerialization) throws Exception {
		serializedObjects.clear();
		
		JsonObject root = new JsonObject();
		JsonArray modules = new JsonArray();
		JsonArray uniqueIds = new JsonArray();
		
		for (String key : uuidMap.keySet()){
			Long value = uuidMap.get(key);
			uniqueIds.add(new JsonObject().add("fullyQualifiedName", key).add("serialVersionUID", value));
		}
		
		root.add("serialVersionUIDs", uniqueIds);
		
		for (String moduleName : moduleMap.keySet()){
			modules.add(new JsonObject().add("moduleName", moduleName)
					.add("module", moduleMap.get(moduleName).toObject(previousSerialization)));
		}
		
		root.add("modules", modules);
		os.write(root.toString(WriterConfig.PRETTY_PRINT).getBytes("utf-8"));
	}
}
