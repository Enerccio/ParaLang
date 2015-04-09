package cz.upol.vanusanik.paralang.plang.types;

import java.io.Serializable;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.runtime.BaseCompiledStub;
import cz.upol.vanusanik.paralang.runtime.PLRuntime;

public class Str extends BaseCompiledStub implements Serializable {
	private static final long serialVersionUID = -7656132457086147070L;

	public Str(){
		
	}
	
	public Str(String value){
		this.value = value;
	}

	String value;
	
	@Override
	public PlangObjectType ___getType() {
		return PlangObjectType.STRING;
	}
	
	@Override
	public String toString(){
		return ""+value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Str other = (Str) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public JsonValue ___toObject() {
		return new JsonObject().add("metaObjectType", ___getType().toString())
				.add("value", value);
	}
	
	@Override
	public boolean ___isNumber() {
		return false;
	}

	@Override
	public Float ___getNumber(PLangObject self) {
		return null;
	}
	
	@Override
	public boolean ___eq(PLangObject self, PLangObject b) {
		if (b.___getType().equals(___getType()))
			return value.equals(((Str)b).value);
		else
			return false;
	}

	@Override
	protected void ___init_internal_datafields(BaseCompiledStub self) {
		___restrictedOverride = true;
		
		___setkey("character_at", new FunctionWrapper("characterAt", this, true));
		___setkey("from", new FunctionWrapper("substringFrom", this, true));
		___setkey("to", new FunctionWrapper("substringTo", this, true));
		___setkey("starts_with", new FunctionWrapper("startsWith", this, true));
		___setkey("ends_with", new FunctionWrapper("endsWith", this, true));
		
		___restrictedOverride = false;
	}
	
	public PLangObject characterAt(PLangObject self, Int id){
		int idx = (int) id.getValue();
		if (idx < 0 || idx >= value.length())
			throw PLRuntime.getRuntime().newInstance("System.BaseException", new Str("Index out of range."));
		
		return new Str(Character.toString(value.charAt(idx)));
	}
	
	public PLangObject substringFrom(PLangObject self, Int from){
		int f = (int) from.getValue();
		if (f < 0 || f >= value.length())
			throw PLRuntime.getRuntime().newInstance("System.BaseException", new Str("Index out of range."));
		
		return new Str(value.substring(f));
	}
	
	public PLangObject substringTo(PLangObject self, Int to){
		int t = (int) to.getValue();
		if (t < 0 || t >= value.length())
			throw PLRuntime.getRuntime().newInstance("System.BaseException", new Str("Index out of range."));
		
		return new Str(value.substring(0, t));
	}
	
	public PLangObject startsWith(PLangObject self, Str sw){
		return BooleanValue.fromBoolean(value.startsWith(sw.value));
	}
	
	public PLangObject endssWith(PLangObject self, Str sw){
		return BooleanValue.fromBoolean(value.endsWith(sw.value));
	}
}
