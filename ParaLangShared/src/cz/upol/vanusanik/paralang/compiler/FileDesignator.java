package cz.upol.vanusanik.paralang.compiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface FileDesignator {

	boolean isRealFile();
	File getAbsoluteFile();
	String getPackageName();
	String getOutputDir();
	InputStream getStream();
	String getSource();
	String getSourceContent() throws IOException;
	
}
