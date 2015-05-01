package cz.upol.vanusanik.paralang.plang;

import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.runtime.BaseCompiledStub;

/**
 * Every PLangObject has to implement this interface
 * 
 * @author Enerccio
 *
 */
public interface PLangObject {

	/**
	 * Returns type of this instance
	 * 
	 * @return
	 */
	public PlangObjectType ___getType();

	/**
	 * Serializes this object into json
	 * 
	 * @return
	 */
	public JsonValue ___toObject();

	/**
	 * Returns whether it is number or not
	 * 
	 * @return
	 */
	public boolean ___isNumber();

	/**
	 * Returns itself as a numeric value.
	 * 
	 * @param self
	 *            This instance (used for objects)
	 * @return
	 */
	public Float ___getNumber(PLangObject self);

	/**
	 * Returns whether these objects are equal
	 * 
	 * @param self
	 *            this object
	 * @param b
	 *            other object
	 * @return
	 */
	public boolean ___eq(PLangObject self, PLangObject b);

	/**
	 * Returns < comparison
	 * 
	 * @param self
	 *            this object
	 * @param other
	 *            other object
	 * @param equals
	 *            <= or not
	 * @return
	 */
	public boolean ___less(PLangObject self, PLangObject other, boolean equals);

	/**
	 * Returns > comparison
	 * 
	 * @param self
	 *            this object
	 * @param other
	 *            other object
	 * @param equals
	 *            >= or not
	 * @return
	 */
	public boolean ___more(PLangObject self, PLangObject other, boolean equals);

	/**
	 * Finds lowest parent
	 * 
	 * @return
	 */
	public BaseCompiledStub ___getLowestClassdef();

	/**
	 * toString
	 * 
	 * @param self
	 *            this object
	 * @return
	 */
	public String toString(PLangObject self);

}
