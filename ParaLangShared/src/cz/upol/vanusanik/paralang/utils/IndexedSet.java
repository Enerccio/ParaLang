package cz.upol.vanusanik.paralang.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

public class IndexedSet<T> {

	public static interface Equalator<X> {
		boolean equal(X x, Object o);

		int hashCode(X x);
	}

	private static class KeyWrapper<X> {
		private X x;
		private Equalator<X> eq;

		public KeyWrapper(X x, Equalator<X> eq) {
			this.x = x;
			this.eq = eq;
		}

		@Override
		public int hashCode() {
			if (eq != null)
				return eq.hashCode(x);
			return x.hashCode();
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object o) {
			if (eq != null)
				return eq.equal(x, ((KeyWrapper<X>) o).x);
			return x.equals(o);
		}
	}

	public IndexedSet() {
		idMap = new DualHashBidiMap<KeyWrapper<T>, Integer>();
	}

	public IndexedSet(Equalator<T> tcomp) {
		eqt = tcomp;
		idMap = new DualHashBidiMap<KeyWrapper<T>, Integer>();
	}

	private Equalator<T> eqt;
	private BidiMap<KeyWrapper<T>, Integer> idMap;
	private int iter = 0;

	public synchronized int add(T o) {
		if (!idMap.containsKey(new KeyWrapper<T>(o, eqt))) {
			idMap.put(new KeyWrapper<T>(o, eqt), iter++);
		}
		return idMap.get(new KeyWrapper<T>(o, eqt));
	}

	public synchronized T[] toIndexedArray(T[] array) {
		List<T> oolist = new ArrayList<T>();
		for (int l = 0; l < iter; l++) {
			KeyWrapper<T> value = idMap.getKey(l);
			oolist.add(value.x);
		}
		return oolist.toArray(array);
	}

}
