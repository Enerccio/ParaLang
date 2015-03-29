package cz.upol.vanusanik.paralang.plang;

import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.types.Flt;
import cz.upol.vanusanik.paralang.plang.types.Int;

public abstract class PLangObject {

	public abstract PlangObjectType getType();
	
	public abstract JsonValue toObject(long previousTime);
	
	public abstract boolean isNumber();
	
	public abstract Float getNumber();
	
	public static PLangObject autocast(Float number, PLangObject a, PLangObject b){
		if (a.getType() == PlangObjectType.FLOAT || b.getType() == PlangObjectType.FLOAT)
			return new Flt(number);
		else
			return new Int(number.intValue());
	}

	public abstract boolean eq(PLangObject b);
}
