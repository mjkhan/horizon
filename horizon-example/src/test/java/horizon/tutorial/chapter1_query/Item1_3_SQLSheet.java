package horizon.tutorial.chapter1_query;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import horizon.data.DataList;
import horizon.data.Dataset;
import horizon.example.customer.Customer;
import horizon.example.order.LineItem;
import horizon.sql.DBAccess;
import horizon.tutorial.TutorialSupport;

/**Describes how to use a horizon.sql.Query to perform database queries
 * with SQL statements loaded from SQL sheets.<br />
 * An SQL sheet is a well-formed XML file where SQL statements are specified.
 * <p>Specifically, you will see how to
 * <ul><li>specify SQL statements in an SQL sheet</li>
 * 	   <li>execute SELECT statements</li>
 * 	   <li>call stored procedures</li>
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
 * <p>The items in this class use the SQL sheet 'sql/query-tutorial.xml.'<br />
 * Please see the file along with items of this class.
 * </p>
 */
class Item1_3_SQLSheet extends TutorialSupport {
	@Test
	void item1_3_1_getDataset() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		Dataset dataset = dbaccess.query()
			//sql id refers to a <query../> element in a namespace.
			//it is in the format of 'namespace.sql-identifier.'
			//"tutorial.allCustomers" looks up the "allCustomers" <query../> in the "tutorial" namespace.
			.sqlId("tutorial.allCustomers")
			//If the statement returns a java.sql.ResultSet,
			//Get it in a Dataset with Query.getDataset().
			.getDataset();
		println(dataset);

		dataset = dbaccess.query()
			.sqlId("tutorial.sp_all_customers")
			//If the statement returns a java.sql.ResultSet,
			//Get it in a Dataset with Query.getDataset().
			.getDataset();
		println(dataset);
	}

	@Test
	void item1_3_2_getDatasetWithParams() {
		String name = "Jane";
		int amount = 0;

		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		Dataset dataset = dbaccess.query()
			//The <query../> element specified by "tutorial.getCustomers" has named parameters
			.sqlId("tutorial.getCustomers")
			//Provide arguments for the parameters in name-value pairs
			.param("name", name).param("amount", amount)
			//If the statement returns a java.sql.ResultSet,
			//Get it in a Dataset with Query.getDataset().
			.getDataset();
		println(dataset);


		dataset = dbaccess.query()
			//The <query../> element specified by "tutorial.sp_get_customers" has named parameters
			.sqlId("tutorial.sp_get_customers")
			//Provide arguments for the parameters in name-value pairs
			.param("name", name).param("amount", amount)
			//If the statement returns a java.sql.ResultSet,
			//Get it in a Dataset with Query.getDataset().
			.getDataset();
		println(dataset);
	}

	@Test
	void item1_3_3_getDatasetForPaging() {
		int amount = 0,
			start = 0,
			fetch = 5;

		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		//To execute multiple SQL statements
		//for pagination information in the same session, as with a MySQL database,
		//Use the DBAccess.perform(...) method.
		dbaccess.perform(db -> {
			Dataset dataset = db.query()
				.sqlId("tutorial.paginateCustomers")
				//Provide arguments for the parameters in name-value pairs
				.param("amount", amount).param("start", start).param("fetch", fetch)
				.getDataset();

			//Get the total size of the query result in the same session
			Number totalSize = db.query()
				.sqlId("tutorial.foundRows")
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
	void item1_3_4_getDatasets() {
		int amount = 0,
			start = 0,
			fetch = 5;

		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		List<Dataset> datasets = dbaccess.query()
			//The <query../> of "tutorial.getDatasets" specifies multiple SELECT statements with named parameters
			.sqlId("tutorial.getDatasets")
			//Provide arguments for the parameters in name-value pairs
			.param("amount", amount).param("start", start).param("fetch", fetch)
			//The statement returns multiple java.sql.ResultSets,
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
			//The <query../> of "sp_paginate_customers" specifies a stored procedure with named parameters.
			//It returns multiple java.sql.ResultSets.
			.sqlId("tutorial.sp_paginate_customers")
			//Provide arguments for the parameters in name-value pairs
			.param("amount", amount).param("start", start).param("fetch", fetch)
			//The statement returns multiple java.sql.ResultSets,
			//get them in a List of Datasets with Query.getDatasets()
			.getDatasets();

		//The Datasets are ordered in accordance with the order of ResultSets the stored procedure returns.
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
	void item1_3_5_getObjects() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		List<Customer> customers = dbaccess.query()
			.sqlId("tutorial.allCustomers")
			//Executes the <query../> element of "tutorial.allCustomers"
			//Gets the result in a List of Customers.
			//The Customer class must have ORM instruction configured.
			.getObjects(row -> new Customer());
		println(customers);

		customers = dbaccess.query()
			.sqlId("tutorial.allCustomers")
			//Executes the <query../> element of "tutorial.allCustomers"
			//Gets the result as objects of the class specified with the "resultType" or "resultAlias" attribute,
			//which is the Customer class in this example.
			//The Customer class must have ORM instruction configured.
			.getObjects();
		println(customers);

		Supplier<Number> totalSize = () -> dbaccess.query().sqlId("tutorial.foundRows").getValue();

		int start = 0,
			fetchSize = 5;
		DataList<Customer> paginated = dbaccess.query().sqlId("tutorial.paginateCustomers")
			.param("amount", 0).param("start", start).param("fetch", fetchSize)
			//Executes the <query../> element of "tutorial.paginateCustomers"
			//Gets the result in a DataList of Customers with pagination information.
			//The Customer class must have ORM instruction configured.
			//The 2nd argument of the getObjects(...) method is to return pagination information, a DataList.Fetch, of the query result.
			//Depending on the database or an SQL statement, the 'row' parameter may or may not be used.
			//The 'row' parameter is a row of the query result.
			.getObjects(
				rs -> new Customer(),
				row -> DataList.getFetch(totalSize.get(), start, fetchSize)
			);
		println(paginated);

		paginated = dbaccess.query().sqlId("tutorial.paginateCustomers")
			.param("amount", 0).param("start", start).param("fetch", fetchSize)
			//Executes the <query../> element of "tutorial.paginateCustomers"
			//Gets the result as objects of the class specified with the "resultType" or "resultAlias" attribute,
			//which is the Customer class in this example.
			//The Customer class must have ORM instruction configured.
			//The argument of the getObjects(...) method is to return pagination information, a DataList.Fetch, of the query result.
			//Depending on the database or an SQL statement, the 'row' parameter may or may not be used.
			//The 'row' parameter is a row of the query result.
			.fetchObjects(row -> DataList.getFetch(totalSize.get(), start, fetchSize));
		println(paginated);

		Dataset dataset = dbaccess.query()
			.sqlId("tutorial.getCustomer")
			.param("id", "00002")
			.getDataset();

		//Creates objects from a Dataset
		List<Customer> custs = dbaccess.query().getObjects(dataset, row -> new Customer());
		custs.forEach(this::println);
	}

	@Test
	void item1_3_6_getObject() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		Optional<Customer> found = dbaccess.query()
			.sqlId("tutorial.getCustomer")
			.param("id", "00001")
			//Executes the <query../> element of "tutorial.getCustomer"
			//Gets the result as a Customer.
			//The Customer class must have ORM instruction configured.
			.getObject(rs -> new Customer());
		found.ifPresent(this::println);

		found = dbaccess.query()
			.sqlId("tutorial.getCustomer")
			.param("id", "00001")
			//Executes the <query../> element of "tutorial.getCustomer"
			//Gets the result as an object of the class specified with the "resultType" or "resultAlias" attribute,
			//which is the Customer class in this example.
			//The Customer class must have ORM instruction configured.
			.getObject();
		found.ifPresent(this::println);

		found = dbaccess.query()
			.param("id", "00001")
			//From the ORM instruction configured for the Customer class,
			//Generates and executes a SELECT statement that retrieves a row for a Customer.
			//The parameters('id') are named after the key properties of a Customer.
			.getObject(Customer.class);
		found.ifPresent(this::println);

		found = dbaccess.query()
			.param("id", "00002")
			.getObject(Customer.class);
		found.ifPresent(this::println);

		Dataset dataset = dbaccess.query()
			.sqlId("tutorial.getCustomer")
			.param("id", "00002")
			.getDataset();

		//Creates an object from a DataObject
		found = dbaccess.query().getObject(dataset.get(0), row -> new Customer());
		found.ifPresent(this::println);

		Optional<LineItem> result = dbaccess.query()
			.param("id", 1)
			.param("orderID", "00001")
			.getObject(LineItem.class);
		result.ifPresent(this::println);
	}

	@Test
	void item1_3_9_putTogether() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		//DBAccess.perform(...) has DBAccesses created in each method share a single java.sql.Connection
		dbaccess.perform(db -> {
			item1_3_1_getDataset();
			item1_3_2_getDatasetWithParams();
			item1_3_3_getDatasetForPaging();
			item1_3_4_getDatasets();
			item1_3_5_getObjects();
			item1_3_6_getObject();
		});
	}
}