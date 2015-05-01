/**
 * Copyright(C) 2013 Patrik Dufresne Service Logiciel <info@patrikdufresne.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.patrikdufresne.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * This data structure allow to store pair of data.
 * 
 * @author Patrik Dufresne
 * 
 * @param <K>
 * @param <V>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class AbstractBidiMultiMap<K, V> extends AbstractMap<K, V>
		implements BidiMultiMap<K, V> {

	/**
	 * KeySet implementation.
	 */
	protected class KeySet extends AbstractSet<K> {

		@Override
		public void clear() {
			AbstractBidiMultiMap.this.clear();
		}

		@Override
		public boolean contains(Object key) {
			return AbstractBidiMultiMap.this.containsKey(key);
		}

		@Override
		public Iterator<K> iterator() {
			return AbstractBidiMultiMap.this.createKeySetIterator();
		}

		@Override
		public boolean remove(Object key) {
			return AbstractBidiMultiMap.this.remove(key) != null;
		}

		@Override
		public int size() {
			return AbstractBidiMultiMap.this.keySize();
		}
	}

	/**
	 * Keys implementation.
	 */
	protected class KeySetForValue extends AbstractSet<K> {

		protected V value;

		public KeySetForValue(V value) {
			this.value = value;
		}

		@Override
		public boolean add(K key) {
			return put(key, this.value) != null;
		}

		@Override
		public void clear() {
			removeValue(this.value);
		}

		@Override
		public boolean contains(Object key) {
			return containsEntry(key, this.value);
		}

		@Override
		public Iterator<K> iterator() {
			return createKeySetIteratorForValue(this.value);
		}

		@Override
		public boolean remove(Object key) {
			return removeEntry(key, this.value);
		}

		@Override
		public int size() {
			int size = 0;
			Iterator<K> iter = iterator();
			while (iter.hasNext()) {
				iter.next();
				size++;
			}
			return size;
		}
	}

	/**
	 * Values implementation.
	 */
	protected class ValueSet extends AbstractSet<V> {

		@Override
		public void clear() {
			AbstractBidiMultiMap.this.clear();
		}

		@Override
		public boolean contains(Object value) {
			return AbstractBidiMultiMap.this.containsValue(value);
		}

		@Override
		public Iterator<V> iterator() {
			return AbstractBidiMultiMap.this.createValueIterator();
		}

		@Override
		public boolean remove(Object value) {
			return AbstractBidiMultiMap.this.removeValue(value) != null;
		}

		@Override
		public int size() {
			return AbstractBidiMultiMap.this.valueSize();
		}

	}

	/**
	 * Values implementation.
	 */
	protected class ValueSetForKey extends AbstractSet<V> {

		protected K key;

		public ValueSetForKey(K key) {
			this.key = key;
		}

		@Override
		public boolean add(V value) {
			return put(this.key, value) != null;
		}

		@Override
		public void clear() {
			AbstractBidiMultiMap.this.remove(this.key);
		}

		@Override
		public boolean contains(Object value) {
			return AbstractBidiMultiMap.this.containsEntry(this.key, value);
		}

		@Override
		public Iterator<V> iterator() {
			return createValueSetIteratorForKey(this.key);
		}

		@Override
		public boolean remove(Object value) {
			return removeEntry(this.key, value);
		}

		@Override
		public int size() {
			int size = 0;
			Iterator<V> iter = iterator();
			while (iter.hasNext()) {
				iter.next();
				size++;
			}
			return size;
		}

	}

	/** Key set */
	protected transient KeySet keySet;

	/** Values */
	protected transient ValueSet valueSet;

	/**
	 * Default constructor.
	 */
	protected AbstractBidiMultiMap() {
		super();
	}

	/**
	 * Checks whether the map contains the specified entry.
	 * 
	 * @param key
	 *            the key to search for
	 * @param value
	 *            the value to search for
	 * @return true if the map contains the entry
	 */
	@Override
	public boolean containsEntry(Object key, Object value) {
		return entrySet().contains(new SimpleEntry(key, value));
	}

	// -----------------------------------------------------------------------
	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * This implementation iterates over <tt>entrySet()</tt> searching for an
	 * entry with the specified key. If such an entry is found, <tt>true</tt> is
	 * returned. If the iteration terminates without finding such an entry,
	 * <tt>false</tt> is returned. Note that this implementation requires linear
	 * time in the size of the map; many implementations will override this
	 * method.
	 * 
	 * @param key
	 *            the key to search for
	 * @return true if the map contains the key
	 */
	@Override
	public boolean containsKey(Object key) {
		Iterator<Map.Entry<K, V>> i = entrySet().iterator();
		if (key == null) {
			while (i.hasNext()) {
				Entry<K, V> e = i.next();
				if (e.getKey() == null)
					return true;
			}
		} else {
			while (i.hasNext()) {
				Entry<K, V> e = i.next();
				if (key.equals(e.getKey()))
					return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * This implementation iterates over <tt>entrySet()</tt> searching for an
	 * entry with the specified value. If such an entry is found, <tt>true</tt>
	 * is returned. If the iteration terminates without finding such an entry,
	 * <tt>false</tt> is returned. Note that this implementation requires linear
	 * time in the size of the map.
	 * 
	 * @param value
	 *            the value to search for
	 * @return true if the map contains the value
	 */
	@Override
	public boolean containsValue(Object value) {
		Iterator<Map.Entry<K, V>> i = entrySet().iterator();
		if (value == null) {
			while (i.hasNext()) {
				Entry<K, V> e = i.next();
				if (e.getValue() == null)
					return true;
			}
		} else {
			while (i.hasNext()) {
				Entry<K, V> e = i.next();
				if (value.equals(e.getValue()))
					return true;
			}
		}
		return false;
	}

	/**
	 * Subclasses must implement this function to provide the set of key.
	 * 
	 * @return
	 */
	protected abstract Iterator<K> createKeySetIterator();

	/**
	 * This implementation create a filter iterator using the entry set.
	 * 
	 * @param value
	 * @return
	 */
	protected Iterator<K> createKeySetIteratorForValue(final V value) {
		final Iterator<Entry<K, V>> iterator = entrySet().iterator();
		return new Iterator<K>() {
			private K nextObject;
			private boolean nextObjectSet = false;

			public boolean hasNext() {
				if (nextObjectSet) {
					return true;
				}
				return setNextObject();
			}

			public K next() {
				if (!nextObjectSet) {
					if (!setNextObject()) {
						throw new NoSuchElementException();
					}
				}
				nextObjectSet = false;
				return nextObject;
			}

			public void remove() {
				if (nextObjectSet) {
					throw new IllegalStateException("remove() cannot be called");
				}
				iterator.remove();
			}

			private boolean setNextObject() {
				while (iterator.hasNext()) {
					Entry<K, V> object = iterator.next();
					if (value == null) {
						if (object.getValue() == null) {
							nextObject = object.getKey();
							nextObjectSet = true;
							return true;
						}
					} else {
						if (value.equals(object.getValue())) {
							nextObject = object.getKey();
							nextObjectSet = true;
							return true;
						}
					}
				}
				return false;
			}
		};
	}

	/**
	 * Subclasses must implement this function to provide the values.
	 * 
	 * @return
	 */
	protected abstract Iterator<V> createValueIterator();

	protected Iterator<V> createValueSetIteratorForKey(final K key) {
		final Iterator<Entry<K, V>> iterator = entrySet().iterator();
		return new Iterator<V>() {
			private V nextObject;
			private boolean nextObjectSet = false;

			public boolean hasNext() {
				if (nextObjectSet) {
					return true;
				}
				return setNextObject();
			}

			public V next() {
				if (!nextObjectSet) {
					if (!setNextObject()) {
						throw new NoSuchElementException();
					}
				}
				nextObjectSet = false;
				return nextObject;
			}

			public void remove() {
				if (nextObjectSet) {
					throw new IllegalStateException("remove() cannot be called");
				}
				iterator.remove();
			}

			private boolean setNextObject() {
				while (iterator.hasNext()) {
					Entry<K, V> object = iterator.next();
					if (key == null) {
						if (object.getKey() == null) {
							nextObject = object.getValue();
							nextObjectSet = true;
							return true;
						}
					} else {
						if (key.equals(object.getKey())) {
							nextObject = object.getValue();
							nextObjectSet = true;
							return true;
						}
					}
				}
				return false;
			}
		};
	}

	// -----------------------------------------------------------------------
	/**
	 * Gets the value mapped to the key specified.
	 * 
	 * @param key
	 *            the key
	 * @return return the first value matching the key.
	 */
	@Override
	public V get(Object key) {
		return null;
	}

	/**
	 * Subclasses must implement this function to provide a set of keys.
	 */
	public Set<K> keySet() {
		if (this.keySet == null) {
			this.keySet = new KeySet();
		}
		return this.keySet;
	}

	/**
	 * Gets the keySet view of the map. Changes made to the view affect this
	 * map. To simply iterate through the keys, use {@link #mapIterator()}.
	 * 
	 * @param value
	 *            the value
	 * @return the keySet view
	 */
	@Override
	public Set<K> keySet(V value) {
		return new KeySetForValue(value);
	}

	// -----------------------------------------------------------------------

	/**
	 * This implementation iterates over <tt>createKeySetIterator()</tt>
	 * counting the number of keys. Note that this implementation requires
	 * linear time in the size of the key.
	 * 
	 * @return
	 */
	protected int keySize() {
		int count = 0;
		Iterator<K> i = createKeySetIterator();
		while (i.hasNext()) {
			i.next();
			count++;
		}
		return count;
	}

	// -----------------------------------------------------------------------

	public V put(K key, V value) {
		entrySet().add(new SimpleEntry(key, value));
		return null;
	}

	@Override
	public V remove(Object key) {
		Set<V> set = removeKey(key);
		if (set == null) {
			return null;
		}
		return set.iterator().next();
	}

	/**
	 * Removes the specified mapping from this map.
	 * 
	 * @param key
	 *            the mapping to remove
	 * @param value
	 *            the mapping to remove
	 * @return False if key not in map
	 */
	@Override
	public boolean removeEntry(Object key, Object value) {
		return entrySet().remove(new SimpleEntry(key, value));
	}

	// -----------------------------------------------------------------------

	/**
	 * Removes the specified mapping from this map.
	 * 
	 * @param key
	 *            the mapping to remove
	 * @return the values mapped to the removed key, null if key not in map
	 */
	@Override
	public Set<V> removeKey(Object key) {
		Iterator<Entry<K, V>> i = entrySet().iterator();
		Set<V> set = null;
		if (key == null) {
			while (i.hasNext()) {
				Entry<K, V> e = i.next();
				if (e.getKey() == null) {
					if (set == null) {
						set = new HashSet<V>();
					}
					set.add(e.getValue());
					i.remove();
				}
			}
		} else {
			while (i.hasNext()) {
				Entry<K, V> e = i.next();
				if (key.equals(e.getKey())) {
					if (set == null) {
						set = new HashSet<V>();
					}
					set.add(e.getValue());
					i.remove();
				}
			}
		}
		return set;
	}

	/**
	 * This implementation iterate on the entry set to remove any entry with the
	 * value specified.
	 * 
	 * @param value
	 *            the value to the removed from this map
	 * @return the keys mapped to the removed value, null if value not in map
	 */
	@Override
	public Set<K> removeValue(Object value) {
		Iterator<Entry<K, V>> i = entrySet().iterator();
		Set<K> set = null;
		if (value == null) {
			while (i.hasNext()) {
				Entry<K, V> e = i.next();
				if (e.getValue() == null) {
					if (set == null) {
						set = new HashSet<K>();
					}
					set.add(e.getKey());
					i.remove();
				}
			}
		} else {
			while (i.hasNext()) {
				Entry<K, V> e = i.next();
				if (value.equals(e.getValue())) {
					if (set == null) {
						set = new HashSet<K>();
					}
					set.add(e.getKey());
					i.remove();
				}
			}
		}
		return set;
	}

	// -----------------------------------------------------------------------
	/**
	 * Gets the values view of the map. Changes made to the view affect this
	 * map.
	 * 
	 * @return the values view
	 */
	@Override
	public Collection<V> values() {
		return valueSet();
	}

	@Override
	public Set<V> valueSet() {
		if (this.valueSet == null) {
			this.valueSet = new ValueSet();
		}
		return this.valueSet;
	}

	/**
	 * Gets the values view of the map for the key specified. Changes made to
	 * the view affect this map.
	 * 
	 * @param key
	 * @return
	 */
	@Override
	public Set<V> valueSet(K key) {
		return new ValueSetForKey(key);
	}

	/**
	 * This implementation iterates over <tt>createValueSetIterator()</tt>
	 * counting the number of keys. Note that this implementation requires
	 * linear time in the size of the key.
	 * 
	 * @return
	 */
	protected int valueSize() {
		int count = 0;
		Iterator<V> i = createValueIterator();
		while (i.hasNext()) {
			i.next();
			count++;
		}
		return count;
	}

}
