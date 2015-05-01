package cz.upol.vanusanik.paralang.runtime;

import cz.upol.vanusanik.paralang.plang.PlangObjectType;

public abstract class PLModule extends BaseCompiledStub {
	private static final long serialVersionUID = -3024698349214517029L;

	@Override
	public PlangObjectType ___getType() {
		return PlangObjectType.MODULE;
	}

}
