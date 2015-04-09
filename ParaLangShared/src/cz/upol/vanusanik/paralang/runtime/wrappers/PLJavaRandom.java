package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.io.Serializable;
import java.util.Date;
import java.util.Random;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.plang.types.NoValue;

public class PLJavaRandom extends ObjectBase implements Serializable {
	private static final long serialVersionUID = -4580778294459270129L;
	private Random random;
	
	public PLJavaRandom(){
		
	}
	
	public PLJavaRandom(PLangObject... args){
		long lv;
		if (args[0] == NoValue.NOVALUE){
			lv = new Date().getTime();
		} else {
			if (args[0] instanceof Int){
				lv = ((Int)args[0]).getValue();
			} else {
				lv = args[0].___getNumber(args[0]).longValue();
			}
		}
		random = new Random(lv);
	}
	
	@Override
	protected String doToString() {
		return random.toString();
	}


	public PLangObject nextInt(Int max){
		return new Int(random.nextLong() % max.getValue());
	}
}
