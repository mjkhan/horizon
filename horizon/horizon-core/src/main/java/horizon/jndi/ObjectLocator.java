/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.jndi;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;

import horizon.base.AbstractComponent;

/**Looks up an object using the JNDI service.
 * <p>
 * You should specify a context name to create or {@link #get(String) get} an ObjectLocator.<br />
 * The context name is the name of a configuration in the <a href="{@docRoot}/horizon/jndi/package-summary.html#Configuration">'object-locator.xml'</a> file.<br />
 * An ObjectLocator uses the context name to pick up the environment information from the configuration file.<br />
 * You can also use a null context name to look up an object in the default JNDI namespace.
 * <p>
 * You then {@link #lookup(String) look up} the required object with the object's name bound in the JNDI namespace.<br />
 * Following is an example of using an ObjectLocator.<br />
 * With 'my-ctx' as the context name and 'my-obj' as the name bound to the required object,
 * you can get it like this:
 * <pre class="shade">Object obj0 = new ObjectLocator("my-ctx").lookup("my-obj");
 * //Or to reuse an ObjectLocator
 * Object obj1 = ObjectLocator.get("my-ctx").lookup("my-obj");</pre>
 * If you decided to go with the default namespace
 * <pre class="shade">Object obj0 = new ObjectLocator(null).lookup("my-obj");
 * //Or to reuse an ObjectLocator
 * Object obj1 = ObjectLocator.get(null).lookup("my-obj");</pre>
 */
public class ObjectLocator extends AbstractComponent {
	private static final String DEFAULT = "_default_";
	private static Hashtable<String, ObjectLocator> cache = new Hashtable<>();

	public static void configure(String path) {
		JNDIConfig.configure(path);
	}

	/**Returns a recycled ObjectLocator associated with the contextName.
	 * @param contextName contextName
	 * @return ObjectLocator
	 */
	public static final ObjectLocator get(String contextName) {
		String key = ifEmpty(contextName, () -> DEFAULT);
		ObjectLocator locator = cache.get(key);
		if (locator == null)
			cache.put(key, locator = new ObjectLocator(key));
		return locator;
	}

	private String contextName;
	private Hashtable<String, Object>
		cfg,
		tmpCfg;
	private InitialContext ictx;

	/**Creates a new ObjectLocator and sets the contextName.
	 * @param contextName the contextName
	 */
	public ObjectLocator(String contextName) {
		setContextName(contextName);
	}

	/**Returns the contextName.
	 * @return the contextName
	 */
	public String contextName() {
		return contextName;
	}

	private void setContextName(String contextName) {
		if (equals(this.contextName, contextName)) return;
		this.contextName = contextName;
		cfg = tmpCfg = null;
		if (!isEmpty(contextName) && !DEFAULT.equals(contextName))
			cfg = JNDIConfig.get(contextName);
		ictx = null;
	}

	/**Returns the configuration of the ObjectLocator.
	 * @return configuration of the ObjectLocator
	 */
	public Map<String, Object> config() {
		return ifEmpty(cfg, Collections::emptyMap);
	}

	/**Adds the key-value pair to the environment information.<br />
	 * If the value is null, the entry associated with the key is removed from the environment information.
	 * @param key	key for the environment information. Typically a constant of the javax.naming.Context interface.
	 * @param value	value of the environment information
	 * @return the ObjectLocator
	 */
	public ObjectLocator add(String key, Object value) {
		if (tmpCfg == null) {
			if (value == null) return this;
			tmpCfg = new Hashtable<>();
			tmpCfg.put(key, value);
		} else {
			if (value == null) tmpCfg.remove(key);
			else tmpCfg.put(key, value);
		}
		return this;
	}

	/**Returns the naming context the ObjectLocator uses in object lookup.
	 * @return naming context the ObjectLocator uses in object lookup
	 */
	public Context context() {
		try {
			if (ictx == null)
				ictx = cfg == null && tmpCfg == null ? new InitialContext() : configuredContext();
			return ictx;
		} catch (Exception e) {
			ictx = null;
			throw runtimeException(e);
		}
	}

	/**Looks up and returns the object bound to the name in the JNDI name space.
	 * @param <T>	any type
	 * @param name	the JNDI name
	 * @return the object bound to the name in the JNDI name space
	 * @throws ClassCastException when invalid type-casting is attempted on the returned object
	 */
	@SuppressWarnings("unchecked")
	public <T> T lookup(String name) {
		try {
			return (T)context().lookup(name);
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	private InitialContext configuredContext() throws Exception {
		Hashtable<String, Object> env = null;
		if (tmpCfg == null) env = cfg;
		else {
			env = new Hashtable<>();
			if (cfg != null) env.putAll(cfg);
			env.putAll(tmpCfg);
		}
		return new InitialContext(env);
	}
}