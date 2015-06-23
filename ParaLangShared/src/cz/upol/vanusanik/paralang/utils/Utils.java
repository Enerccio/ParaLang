package cz.upol.vanusanik.paralang.utils;

import java.lang.reflect.Array;
import java.net.Socket;

import org.apache.commons.lang3.StringUtils;

import com.eclipsesource.json.JsonObject;

import cz.upol.vanusanik.paralang.compiler.FileDesignator;
import cz.upol.vanusanik.paralang.connector.Protocol;
import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.Flt;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.plang.types.NoValue;
import cz.upol.vanusanik.paralang.plang.types.Pointer;
import cz.upol.vanusanik.paralang.plang.types.Pointer.PointerMethodIncompatibleException;
import cz.upol.vanusanik.paralang.plang.types.Str;
import cz.upol.vanusanik.paralang.plang.types.TypeOperations;
import cz.upol.vanusanik.paralang.runtime.BaseNumber;
import cz.upol.vanusanik.paralang.runtime.PLClass;
import cz.upol.vanusanik.paralang.runtime.PLRuntime;

/**
 * Utility class, providing utility functions
 * 
 * @author Enerccio
 *
 */
public class Utils {

	/**
	 * Adds object to the left of the array, ie x + [a, b] => [x, a, b]
	 * 
	 * @param data
	 *            what to be pushed
	 * @param array
	 *            array to be pushed in
	 * @return new array with object pushed in left
	 */
	public static <T> T[] pushLeft(T data, T[] array) {
		@SuppressWarnings("unchecked")
		T[] pushed = (T[]) Array.newInstance(array.getClass()
				.getComponentType(), array.length + 1);
		pushed[0] = data;
		System.arraycopy(array, 0, pushed, 1, array.length);
		return pushed;
	}

	/**
	 * Returns package name from file designator
	 * 
	 * @param in
	 * @return
	 */
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

	/**
	 * Converts dot class name into slashed class name. java.lang.String ->
	 * java/lang/String
	 * 
	 * @param fqName
	 *            fully qualified name
	 * @return slashified version
	 */
	public static String slashify(String fqName) {
		return StringUtils.replace(fqName, ".", "/");
	}

	/**
	 * Removes "" from string, "aaa" -> aaa
	 * 
	 * @param text
	 *            to be removed
	 * @return
	 */
	public static String removeStringQuotes(String text) {
		if (text.equals(""))
			return "";
		return StringUtils.removeEnd(StringUtils.removeStart(text, "\""), "\"");
	}

	/**
	 * Stupid method but required by method handle
	 * 
	 * @param args
	 *            args to be casted
	 * @return passed args
	 */
	public static Object[] asObjectArray(PLangObject[] args) {
		return args;
	}

	/**
	 * Casts object of the retType as PLangObject
	 * 
	 * @param ret
	 *            object
	 * @param retType
	 *            type of that object
	 * @return PLangObject version of it
	 */
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

	/**
	 * Converts PLangObject datum into java type of aType.
	 * 
	 * @param aType
	 *            type required
	 * @param datum
	 *            PLangObject data
	 * @return java object
	 * @throws PointerMethodIncompatibleException
	 *             if the conversion is not possible
	 */
	public static Object asJavaObject(Class<?> aType, PLangObject datum)
			throws PointerMethodIncompatibleException {

		if (aType == Integer.class || aType == int.class) {
			if (datum instanceof Int)
				return new Integer((int) ((Int) datum).getValue());
			if (PLRuntime.getRuntime().checkInstanceOf(datum, "System.Integer") == BooleanValue.TRUE) {
				return new Integer(
						(int) ((Int) ((PLClass) datum)
								.___getkey(BaseNumber.__valKey, false)).getValue());
			}
		}

		if (aType == Long.class || aType == long.class) {
			if (datum instanceof Int)
				return new Long(((Int) datum).getValue());
			if (PLRuntime.getRuntime().checkInstanceOf(datum, "System.Integer") == BooleanValue.TRUE) {
				return new Long(
						((Int) ((PLClass) datum)
								.___getkey(BaseNumber.__valKey, false)).getValue());
			}
		}

		if (aType == Float.class || aType == float.class) {
			if (datum instanceof Flt)
				return new Float(datum.___getNumber(datum));
			if ((PLRuntime.getRuntime().checkInstanceOf(datum, "System.Float") == BooleanValue.TRUE))
				return new Float((float) ((Flt) ((PLClass) datum).___getkey(BaseNumber.__valKey, false)).getValue());
		}

		if (aType == Double.class || aType == double.class) {
			if (datum instanceof Flt)
				return new Double(datum.___getNumber(datum));
			if ((PLRuntime.getRuntime().checkInstanceOf(datum, "System.Float") == BooleanValue.TRUE))
				return new Double((double) ((Flt) ((PLClass) datum).___getkey(BaseNumber.__valKey, false)).getValue());
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

	public static StackTraceElement[] removeStackElements(StackTraceElement[] stackTrace, int no) {
		StackTraceElement[] ed = new StackTraceElement[stackTrace.length - no];
		for (int i=no; i<stackTrace.length; i++)
			ed[i-no] = stackTrace[i]; 
		return ed;
	}

	/**
	 * Sends the error back to the client.
	 * 
	 * This is not used to send back exception, instead it is used to send when
	 * error happened prior to the running of the runtime.
	 * 
	 * @param s
	 * @param payload
	 * @param ecode
	 * @param dmesg
	 * @throws Exception
	 */
	public static void sendError(Socket s, JsonObject payload, long ecode,
			String dmesg) throws Exception {
		payload.add("payload",
				new JsonObject().add("error", true).add("errorCode", ecode)
						.add("errorDetails", dmesg));
		Protocol.send(s.getOutputStream(), payload);
	}

	public static int asIntegerValue(PLangObject datum) {
		if (datum instanceof Int)
			return new Integer((int) ((Int) datum).getValue());
		if (PLRuntime.getRuntime().checkInstanceOf(datum, "System.Integer") == BooleanValue.TRUE) {
			return new Integer(
					(int) ((Int) ((PLClass) datum)
							.___getkey(BaseNumber.__valKey, false)).getValue());
		}
		throw PLRuntime.getRuntime().newInstance("Utils.IllegalArgumentException", new Str("Incorrect type, must be int or Integer"));
	}

}
