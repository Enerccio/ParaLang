package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.io.Serializable;

public class StringArray extends ArrayBase implements Serializable {
	private static final long serialVersionUID = -9071517535373056290L;

	private String[] data;
	public StringArray(int n){
		data = new String[n];
	}
	
	@Override
	protected String doToString() {
		return "FloatArray of size " + data.length;
	}

	public String getUnderlineObject(int idx) {
		return data[idx];
	}

	public void setUnderlineObject(int idx, String value) {
		data[idx] = value;
	}

	@Override
	public int get_length() {
		return data.length;
	}

}
