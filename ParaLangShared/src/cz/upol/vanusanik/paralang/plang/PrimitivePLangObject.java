package cz.upol.vanusanik.paralang.plang;

import cz.upol.vanusanik.paralang.runtime.BaseCompiledStub;

/**
 * PrimitivePLangObject is base class for primitive types, such as Int, Flt etc.
 * 
 * @author Enerccio
 *
 */
public abstract class PrimitivePLangObject implements PLangObject {

	@Override
	public boolean ___less(PLangObject self, PLangObject other, boolean equals) {
		throw new RuntimeException("Undefined method for this type!");
	}

	@Override
	public boolean ___more(PLangObject self, PLangObject other, boolean equals) {
		throw new RuntimeException("Undefined method for this type!");
	}

	@Override
	public BaseCompiledStub ___getLowestClassdef() {
		return null;
	}

	@Override
	public String toString(PLangObject self) {
		return toString();
	}

}
