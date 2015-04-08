package cz.upol.vanusanik.paralang.plang.types;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;

import org.apache.commons.codec.binary.Base64;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.utils.Utils;

public class Pointer extends PLangObject implements Serializable {
	private static final long serialVersionUID = -4564277494396267580L;

	public Pointer(){
		
	}
	
	public Pointer(Object value){
		this.value = value;
		if (!(value instanceof Serializable))
			throw new RuntimeException("Not serializable...");
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
			serstream.writeObject(value);
			String serializedForm = Base64.encodeBase64String(out.toByteArray());
			return new JsonObject().add("metaObjectType", ___getType().toString())
					.add("value", serializedForm);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public PLangObject runMethod(String methodName, PLangObject[] args) throws Throwable {
		for (Method m : value.getClass().getMethods()){
			if (m.getName().equals(methodName)){
				try {
					return (PLangObject) m.invoke(value, Utils.asObjectArray(args));
				} catch (Exception e){
					throw new RuntimeException(e);
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
}
