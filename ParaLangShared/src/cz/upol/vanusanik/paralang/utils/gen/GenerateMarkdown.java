package cz.upol.vanusanik.paralang.utils.gen;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.io.IOUtils;

import cz.upol.vanusanik.paralang.plang.PLangBaseVisitor;
import cz.upol.vanusanik.paralang.plang.PLangLexer;
import cz.upol.vanusanik.paralang.plang.PLangParser;
import cz.upol.vanusanik.paralang.plang.PLangParser.BlockContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.BlockStatementContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.CatchClauseContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ClassBodyContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ClassBodyDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ClassDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.CompilationUnitContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ConstExprContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ConstantExpressionContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ConstructorBodyContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ConstructorCallContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ExpressionContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ExpressionListContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ExtendedContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.FieldDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.FinallyBlockContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ForControlContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ForInitContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ForUpdateContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.FormalParameterContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.FormalParameterListContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.FormalParametersContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.FunctionBodyContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.FunctionDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.IdContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.IdentifiedContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ImportDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.LastFormalParameterContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.LiteralContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.LocalVariableDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.LocalVariableDeclarationStatementContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.MemberDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.MethodCallContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ModuleDeclarationContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ModuleDeclarationsContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.ParExpressionContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.PrimaryContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.QualifiedNameContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.QualifiedNameListContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.SingleQualifiedNameContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.StatementContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.StatementExpressionContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.TypeContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.VariableDeclaratorContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.VariableDeclaratorIdContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.VariableDeclaratorsContext;
import cz.upol.vanusanik.paralang.plang.PLangParser.VariableInitializerContext;

public class GenerateMarkdown {

	public static void main(String[] a) throws Exception {
		new GenerateMarkdown()
				.run(new FileInputStream(new File("src/input.in")));
	}

	private void run(FileInputStream fileInputStream) throws Exception {
		String data = IOUtils.toString(fileInputStream);
		String output = "<!-- " + data + "-->\n";
		output += format(data);
		System.out.println(output);
	}

	public String run(String content) throws Exception {
		String output = "<!-- " + content + "-->\n";
		output += format(content);
		return output;
	}

	private class Block implements Comparable<Block> {
		private int index;
		private String value;

		@Override
		public int compareTo(Block arg0) {
			return Integer.compare(index, arg0.index);
		}
	}

	private String format(String data) throws Exception {
		ParserRuleContext ctx = parse(data);
		String content = data;

		final List<Block> blocks = new ArrayList<Block>();

		PLangBaseVisitor<Void> visitor = new PLangBaseVisitor<Void>() {

			@Override
			public Void visitExpression(ExpressionContext ctx) {
				return super.visitExpression(ctx);
			}

			@Override
			public Void visitVariableDeclarator(VariableDeclaratorContext ctx) {
				return super.visitVariableDeclarator(ctx);
			}

			@Override
			public Void visitVariableDeclaratorId(
					VariableDeclaratorIdContext ctx) {
				return super.visitVariableDeclaratorId(ctx);
			}

			@Override
			public Void visitMethodCall(MethodCallContext ctx) {
				return super.visitMethodCall(ctx);
			}

			@Override
			public Void visitCompilationUnit(CompilationUnitContext ctx) {
				return super.visitCompilationUnit(ctx);
			}

			@Override
			public Void visitFormalParameter(FormalParameterContext ctx) {
				return super.visitFormalParameter(ctx);
			}

			@Override
			public Void visitSingleQualifiedName(SingleQualifiedNameContext ctx) {
				return super.visitSingleQualifiedName(ctx);
			}

			@Override
			public Void visitConstExpr(ConstExprContext ctx) {
				int start = ctx.getStart().getStartIndex();
				int end = ctx.getStop().getStopIndex() + 1;
				Block b = new Block();
				b.index = start;
				b.value = "<span class=\"pl-s3\">";
				blocks.add(b);
				b = new Block();
				b.index = end;
				b.value = "</span>";
				blocks.add(b);
				return super.visitConstExpr(ctx);
			}

			@Override
			public Void visitExpressionList(ExpressionListContext ctx) {
				return super.visitExpressionList(ctx);
			}

			@Override
			public Void visitStatementExpression(StatementExpressionContext ctx) {
				return super.visitStatementExpression(ctx);
			}

			@Override
			public Void visitBlock(BlockContext ctx) {
				return super.visitBlock(ctx);
			}

			@Override
			public Void visitVariableInitializer(VariableInitializerContext ctx) {
				return super.visitVariableInitializer(ctx);
			}

			@Override
			public Void visitBlockStatement(BlockStatementContext ctx) {
				return super.visitBlockStatement(ctx);
			}

			@Override
			public Void visitIdentified(IdentifiedContext ctx) {
				return super.visitIdentified(ctx);
			}

			@Override
			public Void visitType(TypeContext ctx) {
				return super.visitType(ctx);
			}

			@Override
			public Void visitForUpdate(ForUpdateContext ctx) {
				return super.visitForUpdate(ctx);
			}

			@Override
			public Void visitQualifiedNameList(QualifiedNameListContext ctx) {
				return super.visitQualifiedNameList(ctx);
			}

			@Override
			public Void visitId(IdContext ctx) {
				return super.visitId(ctx);
			}

			@Override
			public Void visitMemberDeclaration(MemberDeclarationContext ctx) {
				return super.visitMemberDeclaration(ctx);
			}

			@Override
			public Void visitLocalVariableDeclarationStatement(
					LocalVariableDeclarationStatementContext ctx) {
				String text = ctx.getChild(0).getText();
				String[] arr = text.split(" ");
				int start = ctx.start.getStartIndex();
				int end = arr[0].length() + start;
				Block b = new Block();
				b.index = start;
				b.value = "<span class=\"pl-k\">";
				blocks.add(b);
				b = new Block();
				b.index = end;
				b.value = "</span>";
				blocks.add(b);
				return super.visitLocalVariableDeclarationStatement(ctx);
			}

			@Override
			public Void visitPrimary(PrimaryContext ctx) {
				return super.visitPrimary(ctx);
			}

			@Override
			public Void visitConstructorCall(ConstructorCallContext ctx) {
				return super.visitConstructorCall(ctx);
			}

			@Override
			public Void visitFieldDeclaration(FieldDeclarationContext ctx) {
				String text = ctx.getChild(0).getText();
				String[] arr = text.split(" ");
				int start = ctx.start.getStartIndex();
				int end = arr[0].length() + start;
				Block b = new Block();
				b.index = start;
				b.value = "<span class=\"pl-k\">";
				blocks.add(b);
				b = new Block();
				b.index = end;
				b.value = "</span>";
				blocks.add(b);
				return super.visitFieldDeclaration(ctx);
			}

			@Override
			public Void visitClassBody(ClassBodyContext ctx) {
				return super.visitClassBody(ctx);
			}

			@Override
			public Void visitModuleDeclarations(ModuleDeclarationsContext ctx) {
				return super.visitModuleDeclarations(ctx);
			}

			@Override
			public Void visitImportDeclaration(ImportDeclarationContext ctx) {
				String text = ctx.getChild(0).getText();
				String[] arr = text.split(" ");
				int start = ctx.start.getStartIndex();
				int end = arr[0].length() + start;
				Block b = new Block();
				b.index = start;
				b.value = "<span class=\"pl-k\">";
				blocks.add(b);
				b = new Block();
				b.index = end;
				b.value = "</span>";
				blocks.add(b);
				return super.visitImportDeclaration(ctx);
			}

			@Override
			public Void visitFormalParameterList(FormalParameterListContext ctx) {
				return super.visitFormalParameterList(ctx);
			}

			@Override
			public Void visitFinallyBlock(FinallyBlockContext ctx) {
				String text = ctx.getChild(0).getText();
				String[] arr = text.split(" ");
				int start = ctx.start.getStartIndex();
				int end = arr[0].length() + start;
				Block b = new Block();
				b.index = start;
				b.value = "<span class=\"pl-k\">";
				blocks.add(b);
				b = new Block();
				b.index = end;
				b.value = "</span>";
				blocks.add(b);
				return super.visitFinallyBlock(ctx);
			}

			@Override
			public Void visitParExpression(ParExpressionContext ctx) {
				return super.visitParExpression(ctx);
			}

			@Override
			public Void visitQualifiedName(QualifiedNameContext ctx) {
				return super.visitQualifiedName(ctx);
			}

			@Override
			public Void visitClassDeclaration(ClassDeclarationContext ctx) {
				String text = ctx.getChild(0).getText();
				String[] arr = text.split(" ");
				int start = ctx.start.getStartIndex();
				int end = arr[0].length() + start;
				Block b = new Block();
				b.index = start;
				b.value = "<span class=\"pl-k\">";
				blocks.add(b);
				b = new Block();
				b.index = end;
				b.value = "</span>";
				blocks.add(b);
				return super.visitClassDeclaration(ctx);
			}

			@Override
			public Void visitExtended(ExtendedContext ctx) {
				return super.visitExtended(ctx);
			}

			@Override
			public Void visitFunctionBody(FunctionBodyContext ctx) {
				return super.visitFunctionBody(ctx);
			}

			@Override
			public Void visitVariableDeclarators(VariableDeclaratorsContext ctx) {
				return super.visitVariableDeclarators(ctx);
			}

			@Override
			public Void visitStatement(StatementContext ctx) {

				if (ctx.ifStatement() != null) {
					if (ctx.ifStatement().statement().size() == 2) {
						// has else
						int start = ctx.ifStatement().statement().get(0)
								.getStop().getStopIndex() + 1;
						int end = ctx.ifStatement().statement().get(1)
								.getStart().getStartIndex();
						Block b = new Block();
						b.index = start;
						b.value = "<span class=\"pl-k\">";
						blocks.add(b);
						b = new Block();
						b.index = end;
						b.value = "</span>";
						blocks.add(b);
					}
				}

				return super.visitStatement(ctx);
			}

			@Override
			public Void visitConstructorBody(ConstructorBodyContext ctx) {
				return super.visitConstructorBody(ctx);
			}

			@Override
			public Void visitCatchClause(CatchClauseContext ctx) {
				String text = ctx.getChild(0).getText();
				String[] arr = text.split(" ");
				int start = ctx.start.getStartIndex();
				int end = arr[0].length() + start;
				Block b = new Block();
				b.index = start;
				b.value = "<span class=\"pl-k\">";
				blocks.add(b);
				b = new Block();
				b.index = end;
				b.value = "</span>";
				blocks.add(b);
				return super.visitCatchClause(ctx);
			}

			@Override
			public Void visitFormalParameters(FormalParametersContext ctx) {
				return super.visitFormalParameters(ctx);
			}

			@Override
			public Void visitConstantExpression(ConstantExpressionContext ctx) {
				return super.visitConstantExpression(ctx);
			}

			@Override
			public Void visitClassBodyDeclaration(
					ClassBodyDeclarationContext ctx) {
				return super.visitClassBodyDeclaration(ctx);
			}

			@Override
			public Void visitForControl(ForControlContext ctx) {
				return super.visitForControl(ctx);
			}

			@Override
			public Void visitLastFormalParameter(LastFormalParameterContext ctx) {
				return super.visitLastFormalParameter(ctx);
			}

			@Override
			public Void visitForInit(ForInitContext ctx) {
				if (ctx.getText().startsWith("var")) {
					String text = ctx.getChild(0).getText();
					String[] arr = text.split(" ");
					int start = ctx.start.getStartIndex();
					int end = arr[0].length() + start;
					Block b = new Block();
					b.index = start;
					b.value = "<span class=\"pl-k\">";
					blocks.add(b);
					b = new Block();
					b.index = end;
					b.value = "</span>";
					blocks.add(b);
				}
				return super.visitForInit(ctx);
			}

			@Override
			public Void visitFunctionDeclaration(FunctionDeclarationContext ctx) {
				int start = ctx.getStart().getStartIndex();
				int end = ctx.formalParameters().getStop().getStopIndex() + 1;
				Block b = new Block();
				b.index = start;
				b.value = "<span class=\"pl-s3\">";
				blocks.add(b);
				b = new Block();
				b.index = end;
				b.value = "</span>";
				blocks.add(b);

				return super.visitFunctionDeclaration(ctx);
			}

			@Override
			public Void visitLocalVariableDeclaration(
					LocalVariableDeclarationContext ctx) {
				return super.visitLocalVariableDeclaration(ctx);
			}

			@Override
			public Void visitModuleDeclaration(ModuleDeclarationContext ctx) {
				String text = ctx.getChild(0).getText();
				String[] arr = text.split(" ");
				int start = ctx.start.getStartIndex();
				int end = arr[0].length() + start;
				Block b = new Block();
				b.index = start;
				b.value = "<span class=\"pl-k\">";
				blocks.add(b);
				b = new Block();
				b.index = end;
				b.value = "</span>";
				blocks.add(b);
				return super.visitModuleDeclaration(ctx);
			}

			@Override
			public Void visitLiteral(LiteralContext ctx) {
				int start = ctx.getStart().getStartIndex();
				int end = ctx.getStop().getStopIndex() + 1;
				Block b = new Block();
				b.index = start;
				b.value = "<span class=\"pl-s2\"><span class=\"pl-pds\">";
				blocks.add(b);
				b = new Block();
				b.index = end;
				b.value = "</span></span>";
				blocks.add(b);
				return super.visitLiteral(ctx);
			}

		};
		visitor.visit(ctx);

		Collections.sort(blocks);

		for (int i = blocks.size() - 1; i >= 0; i--) {
			Block b = blocks.get(i);
			content = content.substring(0, b.index) + b.value
					+ content.substring(b.index);
		}

		return "<div class=\"highlight highlight-js\"><pre>" + content
				+ "</pre></div>";
	}

	private CompilationUnitContext parse(String in) throws Exception {
		ANTLRInputStream is = new ANTLRInputStream(new ByteArrayInputStream(
				in.getBytes("utf-8")));
		PLangLexer lexer = new PLangLexer(is);
		CommonTokenStream stream = new CommonTokenStream(lexer);
		PLangParser parser = new PLangParser(stream);
		return parser.compilationUnit();
	}
}
