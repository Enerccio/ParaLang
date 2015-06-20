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
import cz.upol.vanusanik.paralang.runtime.PLClass;
import cz.upol.vanusanik.paralang.runtime.PLModule;
import cz.upol.vanusanik.paralang.runtime.PLRuntime;
import cz.upol.vanusanik.paralang.utils.Utils;

/**
 * Main compiler. Compiles source file designator into java modules bound to
 * current runtime. Needs a bound runtime via PLRuntime.setAsCurrent()
 * 
 * @author Enerccio
 *
 */
public class PLCompiler {

	// static initialization and static variables

	/** Set operators are listed here */
	private static final Set<String> setOperators = new HashSet<String>();
	/** Binary operator are listed here */
	private static final Set<String> bioperators = new HashSet<String>();
	/** Left operators are listed here */
	private static final Set<String> leftOperators = new HashSet<String>();
	/** Right operators are listed here */
	private static final Set<String> rightOperators = new HashSet<String>();
	static {
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

	// Inner helper classes definition

	/**
	 * Finally block protocol is simple helper interface used to provide work
	 * when nonlocal exit happens and there is finally block that needs to be
	 * processed when nonlocal exit happens.
	 * 
	 * @author Enerccio
	 *
	 */
	private static interface FinallyBlockProtocol {
		public void doCompile() throws Exception;
	}

	/**
	 * Error handler when source code is wrong.
	 * 
	 * @author Enerccio
	 *
	 */
	private class ThrowingErrorListener extends BaseErrorListener {
		private String source;

		public ThrowingErrorListener(String loc) {
			this.source = loc;
		}

		@Override
		public void syntaxError(Recognizer<?, ?> recognizer,
				Object offendingSymbol, int line, int charPositionInLine,
				String msg, RecognitionException e)
				throws ParseCancellationException {
			throw new ParseCancellationException("file " + source + " line "
					+ line + ":" + charPositionInLine + " " + msg);
		}
	}

	/**
	 * BlockDescription container, contains AST of the block and name of the
	 * generated method
	 * 
	 * @author Enerccio
	 *
	 */
	private static class BlockDescription {
		public BlockContext b;
		public String mn;
	}

	/**
	 * Helper class containing compilation into class/module field
	 * 
	 * @author Enerccio
	 *
	 */
	private abstract class StoreToField {
		private String varId;

		public StoreToField(String varId) {
			this.varId = varId;
		}

		/**
		 * This method should compile a way to provide data (ie compile
		 * expression or add NoValue etc).
		 * 
		 * @throws Exception
		 */
		protected abstract void provideSourceValue() throws Exception;

		/**
		 * Compiles the store to field
		 * 
		 * @throws Exception
		 */
		public void compile() throws Exception {

			if (compilingClass && !cmpInitFuncwraps)
				bc.addAload(1); // load self
			else
				bc.addAload(0); // load this
			bc.addCheckcast(Strings.BASE_COMPILED_STUB);
			bc.addLdc(cacheStrings(varId)); // load string from constants
			provideSourceValue();
			bc.addCheckcast(Strings.PLANGOBJECT); // cast obj to PLangObject
			bc.addInvokevirtual(Strings.BASE_COMPILED_STUB,
					Strings.BASE_COMPILED_STUB__SETKEY, "(" + Strings.STRING_L
							+ Strings.PLANGOBJECT_L + ")V");
		}
	}

	/**
	 * Helper class to compile set operator
	 * 
	 * @author Enerccio
	 *
	 */
	private abstract class CompileSetOperator {
		/** Lvalue of set operator */
		private ExtendedContext lvalue;
		/** Whether this is simple set or set with operation */
		private boolean simpleSet;
		/** Whether we are compiling method accessor or not */
		private boolean compilingMethod;
		/** Local variable to store when compiling method accessor */
		private int storeVar;

		public CompileSetOperator(ExtendedContext lvalue, boolean simpleSet,
				boolean compilingMethod, int storeVar) {
			this.lvalue = lvalue;
			this.simpleSet = simpleSet;
			this.compilingMethod = compilingMethod;
			this.storeVar = storeVar;
		}

		/**
		 * Compiles set operator
		 * 
		 * @throws Exception
		 */
		public void compileSetOperator() throws Exception {
			markLine(bc.currentPc(), lvalue.start.getLine());
			String identifier = null;
			VariableType vt = null;
			int ord = -1;
			if (lvalue.constExpr() != null) {
				identifier = lvalue.constExpr().id() != null ? lvalue
						.constExpr().id().getText() : lvalue.getText();
				vt = varStack.getType(identifier);
				if (vt == VariableType.LOCAL_VARIABLE)
					ord = varStack.getLocal(identifier);
			} else {
				vt = null;
				compilePrimaryExpression(lvalue.identified().primary(), false,
						-1);
				identifier = lvalue.identified().getChild(2).getText();
			}

			if (vt == null || vt != VariableType.LOCAL_VARIABLE) {

				{
					/*
					 * Prepares the store operation by getting either class or
					 * module on the stack and ready
					 */
					if (vt != null && vt == VariableType.CLASS_VARIABLE) {
						if (compilingClass)
							bc.addAload(1); // load self
						else
							bc.addAload(0); // load this
						bc.addCheckcast(Strings.BASE_COMPILED_STUB);
					} else if (vt != null && vt == VariableType.MODULE_VARIABLE) {
						addGetRuntime();
						bc.addLdc(cacheStrings(moduleName));
						bc.addInvokevirtual(Strings.RUNTIME,
								Strings.RUNTIME__GET_MODULE, "("
										+ Strings.STRING_L + ")"
										+ Strings.PL_MODULE_L); // get module on
																// stack or fail
					}

					bc.addCheckcast(Strings.BASE_COMPILED_STUB);
					bc.addLdc(cacheStrings(identifier)); // load string from
															// constants
				}

				if (!simpleSet) {
					/*
					 * Loads the old value on the stack either from instance or
					 * from module field
					 */
					if (vt != null && vt == VariableType.CLASS_VARIABLE) {
						if (compilingClass)
							bc.addAload(1); // load self
						else
							bc.addAload(0); // load this
						bc.addCheckcast(Strings.BASE_COMPILED_STUB);
					} else if (vt != null && vt == VariableType.MODULE_VARIABLE) {
						addGetRuntime();
						bc.addLdc(cacheStrings(moduleName));
						bc.addInvokevirtual(Strings.RUNTIME,
								Strings.RUNTIME__GET_MODULE, "("
										+ Strings.STRING_L + ")"
										+ Strings.PL_MODULE_L); // get module on
																// stack or fail
						bc.addCheckcast(Strings.BASE_COMPILED_STUB);
					}
					bc.addLdc(cacheStrings(identifier)); // load string from
															// constants
					bc.addIconst(0);
					bc.addInvokevirtual(Strings.BASE_COMPILED_STUB,
							Strings.BASE_COMPILED_STUB__GETKEY, "("
									+ Strings.STRING_L + "Z)"
									+ Strings.PLANGOBJECT_L);

				}

				compileRight();

				/* Stores the value into __setKey of the object on stack */
				bc.addInvokevirtual(Strings.BASE_COMPILED_STUB,
						Strings.BASE_COMPILED_STUB__SETKEY, "("
								+ Strings.STRING_L + Strings.PLANGOBJECT_L
								+ ")V");

				if (isStatementExpression.peek() == false) {
					/* Put new value on stack */
					if (vt != null && vt == VariableType.CLASS_VARIABLE) {
						if (compilingClass)
							bc.addAload(1); // load self
						else
							bc.addAload(0); // load this
						bc.addCheckcast(Strings.BASE_COMPILED_STUB);
					} else if (vt != null && vt == VariableType.MODULE_VARIABLE) {
						addGetRuntime();
						bc.addLdc(cacheStrings(moduleName));
						bc.addInvokevirtual(Strings.RUNTIME,
								Strings.RUNTIME__GET_MODULE, "("
										+ Strings.STRING_L + ")"
										+ Strings.PL_MODULE_L); // get module on
																// stack or fail
						bc.addCheckcast(Strings.BASE_COMPILED_STUB);
					} else {
						compilePrimaryExpression(lvalue.identified().primary(),
								false, -1);
						bc.addCheckcast(Strings.BASE_COMPILED_STUB);
					}

					bc.addLdc(cacheStrings(identifier)); // load string from
															// constants
					bc.addIconst(0);
					bc.addInvokevirtual(Strings.BASE_COMPILED_STUB,
							Strings.BASE_COMPILED_STUB__GETKEY, "("
									+ Strings.STRING_L + "Z)"
									+ Strings.PLANGOBJECT_L);
				} else {
					addNil();
				}

				if (compilingMethod) {
					bc.add(Opcode.DUP);
					bc.addAstore(storeVar);
				}
			} else {
				/*
				 * Loads the local variable on stack, then writes to the same
				 * position
				 */
				if (!simpleSet)
					bc.addAload(ord);
				compileRight();
				bc.addAstore(ord);
				if (isStatementExpression.peek() == false) {
					bc.addAload(ord);
				} else {
					addNil();
				}

				if (compilingMethod) {
					bc.add(Opcode.DUP);
					bc.addAstore(storeVar);
				}
			}

		}

		/**
		 * Provide data for the right side of the set operator
		 * 
		 * @throws Exception
		 */
		public abstract void compileRight() throws Exception;
	}

	/**
	 * LabelInfo class holds position to label in bytecode and key to the label
	 * it is bound to
	 * 
	 * @author Enerccio
	 *
	 */
	private abstract class LabelInfo {
		public int bcpos;
		public int poskey;

		/**
		 * Compiles the jump
		 * 
		 * @param bc
		 * @throws CompilationException
		 */
		protected abstract void add(Bytecode bc) throws CompilationException;
	}

	/**
	 * Jump variant of the LabelInfo
	 * 
	 * @author Enerccio
	 *
	 */
	private class JumpLabelInfo extends LabelInfo {

		@Override
		protected void add(Bytecode bc) throws CompilationException {
			int offset = getValue(poskey) - bcpos;
			if (Math.abs(offset) > (65535 / 2)) {
				throw new CompilationException(
						"Too long jump. Please reformate the code!");
			} else {
				bc.write(bcpos, Opcode.GOTO);
				bc.write16bit(bcpos + 1, offset);
			}
		}

	};

	/**
	 * IFEQ variant of the LabelInfo
	 * 
	 * @author Enerccio
	 *
	 */
	private class IfEqJumpLabelInfo extends LabelInfo {

		@Override
		protected void add(Bytecode bc) throws CompilationException {
			int offset = getValue(poskey) - bcpos;
			bc.write(bcpos, Opcode.IFEQ); // jump to else if true or to the next
											// bytecode if not
			bc.write16bit(bcpos + 1, offset);
		}

	}

	/**
	 * IFNE variant of LabelInfo
	 * 
	 * @author Enerccio
	 *
	 */
	private class IfNeJumpLabelInfo extends LabelInfo {

		@Override
		protected void add(Bytecode bc) throws CompilationException {
			int offset = getValue(poskey) - bcpos;
			bc.write(bcpos, Opcode.IFNE); // jump to else if true or to the next
											// bytecode if not
			bc.write16bit(bcpos + 1, offset);
		}

	}

	/**
	 * IFNNONULL variant of LabelInfo
	 * 
	 * @author Enerccio
	 *
	 */
	private class IfNotNullJumpLabelInfo extends LabelInfo {

		@Override
		protected void add(Bytecode bc) throws CompilationException {
			int offset = getValue(poskey) - bcpos;
			bc.write(bcpos, Opcode.IFNONNULL);
			bc.write16bit(bcpos + 1, offset);
		}

	}

	/**
	 * Helper class compiling method, doing all the grunt work of setting it up
	 * 
	 * @author Enerccio
	 *
	 */
	private abstract class MethodCompiler {
		private CtMethod m;

		public MethodCompiler(CtMethod m) {
			this.m = m;
		}

		/**
		 * Compiles method
		 * 
		 * @throws Exception
		 */
		@SuppressWarnings("unchecked")
		public void compileMethod() throws Exception {
			// initialize methods used during compilation of method/function
			lastLineWritten = -1;
			stacker = new AutoIntStacker(1);
			labelCounter = 0;
			labelMap = new HashMap<Integer, Integer>();
			labelList = new ArrayList<LabelInfo>();
			bcToLineMap = new HashMap<Integer, Integer>();

			pool = m.getMethodInfo().getConstPool();
			// add source file attribute
			SourceFileAttribute attr = new SourceFileAttribute(pool, source);
			cls.getClassFile().addAttribute(attr);

			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			lineNumberStream = new DataOutputStream(stream);
			lineNumberStream.writeShort(0);

			bc = new Bytecode(pool);

			// compile body of the method/function
			compileDataSources();

			// compile all labels used in the method
			for (LabelInfo nfo : labelList)
				nfo.add(bc);

			// prune dead code in the compiled bytecode
			pruneDeadCode();

			// write line numbers
			byte[] bytes = stream.toByteArray();
			int size = (bytes.length - 2) / 4;
			bytes[0] = (byte) ((size >>> 8) & 0xFF);
			bytes[1] = (byte) ((size >>> 0) & 0xFF);
			AttributeInfo lineNubmerInfo = new AttributeInfo(pool,
					LineNumberAttribute.tag, bytes);

			CodeAttribute at = bc.toCodeAttribute();
			// set stacker size and maximum number of locals
			at.computeMaxStack();
			at.setMaxLocals(stacker.getMax());
			at.getAttributes().add(lineNubmerInfo);

			// build stackmaps
			m.getMethodInfo().setCodeAttribute(at);
			m.getMethodInfo().rebuildStackMap(cp);

			// add method to the class
			cls.addMethod(m);
		}

		/**
		 * Actually compiles the bytecode
		 * 
		 * @throws Exception
		 */
		protected abstract void compileDataSources() throws Exception;
	}

	/**
	 * Instruction is disected in this class. Contains also visited flag and
	 * other various information.
	 * 
	 * @author Enerccio
	 *
	 */
	private static class Instruction {
		/**
		 * Whether instruction was visited
		 */
		private boolean visited = false;
		/**
		 * Line number of this instruction
		 */
		private Integer lineNo = null;
		/**
		 * bytes of this instruction
		 */
		private byte[] instBytes = null;
		/**
		 * Instruction this instruction branches to, if it does
		 */
		private Instruction branchTarget = null;
		/**
		 * Original position in the whole bytecode
		 */
		private int originalPos;

		@Override
		public String toString() {
			return Mnemonic.OPCODE[instBytes[0] & 0x000000FF] + ": "
					+ visited;
		}
	}

	/**
	 * Exception handler constants are stored here
	 * 
	 * @author Enerccio
	 *
	 */
	private static class ExceptionHandler {
		Integer startLink;
		Integer endLink;
		Integer bcLink;
		int type;
	}

	/**
	 * IntegerLink is class holding integer value and whether it is start
	 * position or not
	 * 
	 * @author Enerccio
	 *
	 */
	private static class IntegerLink {
		public IntegerLink(int i) {
			this.i = i;
		}

		private int i;
		private boolean isStartPos = false;
	}

	/**
	 * Full Exception data is stored here
	 * 
	 * @author Enerccio
	 *
	 */
	private static class ExceptionData {
		/** Exception constants */
		private int s, e, h, t;

		public ExceptionData(int s, int e, int h, int t) {
			super();
			this.s = s;
			this.e = e;
			this.h = h;
			this.t = t;
		}
	}

	// Fields

	/** Holds the references to a simple type, ie String -> Reference */
	private Map<String, Reference> referenceMap = new HashMap<String, Reference>();
	/** Source file name */
	private String source;
	/** Current module name */
	private String moduleName;
	/*
	 * Label handling, labelCounter provides unique label ids, then map maps it
	 * into current bytecode
	 */
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
	/**
	 * Local variables stacker providing how many locals are being used and
	 * reused
	 */
	private AutoIntStacker stacker;
	/** String cache caching strings into consts */
	private HashMap<String, Integer> cache;
	/**
	 * Variable Scope is decided by this stack, decides whether variable is
	 * local, class or module
	 */
	private VariableScopeStack varStack;
	/** Whether init method is being compiled */
	private boolean compilingInit;
	/**
	 * List of finally block protocol instances, used to mark when nonlocal exit
	 * happens in finally blocks on the stack
	 */
	private List<FinallyBlockProtocol> fbcList;
	/**
	 * List of finally block protocols for loops.
	 */
	private List<FinallyBlockProtocol> fbcLoopList;

	/**
	 * Stack of the statement expressions. Contains true/false. Every time an
	 * expression is being compiled, either true or false will be on this stack.
	 * If it is true, compiler then can optimize out parts of the compilation
	 * that would end up with copies of new variables and pops due to no need to
	 * store result of the expression. If it is false, compiler has to store the
	 * result of the expression on the stack.
	 */
	private Stack<Boolean> isStatementExpression = new Stack<Boolean>();
	/**
	 * Continue stack contains the bytecode position where should continue hop
	 * to if it happens.
	 */
	private Stack<Integer> continueStack = new Stack<Integer>();
	/**
	 * Break stack contains the bytecode position where should break hop to if
	 * it happens
	 */
	private Stack<Integer> breakStack = new Stack<Integer>();

	/**
	 * List of blocks of codes that are in the distributed expression, which
	 * will be later compiled into auxiliary methods
	 */
	private List<BlockDescription> distributed = new ArrayList<BlockDescription>();
	/**
	 * Marks whether we are compiling function wrappers or not, used to
	 * determine whether we use this or self
	 */
	private boolean cmpInitFuncwraps = false;
	/** list of labels defined */
	private List<LabelInfo> labelList;
	/** links bytecode into line number */
	private Map<Integer, Integer> bcToLineMap;

	// Methods

	/**
	 * Compiles file and loads it into current runtime
	 * 
	 * @param in
	 * @return
	 * @throws Exception
	 */
	public String compile(FileDesignator in) throws Exception {
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
	 * 
	 * @param ctx
	 */
	private void buildReferenced(CompilationUnitContext ctx) {
		moduleName = ctx.moduleDeclaration().Identifier().getText();
		varStack = new VariableScopeStack();

		List<ImportDeclarationContext> idc = ctx.importDeclaration();
		if (idc != null) {
			for (ImportDeclarationContext id : idc) {
				Reference r = null;
				String name = null;
				if (id.getText().startsWith("import")) {
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

		// Add all classes declared in this module into references for self
		// references to it's own types
		for (ModuleDeclarationsContext mdc : ctx.moduleDeclaration()
				.moduleDeclarations()) {
			if (mdc.classDeclaration() != null) {
				String name = mdc.classDeclaration().children.get(1).getText();
				Reference r = new Reference(moduleName + "." + name, name,
						false);
				referenceMap.put(name, r);
			}
		}

		// System classes are always referenced
		for (String cn : PLRuntime.SYSTEM_CLASSES.keySet()) {
			Reference r = new Reference("System." + cn, cn, false);
			referenceMap.put(cn, r);
		}
	}

	/**
	 * Parses FileDesignator into AST
	 * 
	 * @param in
	 * @return
	 * @throws Exception
	 */
	private CompilationUnitContext parse(FileDesignator in) throws Exception {
		ANTLRInputStream is = new ANTLRInputStream(in.getStream());
		PLangLexer lexer = new PLangLexer(is);
		lexer.removeErrorListeners();
		lexer.addErrorListener(new ThrowingErrorListener(in.getSource()));
		CommonTokenStream stream = new CommonTokenStream(lexer);
		PLangParser parser = new PLangParser(stream);
		parser.removeErrorListeners();
		parser.addErrorListener(new ThrowingErrorListener(in.getSource()));
		return parser.compilationUnit();
	}

	/**
	 * Compiles module and class declarations
	 * 
	 * @param ctx
	 * @param in
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private void compileModule(CompilationUnitContext ctx, FileDesignator in)
			throws Exception {

		for (ModuleDeclarationsContext mdc : ctx.moduleDeclaration()
				.moduleDeclarations()) {
			if (mdc.classDeclaration() != null) {
				// Compile class definition
				Class<?> klazz = compileClassDefinition(ctx.moduleDeclaration()
						.Identifier().getText(), mdc.classDeclaration(), in);
				PLRuntime.getRuntime()
						.registerClass(
								moduleName,
								mdc.classDeclaration().children.get(1)
										.getText(), klazz);
			}
		}

		// Compile module definition
		Class<?> klazz = compileModuleClass(ctx, in);
		PLRuntime.getRuntime().addModule(moduleName,
				(Class<? extends PLModule>) klazz);
	}

	/**
	 * Compiles module into java class
	 * 
	 * @param ctx
	 *            CompilationUnitContext AST
	 * @param in
	 *            FileDesignator of the source
	 * @return Compiled class
	 * @throws Exception
	 */
	private Class<?> compileModuleClass(CompilationUnitContext ctx,
			FileDesignator in) throws Exception {
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

		CtField f = new CtField(CtClass.longType, Strings.SERIALIZATION_UID,
				cls);
		f.setModifiers(Modifier.STATIC | Modifier.FINAL);
		cls.addField(f, "" + PLRuntime.getRuntime().getUuid(className));

		CtConstructor ct = CtNewConstructor.defaultConstructor(cls);
		cls.addConstructor(ct);

		CtMethod serM = CtNewMethod
				.make("private java.lang.Object readResolve() "
						+ "{ return cz.upol.vanusanik.paralang.runtime.PLRuntime.getRuntime()"
						+ ".resolveModule(\"" + moduleName + "\"); }", cls);
		cls.addMethod(serM);

		final List<FieldDeclarationContext> fields = new ArrayList<FieldDeclarationContext>();
		// List all fields
		for (ModuleDeclarationsContext mdc : ctx.moduleDeclaration()
				.moduleDeclarations()) {
			FieldDeclarationContext fdc = mdc.fieldDeclaration();
			if (fdc != null)
				fields.add(fdc);
		}

		varStack.pushNewStack(); // push module variables
		varStack.addVariable("init", VariableType.CLASS_VARIABLE);

		// Find all methods
		final Set<String> methods = new HashSet<String>();
		for (ModuleDeclarationsContext mdc : ctx.moduleDeclaration()
				.moduleDeclarations()) {
			if (mdc.functionDeclaration() != null) {
				FunctionDeclarationContext fcx = mdc.functionDeclaration();
				boolean restricted = fcx.getChild(1).getText()
						.startsWith("restricted");
				String name = restricted ? fcx.getChild(2).getText() : fcx
						.getChild(1).getText();
				if (methods.contains(name))
					throw new CompilationException(
							"Already containing function " + name);
				methods.add(name);
			}
		}

		// If there is no init, create empty init method without arguments
		if (!methods.contains("init")) {
			CtMethod initM = CtNewMethod
					.make("public cz.upol.vanusanik.paralang.plang.PLangObject init() { return cz.upol.vanusanik.paralang.plang.types.NoValue.NOVALUE; }",
							cls);
			cls.addMethod(initM);
			methods.add("init");
		}

		// Add all fields declared in this class into class type variables
		for (FieldDeclarationContext fdc : fields) {
			for (final VariableDeclaratorContext vd : fdc.variableDeclarators()
					.variableDeclarator()) {
				String varId = vd.variableDeclaratorId().getText();
				varStack.addVariable(varId, VariableType.CLASS_VARIABLE);
			}
		}

		// Compile all methods
		for (ModuleDeclarationsContext mdc : ctx.moduleDeclaration()
				.moduleDeclarations()) {
			if (mdc.functionDeclaration() != null) {
				FunctionDeclarationContext fcx = mdc.functionDeclaration();
				compileFunction(fcx);
			}
		}

		// Compile distributed auxiliary methods
		for (BlockDescription bd : distributed) {
			methods.add(compileFunction(bd));
		}

		// Compile system init method
		final CtMethod m = CtNewMethod
				.make("protected void ___init_internal_datafields(cz.upol.vanusanik.paralang.runtime.BaseCompiledStub self) {  }",
						cls);
		new MethodCompiler(m) {

			@Override
			protected void compileDataSources() throws Exception {
				compileInitMethod(fields, methods, null);
			}

		}.compileMethod();

		// pop class variables
		varStack.popStack();
		// cls.debugWriteFile();

		// grab class bytedata and save it into runtime
		byte[] bytedata = cls.toBytecode();
		PLRuntime.getRuntime().addModuleBytedata(moduleName, bytedata);

		return cls.toClass(getClassLoader(), null);
	}

	/**
	 * Compile plang class into java class
	 * 
	 * @param moduleName
	 * @param classDeclaration
	 * @param in
	 * @return
	 * @throws Exception
	 */
	private Class<?> compileClassDefinition(String moduleName,
			ClassDeclarationContext classDeclaration, FileDesignator in)
			throws Exception {
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
		CtField f = new CtField(CtClass.longType, Strings.SERIALIZATION_UID,
				cls);
		f.setModifiers(Modifier.STATIC | Modifier.FINAL);
		cls.addField(f, "" + PLRuntime.getRuntime().getUuid(className));

		genBootstraps();

		// Empty constructor. Every class is a bean
		CtConstructor ct = CtNewConstructor.defaultConstructor(cls);
		cls.addConstructor(ct);

		List<ClassBodyDeclarationContext> cbdList = classDeclaration
				.classBody().classBodyDeclaration();

		final List<FieldDeclarationContext> fields = new ArrayList<FieldDeclarationContext>();
		// List all fields
		for (ClassBodyDeclarationContext cbd : cbdList) {
			FieldDeclarationContext fdc = cbd.memberDeclaration()
					.fieldDeclaration();
			if (fdc != null)
				fields.add(fdc);
		}

		varStack.pushNewStack(); // push class variables
		varStack.addVariable("init", VariableType.CLASS_VARIABLE);

		// Find all methods
		final Set<String> methods = new HashSet<String>();
		for (ClassBodyDeclarationContext cbd : cbdList) {
			if (cbd.memberDeclaration().functionDeclaration() != null) {
				FunctionDeclarationContext fcx = cbd.memberDeclaration()
						.functionDeclaration();
				boolean restricted = fcx.getChild(1).getText()
						.equals("restricted");
				String name = restricted ? fcx.getChild(2).getText() : fcx
						.getChild(1).getText();
				if (methods.contains(name))
					throw new CompilationException(
							"Already containing function " + name);
				methods.add(name);
			}
		}

		// Get all variables as class variables
		for (FieldDeclarationContext fdc : fields) {
			for (final VariableDeclaratorContext vd : fdc.variableDeclarators()
					.variableDeclarator()) {
				String varId = vd.variableDeclaratorId().getText();
				varStack.addVariable(varId, VariableType.CLASS_VARIABLE);
			}
		}

		// Compile all methods
		for (ClassBodyDeclarationContext cbd : cbdList) {
			if (cbd.memberDeclaration().functionDeclaration() != null) {
				FunctionDeclarationContext fcx = cbd.memberDeclaration()
						.functionDeclaration();
				compileFunction(fcx);
			}
		}

		// Compile all dist blocks as separate system functions
		for (BlockDescription bd : distributed) {
			methods.add(compileFunction(bd));
		}

		// Compile system init method
		final CtMethod m = CtNewMethod
				.make("protected void ___init_internal_datafields(cz.upol.vanusanik.paralang.runtime.BaseCompiledStub self) { }",
						cls);
		new MethodCompiler(m) {

			@Override
			protected void compileDataSources() throws Exception {
				compileInitMethod(fields, methods, sc);
			}

		}.compileMethod();

		// pop class variables
		varStack.popStack();

		// cls.debugWriteFile();

		// Grab bytedata of the result class and store it in the runtime
		byte[] bytedata = cls.toBytecode();
		PLRuntime.getRuntime().addClassBytedata(moduleName, clsName, bytedata);

		return cls.toClass(getClassLoader(), null);
	}

	/**
	 * Generates handlers for boostrap for dynamic methods of operators. Needed
	 * to be created once per java class
	 */
	// Magic method, DO NOT TOUCH!
	private void genBootstraps() {
		pool = cls.getClassFile().getConstPool();
		// 0 binary operation
		int mRefIdxBin = pool
				.addMethodrefInfo(
						pool.addClassInfo(Strings.TYPEOPS),
						pool.addNameAndTypeInfo(
								"binopbootstrap",
								"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
										+ ")Ljava/lang/invoke/CallSite;"));
		int mHandleIdxBin = pool.addMethodHandleInfo(
				ConstPool.REF_invokeStatic, mRefIdxBin);
		// 1 unary operation
		int mRefIdxUn = pool
				.addMethodrefInfo(
						pool.addClassInfo(Strings.TYPEOPS),
						pool.addNameAndTypeInfo(
								"unopbootstrap",
								"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
										+ ")Ljava/lang/invoke/CallSite;"));
		int mHandleIdxUn = pool.addMethodHandleInfo(ConstPool.REF_invokeStatic,
				mRefIdxUn);

		/*
		 * create bootstrap methods attribute; there can only be one per class
		 * file!
		 */
		BootstrapMethodsAttribute.BootstrapMethod[] bms = new BootstrapMethodsAttribute.BootstrapMethod[] {
				new BootstrapMethodsAttribute.BootstrapMethod(mHandleIdxBin,
						new int[] {}),
				new BootstrapMethodsAttribute.BootstrapMethod(mHandleIdxUn,
						new int[] {}) };
		BootstrapMethodsAttribute bmsAttribute = new BootstrapMethodsAttribute(
				pool, bms);
		cls.getClassFile().addAttribute(bmsAttribute);
	}

	/**
	 * Compiles function/method of module/class.
	 * 
	 * @param fcx
	 * @return
	 * @throws Exception
	 */
	private String compileFunction(final FunctionDeclarationContext fcx)
			throws Exception {
		final boolean restricted = fcx.getChild(1).getText()
				.startsWith("restricted");
		String name = restricted ? fcx.getChild(2).getText() : fcx.getChild(1)
				.getText();

		if (name.equals("init")) {
			compilingInit = true;
		} else
			compilingInit = false;

		// Transform plang arguments into method arguments
		FormalParametersContext fpx = fcx.formalParameters();
		final List<String> args = new ArrayList<String>();
		if (compilingClass) // if class is being compiled, it's method have
							// implicit "inst" (this)
			args.add("inst");
		if (fpx.formalParameterList() != null) {
			for (FormalParameterContext fpcx : fpx.formalParameterList()
					.formalParameter()) {
				args.add(fpcx.getText());
			}
		}
		String methArgsSign = "";
		for (String arg : args)
			methArgsSign += Strings.PLANGOBJECT_N + " " + arg + ", ";

		methArgsSign = StringUtils.removeEnd(methArgsSign, ", ");

		// final method signature
		String methodSignature = "public " + Strings.PLANGOBJECT_N + " " + name
				+ "(" + methArgsSign + "){ return null; }";

		// Actually compile method/function
		final CtMethod m = CtNewMethod.make(methodSignature, cls);
		new MethodCompiler(m) {

			@Override
			protected void compileDataSources() throws Exception {
				if (compilingInit) {
					bc.addAload(0); // load this
					bc.addIconst(1); // add true
					bc.addPutfield(Strings.BASE_COMPILED_STUB,
							Strings.BASE_COMPILED_STUB__RESTRICTED_OVERRIDE,
							"Z");
				}

				varStack.pushNewStack();
				for (String arg : args) {
					varStack.addVariable(arg, VariableType.LOCAL_VARIABLE,
							stacker.acquire());
				}
				compileFunction(fcx.functionBody(), restricted);
				varStack.popStack();
			}

		}.compileMethod();

		return name;

	}

	/**
	 * Compiles auxiliary method/function. These are created when dist
	 * expression is accessed and their body is what is inside dist block. These
	 * auxiliary methods/functions have names that are normally illegal in the
	 * source code (they start with three underscores: ___)
	 * 
	 * @param bd
	 *            BlockDescriptor holding the description to the dist block and
	 *            all the information needed for the auxiliary method.
	 * @return name of the auxiliary function/method
	 * @throws Exception
	 */
	private String compileFunction(final BlockDescription bd) throws Exception {
		final boolean restricted = false;
		String name = bd.mn;

		if (name.equals("init")) {
			compilingInit = true;
		} else
			compilingInit = false;

		// Generate method description based on whether it is function or class.
		String methArgsSign;
		if (compilingClass)
			methArgsSign = Strings.PLANGOBJECT_N + " inst, "
					+ Strings.PLANGOBJECT_N + " run_id, "
					+ Strings.PLANGOBJECT_N + " passed_arg";
		else
			methArgsSign = Strings.PLANGOBJECT_N + " run_id, "
					+ Strings.PLANGOBJECT_N + " passed_arg";
		methArgsSign = StringUtils.removeEnd(methArgsSign, ", ");
		String methodSignature = "public " + Strings.PLANGOBJECT_N + " " + name
				+ "(" + methArgsSign + "){ return null; }";

		// Actually compile method/function
		final CtMethod m = CtNewMethod.make(methodSignature, cls);
		new MethodCompiler(m) {

			@Override
			protected void compileDataSources() throws Exception {
				varStack.pushNewStack();
				if (compilingClass) {
					varStack.addVariable("inst", VariableType.LOCAL_VARIABLE,
							stacker.acquire());
				}
				varStack.addVariable("run_id", VariableType.LOCAL_VARIABLE,
						stacker.acquire());
				varStack.addVariable("passed_arg", VariableType.LOCAL_VARIABLE,
						stacker.acquire());
				compileFunction(bd.b, restricted);
				varStack.popStack();
			}

		}.compileMethod();

		return name;
	}

	/**
	 * Compiles auxiliary method/function. These are created when dist
	 * expression is accessed and their body is what is inside dist block. These
	 * auxiliary methods/functions have names that are normally illegal in the
	 * source code (they start with three underscores: ___)
	 * 
	 * @param bd
	 *            BlockDescriptor holding the description to the dist block and
	 *            all the information needed for the auxiliary method.
	 * @return name of the auxiliary function/method
	 * @throws Exception
	 */
	protected void compileFunction(FunctionBodyContext functionBody,
			boolean restricted) throws Exception {
		compileFunction(functionBody.block(), restricted);
	}

	/**
	 * Compiles the body of method or function into java bytecode.
	 * 
	 * @param b
	 *            BlockContext containing AST of the function/method
	 * @param restricted
	 *            whether the function is marker as restricted or not
	 * @throws Exception
	 */
	protected void compileFunction(BlockContext b, boolean restricted)
			throws Exception {
		markLine(bc.currentPc(), b.start.getLine());

		if (restricted) {
			addGetRuntime();
			bc.addAload(0);
			bc.addCheckcast(Strings.BASE_COMPILED_STUB);
			bc.addInvokevirtual(Strings.RUNTIME,
					Strings.RUNTIME__CHECK_RESTRICTED_ACCESS, "("
							+ Strings.BASE_COMPILED_STUB_L + ")V");
		}

		// Initializes the finaly block protocol handlers for loops and for
		// normal exits
		fbcList = new LinkedList<FinallyBlockProtocol>();
		fbcLoopList = new LinkedList<FinallyBlockProtocol>();

		// compiles the block
		compileBlock(b);

		markLine(bc.currentPc(), b.stop.getLine());
		// Adds the exit protocol to function
		functionExitProtocol();
		// Empty nil value and return. This will be optimized out in dead code
		// pruning phrase, if necessary
		addNil();
		bc.add(Opcode.ARETURN);
	}

	/**
	 * Adds all finally blocks within current current block to break exit
	 * 
	 * @throws Exception
	 */
	private void breakContinueExitProtocol() throws Exception {
		List<FinallyBlockProtocol> copy = new ArrayList<FinallyBlockProtocol>(
				fbcLoopList);
		Collections.reverse(copy);
		for (FinallyBlockProtocol fbc : copy) {
			if (fbc == null)
				break;
			fbc.doCompile();
		}
		return;

	}

	/**
	 * Adds all finally blocks to this nonlocal function exit (ie return).
	 * 
	 * @throws Exception
	 */
	private void functionExitProtocol() throws Exception {
		if (!compilingInit) {
			fbcLoopList.add(null);
			List<FinallyBlockProtocol> copy = new ArrayList<FinallyBlockProtocol>(
					fbcList);
			Collections.reverse(copy);
			for (FinallyBlockProtocol fbc : copy)
				fbc.doCompile();
			fbcLoopList.remove(fbcLoopList.size() - 1);
			return;
		}

		bc.addAload(0); // load this
		bc.addIconst(0); // add false
		bc.addPutfield(Strings.BASE_COMPILED_STUB,
				Strings.BASE_COMPILED_STUB__RESTRICTED_OVERRIDE, "Z");
	}

	/**
	 * Compiles block of code into bytecode
	 * 
	 * @param block
	 *            BlockContext AST
	 * @throws Exception
	 */
	private void compileBlock(BlockContext block) throws Exception {
		// Insert new var stack, local variables are only valid within same
		// block
		varStack.pushNewStack();
		// Number of local variables in the block
		int pushCount = 0;
		for (BlockStatementContext bscx : block.blockStatement()) {
			if (bscx.localVariableDeclarationStatement() != null) {
				// Compile variable declarations
				List<VariableDeclaratorContext> decls = bscx
						.localVariableDeclarationStatement()
						.localVariableDeclaration().variableDeclarators()
						.variableDeclarator();
				for (VariableDeclaratorContext vd : decls) {
					// Compile variable declaration with optional expression as
					// initializer
					markLine(bc.currentPc(), vd.start.getLine());
					String varId = vd.variableDeclaratorId().getText();
					int localId = stacker.acquire();
					// Inc the number of local variables in the block
					++pushCount;

					if (vd.variableInitializer() != null) {
						// Variables are initialized to the expression they have
						isStatementExpression.add(false);
						compileExpression(
								vd.variableInitializer().expression(), false,
								-1);
						isStatementExpression.pop();
					} else {
						// Uninitialized variables are set to NoValue.
						addNil();
					}
					bc.addAstore(localId);

					// Add local variable type on var stack, ready to be used by
					// the rest of the code
					varStack.addVariable(varId, VariableType.LOCAL_VARIABLE,
							localId);
				}
			}
			if (bscx.statement() != null)
				// Compile statement of the block
				compileStatement(bscx.statement());
		}
		// All locals inside the block are now free, so we release them ready
		// for another use
		while (pushCount-- != 0)
			stacker.release();
		// Makes all local declaration now invalid in var stack
		varStack.popStack();
	}

	/**
	 * Long method compiling statements into bytecode
	 * 
	 * @param statement
	 *            StatementContext containing the AST
	 * @throws Exception
	 */
	private void compileStatement(final StatementContext statement)
			throws Exception {
		markLine(bc.currentPc(), statement.start.getLine());

		// Compile continue statement
		if (statement.continueStatement() != null) {
			if (continueStack.empty())
				throw new CompilationException("Continue not inside any loop");

			breakContinueExitProtocol();
			int label = continueStack.peek();
			addLabel(new JumpLabelInfo(), label);

			return;
		}

		// Compile break statement
		if (statement.breakStatement() != null) {
			if (breakStack.empty())
				throw new CompilationException("Break not inside any loop");

			breakContinueExitProtocol();
			int label = breakStack.peek();
			addLabel(new JumpLabelInfo(), label);

			return;
		}

		// Compile for loop
		if (statement.forStatement() != null) {
			ForControlContext fcc = statement.forStatement().forControl();

			// for loop needs three jump points, to loop start, to loop end and
			// to continue loop (which is test expression loop)
			int loopStart = labelCounter++;
			int loopEnd = labelCounter++;
			int continueLoop = labelCounter++;

			// for is considered new block
			varStack.pushNewStack();
			int pushCount = 0;

			// List all declarations in this block
			List<VariableDeclaratorContext> decls;

			// If for loop init contains declaration, add it, otherwise list is
			// empty
			if (fcc.forInit() != null
					&& fcc.forInit().localVariableDeclaration() != null) {
				decls = fcc.forInit().localVariableDeclaration()
						.variableDeclarators().variableDeclarator();
			} else
				decls = new ArrayList<VariableDeclaratorContext>();

			// Compile variable declarators
			for (VariableDeclaratorContext vd : decls) {
				markLine(bc.currentPc(), vd.start.getLine());

				// Compile variable declaration with optional expression as
				// initializer
				String varId = vd.variableDeclaratorId().getText();
				int localId = stacker.acquire();
				// Inc the number of local variables in the block
				++pushCount;

				if (vd.variableInitializer() != null) {
					// Variables are initialized to the expression they have
					isStatementExpression.add(false);
					compileExpression(vd.variableInitializer().expression(),
							false, -1);
					isStatementExpression.pop();
				} else {
					// Uninitialized variables are set to NoValue.
					addNil();
				}
				bc.addAstore(localId);

				// Add local variable type on var stack, ready to be used by the
				// rest of the code
				varStack.addVariable(varId, VariableType.LOCAL_VARIABLE,
						localId);
			}

			// Otherwise the init will contain expression, so it will be
			// compiled here
			if (fcc.forInit().expressionList() != null) {
				ExpressionListContext el = fcc.forInit().expressionList();
				for (ExpressionContext ex : el.expression()) {
					isStatementExpression.add(true);
					compileExpression(ex, false, -1);
					isStatementExpression.pop();
				}
			}

			// Actual loop starts here
			setLabelPos(loopStart);

			// Compile the test expression
			if (fcc.expression() != null) {
				isStatementExpression.add(false);
				compileExpression(fcc.expression(), false, -1);
				isStatementExpression.pop();
				bc.addInvokestatic(Strings.TYPEOPS,
						Strings.TYPEOPS__CONVERT_TO_BOOLEAN, "("
								+ Strings.PLANGOBJECT_L + ")Z"); // boolean on
																	// stack
				addLabel(new IfEqJumpLabelInfo(), loopEnd);
			}

			// Insert jump identifiers onto stack so they are reachable by inner
			// compiled block
			breakStack.add(loopEnd);
			continueStack.add(continueLoop);
			// Mark the loop by null value (for loops manage the breaks by null
			// value)
			// This is a little bit of logic here, the stack is used for the
			// whole function, but uses same stack,
			// thus in the stack we need to show that loop has happened by null
			// value. So when break/continue is
			// compiled, the exit protocol will know how many exit protocols
			// should it compile
			fbcLoopList.add(null);
			// Compile inner statement/block of the loop
			compileStatement(statement.forStatement().statement());
			// pop all stacks
			fbcLoopList.remove(fbcLoopList.size() - 1);
			breakStack.pop();
			continueStack.pop();

			// Mark end of the loop where continue should jump to
			setLabelPos(continueLoop);

			// For update is compiled here, if there is any
			// Every expression is compiled
			if (fcc.forUpdate() != null) {
				for (ExpressionContext ex : fcc.forUpdate().expressionList()
						.expression()) {
					isStatementExpression.add(true);
					compileExpression(ex, false, -1);
					isStatementExpression.pop();
				}
			}

			// and finally jump into start of the loop to the test
			addLabel(new JumpLabelInfo(), loopStart);

			// end the block, thus pop all used locals and variables declared
			while (pushCount-- != 0)
				stacker.release();
			varStack.popStack();

			// mark end of the loop and add sentinel nop
			setLabelPos(loopEnd);
			bc.add(Opcode.NOP);

			return;
		}

		// Compile while loop
		if (statement.whileStatement() != null) {

			// while loop uses two labels, start and the end of the loop
			int loopStart = labelCounter++;
			int loopEnd = labelCounter++;

			// mark start of the loop
			setLabelPos(loopStart);

			// compile the test of the while statement
			isStatementExpression.add(false);
			compileExpression(statement.whileStatement().parExpression()
					.expression(), false, -1);
			isStatementExpression.pop();
			bc.addInvokestatic(Strings.TYPEOPS,
					Strings.TYPEOPS__CONVERT_TO_BOOLEAN, "("
							+ Strings.PLANGOBJECT_L + ")Z"); // boolean on stack
			addLabel(new IfEqJumpLabelInfo(), loopEnd);

			// fill the loop stacks, see for loop for more info
			continueStack.add(loopStart);
			breakStack.add(loopEnd);
			fbcLoopList.add(null);
			// compile inner statement of the while body
			compileStatement(statement.whileStatement().statement());
			// cleanup stack
			fbcLoopList.remove(fbcLoopList.size() - 1);
			continueStack.pop();
			breakStack.pop();

			// jump back to the start of the loop
			addLabel(new JumpLabelInfo(), loopStart);

			// mark end of the loop and add sentinel nop
			setLabelPos(loopEnd);
			bc.add(Opcode.NOP);
			return;
		}

		// Compile do statement
		if (statement.doStatement() != null) {

			// Do statement uses three labels, start of the loop, end of the
			// loop and beginning of the test of the loop
			int loopStart = labelCounter++;
			int loopTest = labelCounter++;
			int loopEnd = labelCounter++;

			// mark start of the loop
			setLabelPos(loopStart);

			// fill the loop stacks, see for loop for more info
			continueStack.add(loopTest);
			breakStack.add(loopEnd);
			fbcLoopList.add(null);
			// compile statement of the do body
			compileStatement(statement.doStatement().statement());
			// cleanup stack
			fbcLoopList.remove(fbcLoopList.size() - 1);
			continueStack.pop();
			breakStack.pop();

			// Mark test label, then compile test for the do loop
			setLabelPos(loopTest);
			isStatementExpression.add(false);
			compileExpression(statement.doStatement().parExpression()
					.expression(), false, -1);
			isStatementExpression.pop();
			bc.addInvokestatic(Strings.TYPEOPS,
					Strings.TYPEOPS__CONVERT_TO_BOOLEAN, "("
							+ Strings.PLANGOBJECT_L + ")Z"); // boolean on stack
			addLabel(new IfNeJumpLabelInfo(), loopStart);

			// mark end of the loop and sentinel nop
			setLabelPos(loopEnd);
			bc.add(Opcode.NOP);
			return;
		}

		// Compile try statement
		if (statement.tryStatement() != null) {
			int endLabel = labelCounter++;

			// need to check whether this try statement has finally block or not
			boolean hasFinally = statement.tryStatement().finallyBlock() != null;

			// we need two locals, throwable to save throwable and finally for
			// storing the result of return then call finally code and
			// then return the finally value
			final int finallyStack = stacker.acquire();
			final int throwableStack = stacker.acquire();

			// Compile finally block code data when finally is compiled
			// (nonlocal exit)
			FinallyBlockProtocol fbc = new FinallyBlockProtocol() {

				@Override
				public void doCompile() throws Exception {
					compileBlock(statement.tryStatement().finallyBlock()
							.block());
				}
			};

			// mark beginning of the code handled by try
			int start = bc.currentPc();
			if (hasFinally) {
				// if it has finally, we need to add finally block protocol into
				// stack
				fbcList.add(fbc);
				fbcLoopList.add(fbcList.get(fbcList.size() - 1));
			}
			// compile try block of code
			compileBlock(statement.tryStatement().block());
			if (hasFinally) {
				// and remove the finally block protocol from stack
				fbcList.remove(fbcList.size() - 1);
				fbcLoopList.remove(fbcLoopList.size() - 1);
			}
			// mark the end of the code handled by try
			int end = bc.currentPc();
			if (hasFinally) {
				// local exit, so compile finally normally
				compileBlock(statement.tryStatement().finallyBlock().block());
			}
			// jump to the end of all try compiled code (skip the exception
			// handling routines)
			addLabel(new JumpLabelInfo(), endLabel);

			int prevKey = -1;

			if (statement.tryStatement().catchClause() != null) {
				// if there is something to catch, we need the catch routines
				// compiled here

				// we need a label to jump to when exception happens
				int throwLabel = labelCounter++;
				// mark the try code with exception of BaseCompiledStub type, ie
				// any class in Plang
				bc.addExceptionHandler(start, end, bc.currentPc(),
						Strings.BASE_COMPILED_STUB);
				// save old exception into locals
				bc.addAstore(throwableStack);

				// compile each catch statement as one big catch statement that
				// uses normal logic (ie call method on the "exception" to
				// determine whether
				// that exception is handled or not)
				Iterator<CatchClauseContext> ccit = statement.tryStatement()
						.catchClause().iterator();
				while (ccit.hasNext()) {
					CatchClauseContext ccc = ccit.next();
					// we need type and fully qualified name of the type
					String type = ccc.type().getText();
					String fqName = type;

					markLine(bc.currentPc(), ccc.start.getLine());

					// get fully qualified name or guess it based on the current
					// runtime state
					if (referenceMap.containsKey(type)) {
						Reference r = referenceMap.get(type);
						if (r.isJava())
							throw new CompilationException(
									"Only PLang class/module type can be in catch expression!");
						fqName = r.getFullReference();
					}

					// we get class name from our reference/type
					String className = PLRuntime.getRuntime()
							.getClassNameOrGuess(fqName);

					// we need to do this in a loop so we know where to jump
					if (prevKey != -1)
						setLabelPos(prevKey);

					// check exception for type
					addGetRuntime();
					bc.addAload(throwableStack);
					bc.addLdc(cacheStrings(className));
					bc.addInvokevirtual(Strings.RUNTIME,
							Strings.RUNTIME__CHECK_EXCEPTION_HIERARCHY, "("
									+ Strings.PLANGOBJECT_L + Strings.STRING_L
									+ ")Z");

					if (!ccit.hasNext()) {
						// no other catch clauses, else should go to exit
						prevKey = throwLabel;
					} else {
						// else should go to next key pos
						prevKey = labelCounter++;
					}
					addLabel(new IfEqJumpLabelInfo(), prevKey);

					// we grab exception into local variable defined in the
					// catch type
					varStack.addVariable(ccc.Identifier().getText(),
							VariableType.LOCAL_VARIABLE, throwableStack);

					// we need to grab another try catch with any throwable so
					// we can execute finally code
					// so grab the current bc position
					int sstart = bc.currentPc();
					if (hasFinally) {
						// fill stack for finally blocks
						fbcList.add(fbc);
						fbcLoopList.add(fbcList.get(fbcList.size() - 1));
					}
					// compile the code of the exception handle
					compileBlock(ccc.block());
					if (hasFinally) {
						// pop stacks
						fbcList.remove(fbcList.size() - 1);
						fbcLoopList.remove(fbcLoopList.size() - 1);
					}
					int ssend = bc.currentPc();
					// add finally block for local exit
					if (hasFinally)
						compileBlock(statement.tryStatement().finallyBlock()
								.block());
					// jump to the end of the try statement after we are done
					addLabel(new JumpLabelInfo(), endLabel);

					if (hasFinally) {
						// if we have finally block, we need to catch throwable,
						// then compile finally, then rethrow that exception
						bc.addExceptionHandler(sstart, ssend, bc.currentPc(),
								Strings.THROWABLE);
						bc.addAstore(finallyStack);
						compileBlock(statement.tryStatement().finallyBlock()
								.block());
						bc.addAload(finallyStack);
						bc.add(Opcode.ATHROW);
					}

				}

				// if nothing was caught, we execute finally blocks, if any, and
				// rethrow
				setLabelPos(throwLabel);
				if (hasFinally)
					compileBlock(statement.tryStatement().finallyBlock()
							.block());
				bc.addAload(throwableStack);
				bc.add(Opcode.ATHROW);
				addLabel(new JumpLabelInfo(), endLabel);
			}

			if (statement.tryStatement().finallyBlock() != null) {
				// if we have finally block, we need to add throwable catch for
				// the block and then compile finally into it and rethrow
				bc.addExceptionHandler(start, end, bc.currentPc(),
						Strings.THROWABLE);
				// save throwable exception on stack
				bc.addAstore(finallyStack);

				compileBlock(statement.tryStatement().finallyBlock().block());

				bc.addAload(finallyStack);
				bc.add(Opcode.ATHROW);
			}

			// mark the end of the try and add sentinel nop
			setLabelPos(endLabel);
			bc.add(Opcode.NOP);

			// we need to release our local variables we used
			stacker.release();
			stacker.release();
			return;
		}

		// Compile block statement
		if (statement.block() != null) {
			compileBlock(statement.block());
			return;
		}

		// Compile return statement
		if (statement.returnStatement() != null) {
			// grab local for saving return expression value and then execute
			// finally blocks
			int local = stacker.acquire();
			// compile expression for return or NoValue if there is no
			// expression provided
			if (statement.returnStatement().expression() != null) {
				isStatementExpression.add(false);
				compileExpression(statement.returnStatement().expression(),
						false, -1);
				isStatementExpression.pop();
			} else {
				addNil();
			}

			// compile exit protocol, ie store the value of expression, add exit
			// protocol, then push value back on stack
			bc.addAstore(local);
			functionExitProtocol();
			bc.addAload(local);

			// return from the function and release auto value
			bc.add(Opcode.ARETURN);
			stacker.release();
			return;
		}

		// Compile throw statement
		if (statement.throwStatement() != null) {
			isStatementExpression.add(false);
			compileExpression(statement.throwStatement().expression(), false,
					-1);
			isStatementExpression.pop();
			bc.add(Opcode.DUP);
			bc.addInvokevirtual(Strings.BASE_COMPILED_STUB, Strings.BASE_COMPILED_STUB__REBUILD_STACK, "()"+Strings.BASE_COMPILED_STUB_L);
			bc.add(Opcode.ATHROW);
			return;
		}

		// Compile statement expression, ie expression as statement
		if (statement.statementExpression() != null) {
			isStatementExpression.add(true);
			compileExpression(statement.statementExpression().expression(),
					false, -1);
			isStatementExpression.pop();
		}

		// Compile if statement
		if (statement.ifStatement() != null) {
			// Compile test
			ExpressionContext e = statement.ifStatement().parExpression()
					.expression();
			isStatementExpression.add(false);
			compileExpression(e, false, -1);
			isStatementExpression.pop();
			bc.addInvokestatic(Strings.TYPEOPS,
					Strings.TYPEOPS__CONVERT_TO_BOOLEAN, "("
							+ Strings.PLANGOBJECT_L + ")Z"); // boolean on stack

			// check if we have else
			boolean hasElse = statement.ifStatement().getChildCount() == 5;
			// grab a label counter used for else jump
			int key = labelCounter++;
			addLabel(new IfEqJumpLabelInfo(), key);
			// reserve key2 for if/else variant
			int key2 = -1;

			// the body of if
			compileStatement((StatementContext) statement.ifStatement()
					.getChild(2));
			if (hasElse) {
				// grab a label for else jump
				key2 = labelCounter++;
				// change jump to the end of else block
				addLabel(new JumpLabelInfo(), key2);
			}

			// jump to when if fails
			setLabelPos(key);

			if (hasElse) {
				// compile else part of the if
				compileStatement((StatementContext) statement.ifStatement()
						.getChild(4));
				// mark the end of else block
				setLabelPos(key2);
			}

			// add sentinel nop
			bc.add(Opcode.NOP);
		}
	}

	/**
	 * Compiles the ___get_runtime() call to return PLRuntime
	 */
	private void addGetRuntime() {
		bc.addAload(0); // load this
		bc.addInvokevirtual(Strings.BASE_COMPILED_STUB,
				Strings.BASE_COMPILED_STUB__GET_RUNTIME, "()"
						+ Strings.RUNTIME_L); // call to ___get_runtime,
												// PLRuntime is on stack
	}

	/**
	 * Compile init method of class/module. Init method performs class/module
	 * init (ie class declared variables and values are stored here)
	 * 
	 * @param fields
	 *            list of fields
	 * @param methods
	 *            list of methods (method wrappers need to be created here)
	 * @param superClass
	 *            name of the super class
	 * @throws Exception
	 */
	private void compileInitMethod(List<FieldDeclarationContext> fields,
			Set<String> methods, final String superClass) throws Exception {
		// grab "self" local variable, ie 1
		stacker.acquire();
		if (compilingClass)
			// if we are compiling class, we need to compile base class variable
			// held in super key
			new StoreToField(PLClass.___superKey) {

				@Override
				protected void provideSourceValue() throws Exception {
					if (superClass == null || superClass.equals("BaseClass")) {
						// if superclass is null or is base class, we need to
						// create new base class
						bc.addNew(Strings.BASE_CLASS);
						bc.add(Opcode.DUP);
						bc.add(Opcode.DUP);
						bc.addInvokespecial(Strings.BASE_CLASS, "<init>", "()V");
						bc.addCheckcast(Strings.PL_CLASS);
						bc.addAload(0);
						bc.addInvokevirtual(Strings.PL_CLASS,
								Strings.PL_CLASS__SET_DERIVED_CLASS, "("
										+ Strings.PL_CLASS_L + ")V");
					} else {
						// otherwise we need to find reference to the class then
						// make new instance
						if (referenceMap.containsKey(superClass)) {
							Reference r = referenceMap.get(superClass);
							if (r.isJava()) {
								throw new CompilationException(
										"Reference is reference to java class, not PLang class!");
							}
							addGetRuntime();
							bc.addLdc(cacheStrings(r.getFullReference()));
							bc.addIconst(1);
							bc.addAnewarray(
									cp.getCtClass(Strings.PLANGOBJECT_N), 0);
							bc.addInvokevirtual(Strings.RUNTIME,
									Strings.RUNTIME__NEW_INSTANCE, "("
											+ Strings.STRING_L + "Z["
											+ Strings.PLANGOBJECT_L + ")"
											+ Strings.PL_CLASS_L);
							bc.add(Opcode.DUP);
							bc.addCheckcast(Strings.PL_CLASS);
							bc.addAload(0);
							bc.addInvokevirtual(Strings.PL_CLASS,
									Strings.PL_CLASS__SET_DERIVED_CLASS, "("
											+ Strings.PL_CLASS_L + ")V");
						} else if (superClass.contains(".")) {
							// full reference to the class, so we need to search
							// it at runtime
							addGetRuntime();
							bc.addLdc(cacheStrings(superClass));
							bc.addIconst(1);
							bc.addAnewarray(
									cp.getCtClass(Strings.PLANGOBJECT_N), 0);
							bc.addInvokevirtual(Strings.RUNTIME,
									Strings.RUNTIME__NEW_INSTANCE, "("
											+ Strings.STRING_L + "Z["
											+ Strings.PLANGOBJECT_L + ")"
											+ Strings.PL_CLASS_L);
							bc.add(Opcode.DUP);
							bc.addCheckcast(Strings.PL_CLASS);
							bc.addAload(0);
							bc.addInvokevirtual(Strings.PL_CLASS,
									Strings.PL_CLASS__SET_DERIVED_CLASS, "("
											+ Strings.PL_CLASS_L + ")V");
						}
					}
				}

			}.compile();

		// compile fields and their initializers
		for (FieldDeclarationContext field : fields) {
			compileField(field);
		}

		// we mark that we are compiling function wrappers
		cmpInitFuncwraps = true;
		for (final String method : methods) {
			new StoreToField(method) {

				@Override
				protected void provideSourceValue() throws Exception {
					bc.addNew(Strings.FUNCTION_WRAPPER); // new function wrapper
					bc.add(Opcode.DUP);
					bc.addLdc(cacheStrings(method)); // Load string name of
														// method
					bc.addAload(0); // Load this
					bc.addIconst(compilingClass ? 1 : 0);
					bc.addInvokespecial(Strings.FUNCTION_WRAPPER, "<init>", "("
							+ Strings.STRING_L + Strings.BASE_COMPILED_STUB_L
							+ "Z)V");
				}

			}.compile();
		}
		cmpInitFuncwraps = false;
		// release "self" from locals
		stacker.release();
		bc.add(Opcode.RETURN);
	}

	/**
	 * Compile field inside initializer
	 * 
	 * @param field
	 *            FieldDeclarationContext AST
	 * @throws Exception
	 */
	private void compileField(FieldDeclarationContext field) throws Exception {
		VariableDeclaratorsContext vdc = field.variableDeclarators();
		for (final VariableDeclaratorContext vd : vdc.variableDeclarator()) {
			markLine(bc.currentPc(), vd.start.getLine());
			String varId = vd.variableDeclaratorId().getText();

			new StoreToField(varId) {

				@Override
				protected void provideSourceValue() throws Exception {
					if (vd.variableInitializer() != null) {
						isStatementExpression.add(false);
						compileExpression(
								vd.variableInitializer().expression(), false,
								-1);
						isStatementExpression.pop();
					} else {
						addNil();
					}
				}

			}.compile();
		}
	}

	/**
	 * Adds NoValue onto stack
	 */
	protected void addNil() {
		bc.addGetstatic(Strings.NONETYPE, "NOVALUE", Strings.NONETYPE_L); // load
																			// NOVALUE
	}

	/**
	 * Compiles expression into bytecode
	 * 
	 * @param expression
	 *            ExpressionContext AST of the expression
	 * @param compilingMethodCall
	 *            whether we are compiling a method call or not
	 * @param storeVar
	 *            what local should we store the expression if it is method call
	 *            (-1 if it is not)
	 * @throws Exception
	 */
	private void compileExpression(ExpressionContext expression,
			boolean compilingMethodCall, int storeVar) throws Exception {
		markLine(bc.currentPc(), expression.start.getLine());
		try {
			if (expression.primary() != null) {
				// expression is primary, so we just compile content of primary
				// expression
				compilePrimaryExpression(expression.primary(),
						compilingMethodCall, storeVar);
				return;
			}

			if (expression.block() != null) {
				// if expression has block, it is dist expression

				// create new name for auxiliary method/function
				String methodName = "___internalMethod" + distributed.size();
				BlockContext block = expression.block();

				// we store block and method name into block descriptor
				BlockDescription bd = new BlockDescription();
				bd.mn = methodName;
				bd.b = block;

				distributed.add(bd);

				// compile expression and name of the method, then call run
				// distributed
				addGetRuntime();
				isStatementExpression.add(false);
				compileExpression(expression.expression(0), false, -1);
				isStatementExpression.pop();
				bc.addLdc(cacheStrings(methodName));
				if (expression.expression().size() > 1) {
					isStatementExpression.add(false);
					compileExpression(expression.expression(1), false, -1);
					isStatementExpression.pop();
				} else {
					addNil();
				}
				bc.addAload(0);
				bc.addInvokevirtual(Strings.RUNTIME,
						Strings.RUNTIME__RUN_DISTRIBUTED, "("
								+ Strings.PLANGOBJECT_L + Strings.STRING_L
								+ Strings.PLANGOBJECT_L
								+ Strings.BASE_COMPILED_STUB_L + ")"
								+ Strings.PLANGOBJECT_L);
				return;
			}

			if (expression.getChildCount() > 2
					&& expression.getChild(1).getText().equals("?")) {
				// Ternary operator expression
				compileTernaryOperator(
						(ExpressionContext) expression.getChild(0),
						(ExpressionContext) expression.getChild(2),
						(ExpressionContext) expression.getChild(4),
						compilingMethodCall, storeVar);
				return;
			}

			if (expression.getChildCount() > 2
					&& expression.getChild(1).getText().equals("->")) {
				// compile java call (->) expression
				if (!PLRuntime.getRuntime().isSafeContext())
					// is not safe context, so do not allow these expressions in
					// the code
					throw new CompilationException(
							"Java method call being compiled under unsafe context.");

				// java call
				ParseTree init = expression.getChild(0);
				String mname = expression.getChild(2).getText();
				String refName = init.getText();
				if (!referenceMap.containsKey(refName)) {
					// instance method, grab Pointer value from it, then call it

					addGetRuntime();
					isStatementExpression.add(false);
					compileExpression((ExpressionContext) init, false, -1);
					isStatementExpression.pop();
					bc.addCheckcast(Strings.POINTER);
					bc.addLdc(cacheStrings(mname));
					compileParameters(expression.expressionList());
					bc.addInvokevirtual(Strings.RUNTIME,
							Strings.RUNTIME__RUN_JAVA_WRAPPER, "("
									+ Strings.POINTER_L + Strings.STRING_L
									+ "[" + Strings.PLANGOBJECT_L + ")"
									+ Strings.PLANGOBJECT_L);

				} else {
					// static method or constructor
					Reference r = referenceMap.get(refName);
					if (r == null)
						throw new CompilationException(
								"Unknown type reference: " + refName + " at "
										+ expression.start.getLine());
					String fqName = r.getFullReference();
					if (!r.isJava())
						throw new CompilationException("Type is not java type!");

					boolean isConstructorCall = refName.equals(mname);

					if (isConstructorCall) {
						addGetRuntime();
						bc.addLdc(cacheStrings(fqName));
						compileParameters(expression.expressionList());
						bc.addInvokevirtual(Strings.RUNTIME,
								Strings.RUNTIME__CREATE_JAVA_WRAPPER, "("
										+ Strings.STRING_L + "["
										+ Strings.PLANGOBJECT_L + ")"
										+ Strings.POINTER_L);
					} else {
						addGetRuntime();
						bc.addLdc(cacheStrings(fqName));
						bc.addLdc(cacheStrings(mname));
						compileParameters(expression.expressionList());
						bc.addInvokevirtual(Strings.RUNTIME,
								Strings.RUNTIME__RUN_JAVA_STATIC_METHOD, "("
										+ Strings.STRING_L + Strings.STRING_L
										+ "[" + Strings.PLANGOBJECT_L + ")"
										+ Strings.PLANGOBJECT_L);
					}
				}
				return;
			}

			if (expression.methodCall() != null) {
				// Method call expression

				// we grab local variable slot
				int stack = stacker.acquire();

				// we need to grab function wrapper/function object from
				// expression
				addGetRuntime();
				isStatementExpression.add(false);
				compileExpression((ExpressionContext) expression.getChild(0),
						true, stack);
				isStatementExpression.pop();
				
				// we load result of that expression from local passed into
				// compile expression
				// then we call run method of PLRuntime
				bc.addAload(stack);
				bc.addCheckcast(Strings.BASE_COMPILED_STUB);
				compileParameters(expression.methodCall().expressionList());
				bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__RUN, "("
						+ Strings.PLANGOBJECT_L + Strings.BASE_COMPILED_STUB_L
						+ "[" + Strings.PLANGOBJECT_L + ")"
						+ Strings.PLANGOBJECT_L);
				// release local variable slot
				stacker.release();
				return;
			}

			if (leftOperators.contains(expression.getChild(0).getText())) {
				// Compile left operator, ++ or -- or unary expression
				final String operator = expression.getChild(0).getText();

				if ("++".equals(operator) || "--".equals(operator)) {
					// compile ++ or -- operator
					new CompileSetOperator(expression.extended(), false,
							compilingMethodCall, storeVar) {

						@Override
						public void compileRight() throws Exception {
							bc.addInvokestatic(
									Strings.TYPEOPS,
									operator.equals("++") ? Strings.TYPEOPS__LEFTPLUSPLUS
											: Strings.TYPEOPS__LEFTMINUSMINUS,
									"(" + Strings.PLANGOBJECT_L + ")"
											+ Strings.PLANGOBJECT_L);
						}

					}.compileSetOperator();
				} else {
					// compile unary operator
					compileUnaryOperator(operator,
							(ExpressionContext) expression.getChild(1),
							compilingMethodCall, storeVar);
				}
				return;
			}

			if (expression.getChild(0) instanceof ExpressionContext) {
				// Compile expression context on the left expression, ie binary
				// operator or instanceof operator
				// or logical operator or right operators

				// grab the operator
				String operator = expression.getChild(1).getText();
				if (bioperators.contains(operator)) {
					// compile binary operator
					compileBinaryOperator(operator,
							(ExpressionContext) expression.getChild(0),
							(ExpressionContext) expression.getChild(2),
							compilingMethodCall, storeVar);
				} else if ("instanceof".equals(operator)) {
					// compile instanceof iterator
					String fqName = "";
					String type = expression.type().getText();
					if (referenceMap.containsKey(type)) {
						Reference r = referenceMap.get(type);
						if (r.isJava())
							throw new CompilationException(
									"Only PLang type can be in catch expression!");
						fqName = r.getFullReference();
					} else {
						fqName = type;
					}

					// instanceof needs fq type or base type defined by
					// PLRuntime
					if (!fqName.contains(".") && !isBaseType(fqName)) {
						throw new CompilationException(
								"Class type "
										+ type
										+ " is unknown. Have you forgotten using declaration?");
					}

					// get the class name or guess it
					String className = PLRuntime.getRuntime()
							.getClassNameOrGuess(fqName);

					// compile right side of the instanceof
					addGetRuntime();
					isStatementExpression.add(false);
					compileExpression(
							(ExpressionContext) expression.getChild(0), false,
							-1);
					isStatementExpression.pop();

					// call the checkInstanceOf to determine the value
					bc.addLdc(cacheStrings(className));
					bc.addInvokevirtual(Strings.RUNTIME,
							Strings.RUNTIME__CHECK_INSTANCEOF, "("
									+ Strings.PLANGOBJECT_L + Strings.STRING_L
									+ ")" + Strings.PLANGOBJECT_L);

				} else if ("||".equals(operator) || "&&".equals(operator)) {
					// compile logic operator
					compileLogic((ExpressionContext) expression.getChild(0),
							(ExpressionContext) expression.getChild(2),
							"||".equals(operator), compilingMethodCall,
							storeVar);
				} else {
					// we compile left side of the binary operation
					isStatementExpression.add(false);
					compileExpression(
							(ExpressionContext) expression.getChild(0), false,
							-1);
					isStatementExpression.pop();
					if (expression.getChild(1).getText().equals(".")) {
						// compiling field accessor
						if (compilingMethodCall) {
							bc.add(Opcode.DUP);
							bc.addAstore(storeVar);
						}

						markLine(bc.currentPc(), expression.stop.getLine());

						// we need to do this logic because of how we detect
						// whether field exists in class or superclass
						// if it is null, then we look for global variable
						String identifier = expression.getChild(2).getText();
						bc.addCheckcast(Strings.BASE_COMPILED_STUB);
						bc.addLdc(cacheStrings(identifier)); // load string from
																// constants
						// check whether previous call was from parent
						bc.addIconst(determineParentCall((ExpressionContext) expression.getChild(0)) ? 1 : 0);
						bc.addInvokevirtual(Strings.BASE_COMPILED_STUB,
								Strings.BASE_COMPILED_STUB__GETKEY, "("
										+ Strings.STRING_L + "Z)"
										+ Strings.PLANGOBJECT_L);

						int kkey = labelCounter++;
						bc.add(Opcode.DUP);
						addLabel(new IfNotNullJumpLabelInfo(), kkey);

						bc.add(Opcode.POP);
						addThrow("Unknown field: " + identifier);

						setLabelPos(kkey);
						bc.add(Opcode.NOP);
					}
				}
				return;
			}

			if (expression.getChild(0) instanceof ExtendedContext
					&& rightOperators
							.contains(expression.getChild(1).getText())) {
				// compile right operator

				// we need extended content (ie identifier or expression that
				// ends with identifier)
				ExtendedContext lvalue = expression.extended();
				final int st = stacker.acquire();
				final boolean add = expression.getChild(1).getText()
						.equals("++");

				isStatementExpression.add(false);
				new CompileSetOperator(lvalue, false, false, -1) {

					@Override
					public void compileRight() throws Exception {
						bc.add(Opcode.DUP);
						bc.addAstore(st);

						bc.addNew(Strings.INT);
						bc.add(Opcode.DUP);
						bc.addIconst(1);
						bc.addInvokespecial(Strings.INT, "<init>", "(I)V");

						compileBinaryOperator(add ? "+" : "-", null, null,
								false, -1);
					}

				}.compileSetOperator();
				isStatementExpression.pop();

				// store into method variable if we are in method call first
				// argument
				if (compilingMethodCall) {
					bc.addAload(st);
					bc.addAstore(storeVar);
				}

				if (!isStatementExpression.peek()) {
					bc.addOpcode(Opcode.POP);
					bc.addAload(st);
				}
				stacker.release();
				return;
			}

			if (expression.getChild(0).getText().equals("new")) {
				// new expression compilation

				// grab fully qualified name
				String fqName = null;
				if (expression.getChildCount() == 4) {
					// fully qualified name is included in new with dot
					fqName = expression.getChild(1).getText()
							+ "."
							+ expression.constructorCall().getChild(0)
									.getText();
				} else {
					// we need to get fully qualified name from references
					String refName = expression.constructorCall().getChild(0)
							.getText();
					Reference r = referenceMap.get(refName);
					if (r == null)
						throw new CompilationException(
								"Unknown type reference: " + refName + " at "
										+ expression.start.getLine());
					fqName = r.getFullReference();
				}

				// call to newInstance method
				addGetRuntime();
				bc.addLdc(cacheStrings(fqName)); // load string from constants
				compileParameters(expression.constructorCall().expressionList());
				bc.addInvokevirtual(Strings.RUNTIME,
						Strings.RUNTIME__NEW_INSTANCE, "(" + Strings.STRING_L
								+ "[" + Strings.PLANGOBJECT_L + ")"
								+ Strings.PL_CLASS_L);

				if (compilingMethodCall) {
					bc.add(Opcode.DUP);
					bc.addAstore(storeVar);
				}
			} else if (expression.getChildCount() == 3) {
				// set expression compilation
				String operator = expression.getChild(1).getText();

				if (setOperators.contains(operator)) {
					compileSetOperator(operator, expression,
							compilingMethodCall, storeVar);
				}
			}
		} finally {
			// if we are in statemenet expression, we need to pop it off stack
			if (isStatementExpression.peek()) {
				bc.add(Opcode.POP);
			}
		}
	}

	private boolean determineParentCall(ExpressionContext child) {
		if (!compilingClass)
			return false;
		if (child.primary() != null){
			if (child.primary().constExpr() != null)
				return child.primary().constExpr().getText().equals("super");
		}
		return false;
	}

	/**
	 * Returns whether fully qualified name is base type or not
	 * 
	 * @param fqName
	 * @return
	 */
	private boolean isBaseType(String fqName) {
		return PLRuntime.BASE_TYPES.contains(fqName);
	}

	/**
	 * Compiles ternary operator
	 * 
	 * @param e
	 *            Test expression
	 * @param et
	 *            Expression when test is true
	 * @param ef
	 *            Expression when test is false
	 * @param compilingMethod
	 *            whether we are compiling method call
	 * @param storeVar
	 *            local to store in if we are compiling method call (-1 if not)
	 * @throws Exception
	 */
	private void compileTernaryOperator(ExpressionContext e,
			ExpressionContext et, ExpressionContext ef,
			boolean compilingMethod, int storeVar) throws Exception {

		// compile test expression, then add jumps
		isStatementExpression.add(false);
		compileExpression(e, false, -1);
		isStatementExpression.pop();
		bc.addInvokestatic(Strings.TYPEOPS,
				Strings.TYPEOPS__CONVERT_TO_BOOLEAN, "("
						+ Strings.PLANGOBJECT_L + ")Z"); // boolean on stack
		int key = labelCounter++;
		addLabel(new IfEqJumpLabelInfo(), key);
		int key2 = -1;

		// compile true expression
		compileExpression(et, false, -1);

		key2 = labelCounter++;
		addLabel(new JumpLabelInfo(), key2);

		setLabelPos(key);

		// compile false expression
		compileExpression(ef, false, -1);
		setLabelPos(key2);

		bc.add(Opcode.NOP);
		if (compilingMethod) {
			bc.add(Opcode.DUP);
			bc.addAstore(storeVar);
		}
	}

	/**
	 * Compile logic expression, either or or and, compiling it correctly with
	 * shortcuting
	 * 
	 * @param left
	 *            left side of the expression
	 * @param right
	 *            right side of the expression
	 * @param or
	 *            whether it is or or and
	 * @param compilingMethod
	 *            whether we are compiling method call
	 * @param storeVar
	 *            local to store in if we are compiling method call (-1 if not)
	 * @throws Exception
	 */
	private void compileLogic(ExpressionContext left, ExpressionContext right,
			final boolean or, boolean compilingMethod, int storeVar)
			throws Exception {

		// load this
		bc.addAload(0);

		// compile left side of the expression
		isStatementExpression.add(false);
		compileExpression(left, false, -1);
		isStatementExpression.pop();

		bc.addInvokestatic(Strings.TYPEOPS,
				Strings.TYPEOPS__CONVERT_TO_BOOLEAN, "("
						+ Strings.PLANGOBJECT_L + ")Z"); // boolean on stack

		int shortCut = labelCounter++;
		int reminder = labelCounter++;
		// add jump info depending on whether it is or or and
		addLabel(new LabelInfo() {

			@Override
			protected void add(Bytecode bc) throws CompilationException {
				int offset = getValue(poskey) - bcpos;
				bc.write(bcpos, or ? Opcode.IFNE : Opcode.IFEQ);
				bc.write16bit(bcpos + 1, offset);
			}

		}, shortCut);

		// compile right side of the expression
		isStatementExpression.add(false);
		compileExpression(right, false, -1);
		isStatementExpression.pop();

		bc.addInvokestatic(Strings.TYPEOPS,
				Strings.TYPEOPS__CONVERT_TO_BOOLEAN, "("
						+ Strings.PLANGOBJECT_L + ")Z"); // boolean on stack

		addLabel(new JumpLabelInfo(), reminder);

		setLabelPos(shortCut);

		// add shortcuting expression based on the boolean value
		if (or) {
			bc.addIconst(1);
		} else {
			bc.addIconst(0);
		}
		setLabelPos(reminder);
		// convert boolean back to the PLangObject
		bc.addInvokevirtual(Strings.BASE_COMPILED_STUB,
				Strings.BASE_COMPILED_STUB__CONVERT_BOOLEAN, "(Z)"
						+ Strings.PLANGOBJECT_L);
		if (compilingMethod) {
			bc.add(Opcode.DUP);
			bc.addAstore(storeVar);
		}
	}

	/**
	 * Compiles unary operator.
	 * 
	 * @param operator
	 *            unary operator
	 * @param expression
	 *            expression of the unary operator
	 * @param compilingMethod
	 *            whether we are compiling method call
	 * @param storeVar
	 *            local to store in if we are compiling method call (-1 if not)
	 * @throws Exception
	 */
	private void compileUnaryOperator(String operator,
			ExpressionContext expression, boolean compilingMethod, int storeVar)
			throws Exception {
		// compile left side of the operator
		if (expression != null) {
			isStatementExpression.add(false);
			compileExpression(expression, false, -1);
			isStatementExpression.pop();
		}

		// add invokedynamic based on the operator
		String method = "";
		switch (operator) {
		case "+":
			method = Strings.TYPEOPS__UNARY_PLUS;
			break;
		case "-":
			method = Strings.TYPEOPS__UNARY_MINUS;
			break;
		case "!":
			method = Strings.TYPEOPS__UNARY_LNEG;
			break;
		case "~":
			method = Strings.TYPEOPS__UNARY_BNEG;
			break;
		}

		bc.addInvokedynamic(1, method, "(" + Strings.PLANGOBJECT_L + ")"
				+ Strings.PLANGOBJECT_L);

		if (compilingMethod) {
			bc.add(Opcode.DUP);
			bc.addAstore(storeVar);
		}
	}

	/**
	 * Compiles binary operation
	 * 
	 * @param operator
	 *            the operator
	 * @param expression1
	 *            left side of the operator
	 * @param expression2
	 *            right side of the operator
	 * @param compilingMethod
	 *            whether we are compiling method call
	 * @param storeVar
	 *            local to store in if we are compiling method call (-1 if not)
	 * @throws Exception
	 */
	private void compileBinaryOperator(String operator,
			ExpressionContext expression1, ExpressionContext expression2,
			boolean compilingMethod, int storeVar) throws Exception {

		// Compile expressions, first left then right
		if (expression1 != null && expression2 != null) {
			isStatementExpression.add(false);
			compileExpression(expression1, false, -1);
			compileExpression(expression2, false, -1);
			isStatementExpression.pop();
		}

		// add invokedynamic based on the operator
		String method = null;

		switch (operator) {
		case "+":
			method = Strings.TYPEOPS__PLUS;
			break;
		case "-":
			method = Strings.TYPEOPS__MINUS;
			break;
		case "*":
			method = Strings.TYPEOPS__MUL;
			break;
		case "/":
			method = Strings.TYPEOPS__DIV;
			break;
		case "%":
			method = Strings.TYPEOPS__MOD;
			break;
		case "<<":
			method = Strings.TYPEOPS__LSHIFT;
			break;
		case ">>":
			method = Strings.TYPEOPS__RSHIFT;
			break;
		case ">>>":
			method = Strings.TYPEOPS__RUSHIFT;
			break;
		case "&":
			method = Strings.TYPEOPS__BITAND;
			break;
		case "|":
			method = Strings.TYPEOPS__BITOR;
			break;
		case "==":
			method = Strings.TYPEOPS__EQ;
			break;
		case "!=":
			method = Strings.TYPEOPS__NEQ;
			break;
		case "<":
			method = Strings.TYPEOPS__LESS;
			break;
		case ">":
			method = Strings.TYPEOPS__MORE;
			break;
		case "<=":
			method = Strings.TYPEOPS__LEQ;
			break;
		case ">=":
			method = Strings.TYPEOPS__MEQ;
			break;
		}

		bc.addInvokedynamic(0, method, "(" + Strings.PLANGOBJECT_L
				+ Strings.PLANGOBJECT_L + ")" + Strings.PLANGOBJECT_L);

		if (compilingMethod) {
			bc.add(Opcode.DUP);
			bc.addAstore(storeVar);
		}
	}

	/**
	 * Compile parameters of a method call into an array
	 * 
	 * @param expressionList
	 *            ExpressionList AST
	 * @throws Exception
	 */
	private void compileParameters(ExpressionListContext expressionList)
			throws Exception {

		int numExpr = expressionList == null ? 0 : expressionList.expression()
				.size();
		int store = stacker.acquire();

		// create PLangObject[] array and save it in local variable
		bc.addAnewarray(cp.getCtClass(Strings.PLANGOBJECT_N), numExpr);
		bc.addAstore(store);

		// Evaluate every expression and save it to the array
		int i = 0;
		if (expressionList != null)
			for (ExpressionContext e : expressionList.expression()) {
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

	/**
	 * Compile set operator
	 * 
	 * @param operator
	 *            type of set operator
	 * @param expression
	 *            ExpressionContext AST
	 * @param compilingMethod
	 *            whether we are compiling method call
	 * @param storeVar
	 *            local to store in if we are compiling method call (-1 if not)
	 * @throws Exception
	 */
	private void compileSetOperator(final String operator,
			ExpressionContext expression, boolean compilingMethod, int storeVar)
			throws Exception {

		ExtendedContext lvalue = expression.extended();
		final ExpressionContext second = (ExpressionContext) expression
				.getChild(2);

		new CompileSetOperator(lvalue, operator.equals("="), compilingMethod,
				storeVar) {

			@Override
			public void compileRight() throws Exception {
				isStatementExpression.add(false);
				compileExpression(second, false, -1);
				isStatementExpression.pop();

				if (operator.equals("=")) {
					// Simple assignment
				} else {
					// Operation assignment
					compileBinaryOperator(operator.replace("=", ""), null,
							null, false, -1);
				}
			}

		}.compileSetOperator();

	}

	/**
	 * Compiles primary expression
	 * 
	 * @param primary
	 *            PrimaryContext AST
	 * @param compilingMethod
	 *            whether we are compiling method call
	 * @param storeVar
	 *            local to store in if we are compiling method call (-1 if not)
	 * @throws Exception
	 */
	private void compilePrimaryExpression(PrimaryContext primary,
			boolean compilingMethod, int storeVar) throws Exception {

		if (primary.expression() != null) {
			// it is inner expression, so just compile it
			isStatementExpression.add(false);
			compileExpression(primary.expression(), compilingMethod, storeVar);
			isStatementExpression.pop();
			return;
		}

		if (primary.literal() != null) {
			// Compile literar expression
			LiteralContext l = primary.literal();
			markLine(bc.currentPc(), l.start.getLine());
			if (l.getText().startsWith("NoValue"))
				// compile NoValue
				addNil();
			else if (l.IntegerLiteral() != null) {
				// compile integer literar
				bc.addNew(Strings.INT);
				bc.add(Opcode.DUP);
				bc.addIconst(NumberUtils.toInt(l.IntegerLiteral().getText()));
				bc.addInvokespecial(Strings.INT, "<init>", "(I)V");
			} else if (l.FloatingPointLiteral() != null) {
				// compile float literar
				bc.addNew(Strings.FLOAT);
				bc.add(Opcode.DUP);
				bc.addFconst(NumberUtils.toFloat(l.FloatingPointLiteral()
						.getText()));
				bc.addInvokespecial(Strings.FLOAT, "<init>", "(F)V");
			} else if (l.CharacterLiteral() != null) {
				// compile char literar as integer
				bc.addNew(Strings.INT);
				bc.add(Opcode.DUP);
				bc.addIconst(NumberUtils.toInt(l.CharacterLiteral().getText()));
				bc.addInvokespecial(Strings.INT, "<init>", "(I)V");
			} else if (l.StringLiteral() != null) {
				// compile string literar
				bc.addNew(Strings.STRING_TYPE);
				bc.add(Opcode.DUP);
				bc.addLdc(cacheStrings(Utils.removeStringQuotes(l
						.StringLiteral().getText())));
				bc.addInvokespecial(Strings.STRING_TYPE, "<init>", "("
						+ Strings.STRING_L + ")V");
			} else if (l.BooleanLiteral() != null) {
				// compile boolean literar
				if (l.BooleanLiteral().getText().startsWith("true"))
					bc.addGetstatic(Strings.BOOLEAN_VALUE, "TRUE",
							Strings.BOOLEAN_VALUE_L); // load TRUE
				else
					bc.addGetstatic(Strings.BOOLEAN_VALUE, "FALSE",
							Strings.BOOLEAN_VALUE_L); // load FALSE
			}

			if (compilingMethod) {
				bc.add(Opcode.DUP);
				bc.addAstore(storeVar);
			}
		}

		if (primary.constExpr() != null || primary.getText().startsWith("inst")
				|| primary.getText().startsWith("super")) {
			// compile the const expression or special keywords inst and parent
			String identifier = primary.constExpr().id() != null ? primary
					.constExpr().id().getText() : primary.getText();

			if (identifier.equals("super"))
				identifier = PLClass.___superKey;

			// check for illegal identifiers, ie ___ identifiers or identifiers
			// related to the throwable base class
			if (identifier.startsWith("___")) {
				// 3x _ or more are illegal prefixes used by system
				// methods/fields
				throw new CompilationException(
						"Identifier cannot start with ___, ___ is disabled due to nameclashing with internal methods and fields");
			} else if (identifier.equals("readResolve")) {
				throw new CompilationException(
						"readResolve is reserved keyword used by serialization");
			} else if (identifier.equals("serialVersionUID")) {
				throw new CompilationException(
						"serialVersionUID is reserved keyword used by serialization");
			} else if (identifier.equals("toString")) {
				throw new CompilationException(
						"toString is reserved keyword used by java itself");
			} else if (identifier.equals("getMessage")) {
				throw new CompilationException(
						"getMessage is reserved keyword used by java itself");
			} else if (identifier.equals("getLocalizedMessage")) {
				throw new CompilationException(
						"getLocalizedMessage is reserved keyword used by java itself");
			} else if (identifier.equals("getCause")) {
				throw new CompilationException(
						"getCause is reserved keyword used by java itself");
			} else if (identifier.equals("initCause")) {
				throw new CompilationException(
						"initCause is reserved keyword used by java itself");
			} else if (identifier.equals("printStackTrace")) {
				throw new CompilationException(
						"printStackTrace is reserved keyword used by java itself");
			} else if (identifier.equals("fillInStackTrace")) {
				throw new CompilationException(
						"fillInStackTrace is reserved keyword used by java itself");
			} else if (identifier.equals("getStackTrace")) {
				throw new CompilationException(
						"getStackTrace is reserved keyword used by java itself");
			} else if (identifier.equals("setStackTrace")) {
				throw new CompilationException(
						"setStackTrace is reserved keyword used by java itself");
			} else if (identifier.equals("addSuppressed")) {
				throw new CompilationException(
						"addSuppressed is reserved keyword used by java itself");
			}

			if (referenceMap.containsKey(identifier)) {
				// is a module identifier, use it as key to the module map
				addGetRuntime();
				bc.addLdc(cacheStrings(identifier)); // load string from
														// constants
				bc.addInvokevirtual(Strings.RUNTIME,
						Strings.RUNTIME__GET_MODULE, "(" + Strings.STRING_L
								+ ")" + Strings.PL_MODULE_L); // get module on
																// stack or fail
				return;
			}

			VariableType vt = varStack.getType(identifier);

			switch (vt) {
			case CLASS_VARIABLE:
				if (compilingClass)
					bc.addAload(1); // load self
				else
					bc.addAload(0); // load this
				bc.addCheckcast(Strings.BASE_COMPILED_STUB);
				if (compilingMethod) {
					bc.add(Opcode.DUP);
					bc.addAstore(storeVar);
				}
				bc.addLdc(cacheStrings(identifier)); // load string from
														// constants
				bc.addIconst(identifier.equals(PLClass.___superKey) ? 1 : 0);
				bc.addInvokevirtual(Strings.BASE_COMPILED_STUB,
						Strings.BASE_COMPILED_STUB__GETKEY, "("
								+ Strings.STRING_L + "Z)"
								+ Strings.PLANGOBJECT_L);
				break;
			case LOCAL_VARIABLE:
				bc.addAload(varStack.getLocal(identifier)); // load from local
															// variables
				if (compilingMethod) {
					bc.add(Opcode.DUP);
					bc.addAstore(storeVar);
				}
				break;
			case MODULE_VARIABLE:
				// test whether class contains the variable since it might be
				// owned by superclass
				if (compilingClass)
					bc.addAload(1); // load self
				else
					bc.addAload(0); // load this
				bc.addCheckcast(Strings.BASE_COMPILED_STUB);
				if (compilingMethod) {
					bc.add(Opcode.DUP);
					bc.addAstore(storeVar);
				}
				bc.addLdc(cacheStrings(identifier)); // load string from
														// constants
				bc.addIconst(0);
				bc.addInvokevirtual(Strings.BASE_COMPILED_STUB,
						Strings.BASE_COMPILED_STUB__GETKEY, "("
								+ Strings.STRING_L + "Z)"
								+ Strings.PLANGOBJECT_L);
				bc.add(Opcode.DUP);

				int key = labelCounter++;
				addLabel(new IfNotNullJumpLabelInfo(), key);
				bc.add(Opcode.POP);
				addGetRuntime();
				bc.addLdc(cacheStrings(moduleName)); // load string from
														// constants
				bc.addInvokevirtual(Strings.RUNTIME,
						Strings.RUNTIME__GET_MODULE, "(" + Strings.STRING_L
								+ ")" + Strings.PL_MODULE_L); // get module on
																// stack or fail
				if (compilingMethod) {
					bc.add(Opcode.DUP);
					bc.addAstore(storeVar);
				}
				bc.addCheckcast(Strings.BASE_COMPILED_STUB);
				bc.addLdc(cacheStrings(identifier)); // load string from
														// constants
				bc.addIconst(0);
				bc.addInvokevirtual(Strings.BASE_COMPILED_STUB,
						Strings.BASE_COMPILED_STUB__GETKEY, "("
								+ Strings.STRING_L + "Z)"
								+ Strings.PLANGOBJECT_L);

				setLabelPos(key);
				bc.add(Opcode.NOP);

				break;
			}
		}

	}

	/**
	 * Add plang throw BaseException statement with string text provided
	 * 
	 * @param string
	 *            Text of the exception
	 * @throws Exception
	 */
	private void addThrow(String string) throws Exception {
		addGetRuntime();
		bc.addLdc(cacheStrings("System.BaseException"));
		bc.addAnewarray(cp.getCtClass(Strings.PLANGOBJECT_N), 1);
		bc.add(Opcode.DUP);
		bc.addIconst(0);
		bc.addNew(Strings.STRING_TYPE);
		bc.add(Opcode.DUP);
		bc.addLdc(cacheStrings(Utils.removeStringQuotes(string)));
		bc.addInvokespecial(Strings.STRING_TYPE, "<init>", "("
				+ Strings.STRING_L + ")V");
		bc.add(Opcode.AASTORE);
		bc.addInvokevirtual(Strings.RUNTIME, Strings.RUNTIME__NEW_INSTANCE, "("
				+ Strings.STRING_L + "[" + Strings.PLANGOBJECT_L + ")"
				+ Strings.PL_CLASS_L);
		bc.addCheckcast(Strings.PL_CLASS);
		bc.add(Opcode.ATHROW);
	}

	/**
	 * Prunes dead code from the bytecode
	 * 
	 * @throws Exception
	 */
	// Magic method again, do not touch
	public void pruneDeadCode() throws Exception {
		byte[] bytecode = bc.get();
		ExceptionTable etable = bc.getExceptionTable();

		int counter = 0;
		// store exception handlers so we can later modify them if we prune some
		// code out
		List<ExceptionHandler> eh = new ArrayList<ExceptionHandler>();
		BidiMultiMap<Integer, IntegerLink> linkMap = new BidiMultiHashMap<Integer, IntegerLink>();

		for (int i = 0; i < etable.size(); i++) {
			ExceptionHandler ehi = new ExceptionHandler();
			ehi.bcLink = counter++;
			linkMap.put(ehi.bcLink, new IntegerLink(etable.handlerPc(i)));
			ehi.startLink = counter++;
			IntegerLink startLink = new IntegerLink(etable.startPc(i));
			linkMap.put(ehi.startLink, startLink);
			startLink.isStartPos = true;
			ehi.endLink = counter++;
			linkMap.put(ehi.endLink, new IntegerLink(etable.endPc(i)));
			ehi.type = etable.catchType(i);
			eh.add(ehi);
		}

		// disected bytecode is stored here
		Map<Integer, Instruction> iPosList = new HashMap<Integer, Instruction>();
		List<Instruction> insts = parseBytecode(bytecode, iPosList);

		// mark dead code in the bytecode
		markDeadCode(insts, iPosList, etable);

		// prune dead code and fix exceptions
		List<ExceptionData> copy = new ArrayList<ExceptionData>();
		List<Instruction> prunned = new ArrayList<Instruction>();
		BidiMultiMap<Integer, IntegerLink> cpyMap = new BidiMultiHashMap<Integer, IntegerLink>(
				linkMap);
		for (Instruction i : insts) {
			if (i.visited)
				// do not prune this instruction
				prunned.add(i);
			else {
				// prune the instruction, then fix the exceptions
				int bcpos = i.originalPos;
				for (IntegerLink ilink : cpyMap.values()) {
					if (ilink.i > bcpos) {
						ilink.i -= i.instBytes.length;
					} else if (ilink.i == bcpos && ilink.isStartPos) {
						linkMap.removeValue(ilink);
					}
				}
			}
		}

		// add exceptions back
		for (ExceptionHandler ehi : eh) {
			if (linkMap.get(ehi.startLink) != null)
				copy.add(new ExceptionData(linkMap.get(ehi.startLink).i,
						linkMap.get(ehi.endLink).i, linkMap.get(ehi.bcLink).i,
						ehi.type));
		}

		recalculateJumps(prunned);

		// write into bytecode
		bc = new Bytecode(pool);
		for (Instruction i : prunned) {
			if (i.lineNo != null)
				writeLine(bc.currentPc(), i.lineNo);
			for (byte b : i.instBytes)
				bc.add(b);
		}

		for (int i = 0; i < copy.size(); i++)
			bc.addExceptionHandler(copy.get(i).s, copy.get(i).e, copy.get(i).h,
					copy.get(i).t);
	}

	/**
	 * Recalculate jumps for new prunned code
	 * 
	 * @param prunned
	 */
	private void recalculateJumps(List<Instruction> prunned) {
		for (Instruction i : prunned) {
			if (i.branchTarget != null) {
				short distance = getDistance(i, i.branchTarget, prunned);
				i.instBytes[1] = (byte) (distance >> 8);
				i.instBytes[2] = (byte) distance;
			}
		}
	}

	/**
	 * Returns distance between two instructions
	 * 
	 * @param a
	 * @param b
	 * @param list
	 * @return
	 */
	private short getDistance(Instruction a, Instruction b,
			List<Instruction> list) {

		int idxa = list.indexOf(a);
		int idxb = list.indexOf(b);

		if (idxa == idxb)
			return 0;

		int idxs = Math.min(idxa, idxb);
		int idxl = Math.max(idxa, idxb);

		short delta = getDistance(idxs, idxl, list);

		if (idxa < idxb)
			return delta;
		else
			return (short) -delta;
	}

	/**
	 * Returns distance between two positions
	 * 
	 * @param idxs
	 * @param idxl
	 * @param list
	 * @return
	 */
	private short getDistance(int idxs, int idxl, List<Instruction> list) {
		short delta = 0;
		for (int i = idxs; i < idxl; i++) {
			delta += list.get(i).instBytes.length;
		}
		return delta;
	}

	/**
	 * Mark dead code in bytecode
	 * 
	 * @param insts
	 * @param iPosList
	 * @param etable
	 */
	private void markDeadCode(List<Instruction> insts,
			Map<Integer, Instruction> iPosList, ExceptionTable etable) {
		markFrom(insts, 0);
		for (int i = 0; i < etable.size(); i++) {
			int startPc = etable.startPc(i);
			if (iPosList.get(startPc).visited)
				markFrom(insts,
						insts.indexOf(iPosList.get(etable.handlerPc(i))));
		}
	}

	/**
	 * Mark dead code from this position in list
	 * 
	 * @param insts
	 * @param i
	 */
	private void markFrom(List<Instruction> insts, int i) {
		while (i < insts.size()) {
			Instruction inst = insts.get(i);
			if (inst.visited)
				return;

			inst.visited = true;
			if (inst.branchTarget != null) {
				markFrom(insts, insts.indexOf(inst.branchTarget));
			}

			int opcode = inst.instBytes[0] & 0x000000FF;

			switch (opcode) {
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

	/**
	 * Parse bytecode into list of instructions
	 * 
	 * @param bytecode
	 * @param iPosList
	 * @return
	 * @throws Exception
	 */
	private List<Instruction> parseBytecode(byte[] bytecode,
			Map<Integer, Instruction> iPosList) throws Exception {
		List<Instruction> iList = new ArrayList<Instruction>();
		for (int i = 0; i < bytecode.length; i++) {
			Instruction in = new Instruction();
			int pos = i;

			switch (bytecode[i] & 0x000000FF) {
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
				int cbc = bytecode[i] & 0x000000FF;
				if (cbc == 0x84) {
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
		for (Instruction i : iList) {
			int opcode = i.instBytes[0] & 0x000000FF;

			switch (opcode) {
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
			}
				break;

			case 0xc8: {
				int branchbyte1 = i.instBytes[1] & 0x000000FF;
				int branchbyte2 = i.instBytes[2] & 0x000000FF;
				int branchbyte3 = i.instBytes[3] & 0x000000FF;
				int branchbyte4 = i.instBytes[4] & 0x000000FF;
				short offset = (short) ((branchbyte1 << 24)
						+ (branchbyte2 << 16) + (branchbyte3 << 8) + branchbyte4);
				int instoffset = it + offset;
				i.branchTarget = iPosList.get(instoffset);
			}
				break;

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

	/**
	 * Returns class loader for where to store new classes
	 * 
	 * @return
	 */
	public ClassLoader getClassLoader() {
		return PLRuntime.getRuntime().getClassLoader();
	}

	/**
	 * Marks the line of the code
	 * 
	 * @param pc
	 * @param line
	 * @throws Exception
	 */
	private void markLine(int pc, int line) throws Exception {
		bcToLineMap.put(pc, line);
	}

	/**
	 * Write line info into stream
	 * 
	 * @param pc
	 * @param line
	 * @throws Exception
	 */
	private void writeLine(int pc, int line) throws Exception {
		if (line == lastLineWritten)
			return;
		lineNumberStream.writeShort(pc);
		lineNumberStream.writeShort(line);
		lastLineWritten = line;
	}

	/**
	 * Cache strings so only one copy is stored into pool
	 * 
	 * @param string
	 * @return
	 */
	private int cacheStrings(String string) {
		if (!cache.containsKey(string)) {
			cache.put(string, pool.addStringInfo(string));
		}
		return cache.get(string);
	}

	/**
	 * Set label position identified by key to current bytecode
	 * 
	 * @param key
	 * @return
	 */
	private int setLabelPos(int key) {
		int cpc = bc.currentPc();
		labelMap.put(key, cpc);
		return key;
	}

	/**
	 * Returns label position from the map based on the key provided
	 * 
	 * @param key
	 * @return
	 */
	private int getValue(int key) {
		return labelMap.get(key);
	}

	/**
	 * Add label of the LabelInfo type identified by key
	 * 
	 * @param nfo
	 * @param key
	 */
	private void addLabel(LabelInfo nfo, int key) {
		nfo.poskey = key;
		nfo.bcpos = bc.currentPc();
		labelList.add(nfo);
		// Placeholder NOPs
		for (int i = 0; i < 3; i++)
			bc.add(Opcode.NOP);
	}
}
