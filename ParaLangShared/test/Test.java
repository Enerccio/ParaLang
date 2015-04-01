import java.io.ByteArrayOutputStream;
import java.io.File;

import cz.upol.vanusanik.paralang.compiler.DiskFileDesignator;
import cz.upol.vanusanik.paralang.compiler.PLCompiler;
import cz.upol.vanusanik.paralang.runtime.PLRuntime;


public class Test {

	public static void main(String[] xx) throws Exception{
		
		File f = new File("bin\\x.plng");
		PLCompiler c = new PLCompiler();
		
		PLRuntime r = new PLRuntime();
		r.setRestricted(false);
		r.setSafeContext(false);
		c.compile(new DiskFileDesignator(f));
		
		r.run("HelloWorldModule", "run");
		
		ByteArrayOutputStream test = new ByteArrayOutputStream();
		
		r.serializeRuntimeContent(test, 0);
		
//		String s = test.toString("utf-8");
//		System.out.println(s);
	}
	
}
