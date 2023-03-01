/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql.support;

import java.io.InputStream;
import java.util.Map;

import org.w3c.dom.Element;

import horizon.base.AbstractComponent;
import horizon.data.StringMap;
import horizon.util.ResourceLoader;
import horizon.util.Xmlement;

public class SQLSheet extends AbstractComponent {
	private static final StringMap<SQLSheet> byNamespace = new StringMap<>();

	public static SQLSheet of(String namespace) {
		return byNamespace.get(namespace);
	}

	public static SQLSheet load(InputStream input) {
		Xmlement xml = Xmlement.get();
		Element doc = xml.getDocument(input);
		String namespace = notEmpty(xml.attribute(doc, "namespace"), "namespace");
		SQLSheet sheet = byNamespace.get(namespace);
		if (sheet == null) {
			sheet = new SQLSheet();
			sheet.namespace = namespace;
			byNamespace.put(namespace, sheet);
		}
		sheet.setSQLs(Instruction.SQL.load(doc));
		sheet.setQueries(Query.load(doc));
		sheet.setUpdates(Update.load(doc));

		Orm.load(doc);

		return sheet;
	}

	public static SQLSheet load(String path) {
		SQLSheet sheet = load(ResourceLoader.load(path));
		sheet.path = path;
		return sheet;
	}

	public static void clear() {
		if (byNamespace.isEmpty()) return;

		Orm.clear();
		Instruction.clear();
		byNamespace.clear();
		log(SQLSheet.class).trace(() -> "SQLSheets cleared");
	}

	private String
		path,
		namespace;

	private Map<String, Instruction.SQL> sqls;
	private Map<String, Query> queries;
	private Map<String, Update> updates;

	/**Returns the path.
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**Returns the namespace.
	 * @return the namespace
	 */
	public String getNamespace() {
		return namespace;
	}

	Map<String, Instruction.SQL> getSQLs() {
		return sqls;
	}

	void setSQLs(Map<String, Instruction.SQL> sqls) {
		if (isEmpty(sqls)) return;

		if (this.sqls == null)
			this.sqls = sqls;
		else {
			sqls.forEach((k, v) -> {
				if (this.sqls.containsKey(k))
					duplicateElements("sql", k, namespace);
				else
					this.sqls.put(k, v);
			});
		}
	}

	private void duplicateElements(String element, String id, String namespace) {
		log().warn(() -> String.format(
			"Duplicate <%s id=\"%s\".../>s found in the '%s' namespace.\nThe first entry will be used and the rest are discarded.",
			element, id, namespace
		));
	}

	Instruction.SQL getSQL(String id) {
		return ifEmpty(sqls.get(id), () -> Instruction.SQL.get(id));
	}

	/**Returns the queries.
	 * @return the queries
	 */
	public Map<String, Query> getQueries() {
		return queries;
	}

	void setQueries(Map<String, Query> queries) {
		if (isEmpty(queries)) return;

		if (this.queries == null)
			this.queries = queries;
		else {
			queries.forEach((k, v) -> {
				if (this.queries.containsKey(k))
					duplicateElements("query", k, namespace);
				else
					this.queries.put(k, v);
			});
		}
	}

	public Query getQuery(String id) {
		return ifEmpty(queries.get(id), () -> Query.get(id));
	}

	/**Returns the updates.
	 * @return the updates
	 */
	public Map<String, Update> getUpdates() {
		return updates;
	}

	void setUpdates(Map<String, Update> updates) {
		if (isEmpty(updates)) return;

		if (this.updates == null)
			this.updates = updates;
		else {
			updates.forEach((k, v) -> {
				if (this.updates.containsKey(k))
					duplicateElements("update", k, namespace);
				this.updates.put(k, v);
			});
		}
	}

	public Update getUpdate(String id) {
		return ifEmpty(updates.get(id), () -> Update.get(id));
	}
}