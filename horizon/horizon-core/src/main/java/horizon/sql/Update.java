/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import horizon.data.DataObject;
import horizon.sql.DBAccess.TryReturn;
import horizon.sql.support.Instruction;
import horizon.sql.support.Orm;
import horizon.sql.support.SQLProc;

/**Executes update statements and returns the number of affected rows.
 * <p>For database update, start by getting a {@link DBAccess#update() Update from the DBAccess}.
 * </p>
 *
 * <p>The Update executes either INSERT, UPDATE, DELETE statements or those calling stored procedures.<br />
 * To set an SQL statement, you use
 * <ul><li>{@link #sql(String)} to provide the SQL directly</li>
 * 	   <li>{@link #sqlId(String)} to load the SQL from an sqlsheet</li>
 * </ul>
 * The statement must be supported by the JDBC driver.
 * </p>
 *
 * <p>And the statement's arguments are provided with
 * <ul><li>{@link #params(Object...)} when the parameters are specified with '?' characters</li>
 * 	   <li>{@link #param(String, Object)} when it has named parameters</li>
 * </ul>
 * </p>
 *
 * <p>An Update can also {@link #create(Iterable) create}, {@link #update(Iterable) update}, and/or {@link #delete(Iterable) delete} objects
 * so that the corresponding information is INSERTed, UPDATEd, and/or DELETEd in the database.<br />
 * For this to work, the classes of the objects must be specified with an {@code <orm../>} instruction in an sqlsheet.<br />
 * The required SQL statements are generated from the {@code <orm../>} instruction.
 * </p>
 *
 * <p>An Update controls the database connection and transaction automatically while executing statements.
 * </p>
 */
public class Update extends DBAction {
	/**Creates a new Update.
	 * @param dbaccess DBAccess this Update is associated with
	 */
	public Update(DBAccess dbaccess) {
		super(dbaccess);
	}

	/**Sets the sql statement to work with.<br />
	 * The sql statement must be supported by the JDBC driver.<br />
	 * It may be either of
	 * <ul><li>INSERT, UPDATE, DELETE... to execute an update</li>
	 * 	   <li>{call stored-procedure-name(arguments)} to execute a stored procedure</li>
	 * </ul>
	 * @param sql sql statement
	 * @return this Update
	 */
	@Override
	public Update sql(String sql) {
		super.sql(sql);
		return this;
	}

	/**Sets the id of the sql instruction in an sqlsheet to work with.
	 * The id must be in the format of "namespace.sql-id".
	 * <p>If, for example, you set "customer.updateCustomer",
	 * the Update looks up the sql instruction with the id of "updateCustomer" in the "customer" namespace.
	 * </p>
	 * @param sqlId id of the sql instruction in an sqlsheet.
	 * @return this Update
	 */
	@Override
	public Update sqlId(String sqlId) {
		super.sqlId(sqlId);
		return this;
	}

	@Override
	public Update params(Object... args) {
		super.params(args);
		return this;
	}

	@Override
	public Update param(String name, Object value) {
		super.param(name, value);
		return this;
	}

	@Override
	public Update params(Map<String, ?> params) {
		super.params(params);
		return this;
	}

	int process(List<Instruction> instructions) {
		return isEmpty(instructions) ? 0 :
			   dbaccess.sqlBuilder().process(instructions, argMap);
	}

	@Override
	boolean parameterizeInstruction() {
		SQLProc sqlproc = dbaccess.sqlBuilder().preprocess(sqlId, argMap);
		setWorkingStatement(sqlproc.getStatement());
		parameters().setEntries(sqlproc.getCurrentEntries());
		return sqlproc.hasMore();
	}

	private void postprocess() {
		if (isEmpty(sqlId)) return;
		dbaccess.sqlBuilder().postprocess(sqlId, argMap);
	}

	private int doExecute(boolean autoInc) throws Exception {
		if (isEmpty(working)) return 0;

		execute(autoInc);
		return pstmt.getUpdateCount();
	}

	/**Executes the statement and returns the number of affected rows.
	 * @return number of affected rows
	 */
	public int execute() {
		return execute((dbaccess) -> {
			boolean more = preprocess();
			int affected = doExecute(false);
			if (more)
				postprocess();
			log().debug(() -> affected + " row(s) affected");
			return affected;
		});
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

	/**Executes the statement and returns the generated keys.<br />
	 * The statement must be an INSERT statement and the target table must have auto-incremented columns.
	 * @return generated keys
	 */
	public DataObject autoIncKeys() {
		return execute((dbaccess) -> {
			boolean more = preprocess();
			int affected = doExecute(true);
			resultset = pstmt.getGeneratedKeys();

			DataObject keys = resultset != null && resultset.next() ?
				datasetBuilder().getDataObject(resultset) :
				new DataObject();
			clearResult();

			if (more)
				postprocess();
			log().debug(() -> affected + " row(s) affected");

			return keys;
		});
	}

	/**Executes the statement and returns the generated key.<br />
	 * The statement must be an INSERT statement and the target table must have an auto-incremented column.
	 * @return generated key
	 */
	public Number autoInc() {
		DataObject keys = autoIncKeys();
		Map.Entry<String, Object> first = keys.entrySet().iterator().next();
		Object value = first.getValue();
		return (Number)value;
	}

	private static List<Object> listOf(Object... objs) {
		return Arrays.asList(objs);
	}

	/**Executes INSERT statements for objs to create information in the database
	 * and returns the number of affected rows.<br />
	 * If the target table of an object has auto-incremented columns,
	 * the auto-incremented values are set to the corresponding properties of the object.<br />
	 * The classes of objs must be specified with an &lt;orm../> instruction in an sqlsheet.
	 * @param objs objects to save
	 * @return number of affected rows
	 */
	public int create(Object... objs) {
		return isEmpty(objs) ? 0 : create(listOf(objs));
	}

	/**Executes INSERT statements for objs to create information in the database
	 * and returns the number of affected rows.<br />
	 * If the target table of an object has auto-incremented columns,
	 * the auto-incremented values are set to the corresponding properties of the object.<br />
	 * The classes of objs must be specified with an &lt;orm../> instruction in an sqlsheet.
	 * @param objs objects to save
	 * @return number of affected rows
	 */
	public int create(Iterable<?> objs) {
		return isEmpty(objs) ? 0 : save(objs, this::create);
	}

	/**Executes UPDATE statements for objs to update information in the database
	 * and returns the number of affected rows.<br />
	 * The classes of objs must be specified with an &lt;orm../> instruction in an sqlsheet.
	 * @param objs objects to save
	 * @return number of affected rows
	 */
	public int update(Object... objs) {
		return isEmpty(objs) ? 0 : update(listOf(objs));
	}

	/**Executes UPDATE statements for objs to update information in the database
	 * and returns the number of affected rows.<br />
	 * The classes of objs must be specified with an &lt;orm../> instruction in an sqlsheet.
	 * @param objs objects to save
	 * @return number of affected rows
	 */
	public int update(Iterable<?> objs) {
		return isEmpty(objs) ? 0 : save(objs, this::update);
	}

	/**Executes DELETE statements for objs to delete information from the database
	 * and returns the number of affected rows.<br />
	 * The classes of objs must be specified with an &lt;orm../> instruction in an sqlsheet.
	 * @param objs objects to save
	 * @return number of affected rows
	 */
	public int delete(Object... objs) {
		return isEmpty(objs) ? 0 : delete(listOf(objs));
	}

	/**Executes DELETE statements for objs to delete information from the database
	 * and returns the number of affected rows.<br />
	 * The classes of objs must be specified with an &lt;orm../> instruction in an sqlsheet.
	 * @param objs objects to save
	 * @return number of affected rows
	 */
	public int delete(Iterable<?> objs) {
		return isEmpty(objs) ? 0 : save(objs, this::delete);
	}

	@FunctionalInterface
	private interface Persist {
		int exec(Object obj, Orm orm);
	}

	private int save(Iterable<?> objs, Persist persist) {
		return execute(dbaccess -> {
			int affected = 0;
			for (Object obj: objs) {
				if (obj == null) continue;

				Orm orm = Orm.get(obj.getClass(), dbaccess);
				param(orm.objRef(), obj);

				affected += persist.exec(obj, orm);
			}
			return affected;
		});
	}

	private int create(Object obj, Orm orm) {
		int affected = process(orm.getBeforeInserts());

		String objRef = orm.objRef();
		List<Orm.Mapping> autoInc = orm.getAutoInc();
		if (autoInc.isEmpty())
			affected += sql(orm.getInsert()).param(objRef, obj).execute();
		else {
			DataObject keys = sql(orm.getInsert()).param(objRef, obj).autoIncKeys();
			List<Object> values = listOf(keys.values().toArray());
			for (int i = 0; i < autoInc.size(); ++i) {
				Orm.Mapping mapping = autoInc.get(i);
				expr().setValue(
					objRef + "." + mapping.getProperty(),
					values.get(i)
				);
			}
			++affected;
		}

		return affected;
	}

	private int update(Object obj, Orm orm) {
		return process(orm.getBeforeUpdates())
			 + sql(orm.getUpdate()).param(orm.objRef(), obj).execute();
	}

	private int delete(Object obj, Orm orm) {
		return process(orm.getBeforeDeletes())
			 + sql(orm.getDelete()).param(orm.objRef(), obj).execute();
	}

	@Override
	public Update close() {
		super.close();
		return this;
	}
}