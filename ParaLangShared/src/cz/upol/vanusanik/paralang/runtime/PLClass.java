package cz.upol.vanusanik.paralang.runtime;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.TypeOperations.Operator;

public abstract class PLClass extends BaseCompiledStub{
	public static final String __superKey = "$$__parent__$$";

	@Override
	public PlangObjectType __sys_m_getType() {
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
		
		PLClass parent = null;
		if (!__fieldsAndMethods.containsKey(key) && ((parent = __getSuper())!=null))
			return parent.__getkey(key);
		else if (!__fieldsAndMethods.containsKey(key) && (parent == null)){
			return null;
		}
		
		PLangObject data = __fieldsAndMethods.get(key);
		return data;
	}

	public PLClass __getSuper() {
		if (!isInited){
			__init_class();
		}
		
		if (!__fieldsAndMethods.containsKey(__superKey))
			return null;
		
		return (PLClass) __getkey(__superKey);
	}
	
	@Override
	public boolean eq(PLangObject b) {
		return BooleanValue.toBoolean(PLRuntime.getRuntime().run(__getkey(Operator.EQ.classMethod), this, b));
	}
}
