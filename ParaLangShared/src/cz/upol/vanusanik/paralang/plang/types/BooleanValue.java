package cz.upol.vanusanik.paralang.plang.types;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;

public class BooleanValue extends PLangObject {

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

}
