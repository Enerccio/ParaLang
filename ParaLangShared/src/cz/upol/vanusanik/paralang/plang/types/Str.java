package cz.upol.vanusanik.paralang.plang.types;

import java.io.Serializable;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;

public class Str extends PLangObject implements Serializable {
	private static final long serialVersionUID = -7656132457086147070L;

	public Str(){
		
	}
	
	public Str(String value){
		this.value = value;
	}

	String value;
	
	@Override
	public PlangObjectType __sys_m_getType() {
		return PlangObjectType.STRING;
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
		Str other = (Str) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
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
		return false;
	}

	@Override
	public Float __sys_m_getNumber(PLangObject self) {
		return null;
	}
	
	@Override
	public boolean eq(PLangObject self, PLangObject b) {
		if (b.__sys_m_getType().equals(__sys_m_getType()))
			return value.equals(((Str)b).value);
		else
			return false;
	}
}
