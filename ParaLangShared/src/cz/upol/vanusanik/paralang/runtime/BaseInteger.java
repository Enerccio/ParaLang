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
		if (!iv.___isNumber()){
			throw new RuntimeException("Value " + iv + " is not a number!");
		}
		___setkey(__valKey, new Int(iv.___getNumber(iv).intValue()));
		return NoValue.NOVALUE;
	}
	
	@Override
	public JsonValue ___toObject(long previousTime) {
		JsonObject metaData = new JsonObject().add("metaObjectType", ___getType().toString());
		metaData.add("isBaseClass", true)
				.add("baseClassType", "INTEGER")
				.add("value", ___getNumber(this).intValue());
		return metaData;
	}

}
