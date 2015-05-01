package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.io.Serializable;

import cz.upol.vanusanik.paralang.plang.PLangObject;

public class Matrix extends ObjectBase implements Serializable {
	private static final long serialVersionUID = -4648854139163042398L;
	private PLangObject[][] data;

	public Matrix(int n, int m) {
		data = new PLangObject[n][m];
	}

	@Override
	protected String doToString() {
		return "";
	}

	public PLangObject get(int i, int j) {
		return data[i][j];
	}

	public void set(int i, int j, PLangObject val) {
		data[i][j] = val;
	}

}
