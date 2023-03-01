/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import horizon.base.AbstractComponent;
import horizon.data.DataObject;
import horizon.data.Dataset;

class DatasetBuilder extends AbstractComponent {
	public Dataset getDataset(ResultSet resultset) throws Exception {
		Dataset dataset = new Dataset();
		if (resultset == null)
			return dataset.init();

		ResultSetMetaData metaData = resultset.getMetaData();
		while (resultset.next()) {
			dataset.add(getDataObject(resultset, metaData));
		}
		return dataset.init();
	}

	public DataObject getDataObject(ResultSet resultset) throws Exception {
		ResultSetMetaData metaData = resultset.getMetaData();
		return getDataObject(resultset, metaData);
	}

	private DataObject getDataObject(ResultSet resultset, ResultSetMetaData metaData) throws Exception {
		DataObject row = new DataObject().caseSensitiveKey(true);
		for (int i = 1, count = metaData.getColumnCount(); i <= count; ++i) {
			Object value = resultset.getObject(i);
			if (value instanceof Clob) {
				value = Query.ResultFactory.toString((Clob)value);
			} else if (value instanceof Blob) {
				value = resultset.getBytes(i);
			}
			row.put(metaData.getColumnLabel(i), value);
		}
		return row.caseSensitiveKey(false);
	}
}