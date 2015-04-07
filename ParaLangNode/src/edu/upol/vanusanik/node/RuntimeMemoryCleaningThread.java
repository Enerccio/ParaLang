package edu.upol.vanusanik.node;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.upol.vanusanik.node.NodeController.RuntimeStoreContainer;

/**
 * RuntimeMemoryCleaningThread is thread that periodically checks runtime caches and removes them if necessary
 * @author Enerccio
 *
 */
public class RuntimeMemoryCleaningThread extends Thread {
	private static final Logger log = Logger.getLogger(RuntimeMemoryCleaningThread.class);
	
	private Object accessor;
	private Set<RuntimeStoreContainer> cs;
	private long timeToCache;
	public RuntimeMemoryCleaningThread(Object accessor, Set<RuntimeStoreContainer> cs, long timeToCache){
		super("Cleaning thread");
		this.accessor = accessor;
		this.cs = cs;
		this.timeToCache = timeToCache;
		setDaemon(true);
		start();
	}
	
	@Override
	public void run(){
		log.info("Starting the garbage cleaning thread");
		
		HashSet<RuntimeStoreContainer> copySet = new HashSet<RuntimeStoreContainer>();
		while (true){
			synchronized (accessor){
				for (RuntimeStoreContainer c : cs){
					if (isOldEnough(c)){
						copySet.add(c);
					}
				}
				
				cs.removeAll(copySet);
			}
			if (copySet.size() > 0){
				copySet.clear();	
				Runtime.getRuntime().gc();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}	
		}
	}

	private boolean isOldEnough(RuntimeStoreContainer c) {
		return System.currentTimeMillis() - c.lastAccessTime >= timeToCache;
	}
	
}
