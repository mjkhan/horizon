/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import horizon.base.AbstractComponent;
import horizon.sql.DBAccess.TryReturn;
import horizon.sql.support.EXProcessor;
import horizon.sql.support.SQLProc;

class DBAction extends AbstractComponent {
	protected DBAccess dbaccess;
	protected PreparedStatement pstmt;
	protected SQLProc sqlproc;
	protected ResultSet resultset;
	protected ResultParser parser;
	protected DatasetBuilder datasetBuilder;

	protected boolean prepared;
	protected String
		sql,
		sqlId,
		working;
	protected Parameters params;
	protected Map<String, Object> argMap;

	protected DBAction(DBAccess dbaccess) {
		setDBAccess(dbaccess);
	}

	/**Sets the dbaccess this object uses in accessing the database.
	 * @param dbaccess DBAccess
	 * @return this object
	 */
	protected DBAction setDBAccess(DBAccess dbaccess) {
		if (!equals(this.dbaccess, dbaccess)) {
			close();
			this.dbaccess = dbaccess;
			dbaccess.add(this);
		}
		return this;
	}

	/**Returns the Connection to the database.
	 * @return database Connection
	 */
	protected Connection connection() {
		return dbaccess.connection();
	}

	public DBAction sql(String sql) {
		if (!equals(this.sql, sql)) {
			close();
			this.sql = this.working = sql;
		}
		return this;
	}

	public DBAction sqlId(String sqlId) {
		if (!equals(this.sqlId, notEmpty(sqlId, "sqlId"))) {
			close();
			this.sqlId = sqlId;
		}

		return this;
	}

	void setWorkingStatement(String sql) {
		if (equals(working, sql)) return;
		if (sql.contains("OUT:"))
			throw new IllegalArgumentException(getClass().getSimpleName() + " does not support OUT parameters");

		try {
			String tmp = this.sql;
			closeStatement();
			working = sql;
			this.sql = tmp;
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	EXProcessor expr() {
		return dbaccess.sqlBuilder().expr();
	}

	boolean isCallable(String sql) {
		if (isEmpty(sql))
			return false;

		if (sql.startsWith("{"))
			return true;

		String str = sql.trim().toLowerCase();
		return str.startsWith("call ") || str.contains("call ") || str.contains(" call ");
	}

	boolean prepare(boolean keys) throws Exception {
		if (prepared) return false;

		notEmpty(working, "sql");
		if (!isEmpty(sqlId)) {
			log().debug(() -> sqlId + " ==> " + working.replaceAll("[\\n\\r]+", "\n"));
		} else {
			log().debug(() -> working);
		}
		if (isCallable(working)) {
			pstmt = connection().prepareCall(working);
		} else {
			pstmt = !keys ? connection().prepareStatement(working)
				  : connection().prepareStatement(working, PreparedStatement.RETURN_GENERATED_KEYS);
		}

		return prepared = pstmt != null;
	}

	/**Returns the Parameters this object works with.<br />
	 * Use this method only when the SQL statement is for a stored procedure that has OUT parameters.<br />
	 * @see Parameters
	 * @return Parameters
	 */
	public Parameters parameters() {
		return ifEmpty(params, () -> params = new Parameters());
	}

	DBAction setParams(Parameters params) {
		if (!equals(this.params, params)) {
			clearParams();
			this.params = params;
		}
		return this;
	}

	/**Sets arguments for the parameters of the SQL statement.<br />
	 * Use this method when the statement is set with {@link #sql(String)} and its parameters are specified with '?' characters.<br />
	 * The arguments must match the parameters in number and order.<br />
	 * If the statement doesn't have any parameters, do not call this method or pass null for the arguments.
	 * @param args arguments
	 * @return this object
	 */
	public DBAction params(Object... args) {
		parameters().setArgs(args);
		return this;
	}

	/**Sets the value as an argument for the named parameter of the SQL statement.<br />
	 * Use this method when the statement has named parameters.<br />
	 * If the statement doesn't have any parameters, do not call this method.
	 * @param name	parameter name
	 * @param value	parameter value
	 * @return this object
	 */
	public DBAction param(String name, Object value) {
		if (argMap == null)
			argMap = new HashMap<>();

		argMap.put(notEmpty(name, "name"), value);
		return this;
	}

	/**Sets arguments for named parameters of the SQL statement using the key-value pairs in params.<br />
	 * The keys are used as parameter names and the associated values as arguments.<br />
	 * Use this method when the statement has named parameters.<br />
	 * If the statement doesn't have any parameters, do not call this method.
	 * @param params key-value pairs for parameter names and values
	 * @return this object
	 */
	public DBAction params(Map<String, ?> params) {
		if (!equals(argMap, params)) {
			if (params != null)
				params.forEach(this::param);
		}
		return this;
	}

	void bind(boolean clear) throws Exception {
		if (params != null)
			params.bind(clear, pstmt);
	}

	boolean preprocess() {
		if (isEmpty(sqlId) && isEmpty(argMap)) return false;

		if (isEmpty(sqlId)) {
			parameterizeSQL();
			return false;
		} else {
			return parameterizeInstruction();
		}
	}

	void parameterizeSQL() {
		SQLProc sqlproc = dbaccess.sqlBuilder().build(sql, argMap);
		setWorkingStatement(sqlproc.getStatement());
		parameters().setEntries(sqlproc.getCurrentEntries());
		this.sqlproc = sqlproc;
	}

	boolean parameterizeInstruction() {
		SQLProc sqlproc = dbaccess.sqlBuilder().buildFromInstruction(sqlId, argMap);
		setWorkingStatement(sqlproc.getStatement());
		parameters().setEntries(sqlproc.getCurrentEntries());
		this.sqlproc = sqlproc;
		return sqlproc.hasMore();
	}

	DatasetBuilder datasetBuilder() {
		return ifEmpty(datasetBuilder, () -> datasetBuilder = new DatasetBuilder());
	}

	ResultParser parser() {
		return ifEmpty(parser, () -> parser = new ResultParser()).setBuilder(datasetBuilder());
	}

	void clearResult() throws Exception {
		if (resultset == null) return;

		resultset.close();
		log().trace(() -> "ResultSet closed");
		resultset = null;
	}

	/**Clears up this object to the initial state.
	 * @return this object
	 */
	public DBAction close() {
		sqlId = null;
		if (argMap != null)
			argMap.clear();
		clearParams();
		try {
			closeStatement();
			return this;
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	private void clearParams() {
		if (params != null)
			params.clear();
	}

	private void closeStatement() throws Exception {
		try {
			clearResult();
			if (pstmt != null) {
				pstmt.close();
				log().trace(() -> "Statement closed");
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (parser != null)
				parser.clear();
			datasetBuilder = null;
			pstmt = null;
			sqlproc = null;
			working = sql = null;
			prepared = false;
		}
	}

	/**Executes the statement.
	 * @param keys
	 * <ul><li>true if the SQL statement returns a generated keys</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 * @return
	 * <ul><li>true if the statement returns a ResultSet</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 * @throws Exception
	 */
	boolean execute(boolean keys) throws Exception {
		prepare(keys);
		bind(true);
		boolean result = pstmt.execute();
		if (params != null)
			params.setResult(pstmt);
		return result;
	}

	/**Executes the task returning a result.
	 * <p>Before the task execution, opens the DBAccess if not yet open.<br />
	 * After the task execution, closes the DBAccess if opened by the current method call.
	 * </p>
	 * @param task task to execute
	 * @return result of the task
	 */
	<T> T execute(TryReturn<T> task) {
		if (task == null)
			return null;

		boolean close = dbaccess.open();
		try {
			return task.attempt(dbaccess);
		} catch (Throwable e) {
			close();
			throw runtimeException(e);
		} finally {
			if (close)
				dbaccess.close();
		}
	}
}