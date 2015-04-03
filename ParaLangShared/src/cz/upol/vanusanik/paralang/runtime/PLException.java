package cz.upol.vanusanik.paralang.runtime;

import java.util.ArrayList;
import java.util.List;

import cz.upol.vanusanik.paralang.plang.PLangObject;

public class PLException extends Exception {
	private static final long serialVersionUID = 2030787167158305381L;

	private RuntimeException e;
	public PLException(RuntimeException e) {
		this.e = e;
		
		List<StackTraceElement> eList = new ArrayList<StackTraceElement>();
		for (StackTraceElement ee : e.getStackTrace()){
			String filename = ee.getFileName();
			if (ee.getMethodName().equals("__init_internal_datafields")) continue;
			if (filename == null) continue;
			if (filename.contains(".plang"))
				eList.add(ee);
		}
		
		setStackTrace(eList.toArray(new StackTraceElement[eList.size()]));
	}
	
	@Override
	public String getMessage() {
		if (e instanceof PLClass){
			PLClass c = (PLClass)e;
			PLangObject runnable = c.__getkey(BaseException.__messageGetter);
			if (runnable != null){
				PLangObject str = PLRuntime.getRuntime().run(runnable, c);
				return str.toString(str);
			} else {
				return c.toString(c);
			}
		}
		return e.getMessage();
	}
	
	@Override
	public synchronized Throwable getCause() {
		return e;
	}
	
	
}
