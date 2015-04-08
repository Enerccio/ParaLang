package edu.upol.vanusanik.paralang.node;

import java.io.File;

import com.beust.jcommander.Parameter;

/**
 * Options this console server application accepts.
 * @author Enerccio
 *
 */
public class NodeOptions {

	@Parameter(required = true, names = {"-p", "--port"}, description="Port on which this node will listen")
	public Integer portNumber;
	
	@Parameter(names = {"-tc", "--thread-count"}, description="Number of available node threads, default is number of cpu hardware threads available")
	public Integer threadCount = Runtime.getRuntime().availableProcessors();

	@Parameter(names = {"-cst", "--cache-store-duration"}, description="Amount of time the cache is kept. Default 3600000 (in ms)")
	public Long cacheStoreTime = 3600L;
	
	@Parameter(names = {"-n", "--nodes"}, description = "List of nodes in <address>:<port>; format in single \"\" string")
	public String nodes = "";
	
	@Parameter(names = {"-nl", "--node-list"}, description = "File containing node list")
	public File nodeListFile;
}
