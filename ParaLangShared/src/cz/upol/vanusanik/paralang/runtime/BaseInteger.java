package cz.upol.vanusanik.paralang.runtime;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.plang.types.NoValue;

public class BaseInteger extends BaseNumber {
	private static final long serialVersionUID = 2544264163945283955L;
	
	public BaseInteger(){
		
	}
	
	@Override
	public PLangObject __init_superclass(PLangObject self, PLangObject iv){
		if (!iv.__sys_m_isNumber()){
			throw new RuntimeException("Value " + iv + " is not a number!");
		}
		__setkey(__valKey, new Int(iv.__sys_m_getNumber(iv).intValue()));
		return NoValue.NOVALUE;
	}
	
	@Override
	public JsonValue __sys_m_toObject(long previousTime) {
		JsonObject metaData = new JsonObject().add("metaObjectType", __sys_m_getType().toString());
		metaData.add("isBaseClass", true)
				.add("baseClassType", "INTEGER")
				.add("value", __sys_m_getNumber(this).intValue());
		return metaData;
	}

}
