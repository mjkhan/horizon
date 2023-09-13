/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql.support;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import horizon.base.AbstractComponent;
import horizon.base.Assert;
import horizon.data.DataObject;
import horizon.data.StringMap;
import horizon.sql.DBAccess;

/**Meta information on a table.<br />
 * A Table describes a table in a database with information such as
 * <ul><li>{@link #name() name} of the Columns</li>
 * 	   <li>{@link #get(Object) Columns} for the Table</li>
 * </ul>
 */
public class Table extends StringMap<Column> {
	private static final long serialVersionUID = 1L;
	private static final StringMap<Table> cache = new StringMap<>();

	public static Table get(String name, DBAccess dbaccess) {
		Table table = cache.get(name);
		if (table == null)
			try {
				cache.put(name, table = new Builder().create(dbaccess, name));
			} catch (Exception e) {
				throw Assert.runtimeException(e);
			}
		return table;
	}

	private String name;

	/**Creates a new Table.*/
	public Table() {
		caseSensitiveKey(false);
	}

	/**Creates a new Table with the name.
	 * @param name name of the Table
	 */
	public Table(String name) {
		setName(name);
	}

	/**Returns the Table's name.
	 * @return the Table's name
	 */
	public String name() {
		return name;
	}

	/**Sets the Table's name
	 * @param name the Table's name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**Returns whether the Table has any column whose value increments automatically.
	 * @return
	 * <ul><li>true if the Table has any column whose value increments automatically.</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 */
	public boolean hasAutoInc() {
		return getAutoInc() != null;
	}

	/**Returns the number of Columns.
	 * @return number of Columns
	 */
	public int columnCount() {
		return size();
	}

	@Override
	public Column get(Object fieldname) {
		Column fieldInfo = super.get(fieldname);
//		if (fieldInfo == null)
//			throw Column.notFound((String)fieldname);
		return fieldInfo;
	}

	public List<Column> getAutoInc() {
		List<Column> columns = getColumns(Column::isAutoIncrement);
		return columns != null ? columns : Collections.emptyList();
	}
/*
	public Column getAutoInc() {
		List<Column> columns = getColumns(Column::isAutoIncrement);
		return !columns.isEmpty() ? columns.get(0) : null;
	}
*/
	public List<Column> getColumns(String... names) {
		if (names == null || names.length < 1)
			return getColumns(column -> true);

		List<String> columnNames = Stream.of(names).map(String::toUpperCase).collect(Collectors.toList());
		return getColumns(column -> columnNames.contains(column.name().toUpperCase()));
	}

	public List<Column> columnsForInsert(Column exclude) {
		return getColumns(column -> !column.equals(exclude));
	}

	public List<Column> getKeys() {
		return getColumns(Column::isKey);
	}

	public List<Column> getNonKeys() {
		return getColumns(column -> !column.isKey());
	}

	public List<Column> getColumnsIn(DataObject dataobject) {
		Set<String> keys = dataobject.keySet();
		return getColumns(column -> keys.contains(column.name()));
	}

	private List<Column> getColumns(Predicate<Column> predicate) {
		if (predicate == null)
			predicate = (column) -> true;

		return values().stream()
			.filter(predicate)
			.collect(Collectors.toList());
	}

	public String insert(List<Column.Token> tokens) {
		StringBuilder cols = new StringBuilder(),
					  vals = new StringBuilder();
		List<String> exclude = getAutoInc().stream()
			.map(Column::name)
			.collect(Collectors.toList());

		tokens.forEach(token -> {
			String columnName = token.getColumnName();
			if (exclude.contains(columnName)) return;

			if (cols.length() > 0)
				cols.append(", ");
			cols.append(columnName);
			if (vals.length() > 0)
				vals.append(", ");
			vals.append(token.getToken());
		});

		return "INSERT INTO {table}({columns}) VALUES ({values})"
			.replace("{table}", name())
			.replace("{columns}", cols.toString())
			.replace("{values}", vals.toString());
	}
/*
	public String insert(List<Column.Token> tokens) {
		StringBuilder cols = new StringBuilder(),
					  vals = new StringBuilder();
		Column autoInc = getAutoInc();
		String exclude = autoInc != null ? autoInc.name() : null;

		tokens.forEach(token -> {
			String columnName = token.getColumnName();
			if (columnName.equalsIgnoreCase(exclude)) return;

			if (cols.length() > 0)
				cols.append(", ");
			cols.append(columnName);
			if (vals.length() > 0)
				vals.append(", ");
			vals.append(token.getToken());
		});

		return INSERT
			.replace("{table}", name())
			.replace("{columns}", cols.toString())
			.replace("{values}", vals.toString());
	}
*/

	public String getInsert(List<Column> columns) {
		return insert(Column.Token.create(columns));
	}

	public String update(List<Column.Token> nonKeys, List<Column.Token> keys) {
		String where = whereColumns(keys);
		return "UPDATE " + name() + " SET " + setColumns(nonKeys) + ("".equals(where) ? where : " " + where);
	}

	public String getUpdate(List<Column> nonKeys, List<Column> keys) {
		return update(Column.Token.create(nonKeys), Column.Token.create(keys));
	}

	private String setColumns(List<Column.Token> tokens) {
		StringBuilder buff = new StringBuilder();
		tokens.forEach(column -> {
			if (buff.length() > 0)
				buff.append(", ");
			buff.append(column.getColumnName())
				.append(" = ")
				.append(column.getToken());
		});
		return buff.toString();
	}

	public String select(List<Column.Token> keys) {
		String where = whereColumns(keys),
			   sql = "SELECT * FROM " + name() + ("".equals(where) ? where : " " + where);
		return sql.replace("", "");
	}

	public String delete(List<Column.Token> keys) {
		String where = whereColumns(keys);
		return "DELETE FROM " + name() + ("".equals(where) ? where : " " + where);
	}

	public String getDelete(List<Column> columns) {
		return delete(Column.Token.create(columns));
	}

	private String whereColumns(List<Column.Token> tokens) {
		if (tokens == null || tokens.isEmpty()) return "";

		StringBuilder buff = new StringBuilder();
		tokens.forEach(token -> {
			if (buff.length() > 0)
				buff.append(" AND ");
			buff.append(token.getColumnName()).append(" = ").append(token.getToken());
		});
		return buff.insert(0, "WHERE ").toString();
	}

	public List<Object> getValues(List<Column> columns, DataObject dataobject) {
		return columns.stream()
			.map(column -> dataobject.get(column.name()))
			.collect(Collectors.toList());
	}

	private static class Builder extends AbstractComponent {
		Table create(DBAccess dbaccess, String name) throws Exception {
			Table table = new Table();
//			table.setName(name);

			Connection connection = dbaccess.getConnection();
			DatabaseMetaData metaData = connection.getMetaData();

			String catalog = dbaccess.getCatalog(),
				   schema = dbaccess.getSchema();
			if (isEmpty(catalog))
				catalog = connection.getCatalog();
			if (isEmpty(schema))
				schema = connection.getSchema();

			ResultSet columns = metaData.getColumns(catalog, schema, name, null);

			while (columns.next()) {
				table.setName(columns.getString("TABLE_NAME"));
				String columnName = columns.getString("COLUMN_NAME");
				table.put(columnName,
					new Column()
						.setName(columnName)
						.setAutoIncrement("YES".equals(columns.getObject("IS_AUTOINCREMENT")))
				);
			}

			ResultSet keys = metaData.getPrimaryKeys(catalog, schema, name);
			while (keys.next()) {
				String columnName = keys.getString("COLUMN_NAME");
				Column column = table.get(columnName);
				if (column == null)
					throw Column.notFound(columnName);
				column.setKey(true);
			}
			log().trace(() -> "Information created for table: " + name);
			return table;
		}
	}
}