package cz.upol.vanusanik.paralang.compiler;

public class Reference {

	private String fullReference;
	private String key;
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
