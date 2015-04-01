package cz.upol.vanusanik.paralang.plang;

import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.types.Flt;
import cz.upol.vanusanik.paralang.plang.types.Int;

public abstract class PLangObject {

	public abstract PlangObjectType __sys_m_getType();
	
	public abstract JsonValue __sys_m_toObject(long previousTime);
	
	public abstract boolean __sys_m_isNumber();
	
	public abstract Float __sys_m_getNumber();
	
	public static PLangObject __sys_m_autocast(Float number, PLangObject a, PLangObject b){
		if (a.__sys_m_getType() == PlangObjectType.FLOAT || b.__sys_m_getType() == PlangObjectType.FLOAT)
			return new Flt(number);
		else
			return new Int(number.intValue());
	}

	public abstract boolean eq(PLangObject b);

	public boolean __sys_m_less(PLangObject other, boolean equals) {
		throw new RuntimeException("Undefined method for this type!");
	}
	
	public boolean __sys_m_more(PLangObject other, boolean equals) {
		throw new RuntimeException("Undefined method for this type!");
	}
}
