package horizon.tutorial.chapter1_query;

import java.util.List;

import org.junit.jupiter.api.Test;

import horizon.data.Dataset;
import horizon.sql.DBAccess;
import horizon.tutorial.TutorialSupport;

/**Describes how to use a horizon.sql.Query to perform database queries
 * with SQL statements whose parameters are specified with names.<br />
 * <p>Specifically, you will see how to
 * <ul><li>execute SELECT statements</li>
 * 	   <li>call stored procedures</li>
 * 	   <li>specify named parameters</li>
 * 	   <li>get a Dataset from a java.sql.ResultSet</li>
 * 	   <li>get a list of Datasets from multiple java.sql.ResultSets</li>
 * 	   <li>get a Dataset with pagination information</li>
 * </ul>
 * </p>
 * <p>Note that each call to Query.getDataset() or Query.getDatasets(), by default,
 * opens and closes the java.sql.Connection of the associated DBAccess respectively.<br />
 * To execute them in the same shared Connection of the DBAccess,
 * see how the DBAccess.perform(...) method is used.
 * </p>
 */
class Item1_2_NamedParams extends TutorialSupport {
	@Test
	void item1_2_1_getDatasetWithParams() {
		String name = "Jane";
		int amount = 0;
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		Dataset dataset = dbaccess.query()
			//Named parameters specified in the '#{parameter}' format are converted to '?' characters.
			.sql("SELECT * FROM CUSTOMER WHERE CUST_NAME LIKE CONCAT(#{name}, '%') AND CREDIT > #{amount}")
			//Provide arguments for the parameters in name-value pairs
			.param("name", name).param("amount", amount)
			//If the statement returns a java.sql.ResultSet,
			//Get it in a Dataset with Query.getDataset().
			.getDataset();
		println(dataset);

		dataset = dbaccess.query()
			//Named parameters specified in the '#{parameter}' format are converted to '?' characters.
			.sql("{call sp_get_customers(#{name}, #{amount})}")
			//Provide arguments for the parameters in name-value pairs
			.param("name", name).param("amount", amount)
			//If the statement returns a java.sql.ResultSet,
			//Get it in a Dataset with Query.getDataset().
			.getDataset();
		println(dataset);
	}

	@Test
	void item1_2_2_getDatasetForPaging() {
		//Named parameters specified in the '#{parameter}' format are converted to '?' characters.
		String
			paginateCustomers = "SELECT SQL_CALC_FOUND_ROWS * FROM CUSTOMER WHERE CREDIT > #{amount} LIMIT #{start}, #{fetch}",
			foundRows = "SELECT FOUND_ROWS() TOT_CNT";
		int amount = 0,
			start = 0,
			fetch = 5;

		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		//To execute multiple SQL statements
		//for pagination information in the same session, as with a MySQL database,
		//Use the DBAccess.perform(...) method.
		dbaccess.perform(db -> {
			Dataset dataset = db.query()
				.sql(paginateCustomers)
				//Provide arguments for the parameters in name-value pairs
				.param("amount", amount).param("start", start).param("fetch", fetch)
				.getDataset();

			//Get the total size of the query result in the same session
			Number totalSize = db.query()
				.sql(foundRows)
				//horizon.sql.Query.getValue() returns the result as a scalar value.
				.getValue();

			//Set the total size, start, and fetch size of the query result
			dataset.setStart(start)
				   .setFetchSize(fetch)
				   .setTotalSize(totalSize);

			//See the example project to see how to generate links from the Dataset for pagination
			println(dataset);
		});
	}

	@Test
	void item1_2_3_getDatasets() {
		//If the JDBC driver supports multiple SELECT statements in an execution
		String
			sql = "SELECT SQL_CALC_FOUND_ROWS * FROM CUSTOMER WHERE CREDIT > #{amount} LIMIT #{start}, #{fetch};\n"
				+ "SELECT FOUND_ROWS() TOT_CNT";
		int amount = 0,
			start = 0,
			fetch = 5;

		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");
		List<Dataset> datasets = dbaccess.query()
			//Named parameters specified in the '#{parameter}' format are converted to '?' characters.
			.sql(sql)
			//Provide arguments for the parameters in name-value pairs
			.param("amount", amount).param("start", start).param("fetch", fetch)
			//If the statement returns multiple java.sql.ResultSets,
			//get them in a List of Datasets with Query.getDatasets()
			.getDatasets();

		//The Datasets are ordered in accordance with the order of the SELECT statements.
		Dataset dataset = datasets.get(0),
				foundRows = datasets.get(1);

		//Get the total size of the query result in the same session
		Number totalSize = foundRows.get(0).number("TOT_CNT");
		//Set the total size, start, and fetch size of the query result
		dataset.setStart(start)
			   .setFetchSize(fetch)
			   .setTotalSize(totalSize);
		println(dataset);

		datasets = dbaccess.query()
			//The stored procedure returns multiple java.sql.ResultSets
			//Named parameters specified in the '#{parameter}' format are converted to '?' characters.
			.sql("call sp_paginate_customers(#{amount}, #{start}, #{fetch})")
			//Provide arguments for the parameters in name-value pairs
			.param("amount", amount).param("start", start).param("fetch", fetch)
			//If the statement returns multiple java.sql.ResultSets,
			//get them in a List of Datasets with Query.getDatasets()
			.getDatasets();

		dataset = datasets.get(0);
		foundRows = datasets.get(1);

		//Get the total size of the query result in the same session
		totalSize = foundRows.get(0).number("TOT_CNT");
		//Set the total size, start, and fetch size of the query result
		dataset.setStart(start)
			   .setFetchSize(fetch)
			   .setTotalSize(totalSize);

		//See the example project to see how to generate links from the Dataset for pagination
		println(dataset);
	}

	@Test
	void item1_2_9_putTogether() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		//DBAccess.perform(...) has DBAccesses created in each method share a single java.sql.Connection
		dbaccess.perform(db -> {
			item1_2_1_getDatasetWithParams();
			item1_2_2_getDatasetForPaging();
			item1_2_3_getDatasets();
		});
	}
}