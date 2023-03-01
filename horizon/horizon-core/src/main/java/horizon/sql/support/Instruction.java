/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import horizon.base.AbstractComponent;
import horizon.data.StringMap;
import horizon.util.Xmlement;

public abstract class Instruction extends AbstractComponent {
	private static final StringMap<Instruction> cache = new StringMap<>();
	protected static Xmlement xml = Xmlement.get();

	static <T extends Instruction> Map<String, T> load(Element doc, String nodeName, Supplier<T> supplier, Function<T, String> idMapper) {
		List<Element> children = xml.getChildren(doc, nodeName);
		if (isEmpty(children)) return Collections.emptyMap();

		String namespace = xml.attribute(doc, "namespace");
		StringMap<T> result = new StringMap<>();
		for (Element child: children) {
			T t = supplier.get();
			t.configure(child);
			String id = idMapper.apply(t);
			if (id.contains("."))
				throw new RuntimeException("'.' is not allowed in " + id);
			if (result.containsKey(id))
				throw new RuntimeException("Duplicate entry: " + id);
			result.put(id, t);
			cache.put(namespace + "." + id, t);
		}
		return result;
	}

	static void clear() {
		cache.clear();
		log(Instruction.class).trace(() -> Instruction.class.getSimpleName() + " cleared");
	}

	@SuppressWarnings("unchecked")
	static <T extends Instruction> T get(String id) {
		return (T)cache.get(id);
	}

	private static Instruction create(Node node) {
		Instruction instr = null;
		switch (node.getNodeType()) {
		case Node.ELEMENT_NODE:
			switch (node.getNodeName()) {
			case "if": instr = If.create(node); break;
			case "foreach": instr = ForEach.create(node); break;
			case "sql": instr = SQL.create(node); break;
			}
			break;
		case Node.TEXT_NODE:
		case Node.CDATA_SECTION_NODE:
			instr = Text.create(node); break;
		}
		return instr;
	}

	protected List<Instruction> children;

	public List<Instruction> getChildren() {
		return ifEmpty(children, Collections::emptyList);
	}

	public List<Instruction> getPreprocs() {
		return Collections.emptyList();
	}

	public List<Instruction> getPostprocs() {
		return Collections.emptyList();
	}

	public void add(Instruction child) {
		if (child == null || equals(child)) return;

		if (children == null)
			children = new ArrayList<>();
		if (children.contains(child)) return;

		children.add(child);
	}

	protected void configure(Node node) {
		NodeList nodes = node.getChildNodes();
		int length = nodes != null ? nodes.getLength() : 0;
		if (length < 1) return;

		for (int i = 0; i < length; ++i) {
			Node child = nodes.item(i);
			add(create(child));
		}
	}

	public static class Text extends Instruction {
		static Text create(Node node) {
			Text text = new Text();
			text.configure(node);
			return !isEmpty(text.content) ? text : null;
		}

		private String
			content,
			sql;
		private List<Param>
			prepareds,
			literals;
		private StringMap<Param> literalMap;

		public String getContent() {
			return content;
		}

		public Text setContent(String content) {
			this.content = content;
			return this;
		}

		public String getSQL() {
			return ifEmpty(sql, () -> content);
		}

		public List<Param> getPrepareds() {
			return ifEmpty(prepareds, Collections::emptyList);
		}

		public List<Param> getLiterals() {
			if (literals == null) {
				if (isEmpty(literalMap))
					literals = Collections.emptyList();
				else {
					literals = new ArrayList<>(literalMap.values());
					literalMap.clear();
					literalMap = null;
				}
			}
			return literals;
		}

		private void add(Param param) {
			if (param == null) return;

			if (param.toPrepare()) {
				if (prepareds == null)
					prepareds = new ArrayList<>();
				prepareds.add(param);
			} else {
				if (literalMap == null)
					literalMap = new StringMap<>();
				literalMap.put(param.token(), param);
			}
		}

		public Text configure() {
			Param.parse(content).forEach(this::add);
			List<Param> params = getPrepareds();
			if (!params.isEmpty()) {
				sql = content;
				params.forEach(p -> sql = sql.replace(p.token(), "?"));
			}
			return this;
		}

		@Override
		protected void configure(Node node) {
			content = xml.content(node);
			configure();
			super.configure(node);
		}

		@Override
		public String toString() {
			return content;
		}
	}

	public static class If extends Instruction {
		static If create(Node node) {
			If condition = new If();
			condition.configure(node);
			return condition;
		}

		private String test;

		/**Returns the test.
		 * @return the test
		 */
		public String getTest() {
			return test;
		}

		/**Sets the test.
		 * @param test the test to set
		 */
		public void setTest(String test) {
			this.test = test;
		}

		@Override
		protected void configure(Node node) {
			test = notEmpty(xml.attribute(node, "test"), "test");
			super.configure(node);
		}
	}

	public static class ForEach extends Instruction {
		static ForEach create(Node node) {
			ForEach foreach = new ForEach();
			foreach.configure(node);
			return foreach;
		}

		private String
			items,
			var,
			separator,
			index;

		/**Returns the items.
		 * @return the items
		 */
		public String getItems() {
			return items;
		}

		/**Sets the items.
		 * @param items the items to set
		 */
		public void setItems(String items) {
			this.items = items;
		}

		/**Returns the var.
		 * @return the var
		 */
		public String getVar() {
			return var;
		}

		/**Sets the var.
		 * @param var the var to set
		 */
		public void setVar(String var) {
			this.var = var;
		}

		/**Returns the separator.
		 * @return the separator
		 */
		public String getSeparator() {
			return separator;
		}

		/**Sets the separator.
		 * @param separator the separator to set
		 */
		public void setSeparator(String separator) {
			this.separator = separator;
		}

		/**Returns the index.
		 * @return the index
		 */
		public String getIndex() {
			return index;
		}

		/**Sets the index.
		 * @param index the index to set
		 */
		public void setIndex(String index) {
			this.index = index;
		}

		@Override
		protected void configure(Node node) {
			items = notEmpty(xml.attribute(node, "items"), "items");
			var = notEmpty(xml.attribute(node, "var"), "var");
			separator = xml.attribute(node, "separator");
			index = xml.attribute(node, "index");
			super.configure(node);
		}
	}

	public static class SQL extends Instruction {
		static Map<String, SQL> load(Element doc) {
			return load(doc, "sql", SQL::new, (sql) -> sql.id);
		}

		static SQL create(Node node) {
			SQL sql = new SQL();
			sql.ref = notEmpty(xml.attribute(node, "ref"), "ref");
			if (sql.ref.contains(".")) {
				int pos = sql.ref.lastIndexOf(".");
				sql.namespace = sql.ref.substring(0, pos);
				sql.ref = sql.ref.substring(pos + 1);
			} else {
				sql.namespace = xml.attribute(node.getOwnerDocument().getDocumentElement(), "namespace");
			}
			return sql;
		}

		private String
			id,
			namespace,
			ref;

		/**Returns the id.
		 * @return the id
		 */
		public String getId() {
			return id;
		}

		/**Returns the ref.
		 * @return the ref
		 */
		public String getRef() {
			return ref;
		}

		@Override
		public List<Instruction> getChildren() {
			if (isEmpty(ref))
				return super.getChildren();
			else {
				if (children == null) {
					SQLSheet sheet = SQLSheet.of(namespace);
					SQL sql = notEmpty(sheet.getSQL(ref), "SQL with the id '" + ref + "'");
					children = sql.getChildren();
				}
				return children;
			}
		}

		@Override
		protected void configure(Node node) {
			String str = notEmpty(xml.attribute(node, "id"), "id");
			if (str.contains("."))
				throw new RuntimeException("'.' is not allowed in " + str);
			id = str;
			super.configure(node);
		}
	}

	public static class BeforeAfter extends Instruction {
		static BeforeAfter create(Node node) {
			BeforeAfter ba = new BeforeAfter();
			ba.configure(node);
			return !ba.getTargetProperties().isEmpty() || !ba.getChildren().isEmpty() ? ba : null;
		}

		private List<String>
			targetProperties,
			sourceColumns,
			sourceProperties;

		public List<String> getTargetProperties() {
			return ifEmpty(targetProperties, Collections::emptyList);
		}

		public List<String> getSourceColumns() {
			return ifEmpty(sourceColumns, Collections::emptyList);
		}

		public List<String> getSourceProperties() {
			return ifEmpty(sourceProperties, Collections::emptyList);
		}

		@Override
		protected void configure(Node node) {
			String str = xml.attribute(node, "set");
			if (!isEmpty(str)) {
				Function<String, List<String>> asList = (s) -> {
					return Stream.of(s.split(",")).map(String::trim).collect(Collectors.toList());
//					return List.of(s.split(",")).stream().map(String::trim).collect(Collectors.toList());
				};

				targetProperties = asList.apply(str);

				str = xml.attribute(node, "columns");
				if (!isEmpty(str)) {
					sourceColumns = asList.apply(str);
					if (targetProperties.size() != sourceColumns.size())
						throw new RuntimeException("The items of the 'set' and 'columns' attributes must match in number");

				} else {
					str = xml.attribute(node, "properties");
					if (!isEmpty(str)) {
						sourceProperties = asList.apply(str);
						if (targetProperties.size() != sourceProperties.size())
							throw new RuntimeException("The items of the 'set' and 'properties' attributes must match in number");
					}
				}
			}
			super.configure(node);
		}
	}
}