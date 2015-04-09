package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.io.Serializable;
import java.security.SecureRandom;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.Int;

public class PLSecureRandom extends ObjectBase implements Serializable {
	private static final long serialVersionUID = -4580778294459270128L;
	private SecureRandom random;
	
	public PLSecureRandom(){
		
	}
	
	public PLSecureRandom(PLangObject... args){
		random = new SecureRandom();
	}
	
	@Override
	protected String doToString() {
		return random.toString();
	}


	public PLangObject nextInt(Int max){
		return new Int(random.nextLong() % max.getValue());
	}
}
