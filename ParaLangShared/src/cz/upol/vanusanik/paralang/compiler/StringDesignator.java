package cz.upol.vanusanik.paralang.compiler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class StringDesignator implements FileDesignator {

	private String classDef;
	private String source;
	
	@Override
	public boolean isRealFile() {
		return false;
	}

	@Override
	public File getAbsoluteFile() {
		return null;
	}

	@Override
	public String getPackageName() {
		return "";
	}

	@Override
	public String getOutputDir() {
		return null;
	}

	@Override
	public InputStream getStream() {
		try {
			return new ByteArrayInputStream(getClassDef().getBytes("utf-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String getSource() {
		return source;
	}
	
	public void setSource(String source){
		this.source = source;
	}

	@Override
	public String getSourceContent() throws IOException {
		return getClassDef();
	}

	public String getClassDef() {
		return classDef;
	}

	public void setClassDef(String classDef) {
		this.classDef = classDef;
	}

}
