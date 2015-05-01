package cz.upol.vanusanik.paralang.compiler;

/**
 * Raised when there is problem with compilation.
 * 
 * @author Enerccio
 */
public class CompilationException extends Exception {
	private static final long serialVersionUID = 4551411199779952804L;

	/**
	 * Creates new CompilationException with String message
	 * 
	 * @param message
	 */
	public CompilationException(String message) {
		super(message);
	}

	/**
	 * Creates new CompilationException with throwable as cause
	 * 
	 * @param t
	 */
	public CompilationException(Throwable t) {
		super(t);
	}
}
