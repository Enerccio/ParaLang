package cz.upol.vanusanik.paralang.utils.gen;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.Interval;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import cz.upol.vanusanik.paralang.plang.PLangBaseVisitor;
import cz.upol.vanusanik.paralang.plang.PLangLexer;
import cz.upol.vanusanik.paralang.plang.PLangParser;
import cz.upol.vanusanik.paralang.plang.PLangParser.ClassDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.CompilationUnitContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.DocCommentContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.FieldDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.FunctionDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ModuleDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.VariableDeclaratorContext;

public class GenerateDocumentation {

	private static class FuncDef {
		public String args;
		public String returns;
		public String doc;
		public String name;
	}

	private static class FieldDec {
		public String doc;
		public String name;
		public String type;
		public String init;
	}

	private static class ClassData {
		public String desc;
		public String pclass;
		public List<FieldDec> fields = new ArrayList<FieldDec>();
		public List<FuncDef> funcs = new ArrayList<FuncDef>();
	}

	private static class ModuleData extends ClassData {
		public Map<String, ClassData> classes = new TreeMap<String, ClassData>();
	}

	private String template;
	private Map<String, ModuleData> module = new TreeMap<String, ModuleData>();

	public static void main(String[] args) throws Exception {
		new GenerateDocumentation().run(args);
	}

	private void run(String[] args) throws Exception {
		template = IOUtils.toString(GenerateDocumentation.class
				.getClassLoader().getResourceAsStream(
						"cz/upol/vanusanik/paralang/utils/gen/template.tpx"));

		File output = new File(args[0]);
		File input = new File(args[1]);

		for (File f : input.listFiles()) {
			if (!f.isDirectory() && f.getName().endsWith(".plang"))
				createDocumentation(f);
		}

		String headers = "";
		String body = "";
		String modLinkBase = "doc-sl-moduledef";
		for (String moduleName : module.keySet()) {
			String modLink = modLinkBase + "-" + moduleName;
			headers += "<li><a href=\"#" + modLink + "\">Module: " + moduleName
					+ "</a>";
			body += "<h3 id=\"" + modLink + "\">Module: " + moduleName
					+ "</h3>";
			body += "\n";

			ModuleData md = module.get(moduleName);
			if (md.desc != null)
				body += md.desc;

			body = addFields(md.fields, body);
			body = addFuncs(md.funcs, body, "function");

			if (md.classes.size() != 0) {
				headers += "<ol>";
				for (String className : md.classes.keySet()) {
					if (className.startsWith("__"))
						continue;
					String classLink = modLink + "-" + className;
					headers += "<li><a href=\"#" + classLink + "\">Class: "
							+ className + "</a>";

					body += "<h4 id=\"" + classLink + "\">Class: " + className
							+ "</h4>";
					body += "\n";

					ClassData cd = md.classes.get(className);
					if (cd.pclass != null) {
						body += "<b>Parent class: </b> <code>" + cd.pclass
								+ "</code> <hr/>";
						body += "\n";
					}

					if (cd.desc != null)
						body += cd.desc;

					body = addFields(cd.fields, body);
					body = addFuncs(cd.funcs, body, "method");
				}
				headers += "</ol>";
			}

			headers += "</li>";
			body += "<hr/><a href=\"#toc\">Back to top</a>";
		}

		String content = template.replace("$$moduledefheader$$", headers);
		content = content.replace("$$moduledefbody$$", body);
		IOUtils.write(content, new FileOutputStream(output));
	}

	private String addFuncs(List<FuncDef> funcs, String body, String funcOrMeth) {
		funcs = replaceFuncs(funcs);
		if (funcs.size() == 0)
			return body;

		body += "<table><tr><td width=\"10%\"><b>Name of "
				+ funcOrMeth
				+ "</b></td><td width=\"20%\"><b>arity and types of parameters</b></td><td width=\"10%\"><b>return type</b></td><td width=\"60%\"><b>Description</b></td></tr>";
		for (FuncDef fd : funcs)
			body = addFunc(fd, body);

		body += "<table>";
		return body;
	}

	private List<FuncDef> replaceFuncs(List<FuncDef> funcs) {
		List<FuncDef> fdl = new ArrayList<FuncDef>();
		for (FuncDef fd : funcs) {
			if (fd.name.startsWith("__"))
				continue;
			fdl.add(fd);
		}
		return fdl;
	}

	private String addFields(List<FieldDec> fields, String body) {
		fields = replace(fields);
		if (fields.size() == 0)
			return body;

		body += "<table><tr><td width=\"10%\"><b>Name of field</b></td><td width=\"20%\"><b>type of field</b></td><td width=\"10%\"><b>initial value</b></td><td width=\"60%\"><b>Description</b></td></tr>";
		for (FieldDec fd : fields)
			body = addField(fd, body);

		body += "<table>";

		return body;
	}

	private List<FieldDec> replace(List<FieldDec> fields) {
		List<FieldDec> fdl = new ArrayList<FieldDec>();
		for (FieldDec fd : fields) {
			if (fd.name.startsWith("__"))
				continue;
			fdl.add(fd);
		}
		return fdl;
	}

	private String addFunc(FuncDef fd, String body) {
		body += "<tr><td width=\"10%\"><code>" + fd.name
				+ "</code></td><td width=\"20%\">"
				+ (fd.args == null ? "" : fd.args)
				+ "</td><td width=\"10%\"><code>"
				+ (fd.returns == null ? "" : fd.returns)
				+ "</code></td><td width=\"60%\">"
				+ (fd.doc == null ? "" : fd.doc) + "</td></tr>";
		return body;
	}

	private String addField(FieldDec fd, String body) {
		body += "<tr><td width=\"10%\"><code>" + fd.name
				+ "</code></td><td width=\"20%\"><code>"
				+ (fd.type == null ? "" : fd.type)
				+ "</code></td><td width=\"10%\">"
				+ (fd.init == null ? "" : fd.init) + "</td><td width=\"60%\">"
				+ (fd.doc == null ? "" : fd.doc) + "</td></tr>";
		return body;
	}

	private String cmodule;
	private String cclass;
	private String parsedDocComment;
	private Map<String, String> keywords;

	private void createDocumentation(File f) throws FileNotFoundException,
			IOException, Exception {
		cmodule = null;
		parsedDocComment = null;

		final ANTLRInputStream is = new ANTLRInputStream(
				new ByteArrayInputStream(IOUtils.toString(
						new FileInputStream(f)).getBytes("utf-8")));
		PLangBaseVisitor<Void> visitor = new PLangBaseVisitor<Void>() {

			@Override
			public Void visitDocComment(DocCommentContext ctx) {
				if (ctx == null) {
					parsedDocComment = null;
					return null;
				}

				int a = ctx.start.getStartIndex();
				int b = ctx.stop.getStopIndex();
				Interval interval = new Interval(a, b);
				String text = is.getText(interval);

				text = text.substring(3);
				text = text.substring(0, text.length() - 3);
				try {
					parsedDocComment = parseDocComment(text);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return super.visitDocComment(ctx);
			}

			@Override
			public Void visitModuleDeclaration(ModuleDeclarationContext ctx) {
				cmodule = ctx.Identifier().getText();
				module.put(cmodule, new ModuleData());

				ModuleData md = module.get(cmodule);
				md.desc = parsedDocComment;

				parsedDocComment = null;
				return super.visitModuleDeclaration(ctx);
			}

			FieldDec fd;

			@Override
			public Void visitFieldDeclaration(FieldDeclarationContext ctx) {
				fd = new FieldDec();
				fd.doc = parsedDocComment;
				if (fd.doc != null) {
					fd.type = keywords.get("type");
					fd.init = keywords.get("init");
				}
				super.visitFieldDeclaration(ctx);
				fd = null;
				parsedDocComment = null;
				return null;
			}

			@Override
			public Void visitVariableDeclarator(VariableDeclaratorContext ctx) {
				if (fd == null)
					return null;

				FieldDec fdd = new FieldDec();
				fdd.doc = fd.doc;
				fdd.type = fd.type;
				fdd.init = fd.init;
				fdd.name = ctx.variableDeclaratorId().Identifier().getText();
				if (cclass != null)
					module.get(cmodule).classes.get(cclass).fields.add(fdd);
				else
					module.get(cmodule).fields.add(fdd);
				return null;
			}

			@Override
			public Void visitClassDeclaration(ClassDeclarationContext ctx) {
				String doc = parsedDocComment;
				cclass = ctx.Identifier().getText();
				module.get(cmodule).classes.put(cclass, new ClassData());
				if (ctx.type() != null)
					module.get(cmodule).classes.get(cclass).pclass = ctx.type()
							.getText();
				super.visitClassDeclaration(ctx);
				module.get(cmodule).classes.get(cclass).desc = doc;
				cclass = null;
				parsedDocComment = null;
				return null;
			}

			@Override
			public Void visitFunctionDeclaration(FunctionDeclarationContext ctx) {
				FuncDef fncDef = new FuncDef();
				fncDef.doc = parsedDocComment;
				if (fncDef.doc != null) {
					fncDef.returns = keywords.get("return");
				}
				int a = ctx.formalParameters().start.getStartIndex();
				int b = ctx.formalParameters().stop.getStopIndex();
				Interval interval = new Interval(a, b);
				fncDef.args = is.getText(interval).substring(1);
				fncDef.args = fncDef.args
						.substring(0, fncDef.args.length() - 1);
				fncDef.name = ctx.Identifier().getText();
				if (cclass != null)
					module.get(cmodule).classes.get(cclass).funcs.add(fncDef);
				else
					module.get(cmodule).funcs.add(fncDef);
				super.visitFunctionDeclaration(ctx);
				parsedDocComment = null;
				return null;
			}

		};
		visitor.visit(parse(is));
	}

	protected String parseDocComment(String doc) throws Exception {
		keywords = new HashMap<String, String>();
		String[] lines = StringUtils.split(doc, "\n\r");
		String res = "";

		for (String l : lines) {
			String line = l.trim();
			if (line.equals(""))
				res += "\n";
			else {
				if (line.startsWith("*ret ")) {
					keywords.put("return", line.substring(5));
					continue;
				} else if (line.startsWith("*type ")) {
					keywords.put("type", line.substring(6));
					continue;
				} else if (line.startsWith("*init ")) {
					keywords.put("init", line.substring(6));
					continue;
				}
				res += line;
			}
		}

		System.err.println(res);
		return res.replace("\n", "<br />");
	}

	private CompilationUnitContext parse(ANTLRInputStream is) throws Exception {
		PLangLexer lexer = new PLangLexer(is);
		CommonTokenStream stream = new CommonTokenStream(lexer);
		PLangParser parser = new PLangParser(stream);
		return parser.compilationUnit();
	}

}
