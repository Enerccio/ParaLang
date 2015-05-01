package cz.upol.vanusanik.paralang.compiler;

/**
 * Stores reference to a key. Ie String -> java.util.String if java.util.String
 * is imported. Same for using directive, ie PrintStream -> IO.PrintStream.
 * 
 * @author Enerccio
 *
 */
public class Reference {

	/** Fully qualified reference */
	private String fullReference;
	/** Short key */
	private String key;
	/** Whether it is java reference or plang reference */
	private boolean isJava;

	public Reference(String fullReference, String key, boolean isJava) {
		super();
		this.fullReference = fullReference;
		this.key = key;
		this.isJava = isJava;
	}

	public String getFullReference() {
		return fullReference;
	}

	public void setFullReference(String fullReference) {
		this.fullReference = fullReference;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public boolean isJava() {
		return isJava;
	}

	public void setJava(boolean isJava) {
		this.isJava = isJava;
	}

}
