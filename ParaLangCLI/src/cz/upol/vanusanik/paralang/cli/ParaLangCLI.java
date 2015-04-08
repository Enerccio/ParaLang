package cz.upol.vanusanik.paralang.cli;

import java.io.File;

import com.beust.jcommander.JCommander;

import cz.upol.vanusanik.paralang.compiler.DiskFileDesignator;
import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.Flt;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.runtime.PLRuntime;

public class ParaLangCLI {

	public static void main(String[] args) throws Exception {
		ParaLangCLIOptions no = new ParaLangCLIOptions();
		new JCommander(no, args);
		
		run(no);
	}

	private static void run(ParaLangCLIOptions no) throws Exception {
		File workingDir = no.sourcesDirectory;
		PLRuntime runtime = new PLRuntime();
		
		for (File f : workingDir.listFiles()){
			if (f.getName().endsWith(".plang")){
				runtime.compileSource(new DiskFileDesignator(f));
			}
		}
		
		String moduleName = no.starters.get(0);
		String methodName = no.starters.get(1);
		
		PLangObject[] args = loadArgs(no.initialFuncArgs);
		
		runtime.run(moduleName, methodName, args);
	}

	private static PLangObject[] loadArgs(String args) {
		String[] initialFuncArgs = args.split(" ");
		PLangObject[] array = new PLangObject[initialFuncArgs.length];
		
		int iter = 0;
		for (String ia : initialFuncArgs){
			try {
				int val = Integer.parseInt(ia);
				array[iter++] = new Int(val);
			} catch (NumberFormatException ignores){
				float val = Float.parseFloat(ia);
				array[iter++] = new Flt(val);
			}
		}
		
		return array;
	}

}
