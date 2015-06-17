package cz.upol.vanusanik.paralang.plang.types;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.apache.commons.lang3.StringUtils;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.runtime.BaseInteger;
import cz.upol.vanusanik.paralang.runtime.BaseNumber;
import cz.upol.vanusanik.paralang.runtime.PLClass;
import cz.upol.vanusanik.paralang.runtime.PLRuntime;

/**
 * Static class providing operator logic. Only allows static access
 * 
 * @author Enerccio
 *
 */
public final class TypeOperations {

	private TypeOperations() {

	}

	/**
	 * Operator enum
	 * 
	 * @author Enerccio
	 *
	 */
	public enum Operator {
		PLUS("_plus"), MINUS("_minus"), MUL("_mul"), DIV("_div"), MOD(
				"_mod"), LSHIFT("_left_shift"), RSHIFT("_right_shift"), RUSHIFT(
				"_right_ushift"), BITOR("_bit_or"), BITAND("_bit_and"), BITXOR(
				"_bit_xor"),

		EQ("_eq"), NEQ("_neq"), LESS("_less"), MORE("_more"), LEQ(
				"_less_eq"), MEQ("_more_eq"),

		LPLUSPLUS("_lplusplus"), LMINUSMINUS("_lminusminus"), UPLUS("_uplus"), UMINUS(
				"_uminus"), ULOGNEG("_ulogneg"), UBINNEG("_ubinneg");

		Operator(String cm) {
			classMethod = cm;
		}

		public final String classMethod;

	}

	/**
	 * Converts PLangObject into boolean
	 * 
	 * @param object
	 * @return
	 */
	public static boolean convertToBoolean(PLangObject object) {
		if (object == null)
			throw new NullPointerException();

		if (object == NoValue.NOVALUE || object == BooleanValue.FALSE)
			return false;

		return true;
	}

	/**
	 * Invokedynamic bootstrap method for binary operators
	 * 
	 * @param callerClass
	 * @param dynMethodName
	 * @param dynMethodType
	 * @return
	 * @throws Throwable
	 */
	public static CallSite binopbootstrap(MethodHandles.Lookup callerClass,
			String dynMethodName, MethodType dynMethodType) throws Throwable {

		MethodHandle mh = callerClass.findStatic(TypeOperations.class,
				dynMethodName, dynMethodType);
		return new ConstantCallSite(mh);
	}

	/**
	 * Invokedynamic bootstrap method for unary operators
	 * 
	 * @param callerClass
	 * @param dynMethodName
	 * @param dynMethodType
	 * @return
	 * @throws Throwable
	 */
	public static CallSite unopbootstrap(MethodHandles.Lookup callerClass,
			String dynMethodName, MethodType dynMethodType) throws Throwable {

		MethodHandle mh = callerClass.findStatic(TypeOperations.class,
				dynMethodName, dynMethodType);
		return new ConstantCallSite(mh);
	}

	// Rest of these methods are methods providing operations. These are then
	// linked via bootstrap methods.

	public static PLangObject plus(PLangObject a, PLangObject b) {
		if (a.___getType() == PlangObjectType.STRING) {
			return new Str(a.toString(a) + b.toString(b));
		}
		if (b.___getType() == PlangObjectType.STRING) {
			return new Str(a.toString(a) + b.toString(b));
		}

		if (a.___getType() == PlangObjectType.CLASS) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.PLUS.classMethod, false),
					(PLClass) a, b);
		}

		if (a instanceof Int
				&& ((b instanceof Int) || (b instanceof BaseInteger))) {
			long va = ((Int) a).value;
			long vb;

			if (b instanceof Int)
				vb = ((Int) b).value;
			else
				vb = ((Int) ((BaseInteger) b).___getkey(BaseNumber.__valKey, false)).value;

			long result = va + vb;
			return new Int(result);
		} else {
			Float va = a.___getNumber(a);
			Float vb = b.___getNumber(b);

			if (va == null || vb == null) {
				throw new RuntimeException("One of the arguments was NoValue");
			}

			Float result = va + vb;
			return new Flt(result);
		}
	}

	public static PLangObject minus(PLangObject a, PLangObject b) {
		if (a.___getType() == PlangObjectType.CLASS) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.MINUS.classMethod, false),
					(PLClass) a, b);
		}
		if (a instanceof Int
				&& ((b instanceof Int) || (b instanceof BaseInteger))) {
			long va = ((Int) a).value;
			long vb;

			if (b instanceof Int)
				vb = ((Int) b).value;
			else
				vb = ((Int) ((BaseInteger) b).___getkey(BaseNumber.__valKey, false)).value;

			long result = va - vb;
			return new Int(result);
		} else {
			Float va = a.___getNumber(a);
			Float vb = b.___getNumber(b);

			if (va == null || vb == null) {
				throw new RuntimeException("One of the arguments was NoValue");
			}

			Float result = va - vb;
			return new Flt(result);
		}
	}

	public static PLangObject mul(PLangObject a, PLangObject b) {
		if (a.___getType() == PlangObjectType.STRING
				&& b.___getType() == PlangObjectType.INTEGER) {
			return new Str(StringUtils.repeat(a.toString(a),
					(int) ((Int) b).value));
		}
		if (a.___getType() == PlangObjectType.CLASS) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.MUL.classMethod, false),
					(PLClass) a, b);
		}
		if (a instanceof Int
				&& ((b instanceof Int) || (b instanceof BaseInteger))) {
			long va = ((Int) a).value;
			long vb;

			if (b instanceof Int)
				vb = ((Int) b).value;
			else
				vb = ((Int) ((BaseInteger) b).___getkey(BaseNumber.__valKey, false)).value;

			long result = va * vb;
			return new Int(result);
		} else {
			Float va = a.___getNumber(a);
			Float vb = b.___getNumber(b);

			if (va == null || vb == null) {
				throw new RuntimeException("One of the arguments was NoValue");
			}

			Float result = va * vb;
			return new Flt(result);
		}
	}

	public static PLangObject div(PLangObject a, PLangObject b) {
		if (a.___getType() == PlangObjectType.CLASS) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.DIV.classMethod, false),
					(PLClass) a, b);
		}
		if (a instanceof Int
				&& ((b instanceof Int) || (b instanceof BaseInteger))) {
			long va = ((Int) a).value;
			long vb;

			if (b instanceof Int)
				vb = ((Int) b).value;
			else
				vb = ((Int) ((BaseInteger) b).___getkey(BaseNumber.__valKey, false)).value;

			long result = va / vb;
			return new Int(result);
		} else {
			Float va = a.___getNumber(a);
			Float vb = b.___getNumber(b);

			if (va == null || vb == null) {
				throw new RuntimeException("One of the arguments was NoValue");
			}

			Float result = va / vb;
			return new Flt(result);
		}
	}

	public static PLangObject mod(PLangObject a, PLangObject b) {
		if (a.___getType() == PlangObjectType.STRING
				&& b.___getType() == PlangObjectType.INTEGER) {
			int radix = (int) ((Int) b).value;
			if (radix < 1 || radix > 16)
				throw new RuntimeException(
						"Incorrect radix for string % int operation.");
			return new Int(Integer.parseInt(a.toString(a), radix));
		}
		if (a.___getType() == PlangObjectType.CLASS) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.MOD.classMethod, false),
					(PLClass) a, b);
		}
		if (a instanceof Int
				&& ((b instanceof Int) || (b instanceof BaseInteger))) {
			long va = ((Int) a).value;
			long vb;

			if (b instanceof Int)
				vb = ((Int) b).value;
			else
				vb = ((Int) ((BaseInteger) b).___getkey(BaseNumber.__valKey, false)).value;

			long result = va % vb;
			return new Int(result);
		} else {
			Float va = a.___getNumber(a);
			Float vb = b.___getNumber(b);

			if (va == null || vb == null) {
				throw new RuntimeException("One of the arguments was NoValue");
			}

			Float result = va % vb;
			return new Flt(result);
		}
	}

	public static PLangObject lshift(PLangObject a, PLangObject b) {
		if (a.___getType() == PlangObjectType.STRING
				&& b.___getType() == PlangObjectType.INTEGER) {
			String val = a.toString(a);
			int len = val.length();
			int lsa = (int) ((Int) b).value;

			if (lsa >= len)
				return new Str("");
			else
				return new Str(val.substring(lsa, len));
		}
		if (a.___getType() == PlangObjectType.CLASS) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.LSHIFT.classMethod, false),
					(PLClass) a, b);
		}
		if (a instanceof Int
				&& ((b instanceof Int) || (b instanceof BaseInteger))) {
			long va = ((Int) a).value;
			long vb;

			if (b instanceof Int)
				vb = ((Int) b).value;
			else
				vb = ((Int) ((BaseInteger) b).___getkey(BaseNumber.__valKey, false)).value;

			long result = va << vb;
			return new Int(result);
		} else {
			Float va = a.___getNumber(a);
			Float vb = b.___getNumber(b);

			if (va == null || vb == null) {
				throw new RuntimeException("One of the arguments was NoValue");
			}

			Float result = (float) (va.longValue() << vb.longValue());
			return new Flt(result);
		}
	}

	public static PLangObject rshift(PLangObject a, PLangObject b) {
		if (a.___getType() == PlangObjectType.STRING
				&& b.___getType() == PlangObjectType.INTEGER) {
			String val = a.toString(a);
			int len = val.length();
			int rsa = (int) ((Int) b).value;

			if (rsa >= len)
				return new Str("");
			else
				return new Str(val.substring(0, len - rsa));
		}
		if (a.___getType() == PlangObjectType.CLASS) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.RSHIFT.classMethod, false),
					(PLClass) a, b);
		}

		if (a instanceof Int
				&& ((b instanceof Int) || (b instanceof BaseInteger))) {
			long va = ((Int) a).value;
			long vb;

			if (b instanceof Int)
				vb = ((Int) b).value;
			else
				vb = ((Int) ((BaseInteger) b).___getkey(BaseNumber.__valKey, false)).value;

			long result = va >> vb;
			return new Int(result);
		} else {
			Float va = a.___getNumber(a);
			Float vb = b.___getNumber(b);

			if (va == null || vb == null) {
				throw new RuntimeException("One of the arguments was NoValue");
			}

			Float result = (float) (va.longValue() >> vb.longValue());
			return new Flt(result);
		}
	}

	public static PLangObject rushift(PLangObject a, PLangObject b) {
		if (a.___getType() == PlangObjectType.STRING
				&& b.___getType() == PlangObjectType.INTEGER) {
			String val = a.toString(a);
			int len = val.length();
			int rsh = (int) ((Int) b).value;

			StringBuilder sb = new StringBuilder();

			if (rsh < 0) {
				rsh = Math.abs(rsh);
				for (int i = 0; i < len; i++) {
					char c = val.charAt((i + rsh) % len);
					sb.append(c);
				}
			} else {
				for (int i = 0; i < len; i++) {
					char c = val.charAt(rsh);
					rsh = (rsh + 1) % len;
					sb.append(c);
				}
			}

			return new Str(sb.toString());
		}
		if (a.___getType() == PlangObjectType.CLASS) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.RUSHIFT.classMethod, false),
					(PLClass) a, b);
		}

		if (a instanceof Int
				&& ((b instanceof Int) || (b instanceof BaseInteger))) {
			long va = ((Int) a).value;
			long vb;

			if (b instanceof Int)
				vb = ((Int) b).value;
			else
				vb = ((Int) ((BaseInteger) b).___getkey(BaseNumber.__valKey, false)).value;

			long result = va >>> vb;
			return new Int(result);
		} else {
			Float va = a.___getNumber(a);
			Float vb = b.___getNumber(b);

			if (va == null || vb == null) {
				throw new RuntimeException("One of the arguments was NoValue");
			}

			Float result = (float) (va.longValue() >>> vb.longValue());
			return new Flt(result);
		}
	}

	public static PLangObject bitor(PLangObject a, PLangObject b) {
		if (a.___getType() == PlangObjectType.CLASS) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.BITOR.classMethod, false),
					(PLClass) a, b);
		}

		if (a instanceof Int
				&& ((b instanceof Int) || (b instanceof BaseInteger))) {
			long va = ((Int) a).value;
			long vb;

			if (b instanceof Int)
				vb = ((Int) b).value;
			else
				vb = ((Int) ((BaseInteger) b).___getkey(BaseNumber.__valKey, false)).value;

			long result = va | vb;
			return new Int(result);
		} else {
			Float va = a.___getNumber(a);
			Float vb = b.___getNumber(b);

			if (va == null || vb == null) {
				throw new RuntimeException("One of the arguments was NoValue");
			}

			Float result = (float) (va.longValue() | vb.longValue());
			return new Flt(result);
		}
	}

	public static PLangObject bitand(PLangObject a, PLangObject b) {
		if (a.___getType() == PlangObjectType.CLASS) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.BITAND.classMethod, false),
					(PLClass) a, b);
		}

		if (a instanceof Int
				&& ((b instanceof Int) || (b instanceof BaseInteger))) {
			long va = ((Int) a).value;
			long vb;

			if (b instanceof Int)
				vb = ((Int) b).value;
			else
				vb = ((Int) ((BaseInteger) b).___getkey(BaseNumber.__valKey, false)).value;

			long result = va & vb;
			return new Int(result);
		} else {
			Float va = a.___getNumber(a);
			Float vb = b.___getNumber(b);

			if (va == null || vb == null) {
				throw new RuntimeException("One of the arguments was NoValue");
			}

			Float result = (float) (va.longValue() & vb.longValue());
			return new Flt(result);
		}
	}

	public static PLangObject bitxor(PLangObject a, PLangObject b) {
		if (a.___getType() == PlangObjectType.CLASS) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.BITXOR.classMethod, false),
					(PLClass) a, b);
		}

		if (a instanceof Int
				&& ((b instanceof Int) || (b instanceof BaseInteger))) {
			long va = ((Int) a).value;
			long vb;

			if (b instanceof Int)
				vb = ((Int) b).value;
			else
				vb = ((Int) ((BaseInteger) b).___getkey(BaseNumber.__valKey, false)).value;

			long result = va ^ vb;
			return new Int(result);
		} else {
			Float va = a.___getNumber(a);
			Float vb = b.___getNumber(b);

			if (va == null || vb == null) {
				throw new RuntimeException("One of the arguments was NoValue");
			}

			Float result = (float) (va.longValue() ^ vb.longValue());
			return new Flt(result);
		}
	}

	public static PLangObject eq(PLangObject a, PLangObject b) {
		if (a.___getType() == PlangObjectType.CLASS) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.EQ.classMethod, false),
					(PLClass) a, b);
		}

		return BooleanValue.fromBoolean(a.___eq(a, b));
	}

	public static PLangObject neq(PLangObject a, PLangObject b) {
		if (a.___getType() == PlangObjectType.CLASS) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.NEQ.classMethod, false),
					(PLClass) a, b);
		}

		return BooleanValue.fromBoolean(!a.___eq(a, b));
	}

	public static PLangObject less(PLangObject a, PLangObject b) {
		if (a.___getType() == PlangObjectType.STRING
				&& b.___getType() == PlangObjectType.STRING) {
			String sa = a.toString(a);
			String sb = a.toString(b);
			int cmp = sa.compareTo(sb);
			return cmp < 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
		}
		if (a.___getType() == PlangObjectType.CLASS) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.LESS.classMethod, false),
					(PLClass) a, b);
		}

		return BooleanValue.fromBoolean(a.___less(a, b, false));
	}

	public static PLangObject more(PLangObject a, PLangObject b) {
		if (a.___getType() == PlangObjectType.STRING
				&& b.___getType() == PlangObjectType.STRING) {
			String sa = a.toString(a);
			String sb = b.toString(b);
			int cmp = sa.compareTo(sb);
			return cmp > 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
		}
		if (a.___getType() == PlangObjectType.CLASS) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.MORE.classMethod, false),
					(PLClass) a, b);
		}

		return BooleanValue.fromBoolean(a.___more(a, b, false));
	}

	public static PLangObject leq(PLangObject a, PLangObject b) {
		if (a.___getType() == PlangObjectType.STRING
				&& b.___getType() == PlangObjectType.STRING) {
			String sa = a.toString(a);
			String sb = a.toString(b);
			int cmp = sa.compareTo(sb);
			return cmp <= 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
		}
		if (a.___getType() == PlangObjectType.CLASS) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.LEQ.classMethod, false),
					(PLClass) a, b);
		}

		return BooleanValue.fromBoolean(a.___less(a, b, true));
	}

	public static PLangObject meq(PLangObject a, PLangObject b) {
		if (a.___getType() == PlangObjectType.STRING
				&& b.___getType() == PlangObjectType.STRING) {
			String sa = a.toString(a);
			String sb = a.toString(b);
			int cmp = sa.compareTo(sb);
			return cmp >= 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
		}
		if (a.___getType() == PlangObjectType.CLASS) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.MEQ.classMethod, false),
					(PLClass) a, b);
		}

		return BooleanValue.fromBoolean(a.___more(a, b, true));
	}

	public static PLangObject lplusplus(PLangObject a) {
		if (a instanceof PLClass) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.LPLUSPLUS.classMethod, false),
					(PLClass) a);
		}

		if (a instanceof Int) {
			long v = ((Int) a).value;
			long add = 1;
			long res = v + add;
			return new Int(res);
		}

		Float v = a.___getNumber(a);
		if (v == null) {
			throw new RuntimeException("NoValue");
		}
		Float add = 1.0f;
		Float res = v + add;
		return new Flt(res);
	}

	public static PLangObject lminusminus(PLangObject a) {
		if (a instanceof PLClass) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.LMINUSMINUS.classMethod, false),
					(PLClass) a);
		}

		if (a instanceof Int) {
			long v = ((Int) a).value;
			long add = 1;
			long res = v - add;
			return new Int(res);
		}

		Float v = a.___getNumber(a);
		if (v == null) {
			throw new RuntimeException("NoValue");
		}
		Float add = 1.0f;
		Float res = v - add;
		return new Flt(res);
	}

	public static PLangObject uplus(PLangObject a) {
		if (a instanceof PLClass) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.UPLUS.classMethod, false),
					(PLClass) a);
		}

		if (a instanceof Int) {
			long v = ((Int) a).value;
			long res = +v;
			return new Int(res);
		}

		Float v = a.___getNumber(a);
		if (v == null) {
			throw new RuntimeException("NoValue");
		}
		Float res = +v;
		return new Flt(res);
	}

	public static PLangObject uminus(PLangObject a) {
		if (a instanceof PLClass) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.UMINUS.classMethod, false),
					(PLClass) a);
		}

		if (a instanceof Int) {
			long v = ((Int) a).value;
			long res = -v;
			return new Int(res);
		}

		Float v = a.___getNumber(a);
		if (v == null) {
			throw new RuntimeException("NoValue");
		}
		Float res = -v;
		return new Flt(res);
	}

	public static PLangObject ulneg(PLangObject a) {
		if (a instanceof PLClass) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.ULOGNEG.classMethod, false),
					(PLClass) a);
		}
		return BooleanValue.fromBoolean(!BooleanValue.toBoolean(a));
	}

	public static PLangObject ubneg(PLangObject a) {
		if (a instanceof PLClass) {
			return PLRuntime.getRuntime().run(
					((PLClass) a).___getkey(Operator.UBINNEG.classMethod, false),
					(PLClass) a);
		}

		if (a instanceof Int) {
			long v = ((Int) a).value;
			long res = ~v;
			return new Int(res);
		}

		Float v = a.___getNumber(a);
		if (v == null) {
			throw new RuntimeException("NoValue");
		}
		Float res = (float) ~v.longValue();
		return new Flt(res);
	}
}
