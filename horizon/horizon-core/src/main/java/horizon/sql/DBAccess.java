/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql;

import java.io.InputStream;
import java.sql.Connection;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import javax.sql.DataSource;
import javax.transaction.Status;

import horizon.base.AbstractComponent;
import horizon.base.Log;
import horizon.sql.support.SQLBuilder;
import horizon.sql.support.SQLSheet;

/**Provides access to a relational database(or sql database)
 * so that you can query and/or update data in the database.
 *
 * <h2>Getting a DBAccess</h2>
 * You get a DBAccess with the name of connection information defined in a <a href="{@docRoot}/horizon/sql/package-summary.html#configuration">configuration</a> file.<br />
 * By default, a DBAccess loads the configuration file 'dbaccess.xml' from classpath.
 * <pre><code> DBAccess dbaccess = {@link DBAccess#setConnectionName(String) new DBAccess().setConnectionName("connectionName")};</code></pre>
 * To change the location of the configuration file, use
 * <pre><code> DBAccess dbaccess = new DBAccess()
 *     .{@link #setConfigLocation(String) setConfigLocation("location of the dbaccess.xml")}
 *     .{@link #setConnectionName(String) setConnectionName("connectionName")};
 * </code></pre>
 * </p>
 *
 * <h2>Performing queries and/or updates</h2>
 * <p>To perform database queries, use a Query from a DBAccess.
 * <pre><code> private void doQueries() {
 *     Dataset dataset = dbaccess.{@link #query() query()}.sql("SELECT * ...").params(...).getDataset();
 *     List&lt;Dataset> datasets = dbaccess.query().sqlId("...").param(...).getDatasets();
 *     Number number = dbaccess.query().sql("SELECT COUNT(*) ...").params(...).getValue();
 * }</code></pre>
 * Each call for SQL execution automatically opens and closes a database connection.
 * </p>
 * <p>To execute updates, use an Update from a DBAccess.
 * <pre><code> private void doUpdates() {
 *     int affected = dbaccess.{@link #update() update()}.sql("UPDATE ...").params(...).execute();
 *     DataObject generatedKeys = dbaccess.update().sqlId("...").params(...).autoInc();
 * }</code></pre>
 * Each call for SQL execution automatically opens and closes a database connection and starts and commits a database transaction.
 *
 * <h2>Connection {@literal &} Transaction</h2>
 * <p>You can rewrite the query example above to have the SQL execution share a database connection as below:
 * <pre><code> private void doQueries() {
 *     dbaccess.{@link #perform(Try) perform}(db -> {
 *       Dataset dataset = db.query().sql("SELECT * ...").params(...).getDataset();
 *       List&lt;Dataset> datasets = db.query().sqlId("...").param(...).getDatasets();
 *       Number number = db.query().sql("SELECT COUNT(*) ...").params(...).getValue();
 *     });
 * }</code></pre>
 * which is equivalent to
 * <pre><code> private void doQueries() {
 *     <b>boolean close = dbaccess.{@link #open() open()};</b>
 *     Dataset dataset = dbaccess.query().sql("SELECT * ...").params(...).getDataset();
 *     List&lt;Dataset> datasets = dbaccess.query().sqlId("...").param(...).getDatasets();
 *     Number number = dbaccess.query().sql("SELECT COUNT(*) ...").params(...).getValue();
 *     <b>if (close)
 *         dbaccess.{@link #close() close()};</b>
 * }</code></pre>
 * </p>
 * <p>You can also rewrite the update example above to have the SQL execution share a database connection and transaction as below:
 * <pre><code> private void doUpdates() {
 *     dbaccess.{@link #transact(Try) transact}(db -> {
 *         int affected = db.update().sql("UPDATE ...").params(...).execute();
 *         Number autoInc = db.update().sqlId("...").params(...).autoInc();
 *     });
 * }</code></pre>
 * which is equivalent to
 * <pre><code> private void doUpdates() {
 *         <b>boolean close = dbaccess.open();
 *        boolean commit = dbaccess.{@link #transaction() transaction()}.begin();</b>
 *     try {
 *         int affected = dbaccess.update().sql("UPDATE ...").params(...).execute();
 *         Number autoInc = dbaccess.update().sqlId("...").params(...).autoInc();
 *         <b>if (commit)
 *             dbaccess.transaction().commit();</b>
 *     } catch (Exception e) {
 *         <b>if (commit)
 *             dbaccess.transaction().rollback();</b>
 *         throw e;
 *     } finally {
 *         <b>if (close)
 *             dbaccess.close();</b>
 *     }
 * }</code></pre>
 * </p>
 * <p>To have another method call the example methods of the simpler versions, you write:
 * <pre><code> public void doWork() {
 *     dbaccess.transact(db -> {
 *         doQueries();
 *         doUpdates();
 *     });
 * }</code></pre>
 * <p>While a DBAccess is open, the {@link Connection} to the database and the transaction
 * are shared by other methods and objects in the current thread.
 * </p>
 * </p>
 * <p>A DBAccess has no mechanism of database connection pool.<br />
 * It assumes that the DataSource it obtains or the JDBC driver somehow provides it and works with the {@link Connection} returned.
 * </p>
 */
public class DBAccess extends AbstractComponent {
	private static final ThreadLocal<MapSupport> resMap = new ThreadLocal<>();

	static MapSupport resources() {
		MapSupport map = resMap.get();
		if (map == null) {
			resMap.set(map = new MapSupport());
		}
		return map;
	}

	/**Is an operation using a DBAccess.
	 */
	public static interface Try extends horizon.base.Try<DBAccess, Throwable> {}

	/**Is an operation using a DBAccess that returns a result.
	 * @param <T> a result type
	 */
	public static interface TryReturn<T> extends horizon.base.TryReturn<DBAccess, T, Throwable> {}

	private String
		configLocation,
		connectionName,
		catalog,
		schema,

		key,
		sqlBuilder,
		dbActions,
		query,
		update,
		batch,
		resetAutocommit;
	private DataSource datasource;

	private Transaction.Factory transactionFactory;

	/**Sets the location of the configuration file,
	 * which may be either from the classpath or from the file system.
	 * @param configLocation location of the configuration file
	 * @return this DBAccess
	 */
	public DBAccess setConfigLocation(String configLocation) {
		if (!equals(this.configLocation, configLocation)) {
			this.configLocation = configLocation;
			setConnectionName(null);
		}
		return this;
	}

	/**Returns the ConfigControl of DBAccess.
	 * @return ConfigControl of DBAccess
	 */
	public static ConfigControl config() {
		return ConfigControl.conf;
	}

	private String key() {
		if (key == null) {
			if (!isEmpty(connectionName))
				key = ifEmpty(configLocation, () -> DatasourceFactory.DEFAULT_LOCATION) + "#" + connectionName;
			else {
				if (datasource != null)
					key = "datasource#" + datasource.hashCode();
			}
			if (key != null) {
				sqlBuilder = key + "#sqlBuilder";
				dbActions = key + "#dbActions";
				query = key + "#query";
				update = key + "#update";
				batch = key + "#batch";
				resetAutocommit = key + "#resetAutocommit";
			} else
				log().warn(() -> "Unable to determine the key for the DBAccess' Connection");
		}
		return key;
	}

	/**Sets the name of connection information in the configuration.<br />
	 * Uses the named information to obtain a DataSource.
	 * @param connectionName name of connection information in the configuration
	 * @return this DBAccess
	 */
	public DBAccess setConnectionName(String connectionName) {
		if (!equals(this.connectionName, connectionName)) {
//			close();
			this.connectionName = connectionName;
			sqlBuilder = dbActions = query = update = batch = resetAutocommit = key = null;
			key();
		}
		return this;
	}

	/**Returns the catalog name.
	 * @return the catalog name
	 */
	public String getCatalog() {
		return catalog;
	}

	/**Sets the catalog name.<br />
	 * Use this method to set the catalog name if the JDBC driver does not support {@link Connection#getCatalog()}.
	 * @param catalog catalog name
	 * @return this DBAccess
	 */
	public DBAccess setCatalog(String catalog) {
		this.catalog = catalog;
		return this;
	}

	/**Returns the schema name.
	 * @return the schema name
	 */
	public String getSchema() {
		return schema;
	}

	/**Sets the schema name.<br />
	 * Use this method to set the schema name if the JDBC driver does not support {@link Connection#getSchema()}.
	 * @param schema schema name
	 * @return this DBAccess
	 */
	public DBAccess setSchema(String schema) {
		this.schema = schema;
		return this;
	}

	/**Returns the DataSource the DBAccess is associated with.
	 * @return DataSource the DBAccess is associated with
	 */
	public DataSource getDatasource() {
		if (datasource != null)
			return datasource;

		datasource = DatasourceFactory.get(configLocation, connectionName);
		DatasourceFactory.Config config = DatasourceFactory.config(connectionName);
		catalog = config.getCatalog();
		schema = config.getSchema();

		return datasource;
	}

	/**Sets the datasource to associate with the DBAccess.
	 * @param datasource DataSource to associate with the DBAccess
	 * @return this DBAccess
	 */
	public DBAccess setDatasource(DataSource datasource) {
		if (!equals(this.datasource, datasource)) {
//			close();
			this.datasource = datasource;
			sqlBuilder = dbActions = query = update = batch = resetAutocommit = key = configLocation = connectionName = null;
			key();
		}
		return this;
	}

	Transaction.Factory getTransactionFactory() {
		return transactionFactory;
	}

	/**Sets a TransactionFactory to obtain a {@link Transaction}.
	 * @param transactionFactory Transaction.Factory to obtain a Transaction
	 * @return this DBAccess
	 */
	public DBAccess setTransactionFactory(Transaction.Factory transactionFactory) {
		this.transactionFactory = transactionFactory;
		return this;
	}

	/**Returns a Transaction.<br />
	 * Bound to the current thread, the Transaction is available for other objects in the current thread.
	 * @return Transaction
	 */
	public Transaction transaction() {
		return Transaction.get(this);
	}

	SQLBuilder sqlBuilder() {
		return resources().getIfAbsent(sqlBuilder, key -> new SQLBuilder().setDBAccess(this));
	}

	void add(DBAction dbaction) {
		HashSet<DBAction> dbActions = resources().getIfAbsent(this.dbActions, key -> new HashSet<>());
		dbActions.add(dbaction);
	}

	/**Returns a Query for database query.
	 * @param reset
	 * <ul><li>true to reset the Query to the initial state</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 * @return Query for database query
	 */
	public Query query(boolean reset) {
		Query query = resources().getIfAbsent(this.query, key -> new Query(this));
		return reset ? query.close() : query;
	}

	/**Returns a Query for database query.<br />
	 * Equivalent to {@link #query(boolean) query(false)}.
	 * @return Query for database query
	 */
	public Query query() {
		return query(false);
	}

	/**Returns an Update for database update.
	 * @param reset
	 * <ul><li>true to reset the Update to the initial state</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 * @return Update for database update
	 */
	public Update update(boolean reset) {
		Update update = resources().getIfAbsent(this.update, key -> new Update(this));
		return reset ? update.close() : update;
	}

	/**Returns an Update for database update.<br />
	 * Equivalent to {@link #update(boolean) update(false)}.
	 * @return Update for database update
	 */
	public Update update() {
		return update(false);
	}

	/**Returns a Batch for batch operation against the database.
	 * @return Batch for batch operation against the database
	 */
	public Batch batch() {
		return resources().getIfAbsent(batch, key -> new Batch(this));
	}

	/**Returns the connection to the database.
	 * @return connection to the database
	 */
	public Connection getConnection() {
		return isOpen() ? connection() : null;
	}

	Connection connection() {
		return Support.get(getDatasource(), key());
	}

	/**Returns whether the DBAccess is open or connected to the database.
	 * @return
	 * <ul><li>true if the DBAccess is open or connected to the database</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 */
	public boolean isOpen() {
		return Support.isOpen(key());
	}

	static ExceptionHolder hold(Throwable e) {
		return ExceptionHolder.get(e);
	}

	/**Opens the database connection.<br />
	 * While open, the {@link Connection} is shared by objects in the current thread.
	 * @return
	 * <ul><li>true if the call to the method opens the database connection</li>
	 * 	   <li>false if the database connection is already open</li>
	 * </ul>
	 */
	public boolean open() {
		if (isOpen()) return false;

		Connection connection = connection();
		try {
			boolean resetAutocommit = connection.getAutoCommit();
			resources().put(this.resetAutocommit, resetAutocommit);
			if (resetAutocommit)
				connection.setAutoCommit(false);
			return true;
		} catch (Throwable e) {
			throw runtimeException(e);
		}
	}

	/**Closes the database connection.
	 */
	public void close() {
		if (!isOpen()) return;

		resources().remove(query);
		resources().remove(update);
		resources().remove(batch);
		HashSet<DBAction> dbActions = resources().remove(this.dbActions);
		if (dbActions != null)
			for (DBAction dbaction: dbActions)
				dbaction.close().dbaccess = null;

		SQLBuilder sqlBuilder = resources().remove(this.sqlBuilder);
		if (sqlBuilder != null)
			sqlBuilder.close();

		boolean resetAutocommit = resources().remove(this.resetAutocommit);
		Support.close(key(), resetAutocommit);

		if (resources().isEmpty()) {
			resMap.remove();
			Log.get(DBAccess.class).trace(() -> "DBAccess resources released");
		}
	}

	/**Equivalent to call {@link #perform(Try, BiConsumer) perform(task, null)}.
	 */
	public void perform(Try task) {
		perform(task, null);
	}

	/**Performs the task.
	 * <p>Before the task execution, opens the DBAccess if not yet open.<br />
	 * After the task execution, closes the DBAccess if opened by the current method call.
	 * </p>
	 * <p>If the task throws an exception, the onException is called to handle the exception.<br />
	 * If no onException is provided, the exception is rethrown wrapped in a RuntimeException.<br />
	 * Use the <code>RuntimeException.getCause()</code> method to get the actual exception.
	 * </p>
	 * <p>You may use this method either in or out of a transaction context,<br />
	 * but it is advised to have the task to be in the nature of the SELECT statement.</p>
	 * @param task task to perform
	 * @param onException exception handler
	 */
	public void perform(Try task, BiConsumer<DBAccess, Throwable> onException) {
		if (task == null) return;
		if (!ExceptionHolder.isEmpty()) return;

		boolean close = open();
		try {
			task.attempt(this);
		} catch (Throwable e) {
			ExceptionHolder eh = hold(e);
			if (!eh.isHandled() && onException != null) {
				onException.accept(this, eh.getCause());
				eh.setHandled();
			}
			if (!eh.isHandled())
				throw eh;
		} finally {
			if (close)
				close();
		}
	}

	/**Equivalent to call {@link #perform(TryReturn, BiFunction) perform(task, null)}.
	 */
	public <T> T perform(TryReturn<? extends T> task) {
		return perform(task, null);
	}

	/**Performs the task returning a result.
	 * <p>Before the task execution, opens the DBAccess if not yet open.<br />
	 * After the task execution, closes the DBAccess if opened by the current method call.
	 * </p>
	 * <p>If the task throws an exception, the onException is called to handle the exception.<br />
	 * If no onException is provided, the exception is rethrown wrapped in a RuntimeException.<br />
	 * Use the <code>RuntimeException.getCause()</code> method to get the actual exception.
	 * </p>
	 * <p>You may use this method either in or out of a transaction context,<br />
	 * but it is advised to have the task to be in the nature of the SELECT statement.</p>
	 * @param task task to perform
	 * @param onException exception handler
	 * @return result of the task
	 */
	public <T> T perform(TryReturn<? extends T> task, BiFunction<DBAccess, Throwable, ? extends T> onException) {
		if (task == null) return null;
		if (!ExceptionHolder.isEmpty()) return null;

		boolean close = open();
		try {
			return task.attempt(this);
		} catch (Throwable e) {
			ExceptionHolder eh = hold(e);
			if (!eh.isHandled() && onException != null) {
				T t = onException.apply(this, eh.getCause());
				eh.setHandled();
				return t;
			}
			if (!eh.isHandled())
				throw eh;
			return null;
		} finally {
			if (close)
				close();
		}
	}

	/**Equivalent to call {@link #transact(Try, BiConsumer) transact(task, null)}.
	 */
	public void transact(Try task) {
		transact(task, null);
	}

	/**Performs the task in a transaction context.
	 * <p>Before the task execution, opens the DBAccess if not yet open
	 * and starts a transaction if not yet started.<br />
	 * After the task execution, commits the transaction if started by the current method call.
	 * Then closes the DBAccess if opened by the current method call.
	 * </p>
	 * <p>If the task throws an exception, the onException is called to handle the exception.<br />
	 * If no onException is provided, the exception is rethrown wrapped in a RuntimeException.<br />
	 * Use the <code>RuntimeException.getCause()</code> method to get the actual exception.<br />
	 * If you have methods nested and some of them calling this method,
	 * it is advised to provide an onException for the outer most <code>transact(...)</code>.<br />
	 * And the task is rolled back in the end.
	 * </p>
	 * @param task task to perform
	 * @param onException exception handler
	 */
	public void transact(Try task, BiConsumer<DBAccess, Throwable> onException) {
		if (task == null) return;
		if (!ExceptionHolder.isEmpty()) return;

		boolean close = open(),
				commit = transaction().begin();
		try {
			task.attempt(this);
			if (commit)
				transaction().commit();
		} catch (Throwable e) {
			ExceptionHolder eh = hold(e);
			if (!eh.isHandled() && onException != null) {
				onException.accept(this, eh.getCause());
				eh.setHandled();
			}
			if (commit)
				transaction().rollback();
			if (!eh.isHandled())
				throw eh;
		} finally {
			if (close)
				close();
		}
	}

	/**Equivalent to call {@link #transact(TryReturn, BiFunction) transact(task, null)}.
	 */
	public <T> T transact(TryReturn<? extends T> task) {
		return transact(task, null);
	}

	/**Performs the task returning a result in a transaction context.
	 * <p>Before the task execution, opens the DBAccess if not yet open
	 * and starts a transaction if not yet started.<br />
	 * After the task execution, commits the transaction if started by the current method call.
	 * Then closes the DBAccess if opened by the current method call.
	 * </p>
	 * <p>If the task throws an exception, the onException is called to handle the exception.<br />
	 * If no onException is provided, the exception is rethrown.<br />
	 * Use the <code>RuntimeException.getCause()</code> method to get the actual exception.<br />
	 * If you have methods nested and some of them calling this method,<br />
	 * it is advised to provide an onException for the outer most <code>transact(...)</code>.<br />
	 * And the task is rolled back in the end.
	 * </p>
	 * @param task task to perform
	 * @param onException exception handler
	 * @return result of the task
	 */
	public <T> T transact(TryReturn<? extends T> task, BiFunction<DBAccess, Throwable, ? extends T> onException) {
		if (task == null) return null;
		if (!ExceptionHolder.isEmpty()) return null;

		boolean close = open(),
				commit = transaction().begin();
		try {
			T t = task.attempt(this);
			if (commit)
				transaction().commit();
			return t;
		} catch (Throwable e) {
			ExceptionHolder eh = hold(e);
			if (!eh.isHandled() && onException != null) {
				T t = onException.apply(this, e);
				eh.setHandled();
				if (commit)
					transaction().rollback();
				return t;
			}
			if (commit)
				transaction().rollback();
			if (!eh.isHandled())
				throw eh;
			return null;
		} finally {
			if (close)
				close();
		}
	}

	@Override
	public String toString() {
		return String.format("%s@%d(\"%s\")", getClass().getSimpleName(), hashCode(), key());
	}

	/**Constrols the DBAccess configuration loading.
	 */
	public static class ConfigControl {
		private static ConfigControl conf = new ConfigControl();

		private ConfigControl() {}

		public ConfigControl location(String configLocation) {
			DatasourceFactory.get(configLocation);
			return this;
		}

		/**Loads the sqlsheet configurations from the inputs.
		 * @param inputs InputStreams for the sqlsheet configuration
		 * @return this ConfigControl
		 */
		public ConfigControl sqlSheetLocations(List<InputStream> inputs) {
			if (!isEmpty(inputs))
				inputs.forEach(SQLSheet::load);
			return this;
		}

		/**Clears up the configurations to the initial state.
		 * @return this ConfigControl
		 */
		public ConfigControl clear() {
			SQLSheet.clear();
			DatasourceFactory.clear();
			return this;
		}
	}

	private static class Support {
		static Connection connection(String key) {
			return !isEmpty(key) ? resources().get(key) : null;
		}

		static boolean isOpen(String key) {
			return connection(key) != null;
		}

		static Connection get(DataSource datasource, String key) {
			if (isEmpty(key) || datasource == null) return null;

			Connection connection = connection(key);
			if (connection == null)
				try {
					resources().put(key, connection = datasource.getConnection());
					Log.get(DBAccess.class).debug(() -> "========== DBAccess('" + key + "') opened ==========");
				} catch (Exception e) {
					throw runtimeException(e);
				}

			return connection;
		}

		static Connection close(String key, boolean resetAutocommit) {
			if (!isOpen(key)) return null;

			try {
				Connection connection = resources().remove(key);
				if (connection == null) return null;
				if (connection.isClosed()) return connection;

				switch (Transaction.release()) {
				case Status.STATUS_MARKED_ROLLBACK:
				case Status.STATUS_ROLLING_BACK:
				case Status.STATUS_ROLLEDBACK: break;
				default:
					reset(connection, resetAutocommit);
					break;
				}

				ExceptionHolder.clear();
				connection.close();
				if (connection.isClosed())
					Log.get(DBAccess.class).debug(() -> "========== DBAccess('" + key + "') closed ==========");
				return connection;
			} catch (Exception e) {
				throw runtimeException(e);
			}
		}

		private static void reset(Connection connection, boolean resetAutocommit) {
			try {
				if (resetAutocommit)
					connection.setAutoCommit(true);
			} catch (Exception e) {
				Log.get(DBAccess.class).trace(() -> rootCause(e).getMessage());
			}
		}
	}
}