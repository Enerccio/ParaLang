package cz.upol.vanusanik.paralang.plang.types;

import java.io.Serializable;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;

public class Int extends PLangObject implements Serializable {
	private static final long serialVersionUID = 3731336418712870225L;
	long value;
	
	public Int(){
		
	}
	
	public Int(int value){
		this.value = value;
	}
	
	public Int(long value){
		this.value = value;
	}

	@Override
	public PlangObjectType ___getType() {
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
		result = prime * result + (int) (value ^ (value >>> 32));
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
	public JsonValue ___toObject() {
		return new JsonObject().add("metaObjectType", ___getType().toString())
				.add("value", value);
	}
	
	@Override
	public boolean ___isNumber() {
		return true;
	}

	@Override
	public Float ___getNumber(PLangObject self) {
		return (float) value;
	}
	
	@Override
	public boolean ___eq(PLangObject self, PLangObject b) {
		if (!b.___isNumber()) return false;
		return value == b.___getNumber(b).intValue();
	}

	public long getValue() {
		return value;
	}
	
	@Override
	public boolean ___less(PLangObject self, PLangObject other, boolean equals) {
		return equals ? (value <= other.___getNumber(other)) : (value < other.___getNumber(other));
	}
	
	@Override
	public boolean ___more(PLangObject self, PLangObject other, boolean equals) {
		return equals ? (value >= other.___getNumber(other)) : (value > other.___getNumber(other));
	}
}
