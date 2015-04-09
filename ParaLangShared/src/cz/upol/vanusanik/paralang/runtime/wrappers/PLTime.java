package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.io.Serializable;
import java.util.Date;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.Int;

public class PLTime extends ObjectBase implements Serializable {
	private static final long serialVersionUID = -235077474720284054L;
	private Date date;
	
	public PLTime(){
		
	}
	
	public PLTime(PLangObject... initValue){
		if (initValue[0] instanceof Int){
			date = new Date(((Int)initValue[0]).getValue());
		} else {
			date = new Date();
		}
	}
	
	@Override
	protected String doToString() {
		return date.toString();
	}

	public PLangObject getUnixTime(){
		return new Int(date.getTime());
	}
}
