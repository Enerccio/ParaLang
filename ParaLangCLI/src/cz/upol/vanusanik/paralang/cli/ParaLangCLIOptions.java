package cz.upol.vanusanik.paralang.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

public class ParaLangCLIOptions {

	@Parameter(names = {"-sd", "--source-directory"}, description="Directory where are source files that will be compiled. Default is cwd")
	public File sourcesDirectory = new File(System.getProperty("user.dir"));
	
	@Parameter(required = true, arity = 2, description = "starting module and starting function")
	public List<String> starters = new ArrayList<String>();
	
	@Parameter(names = {"-ia", "--init-args"}, description = "You can specify number only parameters that will be applied to your starting function as arguments in this as string enclosed by \"\".")
	public String initialFuncArgs = "";
	
}
