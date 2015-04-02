package cz.upol.vanusanik.paralang.runtime;

import cz.upol.vanusanik.paralang.plang.PLangObject;

public class PLException extends Exception {
	private static final long serialVersionUID = 2030787167158305381L;

	private RuntimeException e;
	public PLException(RuntimeException e) {
		this.e = e;
	}
	
	@Override
	public String getMessage() {
		if (e instanceof PLClass){
			PLClass c = (PLClass)e;
			PLangObject runnable = c.__getkey(BaseException.__messageGetter);
			if (runnable != null){
				PLangObject str = PLRuntime.getRuntime().run(runnable, c);
				return str.toString(str);
			}
		}
		return e.getMessage();
	}
	
	@Override
	public synchronized Throwable getCause() {
		return e;
	}
	
	
}
