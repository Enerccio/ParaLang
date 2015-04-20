package cz.upol.vanusanik.paralang.plang;

import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.runtime.BaseCompiledStub;

public interface PLangObject {

	public PlangObjectType ___getType();
	
	public JsonValue ___toObject();
	
	public boolean ___isNumber();
	
	public Float ___getNumber(PLangObject self);

	public boolean ___eq(PLangObject self, PLangObject b);

	public boolean ___less(PLangObject self, PLangObject other, boolean equals);
	
	public boolean ___more(PLangObject self, PLangObject other, boolean equals);
	
	public BaseCompiledStub ___getLowestClassdef();

	public String toString(PLangObject self);
	
}
