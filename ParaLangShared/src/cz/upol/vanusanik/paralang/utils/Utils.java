package cz.upol.vanusanik.paralang.utils;

import java.lang.reflect.Array;

import org.apache.commons.lang3.StringUtils;

import cz.upol.vanusanik.paralang.compiler.FileDesignator;
import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.Flt;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.plang.types.NoValue;
import cz.upol.vanusanik.paralang.plang.types.Pointer;
import cz.upol.vanusanik.paralang.plang.types.TypeOperations;
import cz.upol.vanusanik.paralang.plang.types.Pointer.PointerMethodIncompatibleException;
import cz.upol.vanusanik.paralang.plang.types.Str;
import cz.upol.vanusanik.paralang.runtime.BaseFloat;
import cz.upol.vanusanik.paralang.runtime.BaseInteger;
import cz.upol.vanusanik.paralang.runtime.BaseNumber;

public class Utils {

	public static <T> T[] pushLeft(T data, T[] array) {
		@SuppressWarnings("unchecked")
		T[] pushed = (T[]) Array.newInstance(array.getClass()
				.getComponentType(), array.length + 1);
		pushed[0] = data;
		System.arraycopy(array, 0, pushed, 1, array.length);
		return pushed;
	}

	public static String packageName(FileDesignator in) {
		if (in.isRealFile()) {
			String path = in.getAbsoluteFile().getParent();
			for (String cp : System.getProperty("java.class.path").split(
					System.getProperty("path.separator")))
				path = path.replace(cp, "");
			return path
					.replaceFirst(
							System.getProperty("file.separator").equals("\\") ? "\\\\"
									: System.getProperty("file.separator"), "")
					.replace(
							System.getProperty("file.separator").equals("\\") ? "\\\\"
									: System.getProperty("file.separator"), ".");
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

	public static PLangObject cast(Object ret, Class<?> retType) {
		if (retType == Integer.class || retType == int.class)
			return new Int(((Integer) ret).longValue());
		if (retType == Long.class || retType == long.class)
			return new Int(((Long) ret).longValue());
		if (retType == Float.class || retType == float.class)
			return new Flt(((Float) ret).floatValue());
		if (retType == Double.class || retType == double.class)
			return new Flt(((Double) ret).floatValue());
		if (retType == String.class)
			return new Str((String) ret);
		if (retType == Void.class)
			return NoValue.NOVALUE;
		if (retType == Boolean.class || retType == boolean.class)
			return BooleanValue.fromBoolean((Boolean) ret);
		return new Pointer(ret);
	}

	public static Object asJavaObject(Class<?> aType, PLangObject datum)
			throws PointerMethodIncompatibleException {

		if (aType == Integer.class || aType == int.class) {
			if (datum instanceof Int)
				return new Integer((int) ((Int) datum).getValue());
			if (datum instanceof BaseInteger) {
				return new Integer(
						(int) ((Int) ((BaseInteger) datum)
								.___getkey(BaseNumber.__valKey)).getValue());
			}
		}

		if (aType == Long.class || aType == long.class) {
			if (datum instanceof Int)
				return new Long(((Int) datum).getValue());
			if (datum instanceof BaseInteger) {
				return new Long(
						((Int) ((BaseInteger) datum)
								.___getkey(BaseNumber.__valKey)).getValue());
			}
		}

		if (aType == Float.class || aType == float.class) {
			if (datum instanceof Flt || datum instanceof BaseFloat)
				return new Float(datum.___getNumber(datum));
		}

		if (aType == Double.class || aType == double.class) {
			if (datum instanceof Flt || datum instanceof BaseFloat)
				return new Double(datum.___getNumber(datum));
		}

		if (aType == Boolean.class || aType == boolean.class) {
			return new Boolean(TypeOperations.convertToBoolean(datum));
		}

		if (aType == String.class) {
			if (datum instanceof Str)
				return ((Str) datum).toString();
		}

		if (datum instanceof Pointer) {
			Class<?> ptype = ((Pointer) datum).getPointer().getClass();
			if (aType.isAssignableFrom(ptype))
				return ((Pointer) datum).getPointer();
		}

		throw new PointerMethodIncompatibleException();
	}

}
