/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import horizon.base.AbstractComponent;
import horizon.data.Dataset;

class ResultParser extends AbstractComponent {
	Statement stmt;
	DatasetBuilder builder;

	ResultParser setBuilder(DatasetBuilder builder) {
		this.builder = builder;
		return this;
	}

	ResultParser setStatement(Statement stmt) {
		this.stmt = stmt;
		return this;
	}

	List<Dataset> getDatasets() throws Exception {
		return getResults().stream()
			.filter(obj -> obj instanceof Dataset)
			.map(Dataset.class::cast)
			.collect(Collectors.toList());
/*
		for (Object obj: results)
			if (!(obj instanceof Dataset))
				results.remove(obj);

		ArrayList<Dataset> datasets = new ArrayList<>();
		for (Object obj: results)
			datasets.add((Dataset)obj);
		return datasets;
*/
	}

	private List<Object> getResults() throws Exception {
		if (stmt == null)
			return Collections.emptyList();

		boolean more = true;
		ArrayList<Object> results = new ArrayList<Object>();
		while (more) {
			int update = stmt.getUpdateCount();
			if (update != -1)
				results.add(Integer.valueOf(update));
			else {
				ResultSet resultset = stmt.getResultSet();
				results.add(builder.getDataset(resultset));
				resultset.close();
			}
			more = update != -1 || stmt.getMoreResults();
		}
		clear();
		return results;
	}

	void clear() {
		stmt = null;
		builder = null;
	}
}