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
	public PlangObjectType ___getType() {
		return PlangObjectType.CLASS;
	}
	
	@Override
	public void ___init_class(){
		super.___init_class();
		___fieldsAndMethods.put("inst", this);
	}

	public PLangObject ___getkey(String key){
		if (!___isInited){
			___init_class();
		}
		
		PLClass parent = null;
		if (!___fieldsAndMethods.containsKey(key) && ((parent = __getSuper())!=null))
			return parent.___getkey(key);
		else if (!___fieldsAndMethods.containsKey(key)){
			return null;
		}
		
		PLangObject data = ___fieldsAndMethods.get(key);
		return data;
	}

	public PLClass __getSuper() {
		if (!___isInited){
			___init_class();
		}
		
		if (!___fieldsAndMethods.containsKey(__superKey))
			return null;
		
		return (PLClass) ___getkey(__superKey);
	}
	
	public void ___setDerivedClass(PLClass derived){
		___setkey(__derivedKey, derived);
	}
	
	@Override
	public boolean ___eq(PLangObject self, PLangObject b) {
		return BooleanValue.toBoolean(PLRuntime.getRuntime().run(___getkey(Operator.EQ.classMethod), (BaseCompiledStub)self, b));
	}
	
	@Override
	public BaseCompiledStub ___getLowestClassInstance() {
		if (___fieldsAndMethods.containsKey(__derivedKey))
			return (BaseCompiledStub) ___fieldsAndMethods.get(__derivedKey);
		return null;
	}
	
	@Override
	public String toString(PLangObject self) {
		if (___fieldsAndMethods.containsKey("__str")){
			PLangObject str = ___getkey("__str");
			if (str instanceof FunctionWrapper){
				PLangObject o = PLRuntime.getRuntime().run(str, (BaseCompiledStub)self);
				return o.toString(o);
			}
		}
		return super.toString();
	}
}
