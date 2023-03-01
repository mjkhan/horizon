/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import horizon.base.AbstractComponent;
import horizon.base.Klass;
import horizon.data.StringMap;
import horizon.sql.DBAccess;
import horizon.sql.support.Instruction.BeforeAfter;
import horizon.util.Xmlement;

public class Orm extends AbstractComponent {
	private static final HashMap<Class<?>, Orm> cache = new HashMap<>();
	private static final HashMap<String, Orm> byAlias = new HashMap<>();

	public static Orm get(Class<?> klass) {
		Orm orm = cache.get(klass);
		if (orm == null)
			throw new NullPointerException(Orm.class.getName() + " not found for " + klass.getName());
		return orm;
	}

	public static Orm by(String alias) {
		Orm orm = byAlias.get(alias);
		if (orm == null)
			throw new NullPointerException(Orm.class.getName() + " not found with alias '" + alias + "'");
		return orm;
	}

	public static Orm get(Class<?> klass, DBAccess dbaccess) {
		Orm orm = get(klass);
		if (orm.tableResolved) return orm;

		boolean close = dbaccess.open();
		try {
			Table table = Table.get(notEmpty(orm.getTable(), "table"), dbaccess.getConnection());
			orm.setTable(table);
			orm.tableResolved = true;
			return orm;
		} catch (Exception e) {
			orm.tableResolved = false;
			throw runtimeException(e);
		} finally {
			if (close)
				dbaccess.close();
		}
	}

	public static void clear() {
		cache.clear();
		byAlias.clear();
		log(Orm.class).trace(() -> Orm.class.getSimpleName() + " cleared");
	}

	private boolean tableResolved;
	private Class<?> type;
	private String
		table,
		alias;
	private StringMap<Mapping>
		byProperty,
		byColumn;
	private List<Mapping> autoInc;

	private String
		select,
		insert,
		update,
		delete;
	private StringMap<List<Instruction>> beforeAfters;

	/**Returns the type.
	 * @return the type
	 */
	public Class<?> getType() {
		return type;
	}

	private static final String THIS = "_this";

	public String objRef() {
		return THIS;
	}

	/**Sets the type.
	 * @param type the type to set
	 */
	public void setType(Class<?> type) {
		this.type = type;
	}

	/**Returns the table.
	 * @return the table
	 */
	public String getTable() {
		return table;
	}

	/**Sets the table.
	 * @param table the table to set
	 */
	public void setTable(String table) {
		this.table = table;
	}

	public void setTable(Table table) {
		this.autoInc = table.getAutoInc().stream()
			.map(this::getMapping)
			.collect(Collectors.toList());

		String ref = objRef();
		List<Column> specified = table.getColumns(byColumn.keySet().toArray(new String[byColumn.size()]));
		Function<List<Column>, List<Column.Token>> toColumnTokens = columns -> columns.stream()
			.map(column -> Column.Token.create(column).setToken("#{" + ref + "." + getMapping(column).getProperty() + "}"))
			.collect(Collectors.toList());

		insert = table.insert(toColumnTokens.apply(specified));

		List<Column>
			keyColumns = table.getKeys(),
			nonKeyColumns = specified.stream().filter(column -> !keyColumns.contains(column)).collect(Collectors.toList());

		List<Column.Token>
			nonKeys = toColumnTokens.apply(nonKeyColumns),
			keys = toColumnTokens.apply(keyColumns);
		update = table.update(nonKeys, keys);
		delete = table.delete(keys);
		select = table.select(keys).replace(ref + ".", "");
	}

	/**Returns the alias.
	 * @return the alias
	 */
	public String getAlias() {
		return alias;
	}

	/**Sets the alias.
	 * @param alias the alias to set
	 */
	public void setAlias(String alias) {
		this.alias = alias;
	}

	public List<Mapping> getAutoInc() {
		return ifEmpty(autoInc, Collections::emptyList);
	}
/*
	public void setTable(Table table) {
		Column autoInc = table.getAutoInc();
		if (autoInc != null) {
			this.autoInc = getMapping(autoInc);
		}

		String ref = objRef();
		List<Column> specified = table.getColumns(byColumn.keySet().toArray(new String[byColumn.size()]));
		Function<List<Column>, List<Column.Token>> toColumnTokens = columns -> columns.stream()
			.map(column -> Column.Token.create(column).setToken("#{" + ref + "." + getMapping(column).getProperty() + "}"))
			.collect(Collectors.toList());

		insert = table.insert(toColumnTokens.apply(specified));

		List<Column>
			keyColumns = table.getKeys(),
			nonKeyColumns = specified.stream().filter(column -> !keyColumns.contains(column)).collect(Collectors.toList());

		List<Column.Token>
			nonKeys = toColumnTokens.apply(nonKeyColumns),
			keys = toColumnTokens.apply(keyColumns);
		update = table.update(nonKeys, keys);
		delete = table.delete(keys);
	}

	public Mapping getAutoInc() {
		return autoInc;
	}
*/

	public String getSelect() {
		return select;
	}

	public String getInsert() {
		return insert;
	}

	public String getUpdate() {
		return update;
	}

	public String getDelete() {
		return delete;
	}

	public Collection<Mapping> getMappings() {
		return byProperty.values();
	}

	private Mapping getMapping(Column column) {
		Mapping result = column != null ? byColumn.get(column.name()) : null;
		if (result == null)
			throw new RuntimeException("No mapping found for " + column.name());
		return result;
	}

	/**Returns the column name associated with the property.
	 * @param property an object's property
	 * @return the column name associated with the property
	 */
	public String getColumn(String property) {
		Mapping mapping = byProperty.get(property);
		return mapping != null ? mapping.column : null;
	}

	/**Returns the property associated with the column name.
	 * @param column column name
	 * @return the property associated with the column name
	 */
	public String getProperty(String column) {
		Mapping mapping = byColumn.get(column);
		return mapping != null ? mapping.property : null;
	}

	private void setMapping(Mapping mapping) {
		if (mapping == null) return;

		if (byProperty == null)
			byProperty = new StringMap<>();
		if (byColumn == null) {
			byColumn = new StringMap<>();
			byColumn.caseSensitiveKey(false);
		}
		byProperty.put(mapping.property, mapping);
		byColumn.put(mapping.column, mapping);
	}

	public List<Instruction> getBeforeInserts() {
		return getBeforeAfters("beforeInsert");
	}

	public List<Instruction> getBeforeUpdates() {
		return getBeforeAfters("beforeUpdate");
	}

	public List<Instruction> getBeforeDeletes() {
		return getBeforeAfters("beforeDelete");
	}

	private List<Instruction> getBeforeAfters(String key) {
		return isEmpty(beforeAfters) ? Collections.emptyList() :
			   ifEmpty(beforeAfters.get(key), Collections::emptyList);
	}

	private void add(String key, Instruction.BeforeAfter beforeAfter) {
		if (beforeAfter == null) return;

		List<String> props = beforeAfter.getTargetProperties();
		if (!props.isEmpty()) {
			String objRef = objRef();
			for (int i = 0; i < props.size(); ++i) {
				props.set(i, objRef + "." + props.get(i));
			}
		}

		if (beforeAfters == null)
			beforeAfters = new StringMap<>();
		List<Instruction> list = beforeAfters.get(key);
		if (list == null)
			beforeAfters.put(key, list = new ArrayList<>());
		list.add(beforeAfter);
	}

	@Override
	public String toString() {
		String str = byProperty.values().stream().map(m -> m.toString()).collect(Collectors.joining(", "));
		return String.format("%s{type:%s, table:\"%s\", mappings:[%s]}", getClass().getName(), type.getName(), table, str);
	}

	public static class Mapping {
		private String
			property,
			column;

		/**Returns the property.
		 * @return the property
		 */
		public String getProperty() {
			return property;
		}

		/**Sets the property.
		 * @param property the property to set
		 */
		public void setProperty(String property) {
			this.property = property;
		}

		/**Returns the column.
		 * @return the column
		 */
		public String getColumn() {
			return column;
		}

		/**Sets the column.
		 * @param column the column to set
		 */
		public void setColumn(String column) {
			this.column = column;
		}

		@Override
		public String toString() {
			return String.format("%s{property:\"%s\", column:\"%s\"}", getClass().getName(), property, column);
		}
	}

	static List<Orm> load(Element doc) {
		Xmlement xml = Xmlement.get();
		ArrayList<Orm> orms = new ArrayList<>();
		for (Element child: xml.getChildren(doc, "orm")) {
			Class<?> klass = Klass.of(notEmpty(xml.attribute(child, "type"), "type"));
			if (cache.containsKey(klass)) {
				log(Orm.class).warn(() -> "Duplicate <orm .../>s found for " + klass.getName() + ".\nThe first entry will be used and the rest are discarded.");
				continue;
			}

			Orm orm = new Orm();
			cache.put(klass, orm);
			orm.type = klass;
			orm.table = xml.attribute(child, "table");
			orm.alias = xml.attribute(child, "alias");
			orms.add(orm);

			if (!isEmpty(orm.alias)) {
				if (byAlias.containsKey(orm.alias))
					log(Orm.class).warn(() -> "Duplicate alias found: " + orm.alias + ".\nThe first entry will be used and the rest are discarded.");
				byAlias.put(orm.alias, orm);
			}

			xml.getChildren(child, "mapping").forEach(child2 -> {
				Mapping mapping = new Mapping();
				mapping.property = xml.attribute(child2, "property");
				mapping.column = xml.attribute(child2, "column");
				orm.setMapping(mapping);
			});

			if (!isEmpty(orm.table))
				Arrays.asList(
					"beforeInsert",
					"beforeUpdate",
					"beforeDelete"
				).forEach((evtName) ->
					xml.getChildren(child, evtName).forEach(node -> orm.add(evtName, BeforeAfter.create(node)))
				);
		}
		return orms;
	}
}