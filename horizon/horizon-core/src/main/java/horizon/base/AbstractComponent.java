/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.base;

import java.util.function.Supplier;

/**Extends java.lang.Object, augmented with frequently used trivial methods.
 * <p>Along with methods offered by the {@link Assert},
 * an AbstractComponent provides methods to use the SLF4J logging framework.
 * </p>
 * <p>By default, the {@link #log()} method returns the Logger associated with the class extending the AbstractComponent.<br />
 * To have the method return a named Logger, the {@link #setLogName(String) Logger name must be set} beforehand.
 * </p>
 */
public abstract class AbstractComponent {
	private String logName;

	protected AbstractComponent() {}

	/**Sets the name of the Logger.
	 * @param logName name of the Logger
	 */
	public void setLogName(String logName) {
		this.logName = logName;
	}

	/**Returns the Log associated with this class.
	 * @return Log associated with this class
	 */
	protected final Log log() {
		return isEmpty(logName) ? log(getClass()) : Log.get(logName);
	}

	/**Returns the Log associated with the klass.
	 * @param klass a Class
	 * @return Log associated with the klass
	 */
	protected final static Log log(Class<?> klass) {
		return Log.get(klass);
	}

	/**See {@link Assert#equals(Object, Object)}.
	 */
	protected static boolean equals(Object lv, Object rv) {
		return Assert.equals(lv, rv);
	}

	/**See {@link Assert#isEmpty(Object)}.
	 */
	protected static boolean isEmpty(Object obj) {
		return Assert.isEmpty(obj);
	}

	/**See {@link Assert#ifEmpty(Object, Supplier)}*/
	protected static <T> T ifEmpty(T t, Supplier<T> nt) {
		return Assert.ifEmpty(t, nt);
	}

	/**See {@link Assert#notEmpty(Object, String)}.
	 */
	protected static <T> T notEmpty(T t, String name) {
		return Assert.notEmpty(t, name);
	}

	/**See {@link Assert#rootCause(Throwable)}.
	 */
	protected static Throwable rootCause(Throwable t) {
		return Assert.rootCause(t);
	}

	/**See {@link Assert#runtimeException(Throwable)}.
	 */
	protected static RuntimeException runtimeException(Throwable t) {
		return Assert.runtimeException(t);
	}

	/**Returns the AbstractComponent cast to T.
	 * @param <T> an AbstractComponent type
	 * @return the AbstractComponent cast to T
	 */
	@SuppressWarnings("unchecked")
	protected <T extends AbstractComponent> T self() {
		Object obj = this;
		return (T)obj;
	}
}