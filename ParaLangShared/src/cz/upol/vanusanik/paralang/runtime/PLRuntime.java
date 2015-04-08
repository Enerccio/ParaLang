package cz.upol.vanusanik.paralang.runtime;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
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
import cz.upol.vanusanik.paralang.plang.types.Str;
import cz.upol.vanusanik.paralang.runtime.wrappers.PLangList;

public class PLRuntime {
	private static final ThreadLocal<PLRuntime> localRuntime = new ThreadLocal<PLRuntime>();
	private static final HashMap<String, Class<? extends PLClass>> __SYSTEM_CLASSES = new HashMap<String, Class<? extends PLClass>>();
	private static final HashMap<String, String> __SYSTEM_CLASSES_N = new HashMap<String, String>();
	public static final Map<String, Class<? extends PLClass>> SYSTEM_CLASSES = Collections.synchronizedMap(Collections.unmodifiableMap(__SYSTEM_CLASSES));
	private final File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
	
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
	}
	
	public static final PLRuntime getRuntime(){
		PLRuntime r = localRuntime.get();
		r.wasAccessed();
		return r;
	}
	
	private long objectIdCounter = 0;
	
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
	private Map<String, String> moduleSourceMap = new HashMap<String, String>();
	
	private boolean isSafeContext = false;
	private boolean isRestricted = true;
	private String packageTarget = "";
	private PLCompiler compiler = new PLCompiler();
	
	public PLRuntime(){		
		initialize(false);
	}
	
	PLRuntime(boolean miniinit){		
		initialize(miniinit);
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
	public void initialize(boolean miniinit){
		setSafeContext(true);
		setRestricted(false);
		
		addModule("System", SystemModule.class);
		
		for (String cn : SYSTEM_CLASSES.keySet()){
			registerClass("System", cn, SYSTEM_CLASSES.get(cn));	
		}
		
		{
			String path = "plang";
			if(jarFile.isFile()) {  
			    JarFile jar = null;
				try {
					jar = new JarFile(jarFile);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			    final Enumeration<JarEntry> entries = jar.entries(); 
			    while(entries.hasMoreElements()) {
			    	JarEntry e = entries.nextElement();
			        String name = e.getName();
			        if (name.startsWith(path + "/")) { //filter according to the path
			            try {
							InputStream is = jar.getInputStream(jar.getEntry(name));
							StringDesignator sd = new StringDesignator();
							sd.setSource(name.replace(path + "/", ""));
							sd.setClassDef(IOUtils.toString(is, "utf-8"));
							compileSource(sd);
						} catch (Exception e1) {
							e1.printStackTrace();
						}
			        }
			    }
			    try {
					jar.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
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
			            	try {
								compileSource(new DiskFileDesignator(ff));
							} catch (Exception e) {
								e.printStackTrace();
							}
			            }
			        } catch (URISyntaxException ex) {
			            // never happens
			        }
			    }
			}
		}
		
		moduleSourceMap.clear();
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
		List<PLangObject> data = ((PLangList)((Pointer) c.___fieldsAndMethods.get("wrappedList")).getPointer()).___innerList();
		
		NetworkExecutionResult r = executeDistributed(runner.___getObjectId(), methodName, tcount);
		if (r.hasExceptions()){
			NetworkException e = (NetworkException) newInstance("System.NetworkException", new Str("Failed distributed network call because of remote exception(s)"));
			for (PLangObject o : r.exceptions)
				data.add(o);
			e.___setkey(NetworkException.listKey, c);
			throw e;
		} else {
			for (PLangObject o : r.results)
				data.add(o);
		}
		
		return c;
	}

	private NetworkExecutionResult executeDistributed(final long ___getObjectId,
			final String methodName, int tcount) {
		
		List<Thread> tList = new ArrayList<Thread>();
		final NetworkExecutionResult result = new NetworkExecutionResult();
		result.results = new PLangObject[tcount];
		result.exceptions = new PLangObject[tcount];
		final JsonObject serializedRuntimeContent = serializeRuntimeContent();
		
		final List<Node> nodes = NodeList.getBestLoadNodes(tcount);
		
		for (int i=0; i<tcount; i++){
			final int tId = i;
			tList.add(new Thread(new Runnable(){

				@Override
				public void run() {
					handleDistributedCall(tId, result, nodes.get(tId), ___getObjectId, methodName, serializedRuntimeContent);
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

	protected void handleDistributedCall(int tId, NetworkExecutionResult result, Node node, long oid, String methodName, JsonObject serializedRuntimeContent) {
		boolean executed = false;
		Socket s = null;
		
		do {
			try {				
				s = new Socket(node.getAddress(), node.getPort());
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
				e.printStackTrace();
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

	private JsonObject buildRuntimeFiles() {
		JsonObject o = new JsonObject();
		
		for (String module : moduleSourceMap.keySet()){
			o.add(module, moduleSourceMap.get(module));
		}
		
		return o;
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
	public void serializeRuntimeContent(OutputStream os) throws Exception {
		JsonObject root = serializeRuntimeContent();
		os.write(root.toString(WriterConfig.PRETTY_PRINT).getBytes("utf-8"));
	}

	private JsonObject serializeRuntimeContent() {
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
					.add("module", moduleMap.get(moduleName).___toObject()));
		}
		
		root.add("modules", modules);
		
		serializedObjects.clear();
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
	
	public void deserialize(JsonArray modules) throws Exception {
		Map<Long, Long> ridxMap = new HashMap<Long, Long>();
		for (JsonValue v : modules){
			buildInstanceMap(v.asObject().get("module").asObject(), ridxMap);
		}
		
		for (JsonValue v : modules){
			deserialize(v.asObject().get("module").asObject(), ridxMap);
		}
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
			return new Int(o.getInt("value", 0));
		case JAVAOBJECT:
			String encoded = o.getString("value", "");
			byte[] decoded = Base64.decodeBase64(encoded);
			ByteArrayInputStream is = new ByteArrayInputStream(decoded);
			ObjectInputStream serstream = new ObjectInputStream(is);
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
	}

	public RuntimeAccessListener getRuntimeAccessListener() {
		return runtimeAccessListener;
	}

	public void setRuntimeAccessListener(RuntimeAccessListener runtimeAccessListener) {
		this.runtimeAccessListener = runtimeAccessListener;
	}

	public PLangObject runByObjectId(long oid, String methodName, Int arg0) {
		return run(instanceInternalMap.get(oid).___getkey(methodName), instanceInternalMap.get(oid), arg0);
	}
}
