package cz.upol.vanusanik.paralang.cli;

import java.io.File;
import java.io.FileInputStream;

import com.beust.jcommander.JCommander;

import cz.upol.vanusanik.paralang.compiler.DiskFileDesignator;
import cz.upol.vanusanik.paralang.connector.NodeList;
import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.Flt;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.runtime.PLRuntime;

/**
 * Main class for CLI runtime
 * 
 * @author Enerccio
 *
 */
public class ParaLangCLI {

	/**
	 * Entry point
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ParaLangCLIOptions no = new ParaLangCLIOptions();
		new JCommander(no, args);

		run(no);
	}

	private static void run(ParaLangCLIOptions no) throws Exception {
		// Set up ssl/tsl truststores and keystores
		if (no.useSSL){
			System.setProperty("javax.net.ssl.keyStore", no.keystore);
			System.setProperty("javax.net.ssl.trustStore", no.keystore);
			System.setProperty("javax.net.ssl.keyStorePassword", no.keystorepass);
		}
		
		NodeList.setUseSSL(no.useSSL);

		File workingDir = no.sourcesDirectory;
		PLRuntime runtime = new PLRuntime();

		// Load all the .plang files from sources directory into runtime
		for (File f : workingDir.listFiles()) {
			if (f.getName().endsWith(".plang")) {
				runtime.compileSource(new DiskFileDesignator(f));
			}
		}

		String moduleName = no.starters.get(0);
		String methodName = no.starters.get(1);

		// transforms arguments passed into cli into PLangObjects
		PLangObject[] args = loadArgs(no.initialFuncArgs);

		// Process nodes from a file into nodelist
		if (no.nodeListFile != null) {
			FileInputStream fis = new FileInputStream(no.nodeListFile);
			NodeList.loadFile(fis);
			fis.close();
		}

		// Process nodes from cli arguments
		String[] parsedNodes = no.nodes.split(";");
		for (String s : parsedNodes) {
			if (!s.equals("")) {
				String[] datum = s.split(":");
				NodeList.addNode(datum[0], Integer.parseInt(datum[1]));
			}
		}

		// execute code
		runtime.run(moduleName, methodName, args);
	}

	/**
	 * Transforms arguments in cli into PLangObjects
	 * 
	 * @param args
	 * @return
	 */
	private static PLangObject[] loadArgs(String args) {
		String[] initialFuncArgs = args.split(" ");
		PLangObject[] array = new PLangObject[initialFuncArgs.length];

		int iter = 0;
		for (String ia : initialFuncArgs) {
			try {
				int val = Integer.parseInt(ia);
				array[iter++] = new Int(val);
			} catch (NumberFormatException ignores) {
				float val = Float.parseFloat(ia);
				array[iter++] = new Flt(val);
			}
		}

		return array;
	}

}
