package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.io.PrintStream;
import java.io.Serializable;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.NoValue;

public class PrintStreamWrapper implements Serializable {
	
	private static final long serialVersionUID = -7893331337659396685L;
	private transient PrintStream stream;
	private boolean isOutStream;
	
	public PrintStreamWrapper(){
	}
	
	public PrintStreamWrapper(PLangObject... args){
		if (args.length != 1) 
			throw new RuntimeException("Wrong number of parameters, expected 1, got " + args.length);
		
		PLangObject arg0 = args[0];
		if (arg0.getType() != PlangObjectType.BOOLEAN)
			throw new RuntimeException("Argument 0 must be of type BOOLEAN!");
		isOutStream = !(arg0 == BooleanValue.FALSE);
		this.stream = isOutStream ? System.out : System.err;
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws Exception {
		stream.defaultReadObject();
		this.stream = isOutStream ? System.out : System.err;
	}
	
	public PLangObject print(PLangObject... args){
		if (args.length > 1) 
			throw new RuntimeException("Wrong number of parameters, expected 0..1, got " + args.length);
		
		if (args.length == 1){
			PLangObject arg0 = args[0];
			this.stream.print(arg0);
			this.stream.flush();
			return arg0;
		} else {
			this.stream.print("");
			this.stream.flush();
			return NoValue.NOVALUE;
		}
	}
	
	public PLangObject println(PLangObject... args){
		PLangObject arg = print(args);
		this.stream.println();
		return arg;
	}

}
