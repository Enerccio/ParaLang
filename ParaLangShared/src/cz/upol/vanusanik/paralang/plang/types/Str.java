package cz.upol.vanusanik.paralang.plang.types;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;

public class Str extends PLangObject {
	
	public Str(){
		
	}
	
	public Str(String value){
		this.value = value;
	}

	String value;
	
	@Override
	public PlangObjectType getType() {
		return PlangObjectType.STRING;
	}
	
	@Override
	public String toString(){
		return ""+value;
	}

}
