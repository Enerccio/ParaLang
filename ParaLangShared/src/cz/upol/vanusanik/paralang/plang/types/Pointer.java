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

public class Pointer extends PLangObject {
	
	public Pointer(){
		
	}
	
	public Pointer(Object value){
		this.value = value;
		if (!(value instanceof Serializable))
			throw new RuntimeException("Not serializable...");
	}

	Object value;
	
	@Override
	public PlangObjectType getType() {
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
	public JsonValue toObject(long previousTime) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutputStream serstream = new ObjectOutputStream(out);
			serstream.writeObject(value);
			String serializedForm = Base64.encodeBase64String(out.toByteArray());
			return new JsonObject().add("metaObjectType", getType().toString())
					.add("value", serializedForm);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public PLangObject runMethod(String methodName, PLangObject[] args) throws Throwable {
		for (Method m : value.getClass().getMethods()){
			if (m.getName().equals(methodName)){
				return (PLangObject) m.invoke(value, new Object[]{args});
			}
		}
		throw new RuntimeException("Unknown method: " + methodName);
	}
}
