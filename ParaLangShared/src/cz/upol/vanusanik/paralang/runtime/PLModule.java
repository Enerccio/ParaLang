package cz.upol.vanusanik.paralang.runtime;

import cz.upol.vanusanik.paralang.plang.PlangObjectType;

public abstract class PLModule extends BaseCompiledStub {

	@Override
	public PlangObjectType getType() {
		return PlangObjectType.MODULE;
	}
	
}
