import java.io.File;

import cz.upol.vanusanik.paralang.compiler.DiskFileDesignator;
import cz.upol.vanusanik.paralang.compiler.PLCompiler;
import cz.upol.vanusanik.paralang.runtime.PLRuntime;


public class Test {

	public static void main(String[] xx){
		
		
		File f = new File("bin\\x.plng");
		PLCompiler c = new PLCompiler();
		
		new PLRuntime();
		c.compile(new DiskFileDesignator(f));
		
	}
	
}
