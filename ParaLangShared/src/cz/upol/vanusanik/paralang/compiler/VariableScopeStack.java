package cz.upol.vanusanik.paralang.compiler;

import java.util.HashMap;
import java.util.Map;

public class VariableScopeStack {
	
	public static enum VariableType {
		MODULE_VARIABLE, CLASS_VARIABLE, LOCAL_VARIABLE
	}
	
	private class VariableStackData {
		VariableType type;
		int locals;
	}

	private class ScopeStackElement {
		private ScopeStackElement parent;
		private Map<String, VariableStackData> map = new HashMap<String, VariableStackData>(); 
	}
	
	private ScopeStackElement top;
	
	public void pushNewStack(){
		ScopeStackElement el = new ScopeStackElement();
		el.parent = top;
		top = el;
	}
	
	public void addVariable(String variable, VariableType type) throws CompilationException{
		addVariable(variable, type, -1);
	}
	
	public void addVariable(String variable, VariableType type, int locals) throws CompilationException{
		if (top == null)
			throw new CompilationException("Variable Scope empty but addVariable called!");
		VariableStackData vsd = new VariableStackData();
		vsd.type = type;
		vsd.locals = locals;
		top.map.put(variable, vsd);
	}
	
	public VariableType getType(String variable){
		ScopeStackElement el = top;
		while (el != null)
			if (el.map.containsKey(variable))
				return el.map.get(variable).type;
			else
				el = el.parent;
		return VariableType.MODULE_VARIABLE;
	}
	
	public int getLocal(String variable){
		ScopeStackElement el = top;
		while (el != null)
			if (el.map.containsKey(variable))
				return el.map.get(variable).locals;
			else
				el = el.parent;
		return -1;
	}
	
	public void popStack() throws CompilationException{
		if (top == null)
			throw new CompilationException("Variable Scope empty but popStack called!");
		top = top.parent;
	}
}
