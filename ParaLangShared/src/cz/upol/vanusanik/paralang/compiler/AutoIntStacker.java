package cz.upol.vanusanik.paralang.compiler;

public class AutoIntStacker {
	
	private int currentValue;
	private int maxValue;
	
	public AutoIntStacker(int startingValue){
		currentValue = maxValue = startingValue;
	}
	
	public synchronized int acquire(){
		int ret = currentValue;
		if (currentValue == maxValue)
			++maxValue;
		++currentValue;
		return ret;
	}
	
	public synchronized void release(){
		--currentValue;
	}

	public int getMax() {
		return maxValue;
	}
}
