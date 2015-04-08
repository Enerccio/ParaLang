package cz.upol.vanusanik.paralang.compiler;

/**
 * Stacker used to get free local variables. Will return max number of locals used.
 * @author Enerccio
 *
 */
public class AutoIntStacker {
	
	private int currentValue;
	private int maxValue;
	
	public AutoIntStacker(int startingValue){
		currentValue = maxValue = startingValue;
	}
	
	/**
	 * Acquires the free unused local variable id
	 * @return
	 */
	public int acquire(){
		int ret = currentValue;
		if (currentValue == maxValue)
			++maxValue;
		++currentValue;
		return ret;
	}
	
	/**
	 * Releases the highest used variable into unused set
	 */
	public void release(){
		--currentValue;
	}

	/**
	 * @return maximum used local variables since creation of AutoIntStacker
	 */
	public int getMax() {
		return maxValue;
	}
}
