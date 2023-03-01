/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import horizon.data.StringMap;

public class Update extends Instruction {
	static Map<String, Update> load(Element doc) {
		return load(doc, "update", Update::new, (q) -> q.id);
	}

	private String id;
	private StringMap<List<Instruction>> beforeAfters;

	public String getId() {
		return id;
	}

	private List<Instruction> getBeforeAfters(String key) {
		return isEmpty(beforeAfters) ? Collections.emptyList() :
			   ifEmpty(beforeAfters.get(key), Collections::emptyList);
	}

	private void add(String key, BeforeAfter beforeAfter) {
		if (beforeAfter == null) return;
		if (beforeAfters == null)
			beforeAfters = new StringMap<>();
		List<Instruction> list = beforeAfters.get(key);
		if (list == null)
			beforeAfters.put(key, list = new ArrayList<>());
		list.add(beforeAfter);
	}

	@Override
	public List<Instruction> getPreprocs() {
		return getBeforeAfters("before");
	}

	void before(BeforeAfter before) {
		add("before", before);
	}

	@Override
	protected void configure(Node node) {
		id = notEmpty(xml.attribute(node, "id"), "id");
		super.configure(node);
		xml.getChildren(node, "before").forEach(child -> before(BeforeAfter.create(child)));
	}
}