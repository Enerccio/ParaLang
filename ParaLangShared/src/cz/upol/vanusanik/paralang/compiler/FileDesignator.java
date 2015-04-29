package cz.upol.vanusanik.paralang.compiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * FileDesignator is abstract interface for multiple types of source files (net/disk/archive etc)
 * @author Enerccio
 *
 */
public interface FileDesignator {

	/**
	 * Returns true if the file is actually real file on the disk
	 * @return
	 */
	boolean isRealFile();
	/**
	 * Returns File of the file. Only applicable if is real file. Otherwise returns null.
	 * @return
	 */
	File getAbsoluteFile();
	/**
	 * Returns package name.
	 * @return
	 */
	String getPackageName();
	/**
	 * Returns output directory
	 * @return
	 */
	String getOutputDir();
	/**
	 * Returns the data contained in this FileDesignator
	 * @return
	 */
	InputStream getStream();
	/**
	 * Returns relative path to source
	 * @return
	 */
	String getSource();
	/**
	 * Returns source content in string in utf-8
	 * @return
	 * @throws IOException
	 */
	String getSourceContent() throws IOException;
	
}
