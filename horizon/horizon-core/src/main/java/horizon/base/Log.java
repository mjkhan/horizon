/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.base;

import java.util.HashMap;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**Provides a facility that wraps the <a href="https://www.slf4j.org/" target="_blank">SLF4J </a> framework.
 * <p>To use the logging facility, you must provide and configure an SLF4J implementation library beforehand.
 * </p>
 */
public class Log {
	private static HashMap<Class<?>, Log> byClass = new HashMap<>();
	private static HashMap<String, Log> byName = new HashMap<String, Log>();
	private final Logger logger;

	/**Returns the Log associated with the klass.
	 * @param klass a Class
	 * @return a Log
	 */
	public static Log get(Class<?> klass) {
		Log log = byClass.get(klass);
		if (log == null) {
			byClass.put(klass, log = new Log(LoggerFactory.getLogger(klass)));
		}
		return log;
	}

	/**Returns the Log associated with the name.
	 * @param name a Logger name
	 * @return a Log
	 */
	public static Log get(String name) {
		Log log = byName.get(name);
		if (log == null) {
			byName.put(name, log = new Log(LoggerFactory.getLogger(name)));
		}
		return log;
	}

	private Log(Logger logger) {
		this.logger = logger;
	}

	/**Returns the Logger wrapped by this Log.
	 * @return the Logger
	 */
	public Logger getLogger() {
		return this.logger;
	}

	/**logs, if the underlying logger is INFO-enabled, informational message returned from msg.
	 * @param msg message supplier
	 */
	public void info(Supplier<String> msg) {
		if (msg == null || !logger.isInfoEnabled()) return;
		logger.info(msg.get());
	}

	/**logs, if the underlying logger is INFO-enabled, the msg.
	 * @param msg a message string
	 */
	public void info(String msg) {
		info(!Assert.isEmpty(msg) ? () -> msg : null);
	}

	/**logs, if the underlying logger is DEBUG-enabled, debugging message returned from msg.
	 * @param msg message supplier
	 */
	public void debug(Supplier<String> msg) {
		if (msg == null || !logger.isDebugEnabled()) return;
		logger.debug(msg.get());
	}

	/**logs, if the underlying logger is DEBUG-enabled, the msg.
	 * @param msg a message string
	 */
	public void debug(String msg) {
		debug(!Assert.isEmpty(msg) ? () -> msg : null);
	}

	/**logs, if the underlying logger is TRACE-enabled, tracing message returned from msg.
	 * @param msg message supplier
	 */
	public void trace(Supplier<String> msg) {
		if (msg == null || !logger.isTraceEnabled()) return;
		logger.trace(msg.get());
	}

	/**logs, if the underlying logger is TRACE-enabled, the msg.
	 * @param msg a message string
	 */
	public void trace(String msg) {
		trace(!Assert.isEmpty(msg) ? () -> msg : null);
	}

	/**logs, if the underlying logger is WARN-enabled, warning message returned from msg.
	 * @param msg message supplier
	 */
	public void warn(Supplier<String> msg) {
		if (msg == null || !logger.isWarnEnabled()) return;
		logger.warn(msg.get());
	}

	/**logs, if the underlying logger is WARN-enabled, the msg.
	 * @param msg a message string
	 */
	public void warn(String msg) {
		warn(!Assert.isEmpty(msg) ? () -> msg : null);
	}

	/**logs, if the underlying logger is ERROR-enabled, error message returned from msg.
	 * @param msg message supplier
	 */
	public void error(Supplier<String> msg) {
		if (msg == null || !logger.isErrorEnabled()) return;
		logger.error(msg.get());
	}

	/**logs, if the underlying logger is ERROR-enabled, the msg.
	 * @param msg a message string
	 */
	public void error(String msg) {
		error(!Assert.isEmpty(msg) ? () -> msg : null);
	}
}