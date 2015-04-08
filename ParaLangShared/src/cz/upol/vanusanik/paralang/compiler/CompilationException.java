package cz.upol.vanusanik.paralang.compiler;

/**
 * Raised when there is problem with compilation.
 * @author Enerccio
 */
public class CompilationException extends Exception {
	private static final long serialVersionUID = 4551411199779952804L;

	public CompilationException(String message){
		super(message);
	}
	
	public CompilationException(Throwable t){
		super(t);
	}
}
