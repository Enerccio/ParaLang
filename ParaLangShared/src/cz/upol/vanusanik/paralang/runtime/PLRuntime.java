package cz.upol.vanusanik.paralang.runtime;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.net.ssl.SSLSocketFactory;

import javassist.ClassPool;
import javassist.CtClass;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;

import cz.upol.vanusanik.paralang.compiler.DiskFileDesignator;
import cz.upol.vanusanik.paralang.compiler.FileDesignator;
import cz.upol.vanusanik.paralang.compiler.PLCompiler;
import cz.upol.vanusanik.paralang.compiler.StringDesignator;
import cz.upol.vanusanik.paralang.connector.NetworkExecutionResult;
import cz.upol.vanusanik.paralang.connector.Node;
import cz.upol.vanusanik.paralang.connector.NodeList;
import cz.upol.vanusanik.paralang.connector.Protocol;
import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.Flt;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.plang.types.NoValue;
import cz.upol.vanusanik.paralang.plang.types.Pointer;
import cz.upol.vanusanik.paralang.plang.types.Pointer.PointerMethodIncompatibleException;
import cz.upol.vanusanik.paralang.plang.types.Str;
import cz.upol.vanusanik.paralang.utils.Utils;

public class PLRuntime {
	private static final ThreadLocal<PLRuntime> localRuntime = new ThreadLocal<PLRuntime>();
	private static final HashMap<String, Class<? extends PLClass>> __SYSTEM_CLASSES = new HashMap<String, Class<? extends PLClass>>();
	private static final HashMap<String, String> __SYSTEM_CLASSES_N = new HashMap<String, String>();
	public static final Map<String, Class<? extends PLClass>> SYSTEM_CLASSES;
	private static final Set<String> __BASE_TYPES = new HashSet<String>();
	public static final Set<String> BASE_TYPES;
	
	static {
		__SYSTEM_CLASSES.put("BaseClass", BaseClass.class);
		__SYSTEM_CLASSES_N.put("BaseClass", "BaseClass");
		__SYSTEM_CLASSES.put("BaseException", BaseException.class);
		__SYSTEM_CLASSES_N.put("BaseException", "BaseException");
		__SYSTEM_CLASSES.put("NetworkException", NetworkException.class);
		__SYSTEM_CLASSES_N.put("NetworkException", "NetworkException");
		__SYSTEM_CLASSES.put("Function", Function.class);
		__SYSTEM_CLASSES_N.put("Function", "Function");
		__SYSTEM_CLASSES.put("Integer", BaseInteger.class);
		__SYSTEM_CLASSES_N.put("BaseInteger", "Integer");
		__SYSTEM_CLASSES.put("Float", BaseFloat.class);
		__SYSTEM_CLASSES_N.put("BaseFloat", "Float");
		
		SYSTEM_CLASSES = Collections.synchronizedMap(Collections.unmodifiableMap(__SYSTEM_CLASSES));
		
		__BASE_TYPES.add("number");
		__BASE_TYPES.add("float");
		__BASE_TYPES.add("integer");
		__BASE_TYPES.add("string");
		__BASE_TYPES.add("boolean");
		__BASE_TYPES.add("null");
		__BASE_TYPES.add("func");
		__BASE_TYPES.add("ptr");
		__BASE_TYPES.add("mod");
		__BASE_TYPES.add("cls");
		
		BASE_TYPES = Collections.synchronizedSet(Collections.unmodifiableSet(__BASE_TYPES));
	}
	
	public static final PLRuntime getRuntime(){
		PLRuntime r = localRuntime.get();
		r.wasAccessed();
		return r;
	}
	
	private long objectIdCounter = 0;
	private final File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
	
	private Map<Long, BaseCompiledStub> instanceInternalMap = new HashMap<Long, BaseCompiledStub>();
	
	public long registerObject(BaseCompiledStub object){
		long id = objectIdCounter++;
		instanceInternalMap.put(id, object);
		return id;
	}

	private ParalangClassLoader classLoader = new ParalangClassLoader();
	private Map<String, Class<? extends PLModule>> preloadedModuleMap = new HashMap<String, Class<? extends PLModule>>();
	private Map<String, PLModule> moduleMap = new HashMap<String, PLModule>();
	private Map<String, Map<String, Class<?>>> classMap = new HashMap<String, Map<String,Class<?>>>();
	private Map<String, Long> uuidMap = new HashMap<String, Long>();
	
	private Map<String, Map<String, String>> classBytecodeData = new HashMap<String, Map<String, String>>();
	private Map<String, String> moduleBytecodeData = new HashMap<String, String>();
	
	private boolean isSafeContext = false;
	private boolean isRestricted = true;
	private String packageTarget = "";
	private PLCompiler compiler = new PLCompiler();
	
	public PLRuntime() throws Exception{		
		cp.appendSystemPath();
		initialize(false);
	}
	
	PLRuntime(boolean miniinit) throws Exception{
		cp.appendSystemPath();
		initialize(miniinit);
	}
	
	public static PLRuntime createEmptyRuntime() throws Exception{
		return new PLRuntime(true);
	}
	
	public synchronized void addClassBytedata(String moduleName, String className, byte[] bytedata){
		addClassBytedata(moduleName, className, Base64.encodeBase64String(bytedata));
	}
	
	public synchronized void addClassBytedata(String moduleName, String className, String bytedata){
		if (!classBytecodeData.containsKey(moduleName))
			classBytecodeData.put(moduleName, new HashMap<String, String>());
		classBytecodeData.get(moduleName).put(className, bytedata);
	}
	
	public synchronized void addModuleBytedata(String moduleName, byte[] bytedata){
		addModuleBytedata(moduleName, Base64.encodeBase64String(bytedata));
	}
	
	public synchronized void addModuleBytedata(String moduleName, String bytedata){
		moduleBytecodeData.put(moduleName, bytedata);
	}
	
	public void compileSource(FileDesignator fd) throws Exception{
		setAsCurrent();
		compiler.compile(fd);
	}
	
	public ClassLoader getClassLoader(){
		return classLoader;
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
	
	@SuppressWarnings("deprecation")
	public void initialize(boolean miniinit) throws Exception {
		setSafeContext(true);
		setRestricted(false);
		
		addModule("System", SystemModule.class);
		
		for (String cn : SYSTEM_CLASSES.keySet()){
			registerClass("System", cn, SYSTEM_CLASSES.get(cn));	
		}
		
		if (!miniinit){
			String path = "plang";
			if(jarFile.isFile()) {  
			    JarFile jar = new JarFile(jarFile);
			    final Enumeration<JarEntry> entries = jar.entries(); 
			    while(entries.hasMoreElements()) {
			    	JarEntry e = entries.nextElement();
			        String name = e.getName();
			        if (name.startsWith(path + "/")) { //filter according to the path
						InputStream is = jar.getInputStream(jar.getEntry(name));
						StringDesignator sd = new StringDesignator();
						sd.setSource(name.replace(path + "/", ""));
						sd.setClassDef(IOUtils.toString(is, "utf-8"));
						compileSource(sd);
			        }
			    }
				jar.close();
			} else { // Run with IDE
			    URL url = null;
				try {
					url = new File(Paths.get("").toFile().getAbsolutePath(), "plang").toURL();
				} catch (MalformedURLException e1) {
					e1.printStackTrace();
				}
			    if (url != null) {
			        try {
			            final File ffs = new File(url.toURI());
			            for (File ff : ffs.listFiles()) {
			            	compileSource(new DiskFileDesignator(ff));
			            }
			        } catch (URISyntaxException ex) {
			            // never happens
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
		if (__SYSTEM_CLASSES_N.containsKey(fqname)){
			return newInstance("System."+__SYSTEM_CLASSES_N.get(fqname), skipInit, inits);
		}
		String[] components = fqname.split("\\.");
		if (components.length != 2) 
			throw new RuntimeException("Malformed name of the class!");
		if (!classMap.containsKey(components[0])) 
			throw new RuntimeException("Unknown module!");
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
		return getModule(moduleName, false);
	}
	
	public PLModule getModule(String moduleName, boolean skipInit){
		if (!moduleMap.containsKey(moduleName)){
			if (!preloadedModuleMap.containsKey(moduleName))
				throw new RuntimeException("Module load failed: No such module " + moduleName);
			try {
				PLModule module = preloadedModuleMap.get(moduleName).newInstance();
				moduleMap.put(moduleName, module);
				if (!skipInit)
					run(moduleName, "init");
			} catch (Exception e) {
				if (e instanceof RuntimeException)
					throw (RuntimeException)e;
				throw new RuntimeException(e);
			}
		}
		return moduleMap.get(moduleName);
	}
	
	public PLangObject run(String module, String runnable, PLangObject... args) throws PLException{
		PLModule mod = getModule(module);
		
		try {
			return run(mod.___getkey(runnable), mod, args);
		} catch (RuntimeException e){
			throw new PLException(e);
		}
	}
	
	public void checkRestrictedAccess(BaseCompiledStub object){
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
			throw new NullPointerException("No such method!");
	
		if (runner.___getType() == PlangObjectType.FUNCTION){
			FunctionWrapper wrapper = (FunctionWrapper)runner;
			try {
				return wrapper.run(currentRunner, args);
			} catch (WrongMethodTypeException e){
				throw newInstance("System.BaseException", new Str("Wrong number of arguments."));
			} catch (RuntimeException e){
				throw e;
				//throw newInstance("System.BaseException", new Str("Error while invocating method: " + e.getMessage()));
			} catch (InvocationTargetException e){
				if (e.getCause() instanceof RuntimeException){
					throw (RuntimeException)e.getCause();
				} 
				throw new RuntimeException(e);
			} catch (Throwable e) {
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
	
	public PLangObject runJavaStaticMethod(String className, String mname, PLangObject... args){
		try {
			Class<?> clazz = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
			
			for (Method m : clazz.getMethods()){
				try {
					if (!Modifier.isStatic(m.getModifiers()))
						continue;
					
					Class<?> retType = m.getReturnType();
					Class<?>[] argTypes = m.getParameterTypes();
					List<Object> constructedArgs = new ArrayList<Object>();
					
					if (argTypes.length != args.length)
						continue;
					
					int it = 0;
					for (Class<?> aType : argTypes){
						if (aType.isAssignableFrom(args[it].getClass())){
							constructedArgs.add(args[it]);
						} else {
							constructedArgs.add(Utils.asJavaObject(aType, args[it]));
						}
						++it;
					}
					
					Object ret = m.invoke(null, constructedArgs.toArray());
					
					if (ret == null)
						return NoValue.NOVALUE;
					
					if (retType.isAssignableFrom(PLangObject.class))
						return (PLangObject) ret;
					
					return Utils.cast(ret, retType);
				} catch (PointerMethodIncompatibleException ce){
					continue;
				}
			}
		} catch (Exception e) {
			throw newInstance("System.BaseException", new Str("Failed to create new java instance: " + e.getMessage()));
		}		
		throw newInstance("System.BaseException", new Str("No method found for arguments " + args));
	}
	
	public Pointer createJavaWrapper(String className, PLangObject... args){
		try {
			Class<?> clazz = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
			
			for (Constructor<?> c : clazz.getDeclaredConstructors()){
				try {
					Class<?>[] argTypes = c.getParameterTypes();
					List<Object> constructedArgs = new ArrayList<Object>();
					
					if (argTypes.length != args.length)
						continue;
					
					int it = 0;
					for (Class<?> aType : argTypes){
						if (aType.isAssignableFrom(args[it].getClass())){
							constructedArgs.add(args[it]);
						} else {
							constructedArgs.add(Utils.asJavaObject(aType, args[it]));
						}
						++it;
					}
					
					Object o = c.newInstance(constructedArgs.toArray());
					return new Pointer(o);
				} catch (PointerMethodIncompatibleException ce){
					continue;
				}
			}
		} catch (Exception e) {
			throw newInstance("System.BaseException", new Str("Failed to create new java instance: " + e.getMessage()));
		}		
		throw newInstance("System.BaseException", new Str("No constructor found for arguments " + args));
	}

	public void setRestricted(boolean restricted) {
		isRestricted = restricted;
	}

	private ThreadLocal<HashSet<Object>> serializedObjects = new ThreadLocal<HashSet<Object>>();
	
	public void setAsAlreadySerialized(BaseCompiledStub baseCompiledStub) {
		if (serializedObjects.get() == null) return;
		serializedObjects.get().add(baseCompiledStub);
	}

	public boolean isAlreadySerialized(BaseCompiledStub baseCompiledStub) {
		if (serializedObjects.get() == null){
			serializedObjects.set(new HashSet<Object>());
		}
		return serializedObjects.get().contains(baseCompiledStub);
	}
	
	public PLangObject wrapJavaObject(Object object){
		return new Pointer(object);
	}
	
	public String getClassNameOrGuess(String fqName) {
		if (BASE_TYPES.contains(fqName))
			return fqName;
		
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
		if (className.equals("number") && (o instanceof Flt || o instanceof Int))
			return BooleanValue.TRUE;
		
		if (className.equals("integer") && o instanceof Int)
			return BooleanValue.TRUE;
		
		if (className.equals("float") && o instanceof Flt)
			return BooleanValue.TRUE;
		
		if (className.equals("string") && o instanceof Str)
			return BooleanValue.TRUE;
		
		if (className.equals("boolean") && o instanceof BooleanValue)
			return BooleanValue.TRUE;
		
		if (className.equals("null") && o instanceof NoValue)
			return BooleanValue.TRUE;
		
		if (className.equals("func") && o instanceof FunctionWrapper)
			return BooleanValue.TRUE;
		
		if (className.equals("ptr") && o instanceof Pointer)
			return BooleanValue.TRUE;
		
		if (className.equals("mod") && o instanceof PLModule)
			return BooleanValue.TRUE;
		
		if (className.equals("cls") && o instanceof PLClass)
			return BooleanValue.TRUE;
		
		if (o.getClass().getName().equals(className))
			return BooleanValue.TRUE;
		
		if (o instanceof PLClass){
			PLClass c = (PLClass)o;
			if (c.___fieldsAndMethods.containsKey(PLClass.___superKey))
				return checkInstanceOf(c.___getkey(PLClass.___superKey), className);
		}
		
		return BooleanValue.FALSE;
	}
	
	public PLangObject runDistributed(PLangObject tcounto, String methodName, PLangObject arg, BaseCompiledStub runner) {
		int tcount = 0;
		try {
			if (tcounto instanceof Int){
				tcount = (int) ((Int) tcounto).getValue();
			} else if (tcounto instanceof BaseCompiledStub){
				tcount = (int) ((Int) PLRuntime.getRuntime().run(((PLClass)tcounto).___getkey(BaseNumber.__toInt), (BaseCompiledStub)tcounto)).getValue();
			}
		} catch (Exception e){
			// do nothing, 0 will override error anyways
		}
		
		if (tcount<=0)
			throw newInstance("System.BaseException", new Str("Incorrect number of run count, expected positive integer"));
		
		PLClass c = newInstance("Collections.List");
		List<PLangObject> data = ((Pointer) c.___fieldsAndMethods.get("wrappedList")).getPointer();
		
		NetworkExecutionResult r = executeDistributed(runner.___getObjectId(), runner, methodName, tcount, arg);
		if (r.hasExceptions()){
			NetworkException e = (NetworkException) newInstance("System.NetworkException", new Str("Failed distributed network call because of remote exception(s)"));
			for (PLangObject o : r.exceptions)
				if (o != null)
					data.add(o);
				else
					data.add(NoValue.NOVALUE);
			e.___setkey(NetworkException.listKey, c);
			throw e;
		} else {
			for (PLangObject o : r.results)
				data.add(o);
		}
		
		return c;
	}

	private NetworkExecutionResult executeDistributed(final long ___getObjectId, final BaseCompiledStub caller, 
			final String methodName, int tcount, final PLangObject arg) {
		
		List<Thread> tList = new ArrayList<Thread>();
		final NetworkExecutionResult result = new NetworkExecutionResult();
		result.results = new PLangObject[tcount];
		result.exceptions = new PLangObject[tcount];
		final JsonObject serializedRuntimeContent = serializeRuntimeContent(caller, arg);
		
		List<Node> nnodes;
		try {
			nnodes = NodeList.getBestLoadNodes(tcount);
		} catch (Exception e){
			throw PLRuntime.getRuntime().newInstance("System.BaseException", new Str("No hosts available"));
		}
		
		final List<Node> nodes = nnodes;
		
		for (int i=0; i<tcount; i++){
			final int tId = i;
			tList.add(new Thread(new Runnable(){

				@Override
				public void run() {
					handleDistributedCall(tId, result, getOrFail(nodes, tId), ___getObjectId, methodName,
							serializedRuntimeContent, (arg instanceof BaseCompiledStub) ? ((BaseCompiledStub) arg).___getObjectId() : -1);
				}

				private Node getOrFail(List<Node> nodes, int tId) {
					if (tId < nodes.size())
						return nodes.get(tId);
					return null;
				}
				
			}));
		}
		
		for (Thread t : tList)
			t.start();
		
		for (Thread t : tList)
			try {
				t.join();
			} catch (InterruptedException e) {
				
			}
		
		return result;
	}

	protected void handleDistributedCall(int tId, NetworkExecutionResult result, Node node, long oid, 
			String methodName, JsonObject serializedRuntimeContent, long arg) {
		boolean executed = false;
		Socket s = null;
		
		do {
			try {				
				s = SSLSocketFactory.getDefault().createSocket(node.getAddress(), node.getPort());
				Protocol.send(s.getOutputStream(), new JsonObject().add("header", Protocol.RESERVE_SPOT_REQUEST));
				JsonObject response = Protocol.receive(s.getInputStream());
				
				if (!response.getString("header", "").equals(Protocol.RESERVE_SPOT_RESPONSE))
					continue; // bad chain reply
				
				if (!response.get("payload").asObject().getBoolean("result", false)){
					node = NodeList.getRandomNode(); 
					continue; // no reserved thread for me :(
				}
				
				setAsCurrent();
				
				JsonObject payload = new JsonObject().add("header", Protocol.RUN_CODE)
		     		    .add("payload", new JsonObject()
		     		    	.add("lastModificationTime", System.currentTimeMillis())
		     		    	.add("runtimeFiles", buildRuntimeFiles())
		     		    	.add("runtimeData", serializedRuntimeContent)
		     		    	.add("runnerId", oid)
		     		    	.add("argId", arg)
		     		    	.add("id", tId)
		     		    	.add("methodName", methodName));
				Protocol.send(s.getOutputStream(), payload);
				
				response = Protocol.receive(s.getInputStream());
				
				if (!response.getString("header", "").equals(Protocol.RETURNED_EXECUTION))
					continue; // bad chain reply
				
				payload = response.get("payload").asObject();
				JsonObject data;
				if (payload.getBoolean("hasResult", false)){
					data = payload.get("result").asObject();
				} else {
					data = payload.get("exception").asObject();
				}
				
				Map<Long, Long> imap = new HashMap<Long, Long>();
				buildInstanceMap(data, imap);
				
				PLangObject deserialized = deserialize(data, imap);
				
				if (payload.getBoolean("hasResult", false)){
					result.results[tId] = deserialized;
				} else {
					result.exceptions[tId] = deserialized;
				}
				
				executed = true;
			} catch (Exception e){
				node = NodeList.getRandomNode(); // Refresh node since error might have been node related
				executed = false;
			} finally {
				if (s != null){
					try {
						s.close();
					} catch (Exception e){
						// ignore
					}	
				}
			}
		} while (!executed);
	}

	private JsonArray buildRuntimeFiles() {
		JsonArray a = new JsonArray();
		
		for (String module : moduleBytecodeData.keySet()){
			if (module.equals("System")) continue;
			
			JsonObject m = new JsonObject();
			m.add("name", module);
			m.add("content", moduleBytecodeData.get(module));
			m.add("type", "module");
			a.add(m);
		}
		
		for (String moduleName : classBytecodeData.keySet()){
			if (moduleName.equals("System")) continue;
			
			for (String className  : classBytecodeData.get(moduleName).keySet()){
				JsonObject c = new JsonObject();
				c.add("name", className);
				c.add("content", classBytecodeData.get(moduleName).get(className));
				c.add("type", "class");
				c.add("module", moduleName);
				a.add(c);
			}
		}
		
		return a;
	}

	public interface RuntimeAccessListener {
		public void wasAccessed();
	}
	
	private RuntimeAccessListener runtimeAccessListener;
	
	private void wasAccessed() {
		if (runtimeAccessListener != null){
			runtimeAccessListener.wasAccessed();
		}
	}

	
	/*
	 * Only serializes the actual content, not class definitions
	 */
	public void serializeRuntimeContent(OutputStream os, BaseCompiledStub currentCaller, PLangObject arg) throws Exception {
		JsonObject root = serializeRuntimeContent(currentCaller, arg);
		os.write(root.toString(WriterConfig.PRETTY_PRINT).getBytes("utf-8"));
	}

	private JsonObject serializeRuntimeContent(BaseCompiledStub currentCaller, PLangObject arg) {
		serializedObjects.set(new HashSet<Object>());
		
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
					.add("module", moduleMap.get(moduleName).___toObject()));
		}
		
		root.add("modules", modules);
		root.add("currentCaller", currentCaller.___toObject());
		root.add("callerArg", arg.___toObject());
		
		serializedObjects.get().clear();
		return root;
	}
	
	public void prepareForDeserialization(JsonArray uuids){
		for (JsonValue v : uuids){
			JsonObject o = v.asObject();
			String name = o.getString("fullyQualifiedName", "");
			long id = o.getLong("serialVersionUID", 0);
			
			uuidMap.put(name, id);
		}
	}
	
	public static class DeserializationResult {
		public Map<Long, Long> ridxMap;
		public PLangObject cobject;
	}
	public DeserializationResult deserialize(JsonArray modules, JsonObject caller, JsonObject arg) throws Exception {
		DeserializationResult r = new DeserializationResult();
		r.ridxMap = new HashMap<Long, Long>();
		for (JsonValue v : modules){
			buildInstanceMap(v.asObject().get("module").asObject(), r.ridxMap);
		}
		buildInstanceMap(caller, r.ridxMap);
		buildInstanceMap(arg, r.ridxMap);
		
		for (JsonValue v : modules){
			deserialize(v.asObject().get("module").asObject(), r.ridxMap);
		}
		deserialize(caller, r.ridxMap);
		r.cobject = deserialize(arg, r.ridxMap);
		
		return r;
	}

	public PLangObject deserialize(JsonObject o, Map<Long, Long> ridxMap) throws Exception {
		PlangObjectType t = PlangObjectType.valueOf(o.getString("metaObjectType", ""));
		
		switch (t){
		case BOOLEAN:
			return BooleanValue.fromBoolean(o.getBoolean("value", false));
		case MODULE:
		case CLASS:
			if (o.getBoolean("link", false)){
				return instanceInternalMap.get(ridxMap.get(o.getLong("linkId", 0)));
			} else {
				BaseCompiledStub stub = instanceInternalMap.get(ridxMap.get(o.getLong("thisLink", 0)));
				JsonArray fields = o.get("fields").asArray();
				for (JsonValue v : fields){
					JsonObject field = v.asObject();
					PLangObject fieldValue = deserialize(field.get("fieldValue").asObject(), ridxMap);
					if (fieldValue == null)
						return null;
					stub.___fieldsAndMethods.put(field.getString("fieldName", ""), fieldValue);
				}
				return stub;
			}
		case FLOAT:
			return new Flt(o.getFloat("value", 0f));
		case FUNCTION:
			JsonObject val = o.get("value").asObject();
			return new FunctionWrapper(val.getString("methodName", ""), (BaseCompiledStub) deserialize(val.get("owner").asObject(), ridxMap), 
					val.getBoolean("isClassMethod", false));
		case INTEGER:
			return new Int(o.getLong("value", 0));
		case JAVAOBJECT:
			String encoded = o.getString("value", "");
			byte[] decoded = Base64.decodeBase64(encoded);
			ByteArrayInputStream is = new ByteArrayInputStream(decoded);
			ObjectInputStream serstream = new ObjectInputStream(is){
				protected Class<?> resolveClass(ObjectStreamClass desc)
				        throws IOException, ClassNotFoundException
			    {
			        String name = desc.getName();
			        try {
			            return Class.forName(name, false, classLoader);
			        } catch (ClassNotFoundException ex) {
			        	return super.resolveClass(desc);
			        }
			    }
			};
			Object value = serstream.readObject();
			return new Pointer(value);
		case NOVALUE:
			return NoValue.NOVALUE;
		case STRING:
			return new Str(o.getString("value", ""));
		default:
			break;
		
		}
		
		
		return null;
	}

	public void buildInstanceMap(JsonObject o, Map<Long, Long> ridxMap) {
		PlangObjectType t = PlangObjectType.valueOf(o.getString("metaObjectType", ""));
		
		if (t == PlangObjectType.MODULE || t == PlangObjectType.CLASS){
			if (!o.getBoolean("isBaseClass", false) && !o.getBoolean("link", false)){
				long lid = o.getLong("thisLink", 0);
				BaseCompiledStub stub;
				
				if (t == PlangObjectType.CLASS)
					stub = newInstance(o.getString("className", "").replace("$", "."), true);
				else
					stub = getModule(o.getString("className", ""), true);
				stub.___isInited = o.getBoolean("isInited", false);
				if (stub.___isInited){
					stub.___fieldsAndMethods = new HashMap<String, PLangObject>();
				}
				
				if (ridxMap != null){
					ridxMap.put(lid, stub.___objectId);
				}
				
				JsonArray fields = o.get("fields").asArray();
				for (JsonValue v : fields){
					JsonObject field = v.asObject();
					buildInstanceMap(field.get("fieldValue").asObject(), ridxMap);
				}
			}
		}
	}

	public String getPackageTarget() {
		return packageTarget;
	}

	public void setPackageTarget(String packageTarget) {
		this.packageTarget = packageTarget;
	}
	
	public void setAsCurrent(){
		localRuntime.set(this);
		Thread.currentThread().setContextClassLoader(classLoader);
	}

	public RuntimeAccessListener getRuntimeAccessListener() {
		return runtimeAccessListener;
	}

	public void setRuntimeAccessListener(RuntimeAccessListener runtimeAccessListener) {
		this.runtimeAccessListener = runtimeAccessListener;
	}

	public PLangObject runByObjectId(long oid, String methodName, Int arg0, PLangObject arg, long argId) {
		return run(instanceInternalMap.get(oid).___getkey(methodName), instanceInternalMap.get(oid), arg0, argId < 0 ? arg : instanceInternalMap.get(argId));
	}

	
	private ClassPool cp = new ClassPool();
	@SuppressWarnings("unchecked")
	public void loadBytecode(String mname, String bytecontent) throws Exception {
		String clsName = mname;
		Class<?> cls = loadClass(clsName, bytecontent);
		addModule(mname, (Class<? extends PLModule>) cls);
		addModuleBytedata(mname, bytecontent);
	}

	public void loadBytecode(String cname, String mname, String bytecontent) throws Exception {
		String clsName = mname + "$" + cname;
		Class<?> cls = loadClass(clsName, bytecontent);
		registerClass(mname, cname, cls);
		addClassBytedata(mname, cname, bytecontent);
	}

	private Class<?> loadClass(String clsName, String bytecontent) throws Exception {
		CtClass cls = cp.makeClass(new ByteArrayInputStream(Base64.decodeBase64(bytecontent)));
		return cls.toClass(classLoader, null);
	}
}
