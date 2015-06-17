package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.io.Serializable;
import java.util.Arrays;

public class IntArray extends ArrayBase implements Serializable {
	private static final long serialVersionUID = -9071517535373056290L;

	private long[] data;
	public IntArray(int n){
		data = new long[n];
	}
	
	@Override
	protected String doToString() {
		return "IntArray of size " + data.length + " " + Arrays.toString(data);
	}

	public long getUnderlineObject(int idx) {
		return data[idx];
	}

	public void setUnderlineObject(int idx, long value) {
		data[idx] = value;
	}

	@Override
	public int get_length() {
		return data.length;
	}
}
