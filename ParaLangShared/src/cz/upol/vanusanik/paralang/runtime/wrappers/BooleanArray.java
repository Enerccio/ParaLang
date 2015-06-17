package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.io.Serializable;
import java.util.Arrays;

public class BooleanArray extends ArrayBase implements Serializable {
	private static final long serialVersionUID = -9071517535373056290L;

	private boolean[] data;
	public BooleanArray(int n){
		data = new boolean[n];
	}
	
	@Override
	protected String doToString() {
		return "BooleanArray of size " + data.length + " " + Arrays.toString(data);
	}

	public Boolean getUnderlineObject(int idx) {
		return data[idx];
	}

	public void setUnderlineObject(int idx, boolean value) {
		data[idx] = value;
	}

	@Override
	public int get_length() {
		return data.length;
	}
}
