package cz.upol.vanusanik.paralang.runtime;

import java.io.Serializable;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.plang.types.Str;
import cz.upol.vanusanik.paralang.plang.types.TypeOperations;
import cz.upol.vanusanik.paralang.plang.types.TypeOperations.Operator;

public abstract class BaseNumber extends PLClass implements Serializable {
	private static final long serialVersionUID = -499503904346523234L;
	protected static final String __valKey = "__val";
	
	@Override
	protected void ___init_internal_datafields() {
		this.___restrictedOverride = true;
		
		___setkey(BaseClass.__superKey, new BaseClass());
		___setkey("init", new FunctionWrapper("__init", this, true));
		___setkey("__str", new FunctionWrapper("__str_base", this, true));
		
		___setkey(Operator.EQ.classMethod, new FunctionWrapper("__eq_base", this, true));
		___setkey(Operator.NEQ.classMethod, new FunctionWrapper("__neq_base", this, true));
		___setkey(Operator.BITAND.classMethod, new FunctionWrapper("__bitand_base", this, true));
		___setkey(Operator.BITOR.classMethod, new FunctionWrapper("__bitor_base", this, true));
		___setkey(Operator.BITXOR.classMethod, new FunctionWrapper("__bitxor_base", this, true));
		___setkey(Operator.DIV.classMethod, new FunctionWrapper("__div_base", this, true));
		___setkey(Operator.LEQ.classMethod, new FunctionWrapper("__leq_base", this, true));
		___setkey(Operator.LESS.classMethod, new FunctionWrapper("__less_base", this, true));
		___setkey(Operator.LSHIFT.classMethod, new FunctionWrapper("__lshift_base", this, true));
		___setkey(Operator.MEQ.classMethod, new FunctionWrapper("__meq_base", this, true));
		___setkey(Operator.MINUS.classMethod, new FunctionWrapper("__minus_base", this, true));
		___setkey(Operator.MOD.classMethod, new FunctionWrapper("__mod_base", this, true));
		___setkey(Operator.MORE.classMethod, new FunctionWrapper("__more_base", this, true));
		___setkey(Operator.MUL.classMethod, new FunctionWrapper("__mul_base", this, true));
		___setkey(Operator.PLUS.classMethod, new FunctionWrapper("__plus_base", this, true));
		___setkey(Operator.RSHIFT.classMethod, new FunctionWrapper("__rshift_base", this, true));
		___setkey(Operator.RUSHIFT.classMethod, new FunctionWrapper("__rushift_base", this, true));
		
		___setkey(__valKey, new Int(0));
		this.___restrictedOverride = false;
	}
	
	public PLangObject __init(PLangObject self, PLangObject iv){
		return __init_superclass(self, iv);
	}
	
	public PLangObject __str_base(PLangObject self){
		return new Str(((PLClass)self).___getkey(__valKey).toString());
	}
	
	public abstract PLangObject __init_superclass(PLangObject self, PLangObject iv);
	
	public PLangObject __eq_base(PLangObject self, PLangObject other){
		return TypeOperations.eq(((PLClass)self).___getkey(__valKey), other);
	}
	
	public PLangObject __neq_base(PLangObject self,  PLangObject other){
		return TypeOperations.neq(((PLClass)self).___getkey(__valKey), other);
	}
	
	public PLangObject __bitand_base(PLangObject self, PLangObject other){
		return TypeOperations.bitand(((PLClass)self).___getkey(__valKey), other);
	}
	
	public PLangObject __bitor_base(PLangObject self, PLangObject other){
		return TypeOperations.bitor(((PLClass)self).___getkey(__valKey), other);
	}
	
	public PLangObject __bitxor_base(PLangObject self, PLangObject other){
		return TypeOperations.bitxor(((PLClass)self).___getkey(__valKey), other);
	}
	
	public PLangObject __div_base(PLangObject self, PLangObject other){
		return TypeOperations.div(((PLClass)self).___getkey(__valKey), other);
	}
	
	public PLangObject __leq_base(PLangObject self, PLangObject other){
		return TypeOperations.leq(((PLClass)self).___getkey(__valKey), other);
	}
	
	public PLangObject __less_base(PLangObject self, PLangObject other){
		return TypeOperations.less(((PLClass)self).___getkey(__valKey), other);
	}
	
	public PLangObject __lshift_base(PLangObject self, PLangObject other){
		return TypeOperations.lshift(((PLClass)self).___getkey(__valKey), other);
	}
	
	public PLangObject __meq_base(PLangObject self, PLangObject other){
		return TypeOperations.meq(((PLClass)self).___getkey(__valKey), other);
	}
	
	public PLangObject __minus_base(PLangObject self, PLangObject other){
		return TypeOperations.minus(((PLClass)self).___getkey(__valKey), other);
	}
	
	public PLangObject __mod_base(PLangObject self, PLangObject other){
		return TypeOperations.mod(((PLClass)self).___getkey(__valKey), other);
	}
	
	public PLangObject __more_base(PLangObject self, PLangObject other){
		return TypeOperations.more(((PLClass)self).___getkey(__valKey), other);
	}
	
	public PLangObject __mul_base(PLangObject self, PLangObject other){
		return TypeOperations.mul(((PLClass)self).___getkey(__valKey), other);
	}
	
	public PLangObject __plus_base(PLangObject self, PLangObject other){
		return TypeOperations.plus(((PLClass)self).___getkey(__valKey), other);
	}
	
	public PLangObject __rshift_base(PLangObject self, PLangObject other){
		return TypeOperations.rshift(((PLClass)self).___getkey(__valKey), other);
	}
	
	public PLangObject __rushift_base(PLangObject self, PLangObject other){
		return TypeOperations.rushift(((PLClass)self).___getkey(__valKey), other);
	}
	
	@Override
	public boolean ___less(PLangObject self, PLangObject other, boolean equals) {
		if (equals){
			return BooleanValue.toBoolean(TypeOperations.leq(((PLClass)self).___getkey(__valKey), other));
		} else {
			return BooleanValue.toBoolean(TypeOperations.less(((PLClass)self).___getkey(__valKey), other));
		}
	}
	
	@Override
	public boolean ___more(PLangObject self, PLangObject other, boolean equals) {
		if (equals){
			return BooleanValue.toBoolean(TypeOperations.meq(((PLClass)self).___getkey(__valKey), other));
		} else {
			return BooleanValue.toBoolean(TypeOperations.more(((PLClass)self).___getkey(__valKey), other));
		}
	}
	
	@Override
	public boolean ___isNumber(){
		return true;
	}
	
	@Override
	public Float ___getNumber(PLangObject self){
		PLangObject val = ___getkey(__valKey);
		return val.___getNumber(val);
	}
}
