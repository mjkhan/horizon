package horizon.tutorial.chapter2_update;

import java.util.Date;

import org.junit.jupiter.api.Test;

import horizon.data.Dataset;
import horizon.sql.DBAccess;
import horizon.tutorial.TutorialSupport;

/**Describes how to use a horizon.sql.Updatee to execute stored procedures with IN and OUT parameters.
 * <p>Specifically, you will see how to
 * <ul><li>specify IN and OUT parameters</li>
 * 	   <li>provide arguments for IN and OUT parameters</li>
 * 	   <li>get results from the OUT parameters</li>
 * </ul>
 * </p>
 * <p>Note that each call to Update.execute(), by default,
 * <ul><li>opens the java.sql.Connection of the associated DBAccess</li>
 * 	   <li>starts a database transaction</li>
 * 	   <li>executes the statement</li>
 * 	   <li>commits or rolls back the transaction depending on the result</li>
 * 	   <li>closes the Connection</li>
 * </ul>
 * respectively.
 * </p>
 * <p>To execute them in the same shared Connection and transaction context,
 * see how the DBAccess.transact(...) method is used.
 * </p>
 * <p>The items in this class use the SQL sheet 'sql/update-tutorial.xml.'<br />
 * Please see the file along with items of this class.
 * </p>
 */
public class Item2_4_MoreStoredProcedures extends TutorialSupport {
	@Test
	void item2_4_1_execute_Outs() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.transact(db -> {
			String name = "Customer" + (new Date().getTime());
			db.update()
				//The stored procedure has parameters of IN and OUT.
				//The first parameter is an OUT parameter.
			  .sql("{call sp_insert_customer_out(?, ?)}")
				//Start with this method to provide arguments for an IN parameter
				//and specify an OUT parameter.
			  .parameters()
				//The number and order must match the parameters of the stored procedure.
			  .out()		//for the first parameter, which is an OUT parameter
			  .in(name);	//for the 2nd parameter
			db.update().execute();
			//Get the result of the OUT parameter, call the method parameters().getValue(...).
			//It must be done before the DBAccess closes.
			String custID = db.update().parameters().getValue(1);
			System.out.println("custID: " + custID);

			Dataset dataset = db.query()
				.sqlId("tutorial.getCustomer")
				.param("id", custID)
				.getDataset();
			println(dataset);

			db.update()
				.sqlId("tutorial.deleteCustomer")
				.param("custID", custID)
				.execute();
		});
	}

	@Test
	void item2_4_2_execute_NamedOuts() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.transact(db -> {
			String name = "Customer" + (new Date().getTime());
			db.update()
			   //The stored procedure has named parameters of IN and OUT.
			   //The name of an OUT parameter must be prepended by 'OUT:'.
			  .sql("{call sp_insert_customer_out(#{OUT:custID}, #{name})}")
				//You do not have to specify OUT parameters.
				//You have already done that with the 'OUT:' prefix.
			  .param("name", name);
			db.update().execute();
			//Gets the result from the 'custID' parameter, which is an OUT parameter.
			//This must be done before the DBAccess closes.
			String custID = db.update().parameters().getValue("custID");
			System.out.println("custID: " + custID);

			Dataset dataset = db.query()
				.sqlId("tutorial.getCustomer")
				.param("id", custID)
				.getDataset();
			println(dataset);

			db.update()
				.sqlId("tutorial.deleteCustomer")
				.param("custID", custID)
				.execute();
		});
	}

	@Test
	void item2_4_3_execute_Outs_Sqlsheet() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.transact(db -> {
			String name = "Customer" + (new Date().getTime());
			db.update()
			  //The stored procedure is defined in the SQL sheet 'sql/update-tutorial.xml.'
			  //And the rest goes the same as the examples above.
			  .sqlId("tutorial.sp_insert_customer_out")
			  .param("name", name);
			db.update().execute();
			String custID = db.update().parameters().getValue("custID");
			System.out.println("custID: " + custID);

			Dataset dataset = db.query()
				.sqlId("tutorial.getCustomer")
				.param("id", custID)
				.getDataset();
			println(dataset);

			db.update()
				.sqlId("tutorial.deleteCustomer")
				.param("custID", custID)
				.execute();
		});
	}

	@Test
	void item2_4_9_putTogether() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.transact(db -> {
			item2_4_1_execute_Outs();
			item2_4_2_execute_NamedOuts();
			item2_4_3_execute_Outs_Sqlsheet();
		});
	}
}