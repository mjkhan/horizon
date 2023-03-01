/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql;

import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import horizon.base.Klass;
import horizon.data.DataList;
import horizon.data.DataObject;
import horizon.data.Dataset;
import horizon.sql.support.EXProcessor;
import horizon.sql.support.Orm;

/**Executes query statements and returns the results.
 * <p>For database query, start by getting a {@link DBAccess#query() Query from the DBAccess}.
 * </p>
 *
 * <p>The Query executes either SELECT statements or those calling stored procedures.
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
 * <p>Depending on the sql statements, you can have results of
 * <ul><li>{@link #getDataset() one} or {@link #getDatasets() more} Datasets</li>
 * 	   <li>{@link #getValue() scalar values}</li>
 *     <li>{@link #getObject(ResultFactory) one} or {@link #getObjects(ResultFactory, java.util.function.Function) more} persistent objects</li>
 *     <li>{@link #parameters() OUT parameters of stored procedures}</li>
 * </ul>
 * </p>
 * <p>A Query controls the database connection automatically while executing statements.
 * Depending on the call site, it may be in a transaction context.<br />
 * </p>
 */
public class Query extends DBAction {
	/**Creates an object from a {@link ResultSet}.
	 * @param <T> a Type
	 */
	@FunctionalInterface
	public interface ResultFactory<T> {
		/**Converts the clob to a String.
		 * @param clob a Clob
		 * @return String converted from the clob
		 * @throws Exception
		 */
		static String toString(Clob clob) throws Exception {
			Reader reader = clob.getCharacterStream();
			int read = -1;
			StringBuilder buffer = new StringBuilder();
			while ((read = reader.read()) != -1)
				buffer.append((char)read);
			return buffer.toString();
		}

		/**Creates an object of T from the resultset.
		 * @param resultset a ResultSet
		 * @return an object of T
		 * @throws Exception
		 */
		T create(ResultSet resultset) throws Exception;
	}

	/**Creates a new Query.
	 * @param dbaccess DBAccess this Query is associated with
	 */
	public Query(DBAccess dbaccess) {
		super(dbaccess);
	}

	/**Sets the SQL statement to work with. It is either
	 * <ul><li>SELECT... to execute a query</li>
	 * 	   <li>{call stored-procedure-name(argument-list)} to execute a stored procedure that returns query results</li>
	 * </ul>
	 * @param sql sql statement
	 * @return this Query
	 */
	@Override
	public Query sql(String sql) {
		super.sql(sql);
		return this;
	}

	/**Sets the id of the {@code <query../>} instruction in an sqlsheet to work with.
	 * The id must be in the format of "namespace.instruction-id".
	 * <p>If, for example, you set "customer.searchCustomers",
	 * the Query looks up the sql instruction with the id of "searchCustomers" in the "customer" namespace.
	 * </p>
	 * @param sqlId id of the sql instruction in an sqlsheet.
	 * @return this Query
	 */
	@Override
	public Query sqlId(String sqlId) {
		super.sqlId(sqlId);
		return this;
	}

	@Override
	public Query params(Object... args) {
		super.params(args);
		return this;
	}

	@Override
	public Query param(String name, Object value) {
		super.param(name, value);
		return this;
	}

	@Override
	public Query params(Map<String, ?> params) {
		super.params(params);
		return this;
	}

	private PreparedStatement getResults() throws Exception {
		preprocess();
		if (isEmpty(working)) return null;

		return execute(false) ? pstmt : null;
	}

	private ResultSet getResultSet() throws Exception {
		PreparedStatement pstmt = getResults();
		return pstmt != null ? resultset = pstmt.getResultSet() : null;
	}

	private <T> T getResult(ResultFactory<T> factory) {
		return execute((dbaccess) -> {
			T result = factory.create(getResultSet());
			clearResult();
			return result;
		});
	}

	/**Executes the statement and returns the ResultSet in a Dataset.<br />
	 * @return Dataset
	 */
	public Dataset getDataset() {
		Dataset dataset = getResult(datasetBuilder()::getDataset);
		log().debug(() -> dataset.size() + " row(s) SELECTed");
		return dataset;
	}

	/**Calls the {@link #getDataset()} method and returns the first column value of the Dataset's first row.<br />
	 * The intention is to help get a scalar value from the query result.
	 * @return
	 * <ul><li>first column value of the Dataset's first row</li>
	 * 	   <li>null if the dataset is empty</li>
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	public <T> T getValue() {
		Dataset dataset = getDataset();
		if (dataset.isEmpty()) return null;

		DataObject row = dataset.get(0);
		return (T)row.values().iterator().next();
	}

	/**Executes the statement and returns the results in Datasets.<br />
	 * The statement must return 1 or more Datasets(i.e. ResultSets).
	 * @return list of Datasets
	 */
	public List<Dataset> getDatasets() {
		return execute((dbaccess) -> {
			getResults();
			List<Dataset> datasets = parser().setStatement(pstmt).getDatasets();
			boolean debug = log().getLogger().isDebugEnabled();
			if (debug) {
				int count = datasets.size();
				StringBuilder buffer = new StringBuilder(count + " Dataset(s) returned");
				if (count > 0) {
					buffer.append(" each with ");
					for (int i = 0; i < count; ++i) {
						buffer.append(datasets.get(i).size());
						if (i < count - 1) buffer.append(", ");
					}
					buffer.append(" row(s)");
				}
				log().debug(buffer.toString());
			}
			clearResult();
			return datasets;
		});
	}

	/**Executes statements for stored procedures that returns no ResultSets but only OUT parameters.
	 */
	public void execute() {
		execute((dbaccess) -> {
			getResults();
			log().trace(() -> "Statement executed");
			return null;
		});
	}

	private <T> void restore(ResultSet rs, T obj) {
		if (obj == null) return;

		Orm orm = Orm.get(obj.getClass());
		String objRef = orm.objRef();
		EXProcessor exproc = expr().setBean(objRef, obj);

		orm.getMappings().forEach(mapping -> {
			try {
				Object value = rs.getObject(mapping.getColumn());
				if (value instanceof Clob) {
					value = ResultFactory.toString((Clob)value);
				} else if (value instanceof Blob) {
					value = rs.getBytes(mapping.getColumn());
				}
				exproc.setValue(objRef + "." + mapping.getProperty(), value);
			} catch (Exception e) {
				throw runtimeException(e);
			}
		});

	}

	private <T> ResultFactory<? extends T> resultSupplier() {
		Class<?> klass = sqlproc != null ? sqlproc.getResultType() : null;
		return resultSupplier(klass);
	}

	@SuppressWarnings("unchecked")
	private <T> ResultFactory<? extends T> resultSupplier(Class<?> klass) {
		if (klass == null)
			throw new RuntimeException("Unable to determine the resultType");
		return (rs) -> {
			return (T)Klass.instance(klass);
		};
	}

	/**Executes the statement and returns a DataList of objects created from the result.<br />
	 * Use the method to return a result with pagination information.<br />
	 *
	 * <p>In populating the result list, the factory returns a new instance of the class for the list's elements.<br />
	 * The Query then sets the values to each instance.
	 * </p>
	 *
	 * <p>Just before returning the result, the Query invokes the atLast function to get a {@link horizon.data.DataList.Fetch} for pagination information.<br />
	 * For the DataList.Fetch, you may use values of the DataObject or execute the Query again for further information.
	 * </p>
	 * @param <T>		class of the returned objects. Must be specified with an {@code <orm../>} instruction in an sqlsheet.
	 * @param factory	function that returns a new instance of a class
	 * @param atLast	function that returns pagination information in a DataList.Fetch
	 * @return DataList of objects created from the result
	 */
	public <T> DataList<T> getObjects(ResultFactory<? extends T> factory, Function<DataObject, DataList.Fetch> atLast) {
		DataObject lastRow = atLast != null ? new DataObject() : null;
		return execute(dbaccess -> {
			DataList<T> result = getResult(rs -> {
				DataList<T> list = new DataList<>();
				ResultFactory<? extends T> resultSupplier = ifEmpty(factory, this::resultSupplier);
				while (rs != null && rs.next()) {
					T t = resultSupplier.create(rs);
					if (isEmpty(t)) continue;

					restore(rs, t);
					list.add(t);
					setLastRow(rs, lastRow);
				}
				log().debug(() -> list.size() + " row(s) SELECTed");
				return list;
			});

			if (result.isEmpty() || atLast == null)
				return result;

			DataList.Fetch fetch = atLast.apply(lastRow);
			if (fetch != null)
				fetch.set(result);
			return result;
		});
	}

	private void setLastRow(ResultSet resultset, DataObject dataobject) throws Exception {
		if (dataobject == null) return;

		ResultSetMetaData meta = resultset.getMetaData();
		int count = meta.getColumnCount();
		for (int i = 1; i <= count; ++i) {
			String name = meta.getColumnName(i);
			dataobject.put(name, resultset.getObject(name));
		}
	}

	/**Executes the statement and returns a List of objects created from the result.<br />
	 *
	 * <p>In populating the result list, the factory returns a new instance of the class for the list's elements.<br />
	 * The Query then sets the values to each instance.
	 * </p>
	 * @param <T>		class of the returned objects. Must be specified with an {@code <orm../>} instruction in an sqlsheet.
	 * @param factory	function that returns a new instance of a class
	 * @return List of objects created from the result
	 */
	public <T> List<T> getObjects(ResultFactory<? extends T> factory) {
		return getObjects(factory, null);
	}

	/**Executes the statement and returns a DataList of objects created from the result.<br />
	 * Use the method to return a result with pagination information.
	 *
	 * <p>The statement is set with {@link #sqlId(String)}.<br />
	 * And the referenced {@code <query../>} instruction in an sqlsheet must have either the 'resultType' attribute or the 'resultAlias' attribute.<br />
	 * And, in turn, the class referenced by resultType or resultAlias must be specified with {@code <orm../>} instruction.<br />
	 * The Query populates the result list with instances of the class determined with the above information.
	 * </p>
	 *
	 * <p>Just before returning the result, the Query invokes the atLast function to get a {@link horizon.data.DataList.Fetch} for pagination information.<br />
	 * For the DataList.Fetch, you may use values of the DataObject or execute the Query again for further information.
	 * </p>
	 * @param <T> class of the returned objects. Must be specified with an {@code <orm../>} instruction in an sqlsheet.
	 * @param atLast function that returns pagination information in a DataList.Fetch
	 * @return DataList of objects created from the result
	 */
	public <T> DataList<T> fetchObjects(Function<DataObject, DataList.Fetch> atLast) {
		return getObjects((ResultFactory<? extends T>)null, atLast);
	}

	/**Executes the statement and returns a list of objects created from the result.
	 *
	 * <p>The statement is set with {@link #sqlId(String)}.<br />
	 * And the referenced {@code <query../>} instruction in an sqlsheet must have either the 'resultType' attribute or the 'resultAlias' attribute.<br />
	 * And, in turn, the class referenced by resultType or resultAlias must be specified with {@code <orm../>} instruction.<br />
	 * The Query populates the result list with instances of the class determined with the above information.
	 * </p>
	 * @param <T> class of the returned objects. Must be specified with an {@code <orm../>} instruction in an sqlsheet.
	 * @return List of objects created from the result
	 */
	public <T> List<T> getObjects() {
		return getObjects((ResultFactory<? extends T>)null);
	}

	/**Executes the statement and returns an object created from the result.
	 * <p>The factory returns a new instance of the class depending on the values of the ResultSet's current row.<br />
	 * The Query then sets the values to the instance.
	 * </p>
	 * @param <T> class of the returned object. Must be specified with an {@code <orm../>} instruction in an sqlsheet.
	 * @param factory	factory that creates a new instance of a class
	 * @return object created from the result
	 */
	public  <T> Optional<T> getObject(ResultFactory<? extends T> factory) {
		List<T> list = getObjects(factory);
		switch (list.size()) {
		case 0: return Optional.empty();
		case 1:
			T t = list.get(0);
			return t != null ? Optional.of(t) : Optional.empty();
		default: throw new RuntimeException("Multiple rows returned from the query");
		}
	}

	/**Executes the statement and returns an object created from the result.<br />
	 * <p>The statement is set with {@link #sqlId(String)}.<br />
	 * And the referenced {@code <query../>} instruction in an sqlsheet must have either the 'resultType' attribute or the 'resultAlias' attribute.<br />
	 * And, in turn, the class referenced by resultType or resultAlias must be specified with {@code <orm../>} instruction.<br />
	 * </p>
	 * The Query sets the values of the instance of the class determined with the above information.
	 * @param <T> class of the returned object. Must be specified with an {@code <orm../>} instruction in an sqlsheet.
	 * @return an object created from the result
	 */
	public <T> Optional<T> getObject() {
		return getObject((ResultFactory<? extends T>)null);
	}

	/**Generates and executes a SELECT statement with conditional clause for PK(primary key) columns and returns an object created from the result.<br />
	 * <p>You do not set an sql or an sqlId, but the class of the returned object must be specified with an {@code <orm../>} instruction in an sqlsheet.<br />
	 * As for PK columns, provide values for corresponding properties as the statement's named parameters.<br >
	 * <p>If, for example, you have an orm instruction below:
	 * <pre><code> &lt;orm type="MyClass" alias="myclass" table="MY_TABLE">
	 *     &lt;mapping property="pkProperty0" column="PK_COL0">&lt;/mapping>
	 *     &lt;mapping property="pkProperty1" column="PK_COL1">&lt;/mapping>
	 *     ...
	 * &lt;/orm>
	 * </code></pre>
	 * You have the Query generate a SELECT statement and get the result like this:
	 * <pre><code> Optional&lt;MyClass> myobject = dbaccess.query()
	 *     .param("pkProperty0", "pkPropertyValue0")
	 *     .param("pkProperty1", "pkPropertyValue1")
	 *     .getObject(MyClass.class);
	 * </code></pre>
	 * </p>
	 * @param <T> class of the returned object. Must be specified with an {@code <orm../>} instruction in an sqlsheet.
	 * @param klass class of the returned object
	 * @return object created from the result
	 */
	public <T> Optional<T> getObject(Class<T> klass) {
		Map<String, Object> tmp = argMap;
		return execute(db -> {
			argMap = null;
			sql(Orm.get(klass, dbaccess).getSelect());
			argMap = tmp;
			return getObject(resultSupplier(klass));
		});
	}

	/**Returns a list of objects created from the dataset.<br />
	 * A new instance of each object is provided by the factory.
	 * @param <T> class of the returned objects. Must be specified with an {@code <orm../>} instruction in an sqlsheet.
	 * @param dataset	Dataset
	 * @param factory	function that returns a new instance of the class
	 * @return list of objects created from the dataset
	 */
	public <T> List<T> getObjects(Dataset dataset, Function<DataObject, T> factory) {
		if (isEmpty(dataset) || factory == null) return Collections.emptyList();

		EXProcessor exproc = expr();
		ArrayList<T> result = new ArrayList<>();
		for (DataObject row: dataset) {
			T obj = factory.apply(row);
			if (obj == null) continue;

			Orm orm = Orm.get(obj.getClass());
			String objRef = orm.objRef();
			Collection<Orm.Mapping> mappings = orm.getMappings();
			exproc.setBean(objRef, obj);

			mappings.forEach(mapping -> {
				try {
					Object value = row.get(mapping.getColumn());
					exproc.setValue(objRef + "." + mapping.getProperty(), value);
				} catch (Exception e) {
					throw runtimeException(e);
				}
			});

			result.add(obj);
		}
		return result;
	}

	/**Returns an object created from the dataobject.
	 * @param <T> class of the returned object. Must be specified with an {@code <orm../>} instruction in an sqlsheet
	 * @param dataobject A DataObject that has values for an object's properties
	 * @param factory supplier that returns a new instance of a class
	 * @return object created from the dataobject
	 */
	public <T> Optional<T> getObject(DataObject dataobject, Function<DataObject, T> factory) {
		if (isEmpty(dataobject) || factory == null) return Optional.empty();

		Dataset dataset = new Dataset();
		dataset.add(dataobject);
		T t = getObjects(dataset, factory).get(0);
		return Optional.of(t);
	}

	@Override
	public Query close() {
		super.close();
		return this;
	}
}