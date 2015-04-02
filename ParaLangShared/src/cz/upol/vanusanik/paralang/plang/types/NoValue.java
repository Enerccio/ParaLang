package cz.upol.vanusanik.paralang.plang.types;

import java.io.Serializable;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;

public class NoValue extends PLangObject implements Serializable {
	private static final long serialVersionUID = 7573052889816332570L;

	private NoValue(){
		
	}
	
	public static final NoValue NOVALUE = new NoValue();

	@Override
	public PlangObjectType __sys_m_getType() {
		return PlangObjectType.NOVALUE;
	}

	@Override
	public String toString(){
		return "NoValue";
	}
	
	@Override
	public JsonValue __sys_m_toObject(long previousTime) {
		return new JsonObject().add("metaObjectType", __sys_m_getType().toString());
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
		return this == b;
	}
	
	private Object readResolve()  {
	    return NOVALUE;
	}
}
