package horizon.tutorial.chapter2_update;

import java.util.Date;

import org.junit.jupiter.api.Test;

import horizon.data.Dataset;
import horizon.sql.DBAccess;
import horizon.tutorial.TutorialSupport;

/**Describes how to use a horizon.sql.Update to perform database updates
 * with SQL statements whose parameters are specified with the '?' character.
 * <p>Specifically, you will see how to
 * <ul><li>execute INSERT, UPDATE, DELETE statements</li>
 * 	   <li>call stored procedures</li>
 * 	   <li>get auto-incremented values after INSERT execution</li>
 * </ul>
 * </p>
 * <p>Note that each call to Update.execute() or Update.autoInc(), by default,
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
public class Item2_1_Update extends TutorialSupport {
	private static final String
		selectCustomer = "SELECT * FROM CUSTOMER WHERE CUST_ID = ?",
		newCustomerID = "SELECT LPAD(IFNULL(MAX(CUST_ID) + 1, 1), 5, '0') NEW_ID FROM CUSTOMER",
		insertCustomer = "INSERT INTO CUSTOMER (CUST_ID, CUST_NAME, ADDRESS, PHONE_NO, EMAIL, CREDIT, INS_TIME, UPD_TIME) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
		updateCustomer = "UPDATE CUSTOMER SET EMAIL = ? WHERE CUST_ID = ?",
		deleteCustomer = "DELETE FROM CUSTOMER WHERE CUST_ID = ?",
		spInsertCustomer = "{call sp_insert_customer(?, ?)}";

	/**Each call to Update.execute(), by default,
	 * <ul><li>opens the java.sql.Connection of the associated DBAccess</li>
	 * 	   <li>starts a database transaction</li>
	 * 	   <li>executes the statement</li>
	 * 	   <li>commits or rolls back the transaction depending on the result</li>
	 * 	   <li>closes the Connection</li>
	 * </ul>
	 * respectively.
	 */
	@Test
	void item2_1_1_execute() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		String custID = dbaccess.query()
			.sql(newCustomerID)
			.getValue();
		String name = "Customer " + custID,
			   address = "Somewhere you may know",
			   phoneNo = "09-009-0009",
			   email = "c" + custID + "@acme.com";
		int credit = 90000;
		Date now = new Date();

		dbaccess.update()
			//You can use a SQL statement supported by the JDBC driver
			.sql(insertCustomer)
			//Arguments must match the statement's parameters in number and order
			.params(custID, name, address, phoneNo, email, credit, now, now)
			.execute();

		Dataset dataset = dbaccess.query().sql(selectCustomer).params(custID).getDataset();
		println(dataset);

		email = custID + "@acme.com";
		dbaccess.update()
			//You can use a SQL statement supported by the JDBC driver
			.sql(updateCustomer)
			//Arguments must match the statement's parameters in number and order
			.params(email, custID)
			.execute();

		dataset = dbaccess.query().sql(selectCustomer).params(custID).getDataset();
		println(dataset);

		dbaccess.update()
			//You can use a SQL statement supported by the JDBC driver
			.sql(deleteCustomer)
			//Arguments must match the statement's parameters in number and order
			.params(custID)
			.execute();

		dbaccess.update()
			//You can use a SQL statement supported by the JDBC driver
			.sql(spInsertCustomer)
			//Arguments must match the statement's parameters in number and order
			.params(custID, name)
			.execute();

		dataset = dbaccess.query().sql(selectCustomer).params(custID).getDataset();
		println(dataset);

		dbaccess.update()
			//You can use a SQL statement supported by the JDBC driver
			.sql(deleteCustomer)
			//Arguments must match the statement's parameters in number and order
			.params(custID)
			.execute();
	}

	/**To execute the item above in the same shared Connection and transaction context,
	 * use the DBAccess.transact(...) method.
	 */
	@Test
	void item2_1_2_transact() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.transact(db -> {
			//dbaccess and db are the same DBAccess.
			String custID = db.query()
				.sql(newCustomerID)
				.getValue();
			String name = "Customer " + custID,
				   address = "Somewhere you may know",
				   phoneNo = "09-009-0009",
				   email = "c" + custID + "@acme.com";
			int credit = 90000;
			Date now = new Date();

			db.update()
				//You can use a SQL statement supported by the JDBC driver
				.sql(insertCustomer)
				//Arguments must match the statement's parameters in number and order
				.params(custID, name, address, phoneNo, email, credit, now, now)
				.execute();

			Dataset dataset = db.query().sql(selectCustomer).params(custID).getDataset();
			println(dataset);

			email = custID + "@acme.com";
			db.update()
				.sql(updateCustomer)
				.params(email, custID)
				.execute();

			dataset = db.query().sql(selectCustomer).params(custID).getDataset();
			println(dataset);

			db.update()
				.sql(deleteCustomer)
				.params(custID)
				.execute();

			dbaccess.update()
				.sql(spInsertCustomer)
				.params(custID, name)
				.execute();

			dataset = dbaccess.query().sql(selectCustomer).params(custID).getDataset();
			println(dataset);

			dbaccess.update()
				.sql(deleteCustomer)
				.params(custID)
				.execute();
		});
	}

	private static final String
		selectProduct = "SELECT * FROM PRODUCT WHERE PROD_ID = ?",
		insertProduct = "INSERT INTO PRODUCT (PROD_NAME, PROD_TYPE, UNIT_PRICE, VENDOR) VALUES (?, ?, ?, ?)",
		updateProduct = "UPDATE PRODUCT SET PROD_TYPE = ?, UNIT_PRICE = ? WHERE PROD_ID = ?",
		deleteProduct = "DELETE FROM PRODUCT WHERE PROD_ID = ?";

	@Test
	void item2_1_3_autoInc() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.transact(db -> {
			String name = "OneBook",
				   type = "Standard",
				   vendor = "Omazon";
			int unitPrice = 1000;
			Number result = db.update()
				//INSERT statement that inserts a row to the table 'PRODUCT' whose primary key's value is auto-incremented
				.sql(insertProduct)
				//Arguments must match the statement's parameters in number and order
				.params(name, type, unitPrice, vendor)
				//Use horizon.sql.Update.autoInc() that inserts a row and returns the generated key.
				.autoInc();
			println(result);
			//Get the generated key.
			int id = result.intValue();

			Dataset dataset = db.query()
				.sql(selectProduct)
				.params(id)
				.getDataset();
			println(dataset);

			type = "Professional";
			unitPrice = 2000;
			db.update()
				.sql(updateProduct)
				.params(type, unitPrice, id)
				.execute();

			dataset = db.query()
				.sql(selectProduct)
				.params(id)
				.getDataset();
			println(dataset);

			db.update()
				.sql(deleteProduct)
				.params(id)
				.execute();
		});
	}

	/**To execute the items above in the same shared Connection and transaction context,
	 * use the DBAccess.transact(...) method.
	 */
	@Test
	void item2_1_9_putTogether() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.transact(db -> {
			item2_1_1_execute();
			item2_1_2_transact();
			item2_1_3_autoInc();
		});
	}
}