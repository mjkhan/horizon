/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql.support;

import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import horizon.base.Klass;

public class Query extends Instruction {
	static Map<String, Query> load(Element doc) {
		return load(doc, "query", Query::new, (q) -> q.id);
	}

	private String id;
	private Class<?> resultType;
	private String resultAlias;

	public String getId() {
		return id;
	}

	public Class<?> resultType() {
		if (resultType == null) {
			if (!isEmpty(resultAlias)) {
				Orm orm = Orm.by(resultAlias);
				if (orm != null)
					resultType = orm.getType();
			}
		}
		return resultType;
	}

	@Override
	protected void configure(Node node) {
		id = notEmpty(xml.attribute(node, "id"), "id");
		super.configure(node);
		String str = xml.attribute(node, "resultType");
		resultType = !isEmpty(str) ? Klass.of(str) : null;
		resultAlias = xml.attribute(node, "resultAlias");
	}
}