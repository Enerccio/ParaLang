package cz.upol.vanusanik.paralang.runtime;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.Flt;
import cz.upol.vanusanik.paralang.plang.types.NoValue;

public class BaseFloat extends BaseNumber {
	private static final long serialVersionUID = 9204688374246651301L;
	
	public BaseFloat(){
		
	}

	@Override
	public PLangObject __init_superclass(PLangObject self, PLangObject iv){
		if (!iv.__sys_m_isNumber()){
			throw new RuntimeException("Value " + iv + " is not a number!");
		}
		__setkey(__valKey, new Flt(iv.__sys_m_getNumber(iv).floatValue()));
		return NoValue.NOVALUE;
	}
	
	@Override
	public JsonValue __sys_m_toObject(long previousTime) {
		JsonObject metaData = new JsonObject().add("metaObjectType", __sys_m_getType().toString());
		metaData.add("isBaseClass", true)
				.add("baseClassType", "FLOAT")
				.add("value", __sys_m_getNumber(this));
		return metaData;
	}
}
