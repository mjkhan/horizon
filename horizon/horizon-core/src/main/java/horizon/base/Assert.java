/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.base;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**Assertion utility
 */
public class Assert {
	private Assert() {}

	/**Returns whether lv and rv are equal.
	 * @param lv value on the left side
	 * @param rv value on the right side
	 * @return
	 * <ul><li>true if lv and rv are equal</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 */
	public static boolean equals(Object lv, Object rv) {
		return lv == rv || lv != null && lv.equals(rv);
	}

	/**Returns whether obj is null, whitespace, an empty collection, an empty array, or an empty map.
	 * @param obj an Object
	 * @return
	 * <ul><li>true if obj is null, white-space, an empty collection, an empty array, or an empty map.</li>
	 * 	   <li>false otherwise.</li>
	 * </ul>
	 */
	public static boolean isEmpty(Object obj) {
		if (obj == null) return true;

		if (obj instanceof String) {
			String str = (String)obj;
			return str.isEmpty() || isWhitespace(str);
		}

		if (obj instanceof CharSequence)
			return ((CharSequence)obj).length() < 1;

		if (obj instanceof Iterable) {
			Iterable<?> objs = (Iterable<?>)obj;
			return !objs.iterator().hasNext();
		}

		if (obj.getClass().isArray())
			return Array.getLength(obj) < 1;

		if (obj instanceof Map)
			return ((Map<?, ?>)obj).isEmpty();

		if (obj instanceof Optional)
			return !((Optional<?>)obj).isPresent();

		return false;
	}

	private static boolean isWhitespace(String str) {
		for (int i = 0, length = str.length(); i < length; ++i) {
			if (!Character.isWhitespace(str.charAt(i)))
				return false;
		}
		return true;
	}

	/**Returns the result of nt if t {@link #isEmpty(Object)}. Otherwise, returns t.
	 * @param <T>	a type
	 * @param t		an Object
	 * @param nt	a Supplier to invoke if t <code>isEmpty(...)</code>
	 * @return
	 * <ul><li>t if t is not empty.</li>
	 * 	   <li>result from nt if t is empty.</li>
	 * </ul>
	 */
	public static <T> T ifEmpty(T t, Supplier<T> nt) {
		return !isEmpty(t) ? t : nt != null ? nt.get() : t;
	}

	/**Asserts that t is not empty and returns t.<br />
	 * To be effective, start the JVM with either <code>-enableassertion:horizon.base.Assert</code> or <code>-ea:horizon.base.Assert</code>.
	 * @param t		an object
	 * @param name	t's name, used for a message on assertion failure
	 * @param <T>	a type
	 * @return t
	 * @throws NullPointerException if t is empty.
	 */
	public static <T> T assertNotEmpty(T t, String name) {
		try {
			assert !isEmpty(t);
			return t;
		} catch (AssertionError e) {
			throw new NullPointerException(name + " is null or empty");
		}
	}

	/**Returns t if t is not empty.<br />
	 * If t {@link #isEmpty(Object) is empty}, throws a NullPointerException.
	 * @param t		an object
	 * @param name	t's name used for exception message
	 * @return t
	 * @throws NullPointerException if t is empty
	 */
	public static <T> T notEmpty(T t, String name) {
		if (!isEmpty(t)) return t;
		throw new NullPointerException(name + " is null or empty");
	}

	/**Returns the root cause of t.
	 * @param t a Throwable
	 * @return the root cause of t
	 */
	public static Throwable rootCause(Throwable t) {
		if (t == null)
			return null;
		Throwable cause = t.getCause();
		if (cause == null
		 && t instanceof InvocationTargetException)
			cause = ((InvocationTargetException)t).getTargetException();
		return cause == null || cause == t ? t : rootCause(cause);
	}

	/**Returns a RuntimeException with the root cause of t.
	 * @param t a Throwable
	 * @return a RuntimeException
	 */
	public static RuntimeException runtimeException(Throwable t) {
		Throwable cause = rootCause(t);
		return cause instanceof RuntimeException ? RuntimeException.class.cast(cause) : new RuntimeException(cause);
	}

	/**Interface to {@link Assert}.
	 * <p>All methods of this interface have default implementation, which delegate operations to corresponding methods of the Assert class.<br />
	 * The intention is to help use the Assert operations easily by implementing this interface.
	 * </p>
	 */
	public static interface Support {
		/**See {@link Assert#equals(Object, Object)}.*/
		default boolean equals(Object lv, Object rv) {
			return Assert.equals(lv, rv);
		}

		/**See {@link Assert#isEmpty(Object)}.*/
		default boolean isEmpty(Object obj) {
			return Assert.isEmpty(obj);
		}

		/**See {@link Assert#ifEmpty(Object, Supplier)}.*/
		default <T> T ifEmpty(T t, Supplier<T> nt) {
			return Assert.ifEmpty(t, nt);
		}

		/**See {@link Assert#assertNotEmpty(Object, String)}.*/
		default <T> T assertNotEmpty(T t, String name) {
			return Assert.assertNotEmpty(t, name);
		}

		/**See {@link Assert#notEmpty(Object, String)}.*/
		default <T> T notEmpty(T t, String name) {
			return Assert.notEmpty(t, name);
		}

		/**See {@link Assert#rootCause(Throwable)}.*/
		default Throwable rootCause(Throwable t) {
			return Assert.rootCause(t);
		}

		/**See {@link Assert#runtimeException(Throwable)}.*/
		default RuntimeException runtimeException(Throwable t) {
			return Assert.runtimeException(t);
		}
	}
}