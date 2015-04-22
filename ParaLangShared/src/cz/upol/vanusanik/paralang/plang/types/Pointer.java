package cz.upol.vanusanik.paralang.plang.types;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import org.apache.commons.codec.binary.Base64;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.runtime.BaseCompiledStub;
import cz.upol.vanusanik.paralang.runtime.PLRuntime;
import cz.upol.vanusanik.paralang.utils.Utils;

public class Pointer extends BaseCompiledStub implements Serializable  {
	private static final long serialVersionUID = -4564277494396267580L;

	public Pointer(){
		
	}
	
	public Pointer(Object value){
		this.value = value;
	}

	private Object value;
	
	@Override
	public PlangObjectType ___getType() {
		return PlangObjectType.JAVAOBJECT;
	}
	
	@Override
	public String toString(){
		return ""+value;
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
			String serializedForm = Base64.encodeBase64String(out.toByteArray());
			return new JsonObject().add("metaObjectType", ___getType().toString())
					.add("value", serializedForm);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static class PointerMethodIncompatibleException extends Exception {
		private static final long serialVersionUID = -7762628083995844743L;
		public PointerMethodIncompatibleException(){
			super();
		}
		@Override
		public synchronized Throwable fillInStackTrace() {
			return this;
		}
		
	};
	
	private static WeakHashMap<Method, MethodHandle> methodHandles
		= new WeakHashMap<Method, MethodHandle>();

	public PLangObject runMethod(String methodName, PLangObject[] args) throws Throwable {
		if (value == null)
			throw PLRuntime.getRuntime().newInstance("System.BaseException", new Str("Transient pointer accessed"));
		
		for (Method m : value.getClass().getMethods()){
			if (m.getName().equals(methodName)){
				try {
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
					
					MethodHandle genHandle = methodHandles.get(m);
					
					if (genHandle == null){
						genHandle = MethodHandles.lookup().unreflect(m);
						methodHandles.put(m, genHandle);
					}
					
					MethodHandle handle = genHandle.bindTo(value);
					Object ret = handle.invokeWithArguments(constructedArgs.toArray());
					
					if (ret == null)
						return NoValue.NOVALUE;
					
					if (retType.isAssignableFrom(PLangObject.class))
						return (PLangObject) ret;
					
					return Utils.cast(ret, retType);
				} catch (PointerMethodIncompatibleException ce){
					continue;
				} catch (Exception e){
					throw PLRuntime.getRuntime().newInstance("System.BaseException", new Str("Failed to execute java method: " + e.getMessage()));
				}
			}
		}
		throw new RuntimeException("Unknown method: " + methodName);
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
			return value.equals(((Pointer)b).value);
		else
			return false;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getPointer(){
		return (T)value;
	}

	@Override
	protected void ___init_internal_datafields(BaseCompiledStub self) {
		___restrictedOverride = true;
		
		___setkey("is_transient_pointer", new FunctionWrapper("transientPointer", this, true));
		___setkey("will_be_transient_pointer", new FunctionWrapper("willBeTransientPointer", this, true));
		
		___restrictedOverride = false;
	}
	
	public PLangObject transientPointer(PLangObject self){
		return BooleanValue.fromBoolean(value == null);
	}
	
	public PLangObject willBeTransientPointer(PLangObject self){
		return BooleanValue.fromBoolean(!(value instanceof Serializable));
	}
}
