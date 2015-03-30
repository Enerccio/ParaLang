package cz.upol.vanusanik.paralang.compiler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.InstructionPrinter;
import javassist.bytecode.LineNumberAttribute;
import javassist.bytecode.Opcode;
import javassist.bytecode.SourceFileAttribute;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
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
import cz.upol.vanusanik.paralang.runtime.PLRuntime;
import cz.upol.vanusanik.paralang.utils.Utils;

public class PLCompiler {
	
	private Map<String, Reference> referenceMap = new HashMap<String, Reference>();
	private boolean restrictions = true;
	private ClassLoader classLoader;
	private String source;
	private String moduleName;
	
	public void compile(FileDesignator in){
		try {
			compileFile(in);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void compileFile(FileDesignator in) throws Exception {
		CompilationUnitContext ctx = parse(in);
		
		buildReferenced(ctx);
		System.err.println();
		source = in.getSource();
		
		compileModule(ctx, in);
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

	private void compileModule(CompilationUnitContext ctx, FileDesignator in) throws Exception{
		for (ModuleDeclarationsContext mdc : ctx.moduleDeclaration().moduleDeclarations()){
			if (mdc.classDeclaration() != null){
				Class<?> klazz = compileClassDefinition(ctx.moduleDeclaration().children.get(1).getText(), mdc.classDeclaration(), in);
				PLRuntime.getRuntime().registerClass(moduleName, mdc.classDeclaration().children.get(1).getText(), klazz);
				
			}
		}
		
		compileModuleClass(ctx, in);
	}
	
	private int counter;
	private Map<Integer, Integer> labelMap;
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
	private enum RestrictedTo { CLASS, MODULE };
	private RestrictedTo isRestrictedMethodQualifier;
	
	@SuppressWarnings("unchecked")
	private void compileModuleClass(CompilationUnitContext ctx, FileDesignator in) throws Exception {
		String moduleName = ctx.moduleDeclaration().children.get(1).getText();
		compilingClass = false;
		
		cp = ClassPool.getDefault();

		cache = new HashMap<String, Integer>();
		String className = moduleName;
		File output = new File(in.getOutputDir(), className + ".class");
		
		cls = cp.makeClass(className);
		cls.setSuperclass(cp.getCtClass(Strings.MODULE_BASE_CLASS));
		
		// Serialization
		cls.addInterface(cp.getCtClass(Strings.SERIALIZABLE));
		CtField f = new CtField(CtClass.longType, Strings.SERIALIZATION_UID, cls);
		f.setModifiers(Modifier.STATIC | Modifier.FINAL);
		cls.addField(f, "" + PLRuntime.getRuntime().getUuid(className));
		
		CtConstructor ct = CtNewConstructor.defaultConstructor(cls);
		cls.addConstructor(ct);
		
		CtMethod serM = CtNewMethod.make("private java.lang.Object readResolve() { return cz.upol.vanusanik.paralang.runtime.PLRuntime.getRuntime().resolveModule(\""+ moduleName + "\"); }", cls);
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
				boolean restricted = fcx.getText().startsWith("restricted");
				String name = restricted ? fcx.getChild(1).getText() : fcx.getChild(0).getText();
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
		
		// Compile system init method
		final CtMethod m = CtNewMethod.make("protected void __init_internal_datafields() { return null; }", cls);
		new MethodCompiler(m){

			@Override
			protected void compileDataSources() throws Exception {
				isRestrictedMethodQualifier = RestrictedTo.MODULE; // default fields are always in restricted mode check off
				compileInitMethod(fields, methods);
			}
			
		}.compileMethod();
		
		// Compile all methods
		for (ModuleDeclarationsContext mdc : ctx.moduleDeclaration().moduleDeclarations()){
			if (mdc.functionDeclaration() != null){
				FunctionDeclarationContext fcx = mdc.functionDeclaration();
				compileFunction(fcx);
			}
		}
		
		varStack.popStack(); // pop class variables
		
		cls.debugWriteFile();
		cls.toBytecode(new DataOutputStream(new FileOutputStream(output)));
		
		PLRuntime.getRuntime().addModule(moduleName, cls.toClass());
	}
	
	private Class<?> compileClassDefinition(String moduleName, ClassDeclarationContext classDeclaration, FileDesignator in) throws Exception {
		compilingClass = true;
		
		cp = ClassPool.getDefault();
		
		cache = new HashMap<String, Integer>();
		String className = moduleName + "$" + classDeclaration.children.get(1).getText();
		File output = new File(in.getOutputDir(), className + ".class");
		
		cls = cp.makeClass(className);
		cls.setSuperclass(cp.getCtClass(Strings.CLASS_BASE_CLASS));
		
		// Serialization
		cls.addInterface(cp.getCtClass(Strings.SERIALIZABLE));
		CtField f = new CtField(CtClass.longType, Strings.SERIALIZATION_UID, cls);
		f.setModifiers(Modifier.STATIC | Modifier.FINAL);
		cls.addField(f, "" + PLRuntime.getRuntime().getUuid(className));
		
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
				if (methods.contains(name))
					throw new CompilationException("Already containing function " + name);
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
				isRestrictedMethodQualifier = RestrictedTo.CLASS; // default fields are always in restricted mode check off
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

	private boolean compilingInit;
	private String compileFunction(final FunctionDeclarationContext fcx) throws Exception{
		final boolean restricted = fcx.getText().startsWith("restricted");
		String name = restricted ? fcx.getChild(1).getText() : fcx.getChild(0).getText();
		
		isRestrictedMethodQualifier = restricted ? RestrictedTo.MODULE : null;
		if (name.equals("init")){
			isRestrictedMethodQualifier = compilingClass ? RestrictedTo.CLASS : RestrictedTo.MODULE;
			compilingInit = true;
		} else
			compilingInit = false;
		
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

	protected void compileFunction(FunctionBodyContext functionBody, boolean restricted) throws Exception {
		markLine(bc.currentPc(), functionBody.start.getLine());
		
		if (restricted){
			addGetRuntime();
			bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__CHECK_RESTRICTED_ACCESS, "()V");
		}
		
		compileBlock(functionBody.block());	
		
		markLine(bc.currentPc(), functionBody.stop.getLine());
		functionExitProtocol();
		addNil();
		bc.add(Opcode.ARETURN);
	}

	private void functionExitProtocol() {
		if (!compilingInit) 
			return;
		
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
						compileExpression(vd.variableInitializer().expression());
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
	private void compileStatement(StatementContext statement) throws Exception {
		markLine(bc.currentPc(), statement.start.getLine());
		if (statement.block() != null){
			compileBlock(statement.block());
			return;
		}
		
		if (statement.getText().startsWith("return")){
			if (statement.expression() != null){
				isStatementExpression.add(false);
				compileExpression(statement.expression());
				isStatementExpression.pop();
			} else {
				addNil();
			}
			
			functionExitProtocol();
			bc.add(Opcode.ARETURN);
		}
		
		if (statement.statementExpression() != null){
			isStatementExpression.add(true);
			compileExpression(statement.statementExpression().expression());
			isStatementExpression.pop();
		} else if (statement.getText().startsWith("if")){
			ExpressionContext e = statement.parExpression().expression();
			isStatementExpression.add(false);
			compileExpression(e);
			isStatementExpression.pop();
			bc.addInvokestatic(Strings.TYPEOPS, Strings.TYPEOPS__CONVERT_TO_BOOLEAN, 
					"("+ Strings.PLANGOBJECT_L + ")Z"); // boolean on stack
			boolean hasElse = statement.getChildCount() == 5;
			int key = counter++;
			addLabel(new LabelInfo(){

				@Override
				protected void add(Bytecode bc) throws CompilationException {
					int offset = getValue(poskey) - bcpos;
					bc.write(bcpos, Opcode.IFEQ); // jump to else if true or to the next bytecode if not 
					bc.write16bit(bcpos+1, offset);
				}
				
			}, key);
			int key2 = -1;
			
			compileStatement((StatementContext) statement.getChild(2));
			if (hasElse){
				key2 = counter++;
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
				compileStatement((StatementContext) statement.getChild(4));
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

	private void compileInitMethod(List<FieldDeclarationContext> fields, Set<String> methods) throws Exception{
		for (FieldDeclarationContext field : fields){
			compileField(field);
		}
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
						compileExpression(vd.variableInitializer().expression());
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
	private Set<String> bioperators = new HashSet<String>();
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
	}

	private void compileExpression(ExpressionContext expression) throws Exception {
		markLine(bc.currentPc(), expression.start.getLine());
		if (expression.primary() != null){
			compilePrimaryExpression(expression.primary());
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
				compileExpression((ExpressionContext) init);
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
				String fqNameS = Utils.slashify(fqName);
				
				if (isConstructorCall){
					addGetRuntime();
					bc.addNew(fqNameS);
					bc.add(Opcode.DUP);
					compileParameters(expression.expressionList());
					bc.addInvokespecial(fqNameS, 
							"<init>", "([" +  Strings.PLANGOBJECT_L + ")V");
					bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__WRAP_JAVA_OBJECT, 
							"(" + Strings.OBJECT_L +")" + Strings.PLANGOBJECT_L);
				} else {
					// TODO
					
				}
			}
		} else if (expression.methodCall() != null){
			// method call
			addGetRuntime();
			isStatementExpression.add(false);
			compileExpression((ExpressionContext) expression.getChild(0));
			isStatementExpression.pop();
			compileParameters(expression.methodCall().expressionList());
			bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__RUN, 
					"(" + Strings.PLANGOBJECT_L + "[" + Strings.PLANGOBJECT_L +")" + Strings.PLANGOBJECT_L);
			
		} else if (expression.getChild(0) instanceof ExpressionContext){
			String operator = expression.getChild(1).getText();
			if (bioperators.contains(operator)){
				compileBinaryOperator(operator, 
						(ExpressionContext)expression.getChild(0), (ExpressionContext)expression.getChild(2));
			} else if ("||".equals(operator) || "&&".equals(operator)) {
				compileLogic((ExpressionContext)expression.getChild(0), (ExpressionContext)expression.getChild(2),
						"||".equals(operator));
			} else {
				isStatementExpression.add(false);
				compileExpression((ExpressionContext) expression.getChild(0));
				isStatementExpression.pop();
				if (expression.getChild(1).getText().equals(".")){
					// compiling field accessor
					markLine(bc.currentPc(), expression.stop.getLine());
					String identifier = expression.getChild(2).getText();
					bc.addCheckcast(Strings.BASE_COMPILED_STUB);
					bc.addLdc(cacheStrings(identifier));			// load string from constants
					bc.addInvokevirtual(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__GETKEY, 
							"(" + Strings.STRING_L +")" + Strings.PLANGOBJECT_L);
				}	
			}
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
		} else if (expression.getChildCount() == 3){
			String operator = expression.getChild(1).getText();
			
			if (setOperators.contains(operator)){
				compileSetOperator(operator, expression);
			}
		} 
	}

	private void compileLogic(ExpressionContext left,
			ExpressionContext right, final boolean or) throws Exception {
		
		bc.addAload(0);
		
		isStatementExpression.add(false);
		compileExpression(left);
		isStatementExpression.pop();
		
		bc.addInvokestatic(Strings.TYPEOPS, Strings.TYPEOPS__CONVERT_TO_BOOLEAN, 
				"("+ Strings.PLANGOBJECT_L + ")Z"); // boolean on stack
		
		int shortCut = counter++;
		int reminder = counter++;
		addLabel(new LabelInfo(){

			@Override
			protected void add(Bytecode bc) throws CompilationException {
				int offset = getValue(poskey) - bcpos;
				bc.write(bcpos, or ? Opcode.IFNE : Opcode.IFEQ);  
				bc.write16bit(bcpos+1, offset);
			}
			
		}, shortCut);
		
		isStatementExpression.add(false);
		compileExpression(right);
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
	}

	private void compileBinaryOperator(String operator,
			ExpressionContext expression1, ExpressionContext expression2) throws Exception {
		
		isStatementExpression.add(false);
		compileExpression(expression1);
		compileExpression(expression2);
		isStatementExpression.pop();
		
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
		
		bc.addInvokestatic(Strings.TYPEOPS, method, 
				"("+ Strings.PLANGOBJECT_L + Strings.PLANGOBJECT_L + ")" + Strings.PLANGOBJECT_L);
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
				compileExpression(e);
				isStatementExpression.pop();
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
				isStatementExpression.add(false);
				compileExpression(second);
				isStatementExpression.pop();
				
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
				
				{
					/*
					 * Prepares the store operation by getting either class or module on the stack and ready
					 */
					if (vt != null && vt == VariableType.CLASS_VARIABLE){
						bc.addAload(0); 								// load this
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
				
				if (isStatementExpression.peek() == false){
					/* Put new value on stack */
					if (vt != null && vt == VariableType.CLASS_VARIABLE){
						bc.addAload(0); 								// load this
					} else if (vt != null && vt == VariableType.MODULE_VARIABLE) {
						addGetRuntime();
						bc.addLdc(cacheStrings(moduleName));
						bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__GET_MODULE, 
								"(" + Strings.STRING_L + ")" + Strings.PL_MODULE_L); // get module on stack or fail
						bc.addCheckcast(Strings.BASE_COMPILED_STUB);
					} else {
						compilePrimaryExpression(lvalue.identified().primary());
						bc.addCheckcast(Strings.BASE_COMPILED_STUB);
					}
					
					bc.addLdc(cacheStrings(identifier));			// load string from constants
					bc.addInvokevirtual(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__GETKEY, 
							"(" + Strings.STRING_L +")" + Strings.PLANGOBJECT_L);
				}
			} else {
				/* Loads the local varaible on stack, then writes to the same position */
				if (!simpleSet)
					bc.addAload(ord);
				compileRight();
				bc.addAstore(ord);
				if (isStatementExpression.peek() == false){
					bc.addAload(ord);
				}
			}
			
			
		}
		
		public abstract void compileRight() throws Exception;
	}

	private void compilePrimaryExpression(PrimaryContext primary) throws Exception {
		if (primary.expression() != null){
			isStatementExpression.add(false);
			compileExpression(primary.expression());
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
		}
		
		if (primary.constExpr() != null || primary.getText().startsWith("inst") || primary.getText().startsWith("parent")){
			String identifier = primary.constExpr().id() != null ? primary.constExpr().id().getText() : primary.getText();
			
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

	private abstract class MethodCompiler {
		private CtMethod m;
		public MethodCompiler(CtMethod m){
			this.m = m;
		}
		@SuppressWarnings("unchecked")
		public void compileMethod() throws Exception{
			lastLineWritten = -1;
			stacker = new AutoIntStacker(1);
			counter = 0;
			labelMap = new HashMap<Integer, Integer>();
			labelList = new ArrayList<LabelInfo>();
			
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
			m.getMethodInfo().rebuildStackMap(cp);
			
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

