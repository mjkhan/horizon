package horizon.tutorial.chapter1_query;

import org.junit.jupiter.api.Test;

import horizon.data.Dataset;
import horizon.sql.DBAccess;
import horizon.tutorial.TutorialSupport;

/**Describes how to use a horizon.sql.Query to execute stored procedures with IN and OUT parameters.
 * <p>Specifically, you will see how to
 * <ul><li>specify IN and OUT parameters</li>
 * 	   <li>provide arguments for IN and OUT parameters</li>
 * 	   <li>get results from the OUT parameters</li>
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
class Item1_4_MoreStoredProcedures extends TutorialSupport {
	@Test
	void item1_4_1_getDataset_Outs() {
		int credit = 0,
			fetch = 5;

		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.perform(db -> {
			int start = 0;
			dbaccess.query()
				//The stored procedure has parameters of INs and OUTs.
				//The last 2 parameters are OUT parameters.
				.sql("{call sp_paginate_customers_outs(?, ?, ?, ?, ?)}")
				//Start with this method to provide arguments for IN parameters
				//and specify OUT parameters.
				.parameters()
				//The number and order must match the parameters of the stored procedure.
				.in(credit)	//for the 1st parameter
				.in(start)	//for the 2nd parameter
				.in(fetch)	//for the 3rd parameter
				.out()		//for the 4th parameter, which is an OUT parameter
				.out();		//for the 5th parameter, which is an OUT parameter

			Dataset dataset = dbaccess.query().getDataset();
			//Get the result of OUT parameters, call the method parameters().getValue(...).
			//It must be done before the DBAccess closes.
			Number number = dbaccess.query().parameters().getValue(4);	//Gets the result from the 4th parameter, which is an OUT parameter.
			String string = dbaccess.query().parameters().getValue(5);	//Gets the result from the 5th parameter, which is an OUT parameter.

			dataset.setTotalSize(number)
				   .setStart(start)
				   .setFetchSize(fetch);

			println(dataset);
			println(number);
			println(string);

			//Executes the stored procedure again with different parameters.
			start = 5;
			dbaccess.query()
				.parameters().clear() //Clears up the previous arguments.
				.in(credit)
				.in(start)
				.in(fetch)
				.out()
				.out();

			dataset = dbaccess.query().getDataset();
			number = dbaccess.query().parameters().getValue(4);
			string = dbaccess.query().parameters().getValue(5);

			dataset.setTotalSize(number)
				   .setStart(start)
				   .setFetchSize(fetch);

			println(dataset);
			println(number);
			println(string);

			dbaccess.query()
				.sql("{call sp_get_outs(?, ?)}")
				.parameters()
				.in("World")
				.out();

			dbaccess.query().execute();

			string = dbaccess.query().parameters().getValue(2);
			println(string);
		});
	}

	@Test
	void item1_4_2_getDataset_NamedOuts() {
		int credit = 0,
			fetch = 5;

		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");
		dbaccess.perform(db -> {
			int start = 0;
			Dataset dataset = dbaccess.query()
				//The stored procedure has named parameters of INs and OUTs.
				//The names of OUT parameters must be prepended by 'OUT:'.
				.sql("{call sp_paginate_customers_outs(#{credit}, #{start}, #{fetch}, #{OUT:totalCount}, #{OUT:queryTime})}")
				.param("credit", credit)
				.param("start", start)
				.param("fetch", fetch)
				//You do not have to specify OUT parameters.
				//You have already done that with the 'OUT:' prefix.
				.getDataset();

			//Gets the result from the 'totalCount' parameter, which is an OUT parameter.
			Number number = dbaccess.query().parameters().getValue("totalCount");
			//Gets the result from the 'queryTime' parameter, which is an OUT parameter.
			String string = dbaccess.query().parameters().getValue("queryTime");
			//These must be done before the DBAccess closes.

			dataset.setTotalSize(number)
				   .setStart(start)
				   .setFetchSize(fetch);

			println(dataset);
			println(number);
			println(string);

			//Executes the stored procedure again with different parameters.
			start = 5;
			dataset = dbaccess.query()
				.param("start", start)
				.param("fetch", fetch)
				.param("credit", credit)
				.getDataset();
			number = dbaccess.query().parameters().getValue("totalCount");
			string = dbaccess.query().parameters().getValue("queryTime");

			dataset.setTotalSize(number)
				   .setStart(start)
				   .setFetchSize(fetch);

			println(dataset);
			println(number);
			println(string);

			dbaccess.query()
				.sql("{call sp_get_outs(#{name}, #{OUT:greeting})}")
				.param("name", "World")
				.execute();

			string = dbaccess.query().parameters().getValue("greeting");
			println(string);
		});
	}

	@Test
	void item1_4_3_getDataset_Outs_Sqlsheet() {
		int credit = 0,
			fetch = 5;

		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.perform(db -> {
			int start = 0;
			Dataset dataset = dbaccess.query()
				//The stored procedure is defined in the SQL sheet 'sql/query-tutorial.xml.'
				//And the rest goes the same as the examples above.
				.sqlId("tutorial.sp_paginate_customers_outs")
				.param("credit", credit)
				.param("start", start)
				.param("fetch", fetch)
				.getDataset();

			Number number = dbaccess.query().parameters().getValue("totalCount");
			String string = dbaccess.query().parameters().getValue("queryTime");

			dataset.setTotalSize(number)
				   .setStart(start)
				   .setFetchSize(fetch);

			println(dataset);
			println(number);
			println(string);

			start = 5;
			dataset = dbaccess.query()
				.param("credit", credit)
				.param("start", start)
				.param("fetch", fetch)
				.getDataset();

			number = dbaccess.query().parameters().getValue("totalCount");
			string = dbaccess.query().parameters().getValue("queryTime");

			dataset.setTotalSize(number)
				   .setStart(start)
				   .setFetchSize(fetch);

			println(dataset);
			println(number);
			println(string);

			dbaccess.query()
				.sqlId("tutorial.sp_get_outs")
				.param("name", "World")
				.execute();

			string = dbaccess.query().parameters().getValue("greeting");
			println(string);
		});
	}

	@Test
	void item1_4_9_putTogether() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.perform(db -> {
			item1_4_1_getDataset_Outs();
			item1_4_2_getDataset_NamedOuts();
			item1_4_3_getDataset_Outs_Sqlsheet();
		});
	}
}