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
		if (!iv.___isNumber()){
			throw new RuntimeException("Value " + iv + " is not a number!");
		}
		___setkey(__valKey, new Flt(iv.___getNumber(iv).floatValue()));
		return NoValue.NOVALUE;
	}
	
	@Override
	public JsonValue ___toObject(long previousTime) {
		JsonObject metaData = new JsonObject().add("metaObjectType", ___getType().toString());
		metaData.add("isBaseClass", true)
				.add("baseClassType", "FLOAT")
				.add("value", ___getNumber(this));
		return metaData;
	}
}
