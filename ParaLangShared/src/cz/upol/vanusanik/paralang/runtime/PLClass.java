package cz.upol.vanusanik.paralang.runtime;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;

public abstract class PLClass extends BaseCompiledStub{
	public static final String __superKey = "$$__parent__$$";

	@Override
	public PlangObjectType getType() {
		return PlangObjectType.CLASS;
	}
	
	@Override
	public void __init_class(){
		super.__init_class();
		__fieldsAndMethods.put("inst", this);
	}

	public PLangObject __getkey(String key){
		if (!isInited){
			__init_class();
		}
		
		if (!__fieldsAndMethods.containsKey(key))
			return __getSuper().__getkey(key);
		
		PLangObject data = __fieldsAndMethods.get(key);
		return data;
	}

	private PLClass __getSuper() {
		if (!isInited){
			__init_class();
		}
		
		if (!__fieldsAndMethods.containsKey(__superKey))
			return null;
		
		return (PLClass) __fieldsAndMethods.get(__superKey);
	}
}
