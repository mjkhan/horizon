/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import horizon.data.DataObject;
import horizon.sql.DBAccess.TryReturn;
import horizon.sql.support.SQLProc;

/**Executes batch update statements and returns the number of affected rows.
 * <p>For database update, start by getting a {@link DBAccess#batch() Batch from the DBAccess}.
 * </p>
 *
 * <p>The Batch executes either INSERT, UPDATE, DELETE statements or those calling stored procedures.<br />
 * To set an SQL statement, you use
 * <ul><li>{@link #sql(String)} to provide the SQL directly</li>
 * 	   <li>{@link #sqlId(String)} to load the SQL from an sqlsheet</li>
 * </ul>
 * The statement must be supported by the JDBC driver.
 * </p>
 *
 * <p>And the statement's arguments are provided with
 * <ul><li>{@link #params(Object...)} when the parameters are specified with '?' characters</li>
 * 	   <li>{@link #params(Map)} and/or {@link #param(String, Object)} when it has named parameters</li>
 * </ul>
 * Call either of the methods as many times as required.
 * Each call to the method adds the arguments as those for a batch execution.
 * </p>
 *
 * <p>A Batch controls the database connection and transaction automatically while executing statements.
 * </p>
 */
public class Batch extends DBAction {
	private ArrayList<Object[]> params;
	private ArrayList<Map<String, Object>> paramMaps;

	/**Creates a new Batch.
	 * @param dbaccess DBAccess this Batch is associated with
	 */
	public Batch(DBAccess dbaccess) {
		super(dbaccess);
	}

	@Override
	protected Batch setDBAccess(DBAccess dbaccess) {
		super.setDBAccess(dbaccess);
		return this;
	}

	/**Sets the sql statement to work with.<br />
	 * The sql statement must be supported by the JDBC driver.<br />
	 * It may be either of
	 * <ul><li>INSERT, UPDATE, DELETE... to execute an update</li>
	 * 	   <li>{call stored-procedure-name(arguments)} to execute a stored procedure</li>
	 * </ul>
	 * On statement change, the Batch is cleared to the initial state.
	 * @param sql sql statement
	 * @return this Batch
	 */
	@Override
	public Batch sql(String sql) {
		super.sql(sql);
		return this;
	}

	/**Sets the id of the sql instruction in an sqlsheet to work with.
	 * The id must be in the format of "namespace.sql-id".
	 * <p>If, for example, you set "customer.updateCustomer",
	 * the Batch looks up the sql instruction with the id of "updateCustomer" in the "customer" namespace.
	 * </p>
	 * <p>On sqlId change, the Batch is cleared to the initial state.
	 * </p>
	 * @param sqlId id of the sql instruction in an sqlsheet.
	 * @return this Batch
	 */
	@Override
	public Batch sqlId(String sqlId) {
		super.sqlId(sqlId);
		return this;
	}

	/**Sets the value as an argument for the named parameter of the SQL statement.<br />
	 * Use this method when the statement has named parameters
	 * and the value is a shared argument across batch execution.<br />
	 * @param name	parameter name
	 * @param value	parameter value
	 * @return this Batch
	 */
	@Override
	public Batch param(String name, Object value) {
		super.param(name, value);
		return this;
	}

	/**Adds arguments for the parameters of the SQL statement for a batch.<br />
	 * Use this method when the statement is set with {@link #sql(String)}
	 * and its parameters are specified with '?' characters.<br />
	 * The arguments must match the parameters in number and order.<br />
	 * Call this method as many times as required to provide arguments for batch execution.
	 * @param args arguments
	 * @return the Batch
	 */
	@Override
	public Batch params(Object... args) {
		if (params == null)
			params = new ArrayList<>();
		params.add(args);
		return this;
	}

	/**Adds arguments for named parameters of the SQL statement for a batch using the key-value pairs in params.<br />
	 * The keys are used as parameter names and the associated values as arguments.<br />
	 * Use this method when the statement has named parameters.<br />
	 * Call this method as many times as required to provide arguments for batch execution.
	 * @param params key-value pairs for parameter names and values
	 * @return this object
	 */
	@Override
	public Batch params(Map<String, ?> params) {
		if (isEmpty(params)) return this;

		if (paramMaps == null)
			paramMaps = new ArrayList<>();
		paramMaps.add(new DataObject().setAll(params));
		return this;
	}

	@Override
	<T> T execute(TryReturn<T> update) {
		if (update == null)
			return null;

		boolean close = dbaccess.open(),
				commit = dbaccess.transaction().begin();
		try {
			T result = update.attempt(dbaccess);
			if (commit)
				dbaccess.transaction().commit();
			return result;
		} catch (Throwable e) {
			close();
			if (commit)
				dbaccess.transaction().rollback();
			throw runtimeException(e);
		} finally {
			if (close)
				dbaccess.close();
		}
	}

	/**Executes the statement in batches and returns the numbers of affected rows from each execution.
	 * @return numbers of affected rows from each execution
	 */
	public int[] execute() {
		return execute((dbaccess) -> {
			boolean withSQL = isEmpty(sqlId);
			if (withSQL && isEmpty(argMap) && isEmpty(paramMaps)) {
				prepare(false);
				if (!isEmpty(params))
					for (Object[] args: params) {
						super.params(args);
						bind(false);
						pstmt.addBatch();
					}
			} else {
				if (!isEmpty(paramMaps)) {
					if (!isEmpty(argMap))
						paramMaps.forEach(params -> params.putAll(argMap));
				}

				SQLProc sqlproc = withSQL ?
					dbaccess.sqlBuilder().build(sql, paramMaps) :
					dbaccess.sqlBuilder().buildFromInstruction(sqlId, paramMaps);

				setWorkingStatement(sqlproc.getStatement());

				prepare(false);
				sqlproc.getParamEntries().forEach(entries -> {
					try {
						parameters().setEntries(entries);
						bind(false);
						pstmt.addBatch();
					} catch (Exception e) {
						throw runtimeException(e);
					}
				});
			}

			int[] result = pstmt.executeBatch();
			log().debug(() -> "affected row(s): " + Arrays.toString(result));

			return result;
		});
	}

	@Override
	public Batch close() {
		if (params != null)
			params.clear();
		if (paramMaps != null)
			paramMaps.clear();
		super.close();
		return this;
	}
}