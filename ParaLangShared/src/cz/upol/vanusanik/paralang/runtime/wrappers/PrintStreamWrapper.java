package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.io.PrintStream;
import java.io.Serializable;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;

public class PrintStreamWrapper extends ObjectBase implements Serializable {
	
	private static final long serialVersionUID = -7893331337659396685L;
	private transient PrintStream stream;
	private boolean isOutStream;
	
	public PrintStreamWrapper(){
	}
	
	public PrintStreamWrapper(PLangObject... args){
		PLangObject arg0 = args[0];
		if (arg0.__sys_m_getType() != PlangObjectType.BOOLEAN)
			throw new RuntimeException("Argument 0 must be of type BOOLEAN!");
		isOutStream = !(arg0 == BooleanValue.FALSE);
		this.stream = isOutStream ? System.out : System.err;
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws Exception {
		stream.defaultReadObject();
		this.stream = isOutStream ? System.out : System.err;
	}
	
	public PLangObject print(PLangObject arg0){
		this.stream.print(arg0);
		this.stream.flush();
		return arg0;
	}
	
	public PLangObject println(PLangObject arg){
		print(arg);
		this.stream.println();
		return arg;
	}

	@Override
	protected String doToString() {
		return stream.toString();
	}

}
