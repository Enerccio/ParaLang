package cz.upol.vanusanik.paralang.runtime;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
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
import cz.upol.vanusanik.paralang.compiler.FileDesignator;
import cz.upol.vanusanik.paralang.compiler.PLCompiler;
import cz.upol.vanusanik.paralang.connector.NetworkProvider;
import cz.upol.vanusanik.paralang.connector.NetworkResult;
import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.Pointer;
import cz.upol.vanusanik.paralang.plang.types.Str;

public class PLRuntime {
	private static final ThreadLocal<PLRuntime> localRuntime = new ThreadLocal<PLRuntime>();
	private static final HashMap<String, Class<? extends PLClass>> __SYSTEM_CLASSES = new HashMap<String, Class<? extends PLClass>>();
	public static final Map<String, Class<? extends PLClass>> SYSTEM_CLASSES = Collections.synchronizedMap(Collections.unmodifiableMap(__SYSTEM_CLASSES));
	
	static {
		__SYSTEM_CLASSES.put("BaseClass", BaseClass.class);
		__SYSTEM_CLASSES.put("BaseException", BaseException.class);
		__SYSTEM_CLASSES.put("NetworkException", NetworkException.class);
		__SYSTEM_CLASSES.put("Function", Function.class);
		__SYSTEM_CLASSES.put("Integer", BaseInteger.class);
		__SYSTEM_CLASSES.put("Float", BaseFloat.class);
	}
	
	public static final PLRuntime getRuntime(){
		return localRuntime.get();
	}
	
	private long objectIdCounter = 0;
	
	public long registerObject(BaseCompiledStub object){
		return objectIdCounter++;
	}

	private ParalangClassLoader classLoader = new ParalangClassLoader();
	private Map<String, Class<? extends PLModule>> preloadedModuleMap = new HashMap<String, Class<? extends PLModule>>();
	private Map<String, PLModule> moduleMap = new HashMap<String, PLModule>();
	private Map<String, Map<String, Class<?>>> classMap = new HashMap<String, Map<String,Class<?>>>();
	private Map<String, Long> uuidMap = new HashMap<String, Long>();
	private Map<String, String> moduleSourceMap = new HashMap<String, String>();
	
	private boolean isSafeContext = false;
	private boolean isRestricted = true;
	private String packageTarget = "";
	private NetworkProvider np;
	private PLCompiler compiler = new PLCompiler();
	
	public PLRuntime(){		
		initialize(false);
		compiler.setClassLoader(classLoader);
	}
	
	PLRuntime(boolean miniinit){		
		initialize(miniinit);
		compiler.setClassLoader(classLoader);
	}
	
	public static PLRuntime createEmptyRuntime(){
		return new PLRuntime(true);
	}
	
	public void compileSource(FileDesignator fd) throws Exception{
		setAsCurrent();
		String moduleName = compiler.compile(fd);
		moduleSourceMap.put(moduleName, fd.getSourceContent());
	}
	
	public ClassLoader getClassLoader(){
		return classLoader;
	}
	
	public void setNetworkProvider(NetworkProvider np){
		this.np = np;
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
	
	public void initialize(boolean miniinit){
		setSafeContext(true);
		setRestricted(false);
		
		addModule("System", SystemModule.class);
		
		for (String cn : SYSTEM_CLASSES.keySet()){
			registerClass("System", cn, SYSTEM_CLASSES.get(cn));	
		}
		
		if (!miniinit){
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
			
			for (File ffd : fList){
				for (File ff : ffd.listFiles(new FileFilter(){

					@Override
					public boolean accept(File pathname) {
						return pathname.getName().endsWith(".plang");
					}
					
				})){
					try {
						compileSource(new DiskFileDesignator(ff));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}	
		}
		
		setSafeContext(false);
	}
	
	public void registerClass(String module, String className, Class<?> cls){
		if (!classMap.containsKey(module))
			classMap.put(module, new HashMap<String, Class<?>>());
		classMap.get(module).put(className, cls);
	}
	
	public PLClass newInstance(String fqname, PLangObject... inits){
		return newInstance(fqname, false, inits);
	}
	
	public PLClass newInstance(String fqname, boolean skipInit, PLangObject... inits){
		String[] components = fqname.split("\\.");
		if (components.length != 2) throw new RuntimeException("Malformed name of the class!");
		if (!classMap.containsKey(components[0])) throw new RuntimeException("Unknown module!");
		if (!classMap.get(components[0]).containsKey(components[1])) throw new RuntimeException("Unknown class!");
		try {
			PLClass instance = (PLClass) classMap.get(components[0]).get(components[1]).newInstance();
			if (!skipInit)
				run(instance.___getkey("init"), instance, inits); // run constructor
			return instance;
		} catch (Throwable e) {
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
				if (e instanceof RuntimeException)
					throw (RuntimeException)e;
				throw new RuntimeException(e);
			}
		}
		return moduleMap.get(moduleName);
	}
	
	public PLangObject run(String module, String runnable) throws PLException{
		PLModule mod = getModule(module);
		
		try {
			return run(mod.___getkey(runnable), mod);
		} catch (RuntimeException e){
			throw new PLException(e);
		}
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
	
	public PLangObject run(PLangObject runner, BaseCompiledStub currentRunner, PLangObject... args){
		if (runner == null)
			throw new NullPointerException("Runner is empty");
	
		if (runner.___getType() == PlangObjectType.FUNCTION){
			FunctionWrapper wrapper = (FunctionWrapper)runner;
			try {
				return wrapper.run(currentRunner, args);
			} catch (RuntimeException e){
				throw e;
			} catch (InvocationTargetException e){
				if (e.getCause() instanceof RuntimeException){
					throw (RuntimeException)e.getCause();
				} 
				throw new RuntimeException(e);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else if (runner.___getType() == PlangObjectType.CLASS){
			PLClass c = (PLClass)runner;
			PLangObject callableMethod = c.___getkey(Function.__applyMethod);
			if (callableMethod != null){
				return run(callableMethod, c, args);
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
	
	public String getClassNameOrGuess(String fqName) {
		String[] components = fqName.split("\\.");
		if (components.length != 2) throw new RuntimeException("Malformed name of the class!");
		if (!classMap.containsKey(components[0]) || !classMap.get(components[0]).containsKey(components[1])){
			return getPackageTarget() + components[0] + "$" + components[1];
		}
		
		return classMap.get(components[0]).get(components[1]).getCanonicalName();
	}
	
	public boolean checkExceptionHierarchy(PLangObject o, String className){
		if (o.getClass().getName().equals(className))
			return true;
		
		if (o instanceof PLClass){
			PLClass c = (PLClass)o;
			if (c.___fieldsAndMethods.containsKey(PLClass.___superKey))
				return checkExceptionHierarchy(c.___getkey(PLClass.___superKey), className);
		}
		
		return false;
	}
	
	public PLangObject checkInstanceOf(PLangObject o, String className){
		if (o.getClass().getName().equals(className))
			return BooleanValue.TRUE;
		
		if (o instanceof PLClass){
			PLClass c = (PLClass)o;
			if (c.___fieldsAndMethods.containsKey(PLClass.___superKey))
				return checkInstanceOf(c.___getkey(PLClass.___superKey), className);
		}
		
		return BooleanValue.FALSE;
	}
	
	public PLangObject runDistributed(int tcount, String methodName, BaseCompiledStub runner) {
		if (tcount<=0)
			throw newInstance("System.BaseException", new Str("Incorrect number of run count, expected positive integer"));
		
		PLClass c = newInstance("Collections.List");
		
		NetworkResult r = np.getConnector().executeDistributed(runner.___getObjectId(), methodName, tcount);
		if (!r.success)
			throw r.exception;
		else {
			List<PLangObject> data = ((Pointer) c.___fieldsAndMethods.get("wrappedList")).getPointer();
			for (PLangObject o : r.results)
				data.add(o);
		}
		
		return c;
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
					.add("module", moduleMap.get(moduleName).___toObject(previousSerialization)));
		}
		
		root.add("modules", modules);
		os.write(root.toString(WriterConfig.PRETTY_PRINT).getBytes("utf-8"));
	}

	public String getPackageTarget() {
		return packageTarget;
	}

	public void setPackageTarget(String packageTarget) {
		this.packageTarget = packageTarget;
	}
	
	public void setAsCurrent(){
		localRuntime.set(this);
	}
}
