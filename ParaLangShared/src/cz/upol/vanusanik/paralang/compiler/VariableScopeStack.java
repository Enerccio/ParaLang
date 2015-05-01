package cz.upol.vanusanik.paralang.compiler;

import java.util.HashMap;
import java.util.Map;

/**
 * VarStack is a scope stack that contains information to which variables are
 * legal in this scope. All variables have three types of scopes,
 * MODULE_VARIABLE refers to the module scope (ie semiglobal), CLASS_VARIABLE is
 * either module variable in functions or class variable in methods.
 * LOCAL_VARIABLE is local to the block.
 * 
 * This class is a stack, so when block/class is exited, information is popped
 * off the stack thus preserving the access. Inner blocks use this to
 * provide/invalidate local variables.
 * 
 * @author Enerccio
 *
 */
public class VariableScopeStack {

	/**
	 * Variable stack type
	 * 
	 * @author Enerccio
	 *
	 */
	public static enum VariableType {
		MODULE_VARIABLE, CLASS_VARIABLE, LOCAL_VARIABLE
	}

	/**
	 * Variable data containing type and bound local java variable number (1+).
	 * 
	 * @author Enerccio
	 *
	 */
	private class VariableStackData {
		VariableType type;
		int locals;
	}

	/**
	 * Stack element. Value is map of string->variable stack data.
	 * 
	 * @author Enerccio
	 *
	 */
	private class ScopeStackElement {
		private ScopeStackElement parent;
		private Map<String, VariableStackData> map = new HashMap<String, VariableStackData>();
	}

	/** Top of the stack */
	private ScopeStackElement top;

	/**
	 * Starts new stack level
	 */
	public void pushNewStack() {
		ScopeStackElement el = new ScopeStackElement();
		el.parent = top;
		top = el;
	}

	/**
	 * Adds variable with variable type onto current stack height.
	 * 
	 * @param variable
	 *            Identifier to the variable
	 * @param type
	 *            type of the variable
	 * @throws CompilationException
	 */
	public void addVariable(String variable, VariableType type)
			throws CompilationException {
		addVariable(variable, type, -1);
	}

	/**
	 * Adds variable with variable type onto current stack height with locals
	 * number (1+).
	 * 
	 * @param variable
	 *            variable Identifier to the variable
	 * @param type
	 *            type of the variable
	 * @param locals
	 *            numerical identifier to the locals array
	 * @throws CompilationException
	 */
	public void addVariable(String variable, VariableType type, int locals)
			throws CompilationException {
		if (top == null)
			throw new CompilationException(
					"Variable Scope empty but addVariable called!");
		VariableStackData vsd = new VariableStackData();
		vsd.type = type;
		vsd.locals = locals;
		top.map.put(variable, vsd);
	}

	/**
	 * Returns type of the variable identified by this identifier
	 * 
	 * @param variable
	 *            Identifier of the variable
	 * @return type of the variable, MODULE_VARIABLE if there is no information
	 *         of the stack
	 */
	public VariableType getType(String variable) {
		ScopeStackElement el = top;
		while (el != null)
			if (el.map.containsKey(variable))
				return el.map.get(variable).type;
			else
				el = el.parent;
		return VariableType.MODULE_VARIABLE;
	}

	/**
	 * Returns local number identifier for variable identified by this
	 * identifier.
	 * 
	 * @param variable
	 *            Identifier of the variable
	 * @return number to the locals array, or -1 if it is not a local variable
	 */
	public int getLocal(String variable) {
		ScopeStackElement el = top;
		while (el != null)
			if (el.map.containsKey(variable))
				return el.map.get(variable).locals;
			else
				el = el.parent;
		return -1;
	}

	/**
	 * Pops the top of the var stack.
	 * 
	 * @throws CompilationException
	 */
	public void popStack() throws CompilationException {
		if (top == null)
			throw new CompilationException(
					"Variable Scope empty but popStack called!");
		top = top.parent;
	}
}
