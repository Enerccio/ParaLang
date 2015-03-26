package cz.upol.vanusanik.paralang.plang.types;

import java.lang.reflect.Method;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.runtime.BaseCompiledStub;
import cz.upol.vanusanik.paralang.utils.Utils;

public class FunctionWrapper extends PLangObject {
	
	public FunctionWrapper(){
		
	}
	
	public FunctionWrapper(String mName, BaseCompiledStub stub, boolean ism){
		methodName = mName;
		owner = stub;
		isMethod = ism;
	}
	
	private boolean isMethod;
	private String methodName;
	private BaseCompiledStub owner;

	public PLangObject run(PLangObject... arguments) throws Exception{
		for (Method m : owner.getClass().getMethods()){
			if (m.getName().equals(methodName)){
				return run(m, owner, arguments);
			}
		}
		throw new RuntimeException("Unknown method: " + methodName);
	}

	private PLangObject run(Method m, BaseCompiledStub runner, PLangObject[] arguments) throws Exception {
		PLangObject[] data = arguments;
		if (isMethod){
			data = Utils.pushLeft(runner.__getThis(), arguments);
		}
		return (PLangObject) m.invoke(runner, (Object[])data);
	}

	public boolean isMethod() {
		return isMethod;
	}

	public void setMethod(boolean isMethod) {
		this.isMethod = isMethod;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	
	public BaseCompiledStub getOwner() {
		return owner;
	}

	public void setOwner(BaseCompiledStub owner) {
		this.owner = owner;
	}

	@Override
	public PlangObjectType getType() {
		return PlangObjectType.FUNCTION;
	}
	
	@Override
	public String toString(){
		return "Function Wrapper of method " + methodName + " of object " + owner;
	}
}
