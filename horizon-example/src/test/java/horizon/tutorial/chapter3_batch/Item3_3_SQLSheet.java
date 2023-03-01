package horizon.tutorial.chapter3_batch;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import horizon.data.DataObject;
import horizon.sql.DBAccess;
import horizon.tutorial.TutorialSupport;

/**Describes how to use a horizon.sql.Batch to perform database updates
 * with SQL statements loaded from SQL sheets.<br />
 * <p>Specifically, you will see how to
 * <ul><li>specify named parameters</li>
 * 	   <li>execute INSERT, UPDATE, DELETE statements</li>
 * 	   <li>call stored procedures</li>
 * </ul>
 * </p>
 * <p>Note that each call to Batch.execute(), by default,
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
 */
public class Item3_3_SQLSheet extends TutorialSupport {
	private static final String deleteCustomer = "DELETE FROM CUSTOMER WHERE CUST_ID >= #{custID}";

	@Test
	void item3_3_1_executeBatch() {
		DecimalFormat numberFormat = new DecimalFormat("00000");
		IntFunction<DataObject> mapParams = (id) -> new DataObject()
				.set("custID", numberFormat.format(id)).set("name", "Customer " + id)
				.set("address", "Somewhere you may know").set("phoneNo", "09-009-0009").set("email", id + "@acme.com")
				.set("credit", 9000);

		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.transact(db -> {
			String custID = dbaccess.query()
				.sqlId("tutorial.newCustomerID")
				.getValue();
			int id = Integer.parseInt(custID);

			dbaccess.batch()
				//sql id refers to a <update../> element in a namespace.
				//it is in the format of 'namespace.sql-identifier.'
				//"tutorial.insertCustomer" looks up the "insertCustomer" <update../> in the "tutorial" namespace.
				.sqlId("tutorial.insertCustomer")
				//Provide arguments for the parameters in name-value pairs
				//that are included in every batch parameters.
				.param("now", new Date())
				//Provide arguments for the parameters in name-value pairs
				//Repeat Batch.params(...) as many times as required
				.params(mapParams.apply(id + 0))
				.params(mapParams.apply(id + 1))
				.params(mapParams.apply(id + 2))
				.params(mapParams.apply(id + 3))
				.params(mapParams.apply(id + 4))
				.execute();

			dbaccess.update()
				.sql(deleteCustomer)
				.param("custID", numberFormat.format(id))
				.execute();

			dbaccess.batch().execute();
			dbaccess.update().execute();

			dbaccess.batch()
				.sqlId("tutorial.sp_insert_customer")
				.params(mapParams.apply(id + 0))
				.params(mapParams.apply(id + 1))
				.params(mapParams.apply(id + 2))
				.params(mapParams.apply(id + 3))
				.params(mapParams.apply(id + 4))
				.execute();

			dbaccess.update().execute();

			dbaccess.batch().execute();
			dbaccess.update().execute();
		});
	}
}