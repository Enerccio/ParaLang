package cz.upol.vanusanik.paralang.plang.types;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(value);
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
		Flt other = (Flt) obj;
		if (Float.floatToIntBits(value) != Float.floatToIntBits(other.value))
			return false;
		return true;
	}

	@Override
	public JsonValue toObject(long previousTime) {
		return new JsonObject().add("metaObjectType", getType().toString())
				.add("value", value);
	}
	
	@Override
	public boolean isNumber() {
		return true;
	}

	@Override
	public Float getNumber() {
		return value;
	}

	@Override
	public boolean eq(PLangObject b) {
		if (!b.isNumber()) return false;
		return value == b.getNumber();
	}
}
