package cz.upol.vanusanik.paralang.plang.types;

import org.apache.commons.lang3.StringUtils;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.runtime.PLClass;
import cz.upol.vanusanik.paralang.runtime.PLRuntime;

public class TypeOperations {
	
	public enum Operator {
		PLUS("__plus"), MINUS("__minus"), MUL("__mul"), 
		DIV("__div"), MOD("__mod"), LSHIFT("__left_shift"), 
		RSHIFT("__right_shift"), RUSHIFT("__right_ushift"), 
		BITOR("__bit_or"), BITAND("__bit_and"), BITXOR("__bit_xor"),
		
		EQ("__eq"), NEQ("__neq"), LESS("__less"), MORE("__more"), 
		LEQ("__less_eq"), MEQ("__more_eq"), 
		
		LPLUSPLUS("__lplusplus"), LMINUSMINUS("__lminusminus");
		
		Operator(String cm){
			classMethod = cm;
		}
		
		public final String classMethod;
		
	}

	public static boolean convertToBoolean(PLangObject object){
		if (object == null) throw new NullPointerException();
		
		if (object == NoValue.NOVALUE || object == BooleanValue.FALSE)
			return false;
		
		return true;
	}
	
	public static PLangObject plus(PLangObject a, PLangObject b){
		if (a.___getType() == PlangObjectType.STRING){
			return new Str(a.toString() + b.toString());
		}
		if (b.___getType() == PlangObjectType.STRING){
			return new Str(a.toString() + b.toString());
		}
		return operator(a, b, Operator.PLUS);
	}
	public static PLangObject minus(PLangObject a, PLangObject b){
		return operator(a, b, Operator.MINUS);
	}
	public static PLangObject mul(PLangObject a, PLangObject b){
		if (a.___getType() == PlangObjectType.STRING && b.___getType() == PlangObjectType.INTEGER){
			return new Str(StringUtils.repeat(a.toString(), ((Int)b).value));
		}
		return operator(a, b, Operator.MUL);
	}
	public static PLangObject div(PLangObject a, PLangObject b){
		return operator(a, b, Operator.DIV);
	}
	public static PLangObject mod(PLangObject a, PLangObject b){
		if (a.___getType() == PlangObjectType.STRING && b.___getType() == PlangObjectType.INTEGER){
			int radix = ((Int)b).value;
			if (radix < 1 || radix > 16)
				throw new RuntimeException("Incorrect radix for string % int operation.");
			return new Int(Integer.parseInt(a.toString(), radix));
		}
		return operator(a, b, Operator.MOD);
	}
	public static PLangObject lshift(PLangObject a, PLangObject b){
		if (a.___getType() == PlangObjectType.STRING && b.___getType() == PlangObjectType.INTEGER){
			String val = a.toString();
			int len = val.length();
			int lsa = ((Int)b).value;
			
			if (lsa >= len)
				return new Str("");
			else
				return new Str(val.substring(lsa, len));
		}
		return operator(a, b, Operator.LSHIFT);
	}
	public static PLangObject rshift(PLangObject a, PLangObject b){
		if (a.___getType() == PlangObjectType.STRING && b.___getType() == PlangObjectType.INTEGER){
			String val = a.toString();
			int len = val.length();
			int rsa = ((Int)b).value;
			
			if (rsa >= len)
				return new Str("");
			else
				return new Str(val.substring(0, len-rsa));
		}
		return operator(a, b, Operator.RSHIFT);
	}
	public static PLangObject rushift(PLangObject a, PLangObject b){
		if (a.___getType() == PlangObjectType.STRING && b.___getType() == PlangObjectType.INTEGER){
			String val = a.toString();
			int len = val.length();
			int rsh = ((Int)b).value;
			
			StringBuilder sb = new StringBuilder();
			
			if (rsh < 0){
				rsh = Math.abs(rsh);
				for (int i=0; i<len; i++){
					char c = val.charAt((i + rsh) % len);
					sb.append(c);
				}
			} else {
				for (int i=0; i<len; i++){
					char c = val.charAt(rsh);
					rsh = (rsh + 1) % len; 
					sb.append(c);
				}
			}
			
			
			return new Str(sb.toString());
		}
		return operator(a, b, Operator.RUSHIFT);
	}
	public static PLangObject bitor(PLangObject a, PLangObject b){
		return operator(a, b, Operator.BITOR);
	}
	public static PLangObject bitand(PLangObject a, PLangObject b){
		return operator(a, b, Operator.BITAND);
	}
	public static PLangObject bitxor(PLangObject a, PLangObject b){
		return operator(a, b, Operator.BITXOR);
	}
	
	public static PLangObject eq(PLangObject a, PLangObject b){
		return operator(a, b, Operator.EQ);
	}
	public static PLangObject neq(PLangObject a, PLangObject b){
		return operator(a, b, Operator.NEQ);
	}
	public static PLangObject less(PLangObject a, PLangObject b){
		if (a.___getType() == PlangObjectType.STRING && b.___getType() == PlangObjectType.STRING){
			String sa = a.toString();
			String sb = a.toString();
			int cmp = sa.compareTo(sb);
			return cmp < 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
		}
		return operator(a, b, Operator.LESS);
	}
	public static PLangObject more(PLangObject a, PLangObject b){
		if (a.___getType() == PlangObjectType.STRING && b.___getType() == PlangObjectType.STRING){
			String sa = a.toString();
			String sb = b.toString();
			int cmp = sa.compareTo(sb);
			return cmp > 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
		}
		return operator(a, b, Operator.MORE);
	}
	public static PLangObject leq(PLangObject a, PLangObject b){
		if (a.___getType() == PlangObjectType.STRING && b.___getType() == PlangObjectType.STRING){
			String sa = a.toString();
			String sb = a.toString();
			int cmp = sa.compareTo(sb);
			return cmp <= 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
		}
		return operator(a, b, Operator.LEQ);
	}
	public static PLangObject meq(PLangObject a, PLangObject b){
		if (a.___getType() == PlangObjectType.STRING && b.___getType() == PlangObjectType.STRING){
			String sa = a.toString();
			String sb = a.toString();
			int cmp = sa.compareTo(sb);
			return cmp >= 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
		}
		return operator(a, b, Operator.MEQ);
	}

	@SuppressWarnings("incomplete-switch")
	private static PLangObject operator(PLangObject a, PLangObject b,
			Operator o) {
		
		if (a instanceof PLClass){
			return PLRuntime.getRuntime().run(((PLClass)a).___getkey(o.classMethod), (PLClass)a, b);
		}
		
		switch (o){
		case DIV:
		case MINUS:
		case MUL:
		case PLUS:
		case MOD:
		case LSHIFT:
		case RSHIFT:
		case RUSHIFT:
		case BITOR:
		case BITAND:
		case BITXOR: {
			Float va = a.___getNumber(a);
			Float vb = b.___getNumber(b);
			Float result = 0f;
			
			switch(o){
			case BITAND:
				result = (float) (va.intValue() & vb.intValue());
				break;
			case BITOR:
				result = (float) (va.intValue() | vb.intValue());
				break;
			case BITXOR:
				result = (float) (va.intValue() ^ vb.intValue());
				break;
			case DIV:
				result = va / vb;
				break;
			case LSHIFT:
				result = (float) (va.intValue() << vb.intValue());
				break;
			case MINUS:
				result = va - vb;
				break;
			case MOD:
				result = va % vb;
				break;
			case MUL:
				result = va * vb;
				break;
			case PLUS:
				result = va + vb;
				break;
			case RSHIFT:
				result = (float) (va.intValue() >> vb.intValue());
				break;
			case RUSHIFT:
				result = (float) (va.intValue() >>> vb.intValue());
				break;			
			}
			
			return PLangObject.___autocast(result, a, b);
		} 
		case EQ:
		case LEQ:
		case LESS:
		case MEQ:
		case MORE:
		case NEQ:{
			
			boolean result = false;
			
			switch (o){
			case EQ:
				result = a.___eq(a, b);
				break;
			case LEQ:
				result = a.___less(a, b, true);
				break;
			case LESS:
				result = a.___less(a, b, false);
				break;
			case MEQ:
				result = a.___more(a, b, true);
				break;
			case MORE:
				result = a.___more(a, b, false);
				break;
			case NEQ:
				result = !a.___eq(a, b);
				break;
			}
			
			return BooleanValue.fromBoolean(result);
		}
		default:
			break;
		
		
		}
		
		return null;
	}
	
	public static PLangObject lplusplus(PLangObject a){
		return operator(a, Operator.LPLUSPLUS);
	}
	
	public static PLangObject lminusminus(PLangObject a){
		return operator(a, Operator.LMINUSMINUS);
	}

	private static PLangObject operator(PLangObject a, Operator o) {
		if (a instanceof PLClass){
			return PLRuntime.getRuntime().run(((PLClass)a).___getkey(o.classMethod), (PLClass)a);
		}
		
		Float v = a.___getNumber(a);
		Float add = 1.0f;
		Float res;
		
		switch (o){
		case LPLUSPLUS:
			res = v + add;
			break;
		case LMINUSMINUS:
			res = v - add;
			break;
		default:
			res = -1f;
		}
		
		return PLangObject.___autocast(res, a);
	}
}
