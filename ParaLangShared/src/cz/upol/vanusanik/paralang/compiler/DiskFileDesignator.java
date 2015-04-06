package cz.upol.vanusanik.paralang.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;

public class DiskFileDesignator implements FileDesignator {

	private File in;
	public DiskFileDesignator(File in){
		this.in = in;
	}
	
	@Override
	public boolean isRealFile() {
		return true;
	}

	@Override
	public File getAbsoluteFile() {
		return in.getAbsoluteFile();
	}

	@Override
	public String getPackageName() {
		return null;
	}

	@Override
	public String getOutputDir() {
		return in.getParent();
	}

	@Override
	public InputStream getStream() {
		try {
			return new FileInputStream(in);
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	@Override
	public String getSource() {
		String path = in.getAbsolutePath();
		for (String cp : System.getProperty("java.class.path").split(System.getProperty("path.separator")))
			path = path.replace(cp, "");
		return path.replaceFirst(System.getProperty("file.separator").equals("\\") ? "\\\\" : System.getProperty("file.separator"), "");
	}

	@Override
	public String getSourceContent() throws IOException {
		return FileUtils.readFileToString(in, "utf-8");
	}

}
