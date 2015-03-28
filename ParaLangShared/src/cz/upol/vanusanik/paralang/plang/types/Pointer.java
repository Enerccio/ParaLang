package cz.upol.vanusanik.paralang.plang.types;

import java.io.Serializable;

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

}
