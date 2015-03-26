package cz.upol.vanusanik.paralang.compiler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.InstructionPrinter;
import javassist.bytecode.LineNumberAttribute;
import javassist.bytecode.Opcode;
import javassist.bytecode.SourceFileAttribute;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import cz.upol.vanusanik.paralang.compiler.VariableScopeStack.VariableType;
import cz.upol.vanusanik.paralang.plang.PLangLexer;
import cz.upol.vanusanik.paralang.plang.PLangParser;
import cz.upol.vanusanik.paralang.plang.PLangParser.BlockContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.BlockStatementContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ClassBodyDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ClassDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.CompilationUnitContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ExpressionContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ExpressionListContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ExtendedContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.FieldDeclarationContext;
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
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.runtime.PLClass;
import cz.upol.vanusanik.paralang.runtime.PLModule;
import cz.upol.vanusanik.paralang.runtime.PLRuntime;
import cz.upol.vanusanik.paralang.utils.Utils;

public class PLCompiler {
	
	private Map<String, Reference> referenceMap = new HashMap<String, Reference>();
	private boolean restrictions = true;
	private ClassLoader classLoader;
	private String source;
	private String moduleName;
	
	public Class<? extends PLModule> compile(FileDesignator in){
		try {
			return compileFile(in);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private Class<? extends PLModule> compileFile(FileDesignator in) throws Exception {
		CompilationUnitContext ctx = parse(in);
		
		buildReferenced(ctx);
		System.err.println();
		source = in.getSource();
		
		return compileModule(ctx, in);
	}

	private void buildReferenced(CompilationUnitContext ctx) {
		moduleName = ctx.moduleDeclaration().getChild(1).getText();
		varStack = new VariableScopeStack();
		
		List<ImportDeclarationContext> idc = ctx.importDeclaration();
		if (idc != null){
			for (ImportDeclarationContext id : idc){
			Reference r = null;
				String name = null;
				if (id.getText().startsWith("import")){
					String qId = id.qualifiedName().getText();
					name = qId;
					if (qId.contains("."))
						name = StringUtils.substringAfterLast(qId, ".");
					r = new Reference(qId, name, true);
				} else {
					String qId = id.singleQualifiedName().getText();
					name = qId;
					if (qId.contains("."))
						name = StringUtils.substringAfterLast(qId, ".");
					r = new Reference(qId, name, false);
				}
				referenceMap.put(name, r);
			}
		}
		
		for (ModuleDeclarationsContext mdc : ctx.moduleDeclaration().moduleDeclarations()){
			if (mdc.classDeclaration() != null){
				String name = mdc.classDeclaration().children.get(1).getText();
				Reference r = new Reference(moduleName + "." + name, name, false);
				referenceMap.put(name, r);
			}
		}
	}

	private CompilationUnitContext parse(FileDesignator in) throws Exception{
		ANTLRInputStream is = new ANTLRInputStream(in.getStream());
		PLangLexer lexer = new PLangLexer(is);
		CommonTokenStream stream = new CommonTokenStream(lexer);
		PLangParser parser = new PLangParser(stream);
		return parser.compilationUnit();
	}

	private Class<? extends PLModule> compileModule(CompilationUnitContext ctx, FileDesignator in) throws Exception{
		for (ModuleDeclarationsContext mdc : ctx.moduleDeclaration().moduleDeclarations()){
			if (mdc.classDeclaration() != null){
				Class<?> klazz = compileClassDefinition(ctx.moduleDeclaration().children.get(1).getText(), mdc.classDeclaration(), in);
				PLRuntime.getRuntime().registerClass(moduleName, mdc.classDeclaration().children.get(1).getText(), klazz);
				
				PLClass o = PLRuntime.getRuntime().newInstance(moduleName + "." + mdc.classDeclaration().children.get(1).getText(), new Int(1234));
				PLRuntime.getRuntime().run(o.__getkey("foo"), new Int(5000));
			}
		}
		return null;
	}
	
	private boolean compilingClass = false;
	private ClassPool cp;
	private DataOutputStream lineNumberStream;
	private CtClass cls;
	private Bytecode bc;
	private ConstPool pool; 
	private AutoIntStacker stacker;
	private int lastLineWritten;
	private HashMap<String, Integer> cache;
	private VariableScopeStack varStack;
	private boolean isRestrictedMethodQualifier;
	
	@SuppressWarnings("unused")
	private Class<?> compileClassDefinition(String moduleName, ClassDeclarationContext classDeclaration, FileDesignator in) throws Exception {
		compilingClass = true;
		
		cp = ClassPool.getDefault();
		
		String packageName = Utils.packageName(in);
		cache = new HashMap<String, Integer>();
		String className = moduleName + "$" + classDeclaration.children.get(1).getText();
		File output = new File(in.getOutputDir(), className + ".class");
		
		cls = cp.makeClass(className);
		cls.setSuperclass(cp.getCtClass(Strings.CLASS_BASE_CLASS));
		
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
		// TODO add super when available
		
		// Find all methods
		final Set<String> methods = new HashSet<String>();
		for (ClassBodyDeclarationContext cbd : cbdList){
			if (cbd.memberDeclaration().functionDeclaration() != null){
				FunctionDeclarationContext fcx = cbd.memberDeclaration().functionDeclaration();
				boolean restricted = fcx.getText().startsWith("restricted");
				String name = restricted ? fcx.getChild(1).getText() : fcx.getChild(0).getText();
				methods.add(name);
			}
		}
		
		if (!methods.contains("init")){
			CtMethod initM = CtNewMethod.make("public cz.upol.vanusanik.paralang.plang.PLangObject init(cz.upol.vanusanik.paralang.plang.PLangObject inst) { return cz.upol.vanusanik.paralang.plang.types.NoValue.NOVALUE; }", cls);
			cls.addMethod(initM);
			methods.add("init");
		}
		
		// Compile system init method
		final CtMethod m = CtNewMethod.make("protected void __init_internal_datafields() { return null; }", cls);
		new MethodCompiler(m){

			@Override
			protected void compileDataSources() throws Exception {
				isRestrictedMethodQualifier = true; // default fields are always in restricted mode check off
				compileInitMethod(fields, methods);
			}
			
		}.compileMethod();
		
		// Compile all methods
		for (ClassBodyDeclarationContext cbd : cbdList){
			if (cbd.memberDeclaration().functionDeclaration() != null){
				FunctionDeclarationContext fcx = cbd.memberDeclaration().functionDeclaration();
				compileFunction(fcx);
			}
		}
		
		varStack.popStack(); // pop class variables
		
		cls.debugWriteFile();
		cls.toBytecode(new DataOutputStream(new FileOutputStream(output)));
		return cls.toClass(getClassLoader(), null);
	}

	private String compileFunction(final FunctionDeclarationContext fcx) throws Exception{
		final boolean restricted = fcx.getText().startsWith("restricted");
		String name = restricted ? fcx.getChild(1).getText() : fcx.getChild(0).getText();
		
		isRestrictedMethodQualifier = restricted;
		if (name.equals("init"))
			isRestrictedMethodQualifier = true;
		
		FormalParametersContext fpx = fcx.formalParameters();
		final List<String> args = new ArrayList<String>();
		if (compilingClass)
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

	protected void compileFunction(FunctionBodyContext functionBody, boolean restricted) throws Exception {
		markLine(bc.currentPc(), functionBody.start.getLine());
		
		if (restricted){
			addGetRuntime();
			bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__CHECK_RESTRICTED_ACCESS, "()V");
		}
		
		compileBlock(functionBody.block());	
		
		addNil();
		bc.add(Opcode.ARETURN);
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
						compileExpression(vd.variableInitializer().expression());
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

	private void compileStatement(StatementContext statement) throws Exception {
		markLine(bc.currentPc(), statement.start.getLine());
		if (statement.block() != null){
			compileBlock(statement.block());
			return;
		}
		if (statement.statementExpression() != null){
			compileExpression(statement.statementExpression().expression());
			bc.add(Opcode.POP);
			return;
		}
	}

	private void addGetRuntime() {
		bc.addAload(0);		// load this
		bc.addInvokevirtual(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__GET_RUNTIME, 
				"()" + Strings.RUNTIME_L); // call to __get_runtime, PLRuntime is on stack
	}

	private void compileInitMethod(List<FieldDeclarationContext> fields, Set<String> methods) throws Exception{
		for (FieldDeclarationContext field : fields){
			compileField(field);
		}
		for (final String method : methods){
			new StoreToClassField(method){

				@Override
				protected void provideSourceValue() throws Exception {
					bc.addNew(Strings.FUNCTION_WRAPPER); // new function wrapper
					bc.add(Opcode.DUP);
					bc.addLdc(cacheStrings(method)); // Load string name of method
					bc.addAload(0); // Load this
					bc.addIconst(1);
					bc.addInvokespecial(Strings.FUNCTION_WRAPPER, 
							"<init>", "(" + Strings.STRING_L + Strings.BASE_COMPILED_STUB_L + "Z)V");
				}
				
			}.compile();
		}
		bc.add(Opcode.RETURN); // Return
	}
	
	private void compileField(FieldDeclarationContext field) throws Exception{
		VariableDeclaratorsContext vdc = field.variableDeclarators();
		for (final VariableDeclaratorContext vd : vdc.variableDeclarator()){
			markLine(bc.currentPc(), vd.start.getLine());
			String varId = vd.variableDeclaratorId().getText();
			
			new StoreToClassField(varId){

				@Override
				protected void provideSourceValue() throws Exception {
					if (vd.variableInitializer() != null){
						compileExpression(vd.variableInitializer().expression());
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

	private abstract class StoreToClassField {
		private String varId;
		public StoreToClassField(String varId) {
			this.varId = varId;
		}
		protected abstract void provideSourceValue() throws Exception;
		public void compile() throws Exception {
			
			bc.addAload(0); 						// load this
			bc.addLdc(cacheStrings(varId));			// load string from constants
			provideSourceValue();
			bc.addCheckcast(Strings.PLANGOBJECT);	// cast obj to PLangObject
			bc.addInvokevirtual(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__SETKEY, 
					"(" + Strings.STRING_L + Strings.PLANGOBJECT_L + ")V");
			

			varStack.addVariable(varId, VariableType.CLASS_VARIABLE);
		}
	}
	
	private Set<String> setOperators = new HashSet<String>();
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
	}

	private void compileExpression(ExpressionContext expression) throws Exception {
		markLine(bc.currentPc(), expression.start.getLine());
		if (expression.primary() != null){
			compilePrimaryExpression(expression.primary());
			return;
		} else if (expression.getChild(0) instanceof ExpressionContext){
			compileExpression((ExpressionContext) expression.getChild(0));
			if (expression.getChild(1).getText().equals(".")){
				// compiling field accessor
				markLine(bc.currentPc(), expression.stop.getLine());
				String identifier = expression.getChild(2).getText();
				bc.addCheckcast(Strings.BASE_COMPILED_STUB);
				bc.addLdc(cacheStrings(identifier));			// load string from constants
				bc.addInvokevirtual(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__GETKEY, 
						"(" + Strings.STRING_L +")" + Strings.PLANGOBJECT_L);
			}
		} else if (expression.getChild(0).getText().equals("new")){
			String fqName = null;
			if (expression.getChildCount() == 5){ // fq
				 fqName = expression.getChild(1).getText() + "." + expression.getChild(3);
			} else {
				String refName = expression.getChild(1).getText();
				Reference r = referenceMap.get(refName);
				if (r == null)
					throw new CompilationException("Unknown type reference: " + refName + " at " + expression.start.getLine());
				fqName = r.getFullReference();
			}
			addGetRuntime();
			bc.addLdc(cacheStrings(fqName));			// load string from constants
			compileParameters(expression.expressionList());
			bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__NEW_INSTANCE, 
					"(" + Strings.STRING_L + "["+ Strings.PLANGOBJECT_L +")" + Strings.PL_CLASS_L);
		} else if (expression.getChildCount() == 3){
			String operator = expression.getChild(1).getText();
			
			if (setOperators.contains(operator)){
				compileSetOperator(operator, expression);
				addNil();
			}
		}
	}

	private void compileParameters(ExpressionListContext expressionList) throws Exception {
		if (expressionList == null)
			return;
		
		int numExpr = expressionList.expression().size();
		int store = stacker.acquire();
		
		// create PLangObject[] array and save it in local variable
		bc.addAnewarray(cp.getCtClass(Strings.PLANGOBJECT_N), numExpr);
		bc.addAstore(store);
		
		// Evaluate every expression and save it to the array
		int i = 0;
		for (ExpressionContext e : expressionList.expression()){
			bc.addAload(store);
			bc.addIconst(i++);
			compileExpression(e);
			bc.add(Opcode.AASTORE);
		}
		
		// put reference array on stack
		bc.addAload(store);
		
		stacker.release();
	}

	private void compileSetOperator(final String operator,
			ExpressionContext expression) throws Exception {
		
		ExtendedContext lvalue = expression.extended();
		final ExpressionContext second = (ExpressionContext) expression.getChild(2);
		
		new CompileSetOperator(lvalue, operator.equals("=")){

			@Override
			public void compileRight() throws Exception {
				compileExpression(second);
				
				if (!operator.equals("=")){
					// Simple assignment
				} else {
					// Operation assignment
					// TODO
				}
			}
			
		}.compileSetOperator();
		
	}
	
	private abstract class CompileSetOperator {
		private ExtendedContext lvalue;
		private boolean simpleSet;
		public CompileSetOperator(ExtendedContext lvalue, boolean simpleSet) {
			this.lvalue = lvalue;
			this.simpleSet = simpleSet;
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
				compilePrimaryExpression(lvalue.identified().primary());
				identifier = lvalue.identified().getChild(2).getText();
			}
			
			if (vt == null || vt != VariableType.LOCAL_VARIABLE){
				if (restrictionCheck())
					throw new CompilationException("Action not allowed in non restricted context");
				
				{
					/*
					 * Prepares the store operation by getting either class or module on the stack and ready
					 */
					if (vt != null && vt == VariableType.CLASS_VARIABLE){
						bc.addAload(0); 								// load this
					} else if (vt != null && vt == VariableType.MODULE_VARIABLE) {
						addGetRuntime();
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
						bc.addAload(0); 								// load this
					} else if (vt != null && vt == VariableType.MODULE_VARIABLE) {
						addGetRuntime();
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
			} else {
				/* Loads the local varaible on stack, then writes to the same position */
				if (!simpleSet)
					bc.addAload(ord);
				compileRight();
				bc.addAstore(ord);
			}
		}
		
		public abstract void compileRight() throws Exception;
	}

	private void compilePrimaryExpression(PrimaryContext primary) throws Exception {
		if (primary.expression() != null){
			compileExpression(primary.expression());
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
				bc.addLdc(cacheStrings(l.StringLiteral().getText()));
				bc.addInvokespecial(Strings.STRING_TYPE, 
						"<init>", "(" + Strings.STRING_L + ")V");
			} else if (l.BooleanLiteral() != null){
				if (l.BooleanLiteral().getText().startsWith("true"))
					bc.addGetstatic(Strings.BOOLEAN_VALUE, "TRUE", Strings.BOOLEAN_VALUE_L); // load TRUE
				else
					bc.addGetstatic(Strings.BOOLEAN_VALUE, "FALSE", Strings.BOOLEAN_VALUE_L); // load FALSE
			}
		}
		
		if (primary.constExpr() != null || primary.getText().startsWith("inst") || primary.getText().startsWith("parent")){
			String identifier = primary.constExpr().id() != null ? primary.constExpr().id().getText() : primary.getText();
			VariableType vt = varStack.getType(identifier);
			
			switch (vt){
			case CLASS_VARIABLE:
				bc.addAload(0); 								// load this
				bc.addLdc(cacheStrings(identifier));			// load string from constants
				bc.addInvokevirtual(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__GETKEY, 
						"(" + Strings.STRING_L +")" + Strings.PLANGOBJECT_L);
				break;
			case LOCAL_VARIABLE:
				bc.addAload(varStack.getLocal(identifier)); // load from local variables
				break;
			case MODULE_VARIABLE:
				addGetRuntime();
				bc.addLdc(cacheStrings(moduleName));			// load string from constants
				bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__GET_MODULE, 
						"(" + Strings.STRING_L + ")" + Strings.PL_MODULE_L); // get module on stack or fail
				bc.addCheckcast(Strings.BASE_COMPILED_STUB);
				bc.addLdc(cacheStrings(identifier));			// load string from constants
				bc.addInvokevirtual(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__GETKEY, 
						"(" + Strings.STRING_L +")" + Strings.PLANGOBJECT_L);
				break;
			}
		}
		
	}
	
	private boolean isSystemCompiler = false;
	
	public void setSystemCompiler(){
		isSystemCompiler = true;
	}

	public boolean restrictionCheck() {
		if (isRestrictedMethodQualifier)
			return false;
		if (isSystemCompiler)
			return false;
		return true;
	}

	private abstract class MethodCompiler {
		private CtMethod m;
		public MethodCompiler(CtMethod m){
			this.m = m;
		}
		@SuppressWarnings("unchecked")
		public void compileMethod() throws Exception{
			lastLineWritten = -1;
			stacker = new AutoIntStacker(1);
			
			pool = m.getMethodInfo().getConstPool();
			SourceFileAttribute attr = new SourceFileAttribute(pool, source);
			cls.getClassFile().addAttribute(attr);
			
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			lineNumberStream = new DataOutputStream(stream);
			lineNumberStream.writeShort(0);
			
			bc = new Bytecode(pool);
			
			compileDataSources();
			
			byte[] bytes = stream.toByteArray();
			int size = (bytes.length - 2) / 4;
			bytes[0] = (byte) ((size >>> 8) & 0xFF);
			bytes[1] = (byte) ((size >>> 0) & 0xFF);
			AttributeInfo lineNubmerInfo = new AttributeInfo(pool, LineNumberAttribute.tag, bytes);
			
			CodeAttribute at = bc.toCodeAttribute();
			at.computeMaxStack();
			//at.setMaxStack(128); // FIXME
			at.setMaxLocals(stacker.getMax());
			at.getAttributes().add(lineNubmerInfo);
			
			m.getMethodInfo().setCodeAttribute(at);
			
			InstructionPrinter.print(m, System.err);
			
			cls.addMethod(m);
		}
		
		protected abstract void compileDataSources() throws Exception;
	}

	public boolean getRestrictions() {
		return restrictions;
	}

	public void setRestrictions(boolean restrictions) {
		this.restrictions = restrictions;
	}

	public ClassLoader getClassLoader() {
		if (classLoader == null)
			setClassLoader(Thread.currentThread().getContextClassLoader());
		return classLoader;
	}

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	private void markLine(int pc, int line) throws Exception{
		markLine(pc, line, false);
	}
	
	private void markLine(int pc, int line, boolean override) throws Exception {
		if (line == lastLineWritten && !override)
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
}
