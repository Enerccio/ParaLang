package cz.upol.vanusanik.paralang.plang;

import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.runtime.BaseCompiledStub;

public abstract class PLangObject extends RuntimeException {
	private static final long serialVersionUID = -4777704860769051988L;

	public abstract PlangObjectType ___getType();
	
	public abstract JsonValue ___toObject();
	
	public abstract boolean ___isNumber();
	
	public abstract Float ___getNumber(PLangObject self);

	public abstract boolean ___eq(PLangObject self, PLangObject b);

	public boolean ___less(PLangObject self, PLangObject other, boolean equals) {
		throw new RuntimeException("Undefined method for this type!");
	}
	
	public boolean ___more(PLangObject self, PLangObject other, boolean equals) {
		throw new RuntimeException("Undefined method for this type!");
	}
	
	public BaseCompiledStub ___getLowestClassdef(){
		return null;
	}

	public String toString(PLangObject self) {
		return toString();
	}
	
	@Override
    public Throwable fillInStackTrace()
    {
		if (___isException(this))
			return super.fillInStackTrace();
		else
			return this;
    }

	protected boolean ___isException(PLangObject self) {
		return false;
	}
}
