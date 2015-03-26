package cz.upol.vanusanik.paralang.compiler;

public class CompilationException extends Exception {
	private static final long serialVersionUID = 4551411199779952804L;

	public CompilationException(String message){
		super(message);
	}
	
	public CompilationException(Throwable t){
		super(t);
	}
}
