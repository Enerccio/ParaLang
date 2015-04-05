package cz.upol.vanusanik.paralang.runtime;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.plang.types.NoValue;
import cz.upol.vanusanik.paralang.plang.types.TypeOperations;
import cz.upol.vanusanik.paralang.plang.types.TypeOperations.Operator;

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
		___setkey(Operator.UBINNEG.classMethod, new FunctionWrapper("__ubn_base", this, true));
		
		return NoValue.NOVALUE;
	}
	
	public PLangObject __ubn_base(PLangObject self){
		return asObject(TypeOperations.ubneg(((PLClass)self).___getkey(__valKey)));
	}
	
	@Override
	public JsonValue ___toObject(long previousTime) {
		JsonObject metaData = new JsonObject().add("metaObjectType", ___getType().toString());
		metaData.add("isBaseClass", true)
				.add("baseClassType", "INTEGER")
				.add("value", ___getNumber(this).intValue());
		return metaData;
	}
	
	@Override
	protected PLangObject asObject(PLangObject o) {
		return PLRuntime.getRuntime().newInstance("System.Integer", o);
	}

}
