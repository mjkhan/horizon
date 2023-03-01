/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql;

import java.sql.CallableStatement;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import horizon.base.AbstractComponent;

/**Holds parameters of an SQL statement calling a stored procedure with OUT parameters.<br />
 * You get a Parameters from a {@link Query#parameters() Query} or an {@link Update#parameters() Update}.
 * <p>To call a stored procedure "sp_query(?, ?, ?, ?)" where 3rd and 4th parameters are OUT parameters,<br />
 * execute the stored procedure as follows:
 * <pre><code> dbaccess.<b>perform</b>(db -> {
 *     dbaccess.query()
 *         .sql("{call sp_query(?, ?, ?, ?)}")
 *         <b>.parameters()</b>
 *         .in("value 1") // sets the 1st (IN) parameter
 *         .in("value 2") // sets the 2nd (IN) parameter
 *         .out()         // registers the 3rd as an OUT parameter
 *         .out();         // registers the 4th as an OUT parameter
 *     Dataset dataset = dbaccess.query().getDataset(); // can be getValue(), getObjects(), getObject(), getStream()
 *     Object out0 = dbaccess.query().parameters().getValue(3), // gets the value of the 3rd OUT parameter
 *            out1 = dbaccess.query().parameters().getValue(4); // gets the value of the 4th OUT parameter
 * });</code></pre>
 * It can be rewritten using named parameters like this:
 * <pre><code> dbaccess.<b>perform</b>(db -> {
 *     dbaccess.query()
 *         .sql("{call sp_query(#{param1}, #{param2}, #{<b>OUT:</b>param3}, #{<b>OUT:</b>param4})}")
 *         <b>.parameters()</b>
 *         .param("param1", "value 1") // sets the 1st (IN) parameter
 *         .param("param2", "value 2") // sets the 2nd (IN) parameter
 *     Dataset dataset = dbaccess.query().getDataset(); // can be getValue(), getObjects(), getObject(), getStream()
 *     Object out0 = dbaccess.query().parameters().getValue("param3"), // gets the value of the OUT parameter named 'param3'
 *            out1 = dbaccess.query().parameters().getValue("param4"); // gets the value of the OUT parameter named 'param4'
 * });</code></pre>
 * To execute the code in a transaction context, use <code>dbaccess.transact(...)</code> instead.
 * <pre><code> dbaccess.<b>transact</b>(db -> {
 *     dbaccess.update()
 *         .sql("{call sp_update(#{param1}, #{param2}, #{<b>OUT:</b>param3}, #{<b>OUT:</b>param4})}")
 *         <b>.parameters()</b>
 *         .param("param1", "value 1") // sets the 1st (IN) parameter
 *         .param("param2", "value 2") // sets the 2nd (IN) parameter
 *     int affected = dbaccess.update().execute();
 *     Object out0 = dbaccess.update().parameters().getValue("param3"),// gets the value of the OUT parameter named 'param3'
 *            out1 = dbaccess.update().parameters().getValue("param4");// gets the value of the OUT parameter named 'param4'
 * });</code></pre>
 * </p>
 */
public class Parameters extends AbstractComponent {
	/**Types of parameters
	 */
	public enum Type {
		/**IN parameter*/
		IN,
		/**OUT parameter*/
		OUT
	}

	/**Parameter entry in a Parameters
	 */
	public static class Entry {
		private Type type;
		private String name;
		private Object value;

		/**Creates a new Entry.
		 * @param type	parameter type
		 * @param name	parameter name
		 * @param value	parameter value
		 */
		public Entry(Type type, String name, Object value) {
			this.type = type;
			this.name = name;
			this.value = value;
		}

		public Type type() {
			return type;
		}

		public String name() {
			return name;
		}

		public Object value() {
			return value;
		}

		void bind(PreparedStatement ps, int index) throws Exception {
				if (value == null)
					ps.setNull(index, Types.NULL);
				else
					ps.setObject(index, value);
		}

		void bind(CallableStatement cs, int index) throws Exception {
			if (Type.IN.equals(type)) {
				bind((PreparedStatement)cs, index);
			} else {
				ParameterMetaData metadata = cs.getParameterMetaData();
				cs.registerOutParameter(index, metadata.getParameterType(index));
			}
/*
			boolean named = !isEmpty(name);
			if (Type.IN.equals(type)) {
				if (named) {
					if (value == null)
						cs.setNull(name, Types.NULL);
					else
						cs.setObject(name, value);
				} else
					bind((PreparedStatement)cs, index);
			} else {
				if (named)
					cs.registerOutParameter(name, Types.OTHER);
				else
					cs.registerOutParameter(index, Types.OTHER);
			}
*/
		}
	}

	private ArrayList<Entry> entries;
	private Object[] args;

	/**Returns whether the Parameters has no entries.
	 * @return
	 * <ul><li>true if the Parameters has no entries</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 */
	public boolean isEmpty() {
		return isEmpty(args) && isEmpty(entries);
	}

	/**Returns the parameter value at the index.<br />
	 * This method is to get the value of an OUT parameter after executing a stored procedure.
	 * @param index position of the parameter. Starts from 1.
	 * @return parameter value at the index
	 */
	@SuppressWarnings("unchecked")
	public <T> T getValue(int index) {
		return isEmpty(entries) ? null : (T)entries.get(index - 1).value;
	}

	/**Returns the value of the named parameter.<br />
	 * This method is to get the value of a named OUT parameter after executing a stored procedure.
	 * @param name parameter name
	 * @return value of the named parameter
	 */
	@SuppressWarnings("unchecked")
	public <T> T getValue(String name) {
		notEmpty(name, "name");
		if (!isEmpty(entries))
			for (Entry entry: entries)
				if (name.equalsIgnoreCase(entry.name))
					return (T)entry.value;
		throw new RuntimeException("parameter not found named '" + name + "'");
	}

	void setEntries(List<Entry> entries) {
		if (!isEmpty(entries))
			clear().setEntries().addAll(entries);
	}

	private List<Entry> setEntries() {
		if (!isEmpty(args))
			throw new IllegalStateException("The methods setArgs(...) and in/out(...) cannot be used together");
		return ifEmpty(entries, () -> entries = new ArrayList<>());
	}

	/**Sets the args as arguments for parameters of an SQL statement.<br />
	 * The SQL statement must have only IN parameters, and
	 * the args must match the SQL statement's parameters in number and order.
	 * @param args arguments
	 * @return the Parameters
	 */
	public Parameters setArgs(Object... args) {
		if (!isEmpty(entries))
			throw new IllegalStateException("The methods setArgs(...) and in/out(...) cannot be used together");
		this.args = args;
		return this;
	}

	/**Sets the value as an argument for an IN parameter.
	 * @param value argument value
	 * @return the Parameters
	 */
	public Parameters in(Object value) {
		setEntries().add(new Entry(Type.IN, null, value));
		return this;
	}

	/**Sets the value as an argument for the named IN parameter.
	 * @param name	parameter name
	 * @param value	argument value
	 * @return the Parameters

	Parameters in(String name, Object value) {
		setEntries().add(new Entry(Type.IN, name, value));
		return this;
	}
	*/
	/**Registers an OUT parameter.
	 * @return the Parameters
	 */
	public Parameters out() {
		setEntries().add(new Entry(Type.OUT, null, null));
		return this;
	}

	/**Registers a named OUT parameter.
	 * @param name name of an OUT parameter
	 * @return the Parameters
	Parameters out(String name) {
		setEntries().add(new Entry(Type.OUT, name, null));
		return this;
	}
	*/
	void bind(boolean clear, PreparedStatement pstmt) throws Exception {
		if (clear)
			pstmt.clearParameters();
		if (isEmpty(args) && isEmpty(entries)) return;

		boolean debug = log().getLogger().isDebugEnabled();
		StringBuilder buff = debug ? new StringBuilder() : null;

		if (!isEmpty(args)) {
			for (int i = 0, length = args.length; i < length; ++i) {
				int index = i + 1;
				Object arg = args[i];
				if (arg == null)
					pstmt.setNull(index, Types.NULL);
				else
					pstmt.setObject(index, arg);
				if (!debug) continue;

				if (buff.length() > 0)
					buff.append(",");
				buff.append("[" + index + "]={" + arg + "}");
			}
		} else if (!isEmpty(entries)) {
			CallableStatement cstmt = pstmt instanceof CallableStatement ? (CallableStatement)pstmt : null;
			for (int i = 0, length = entries.size(); i < length; ++i) {
				int index = i + 1;
				Entry entry = entries.get(i);
				if (cstmt == null)
					entry.bind(pstmt, index);
				else
					entry.bind(cstmt, index);
				if (!debug) continue;

				if (buff.length() > 0)
					buff.append(",");
				buff.append("[" + index + "]=");
				if (Type.IN.equals(entry.type)) {
					buff.append("{" + entry.value + "}");
				} else {
					buff.append("OUT");
				}
			}
		}

		if (debug)
			log().debug("args:" + buff.toString());
	}

	void setResult(PreparedStatement pstmt) {
		if (isEmpty(entries) || !(pstmt instanceof CallableStatement)) return;

		try {
			CallableStatement cstmt = (CallableStatement)pstmt;
			for (int i = 0; i < entries.size(); ++i) {
				Entry entry = entries.get(i);
				if (Type.IN.equals(entry.type)) continue;

				entry.value = cstmt.getObject(i + 1);
			}
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	Parameters clear(Iterable<String> paramNames) {
		if (!isEmpty(paramNames) && !isEmpty(entries)) {
			paramNames.forEach(name -> entries.removeIf(entry -> name.equals(entry.name)));
		}
		return this;
	}

	/**Clears up the internal state.
	 * @return the Parameters
	 */
	public Parameters clear() {
		if (!isEmpty()) {
			if (entries != null)
				entries.clear();
			args = null;
			log().trace(() -> getClass().getSimpleName() + " cleared");
		}
		return this;
	}
}