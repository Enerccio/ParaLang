package cz.upol.vanusanik.paralang.plang;

import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.types.Flt;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.runtime.BaseCompiledStub;

public abstract class PLangObject extends RuntimeException {
	private static final long serialVersionUID = -4777704860769051988L;

	public abstract PlangObjectType __sys_m_getType();
	
	public abstract JsonValue __sys_m_toObject(long previousTime);
	
	public abstract boolean __sys_m_isNumber();
	
	public abstract Float __sys_m_getNumber(PLangObject self);
	
	public static PLangObject __sys_m_autocast(Float number, PLangObject a, PLangObject b){
		if (a.__sys_m_getType() == PlangObjectType.FLOAT || b.__sys_m_getType() == PlangObjectType.FLOAT)
			return new Flt(number);
		else
			return new Int(number.intValue());
	}

	public abstract boolean eq(PLangObject self, PLangObject b);

	public boolean __sys_m_less(PLangObject self, PLangObject other, boolean equals) {
		throw new RuntimeException("Undefined method for this type!");
	}
	
	public boolean __sys_m_more(PLangObject self, PLangObject other, boolean equals) {
		throw new RuntimeException("Undefined method for this type!");
	}
	
	public BaseCompiledStub __getLowestClassdef(){
		return null;
	}

	public String toString(PLangObject self) {
		return toString();
	}
}
