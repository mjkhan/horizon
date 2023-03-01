/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.el.BeanNameResolver;
import javax.el.ELProcessor;
import javax.el.PropertyNotWritableException;

import horizon.base.AbstractComponent;

public class EXProcessor extends AbstractComponent {
	private ELProcessor proc;
	private Map<String, Object> beans;

	private ELProcessor processor() {
		if (proc == null) {
			proc = new ELProcessor();
			proc.getELManager().addBeanNameResolver(new BeanResolver(this));
		}
		return proc;
	}

	private Object getBean(String name) {
		return beans != null ? beans.get(name) : null;
	}

	public Map<String, Object> getBeans() {
		return ifEmpty(beans, Collections::emptyMap);
	}

	public EXProcessor setBean(String name, Object obj) {
		processor().defineBean(name, obj);
		putBean(name, obj);
		return this;
	}

	private void putBean(String name, Object obj) {
		if (beans == null)
			beans = new HashMap<>();
		beans.put(name, obj);
	}

	public EXProcessor setBeans(Map<String, ?> beans) {
		if (!equals(this.beans, beans)) {
			processor();
			if (beans != null)
				beans.forEach(this::setBean);
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T> T getValue(String expr) {
		return (T)processor().getValue(expr, Object.class);
	}

	public boolean test(String expr) {
		return getValue(expr);
	}

	public EXProcessor setValue(String expr, Object value) {
		processor().setValue(expr, value);
		return this;
	}

	public void clearBeans() {
		if (proc == null || isEmpty(beans)) return;

		beans.forEach((key, value) -> proc.defineBean(key, null));
		beans.clear();
	}

	private static class BeanResolver extends BeanNameResolver {
		private EXProcessor xproc;

		BeanResolver(EXProcessor xproc) {
			this.xproc = xproc;
		}

		@Override
		public boolean isNameResolved(String beanName) {
			return true;
		}

		@Override
		public Object getBean(String beanName) {
			return xproc.getBean(beanName);
		}

		@Override
		public void setBeanValue(String beanName, Object value) throws PropertyNotWritableException {
			xproc.putBean(beanName, value);
		}

		@Override
		public boolean isReadOnly(String beanName) {
			return false;
		}

		@Override
		public boolean canCreateBean(String beanName) {
			return true;
		}
	}
}