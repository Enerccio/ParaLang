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

}
