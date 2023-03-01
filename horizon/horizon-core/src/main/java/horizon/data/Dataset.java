/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**Convenience class for a DataList of {@link DataObject}s.<br />
 * The Dataset may be either complete or partial.
 */
public class Dataset extends DataList<DataObject> {
	private static final long serialVersionUID = 1L;

	/**Changes the names of the DataObjects' fields to camel case if they contained the underscore('_') character.
	 * @return the Dataset
	 */
	public Dataset underscoredToCamelCase(boolean camelCase) {
		if (!isEmpty() && camelCase) {
			Set<Map.Entry<String, String>> keymap = get(0).keySet().stream().collect(
					Collectors.toMap(k -> k, v -> camelCase(v), (k1, k2) -> k1, LinkedHashMap::new)
				).entrySet();

			forEach(row ->
				keymap.forEach(entry -> row.put(entry.getValue(), row.remove(entry.getKey())))
			);
		}
		return this;
	}

	private static String camelCase(String str) {
		StringBuilder buf = new StringBuilder();
		for (String token: str.split("_")) {
			if (buf.length() < 1)
				buf.append(token.toLowerCase());
			else
				buf.append(token.substring(0, 1).toUpperCase() + token.substring(1).toLowerCase());
		}
		return buf.toString();
	}
}