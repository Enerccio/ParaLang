package edu.upol.vanusanik.node;

import com.beust.jcommander.Parameter;

public class NodeOptions {

	@Parameter(required = true, names = {"-p", "--port"}, description="Port on which this node will listen")
	public Integer portNumber;
	
	@Parameter(names = {"-tc", "--thread-count"}, description="Number of available node threads, default is number of cpu hardware threads available")
	public Integer threadCount = Runtime.getRuntime().availableProcessors();
}
