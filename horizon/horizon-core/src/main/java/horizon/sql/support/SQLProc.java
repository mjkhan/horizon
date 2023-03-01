/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import horizon.base.AbstractComponent;
import horizon.sql.Parameters;

public class SQLProc extends AbstractComponent {
	private String statement;
	private Class<?> resultType;
	private StringBuilder buff;
	private List<Parameters.Entry> currentEntries;
	private List<List<Parameters.Entry>> entries;
	private boolean more;

	public String getStatement() {
		if (isEmpty(statement)) {
			if (buff != null) {
				statement = buff.toString().trim();
				buff = null;
			}
		}
		return statement;
	}

	public void addStatement(String statement) {
		if (this.statement != null || statement == null) return;

		if (buff == null)
			buff = new StringBuilder();
		buff.append(statement);
	}

	public Class<?> getResultType() {
		return resultType;
	}

	public void setResultType(Class<?> resultType) {
		this.resultType = resultType;
	}

	public int length() {
		return buff != null ? buff.length() : 0;
	}

	public List<List<Parameters.Entry>> getParamEntries() {
		return entries != null ? entries : Collections.emptyList();
//		return ifEmpty(entries, Collections::emptyList);
	}

	public List<Parameters.Entry> getCurrentEntries() {
		return currentEntries != null ? currentEntries : Collections.emptyList();
//		return ifEmpty(currentEntries, Collections::emptyList);
	}

	public void addCurrentEntries(List<Parameters.Entry> entries) {
		if (isEmpty(entries)) return;

		if (currentEntries == null)
			currentEntries = new ArrayList<>();
		currentEntries.addAll(entries);
	}

	/**Returns the more.
	 * @return the more
	 */
	public boolean hasMore() {
		return more;
	}

	/**Sets the more.
	 * @param more the more to set
	 */
	public SQLProc setMore(boolean more) {
		this.more = more;
		return this;
	}

	public void next() {
		if (isEmpty(currentEntries)) return;

		if (entries == null)
			entries = new ArrayList<>();
		entries.add(currentEntries);
		currentEntries = null;
	}
}