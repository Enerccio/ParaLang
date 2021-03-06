package cz.upol.vanusanik.paralang.plang.types;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.plang.PrimitivePLangObject;
import cz.upol.vanusanik.paralang.runtime.BaseCompiledStub;
import cz.upol.vanusanik.paralang.runtime.PLClass;
import cz.upol.vanusanik.paralang.runtime.PLRuntime;
import cz.upol.vanusanik.paralang.utils.Utils;

/**
 * FunctionWrapper, ie a function/method accessor object. This object is used
 * when method is called in PLang.
 * 
 * @author Enerccio
 *
 */
public class FunctionWrapper extends PrimitivePLangObject implements
		Serializable {

	private static final long serialVersionUID = 3998164784189902299L;

	public FunctionWrapper() {

	}

	public FunctionWrapper(String mName, BaseCompiledStub stub, boolean ism) {
		methodName = mName;
		owner = stub;
		isMethod = ism;
	}

	/** Whether this is method or a function */
	private boolean isMethod;
	/** Java method name */
	private String methodName;
	/** Bound object */
	private BaseCompiledStub owner;

	@Override
	public BaseCompiledStub ___getLowestClassdef() {
		BaseCompiledStub lowest = owner;
		BaseCompiledStub nextLowest = null;
		do {
			nextLowest = lowest.___getLowestClassInstance();
			if (nextLowest != null)
				lowest = nextLowest;
		} while (nextLowest != null);
		return lowest;
	}

	/**
	 * MethodAccessor helper class
	 * 
	 * @author Enerccio
	 *
	 */
	private class MethodAccessor {
		public Method m;
		public BaseCompiledStub o;
	}

	/**
	 * All method accessors are cached here.
	 */
	private transient WeakHashMap<BaseCompiledStub, MethodAccessor> ___accessorCache = new WeakHashMap<BaseCompiledStub, MethodAccessor>();

	/**
	 * Runs the code with arguments
	 * 
	 * @param owner
	 * @param arguments
	 * @return
	 * @throws Throwable
	 */
	public PLangObject ___run(PLangObject owner, PLangObject... arguments)
			throws Throwable {
		if (owner instanceof FunctionWrapper){
			BaseCompiledStub rebase = ((FunctionWrapper) owner).___getOwner();
			while (rebase.___fieldsAndMethods.containsKey(PLClass.___derivedKey))
				rebase = (BaseCompiledStub) rebase.___fieldsAndMethods.get(PLClass.___derivedKey);
			owner = rebase;
		}
		MethodAccessor ma = ___accessorCache.get(owner);
		if (ma == null) {
			for (MethodAccessor maa : getAllMethods((BaseCompiledStub) owner)) {
				if (maa.m.getName().equals(methodName)) {
					ma = maa;
					___accessorCache.put((BaseCompiledStub) owner, ma);
					break;
				}
			}
			if (ma == null)
				throw new RuntimeException("Unknown method: " + methodName);
		}
		return ___run(ma, (BaseCompiledStub) owner, arguments);
	}

	/**
	 * Returns all the methods of that owner.
	 * 
	 * @param owner
	 * @return
	 */
	private List<MethodAccessor> getAllMethods(BaseCompiledStub owner) {
		List<MethodAccessor> mList = new ArrayList<MethodAccessor>();

		do {
			for (Method m : owner.getClass().getMethods()) {
				MethodAccessor ma = new MethodAccessor();
				ma.m = m;
				ma.o = owner;
				mList.add(ma);
			}
			owner = owner.___getParent();
		} while (owner != null);

		return mList;
	}

	/** Method handle cache */
	private static ThreadLocal<Map<Method, MethodHandle>> methodHandles = new ThreadLocal<Map<Method, MethodHandle>>(){

		@Override
		protected Map<Method, MethodHandle> initialValue() {
			return new WeakHashMap<Method, MethodHandle>();
		}
		
	};
	private static ThreadLocal<Map<BaseCompiledStub, Map<MethodHandle, MethodHandle>>> boundHandles =  
			new ThreadLocal<Map<BaseCompiledStub, Map<MethodHandle, MethodHandle>>>(){

		@Override
		protected Map<BaseCompiledStub, Map<MethodHandle, MethodHandle>> initialValue() {
			return new WeakHashMap<BaseCompiledStub, Map<MethodHandle, MethodHandle>>();
		}
		
	};

	/**
	 * Runs the method accessor with the self and arguments
	 * 
	 * @param ma
	 * @param self
	 * @param arguments
	 * @return
	 * @throws Throwable
	 */
	private PLangObject ___run(MethodAccessor ma, BaseCompiledStub self,
			PLangObject[] arguments) throws Throwable {
		MethodHandle genHandle = methodHandles.get().get(ma.m);

		if (genHandle == null) {
			genHandle = MethodHandles.lookup().unreflect(ma.m);
			methodHandles.get().put(ma.m, genHandle);
		}
		
		if (!boundHandles.get().containsKey(ma.o)){
			boundHandles.get().put(ma.o, new WeakHashMap<MethodHandle, MethodHandle>());
		}
		
		MethodHandle handle = boundHandles.get().get(ma.o).get(genHandle);
		if (handle == null){
			handle = genHandle.bindTo(ma.o);
			boundHandles.get().get(ma.o).put(genHandle, handle);
		}

		PLangObject[] data = arguments;
		if (isMethod) {
			data = Utils.pushLeft(self, arguments);
		}
		
		switch (data.length){
		case 0:	return (PLangObject) handle.invokeExact();
		case 1:	return (PLangObject) handle.invokeExact(data[0]);
		case 2: return (PLangObject) handle.invokeExact(data[0], data[1]);
		case 3: return (PLangObject) handle.invokeExact(data[0], data[1], data[2]);
		case 4: return (PLangObject) handle.invokeExact(data[0], data[1], data[2], data[3]);
		case 5: return (PLangObject) handle.invokeExact(data[0], data[1], data[2], data[3], data[4]); 
		case 6: return (PLangObject) handle.invokeExact(data[0], data[1], data[2], data[3], data[4], data[5]);
		case 7: return (PLangObject) handle.invokeExact(data[0], data[1], data[2], data[3], data[4], data[5], data[6]);
		case 8: return (PLangObject) handle.invokeExact(data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7]);
		case 9: return (PLangObject) handle.invokeExact(data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8]);
		}
		
		return (PLangObject) handle.invokeWithArguments((Object[]) data);
	}

	public boolean ___isMethod() {
		return isMethod;
	}

	public void ___setMethod(boolean isMethod) {
		this.isMethod = isMethod;
	}

	public String ___getMethodName() {
		return methodName;
	}

	public void ___setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public BaseCompiledStub ___getOwner() {
		return owner;
	}

	public void ___setOwner(BaseCompiledStub owner) {
		this.owner = owner;
	}

	@Override
	public PlangObjectType ___getType() {
		return PlangObjectType.FUNCTION;
	}

	@Override
	public String toString() {
		return "Function Wrapper of method " + methodName + " of object "
				+ owner;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((methodName == null) ? 0 : methodName.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FunctionWrapper other = (FunctionWrapper) obj;
		if (methodName == null) {
			if (other.methodName != null)
				return false;
		} else if (!methodName.equals(other.methodName))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		return true;
	}

	@Override
	public JsonValue ___toObject(Set<Long> alreadySerialized, boolean serializeFully) {
		return new JsonObject().add("metaObjectType", ___getType().toString())
				.add("value",
						new JsonObject().add("methodName", methodName)
								.add("owner", owner.___toObject(alreadySerialized, serializeFully))
								.add("isClassMethod", isMethod));
	}

	@Override
	public boolean ___isNumber() {
		return false;
	}

	@Override
	public Float ___getNumber(PLangObject self) {
		throw PLRuntime.getRuntime().newInstance("System.BaseException", new Str("Function cannot be used as number"));
	}

	@Override
	public boolean ___eq(PLangObject self, PLangObject b) {
		return this == b;
	}

	/**
	 * Needs to provide own implementation of accessorCache since it is not
	 * serialized
	 * 
	 * @param in
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		in.defaultReadObject();

		___accessorCache = new WeakHashMap<BaseCompiledStub, MethodAccessor>();
	}
}
