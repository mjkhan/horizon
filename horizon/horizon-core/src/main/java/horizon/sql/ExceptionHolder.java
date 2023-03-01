/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql;

import java.util.ArrayList;

import horizon.base.Assert;
import horizon.base.Log;

class ExceptionHolder extends RuntimeException {
	private static final long serialVersionUID = 1L;

	static final ExceptionHolder get(Throwable t) {
		if (t == null)
			throw new IllegalArgumentException("t is null");
		if (t instanceof ExceptionHolder)
			return ExceptionHolder.class.cast(t);

		return new ExceptionHolder(t);
	}

	private static ArrayList<ExceptionHolder> getExceptions() {
		return DBAccess.resources().get("exceptionHolder");
	}

	private static void add(ExceptionHolder eh) {
		if (eh == null || !eh.handled) return;

		ArrayList<ExceptionHolder> list = getExceptions();
		if (list == null) {
			DBAccess.resources().put("exceptionHolder", list = new ArrayList<>());
		}
		if (!list.contains(eh))
			list.add(eh);
	}

	static final boolean isEmpty() {
		ArrayList<ExceptionHolder> list = getExceptions();
		return Assert.isEmpty(list);
	}

	static final void clear() {
		ArrayList<ExceptionHolder> list = DBAccess.resources().remove("exceptionHolder");
		if (!Assert.isEmpty(list)) {
			list.clear();
			Log.get(ExceptionHolder.class).trace(() -> "Exceptions cleared");
		}
	}

	private boolean handled;

	ExceptionHolder(Throwable cause) {
		super(Assert.rootCause(cause));
	}

	boolean isHandled() {
		return handled;
	}

	void setHandled() {
		this.handled = true;
		add(this);
	}
}