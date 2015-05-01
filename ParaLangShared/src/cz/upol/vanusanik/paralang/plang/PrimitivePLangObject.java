package cz.upol.vanusanik.paralang.plang;

import cz.upol.vanusanik.paralang.runtime.BaseCompiledStub;

public abstract class PrimitivePLangObject implements PLangObject {

	public boolean ___less(PLangObject self, PLangObject other, boolean equals) {
		throw new RuntimeException("Undefined method for this type!");
	}

	public boolean ___more(PLangObject self, PLangObject other, boolean equals) {
		throw new RuntimeException("Undefined method for this type!");
	}

	public BaseCompiledStub ___getLowestClassdef() {
		return null;
	}

	public String toString(PLangObject self) {
		return toString();
	}

}
