/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.jndi;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;

import org.w3c.dom.Element;

import horizon.base.AbstractComponent;
import horizon.util.ResourceLoader;
import horizon.util.Xmlement;

class JNDIConfig extends AbstractComponent {
	private static Hashtable<String, Hashtable<String, Object>> cfgs;

	static void configure(String path) {
		cfgs = new Loader().getConfigurations(path);
	}

	static final Hashtable<String, Object> get(String contextName) {
		if (cfgs == null)
			configure("object-locator.xml");
		Hashtable<String, Object> env = cfgs.get(contextName);
		if (env == null)
			throw new NullPointerException("JNDI configuration not found for " + contextName);
		return env;
	}

	static final List<String> contextNames(String... exclude) {
		ArrayList<String> result = new ArrayList<>(cfgs.keySet());
		if (!isEmpty(exclude))
			remove(result, exclude);
		return result;
	}

	private static void remove(ArrayList<String> names, String... excludes) {
		for (int i = 0; i < names.size();) {
			String name = names.get(i);
			for (String exclude: excludes) {
				if (name.equals(exclude))
					names.remove(i);
				else
					++i;
			}
		}
	}

	private static class Loader extends AbstractComponent {
		private Hashtable<String, String> constants = new Hashtable<>();
		private Xmlement xml = Xmlement.get();

		Loader() {
			setConstants();
		}

		private void setConstants() {
			for (Field field: Context.class.getFields()) {
				int mod = field.getModifiers();
				if (!Modifier.isPublic(mod) || !Modifier.isStatic(mod)) continue;
				try {
					constants.put(field.getName(), String.class.cast(field.get(null)));
				} catch (Exception e) {
					log().warn(() -> "Failed to get the value of " + Context.class.getName() + "." + field.getName());
				}
			}
		}

		public Hashtable<String, Hashtable<String, Object>> getConfigurations(String path) {
			return getConfigurations(ResourceLoader.find(path));
		}

		public Hashtable<String, Hashtable<String, Object>> getConfigurations(InputStream input) {
			Hashtable<String, Hashtable<String, Object>> cfgs = new Hashtable<>();
			if (input != null)
			for (Element child: xml.getChildren(xml.getDocument(input), "naming-context")) {
				String contextName = xml.attribute(child, "name");
				cfgs.put(contextName, getEnvironment(child));
				log().debug(() -> "JNDI '" + contextName + "' (re)configured");
			}
			return cfgs;
		}

		private Hashtable<String, Object> getEnvironment(Element node) {
			List<Element> children = xml.getChildren(node, null);

			Hashtable<String, Object> env = new Hashtable<>();
			for (Element child: children) {
				String value = child.getTextContent();
				if (isEmpty(value)) continue;
				String constant = constants.get(child.getNodeName().trim());
				if (isEmpty(constant)) continue;

				env.put(constant, value);
			}

			return env;
		}
	}
}