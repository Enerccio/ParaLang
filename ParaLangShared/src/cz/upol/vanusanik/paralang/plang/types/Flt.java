package cz.upol.vanusanik.paralang.plang.types;

import java.io.Serializable;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.plang.PrimitivePLangObject;

/**
 * PLang float
 * 
 * @author Enerccio
 *
 */
public class Flt extends PrimitivePLangObject implements Serializable {
	private static final long serialVersionUID = -2146628641767169636L;

	public Flt() {

	}

	public Flt(float value) {
		this.value = value;
	}

	/** Actual value of this instance */
	float value;

	@Override
	public PlangObjectType ___getType() {
		return PlangObjectType.FLOAT;
	}

	@Override
	public String toString() {
		return "" + value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(value);
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
		Flt other = (Flt) obj;
		if (Float.floatToIntBits(value) != Float.floatToIntBits(other.value))
			return false;
		return true;
	}

	@Override
	public JsonValue ___toObject(Set<Long> alreadySerialized, boolean serializeFully) {
		return new JsonObject().add("metaObjectType", ___getType().toString())
				.add("value", value);
	}

	@Override
	public boolean ___isNumber() {
		return true;
	}

	@Override
	public Float ___getNumber(PLangObject self) {
		return value;
	}

	@Override
	public boolean ___eq(PLangObject self, PLangObject b) {
		if (!b.___isNumber())
			return false;
		return value == b.___getNumber(b);
	}

	@Override
	public boolean ___less(PLangObject self, PLangObject other, boolean equals) {
		return equals ? (value <= other.___getNumber(other)) : (value < other
				.___getNumber(other));
	}

	@Override
	public boolean ___more(PLangObject self, PLangObject other, boolean equals) {
		return equals ? (value >= other.___getNumber(other)) : (value > other
				.___getNumber(other));
	}
}
