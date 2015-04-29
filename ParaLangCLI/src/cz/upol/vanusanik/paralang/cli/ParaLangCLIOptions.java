package cz.upol.vanusanik.paralang.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

/**
 * Options for CLI version of the program
 * @author Enerccio
 *
 */
public class ParaLangCLIOptions {

	@Parameter(names = {"-sd", "--source-directory"}, description="Directory where are source files that will be compiled. Default is cwd")
	public File sourcesDirectory = new File(System.getProperty("user.dir"));
	
	@Parameter(required = true, arity = 2, description = "starting module and starting function")
	public List<String> starters = new ArrayList<String>();
	
	@Parameter(names = {"-ia", "--init-args"}, description = "You can specify number only parameters that will be applied to your starting function as arguments in this as string enclosed by \"\".")
	public String initialFuncArgs = "";
	
	@Parameter(names = {"-n", "--nodes"}, description = "List of nodes in <address>:<port>; format in single \"\" string")
	public String nodes = "";
	
	@Parameter(names = {"-nl", "--node-list"}, description = "File containing node list")
	public File nodeListFile;
	
	@Parameter(required = true, names = {"-ks", "--keystore"}, description="Keystore for SSL conecction.  Must be in same dir as running process.")
	public String keystore;
	
	@Parameter(required = true, names = {"-ksp", "--keystore-password"}, description="Keystore password for SSL conecction.")
	public String keystorepass;
	
}
