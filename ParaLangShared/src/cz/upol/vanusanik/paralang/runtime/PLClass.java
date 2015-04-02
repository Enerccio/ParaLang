package cz.upol.vanusanik.paralang.runtime;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.TypeOperations.Operator;

public abstract class PLClass extends BaseCompiledStub{
	private static final long serialVersionUID = 715934816813529044L;
	public static final String __superKey = "$$__parent__$$";
	public static final String __derivedKey = "$$__derived__$$";

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
		if (!__isInited){
			__init_class();
		}
		
		PLClass parent = null;
		if (!__fieldsAndMethods.containsKey(key) && ((parent = __getSuper())!=null))
			return parent.__getkey(key);
		else if (!__fieldsAndMethods.containsKey(key)){
			return null;
		}
		
		PLangObject data = __fieldsAndMethods.get(key);
		return data;
	}

	public PLClass __getSuper() {
		if (!__isInited){
			__init_class();
		}
		
		if (!__fieldsAndMethods.containsKey(__superKey))
			return null;
		
		return (PLClass) __getkey(__superKey);
	}
	
	public void __setDerivedClass(PLClass derived){
		__setkey(__derivedKey, derived);
	}
	
	@Override
	public boolean eq(PLangObject self, PLangObject b) {
		return BooleanValue.toBoolean(PLRuntime.getRuntime().run(__getkey(Operator.EQ.classMethod), (BaseCompiledStub)self, b));
	}
	
	@Override
	public BaseCompiledStub __getLowestClassInstance() {
		if (__fieldsAndMethods.containsKey(__derivedKey))
			return (BaseCompiledStub) __fieldsAndMethods.get(__derivedKey);
		return null;
	}
	
	@Override
	public String toString(PLangObject self) {
		if (__fieldsAndMethods.containsKey("__str")){
			PLangObject str = __getkey("__str");
			if (str instanceof FunctionWrapper){
				PLangObject o = PLRuntime.getRuntime().run(str, (BaseCompiledStub)self);
				return o.toString(o);
			}
		}
		return super.toString();
	}
}
