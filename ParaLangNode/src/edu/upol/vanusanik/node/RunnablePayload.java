package edu.upol.vanusanik.node;

public abstract class RunnablePayload<T> implements Runnable {

	private volatile boolean finished = false;
	private T result;
	
	public boolean hasFinished(){
		return finished;
	}
	
	public T getResult(){
		return result;
	}
	
	@Override
	public void run(){
		result = executePayload();
		finished = true;
	}

	protected abstract T executePayload();
	
}

 