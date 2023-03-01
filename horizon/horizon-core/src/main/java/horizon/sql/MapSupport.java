/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql;

import java.util.HashMap;
import java.util.function.Function;

class MapSupport extends HashMap<String, Object> {
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		Object value = super.get(key);
		return (T)value;
	}

	public <T> T getIfAbsent(String key, Function<? super String, ? extends T> mappingFunction) {
		if (!containsKey(key)) {
			put(key, mappingFunction.apply(key));
		}
		return get(key);
	}

	@SuppressWarnings("unchecked")
	public <T> T remove(String key) {
		Object removed = super.remove(key);
		return (T)removed;
	}
}
