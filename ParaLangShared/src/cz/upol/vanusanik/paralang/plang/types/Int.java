package cz.upol.vanusanik.paralang.plang.types;

import java.io.Serializable;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;

public class Int extends PLangObject implements Serializable {
	private static final long serialVersionUID = 3731336418712870225L;
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
	
	@Override
	public boolean isNumber() {
		return true;
	}

	@Override
	public Float getNumber() {
		return (float) value;
	}
	
	@Override
	public boolean eq(PLangObject b) {
		if (!b.isNumber()) return false;
		return value == b.getNumber();
	}

	public int getValue() {
		return value;
	}
	
	public boolean less(PLangObject other, boolean equals) {
		return equals ? (value <= other.getNumber()) : (value < other.getNumber());
	}
	
	public boolean more(PLangObject other, boolean equals) {
		return equals ? (value >= other.getNumber()) : (value > other.getNumber());
	}
}
