package cz.upol.vanusanik.paralang.runtime;

import cz.upol.vanusanik.paralang.plang.PlangObjectType;

public abstract class PLClass extends BaseCompiledStub{

	@Override
	public PlangObjectType getType() {
		return PlangObjectType.CLASS;
	}
	
	@Override
	public void __init_class(){
		super.__init_class();
		__fieldsAndMethods.put("inst", this);
	}

}
