package cz.upol.vanusanik.paralang.plang.types;

import java.io.Serializable;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;

public class BooleanValue extends PLangObject implements Serializable {
	private static final long serialVersionUID = -4278817229742488910L;
	boolean value;
	
	private BooleanValue(boolean value){
		this.value = value;
	}
	
	public static final BooleanValue TRUE = new BooleanValue(true);
	public static final BooleanValue FALSE = new BooleanValue(false);
	
	@Override
	public PlangObjectType getType() {
		return PlangObjectType.BOOLEAN;
	}
	
	@Override
	public String toString(){
		return value ? "TRUE" : "FALSE";
	}

	@Override
	public JsonValue toObject(long previousTime) {
		return new JsonObject().add("metaObjectType", getType().toString())
				.add("value", value);
	}

	@Override
	public boolean isNumber() {
		return false;
	}

	@Override
	public Float getNumber() {
		return null;
	}

	public static PLangObject fromBoolean(boolean result) {
		return result ? TRUE : FALSE;
	}

	@Override
	public boolean eq(PLangObject b) {
		return this == b;
	}
	
	private Object readResolve()  {
		return value ? TRUE : FALSE;
	}

	public static boolean toBoolean(PLangObject b) {
		return TypeOperations.convertToBoolean(b);
	}

}
