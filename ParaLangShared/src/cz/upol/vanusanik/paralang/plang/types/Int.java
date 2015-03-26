package cz.upol.vanusanik.paralang.plang.types;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;

public class Int extends PLangObject {
	
	int value;
	
	public Int(){
		
	}
	
	public Int(int value){
		this.value = value;
	}

	@Override
	public PlangObjectType getType() {
		return PlangObjectType.INTEGER;
	}
	
	@Override
	public String toString(){
		return ""+value;
	}

}
