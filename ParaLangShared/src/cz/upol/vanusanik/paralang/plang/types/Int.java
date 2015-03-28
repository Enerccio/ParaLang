package cz.upol.vanusanik.paralang.plang.types;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + value;
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
		Int other = (Int) obj;
		if (value != other.value)
			return false;
		return true;
	}

	@Override
	public JsonValue toObject(long previousTime) {
		return new JsonObject().add("metaObjectType", getType().toString())
				.add("value", value);
	}
}
