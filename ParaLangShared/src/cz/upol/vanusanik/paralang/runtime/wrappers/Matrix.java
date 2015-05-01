package cz.upol.vanusanik.paralang.runtime.wrappers;

import java.io.Serializable;

import cz.upol.vanusanik.paralang.plang.PLangObject;

/**
 * Java matrix object wrapped by plang system library
 * 
 * @author Enerccio
 *
 */
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

	/**
	 * Get data from matrix at i, j
	 * 
	 * @param i
	 * @param j
	 * @return
	 */
	public PLangObject get(int i, int j) {
		return data[i][j];
	}

	/**
	 * Set data at i, j
	 * 
	 * @param i
	 * @param j
	 * @param val
	 */
	public void set(int i, int j, PLangObject val) {
		data[i][j] = val;
	}

}
