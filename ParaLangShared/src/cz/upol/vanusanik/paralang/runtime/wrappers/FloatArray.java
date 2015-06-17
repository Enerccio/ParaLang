package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.io.Serializable;

public class FloatArray extends ArrayBase implements Serializable {
	private static final long serialVersionUID = -9071517535373056290L;

	private float[] data;
	public FloatArray(int n){
		data = new float[n];
	}
	
	@Override
	protected String doToString() {
		return "FloatArray of size " + data.length;
	}

	public float getUnderlineObject(int idx) {
		return data[idx];
	}

	public void setUnderlineObject(int idx, float value) {
		data[idx] = value;
	}
	
	public void setUnderlineObject(int idx, long value) {
		data[idx] = value;
	}

	@Override
	public int get_length() {
		return data.length;
	}
}
