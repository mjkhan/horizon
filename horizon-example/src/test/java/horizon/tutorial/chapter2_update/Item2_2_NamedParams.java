package horizon.tutorial.chapter2_update;

import java.util.Date;

import org.junit.jupiter.api.Test;

import horizon.data.Dataset;
import horizon.example.customer.Customer;
import horizon.sql.DBAccess;
import horizon.tutorial.TutorialSupport;

/**Describes how to use a horizon.sql.Update to perform database updates
 * with SQL statements whose parameters are specified with names.<br />.
 * <p>Specifically, you will see how to
 * <ul><li>specify named parameters</li>
 * 	   <li>execute INSERT, UPDATE, DELETE statements</li>
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
public class Item2_2_NamedParams extends TutorialSupport {
	private DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");
	private static final String
		selectCustomer = "SELECT * FROM CUSTOMER WHERE CUST_ID = #{custID}",
		newCustomerID = "SELECT LPAD(IFNULL(MAX(CUST_ID) + 1, 1), 5, '0') NEW_ID FROM CUSTOMER",
		insertCustomer = "INSERT INTO CUSTOMER (CUST_ID, CUST_NAME, ADDRESS, PHONE_NO, EMAIL, CREDIT, INS_TIME, UPD_TIME) "
					   + "VALUES (#{custID}, #{name}, #{address}, #{phoneNo}, #{email}, #{credit}, #{now}, #{now})",
		updateCustomer = "UPDATE CUSTOMER SET EMAIL = #{email} WHERE CUST_ID = #{custID}",
		deleteCustomer = "DELETE FROM CUSTOMER WHERE CUST_ID = #{custID}",
		spInsertCustomer = "{call sp_insert_customer(#{custID}, #{name})}";

	@Test
	void item2_2_1_transact() {
		dbaccess.transact(db -> {
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
				//Named parameters specified in the '#{parameter}' format are converted to '?' characters.
				.sql(insertCustomer)
				//Provide arguments for the parameters in name-value pairs
				.param("custID", custID).param("name", name)
				.param("address", address).param("phoneNo", phoneNo)
				.param("email", email).param("credit", credit)
				.param("now", now)
				.execute();

			Dataset dataset = db.query().sql(selectCustomer).param("custID", custID).getDataset();
			println(dataset);

			email = custID + "@acme.com";
			db.update()
				//Named parameters specified in the '#{parameter}' format are converted to '?' characters.
				.sql(updateCustomer)
				//Provide arguments for the parameters in name-value pairs
				.param("email", email).param("custID", custID)
				.execute();

			dataset = db.query().sql(selectCustomer).param("custID", custID).getDataset();
			println(dataset);

			db.update()
				//Named parameters specified in the '#{parameter}' format are converted to '?' characters.
				.sql(deleteCustomer)
				//Provide arguments for the parameters in name-value pairs
				.param("custID", custID)
				.execute();

			dbaccess.update()
				//Named parameters specified in the '#{parameter}' format are converted to '?' characters.
				.sql(spInsertCustomer)
				//Provide arguments for the parameters in name-value pairs
				.param("custID", custID).param("name", name)
				.execute();

			dataset = db.query().sql(selectCustomer).param("custID", custID).getDataset();
			println(dataset);

			db.update()
				.sql(deleteCustomer)
				.param("custID", custID)
				.execute();
		});
	}

	private static final String
		insertCustomer2 = "INSERT INTO CUSTOMER (CUST_ID, CUST_NAME, ADDRESS, PHONE_NO, EMAIL, CREDIT, INS_TIME, UPD_TIME) "
					   + "VALUES (#{cust.id}, #{cust.name}, #{cust.address}, #{cust.phoneNumber}, #{cust.email}, #{cust.credit}, #{cust.createdAt}, #{cust.lastModified})",
		updateCustomer2 = "UPDATE CUSTOMER SET EMAIL = #{cust.email} WHERE CUST_ID = #{cust.id}",
		deleteCustomer2 = "DELETE FROM CUSTOMER WHERE CUST_ID = #{cust.id}";

	@Test
	void item2_2_2_insertObject() {
		dbaccess.transact(db -> {
			String custID = db.query()
				.sql(newCustomerID)
				.getValue();
			Customer customer = new Customer();
			customer.setId(custID);
			customer.setName( "Customer " + custID);
			customer.setAddress("Somewhere you may know");
			customer.setPhoneNumber("09-009-0009");
			customer.setEmail("c" + custID + "@acme.com");
			customer.setCredit(90000);
			Date now = new Date();
			customer.setCreatedAt(now);
			customer.setLastModified(now);

			db.update()
				//For named parameters, you can use the Java expression language in the '#{parameter expression}' format
				//as in #{object.property}.
				//The expression is evaluated to resolve arguments for parameters.
				.sql(insertCustomer2)
				//The 'customer' is provided with the name 'cust',
				//which is used in evaluation of the parameter expression to provide arguments for the SQL statement.
				.param("cust", customer)
				.execute();

			Dataset dataset = db.query().sql(selectCustomer).param("custID", custID).getDataset();
			println(dataset);

			customer.setEmail(custID + "@acme.com");
			db.update()
				//For named parameters, you can use Java expression language in the '#{parameter expression}' format
				//as in #{object.property}.
				//The expression is evaluated to resolve arguments for parameters.
				.sql(updateCustomer2)
				//The 'customer' is provided with the name 'cust',
				//which is used in evaluation of the parameter expression to provide arguments for the SQL statement.
				.param("cust", customer)
				.execute();

			dataset = db.query().sql(selectCustomer).param("custID", custID).getDataset();
			println(dataset);

			db.update()
				//For named parameters, you can use Java expression language in the '#{parameter expression}' format
				//as in #{object.property}.
				//The expression is evaluated to resolve arguments for parameters.
				.sql(deleteCustomer2)
				//The 'customer' is provided with the name 'cust',
				//which is used in evaluation of the parameter expression to provide arguments for the SQL statement.
				.param("cust", customer)
				.execute();
		});
	}

	private static final String
		selectProduct = "SELECT * FROM PRODUCT WHERE PROD_ID = #{prodID}",
		insertProduct = "INSERT INTO PRODUCT (PROD_NAME, PROD_TYPE, UNIT_PRICE, VENDOR) VALUES (#{name}, #{type}, #{unitPrice}, #{vendor})",
		updateProduct = "UPDATE PRODUCT SET PROD_TYPE = #{type}, UNIT_PRICE = #{unitPrice} WHERE PROD_ID = #{prodID}",
		deleteProduct = "DELETE FROM PRODUCT WHERE PROD_ID = #{prodID}";

	@Test
	void item2_2_3_autoInc() {
		dbaccess.transact(db -> {
			String name = "OneBook",
				   type = "Standard",
				   vendor = "Omazon";
			int unitPrice = 1000;
			Number result = db.update()
				//INSERT statement that inserts a row to the table 'PRODUCT' whose primary key's value is auto-incremented
				.sql(insertProduct)
				//Provide arguments for the parameters in name-value pairs
				.param("name", name).param("type", type).param("unitPrice", unitPrice).param("vendor", vendor)
				//Use horizon.sql.Update.autoInc() that inserts a row and returns the generated key.
				.autoInc();
			println(result);
			//Get the generated key
			int id = result.intValue();

			Dataset dataset = db.query()
				.sql(selectProduct)
				.param("prodID", id)
				.getDataset();
			println(dataset);

			type = "Professional";
			unitPrice = 2000;
			db.update()
				.sql(updateProduct)
				.param("type", type).param("unitPrice", unitPrice).param("prodID", id)
				.execute();

			dataset = db.query()
				.sql(selectProduct)
				.param("prodID", id)
				.getDataset();
			println(dataset);

			db.update().sql(deleteProduct).param("prodID", id).execute();
		});
	}

	@Test
	void item2_2_9_putTogether() {
		dbaccess.transact(db -> {
			item2_2_1_transact();
			item2_2_2_insertObject();
			item2_2_3_autoInc();
		});
	}
}