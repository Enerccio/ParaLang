package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.io.Serializable;

public class ArrayArray extends ArrayBase implements Serializable {
	private static final long serialVersionUID = -9071517535373056290L;

	private ArrayBase[] data;
	public ArrayArray(int n){
		data = new ArrayBase[n];
	}
	
	@Override
	protected String doToString() {
		return "ArrayArray of size " + data.length;
	}

	public ArrayBase getUnderlineObject(int idx) {
		return data[idx];
	}

	public void setUnderlineObject(int idx, ArrayBase value) {
		data[idx] = value;
	}

	@Override
	public int get_length() {
		return data.length;
	}
}
