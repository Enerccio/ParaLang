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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Defines a map that holds a many-to-many relation between a key and it's
 * value.
 * <p>
 * A MultiMap is a Map with slightly different semantics. Putting a key-value
 * into the map will add a new entry to the map.
 * <p>
 * For example:
 * <p>
 * 
 * <pre>
 * BidiMultiMap&lt;String, String&gt; map = new BidiMultiHashMap&lt;String, String&gt;();
 * map.put(&quot;a&quot;, &quot;1&quot;);
 * map.put(&quot;a&quot;, &quot;2&quot;);
 * map.put(&quot;a&quot;, &quot;3&quot;);
 * map.put(&quot;b&quot;, &quot;1&quot;);
 * map.put(&quot;b&quot;, &quot;2&quot;);
 * int size = map.size();
 * Set&lt;K&gt; keys = mhm.keySet();
 * Set&lt;V&gt; values = mhm.valueSet();
 * </pre>
 * 
 * <ul>
 * <li><code>size</code> will be 5.</li>
 * <li><code>keys</code> will be a set containing "a", "b".</li>
 * <li><code>values</code> will be a set containing "1", "2", "3".</li>
 * </ul>
 * 
 * @author Patrik Dufresne
 * 
 * @param <K>
 * @param <V>
 */
public interface BidiMultiMap<K, V> extends Map<K, V> {

    /**
     * The empty map (immutable).
     */
    @SuppressWarnings("rawtypes")
	public static final BidiMultiMap EMPTY = new BidiMultiMap() {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public Object get(Object key) {
            return null;
        }

        @Override
        public Object put(Object key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            // Nothing to do
        }

        @Override
        public Set keySet() {
            return Collections.EMPTY_SET;
        }

        @Override
        public Collection values() {
            return Collections.EMPTY_SET;
        }

        @Override
        public Set entrySet() {
            return Collections.EMPTY_SET;
        }

        @Override
        public boolean containsEntry(Object key, Object value) {
            return false;
        }

        @Override
        public Set keySet(Object value) {
            return Collections.EMPTY_SET;
        }

        @Override
        public boolean removeEntry(Object key, Object value) {
            return false;
        }

        @Override
        public Set removeKey(Object key) {
            return null;
        }

        @Override
        public Set removeValue(Object value) {
            return null;
        }

        @Override
        public Set valueSet() {
            return Collections.EMPTY_SET;
        }

        @Override
        public Set valueSet(Object key) {
            return Collections.EMPTY_SET;
        }

    };

    /**
     * Checks whether the map contains the specified entry.
     * 
     * @param key
     *            the key to search for
     * @param value
     *            the value to search for
     * @return true if the map contains the entry
     */
    boolean containsEntry(Object key, Object value);

    /**
     * Gets the keySet view of the map. Changes made to the view affect this
     * map. To simply iterate through the keys, use {@link #mapIterator()}.
     * 
     * @param value
     *            the value
     * @return the keySet view
     */
    Set<K> keySet(V value);

    /**
     * Removes the specified mapping from this map.
     * 
     * @param key
     *            the mapping to remove
     * @param value
     *            the mapping to remove
     * @return False if key not in map
     */
    boolean removeEntry(Object key, Object value);

    /**
     * Remove the specified key from this map.
     * 
     * @param key
     *            the key to remove
     * @return the values mapped to the removed key, null if key not in map
     */
    Set<V> removeKey(Object key);

    /**
     * Removes the specified value from this map.
     * 
     * @param value
     *            the value to remove
     * @return the keys mapped to the removed value, null if value not in map
     */
    Set<K> removeValue(Object value);

    /**
     * Gets the values view of the map. Changes made to the view affect this
     * map.
     * 
     * @return the values view
     */
    Set<V> valueSet();

    /**
     * Gets the values view of the map for the key specified. Changes made to
     * the view affect this map.
     * 
     * @param key
     * @return
     */
    Set<V> valueSet(K key);

}
