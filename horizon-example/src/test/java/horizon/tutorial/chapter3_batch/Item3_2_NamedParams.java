package horizon.tutorial.chapter3_batch;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import horizon.data.DataObject;
import horizon.sql.DBAccess;
import horizon.tutorial.TutorialSupport;

/**Describes how to use a horizon.sql.Batch to perform database updates
 * with SQL statements whose parameters are specified with names.<br />
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
public class Item3_2_NamedParams extends TutorialSupport {
	private static final String
		newCustomerID = "SELECT LPAD(IFNULL(MAX(CUST_ID) + 1, 1), 5, '0') NEW_ID FROM CUSTOMER",
		insertCustomer = "INSERT INTO CUSTOMER (CUST_ID, CUST_NAME, ADDRESS, PHONE_NO, EMAIL, CREDIT, INS_TIME, UPD_TIME) "
					   + "VALUES (#{custID}, #{custName}, #{address}, #{phoneNo}, #{email}, #{credit}, #{now}, #{now})",
		deleteCustomer = "DELETE FROM CUSTOMER WHERE CUST_ID >= #{custID}",
		spInsertCustomer = "{call sp_insert_customer(#{custID}, #{custName})}";

	@Test
	void item3_2_1_executeBatch() {
		DecimalFormat numberFormat = new DecimalFormat("00000");
		IntFunction<DataObject> mapParams = (id) -> new DataObject()
				.set("custID", numberFormat.format(id)).set("custName", "Customer " + id)
				.set("address", "Somewhere you may know").set("phoneNo", "09-009-0009").set("email", id + "@acme.com")
				.set("credit", 9000);

		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.transact(db -> {
			String custID = dbaccess.query()
				.sql(newCustomerID)
				.getValue();
			int id = Integer.parseInt(custID);

			dbaccess.batch()
				//You can use a SQL statement supported by the JDBC driver
				.sql(insertCustomer)
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
				//You can use a SQL statement supported by the JDBC driver
				.sql(spInsertCustomer)
				//Provide arguments for the parameters in name-value pairs
				//Repeat Batch.params(...) as many times as required
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