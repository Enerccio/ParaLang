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
	public PlangObjectType __sys_m_getType() {
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
	public JsonValue __sys_m_toObject(long previousTime) {
		return new JsonObject().add("metaObjectType", __sys_m_getType().toString())
				.add("value", value);
	}
	
	@Override
	public boolean __sys_m_isNumber() {
		return true;
	}

	@Override
	public Float __sys_m_getNumber(PLangObject self) {
		return (float) value;
	}
	
	@Override
	public boolean eq(PLangObject self, PLangObject b) {
		if (!b.__sys_m_isNumber()) return false;
		return value == b.__sys_m_getNumber(b);
	}

	public int getValue() {
		return value;
	}
	
	@Override
	public boolean __sys_m_less(PLangObject self, PLangObject other, boolean equals) {
		return equals ? (value <= other.__sys_m_getNumber(other)) : (value < other.__sys_m_getNumber(other));
	}
	
	@Override
	public boolean __sys_m_more(PLangObject self, PLangObject other, boolean equals) {
		return equals ? (value >= other.__sys_m_getNumber(other)) : (value > other.__sys_m_getNumber(other));
	}
}
