package cz.upol.vanusanik.paralang.plang.types;

import cz.upol.vanusanik.paralang.plang.PLangObject;

public class TypeOperations {
	
	private enum Operator {
		PLUS, MINUS, MUL, DIV, MOD, LSHIFT, RSHIFT, RUSHIFT, BITOR, BITAND, BITXOR,
		
		EQ, NEQ, LESS, MORE, LEQ, MEQ
	}

	public static boolean convertToBoolean(PLangObject object){
		if (object == null) throw new NullPointerException();
		
		if (object == NoValue.NOVALUE || object == BooleanValue.FALSE)
			return false;
		
		return true;
	}
	
	public static PLangObject plus(PLangObject a, PLangObject b){
		return operator(a, b, Operator.PLUS);
	}
	public static PLangObject minus(PLangObject a, PLangObject b){
		return operator(a, b, Operator.MINUS);
	}
	public static PLangObject mul(PLangObject a, PLangObject b){
		return operator(a, b, Operator.MUL);
	}
	public static PLangObject div(PLangObject a, PLangObject b){
		return operator(a, b, Operator.DIV);
	}
	public static PLangObject mod(PLangObject a, PLangObject b){
		return operator(a, b, Operator.MOD);
	}
	public static PLangObject lshift(PLangObject a, PLangObject b){
		return operator(a, b, Operator.LSHIFT);
	}
	public static PLangObject rshift(PLangObject a, PLangObject b){
		return operator(a, b, Operator.RSHIFT);
	}
	public static PLangObject rushift(PLangObject a, PLangObject b){
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
		return operator(a, b, Operator.LESS);
	}
	public static PLangObject more(PLangObject a, PLangObject b){
		return operator(a, b, Operator.MORE);
	}
	public static PLangObject leq(PLangObject a, PLangObject b){
		return operator(a, b, Operator.LEQ);
	}
	public static PLangObject meq(PLangObject a, PLangObject b){
		return operator(a, b, Operator.MEQ);
	}

	@SuppressWarnings("incomplete-switch")
	private static PLangObject operator(PLangObject a, PLangObject b,
			Operator o) {
		
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
			Float va = a.getNumber();
			Float vb = b.getNumber();
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
			
			return PLangObject.autocast(result, a, b);
		} 
		case EQ:
		case LEQ:
		case LESS:
		case MEQ:
		case MORE:
		case NEQ:{
			
			Float va = a.getNumber();
			Float vb = b.getNumber();
			boolean result = false;
			
			switch (o){
			case EQ:
				result = a.eq(b);
				break;
			case LEQ:
				result = va <= vb;
				break;
			case LESS:
				result = va < vb;
				break;
			case MEQ:
				result = va >= vb;
				break;
			case MORE:
				result = va > vb;
				break;
			case NEQ:
				result = !a.eq(b);
				break;
			}
			
			return BooleanValue.fromBoolean(result);
		}
		default:
			break;
		
		
		}
		
		return null;
	}
}
