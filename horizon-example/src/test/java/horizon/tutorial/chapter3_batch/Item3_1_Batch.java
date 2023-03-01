package horizon.tutorial.chapter3_batch;

import java.text.DecimalFormat;
import java.util.Date;

import org.junit.jupiter.api.Test;

import horizon.sql.DBAccess;
import horizon.tutorial.TutorialSupport;

/**Describes how to use a horizon.sql.Batch to perform database updates in batch
 * with SQL statements whose parameters are specified with the '?' character.
 * <p>Specifically, you will see how to
 * <ul><li>execute INSERT, UPDATE, DELETE statements</li>
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
public class Item3_1_Batch extends TutorialSupport {
	private static final String
		newCustomerID = "SELECT LPAD(IFNULL(MAX(CUST_ID) + 1, 1), 5, '0') NEW_ID FROM CUSTOMER",
		insertCustomer = "INSERT INTO CUSTOMER (CUST_ID, CUST_NAME, ADDRESS, PHONE_NO, EMAIL, CREDIT, INS_TIME, UPD_TIME) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
		deleteCustomer = "DELETE FROM CUSTOMER WHERE CUST_ID >= ?",
		spInsertCustomer = "{call sp_insert_customer(?, ?)}";

	@Test
	void item3_1_1_executeBatch() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.transact(db -> {
			String custID = dbaccess.query()
				.sql(newCustomerID)
				.getValue();
			String address = "Somewhere you may know",
				   phoneNo = "09-009-0009";
			int id = Integer.parseInt(custID),
				credit = 90000;
			Date now = new Date();

			DecimalFormat numberFormat = new DecimalFormat("00000");
			dbaccess.batch()
				//You can use a SQL statement supported by the JDBC driver
				.sql(insertCustomer)
				//Arguments must match the statement's parameters in number and order.
				//Repeat Batch.params(...) as many times as required
				.params(numberFormat.format(id + 0), "Customer " + (id + 0), address, phoneNo, (id + 0) + "@acme.com", credit, now, now)
				.params(numberFormat.format(id + 1), "Customer " + (id + 1), address, phoneNo, (id + 1) + "@acme.com", credit, now, now)
				.params(numberFormat.format(id + 2), "Customer " + (id + 2), address, phoneNo, (id + 2) + "@acme.com", credit, now, now)
				.params(numberFormat.format(id + 3), "Customer " + (id + 3), address, phoneNo, (id + 3) + "@acme.com", credit, now, now)
				.params(numberFormat.format(id + 4), "Customer " + (id + 4), address, phoneNo, (id + 4) + "@acme.com", credit, now, now)
				.execute();

			dbaccess.update()
				.sql(deleteCustomer)
				.params(numberFormat.format(id))
				.execute();

			dbaccess.batch().execute();
			dbaccess.update().execute();

			dbaccess.batch()
				//You can use a SQL statement supported by the JDBC driver
				.sql(spInsertCustomer)
				//Arguments must match the statement's parameters in number and order.
				//Repeat Batch.params(...) as many times as required
				.params(numberFormat.format(id + 0), "Customer " + (id + 0))
				.params(numberFormat.format(id + 1), "Customer " + (id + 1))
				.params(numberFormat.format(id + 2), "Customer " + (id + 2))
				.params(numberFormat.format(id + 3), "Customer " + (id + 3))
				.params(numberFormat.format(id + 4), "Customer " + (id + 4))
				.execute();

			dbaccess.update().execute();

			dbaccess.batch().execute();
			dbaccess.update().execute();
		});
	}
}