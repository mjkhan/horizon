/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.base;

/**Utility to help using Class objects.
 */
public class Klass extends AbstractComponent {
	private Klass() {}

	/**Returns the klass cast to {@code Class<T>}.
	 * @param <T>	a type
	 * @param klass	class
	 * @return Class<T>
	 * @throws ClassCastException
	 */
	@SuppressWarnings("unchecked")
	public static final <T> Class<T> as(Class<?> klass) {
		return (Class<T>)klass;
	}

	/**Loads and returns the named class.
	 * @param <T>		a type
	 * @param className	name of the class
	 * @return the named class
	 */
	public static final <T> Class<T> of(String className) {
		try {
			Class<?> cls = isEmpty(className) ? null : Class.forName(notEmpty(className, "className"));
			return as(cls);
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	/**Creates and returns an object of the klass.
	 * The klass must have a default constructor with no arguments.
	 * @param <T>	a type
	 * @param klass	a class
	 * @return object of the klass
	 */
	public static final <T> T instance(Class<T> klass) {
		try {
			return klass == null ? null : klass.getDeclaredConstructor((Class<?>[])null).newInstance();
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}
}