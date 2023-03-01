/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import horizon.base.AbstractComponent;
import horizon.data.DataObject;
import horizon.data.Dataset;
import horizon.sql.DBAccess;
import horizon.sql.Parameters;
import horizon.util.LRUCache;

public class SQLBuilder extends AbstractComponent {
	public static void loadSQLSheets(String... paths) {
		if (isEmpty(paths)) return;

		Stream.of(paths)
			.filter(path -> !isEmpty(path))
			.forEach(path -> SQLSheet.load(path.trim()));
	}

	private static final LRUCache<Integer, Instruction.Text> sqls = new LRUCache<>(100);
	private DBAccess dbaccess;
	private EXProcessor exproc;
	private horizon.sql.Query query;
	private horizon.sql.Update update;

	private Text txt;
	private Processing proc;

	public SQLBuilder setDBAccess(DBAccess dbaccess) {
		this.dbaccess = dbaccess;
		return this;
	}

	private horizon.sql.Query query() {
		return ifEmpty(query, () -> query = new horizon.sql.Query(dbaccess));
	}

	private horizon.sql.Update update() {
		return ifEmpty(update, () -> update = new horizon.sql.Update(dbaccess));
	}

	public EXProcessor expr() {
		return ifEmpty(exproc, () -> exproc = new EXProcessor());
	}

	private Text text() {
		if (txt == null) {
			txt = new Text();
			txt.setBuilder(this);
		}
		return txt;
	}

	private Processing proc() {
		if (proc == null) {
			proc = new Processing();
			proc.setBuilder(this);
		}
		return proc;
	}

	public SQLProc build(String sql, Map<String, Object> params) {
		return build(getText(sql), params);
	}

	public SQLProc build(String sql, List<Map<String, Object>> paramList) {
		return build(getText(sql), paramList);
	}

	private static Instruction.Text getText(String sql) {
		int key = notEmpty(sql, "sql").hashCode();
		Instruction.Text instruction = sqls.get(key);
		if (instruction == null)
			sqls.put(key, instruction = new Instruction.Text().setContent(sql).configure());
		return instruction;
	}

	public Instruction getInstruction(String sqlID) {
		Instruction instruction = Instruction.get(sqlID);
		if (instruction == null)
			throw new RuntimeException("SQL Instruction not found: '" + sqlID + "'");
		return instruction;
	}

	public SQLProc buildFromInstruction(String sqlID, Map<String, Object> params) {
		return build(getInstruction(sqlID), params);
	}

	public SQLProc buildFromInstruction(String sqlID, List<Map<String, Object>> paramList) {
		return build(getInstruction(sqlID), paramList);
	}

	private SQLProc build(Instruction instruction, Map<String, Object> params) {
		expr().setBeans(params);
		SQLProc result = new SQLProc();
		getAssistant(instruction).build(null, result);

		if (instruction instanceof Query) {
			Query q = (Query)instruction;
			result.setResultType(q.resultType());
		}

		return result;
	}

	private SQLProc build(Instruction instruction, List<Map<String, Object>> paramList) {
		SQLProc result = new SQLProc();

		for (Map<String, Object> params: paramList) {
			expr().setBeans(params);
			getAssistant(instruction).build(null, result);
			result.getStatement();
			result.next();
		}

		return result;
	}

	public int process(List<Instruction> instructions, Map<String, Object> params) {
		expr().setBeans(params);
		return process(instructions);
	}

	public int process(List<Instruction> instructions) {
		if (isEmpty(instructions)) return 0;

		int affected = 0;
		for (Instruction instruction: instructions) {
			proc().setInstruction(instruction);
			affected += proc().execute();
		}
		return affected;
	}

	public SQLProc preprocess(String sqlID, Map<String, Object> params) {
		expr().setBeans(params);
		Assistant<?> assistant = getAssistant(getInstruction(sqlID));
		boolean more = assistant.preprocess();
		SQLProc result = new SQLProc();
		assistant.build(null, result);
		return result.setMore(more);
	}

	public void postprocess(String sqlID, Map<String, Object> params) {
		expr().setBeans(params);
		getAssistant(getInstruction(sqlID)).postprocess();
	}

	public void close() {
		if (query != null)
			query.close();
		if (update != null)
			update.close();
		if (exproc != null)
			exproc.clearBeans();
		query = null;
		update = null;
		dbaccess = null;
/*		exproc = null;
		txt = null;
		proc = null; */
	}

	private static final HashMap<Class<?>, Supplier<Assistant<?>>> assistants = new HashMap<>();
	static {
		assistants.put(Instruction.SQL.class, Assistant<Instruction.SQL>::new);
		assistants.put(Query.class, Assistant<Query>::new);
		assistants.put(Update.class, UpdateAssistant::new);
		assistants.put(Instruction.If.class, Predicate::new);
		assistants.put(Instruction.ForEach.class, Loop::new);
	}

	private Assistant<?> getAssistant(Instruction instruction) {
		Assistant<?> assistant =
			instruction instanceof Instruction.Text ? text() :
			instruction instanceof Instruction.BeforeAfter ? proc() :
			assistants.get(instruction.getClass()).get();
		return assistant
			.setBuilder(this)
			.setInstruction(instruction);
	}

	private static class Assistant<T extends Instruction> {
		protected SQLBuilder builder;
		protected T instruction;

		public Assistant<T> setBuilder(SQLBuilder builder) {
			this.builder = builder;
			return this;
		}

		@SuppressWarnings("unchecked")
		public Assistant<T> setInstruction(Instruction instruction) {
			this.instruction = (T)instruction;
			return this;
		}

		protected EXProcessor expr() {
			return builder.exproc;
		}

		protected boolean build(String prefix, SQLProc sql) {
			return build(instruction.getChildren(), prefix, sql);
		}

		protected boolean build(List<Instruction> instructions, String prefix, SQLProc sql) {
			boolean result = false;
			for (Instruction instruction: instructions)
				result = builder.getAssistant(instruction).build(prefix, sql) || result;
			return result;
		}

		protected boolean preprocess() {
			return false;
		}

		protected void postprocess() {}
	}

	private static class UpdateAssistant extends Assistant<Update> {
		private List<Instruction>
			preprocs,
			postprocs;

		@Override
		public Assistant<Update> setInstruction(Instruction instruction) {
			super.setInstruction(instruction);
			preprocs = this.instruction.getPreprocs();
			postprocs = this.instruction.getPostprocs();
			return this;
		}

		@Override
		protected boolean preprocess() {
			builder.process(preprocs);
/*
			if (!isEmpty(preprocs))
				doProcess(preprocs);
*/
			return !postprocs.isEmpty();
		}

		@Override
		protected void postprocess() {
			builder.process(postprocs);
/*
			if (isEmpty(postprocs)) return;

			doProcess(postprocs);
*/
		}
/*
		private void doProcess(List<Instruction> instructions) {
			builder.process(instructions);
		}
*/
	}

	private static class Text extends Assistant<Instruction.Text> {
		@Override
		public boolean build(String prefix, SQLProc sql) {
			String str = instruction.getSQL();

			List<Parameters.Entry> entries = instruction.getPrepareds().stream()
				.map(param -> new Parameters.Entry(param.type(), param.ref(), Parameters.Type.IN.equals(param.type()) ? expr().getValue(param.ref()) : null))
				.collect(Collectors.toList());
			sql.addCurrentEntries(entries);

			for (Param param: instruction.getLiterals()) {
				Object value = expr().getValue(param.ref());
				if (value == null)
					value = "";
				String s = value instanceof String ? (String)value : value.toString();
				str = str.replace(param.token(), s);
			}
			if (prefix != null)
				str = prefix + str;
			sql.addStatement(str);
			instruction = null;
			return true;
		}
	}

	private static class Predicate extends Assistant<Instruction.If> {
		@Override
		public boolean build(String prefix, SQLProc sql) {
			boolean proceed = expr().test(instruction.getTest());
			if (!proceed) return false;

			return build(instruction.getChildren(), prefix, sql);
		}
	}

	private static class Loop extends Assistant<Instruction.ForEach> {
		@Override
		public boolean build(String prefix, SQLProc sql) {
			List<?> items = getItems();
			if (isEmpty(items)) return false;

			boolean result = false;
			String var = instruction.getVar(),
				   separator = instruction.getSeparator(),
				   index = instruction.getIndex();

			for (int i = 0, size = items.size(); i < size; ++i) {
				Object obj = items.get(i);
				expr().setBean(var, obj);
				if (!isEmpty(index))
					expr().setBean(index, i);
				boolean separate = i > 0 && separator != null,
						changed = build(instruction.getChildren(), separate ? separator : null, sql);
				if (changed && !separate)
					separate = true;
				result = changed || result;
			}
			return result;
		}

		@Override
		protected boolean build(List<Instruction> instructions, String prefix, SQLProc sql) {
			boolean result = false,
					separate = prefix != null;
			for (Instruction instruction: instructions) {
				boolean changed = builder.getAssistant(instruction).build(prefix, sql);
//				boolean changed = builder.getAssistant(instruction).build(separate ? prefix : null, sql);
				if (changed && separate)
					separate = false;
				result = changed || result;
			}
			return result;
		}

		private List<?> getItems() {
			Object value = expr().getValue(instruction.getItems());
			if (isEmpty(value))
				return Collections.emptyList();
			if (value instanceof List)
				return (List<?>)value;
			if (value instanceof Iterable) {
				List<Object> items = new ArrayList<>();
				for (Object obj: (Iterable<?>)value)
					items.add(obj);
				return items;
			}
			if (value.getClass().isArray())
				return Arrays.asList((Object[])value);
			if (value instanceof Map) {
				Map<?, ?> map = (Map<?, ?>)value;
				return new ArrayList<Object>(map.entrySet());
			}

			throw new IllegalArgumentException(instruction.getItems() + " is neither Iterable, Array, nor Map");
		}
	}

	private static class Processing extends Assistant<Instruction.BeforeAfter> {
		public int execute() {
			if (instruction.getTargetProperties().isEmpty())
				return doUpdate();
			else {
				if (!instruction.getSourceProperties().isEmpty())
					doProperties();
				if (!instruction.getSourceColumns().isEmpty())
					doQuery();
				return 0;
			}
		}

		private void doProperties() {
			doProperties(
				instruction.getSourceProperties().stream()
					.map(s -> builder.expr().getValue(s))
					.collect(Collectors.toList())
			);
		}

		private void doProperties(List<Object> sources) {
			List<String> targets = instruction.getTargetProperties();
			for (int i = 0; i < targets.size(); ++i) {
				builder.expr().setValue(
					targets.get(i),
					sources.get(i)
				);
			}
		}

		private void doQuery() {
			SQLProc sqlproc = builder.build(instruction, builder.expr().getBeans());
			Dataset dataset = builder.query()
				.sql(sqlproc.getStatement())
				.params(sqlproc.getCurrentEntries().stream().map(Parameters.Entry::value).toArray())
				.getDataset();

			if (dataset.isEmpty())
				throw new RuntimeException("");
			DataObject record = dataset.get(0);
			doProperties(instruction.getSourceColumns().stream().map(record::get).collect(Collectors.toList()));
		}

		private int doUpdate() {
			SQLProc sqlproc = builder.build(instruction, builder.expr().getBeans());
			return builder.update()
				.sql(sqlproc.getStatement())
				.params(sqlproc.getCurrentEntries().stream().map(Parameters.Entry::value).toArray())
				.execute();
		}
	}
}