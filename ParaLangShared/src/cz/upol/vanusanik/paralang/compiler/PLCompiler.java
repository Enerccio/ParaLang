package cz.upol.vanusanik.paralang.compiler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.BootstrapMethodsAttribute;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.ExceptionTable;
import javassist.bytecode.LineNumberAttribute;
import javassist.bytecode.Mnemonic;
import javassist.bytecode.Opcode;
import javassist.bytecode.SourceFileAttribute;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.patrikdufresne.util.BidiMultiHashMap;
import com.patrikdufresne.util.BidiMultiMap;

import cz.upol.vanusanik.paralang.compiler.VariableScopeStack.VariableType;
import cz.upol.vanusanik.paralang.plang.PLangLexer;
import cz.upol.vanusanik.paralang.plang.PLangParser;
import cz.upol.vanusanik.paralang.plang.PLangParser.BlockContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.BlockStatementContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.CatchClauseContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ClassBodyDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ClassDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.CompilationUnitContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ExpressionContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ExpressionListContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ExtendedContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.FieldDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ForControlContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.FormalParameterContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.FormalParametersContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.FunctionBodyContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.FunctionDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ImportDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.LiteralContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ModuleDeclarationsContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.PrimaryContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.StatementContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.VariableDeclaratorContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.VariableDeclaratorsContext;
import cz.upol.vanusanik.paralang.runtime.BaseClass;
import cz.upol.vanusanik.paralang.runtime.PLModule;
import cz.upol.vanusanik.paralang.runtime.PLRuntime;
import cz.upol.vanusanik.paralang.utils.Utils;

/**
 * Main compiler. Compiles source file designator into java modules bound to current runtime.
 * Needs a bound runtime via PLRuntime.setAsCurrent()
 * @author Enerccio
 *
 */
public class PLCompiler {
	
	/** Holds the references to a simple type, ie String -> Reference */
	private Map<String, Reference> referenceMap = new HashMap<String, Reference>();
	/** Source file name */
	private String source;
	/** Current module name */
	private String moduleName;
	
	/**
	 * Compiles file and loads it into current runtime
	 * @param in
	 * @return
	 * @throws Exception 
	 */
	public String compile(FileDesignator in) throws Exception{
		try {
			return compileFile(in);
		} catch (Exception e) {
			throw e;
		}
	}
	
	private String compileFile(FileDesignator in) throws Exception {
		// Parse the input into AST
		CompilationUnitContext ctx = parse(in);
		
		// build all references so compilation knows where to look for types
		buildReferenced(ctx);
		
		source = in.getSource();
		
		// compile the module and load it into runtime
		compileModule(ctx, in);
		return moduleName;
	}

	/**
	 * Loads the references from the header of file into map
	 * @param ctx
	 */
	private void buildReferenced(CompilationUnitContext ctx) {
		moduleName = ctx.moduleDeclaration().Identifier().getText();
		varStack = new VariableScopeStack();
		
		List<ImportDeclarationContext> idc = ctx.importDeclaration();
		if (idc != null){
			for (ImportDeclarationContext id : idc){
			Reference r = null;
				String name = null;
				if (id.getText().startsWith("import")){
					// Java import references
					String qId = id.qualifiedName().getText();
					name = qId;
					if (qId.contains("."))
						name = StringUtils.substringAfterLast(qId, ".");
					if (id.Identifier() != null)
						name = id.Identifier().getText();
					r = new Reference(qId, name, true);
				} else {
					// PLang import references (using keyword)
					String qId = id.singleQualifiedName().getText();
					name = qId;
					if (qId.contains("."))
						name = StringUtils.substringAfterLast(qId, ".");
					if (id.Identifier() != null)
						name = id.Identifier().getText();
					r = new Reference(qId, name, false);
				}
				referenceMap.put(name, r);
			}
		}
		
		// Add all classes declared in this module into references for self references to it's own types
		for (ModuleDeclarationsContext mdc : ctx.moduleDeclaration().moduleDeclarations()){
			if (mdc.classDeclaration() != null){
				String name = mdc.classDeclaration().children.get(1).getText();
				Reference r = new Reference(moduleName + "." + name, name, false);
				referenceMap.put(name, r);
			}
		}
		
		// System classes are always referenced 
		for (String cn : PLRuntime.SYSTEM_CLASSES.keySet()){
			Reference r = new Reference("System." + cn, cn, false);
			referenceMap.put(cn, r);
		}
	}
	
	private class ThrowingErrorListener extends BaseErrorListener {
		   @Override
		   public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
		      throws ParseCancellationException {
		         throw new ParseCancellationException("file " + source + " line " + line + ":" + charPositionInLine + " " + msg);
		   }
	}

	/**
	 * Parses FileDesignator into AST
	 * @param in
	 * @return
	 * @throws Exception
	 */
	private CompilationUnitContext parse(FileDesignator in) throws Exception {
		ANTLRInputStream is = new ANTLRInputStream(in.getStream());
		PLangLexer lexer = new PLangLexer(is);
		lexer.removeErrorListeners();
		lexer.addErrorListener(new ThrowingErrorListener());
		CommonTokenStream stream = new CommonTokenStream(lexer);
		PLangParser parser = new PLangParser(stream);
		parser.removeErrorListeners();
		parser.addErrorListener(new ThrowingErrorListener());
		return parser.compilationUnit();
	}

	/**
	 * Compiles module and class declarations
	 * @param ctx
	 * @param in
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private void compileModule(CompilationUnitContext ctx, FileDesignator in) throws Exception{
		
		for (ModuleDeclarationsContext mdc : ctx.moduleDeclaration().moduleDeclarations()){
			if (mdc.classDeclaration() != null){
				// Compile class definition
				Class<?> klazz = compileClassDefinition(ctx.moduleDeclaration().Identifier().getText(), mdc.classDeclaration(), in);
				PLRuntime.getRuntime().registerClass(moduleName, mdc.classDeclaration().children.get(1).getText(), klazz);
			}
		}
		
		// Compile module definition
		Class<?> klazz = compileModuleClass(ctx, in);
		PLRuntime.getRuntime().addModule(moduleName, (Class<? extends PLModule>) klazz);
	}
	
	/* Label handling, labelCounter provides unique label ids, then map maps it into current bytecode */
	/** Unique label counter generator */
	private int labelCounter;
	/** Map of label ids -> bytecode */
	private Map<Integer, Integer> labelMap;
	
	/** Whether it is currently class being compiled */
	private boolean compilingClass = false;
	/** Class pool */
	private ClassPool cp;
	/** Line numbers are written into this stream as bytes */
	private DataOutputStream lineNumberStream;
	/** Last line written into the lineNumberStream */
	private int lastLineWritten;
	/** CtClass of the class/module being compiled */
	private CtClass cls;
	/** Current bytecode being created */
	private Bytecode bc;
	/** Const pool of currently compiled object */
	private ConstPool pool; 
	/** Local variables stacker providing how many locals are being used and reused */
	private AutoIntStacker stacker;
	/** String cache caching strings into consts */
	private HashMap<String, Integer> cache;
	/** Variable Scope is decided by this stack, decides whether variable is local, class or module */
	private VariableScopeStack varStack;
	private enum RestrictedTo { CLASS, MODULE };
	private RestrictedTo isRestrictedMethodQualifier;
	
	private static class BlockDescription {
		public BlockContext b;
		public String mn;
	}
	
	/** List of blocks of codes that are in the distributed expression, which will be later compiled into auxiliary methods */
	private List<BlockDescription> distributed = new ArrayList<BlockDescription>();
	
	/**
	 * Compiles module into java class
	 * @param ctx
	 * @param in
	 * @return
	 * @throws Exception
	 */
	private Class<?> compileModuleClass(CompilationUnitContext ctx, FileDesignator in) throws Exception {
		String moduleName = ctx.moduleDeclaration().Identifier().getText();
		compilingClass = false;
		distributed.clear();
		
		cp = new ClassPool();
		cp.appendSystemPath();

		cache = new HashMap<String, Integer>();
		String className = moduleName;
		
		cls = cp.makeClass(className);
		cls.setSuperclass(cp.getCtClass(Strings.MODULE_BASE_CLASS));
		// Serialization
		cls.addInterface(cp.getCtClass(Strings.SERIALIZABLE));
		
		genBootstraps();
		
		CtField f = new CtField(CtClass.longType, Strings.SERIALIZATION_UID, cls);
		f.setModifiers(Modifier.STATIC | Modifier.FINAL);
		cls.addField(f, "" + PLRuntime.getRuntime().getUuid(className));
		
		CtConstructor ct = CtNewConstructor.defaultConstructor(cls);
		cls.addConstructor(ct);
		
		CtMethod serM = CtNewMethod.make("private java.lang.Object readResolve() "
				+ "{ return cz.upol.vanusanik.paralang.runtime.PLRuntime.getRuntime()"
					+ ".resolveModule(\""+ moduleName + "\"); }", cls);
		cls.addMethod(serM);
		
		final List<FieldDeclarationContext> fields = new ArrayList<FieldDeclarationContext>();
		// List all fields
		for (ModuleDeclarationsContext mdc : ctx.moduleDeclaration().moduleDeclarations()){
			FieldDeclarationContext fdc = mdc.fieldDeclaration();
			if (fdc != null)
				fields.add(fdc);
		}
		
		varStack.pushNewStack(); // push module variables
		varStack.addVariable("init", VariableType.CLASS_VARIABLE);
		
		// Find all methods
		final Set<String> methods = new HashSet<String>();
		for (ModuleDeclarationsContext mdc : ctx.moduleDeclaration().moduleDeclarations()){
			if (mdc.functionDeclaration() != null){
				FunctionDeclarationContext fcx = mdc.functionDeclaration();
				boolean restricted = fcx.getChild(1).getText().startsWith("restricted");
				String name = restricted ? fcx.getChild(2).getText() : fcx.getChild(1).getText();
				if (methods.contains(name))
					throw new CompilationException("Already containing function " + name);
				methods.add(name);
			}
		}
		
		if (!methods.contains("init")){
			CtMethod initM = CtNewMethod.make("public cz.upol.vanusanik.paralang.plang.PLangObject init() { return cz.upol.vanusanik.paralang.plang.types.NoValue.NOVALUE; }", cls);
			cls.addMethod(initM);
			methods.add("init");
		}
		
		for (FieldDeclarationContext fdc : fields){
			for (final VariableDeclaratorContext vd : fdc.variableDeclarators().variableDeclarator()){
				String varId = vd.variableDeclaratorId().getText();
				varStack.addVariable(varId, VariableType.CLASS_VARIABLE);
			}
		}
		
		// Compile all methods
		for (ModuleDeclarationsContext mdc : ctx.moduleDeclaration().moduleDeclarations()){
			if (mdc.functionDeclaration() != null){
				FunctionDeclarationContext fcx = mdc.functionDeclaration();
				compileFunction(fcx);
			}
		}
		
		// Compile distributed auxiliary methods
		for (BlockDescription bd : distributed){
			methods.add(compileFunction(bd));
		}
		
		// Compile system init method
		final CtMethod m = CtNewMethod.make("protected void ___init_internal_datafields(cz.upol.vanusanik.paralang.runtime.BaseCompiledStub self) {  }", cls);
		new MethodCompiler(m){

			@Override
			protected void compileDataSources() throws Exception {
				isRestrictedMethodQualifier = RestrictedTo.MODULE; // default fields are always in restricted mode check off
				compileInitMethod(fields, methods, null);
			}
			
		}.compileMethod();
		
		varStack.popStack(); // pop class variable
		cls.debugWriteFile();
		byte[] bytedata = cls.toBytecode();
		PLRuntime.getRuntime().addModuleBytedata(moduleName, bytedata);
		return cls.toClass(getClassLoader(), null);
	}
	

	/**
	 * Compile plang class into java class
	 * @param moduleName
	 * @param classDeclaration
	 * @param in
	 * @return
	 * @throws Exception
	 */
	private Class<?> compileClassDefinition(String moduleName, ClassDeclarationContext classDeclaration, FileDesignator in) throws Exception {
		compilingClass = true;
		distributed.clear();
		
		cp = new ClassPool();
		cp.appendSystemPath();
		
		cache = new HashMap<String, Integer>();
		String clsName = classDeclaration.children.get(1).getText();
		String className = moduleName + "$" + clsName;
		
		cls = cp.makeClass(className);
		cls.setSuperclass(cp.getCtClass(Strings.CLASS_BASE_CLASS));
		
		String superClass = null;
		if (classDeclaration.type() != null)
			superClass = classDeclaration.type().getText();
		final String sc = superClass;
		
		// Serialization
		cls.addInterface(cp.getCtClass(Strings.SERIALIZABLE));
		CtField f = new CtField(CtClass.longType, Strings.SERIALIZATION_UID, cls);
		f.setModifiers(Modifier.STATIC | Modifier.FINAL);
		cls.addField(f, "" + PLRuntime.getRuntime().getUuid(className));
		
		genBootstraps();
		
		CtConstructor ct = CtNewConstructor.defaultConstructor(cls);
		cls.addConstructor(ct);
		
		List<ClassBodyDeclarationContext> cbdList = classDeclaration.classBody().classBodyDeclaration();
		
		final List<FieldDeclarationContext> fields = new ArrayList<FieldDeclarationContext>();
		// List all fields
		for (ClassBodyDeclarationContext cbd : cbdList){
			FieldDeclarationContext fdc = cbd.memberDeclaration().fieldDeclaration();
			if (fdc != null)
				fields.add(fdc);
		}
		
		varStack.pushNewStack(); // push class variables
		varStack.addVariable("init", VariableType.CLASS_VARIABLE);
		
		// Find all methods
		final Set<String> methods = new HashSet<String>();
		for (ClassBodyDeclarationContext cbd : cbdList){
			if (cbd.memberDeclaration().functionDeclaration() != null){
				FunctionDeclarationContext fcx = cbd.memberDeclaration().functionDeclaration();
				boolean restricted = fcx.getChild(1).getText().equals("restricted");
				String name = restricted ? fcx.getChild(2).getText() : fcx.getChild(1).getText();
				if (methods.contains(name))
					throw new CompilationException("Already containing function " + name);
				methods.add(name);
			}
		}
		
		for (FieldDeclarationContext fdc : fields){
			for (final VariableDeclaratorContext vd : fdc.variableDeclarators().variableDeclarator()){
				String varId = vd.variableDeclaratorId().getText();
				varStack.addVariable(varId, VariableType.CLASS_VARIABLE);
			}
		}
		
		// Compile all methods
		for (ClassBodyDeclarationContext cbd : cbdList){
			if (cbd.memberDeclaration().functionDeclaration() != null){
				FunctionDeclarationContext fcx = cbd.memberDeclaration().functionDeclaration();
				compileFunction(fcx);
			}
		}
		
		for (BlockDescription bd : distributed){
			methods.add(compileFunction(bd));
		}

		// Compile system init method
		
		final CtMethod m = CtNewMethod.make("protected void ___init_internal_datafields(cz.upol.vanusanik.paralang.runtime.BaseCompiledStub self) { }", cls);
		new MethodCompiler(m){

			@Override
			protected void compileDataSources() throws Exception {
				isRestrictedMethodQualifier = RestrictedTo.CLASS; // default fields are always in restricted mode check off
				compileInitMethod(fields, methods, sc);
			}
			
		}.compileMethod();		
		
		varStack.popStack(); // pop class variables
		cls.debugWriteFile();
		byte[] bytedata = cls.toBytecode();
		PLRuntime.getRuntime().addClassBytedata(moduleName, clsName, bytedata);
		return cls.toClass(getClassLoader(), null);
	}
	
	/**
	 * Generates handlers for boostrap for dynamic methods of operators. 
	 * Needed to be created once per java class
	 */
	private void genBootstraps() {
		pool = cls.getClassFile().getConstPool();
		// 0 binary operation
		int mRefIdxBin = pool.addMethodrefInfo(pool.addClassInfo(Strings.TYPEOPS), pool.addNameAndTypeInfo("binopbootstrap", 
				"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
				+ ")Ljava/lang/invoke/CallSite;"));
		int mHandleIdxBin = pool.addMethodHandleInfo(ConstPool.REF_invokeStatic, mRefIdxBin);
		// 1 unary operation
		int mRefIdxUn = pool.addMethodrefInfo(pool.addClassInfo(Strings.TYPEOPS), pool.addNameAndTypeInfo("unopbootstrap", 
				"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
				+ ")Ljava/lang/invoke/CallSite;"));
		int mHandleIdxUn = pool.addMethodHandleInfo(ConstPool.REF_invokeStatic, mRefIdxUn);
		
		/* create bootstrap methods attribute; there can only be one per class file! */
		BootstrapMethodsAttribute.BootstrapMethod[] bms = new BootstrapMethodsAttribute.BootstrapMethod[] {
				new BootstrapMethodsAttribute.BootstrapMethod(mHandleIdxBin, new int[] {}),
				new BootstrapMethodsAttribute.BootstrapMethod(mHandleIdxUn, new int[] {})
		};
		BootstrapMethodsAttribute bmsAttribute = new BootstrapMethodsAttribute(pool, bms);
		cls.getClassFile().addAttribute(bmsAttribute);
	}

	/** Whether init method is being compiled */
	private boolean compilingInit;
	/**
	 * Compiles function/method of module/class.
	 * @param fcx
	 * @return
	 * @throws Exception
	 */
	private String compileFunction(final FunctionDeclarationContext fcx) throws Exception{
		final boolean restricted = fcx.getChild(1).getText().startsWith("restricted");
		String name = restricted ? fcx.getChild(2).getText() : fcx.getChild(1).getText();
		
		isRestrictedMethodQualifier = restricted ? RestrictedTo.MODULE : null;
		if (name.equals("init")){
			isRestrictedMethodQualifier = compilingClass ? RestrictedTo.CLASS : RestrictedTo.MODULE;
			compilingInit = true;
		} else
			compilingInit = false;
		
		FormalParametersContext fpx = fcx.formalParameters();
		final List<String> args = new ArrayList<String>();
		if (compilingClass) // if class is being compiled, it's method have implicit "inst" (this)
			args.add("inst");
		if (fpx.formalParameterList() != null){
			for (FormalParameterContext fpcx : fpx.formalParameterList().formalParameter()){
				args.add(fpcx.getText());
			}
		}
		String methArgsSign = "";
		for (String arg : args){
			if (arg.endsWith("...")){
				methArgsSign += Strings.PLANGOBJECT_N + "... " + arg.replace("...", "");
				break;
			} else {
				methArgsSign += Strings.PLANGOBJECT_N + " " + arg + ", ";
			}
		}
		
		methArgsSign = StringUtils.removeEnd(methArgsSign, ", ");
		
		String methodSignature = "public " + Strings.PLANGOBJECT_N + " " + name + "(" + methArgsSign + "){ return null; }";
		
		final CtMethod m = CtNewMethod.make(methodSignature, cls);
		new MethodCompiler(m){

			@Override
			protected void compileDataSources() throws Exception {
				if (compilingInit){
					bc.addAload(0);  // load this
					bc.addIconst(1); // add true
					bc.addPutfield(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__RESTRICTED_OVERRIDE, "Z");
				}
				
				varStack.pushNewStack();
				for (String arg : args){
					varStack.addVariable(arg, VariableType.LOCAL_VARIABLE, stacker.acquire());
				}
				compileFunction(fcx.functionBody(), restricted);
				varStack.popStack();
			}
			
		}.compileMethod();
		
		return name;
		
	}
	
	private String compileFunction(final BlockDescription bd) throws Exception{
		final boolean restricted = false;
		String name = bd.mn;
		
		isRestrictedMethodQualifier = restricted ? RestrictedTo.MODULE : null;
		if (name.equals("init")){
			isRestrictedMethodQualifier = compilingClass ? RestrictedTo.CLASS : RestrictedTo.MODULE;
			compilingInit = true;
		} else
			compilingInit = false;
		String methArgsSign;
		if (compilingClass)
			methArgsSign = Strings.PLANGOBJECT_N + " inst, " + Strings.PLANGOBJECT_N + " run_id, " + Strings.PLANGOBJECT_N + " passed_arg";
		else
			methArgsSign = Strings.PLANGOBJECT_N + " run_id, " + Strings.PLANGOBJECT_N + " passed_arg";
		methArgsSign = StringUtils.removeEnd(methArgsSign, ", ");
		String methodSignature = "public " + Strings.PLANGOBJECT_N + " " + name + "(" + methArgsSign + "){ return null; }";
		
		final CtMethod m = CtNewMethod.make(methodSignature, cls);
		new MethodCompiler(m){

			@Override
			protected void compileDataSources() throws Exception {
				varStack.pushNewStack();
				if (compilingClass){
					varStack.addVariable("inst", VariableType.LOCAL_VARIABLE, stacker.acquire());
				}
				varStack.addVariable("run_id", VariableType.LOCAL_VARIABLE, stacker.acquire());
				varStack.addVariable("passed_arg", VariableType.LOCAL_VARIABLE, stacker.acquire());
				compileFunction(bd.b, restricted);
				varStack.popStack();
			}
			
		}.compileMethod();
		
		return name;
	}

	private List<FinallyBlockProtocol> fbcList;
	protected void compileFunction(FunctionBodyContext functionBody, boolean restricted) throws Exception {
		compileFunction(functionBody.block(), restricted);
	}
	
	protected void compileFunction(BlockContext b, boolean restricted) throws Exception {
		markLine(bc.currentPc(), b.start.getLine());
		
		if (restricted){
			addGetRuntime();
			bc.addAload(0);
			bc.addCheckcast(Strings.BASE_COMPILED_STUB);
			bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__CHECK_RESTRICTED_ACCESS, "(" + Strings.BASE_COMPILED_STUB_L + ")V");
		}
		
		fbcList = new LinkedList<FinallyBlockProtocol>();
		fbcLoopList = new LinkedList<FinallyBlockProtocol>();
		
		compileBlock(b);	
		
		markLine(bc.currentPc(), b.stop.getLine());
		functionExitProtocol();
		addNil();
		bc.add(Opcode.ARETURN);
	}
	
	private static interface FinallyBlockProtocol {
		public void doCompile() throws Exception;
	}
	
	private void breakContinueExitProtocol() throws Exception {
		List<FinallyBlockProtocol> copy = new ArrayList<FinallyBlockProtocol>(fbcLoopList);
		Collections.reverse(copy);
		for (FinallyBlockProtocol fbc : copy){
			if (fbc == null) break;
			fbc.doCompile();
		}
		return;
		
	}

	private void functionExitProtocol() throws Exception {
		if (!compilingInit){
			fbcLoopList.add(null);
			List<FinallyBlockProtocol> copy = new ArrayList<FinallyBlockProtocol>(fbcList);
			Collections.reverse(copy);
			for (FinallyBlockProtocol fbc : copy)
				fbc.doCompile();
			fbcLoopList.remove(fbcLoopList.size()-1);
			return;
		}
		
		bc.addAload(0);  // load this
		bc.addIconst(0); // add false
		bc.addPutfield(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__RESTRICTED_OVERRIDE, "Z");
	}

	private void compileBlock(BlockContext block) throws Exception {
		varStack.pushNewStack();
		int pushCount = 0;
		for (BlockStatementContext bscx : block.blockStatement()){
			if (bscx.localVariableDeclarationStatement() != null){
				List<VariableDeclaratorContext> decls = bscx.localVariableDeclarationStatement().localVariableDeclaration().variableDeclarators().variableDeclarator();
				for (VariableDeclaratorContext vd : decls){
					markLine(bc.currentPc(), vd.start.getLine());
					String varId = vd.variableDeclaratorId().getText();
					int localId = stacker.acquire();
					++pushCount;
					
					if (vd.variableInitializer() != null){
						isStatementExpression.add(false);
						compileExpression(vd.variableInitializer().expression(), false, -1);
						isStatementExpression.pop();
					} else {
						addNil();
					}
					bc.addAstore(localId);
					
					varStack.addVariable(varId, VariableType.LOCAL_VARIABLE, localId);
				}
			}
			if (bscx.statement() != null)
				compileStatement(bscx.statement());
		}
		while (pushCount-- != 0)
			stacker.release();
		varStack.popStack();
	}

	private Stack<Boolean> isStatementExpression = new Stack<Boolean>();
	private Stack<Integer> continueStack = new Stack<Integer>();
	private Stack<Integer> breakStack = new Stack<Integer>();
	private List<FinallyBlockProtocol> fbcLoopList;
	private void compileStatement(final StatementContext statement) throws Exception {
		markLine(bc.currentPc(), statement.start.getLine());
		
		if (statement.continueStatement() != null){
			if (continueStack.empty())
				throw new CompilationException("Continue not inside any loop");
			
			breakContinueExitProtocol();
			int label = continueStack.peek();
			addLabel(new LabelInfo(){

				@Override
				protected void add(Bytecode bc) throws CompilationException {
					int offset = getValue(poskey) - bcpos;
					if (Math.abs(offset) > (65535/2)){
						throw new CompilationException("Too long jump. Please reformate the code!");
					} else {
						bc.write(bcpos, Opcode.GOTO);
						bc.write16bit(bcpos+1, offset);
					}
				}
				
			}, label);
			
			return;
		}
		if (statement.breakStatement() != null){
			if (breakStack.empty())
				throw new CompilationException("Break not inside any loop");
			
			breakContinueExitProtocol();
			int label = breakStack.peek();
			addLabel(new LabelInfo(){

				@Override
				protected void add(Bytecode bc) throws CompilationException {
					int offset = getValue(poskey) - bcpos;
					if (Math.abs(offset) > (65535/2)){
						throw new CompilationException("Too long jump. Please reformate the code!");
					} else {
						bc.write(bcpos, Opcode.GOTO);
						bc.write16bit(bcpos+1, offset);
					}
				}
				
			}, label);
			
			return;
		}
		if (statement.forStatement() != null){
			ForControlContext fcc = statement.forStatement().forControl();
			
			int loopStart = labelCounter++;
			int loopEnd = labelCounter++;
			int continueLoop = labelCounter++;
			
			varStack.pushNewStack();
			int pushCount = 0;
			
			List<VariableDeclaratorContext> decls;
			
			if (fcc.forInit() != null && fcc.forInit().localVariableDeclaration() != null){
				decls = fcc.forInit().localVariableDeclaration().variableDeclarators().variableDeclarator();
			} else
				decls = new ArrayList<VariableDeclaratorContext>();
			for (VariableDeclaratorContext vd : decls){
				markLine(bc.currentPc(), vd.start.getLine());
				String varId = vd.variableDeclaratorId().getText();
				int localId = stacker.acquire();
				++pushCount;
				
				if (vd.variableInitializer() != null){
					isStatementExpression.add(false);
					compileExpression(vd.variableInitializer().expression(), false, -1);
					isStatementExpression.pop();
				} else {
					addNil();
				}
				bc.addAstore(localId);
				
				varStack.addVariable(varId, VariableType.LOCAL_VARIABLE, localId);
			}
			
			if (fcc.forInit().expressionList() != null){
				ExpressionListContext el = fcc.forInit().expressionList();
				for (ExpressionContext ex : el.expression()){
					isStatementExpression.add(true);
					compileExpression(ex, false, -1);
					isStatementExpression.pop();
				}
			}
			
			setLabelPos(loopStart);
			
			if (fcc.expression() != null){
				isStatementExpression.add(false);
				compileExpression(fcc.expression(), false, -1);
				isStatementExpression.pop();
				bc.addInvokestatic(Strings.TYPEOPS, Strings.TYPEOPS__CONVERT_TO_BOOLEAN, 
						"("+ Strings.PLANGOBJECT_L + ")Z"); // boolean on stack
				addLabel(new LabelInfo(){

					@Override
					protected void add(Bytecode bc) throws CompilationException {
						int offset = getValue(poskey) - bcpos;
						bc.write(bcpos, Opcode.IFEQ); // jump to else if true or to the next bytecode if not 
						bc.write16bit(bcpos+1, offset);
					}
					
				}, loopEnd);
			}
			
			breakStack.add(loopEnd);
			continueStack.add(continueLoop);
			fbcLoopList.add(null);
			compileStatement(statement.forStatement().statement());
			fbcLoopList.remove(fbcLoopList.size()-1);
			breakStack.pop();
			continueStack.pop();
			
			setLabelPos(continueLoop);
			
			if (fcc.forUpdate() != null){
				for (ExpressionContext ex : fcc.forUpdate().expressionList().expression()){
					isStatementExpression.add(true);
					compileExpression(ex, false, -1);
					isStatementExpression.pop();
				}
			}
			
			addLabel(new LabelInfo(){

				@Override
				protected void add(Bytecode bc) throws CompilationException {
					int offset = getValue(poskey) - bcpos;
					if (Math.abs(offset) > (65535/2)){
						throw new CompilationException("Too long jump. Please reformate the code!");
					} else {
						bc.write(bcpos, Opcode.GOTO);
						bc.write16bit(bcpos+1, offset);
					}
				}
				
			}, loopStart);
			
			while (pushCount-- != 0)
				stacker.release();
			varStack.popStack();

			setLabelPos(loopEnd);
			bc.add(Opcode.NOP);
			
			return;
		}
		if (statement.whileStatement() != null){
			
			int loopStart = labelCounter++;
			int loopEnd = labelCounter++;
			
			setLabelPos(loopStart);
			isStatementExpression.add(false);
			compileExpression(statement.whileStatement().parExpression().expression(), false, -1);
			isStatementExpression.pop();
			bc.addInvokestatic(Strings.TYPEOPS, Strings.TYPEOPS__CONVERT_TO_BOOLEAN, 
					"("+ Strings.PLANGOBJECT_L + ")Z"); // boolean on stack
			addLabel(new LabelInfo(){

				@Override
				protected void add(Bytecode bc) throws CompilationException {
					int offset = getValue(poskey) - bcpos;
					bc.write(bcpos, Opcode.IFEQ); // jump to else if true or to the next bytecode if not 
					bc.write16bit(bcpos+1, offset);
				}
				
			}, loopEnd);
			
			
			continueStack.add(loopStart);
			breakStack.add(loopEnd);
			
			fbcLoopList.add(null);
			compileStatement(statement.whileStatement().statement());
			fbcLoopList.remove(fbcLoopList.size()-1);
			addLabel(new LabelInfo(){

				@Override
				protected void add(Bytecode bc) throws CompilationException {
					int offset = getValue(poskey) - bcpos;
					if (Math.abs(offset) > (65535/2)){
						throw new CompilationException("Too long jump. Please reformate the code!");
					} else {
						bc.write(bcpos, Opcode.GOTO);
						bc.write16bit(bcpos+1, offset);
					}
				}
				
			}, loopStart);
			
			continueStack.pop();
			breakStack.pop();
			
			setLabelPos(loopEnd);
			bc.add(Opcode.NOP);
			return;
		}
		if (statement.doStatement() != null){
			
			int loopStart = labelCounter++;
			int loopEnd = labelCounter++;
			int loopBegin = labelCounter++;
			
			setLabelPos(loopBegin);
			
			continueStack.add(loopStart);
			breakStack.add(loopEnd);
			
			fbcLoopList.add(null);
			compileStatement(statement.doStatement().statement());
			fbcLoopList.remove(fbcLoopList.size()-1);
			addLabel(new LabelInfo(){

				@Override
				protected void add(Bytecode bc) throws CompilationException {
					int offset = getValue(poskey) - bcpos;
					if (Math.abs(offset) > (65535/2)){
						throw new CompilationException("Too long jump. Please reformate the code!");
					} else {
						bc.write(bcpos, Opcode.GOTO);
						bc.write16bit(bcpos+1, offset);
					}
				}
				
			}, loopStart);
			
			continueStack.pop();
			breakStack.pop();
			
			setLabelPos(loopStart);
			isStatementExpression.add(false);
			compileExpression(statement.doStatement().parExpression().expression(), false, -1);
			isStatementExpression.pop();
			bc.addInvokestatic(Strings.TYPEOPS, Strings.TYPEOPS__CONVERT_TO_BOOLEAN, 
					"("+ Strings.PLANGOBJECT_L + ")Z"); // boolean on stack
			addLabel(new LabelInfo(){

				@Override
				protected void add(Bytecode bc) throws CompilationException {
					int offset = getValue(poskey) - bcpos;
					bc.write(bcpos, Opcode.IFNE); // jump to else if true or to the next bytecode if not 
					bc.write16bit(bcpos+1, offset);
				}
				
			}, loopBegin);
			
			setLabelPos(loopEnd);
			bc.add(Opcode.NOP);
			return;
		}
		if (statement.tryStatement() != null){
			int endLabel = labelCounter++;
			boolean hasFinally = statement.tryStatement().finallyBlock() != null;
			
			final int finallyStack = stacker.acquire();
			final int throwableStack = stacker.acquire();
			
			FinallyBlockProtocol fbc = new FinallyBlockProtocol() {
				
				@Override
				public void doCompile() throws Exception {
					compileBlock(statement.tryStatement().finallyBlock().block());
				}
			};
			
			
			int start = bc.currentPc();
			if (hasFinally){
				fbcList.add(fbc);
				fbcLoopList.add(fbcList.get(fbcList.size()-1));
			}
			compileBlock(statement.tryStatement().block());
			if (hasFinally){
				fbcList.remove(fbcList.size()-1);
				fbcLoopList.remove(fbcLoopList.size()-1);
			}
			
			int end = bc.currentPc();
			if (hasFinally){
				compileBlock(statement.tryStatement().finallyBlock().block());
			}
			
			addLabel(new LabelInfo(){

				@Override
				protected void add(Bytecode bc) throws CompilationException {
					int offset = getValue(poskey) - bcpos;
					if (Math.abs(offset) > (65535/2)){
						throw new CompilationException("Too long jump. Please reformate the code!");
					} else {
						bc.write(bcpos, Opcode.GOTO);
						bc.write16bit(bcpos+1, offset);
					}
				}
				
			}, endLabel);
			
			int prevKey = -1;
			
			if (statement.tryStatement().catchClause() != null){
				int throwLabel = labelCounter++;
				bc.addExceptionHandler(start, end, bc.currentPc(), Strings.BASE_COMPILED_STUB);
				bc.addAstore(throwableStack); // save old exception
				
				Iterator<CatchClauseContext> ccit = statement.tryStatement().catchClause().iterator();
				while (ccit.hasNext()){
					CatchClauseContext ccc = ccit.next();
					String type = ccc.type().getText();
					String fqName = type;
					
					markLine(bc.currentPc(), ccc.start.getLine());
					
					if (referenceMap.containsKey(type)){
						Reference r = referenceMap.get(type);
						if (r.isJava())
							throw new CompilationException("Only PLang type can be in catch expression!");
						fqName = r.getFullReference();
					}
					
					String className = PLRuntime.getRuntime().getClassNameOrGuess(fqName);
					
					if (prevKey != -1)
						setLabelPos(prevKey);
					addGetRuntime();
					bc.addAload(throwableStack);
					bc.addLdc(cacheStrings(className));
					bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__CHECK_EXCEPTION_HIERARCHY, 
							"(" + Strings.PLANGOBJECT_L + Strings.STRING_L + ")Z");
					
					if (!ccit.hasNext()){
						// no other catch clauses, else should go to exit
						prevKey = throwLabel;
					} else {
						// else should go to next key pos
						prevKey = labelCounter++;
					}
					addLabel(new LabelInfo(){

						@Override
						protected void add(Bytecode bc) throws CompilationException {
							int offset = getValue(poskey) - bcpos;
							bc.write(bcpos, Opcode.IFEQ);
							bc.write16bit(bcpos+1, offset);
						}
						
					}, prevKey);
					
					varStack.addVariable(ccc.Identifier().getText(), VariableType.LOCAL_VARIABLE, throwableStack);
					int sstart = bc.currentPc();
					
					if (hasFinally){
						fbcList.add(fbc);
						fbcLoopList.add(fbcList.get(fbcList.size()-1));
					}
					compileBlock(ccc.block());
					if (hasFinally){
						fbcList.remove(fbcList.size()-1);
						fbcLoopList.remove(fbcLoopList.size()-1);
					}
					
					int ssend = bc.currentPc();
					addLabel(new LabelInfo(){

						@Override
						protected void add(Bytecode bc) throws CompilationException {
							int offset = getValue(poskey) - bcpos;
							if (Math.abs(offset) > (65535/2)){
								throw new CompilationException("Too long jump. Please reformate the code!");
							} else {
								bc.write(bcpos, Opcode.GOTO);
								bc.write16bit(bcpos+1, offset);
							}
						}
						
					}, endLabel);
					
					if (hasFinally){
						bc.addExceptionHandler(sstart, ssend, bc.currentPc(), Strings.THROWABLE);
						bc.addAstore(finallyStack);
						compileBlock(statement.tryStatement().finallyBlock().block());
						bc.addAload(finallyStack);
						bc.add(Opcode.ATHROW);
					}
					
				}
				setLabelPos(throwLabel);
				if (hasFinally)
					compileBlock(statement.tryStatement().finallyBlock().block());
				bc.addAload(throwableStack);
				bc.add(Opcode.ATHROW);
				addLabel(new LabelInfo(){

					@Override
					protected void add(Bytecode bc) throws CompilationException {
						int offset = getValue(poskey) - bcpos;
						if (Math.abs(offset) > (65535/2)){
							throw new CompilationException("Too long jump. Please reformate the code!");
						} else {
							bc.write(bcpos, Opcode.GOTO);
							bc.write16bit(bcpos+1, offset);
						}
					}
					
				}, endLabel);			
			}
			
			if (statement.tryStatement().finallyBlock() != null){
				bc.addExceptionHandler(start, end, bc.currentPc(), Strings.THROWABLE);
				bc.addAstore(finallyStack); // save throwable exception on stack
				
				compileBlock(statement.tryStatement().finallyBlock().block());
				
				bc.addAload(finallyStack);
				bc.add(Opcode.ATHROW);
			}
			
			setLabelPos(endLabel);
			bc.add(Opcode.NOP);

			stacker.release();
			stacker.release();
			
			return;
		}
		
		if (statement.block() != null){
			compileBlock(statement.block());
			return;
		}
		
		if (statement.returnStatement() != null){
			
			int local = stacker.acquire();
			
			if (statement.returnStatement().expression() != null){
				isStatementExpression.add(false);
				compileExpression(statement.returnStatement().expression(), false, -1);
				isStatementExpression.pop();
			} else {
				addNil();
			}
			
			bc.addAstore(local);
			
			functionExitProtocol();
			
			bc.addAload(local);
			bc.add(Opcode.ARETURN);
			stacker.release();
			return;
		}
		
		if (statement.throwStatement() != null){
			isStatementExpression.add(false);
			compileExpression(statement.throwStatement().expression(), false, -1);
			isStatementExpression.pop();
			bc.add(Opcode.ATHROW);
			return;
		}
		
		if (statement.statementExpression() != null){
			isStatementExpression.add(true);
			compileExpression(statement.statementExpression().expression(), false, -1);
			isStatementExpression.pop();
		} else if (statement.ifStatement() != null){
			ExpressionContext e = statement.ifStatement().parExpression().expression();
			isStatementExpression.add(false);
			compileExpression(e, false, -1);
			isStatementExpression.pop();
			bc.addInvokestatic(Strings.TYPEOPS, Strings.TYPEOPS__CONVERT_TO_BOOLEAN, 
					"("+ Strings.PLANGOBJECT_L + ")Z"); // boolean on stack
			boolean hasElse = statement.ifStatement().getChildCount() == 5;
			int key = labelCounter++;
			addLabel(new LabelInfo(){

				@Override
				protected void add(Bytecode bc) throws CompilationException {
					int offset = getValue(poskey) - bcpos;
					bc.write(bcpos, Opcode.IFEQ); // jump to else if true or to the next bytecode if not 
					bc.write16bit(bcpos+1, offset);
				}
				
			}, key);
			int key2 = -1;
			
			compileStatement((StatementContext) statement.ifStatement().getChild(2));
			if (hasElse){
				key2 = labelCounter++;
				addLabel(new LabelInfo(){

					@Override
					protected void add(Bytecode bc) throws CompilationException {
						int offset = getValue(poskey) - bcpos;
						if (Math.abs(offset) > (65535/2)){
							throw new CompilationException("Too long jump. Please reformate the code!");
						} else {
							bc.write(bcpos, Opcode.GOTO);
							bc.write16bit(bcpos+1, offset);
						}
					}
					
				}, key2);
			}
			
			setLabelPos(key);
			
			if (hasElse){
				compileStatement((StatementContext) statement.ifStatement().getChild(4));
				setLabelPos(key2);
			}
			bc.add(Opcode.NOP);
		}
	}

	private void addGetRuntime() {
		bc.addAload(0);		// load this
		bc.addInvokevirtual(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__GET_RUNTIME, 
				"()" + Strings.RUNTIME_L); // call to __get_runtime, PLRuntime is on stack
	}

	private boolean cmpInitFuncwraps = false;
	private void compileInitMethod(List<FieldDeclarationContext> fields, Set<String> methods, final String superClass) throws Exception{
		stacker.acquire();
		if (compilingClass)
			new StoreToField(BaseClass.___superKey){
	
				@Override
				protected void provideSourceValue() throws Exception {
					
					if (superClass == null || superClass.equals("BaseClass")){
						bc.addNew(Strings.BASE_CLASS);
						bc.add(Opcode.DUP);
						bc.add(Opcode.DUP);
						bc.addInvokespecial(Strings.BASE_CLASS, 
								"<init>", "()V");
						bc.addCheckcast(Strings.PL_CLASS);
						bc.addAload(0);
						bc.addInvokevirtual(Strings.PL_CLASS, Strings.PL_CLASS__SET_DERIVED_CLASS, 
								"(" + Strings.PL_CLASS_L + ")V");
					} else {
						if (referenceMap.containsKey(superClass)){
							Reference r = referenceMap.get(superClass);
							if (r.isJava()){
								throw new CompilationException("Reference is reference to java class, not PLang class!");
							}
							addGetRuntime();
							bc.addLdc(cacheStrings(r.getFullReference()));
							bc.addIconst(1);
							bc.addAnewarray(cp.getCtClass(Strings.PLANGOBJECT_N), 0);
							bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__NEW_INSTANCE, 
									"(" + Strings.STRING_L + "Z[" + Strings.PLANGOBJECT_L + ")" + Strings.PL_CLASS_L);
							bc.add(Opcode.DUP);
							bc.addCheckcast(Strings.PL_CLASS);
							bc.addAload(0);
							bc.addInvokevirtual(Strings.PL_CLASS, Strings.PL_CLASS__SET_DERIVED_CLASS, 
									"(" + Strings.PL_CLASS_L + ")V");
						} else if (superClass.contains(".")){
							addGetRuntime();
							bc.addLdc(cacheStrings(superClass));
							bc.addIconst(1);
							bc.addAnewarray(cp.getCtClass(Strings.PLANGOBJECT_N), 0);
							bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__NEW_INSTANCE, 
									"(" + Strings.STRING_L + "Z[" + Strings.PLANGOBJECT_L + ")" + Strings.PL_CLASS_L);
							bc.add(Opcode.DUP);
							bc.addCheckcast(Strings.PL_CLASS);
							bc.addAload(0);
							bc.addInvokevirtual(Strings.PL_CLASS, Strings.PL_CLASS__SET_DERIVED_CLASS, 
									"(" + Strings.PL_CLASS_L + ")V");
						}
					}
				}
				
			}.compile();
		
		for (FieldDeclarationContext field : fields){
			compileField(field);
		}
		cmpInitFuncwraps = true;
		for (final String method : methods){
			new StoreToField(method){

				@Override
				protected void provideSourceValue() throws Exception {
					bc.addNew(Strings.FUNCTION_WRAPPER); // new function wrapper
					bc.add(Opcode.DUP);
					bc.addLdc(cacheStrings(method)); // Load string name of method
					bc.addAload(0); // Load this
					bc.addIconst(compilingClass ? 1 : 0);
					bc.addInvokespecial(Strings.FUNCTION_WRAPPER, 
							"<init>", "(" + Strings.STRING_L + Strings.BASE_COMPILED_STUB_L + "Z)V");
				}
				
			}.compile();
		}
		cmpInitFuncwraps = false;
		stacker.release();
		bc.add(Opcode.RETURN); // Return
	}
	
	private void compileField(FieldDeclarationContext field) throws Exception{
		VariableDeclaratorsContext vdc = field.variableDeclarators();
		for (final VariableDeclaratorContext vd : vdc.variableDeclarator()){
			markLine(bc.currentPc(), vd.start.getLine());
			String varId = vd.variableDeclaratorId().getText();
			
			new StoreToField(varId){

				@Override
				protected void provideSourceValue() throws Exception {
					if (vd.variableInitializer() != null){
						isStatementExpression.add(false);
						compileExpression(vd.variableInitializer().expression(), false, -1);
						isStatementExpression.pop();
					} else {
						addNil();
					}
				}
				
			}.compile();
		}
	}

	protected void addNil() {
		bc.addGetstatic(Strings.NONETYPE, "NOVALUE", Strings.NONETYPE_L); // load NOVALUE
	}

	private abstract class StoreToField {
		private String varId;
		public StoreToField(String varId) {
			this.varId = varId;
		}
		protected abstract void provideSourceValue() throws Exception;
		public void compile() throws Exception {
			
			if (compilingClass && !cmpInitFuncwraps)
				bc.addAload(1); 						// load self
			else
				bc.addAload(0); 						// load this
			bc.addCheckcast(Strings.BASE_COMPILED_STUB);
			bc.addLdc(cacheStrings(varId));			// load string from constants
			provideSourceValue();
			bc.addCheckcast(Strings.PLANGOBJECT);	// cast obj to PLangObject
			bc.addInvokevirtual(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__SETKEY, 
					"(" + Strings.STRING_L + Strings.PLANGOBJECT_L + ")V");
		}
	}
	
	private Set<String> setOperators = new HashSet<String>();
	private Set<String> bioperators = new HashSet<String>();
	private Set<String> leftOperators = new HashSet<String>();
	private Set<String> rightOperators = new HashSet<String>();
	{
		setOperators.add("=");
		setOperators.add("+=");
		setOperators.add("-=");
		setOperators.add("+=");
		setOperators.add("-=");
		setOperators.add("*=");
		setOperators.add("/=");
		setOperators.add("&=");
		setOperators.add("|=");
		setOperators.add("^=");
		setOperators.add(">>=");
		setOperators.add(">>>=");
		setOperators.add("<<=");
		setOperators.add("%=");
		
		bioperators.add("+");
		bioperators.add("-");
		bioperators.add("*");
		bioperators.add("/");
		bioperators.add("<<");
		bioperators.add(">>");
		bioperators.add(">>>");
		bioperators.add("%");
		bioperators.add("==");
		bioperators.add("!=");
		bioperators.add("<");
		bioperators.add(">");
		bioperators.add("<=");
		bioperators.add(">=");
		bioperators.add("&");
		bioperators.add("|");
		bioperators.add("^");
		
		leftOperators.add("+");
		leftOperators.add("-");
		leftOperators.add("++");
		leftOperators.add("--");
		leftOperators.add("!");
		leftOperators.add("~");
		
		rightOperators.add("++");
		rightOperators.add("--");
	}

	private void compileExpression(ExpressionContext expression, boolean compilingMethodCall, int storeVar) throws Exception {
		markLine(bc.currentPc(), expression.start.getLine());
		try {
			if (expression.primary() != null){
				compilePrimaryExpression(expression.primary(), compilingMethodCall, storeVar);
				return;
			} else if (expression.block() != null){
				String methodName = "___internalMethod" + distributed.size();
				BlockContext block = expression.block();
				
				BlockDescription bd = new BlockDescription();
				bd.mn = methodName;
				bd.b = block;
				
				distributed.add(bd);
				addGetRuntime();
				isStatementExpression.add(false);
				compileExpression(expression.expression(0), false, -1);
				isStatementExpression.pop();
				bc.addLdc(cacheStrings(methodName));
				if (expression.expression().size() > 1){
					isStatementExpression.add(false);
					compileExpression(expression.expression(1), false, -1);
					isStatementExpression.pop();
				} else {
					addNil();
				}
				bc.addAload(0);
				bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__RUN_DISTRIBUTED, 
						"(" + Strings.PLANGOBJECT_L + Strings.STRING_L + Strings.PLANGOBJECT_L +  Strings.BASE_COMPILED_STUB_L + ")" + Strings.PLANGOBJECT_L);
				return;
			} else if (expression.getChildCount() > 2 && expression.getChild(1).getText().equals("?")){
				compileTernaryOperator((ExpressionContext)expression.getChild(0), 
						(ExpressionContext)expression.getChild(2), (ExpressionContext)expression.getChild(4), compilingMethodCall, storeVar);
				return;
			} else if (expression.getChildCount() > 2 && expression.getChild(1).getText().equals("->")){
				if (!PLRuntime.getRuntime().isSafeContext())
					throw new CompilationException("Java method call being compiled under unsafe context.");
				
				// java call
				ParseTree init = expression.getChild(0);
				String mname = expression.getChild(2).getText();
				String refName = init.getText();
				if (!referenceMap.containsKey(refName)){
					// instance method, grab Pointer value from it, then call it
					
					addGetRuntime();
					isStatementExpression.add(false);
					compileExpression((ExpressionContext) init, false, -1);
					isStatementExpression.pop();
					bc.addCheckcast(Strings.POINTER);
					bc.addLdc(cacheStrings(mname));
					compileParameters(expression.expressionList());
					bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__RUN_JAVA_WRAPPER, 
							"(" + Strings.POINTER_L + Strings.STRING_L + "[" + Strings.PLANGOBJECT_L +")" + Strings.PLANGOBJECT_L);
					
				} else {
					// static method or constructor
					Reference r = referenceMap.get(refName);
					if (r == null)
						throw new CompilationException("Unknown type reference: " + refName + " at " + expression.start.getLine());
					String fqName = r.getFullReference();
					if (!r.isJava())
						throw new CompilationException("Type is not java type!");
					
					boolean isConstructorCall = refName.equals(mname);
					
					if (isConstructorCall){
						addGetRuntime();
						bc.addLdc(cacheStrings(fqName));
						compileParameters(expression.expressionList());
						bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__CREATE_JAVA_WRAPPER, 
								"(" + Strings.STRING_L + "[" + Strings.PLANGOBJECT_L +")" + Strings.POINTER_L);
					} else {
						addGetRuntime();
						bc.addLdc(cacheStrings(fqName));
						bc.addLdc(cacheStrings(mname));
						compileParameters(expression.expressionList());
						bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__RUN_JAVA_STATIC_METHOD, 
								"(" + Strings.STRING_L + Strings.STRING_L + "[" + Strings.PLANGOBJECT_L +")" + Strings.PLANGOBJECT_L);
					}
				}
			} else if (expression.methodCall() != null){
				int stack = stacker.acquire();
				// method call
				addGetRuntime();
				isStatementExpression.add(false);
				compileExpression((ExpressionContext) expression.getChild(0), true, stack);
				isStatementExpression.pop();
				bc.addAload(stack);
				bc.addCheckcast(Strings.BASE_COMPILED_STUB);
				compileParameters(expression.methodCall().expressionList());
				bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__RUN, 
						"(" + Strings.PLANGOBJECT_L + Strings.BASE_COMPILED_STUB_L + "[" + Strings.PLANGOBJECT_L +")" + Strings.PLANGOBJECT_L);
				stacker.release();
			} else if (leftOperators.contains(expression.getChild(0).getText())){
				final String operator = expression.getChild(0).getText();
				
				if ("++".equals(operator) || "--".equals(operator)){
					new CompileSetOperator(expression.extended(),  false, compilingMethodCall, storeVar){

						@Override
						public void compileRight() throws Exception {
							bc.addInvokestatic(Strings.TYPEOPS, operator.equals("++") ? Strings.TYPEOPS__LEFTPLUSPLUS : Strings.TYPEOPS__LEFTMINUSMINUS, 
									"("+ Strings.PLANGOBJECT_L + ")" + Strings.PLANGOBJECT_L);
						}
						
					}.compileSetOperator();
				} else {
					compileUnaryOperator(operator, (ExpressionContext)expression.getChild(1), compilingMethodCall, storeVar);
				}
			} else if (expression.getChild(0) instanceof ExpressionContext){
				String operator = expression.getChild(1).getText();
				if (bioperators.contains(operator)){
					compileBinaryOperator(operator, 
							(ExpressionContext)expression.getChild(0), (ExpressionContext)expression.getChild(2), 
							compilingMethodCall, storeVar);
				} else if ("instanceof".equals(operator)){
					String fqName = "";
					String type = expression.type().getText();
					if (referenceMap.containsKey(type)){
						Reference r = referenceMap.get(type);
						if (r.isJava())
							throw new CompilationException("Only PLang type can be in catch expression!");
						fqName = r.getFullReference();
					} else {
						fqName = type;
					}
					
					if (!fqName.contains(".") && !isBaseType(fqName)){
						throw new CompilationException("Class type " + type + " is unknown. Have you forgotten using declaration?");
					}
					
					String className = PLRuntime.getRuntime().getClassNameOrGuess(fqName);
					
					addGetRuntime();

					isStatementExpression.add(false);
					compileExpression((ExpressionContext) expression.getChild(0), false, -1);
					isStatementExpression.pop();
					
					bc.addLdc(cacheStrings(className));
					bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__CHECK_INSTANCEOF, 
							"(" + Strings.PLANGOBJECT_L + Strings.STRING_L + ")" + Strings.PLANGOBJECT_L);
					
				} else if ("||".equals(operator) || "&&".equals(operator)) {
					compileLogic((ExpressionContext)expression.getChild(0), (ExpressionContext)expression.getChild(2),
							"||".equals(operator), compilingMethodCall, storeVar);
				} else {
					isStatementExpression.add(false);
					compileExpression((ExpressionContext) expression.getChild(0), false, -1);
					isStatementExpression.pop();
					if (expression.getChild(1).getText().equals(".")){
						// compiling field accessor
						if (compilingMethodCall){
							bc.add(Opcode.DUP);
							bc.addAstore(storeVar);
						}
						
						markLine(bc.currentPc(), expression.stop.getLine());
						String identifier = expression.getChild(2).getText();
						bc.addCheckcast(Strings.BASE_COMPILED_STUB);
						bc.addLdc(cacheStrings(identifier));			// load string from constants
						bc.addInvokevirtual(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__GETKEY, 
								"(" + Strings.STRING_L +")" + Strings.PLANGOBJECT_L);
						
						int kkey = labelCounter++;
						bc.add(Opcode.DUP);
						addLabel(new LabelInfo(){

							@Override
							protected void add(Bytecode bc) throws CompilationException {
								int offset = getValue(poskey) - bcpos;
								bc.write(bcpos, Opcode.IFNONNULL); 
								bc.write16bit(bcpos+1, offset);
							}
							
						}, kkey);

						bc.add(Opcode.POP);
						addThrow("Unknown field: " + identifier);
						
						setLabelPos(kkey);
						bc.add(Opcode.NOP);
					}	
				}
			} else if (expression.getChild(0) instanceof ExtendedContext && rightOperators.contains(expression.getChild(1).getText())){
				ExtendedContext lvalue = expression.extended();
				final int st = stacker.acquire();
				final boolean add = expression.getChild(1).getText().equals("++");
				
				isStatementExpression.add(false);
				new CompileSetOperator(lvalue, false, false, -1){
					
					@Override
					public void compileRight() throws Exception {
						bc.add(Opcode.DUP);
						bc.addAstore(st);
						
						bc.addNew(Strings.INT);
						bc.add(Opcode.DUP);
						bc.addIconst(1);
						bc.addInvokespecial(Strings.INT, 
								"<init>", "(I)V");
						
						compileBinaryOperator(add ? "+" : "-", null, null, false, -1);
					}
					
				}.compileSetOperator();
				isStatementExpression.pop();
				
				if (compilingMethodCall){
					bc.addAload(st);
					bc.addAstore(storeVar);
				} 
				
				if (!isStatementExpression.peek()){
					bc.addOpcode(Opcode.POP);
					bc.addAload(st);
				} 
				stacker.release();
			} else if (expression.getChild(0).getText().equals("new")){
				String fqName = null;
				if (expression.getChildCount() == 4){ // fq
					 fqName = expression.getChild(1).getText() + "." + expression.constructorCall().getChild(0).getText();
				} else {
					String refName = expression.constructorCall().getChild(0).getText();
					Reference r = referenceMap.get(refName);
					if (r == null)
						throw new CompilationException("Unknown type reference: " + refName + " at " + expression.start.getLine());
					fqName = r.getFullReference();
				}
				addGetRuntime();
				bc.addLdc(cacheStrings(fqName));			// load string from constants
				compileParameters(expression.constructorCall().expressionList());
				bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__NEW_INSTANCE, 
						"(" + Strings.STRING_L + "["+ Strings.PLANGOBJECT_L +")" + Strings.PL_CLASS_L);
				
				if (compilingMethodCall){
					bc.add(Opcode.DUP);
					bc.addAstore(storeVar);
				}
			} else if (expression.getChildCount() == 3){
				String operator = expression.getChild(1).getText();
				
				if (setOperators.contains(operator)){
					compileSetOperator(operator, expression, compilingMethodCall, storeVar);
				}
			}
		} finally {
			if (isStatementExpression.peek()){
				bc.add(Opcode.POP);
			}
		}
	}

	private boolean isBaseType(String fqName) {
		return PLRuntime.BASE_TYPES.contains(fqName);
	}

	private void compileTernaryOperator(ExpressionContext e,
			ExpressionContext et, ExpressionContext ef,
			boolean compilingMethod, int storeVar) throws Exception {
		isStatementExpression.add(false);
		compileExpression(e, false, -1);
		isStatementExpression.pop();
		bc.addInvokestatic(Strings.TYPEOPS, Strings.TYPEOPS__CONVERT_TO_BOOLEAN, 
				"("+ Strings.PLANGOBJECT_L + ")Z"); // boolean on stack
		int key = labelCounter++;
		addLabel(new LabelInfo(){

			@Override
			protected void add(Bytecode bc) throws CompilationException {
				int offset = getValue(poskey) - bcpos;
				bc.write(bcpos, Opcode.IFEQ); // jump to else if true or to the next bytecode if not 
				bc.write16bit(bcpos+1, offset);
			}
			
		}, key);
		int key2 = -1;
		
		compileExpression(et, false, -1);
		
		key2 = labelCounter++;
		addLabel(new LabelInfo(){

			@Override
			protected void add(Bytecode bc) throws CompilationException {
				int offset = getValue(poskey) - bcpos;
				if (Math.abs(offset) > (65535/2)){
					throw new CompilationException("Too long jump. Please reformate the code!");
				} else {
					bc.write(bcpos, Opcode.GOTO);
					bc.write16bit(bcpos+1, offset);
				}
			}
			
		}, key2);
		
		setLabelPos(key);	
		
		compileExpression(ef, false, -1);
		setLabelPos(key2);
		
		bc.add(Opcode.NOP);
		if (compilingMethod){
			bc.add(Opcode.DUP);
			bc.addAstore(storeVar);
		}
	}

	private void compileLogic(ExpressionContext left,
			ExpressionContext right, final boolean or, boolean compilingMethod, int storeVar) throws Exception {
		
		bc.addAload(0);
		
		isStatementExpression.add(false);
		compileExpression(left, false, -1);
		isStatementExpression.pop();
		
		bc.addInvokestatic(Strings.TYPEOPS, Strings.TYPEOPS__CONVERT_TO_BOOLEAN, 
				"("+ Strings.PLANGOBJECT_L + ")Z"); // boolean on stack
		
		int shortCut = labelCounter++;
		int reminder = labelCounter++;
		addLabel(new LabelInfo(){

			@Override
			protected void add(Bytecode bc) throws CompilationException {
				int offset = getValue(poskey) - bcpos;
				bc.write(bcpos, or ? Opcode.IFNE : Opcode.IFEQ);  
				bc.write16bit(bcpos+1, offset);
			}
			
		}, shortCut);
		
		isStatementExpression.add(false);
		compileExpression(right, false, -1);
		isStatementExpression.pop();
		
		bc.addInvokestatic(Strings.TYPEOPS, Strings.TYPEOPS__CONVERT_TO_BOOLEAN, 
				"("+ Strings.PLANGOBJECT_L + ")Z"); // boolean on stack
		
		addLabel(new LabelInfo(){

			@Override
			protected void add(Bytecode bc) throws CompilationException {
				int offset = getValue(poskey) - bcpos;
				if (Math.abs(offset) > (65535/2)){
					throw new CompilationException("Too long jump. Please reformate the code!");
				} else {
					bc.write(bcpos, Opcode.GOTO);
					bc.write16bit(bcpos+1, offset);
				}
			}
			
		}, reminder);
		
		setLabelPos(shortCut);
		
		if (or){
			bc.addIconst(1);
		} else {
			bc.addIconst(0);
		}
		
		setLabelPos(reminder);
		bc.addInvokevirtual(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__CONVERT_BOOLEAN, 
				"(Z)" + Strings.PLANGOBJECT_L);
		if (compilingMethod){
			bc.add(Opcode.DUP);
			bc.addAstore(storeVar);
		}
	}
	
	private void compileUnaryOperator(String operator, ExpressionContext expression, boolean compilingMethod, int storeVar) throws Exception {
		if (expression != null){
			isStatementExpression.add(false);
			compileExpression(expression, false, -1);
			isStatementExpression.pop();
		}
		
		String method = "";
		switch (operator){
		case "+": method = Strings.TYPEOPS__UNARY_PLUS; break;
		case "-": method = Strings.TYPEOPS__UNARY_MINUS; break;
		case "!": method = Strings.TYPEOPS__UNARY_LNEG; break;
		case "~": method = Strings.TYPEOPS__UNARY_BNEG; break;
		}
		
		bc.addInvokedynamic(1, method, 
				"("+ Strings.PLANGOBJECT_L + ")" + Strings.PLANGOBJECT_L);
		
		if (compilingMethod){
			bc.add(Opcode.DUP);
			bc.addAstore(storeVar);
		}
	}

	private void compileBinaryOperator(String operator,
			ExpressionContext expression1, ExpressionContext expression2, 
			boolean compilingMethod, int storeVar) throws Exception {
		
		if (expression1 != null && expression2 != null){
			isStatementExpression.add(false);
			compileExpression(expression1, false, -1);
			compileExpression(expression2, false, -1);
			isStatementExpression.pop();
		}
		
		String method = null;
		
		switch (operator){
		case "+": method = Strings.TYPEOPS__PLUS; break;
		case "-": method = Strings.TYPEOPS__MINUS; break;
		case "*": method = Strings.TYPEOPS__MUL; break;
		case "/": method = Strings.TYPEOPS__DIV; break;
		case "%": method = Strings.TYPEOPS__MOD; break;
		case "<<": method = Strings.TYPEOPS__LSHIFT; break;
		case ">>": method = Strings.TYPEOPS__RSHIFT; break;
		case ">>>": method = Strings.TYPEOPS__RUSHIFT; break;
		case "&": method = Strings.TYPEOPS__BITAND; break;
		case "|": method = Strings.TYPEOPS__BITOR; break;
		case "==": method = Strings.TYPEOPS__EQ; break;
		case "!=": method = Strings.TYPEOPS__NEQ; break;
		case "<": method = Strings.TYPEOPS__LESS; break;
		case ">": method = Strings.TYPEOPS__MORE; break;
		case "<=": method = Strings.TYPEOPS__LEQ; break;
		case ">=": method = Strings.TYPEOPS__MEQ; break;
		}
		
		bc.addInvokedynamic(0, method, "(" + Strings.PLANGOBJECT_L + Strings.PLANGOBJECT_L + ")" + Strings.PLANGOBJECT_L);
		
		if (compilingMethod){
			bc.add(Opcode.DUP);
			bc.addAstore(storeVar);
		}
	}

	private void compileParameters(ExpressionListContext expressionList) throws Exception {
		
		int numExpr = expressionList == null ? 0 : expressionList.expression().size();
		int store = stacker.acquire();
		
		// create PLangObject[] array and save it in local variable
		bc.addAnewarray(cp.getCtClass(Strings.PLANGOBJECT_N), numExpr);
		bc.addAstore(store);
		
		// Evaluate every expression and save it to the array
		int i = 0;
		if (expressionList != null)
			for (ExpressionContext e : expressionList.expression()){
				bc.addAload(store);
				bc.addIconst(i++);
				isStatementExpression.add(false);
				compileExpression(e, false, -1);
				isStatementExpression.pop();
				bc.add(Opcode.AASTORE);
			}
		
		// put reference array on stack
		bc.addAload(store);
		
		stacker.release();
	}

	private void compileSetOperator(final String operator,
			ExpressionContext expression, boolean compilingMethod, int storeVar) throws Exception {
		
		ExtendedContext lvalue = expression.extended();
		final ExpressionContext second = (ExpressionContext) expression.getChild(2);
		
		new CompileSetOperator(lvalue, operator.equals("="), compilingMethod, storeVar){

			@Override
			public void compileRight() throws Exception {
				isStatementExpression.add(false);
				compileExpression(second, false, -1);
				isStatementExpression.pop();
				
				if (operator.equals("=")){
					// Simple assignment
				} else {
					// Operation assignment
					compileBinaryOperator(operator.replace("=", ""),	null, null, false, -1);
				}
			}
			
		}.compileSetOperator();
		
	}
	
	private abstract class CompileSetOperator {
		private ExtendedContext lvalue;
		private boolean simpleSet;
		private boolean compilingMethod;
		private int storeVar;
		public CompileSetOperator(ExtendedContext lvalue, boolean simpleSet, boolean compilingMethod, int storeVar) {
			this.lvalue = lvalue;
			this.simpleSet = simpleSet;
			this.compilingMethod = compilingMethod;
			this.storeVar = storeVar;
		}

		public void compileSetOperator() throws Exception {
			markLine(bc.currentPc(), lvalue.start.getLine());
			String identifier = null;
			VariableType vt = null;
			int ord = -1;
			if (lvalue.constExpr() != null){
				identifier = lvalue.constExpr().id() != null ? lvalue.constExpr().id().getText() : lvalue.getText();
				vt = varStack.getType(identifier);
				if (vt == VariableType.LOCAL_VARIABLE)
					ord = varStack.getLocal(identifier);
			} else {
				vt = null;
				compilePrimaryExpression(lvalue.identified().primary(), false, -1);
				identifier = lvalue.identified().getChild(2).getText();
			}
			
			if (vt == null || vt != VariableType.LOCAL_VARIABLE){
				
				{
					/*
					 * Prepares the store operation by getting either class or module on the stack and ready
					 */
					if (vt != null && vt == VariableType.CLASS_VARIABLE){
						if (compilingClass)
							bc.addAload(1); // load self
						else
 							bc.addAload(0); 								// load this
						bc.addCheckcast(Strings.BASE_COMPILED_STUB);
					} else if (vt != null && vt == VariableType.MODULE_VARIABLE) {
						addGetRuntime();
						bc.addLdc(cacheStrings(moduleName));
						bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__GET_MODULE, 
								"(" + Strings.STRING_L + ")" + Strings.PL_MODULE_L); // get module on stack or fail
					}
					
					bc.addCheckcast(Strings.BASE_COMPILED_STUB);
					bc.addLdc(cacheStrings(identifier));			// load string from constants
				}
				
				if (!simpleSet){
					/*
					 * Loads the old value on the stack either from instance or from module field
					 */
					if (vt != null && vt == VariableType.CLASS_VARIABLE){
						if (compilingClass)
							bc.addAload(1); 								// load self
						else
							bc.addAload(0);									// load this
						bc.addCheckcast(Strings.BASE_COMPILED_STUB);
					} else if (vt != null && vt == VariableType.MODULE_VARIABLE) {
						addGetRuntime();
						bc.addLdc(cacheStrings(moduleName));
						bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__GET_MODULE, 
								"(" + Strings.STRING_L + ")" + Strings.PL_MODULE_L); // get module on stack or fail
						bc.addCheckcast(Strings.BASE_COMPILED_STUB);
					}
					bc.addLdc(cacheStrings(identifier));			// load string from constants
					bc.addInvokevirtual(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__GETKEY, 
							"(" + Strings.STRING_L +")" + Strings.PLANGOBJECT_L);
					
				}
				
				compileRight();
				
				/* Stores the value into __setKey of the object on stack */
				bc.addInvokevirtual(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__SETKEY, 
						"(" + Strings.STRING_L + Strings.PLANGOBJECT_L +")V");
				
				if (isStatementExpression.peek() == false){
					/* Put new value on stack */
					if (vt != null && vt == VariableType.CLASS_VARIABLE){
						if (compilingClass)
							bc.addAload(1); 								// load self
						else
							bc.addAload(0);									// load this
						bc.addCheckcast(Strings.BASE_COMPILED_STUB);
					} else if (vt != null && vt == VariableType.MODULE_VARIABLE) {
						addGetRuntime();
						bc.addLdc(cacheStrings(moduleName));
						bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__GET_MODULE, 
								"(" + Strings.STRING_L + ")" + Strings.PL_MODULE_L); // get module on stack or fail
						bc.addCheckcast(Strings.BASE_COMPILED_STUB);
					} else {
						compilePrimaryExpression(lvalue.identified().primary(), false, -1);
						bc.addCheckcast(Strings.BASE_COMPILED_STUB);
					}
					
					bc.addLdc(cacheStrings(identifier));			// load string from constants
					bc.addInvokevirtual(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__GETKEY, 
							"(" + Strings.STRING_L +")" + Strings.PLANGOBJECT_L);
				} else {
					addNil();
				}
				
				if (compilingMethod){
					bc.add(Opcode.DUP);
					bc.addAstore(storeVar);
				}
			} else {
				/* Loads the local variable on stack, then writes to the same position */
				if (!simpleSet)
					bc.addAload(ord);
				compileRight();
				bc.addAstore(ord);
				if (isStatementExpression.peek() == false){
					bc.addAload(ord);
				} else {
					addNil();
				}
				
				if (compilingMethod){
					bc.add(Opcode.DUP);
					bc.addAstore(storeVar);
				}
			}
			
			
		}
		
		public abstract void compileRight() throws Exception;
	}

	private void compilePrimaryExpression(PrimaryContext primary, boolean compilingMethod, int storeVar) throws Exception {
		if (primary.expression() != null){
			isStatementExpression.add(false);
			compileExpression(primary.expression(), compilingMethod, storeVar);
			isStatementExpression.pop();
			return;
		}
		
		if (primary.literal() != null){
			LiteralContext l = primary.literal();
			markLine(bc.currentPc(), l.start.getLine());
			if (l.getText().startsWith("NoValue"))
				addNil();
			else if (l.IntegerLiteral() != null){
				bc.addNew(Strings.INT);
				bc.add(Opcode.DUP);
				bc.addIconst(NumberUtils.toInt(l.IntegerLiteral().getText()));
				bc.addInvokespecial(Strings.INT, 
						"<init>", "(I)V");
			} else if (l.FloatingPointLiteral() != null){
				bc.addNew(Strings.FLOAT);
				bc.add(Opcode.DUP);
				bc.addFconst(NumberUtils.toFloat(l.FloatingPointLiteral().getText()));
				bc.addInvokespecial(Strings.FLOAT, 
						"<init>", "(F)V");
			} else if (l.CharacterLiteral() != null){
				bc.addNew(Strings.INT);
				bc.add(Opcode.DUP);
				bc.addIconst(NumberUtils.toInt(l.CharacterLiteral().getText()));
				bc.addInvokespecial(Strings.INT, 
						"<init>", "(I)V");
			} else if (l.StringLiteral() != null){
				bc.addNew(Strings.STRING_TYPE);
				bc.add(Opcode.DUP);
				bc.addLdc(cacheStrings(Utils.removeStringQuotes(l.StringLiteral().getText())));
				bc.addInvokespecial(Strings.STRING_TYPE, 
						"<init>", "(" + Strings.STRING_L + ")V");
			} else if (l.BooleanLiteral() != null){
				if (l.BooleanLiteral().getText().startsWith("true"))
					bc.addGetstatic(Strings.BOOLEAN_VALUE, "TRUE", Strings.BOOLEAN_VALUE_L); // load TRUE
				else
					bc.addGetstatic(Strings.BOOLEAN_VALUE, "FALSE", Strings.BOOLEAN_VALUE_L); // load FALSE
			}
			
			if (compilingMethod){
				bc.add(Opcode.DUP);
				bc.addAstore(storeVar);
			}
		}
		
		if (primary.constExpr() != null || primary.getText().startsWith("inst") || primary.getText().startsWith("parent")){
			String identifier = primary.constExpr().id() != null ? primary.constExpr().id().getText() : primary.getText();
			
			if (identifier.equals("parent"))
				identifier = BaseClass.___superKey;
			
			if (identifier.startsWith("___")){
				throw new CompilationException("Identifier cannot start with ___, ___ is disabled due to nameclashing with internal methods and fields");
			} else if (identifier.equals("readResolve")){
				throw new CompilationException("readResolve is reserved keyword used by serialization");
			} else if (identifier.equals("serialVersionUID")){
				throw new CompilationException("serialVersionUID is reserved keyword used by serialization");
			} else if (identifier.equals("toString")){
					throw new CompilationException("toString is reserved keyword used by java itself");
			} else if (identifier.equals("getMessage")){
				throw new CompilationException("getMessage is reserved keyword used by java itself");
			} else if (identifier.equals("getLocalizedMessage")){
				throw new CompilationException("getLocalizedMessage is reserved keyword used by java itself");
			} else if (identifier.equals("getCause")){
				throw new CompilationException("getCause is reserved keyword used by java itself");
			} else if (identifier.equals("initCause")){
				throw new CompilationException("initCause is reserved keyword used by java itself");
			} else if (identifier.equals("printStackTrace")){
				throw new CompilationException("printStackTrace is reserved keyword used by java itself");
			} else if (identifier.equals("fillInStackTrace")){
				throw new CompilationException("fillInStackTrace is reserved keyword used by java itself");
			} else if (identifier.equals("getStackTrace")){
				throw new CompilationException("getStackTrace is reserved keyword used by java itself");
			} else if (identifier.equals("setStackTrace")){
				throw new CompilationException("setStackTrace is reserved keyword used by java itself");
			} else if (identifier.equals("addSuppressed")){
				throw new CompilationException("addSuppressed is reserved keyword used by java itself");
			}
			
			if (referenceMap.containsKey(identifier)){
				// is a module identifier, use it as key to the module map
				addGetRuntime();
				bc.addLdc(cacheStrings(identifier));			// load string from constants
				bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__GET_MODULE, 
						"(" + Strings.STRING_L + ")" + Strings.PL_MODULE_L); // get module on stack or fail
				return;
			}
			
			VariableType vt = varStack.getType(identifier);
			
			switch (vt){
			case CLASS_VARIABLE:
				if (compilingClass)
					bc.addAload(1); 								// load self
				else
					bc.addAload(0);									// load this
				bc.addCheckcast(Strings.BASE_COMPILED_STUB);
				if (compilingMethod){
					bc.add(Opcode.DUP);
					bc.addAstore(storeVar);
				}
				bc.addLdc(cacheStrings(identifier));			// load string from constants
				bc.addInvokevirtual(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__GETKEY, 
						"(" + Strings.STRING_L +")" + Strings.PLANGOBJECT_L);
				break;
			case LOCAL_VARIABLE:
				bc.addAload(varStack.getLocal(identifier)); // load from local variables
				if (compilingMethod){
					bc.add(Opcode.DUP);
					bc.addAstore(storeVar);
				}
				break;
			case MODULE_VARIABLE:
				// test whether class contains the variable since it might be owned by superclass
				if (compilingClass)
					bc.addAload(1); // load self
				else
					bc.addAload(0); // load this
				bc.addCheckcast(Strings.BASE_COMPILED_STUB);
				if (compilingMethod){
					bc.add(Opcode.DUP);
					bc.addAstore(storeVar);
				}
				bc.addLdc(cacheStrings(identifier));			// load string from constants
				bc.addInvokevirtual(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__GETKEY, 
						"(" + Strings.STRING_L +")" + Strings.PLANGOBJECT_L);
				bc.add(Opcode.DUP);
				
				int key = labelCounter++;
				addLabel(new LabelInfo(){

					@Override
					protected void add(Bytecode bc) throws CompilationException {
						int offset = getValue(poskey) - bcpos;
						bc.write(bcpos, Opcode.IFNONNULL); 
						bc.write16bit(bcpos+1, offset);
					}
					
				}, key);
				bc.add(Opcode.POP);
				addGetRuntime();
				bc.addLdc(cacheStrings(moduleName));			// load string from constants
				bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__GET_MODULE, 
						"(" + Strings.STRING_L + ")" + Strings.PL_MODULE_L); // get module on stack or fail
				if (compilingMethod){
					bc.add(Opcode.DUP);
					bc.addAstore(storeVar);
				}
				bc.addCheckcast(Strings.BASE_COMPILED_STUB);
				bc.addLdc(cacheStrings(identifier));			// load string from constants
				bc.addInvokevirtual(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__GETKEY, 
						"(" + Strings.STRING_L +")" + Strings.PLANGOBJECT_L);
				

				setLabelPos(key);
				bc.add(Opcode.NOP);
				
				break;
			}
		}
		
	}
	
	private void addThrow(String string) throws Exception {
		addGetRuntime();
		bc.addLdc(cacheStrings("System.BaseException"));
		bc.addAnewarray(cp.getCtClass(Strings.PLANGOBJECT_N), 1);
		bc.add(Opcode.DUP);
		bc.addIconst(0);
		bc.addNew(Strings.STRING_TYPE);
		bc.add(Opcode.DUP);
		bc.addLdc(cacheStrings(Utils.removeStringQuotes(string)));
		bc.addInvokespecial(Strings.STRING_TYPE, 
				"<init>", "(" + Strings.STRING_L + ")V");
		bc.add(Opcode.AASTORE);
		bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__NEW_INSTANCE, 
				"(" + Strings.STRING_L + "[" + Strings.PLANGOBJECT_L + ")" + Strings.PL_CLASS_L);
		bc.addCheckcast(Strings.PL_CLASS);
		bc.add(Opcode.ATHROW);
	}

	private boolean isSystemCompiler = false;
	
	public void setSystemCompiler(){
		isSystemCompiler = true;
	}

	public boolean restrictionCheck(VariableType vt) {
		if (isRestrictedMethodQualifier != null)
			return vt == null ? false : (vt == VariableType.MODULE_VARIABLE ? 
					isRestrictedMethodQualifier == RestrictedTo.CLASS 
					: false);
		if (isSystemCompiler)
			return false;
		return true;
	}
	
	private abstract class LabelInfo {
		public int bcpos;
		public int poskey;
		
		protected abstract void add(Bytecode bc) throws CompilationException;
	}
	
	private List<LabelInfo> labelList;

	
	private Map<Integer, Integer> bcToLineMap;
	private abstract class MethodCompiler {
		private CtMethod m;
		public MethodCompiler(CtMethod m){
			this.m = m;
		}
		@SuppressWarnings("unchecked")
		public void compileMethod() throws Exception{
			lastLineWritten = -1;
			stacker = new AutoIntStacker(1);
			labelCounter = 0;
			labelMap = new HashMap<Integer, Integer>();
			labelList = new ArrayList<LabelInfo>();
			bcToLineMap = new HashMap<Integer, Integer>();
			
			pool = m.getMethodInfo().getConstPool();
			SourceFileAttribute attr = new SourceFileAttribute(pool, source);
			cls.getClassFile().addAttribute(attr);
			
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			lineNumberStream = new DataOutputStream(stream);
			lineNumberStream.writeShort(0);
			
			bc = new Bytecode(pool);
			
			compileDataSources();
			
			for (LabelInfo nfo : labelList)
				nfo.add(bc);
			
			pruneDeadCode();
			
			byte[] bytes = stream.toByteArray();
			int size = (bytes.length - 2) / 4;
			bytes[0] = (byte) ((size >>> 8) & 0xFF);
			bytes[1] = (byte) ((size >>> 0) & 0xFF);
			AttributeInfo lineNubmerInfo = new AttributeInfo(pool, LineNumberAttribute.tag, bytes);
			
			CodeAttribute at = bc.toCodeAttribute();
			at.computeMaxStack();
			at.setMaxLocals(stacker.getMax());
			at.getAttributes().add(lineNubmerInfo);
			
			m.getMethodInfo().setCodeAttribute(at);
			m.getMethodInfo().rebuildStackMap(cp);
						
			cls.addMethod(m);
		}
		
		protected abstract void compileDataSources() throws Exception;
	}

	private static class Instruction {
		private boolean visited 		 = false;
		private Integer lineNo 			 = null;
		private byte[] instBytes 		 = null;
		private Instruction branchTarget = null;
		private int originalPos;
		
		@Override
		public String toString(){
			return Mnemonic.OPCODE[(int)instBytes[0] & 0x000000FF] + ": " + visited;
		}
	}
	
	private static class ExceptionHandler {
		Integer startLink;
		Integer endLink;
		Integer bcLink;
		int type;
	}
	
	private static class IntegerLink {
		public IntegerLink(int i) {
			this.i = i;
		}

		private int i;
		private boolean isStartPos = false;
	}
	
	private static class ExceptionData {
		private int s, e, h, t;
		public ExceptionData(int s, int e, int h, int t) {
			super();
			this.s = s;
			this.e = e;
			this.h = h;
			this.t = t;
		}
	}
	
	public void pruneDeadCode() throws Exception {
		byte[] bytecode = bc.get();
		ExceptionTable etable = bc.getExceptionTable();
		
		int counter = 0;
		List<ExceptionHandler> eh = new ArrayList<ExceptionHandler>();
		BidiMultiMap<Integer, IntegerLink> linkMap = new BidiMultiHashMap<Integer, IntegerLink>();
		
		for (int i=0; i<etable.size(); i++){
			ExceptionHandler ehi = new ExceptionHandler();
			ehi.bcLink = counter++;
			linkMap.put(ehi.bcLink, new IntegerLink(etable.handlerPc(i)));
			ehi.startLink = counter++;
			IntegerLink startLink = new IntegerLink(etable.startPc(i));
			linkMap.put(ehi.startLink, startLink);
			startLink.isStartPos  = true;
			ehi.endLink = counter++;
			linkMap.put(ehi.endLink, new IntegerLink(etable.endPc(i)));
			ehi.type = etable.catchType(i);
			eh.add(ehi);
		}	
		
		Map<Integer, Instruction> iPosList = new HashMap<Integer, Instruction>();
		List<Instruction> insts = parseBytecode(bytecode, iPosList);
		
		markDeadCode(insts, iPosList, etable);
		
		List<ExceptionData> copy = new ArrayList<ExceptionData>();
		List<Instruction> prunned = new ArrayList<Instruction>();
		BidiMultiMap<Integer, IntegerLink> cpyMap = new BidiMultiHashMap<Integer, IntegerLink>(linkMap);
		for (Instruction i : insts){
			if (i.visited)
				prunned.add(i);
			else {
				int bcpos = i.originalPos;
				for (IntegerLink ilink : cpyMap.values()){
					if (ilink.i > bcpos){
						ilink.i -= i.instBytes.length;
					} else if (ilink.i == bcpos && ilink.isStartPos){
						linkMap.removeValue(ilink);
					}
				}
			}
		}
		
		for (ExceptionHandler ehi : eh){
			if (linkMap.get(ehi.startLink) != null)
				copy.add(new ExceptionData(linkMap.get(ehi.startLink).i, linkMap.get(ehi.endLink).i, linkMap.get(ehi.bcLink).i, ehi.type));
		}
		
		recalculateJumps(prunned);
		
		bc = new Bytecode(pool);
		for (Instruction i : prunned){
			if (i.lineNo != null)
				writeLine(bc.currentPc(), i.lineNo);
			for (byte b : i.instBytes)
				bc.add(b);
		}
		
		for (int i=0; i<copy.size(); i++)
			bc.addExceptionHandler(copy.get(i).s, copy.get(i).e, copy.get(i).h, copy.get(i).t);
	}

	private void recalculateJumps(List<Instruction> prunned) {
		for (Instruction i : prunned){
			if (i.branchTarget != null){
				short distance = getDistance(i, i.branchTarget, prunned);
				i.instBytes[1] = (byte) (distance >> 8);
				i.instBytes[2] = (byte) distance;
			}
		}
	}

	private short getDistance(Instruction a, Instruction b,
			List<Instruction> list) {
		
		int idxa = list.indexOf(a);
		int idxb = list.indexOf(b);
		
		if (idxa == idxb) return 0;
		
		int idxs = Math.min(idxa, idxb);
		int idxl = Math.max(idxa, idxb);
		
		short delta = getDistance(idxs, idxl, list);
		
		if (idxa < idxb)
			return delta;
		else
			return (short) -delta;
	}

	private short getDistance(int idxs, int idxl, List<Instruction> list) {
		short delta = 0;
		for (int i=idxs; i<idxl; i++){
			delta += list.get(i).instBytes.length;
		}
		return delta;
	}

	private void markDeadCode(List<Instruction> insts, Map<Integer, Instruction> iPosList, ExceptionTable etable) {
		markFrom(insts, 0);
		for (int i=0; i<etable.size(); i++){
			int startPc = etable.startPc(i);
			if (iPosList.get(startPc).visited)
				markFrom(insts, insts.indexOf(iPosList.get(etable.handlerPc(i))));
		}
	}

	private void markFrom(List<Instruction> insts, int i) {
		while (i < insts.size()){
			Instruction inst = insts.get(i);
			if (inst.visited) return;
			
			inst.visited = true;
			if (inst.branchTarget != null){
				markFrom(insts, insts.indexOf(inst.branchTarget));
			}
			
			int opcode = inst.instBytes[0] & 0x000000FF;
			
			switch (opcode){
			case 0xac:
			case 0xad:
			case 0xb1:
			case 0xb0:
			case 0xaf:
			case 0xae:
			case 0xa7:
			case 0xbf:
				return; // is return
				
			}
			
			++i;
		}
	}

	private List<Instruction> parseBytecode(byte[] bytecode, Map<Integer, Instruction> iPosList) throws Exception {
		List<Instruction> iList = new ArrayList<Instruction>();
		for (int i=0; i<bytecode.length; i++){
			Instruction in = new Instruction();
			int pos = i;
			
			switch ((int)bytecode[i] & 0x000000FF){
				// 1
			case 0x19:
			case 0x3a:
			case 0x10:
			case 0x39:
			case 0x17:
			case 0x38:
			case 0x15:
			case 0x36:
			case 0x12:
			case 0x16:
			case 0x37:
			case 0xbc:
			case 0xa9:
				in.instBytes = new byte[2];
				in.instBytes[0] = bytecode[i];
				in.instBytes[1] = bytecode[++i];
				break;
				
				// 2
			case 0xbd:
			case 0xc0:
			case 0xb4:
			case 0xb2:
			case 0xa7:
			case 0xa5:
			case 0xa6:
			case 0x9f:
			case 0xa2:
			case 0xa3:
			case 0xa4:
			case 0xa1:
			case 0xa0:
			case 0x99:
			case 0x9c:
			case 0x9d:
			case 0x9e:
			case 0x9b:
			case 0x9a:
			case 0xc7:
			case 0xc6:
			case 0x84:
			case 0xc1:
			case 0xb7:
			case 0xb8:
			case 0xb6:
			case 0xa8:
			case 0x13:
			case 0x14:
			case 0xbb:
			case 0xb5:
			case 0xb3:
			case 0x11:
				in.instBytes = new byte[3];
				in.instBytes[0] = bytecode[i];
				in.instBytes[1] = bytecode[++i];
				in.instBytes[2] = bytecode[++i];
				break;
				
				// 3
			case 0xc5:				
				in.instBytes = new byte[4];
				in.instBytes[0] = bytecode[i];
				in.instBytes[1] = bytecode[++i];
				in.instBytes[2] = bytecode[++i];
				in.instBytes[3] = bytecode[++i];
				break;
				
				// 4
			case 0xc8:
			case 0xba:
			case 0xb9:
			case 0xc9:
				in.instBytes = new byte[5];
				in.instBytes[0] = bytecode[i];
				in.instBytes[1] = bytecode[++i];
				in.instBytes[2] = bytecode[++i];
				in.instBytes[3] = bytecode[++i];
				in.instBytes[4] = bytecode[++i];
				break;
				
				// x not used
			case 0xab:
			case 0xaa:
				throw new CompilationException("Not allowed");
				
				// wide
			case 0xc4:
				int cbc = (int)bytecode[i] & 0x000000FF;
				if (cbc == 0x84){
					in.instBytes = new byte[6];
					in.instBytes[0] = bytecode[i];
					in.instBytes[1] = bytecode[++i];
					in.instBytes[2] = bytecode[++i];
					in.instBytes[3] = bytecode[++i];
					in.instBytes[4] = bytecode[++i];
					in.instBytes[5] = bytecode[++i];
				} else {
					in.instBytes = new byte[4];
					in.instBytes[0] = bytecode[i];
					in.instBytes[1] = bytecode[++i];
					in.instBytes[2] = bytecode[++i];
					in.instBytes[3] = bytecode[++i];
				}
				break;
				
			case 0x32:
			case 0x53:
			case 0x01:
			case 0x2a:
			case 0x2b:
			case 0x2c:
			case 0x2d:
			case 0xb0:
			case 0xbe:
			case 0x4b:
			case 0x4c:
			case 0x4d:
			case 0x4e:
			case 0xbf:
			case 0x33:
			case 0x54:
			case 0xca:
			case 0x34:
			case 0x55:
			case 0x90:
			case 0x8e:
			case 0x8f:
			case 0x63:
			case 0x31:
			case 0x52:
			case 0x98:
			case 0x97:
			case 0x0e:
			case 0x0f:
			case 0x6f:
			case 0x18:
			case 0x26:
			case 0x27:
			case 0x28:
			case 0x29:
			case 0x6b:
			case 0x77:
			case 0x73:
			case 0xaf:
			case 0x47:
			case 0x48:
			case 0x49:
			case 0x4a:
			case 0x67:
			case 0x59:
			case 0x5a:
			case 0x5b:
			case 0x5c:
			case 0x5d:
			case 0x5e:
			case 0x8d:
			case 0x8b:
			case 0x8c:
			case 0x62:
			case 0x30:
			case 0x51:
			case 0x96:
			case 0x95:
			case 0x0b:
			case 0x0c:
			case 0x0d:
			case 0x6e:
			case 0x22:
			case 0x23:
			case 0x24:
			case 0x25:
			case 0x76:
			case 0x6a:
			case 0x72:
			case 0xae:
			case 0x43:
			case 0x44:
			case 0x45:
			case 0x46:
			case 0x66:
			case 0x91:
			case 0x92:
			case 0x87:
			case 0x86:
			case 0x85:
			case 0x93:
			case 0x60:
			case 0x2e:
			case 0x7e:
			case 0x4f:
			case 0x02:
			case 0x03:
			case 0x04:
			case 0x05:
			case 0x06:
			case 0x07:
			case 0x08:
			case 0x6c:
			case 0x1a:
			case 0x1b:
			case 0x1c:
			case 0x1d:
			case 0xfe:
			case 0xff:
			case 0x68:
			case 0x74:
			case 0x80:
			case 0x70:
			case 0xac:
			case 0x78:
			case 0x7a:
			case 0x3b:
			case 0x3c:
			case 0x3d:
			case 0x3e:
			case 0x64:
			case 0x7c:
			case 0x82:
			case 0x8a:
			case 0x89:
			case 0x88:
			case 0x61:
			case 0x2f:
			case 0x7f:
			case 0x50:
			case 0x94:
			case 0x09:
			case 0x0a:
			case 0x6d:
			case 0x1e:
			case 0x1f:
			case 0x20:
			case 0x21:
			case 0x69:
			case 0x75:
			case 0x79:
			case 0x7b:
			case 0x81:
			case 0x71:
			case 0xad:
			case 0x3f:
			case 0x40:
			case 0x41:
			case 0x42:
			case 0x65:
			case 0x7d:
			case 0x83:
			case 0xc2:
			case 0xc3:
			case 0x00:
			case 0x57:
			case 0x58:
			case 0xb1:
			case 0x35:
			case 0x56:
			case 0x5f:
			default:
				in.instBytes = new byte[1];
				in.instBytes[0] = bytecode[i];
			}
			
			if (bcToLineMap.containsKey(pos))
				in.lineNo = bcToLineMap.get(pos);
			in.originalPos = pos;
			iList.add(in);
			iPosList.put(pos, in);
		}
		
		int it = 0;
		for (Instruction i : iList){
			int opcode = (int)i.instBytes[0] & 0x000000FF;
			
			switch (opcode){
			case 0xa7:
			case 0xa5:
			case 0xa6:
			case 0x9f:
			case 0xa2:
			case 0xa3:
			case 0xa4:
			case 0xa1:
			case 0xa0:
			case 0x99:
			case 0x9c:
			case 0x9d:
			case 0x9e:
			case 0x9b:
			case 0x9a:
			case 0xc7:
			case 0xc6: {
				int branchbyte1 = i.instBytes[1] & 0x000000FF;
				int branchbyte2 = i.instBytes[2] & 0x000000FF;
				short offset = (short) ((branchbyte1 << 8) + branchbyte2);
				int instoffset = it + offset;
				i.branchTarget = iPosList.get(instoffset);
			} break;
			
			case 0xc8:{
				int branchbyte1 = i.instBytes[1] & 0x000000FF;
				int branchbyte2 = i.instBytes[2] & 0x000000FF;
				int branchbyte3 = i.instBytes[3] & 0x000000FF;
				int branchbyte4 = i.instBytes[4] & 0x000000FF;
				short offset = (short) ((branchbyte1 << 24) + (branchbyte2 << 16) + (branchbyte3 << 8) + branchbyte4);
				int instoffset = it + offset;
				i.branchTarget = iPosList.get(instoffset);
			} break;
				
			case 0xa8:
			case 0xc9:
				throw new CompilationException("Not allowed");
				
			default:
				break;
			}
			
			it += i.instBytes.length;
		}
		
		return iList;
	}

	public ClassLoader getClassLoader() {
		return PLRuntime.getRuntime().getClassLoader();
	}

	private void markLine(int pc, int line) throws Exception{
		markLine(pc, line, false);
	}
	
	private void markLine(int pc, int line, boolean override) throws Exception {
		bcToLineMap.put(pc, line);
	}
	
	private void writeLine(int pc, int line) throws Exception {
		if (line == lastLineWritten)
			return;
		lineNumberStream.writeShort(pc);
		lineNumberStream.writeShort(line);
		lastLineWritten = line;
	}
	
	private int cacheStrings(String string) {
		if (!cache.containsKey(string)){
			cache.put(string, pool.addStringInfo(string));
		}
		return cache.get(string);
	}
	
	private int setLabelPos(int key){
		int cpc = bc.currentPc();
		labelMap.put(key, cpc);
		return key;
	}
	
	private int getValue(int key){
		return labelMap.get(key);
	}
	
	private void addLabel(LabelInfo nfo, int key){
		nfo.poskey = key;
		nfo.bcpos = bc.currentPc();
		labelList.add(nfo);
		// Placeholder NOPs
		for (int i=0; i<3; i++)
			bc.add(Opcode.NOP);
	}
}

