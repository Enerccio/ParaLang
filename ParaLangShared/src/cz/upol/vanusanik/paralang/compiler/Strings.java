package cz.upol.vanusanik.paralang.compiler;

public class Strings {

	public static final String CLASS_BASE_CLASS = "cz.upol.vanusanik.paralang.runtime.PLClass";
	public static final String MODULE_BASE_CLASS = "cz.upol.vanusanik.paralang.runtime.PLModule";
	
	public static final String BASE_COMPILED_STUB = "cz/upol/vanusanik/paralang/runtime/BaseCompiledStub";
	public static final String BASE_COMPILED_STUB_L = "L" + BASE_COMPILED_STUB + ";";
	public static final String BASE_COMPILED_STUB__SETKEY = "___setkey";
	public static final String BASE_COMPILED_STUB__GETKEY = "___getkey";
	public static final String BASE_COMPILED_STUB__GET_RUNTIME = "___get_runtime";
	public static final String BASE_COMPILED_STUB__GET_THIS = "___get_this";
	public static final String BASE_COMPILED_STUB__RESTRICTED_OVERRIDE = "___restrictedOverride";
	public static final String BASE_COMPILED_STUB__CONVERT_BOOLEAN = "___convertBoolean";
	
	public static final String PL_MODULE = "cz/upol/vanusanik/paralang/runtime/PLModule";
	public static final String PL_MODULE_L = "L" + PL_MODULE + ";";
	public static final String PL_CLASS = "cz/upol/vanusanik/paralang/runtime/PLClass";
	public static final String PL_CLASS_L = "L" + PL_CLASS + ";";
	public static final String PL_CLASS__SET_DERIVED_CLASS = "___setDerivedClass";
	public static final String BASE_CLASS = "cz/upol/vanusanik/paralang/runtime/BaseClass";
	public static final String BASE_CLASS_L = "L" + BASE_CLASS + ";";
	
	public static final String RUNTIME = "cz/upol/vanusanik/paralang/runtime/PLRuntime";
	public static final String RUNTIME_L = "L" + RUNTIME + ";";
	public static final String RUNTIME_N = "cz.upol.vanusanik.paralang.runtime.PLRuntime";
	public static final String RUNTIME__CHECK_RESTRICTED_ACCESS = "checkRestrictedAccess";
	public static final String RUNTIME__GET_MODULE = "getModule";
	public static final String RUNTIME__NEW_INSTANCE = "newInstance";
	public static final String RUNTIME__WRAP_JAVA_OBJECT = "wrapJavaObject";
	public static final String RUNTIME__RUN = "run";
	public static final String RUNTIME__RUN_JAVA_WRAPPER = "runJavaWrapper";
	public static final String RUNTIME__CHECK_EXCEPTION_HIERARCHY = "checkExceptionHierarchy";
	public static final String TYPEOPS__PLUS = "plus";
	public static final String TYPEOPS__MINUS = "minus";
	public static final String TYPEOPS__MUL = "mul";
	public static final String TYPEOPS__DIV = "div";
	public static final String TYPEOPS__MOD = "mod";
	public static final String TYPEOPS__LSHIFT = "lshift";
	public static final String TYPEOPS__RSHIFT = "rshift";
	public static final String TYPEOPS__RUSHIFT = "rushift";
	public static final String TYPEOPS__BITOR = "bitor";
	public static final String TYPEOPS__BITAND = "bitand";
	public static final String TYPEOPS__BITXOR = "bitxor";
	public static final String TYPEOPS__EQ = "eq";
	public static final String TYPEOPS__NEQ = "neq";
	public static final String TYPEOPS__MORE = "more";
	public static final String TYPEOPS__LESS = "less";
	public static final String TYPEOPS__LEQ = "leq";
	public static final String TYPEOPS__MEQ = "meq";
	public static final String TYPEOPS__LEFTPLUSPLUS = "lplusplus";
	public static final String TYPEOPS__LEFTMINUSMINUS = "lminusminus";
	public static final String TYPEOPS__UNARY_PLUS = "uplus";
	public static final String TYPEOPS__UNARY_MINUS = "uminus";
	public static final String TYPEOPS__UNARY_LNEG = "ulneg";
	public static final String TYPEOPS__UNARY_BNEG = "ubneg";
	
	public static final String TYPEOPS = "cz/upol/vanusanik/paralang/plang/types/TypeOperations";
	public static final String TYPEOPS__CONVERT_TO_BOOLEAN = "convertToBoolean";
	
	public static final String PLANGOBJECT = "cz/upol/vanusanik/paralang/plang/PLangObject";
	public static final String PLANGOBJECT_L = "L" + PLANGOBJECT + ";";
	public static final String PLANGOBJECT_N = "cz.upol.vanusanik.paralang.plang.PLangObject";
	public static final String PLANGOBJECT__GET_LOWEST_CLASSDEF = "__getLowestClassdef";
	public static final String NONETYPE = "cz/upol/vanusanik/paralang/plang/types/NoValue";
	public static final String NONETYPE_L = "L" + NONETYPE + ";";
	public static final String INT = "cz/upol/vanusanik/paralang/plang/types/Int";
	public static final String INT_L = "L" + INT + ";"; 
	public static final String FLOAT = "cz/upol/vanusanik/paralang/plang/types/Flt";
	public static final String FLOAT_L = "L" + FLOAT + ";";
	public static final String STRING_TYPE = "cz/upol/vanusanik/paralang/plang/types/Str";
	public static final String STRING_TYPE_L = "L" + STRING_TYPE + ";";
	public static final String BOOLEAN_VALUE = "cz/upol/vanusanik/paralang/plang/types/BooleanValue";
	public static final String BOOLEAN_VALUE_L = "L" + BOOLEAN_VALUE + ";";
	public static final String FUNCTION_WRAPPER = "cz/upol/vanusanik/paralang/plang/types/FunctionWrapper";
	public static final String FUNCTION_WRAPPER_L = "L" + FUNCTION_WRAPPER + ";";
	public static final String POINTER = "cz/upol/vanusanik/paralang/plang/types/Pointer";
	public static final String POINTER_L = "L" + POINTER + ";";
	
	public static final String STRING_L = "Ljava/lang/String;";
	public static final String OBJECT_L = "Ljava/lang/Object;";
	
	public static final String SERIALIZABLE = "java/io/Serializable";
	public static final String SERIALIZATION_UID = "serialVersionUID";
	public static final String THROWABLE = "java/lang/Throwable";
}
