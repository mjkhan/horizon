/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.data;

import java.util.function.Supplier;

import horizon.base.Assert;

/**Generic value object that has an object's fields and values in key-value pairs.<br />
 * In accessing fields' values, you can use case-insensitive names.
 */
public class DataObject extends StringMap<Object> {
	private static final long serialVersionUID = 1L;

	/**Creates a new DataObject.*/
	public DataObject() {
		caseSensitiveKey(false);
	}

	/**See {@link Assert#equals(Object, Object)}. */
	protected static boolean equals(Object lv, Object rv) {
		return Assert.equals(lv, rv);
	}

	/**See {@link Assert#isEmpty(Object)}.*/
	protected static boolean isEmpty(Object obj) {
		return Assert.isEmpty(obj);
	}

	/**See {@link Assert#ifEmpty(Object, Supplier)}. */
	protected static <T> T ifEmpty(T t, Supplier<T> nt) {
		return Assert.ifEmpty(t, nt);
	}

	/**See {@link Assert#notEmpty(Object, String)}. */
	protected static <T> T notEmpty(T t, String name) {
		return Assert.notEmpty(t, name);
	}

	/**Returns the named field's value cast to a String.
	 * @param key a field's name
	 * @return the named field's value
	 * @throws ClassCastException
	 */
	public String string(String key) {
		return Convert.toString(get(key));
	}

	/**Returns the named field's value cast to a Number.
	 * @param key a field's name
	 * @return
	 * <ul><li>the value cast to Number</li>
	 * 	   <li>Integer(0) if the value is null</li>
	 * </ul>
	 * @throws ClassCastException
	 */
	public Number number(String key) {
		return Convert.toNumber(get(key));
	}

	/**Returns the named field's value converted to a boolean value.
	 * @param key a field's name
	 * @return
	 * <ul><li>the value converted to a boolean value</li>
	 * 	   <li>false if the value is null</li>
	 * </ul>
	 */
	public boolean bool(String key) {
		return Convert.toBoolean(get(key));
	}
}