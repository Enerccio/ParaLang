package cz.upol.vanusanik.paralang.plang.types;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.runtime.BaseCompiledStub;
import cz.upol.vanusanik.paralang.utils.Utils;

public class FunctionWrapper extends PLangObject implements Serializable {

	private static final long serialVersionUID = 3998164784189902299L;

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
	
	@Override
	public BaseCompiledStub ___getLowestClassdef() {
		BaseCompiledStub lowest = owner;
		BaseCompiledStub nextLowest = null;
		do {
			nextLowest = lowest.___getLowestClassInstance();
			if (nextLowest != null)
				lowest = nextLowest;
		} while (nextLowest != null);
		return lowest;
	}
	
	private class MethodAccessor {
		public Method m;
		public BaseCompiledStub o;
	}

	public PLangObject run(BaseCompiledStub owner, PLangObject... arguments) throws Exception{
		for (MethodAccessor ma : getAllMethods(owner)){
			if (ma.m.getName().equals(methodName)){
				return run(ma.m, ma.o, owner, arguments);
			}
		}
		throw new RuntimeException("Unknown method: " + methodName);
	}

	private List<MethodAccessor> getAllMethods(BaseCompiledStub owner) {
		List<MethodAccessor> mList = new ArrayList<MethodAccessor>();
		
		do {
			for (Method m : owner.getClass().getMethods()){
				MethodAccessor ma = new MethodAccessor();
				ma.m = m;
				ma.o = owner;
				mList.add(ma);
			}
			owner = owner.___getParent();
		} while (owner != null);
		
		return mList;
	}

	private PLangObject run(Method m, BaseCompiledStub runner, BaseCompiledStub self, PLangObject[] arguments) throws Exception {
		PLangObject[] data = arguments;
		if (isMethod){
			data = Utils.pushLeft(self, arguments);
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
	public PlangObjectType ___getType() {
		return PlangObjectType.FUNCTION;
	}
	
	@Override
	public String toString(){
		return "Function Wrapper of method " + methodName + " of object " + owner;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((methodName == null) ? 0 : methodName.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FunctionWrapper other = (FunctionWrapper) obj;
		if (methodName == null) {
			if (other.methodName != null)
				return false;
		} else if (!methodName.equals(other.methodName))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		return true;
	}
	
	@Override
	public JsonValue ___toObject(long previousTime) {
		return new JsonObject().add("metaObjectType", ___getType().toString())
				.add("value", new JsonObject()
					.add("methodName", methodName)
					.add("owner", owner.___toObject(previousTime))
					.add("isClassMethod", isMethod));
	}
	
	@Override
	public boolean ___isNumber() {
		return false;
	}

	@Override
	public Float ___getNumber(PLangObject self) {
		return null;
	}
	
	@Override
	public boolean ___eq(PLangObject self, PLangObject b) {
		return this == b;
	}
}
