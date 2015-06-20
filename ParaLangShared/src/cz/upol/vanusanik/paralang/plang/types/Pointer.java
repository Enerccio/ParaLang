package cz.upol.vanusanik.paralang.plang.types;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.codec.binary.Base64;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.runtime.BaseCompiledStub;
import cz.upol.vanusanik.paralang.runtime.FunctionWrapper;
import cz.upol.vanusanik.paralang.runtime.PLRuntime;
import cz.upol.vanusanik.paralang.utils.Utils;

/**
 * Java instance wrapper for PLang, wraps java instance for use in PLang.
 * 
 * @author Enerccio
 *
 */
public class Pointer extends BaseCompiledStub implements Serializable {
	private static final long serialVersionUID = -4564277494396267580L;

	public Pointer() {

	}

	public Pointer(Object value) {
		this.value = value;
	}

	/** Wrapped object */
	private Object value;

	@Override
	public PlangObjectType ___getType() {
		return PlangObjectType.JAVAOBJECT;
	}

	@Override
	public String toString() {
		return "" + value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		Pointer other = (Pointer) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public JsonValue ___toObject() {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutputStream serstream = new ObjectOutputStream(out);
			if (value instanceof Serializable)
				serstream.writeObject(value);
			else
				serstream.writeObject(NoValue.NOVALUE);
			String serializedForm = Base64
					.encodeBase64String(out.toByteArray());
			return new JsonObject().add("metaObjectType",
					___getType().toString()).add("value", serializedForm);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Exception thrown when there is no such method or method is wrong.
	 * Internal exception that is never thrown outside this class.
	 * 
	 * @author Enerccio
	 *
	 */
	public static class PointerMethodIncompatibleException extends Exception {
		private static final long serialVersionUID = -7762628083995844743L;

		public PointerMethodIncompatibleException() {
			super();
		}

		@Override
		public synchronized Throwable fillInStackTrace() {
			// No stack generation, is used in object flow
			return this;
		}

	};

	/** MethodHandle cache */
	private static Map<Method, MethodHandle> methodHandles = Collections
			.synchronizedMap(new WeakHashMap<Method, MethodHandle>());

	/**
	 * Runs method identified by this string with those arguments. Does
	 * automatic type conversion from PLang to Java
	 * 
	 * @param methodName
	 * @param args
	 * @return
	 * @throws Throwable
	 */
	public PLangObject runMethod(String methodName, PLangObject[] args)
			throws Throwable {
		if (value == null)
			// check for transient pointer, ie deserialized pointer of object
			// that does not support serialization
			throw PLRuntime.getRuntime().newInstance("System.BaseException",
					new Str("Transient pointer accessed"));

		for (Method m : value.getClass().getMethods()) {
			if (m.getName().equals(methodName)) {
				try {
					// maybe found method, do the type conversions and check

					Class<?> retType = m.getReturnType();
					Class<?>[] argTypes = m.getParameterTypes();
					List<Object> constructedArgs = new ArrayList<Object>();

					if (argTypes.length != args.length)
						continue;

					// found it, do type conversions
					int it = 0;
					for (Class<?> aType : argTypes) {
						if (aType.isAssignableFrom(args[it].getClass())) {
							constructedArgs.add(args[it]);
						} else {
							constructedArgs.add(Utils.asJavaObject(aType,
									args[it]));
						}
						++it;
					}

					// find method handle in cache
					MethodHandle genHandle = methodHandles.get(m);

					if (genHandle == null) {
						// store handle in cache if there is none
						genHandle = MethodHandles.lookup().unreflect(m);
						methodHandles.put(m, genHandle);
					}

					MethodHandle handle = genHandle.bindTo(value);
					// run the method
					Object ret = handle.invokeWithArguments(constructedArgs
							.toArray());

					// recast the return value back
					if (ret == null)
						return NoValue.NOVALUE;

					if (retType.isAssignableFrom(PLangObject.class))
						return (PLangObject) ret;

					return Utils.cast(ret, retType);
				} catch (PointerMethodIncompatibleException ce) {
					// method incompatible, find another method
					continue;
				} catch (Exception e) {
					throw PLRuntime.getRuntime().newInstance(
							"System.BaseException",
							new Str("Failed to execute java method: "
									+ e.getMessage()));
				}
			}
		}
		// No method found
		throw PLRuntime.getRuntime().newInstance("System.BaseException",
				new Str(("Unknown method " + methodName + " for arguments of " + Arrays.asList(args))));
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
		if (b.___getType().equals(___getType()))
			return value.equals(((Pointer) b).value);
		else
			return false;
	}

	@SuppressWarnings("unchecked")
	public <T> T getPointer() {
		return (T) value;
	}

	@Override
	protected void ___init_internal_datafields(BaseCompiledStub self) {
		___restrictedOverride = true;

		___setkey("is_transient_pointer", new FunctionWrapper(
				"transientPointer", this, true));
		___setkey("will_be_transient_pointer", new FunctionWrapper(
				"willBeTransientPointer", this, true));
		___setkey("_str", new FunctionWrapper("__str", this, true));

		___restrictedOverride = false;
	}
	
	public PLangObject __str(PLangObject self){
		return new Str(value.toString());
	}

	/**
	 * Check for transient pointer
	 * 
	 * @param self
	 * @return
	 */
	public PLangObject transientPointer(PLangObject self) {
		return BooleanValue.fromBoolean(value == null);
	}

	/**
	 * Checks whether it might be transient pointer or not
	 * 
	 * @param self
	 * @return
	 */
	public PLangObject willBeTransientPointer(PLangObject self) {
		return BooleanValue.fromBoolean(!(value instanceof Serializable));
	}
}
