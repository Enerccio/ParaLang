package cz.upol.vanusanik.paralang.utils;

import java.lang.reflect.Array;

import org.apache.commons.lang3.StringUtils;

import cz.upol.vanusanik.paralang.compiler.FileDesignator;
import cz.upol.vanusanik.paralang.plang.PLangObject;

public class Utils {

	public static <T> T[] pushLeft(T data, T[] array) {
		@SuppressWarnings("unchecked")
		T[] pushed = (T[])Array.newInstance(array.getClass().getComponentType(), array.length + 1);
	    pushed[0] = data;
	    System.arraycopy(array, 0, pushed, 1, array.length);
	    return pushed;
	}

	public static String packageName(FileDesignator in) {
		if (in.isRealFile()){
			String path = in.getAbsoluteFile().getParent();
			for (String cp : System.getProperty("java.class.path").split(System.getProperty("path.separator")))
				path = path.replace(cp, "");
			return path.replaceFirst(System.getProperty("file.separator").equals("\\") ? "\\\\" : System.getProperty("file.separator"), "")
					.replace(System.getProperty("file.separator").equals("\\") ? "\\\\" : System.getProperty("file.separator"), ".");
		} else
			return in.getPackageName();

	}

	public static String slashify(String fqName) {
		return StringUtils.replace(fqName, ".", "/");
	}

	public static String removeStringQuotes(String text) {
		if (text.equals(""))
			return "";
		return StringUtils.removeEnd(StringUtils.removeStart(text, "\""), "\"");
	}

	public static Object[] asObjectArray(PLangObject[] args) {
		return args;
	}

}
