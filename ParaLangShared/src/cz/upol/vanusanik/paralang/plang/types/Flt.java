package cz.upol.vanusanik.paralang.plang.types;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;

public class Flt extends PLangObject {
	
	public Flt(){
		
	}
	
	public Flt(float value){
		this.value = value;
	}

	float value;
	
	@Override
	public PlangObjectType getType() {
		return PlangObjectType.FLOAT;
	}
	
	@Override
	public String toString(){
		return ""+value;
	}

}
