package cz.upol.vanusanik.paralang.plang.types;

import java.io.Serializable;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.plang.PrimitivePLangObject;

/**
 * PLang boolean
 * 
 * @author Enerccio
 *
 */
public class BooleanValue extends PrimitivePLangObject implements Serializable {
	private static final long serialVersionUID = -4278817229742488910L;
	/** boolean value held by this instance */
	boolean value;

	/**
	 * BooleanValue is singleton
	 * 
	 * @param value
	 */
	private BooleanValue(boolean value) {
		this.value = value;
	}

	/** PLang true */
	public static final BooleanValue TRUE = new BooleanValue(true);
	/** PLang false */
	public static final BooleanValue FALSE = new BooleanValue(false);

	@Override
	public PlangObjectType ___getType() {
		return PlangObjectType.BOOLEAN;
	}

	@Override
	public String toString() {
		return value ? "TRUE" : "FALSE";
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

	/**
	 * Returns PLang value from boolean
	 * 
	 * @param result
	 * @return
	 */
	public static PLangObject fromBoolean(boolean result) {
		return result ? TRUE : FALSE;
	}

	@Override
	public boolean ___eq(PLangObject self, PLangObject b) {
		return this == b;
	}

	/**
	 * Since this value is singleton, deserialize it as such
	 * 
	 * @return
	 */
	private Object readResolve() {
		return value ? TRUE : FALSE;
	}

	/**
	 * Converts any Plang value into boolean
	 * 
	 * @param b
	 * @return
	 */
	public static boolean toBoolean(PLangObject b) {
		return TypeOperations.convertToBoolean(b);
	}

}
