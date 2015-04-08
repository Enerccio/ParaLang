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
	public PlangObjectType ___getType() {
		return PlangObjectType.NOVALUE;
	}

	@Override
	public String toString(){
		return "NoValue";
	}
	
	@Override
	public JsonValue ___toObject() {
		return new JsonObject().add("metaObjectType", ___getType().toString());
	}
	
	@Override
	public boolean ___isNumber() {
		return false;
	}

	@Override
	public Float ___getNumber(PLangObject self) {
		return null;
	}
	
	@Override
	public boolean ___eq(PLangObject self, PLangObject b) {
		return this == b;
	}
	
	private Object readResolve()  {
	    return NOVALUE;
	}
}
