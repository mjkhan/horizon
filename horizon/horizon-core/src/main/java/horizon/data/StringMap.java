/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**A map of String keys and V values augmented with convenience methods.<br />
 * You can have a StringMap use {@link #caseSensitiveKey(boolean) either case-sensitive keys or case-insensitive keys} in associating values.<br />
 * It uses, by default, case-sensitive keys.
 * @param <V> a value type
 */
public class StringMap<V> extends LinkedHashMap<String, V> {
	private static final long serialVersionUID = 1L;
	private boolean caseSensitiveKey = true;

	/**Returns whether the StringMap uses case-sensitive keys.
	 * @return
	 * <ul><li>true when the StringMap uses case-sensitive keys</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 */
	public boolean caseSensitiveKey() {
		return caseSensitiveKey;
	}

	/**Sets the StringMap to use either case-sensitive keys or case-insensitive keys.
	 * @param sensitive
	 * <ul><li>true to use case-sensitive keys</li>
	 * 	   <li>false to use case-insensitive keys</li>
	 * </ul>
	 * @return this StringMap
	 */
	public <T extends StringMap<V>> T  caseSensitiveKey(boolean sensitive) {
		caseSensitiveKey = sensitive;
		return self();
	}

	private String findKey(Object obj) {
		String s = (String)obj;
		if (caseSensitiveKey)
			return s;
		for (String key: keySet())
			if (key.equalsIgnoreCase(s))
				return key;
		return s;
	}

	@Override
	public boolean containsKey(Object key) {
		return super.containsKey(key) || super.containsKey(findKey(key));
	}

	@Override
	public V get(Object key) {
		V v = super.get(key);
		return v != null ? v : super.get(findKey(key));
	}

	@Override
	public V getOrDefault(Object key, V defaultValue) {
		return super.getOrDefault(findKey(key), defaultValue);
	}

	@Override
	public V put(String key, V value) {
		return super.put(findKey(key), value);
	}

	/**Associates the value with the key in the map.
	 * @param key	key with which the value is to be associated
	 * @param value value to be associated with the key
	 * @return this StringMap
	 */
	public <T extends StringMap<V>> T set(String key, V value) {
		put(key, value);
		return self();
	}

	/**See {@link #putAll(Map)}
	 * @return this StringMap
	 */
	public <T extends StringMap<V>> T  setAll(Map<? extends String, ? extends V> map) {
		putAll(map);
		return self();
	}

	@Override
	public V remove(Object key) {
		V v = super.remove(key);
		return v != null ? v : super.remove(findKey(key));
	}

	@Override
	public V computeIfAbsent(String key, Function<? super String, ? extends V> mappingFunction) {
		return super.computeIfAbsent(findKey(key), mappingFunction);
	}

	@Override
	public V computeIfPresent(String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
		return super.computeIfPresent(findKey(key), remappingFunction);
	}

	@Override
	public V merge(String key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		return super.merge(findKey(key), value, remappingFunction);
	}

	@Override
	public V putIfAbsent(String key, V value) {
		return super.putIfAbsent(findKey(key), value);
	}

	/**Sets the value if there is no association with the key.
	 * @param key
	 * @param value
	 * @return this StringMap
	 */
	public <T extends StringMap<V>> T setIfAbsent(String key, V value) {
		putIfAbsent(key, value);
		return self();
	}

	/**Returns this StringMap cast to T.
	 * @param <T> a StringMap type
	 * @return this StringMap cast to T
	 */
	@SuppressWarnings("unchecked")
	protected <T extends StringMap<V>> T self() {
		Object obj = this;
		return (T)obj;
	}
}