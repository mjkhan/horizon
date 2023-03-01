package horizon.tutorial.chapter2_update;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import horizon.data.Dataset;
import horizon.example.customer.Customer;
import horizon.example.product.Product;
import horizon.sql.DBAccess;
import horizon.tutorial.TutorialSupport;

/**Describes how to use a horizon.sql.Update to perform database updates
 * with SQL statements loaded from SQL sheets.<br />
 * <p>Specifically, you will see how to
 * <ul><li>specify SQL statements in an SQL sheet</li>
 * 	   <li>execute INSERT, UPDATE, DELETE statements</li>
 * 	   <li>call stored procedures</li>
 * 	   <li>use ORM instruction configured</li>
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
 * <p>The items in this class use the SQL sheet 'sql/update-tutorial.xml.'<br />
 * Please see the file along with items of this class.
 * </p>
 */
public class Item2_3_SQLSheet extends TutorialSupport {
	@Test
	void item2_3_1_transact() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.transact(db -> {
			String custID = db.query()
				.sqlId("tutorial.newCustomerID")
				.getValue();
			String name = "Customer " + custID,
				   address = "Somewhere you may know",
				   phoneNo = "09-009-0009",
				   email = "c" + custID + "@acme.com",
				   userID = "c" + custID,
				   password = "pass" + custID;
			int credit = 90000;
			Date now = new Date();

			db.update()
				//sql id refers to a <update../> element in a namespace.
				//it is in the format of 'namespace.sql-identifier.'
				//"tutorial.insertCustomer" looks up the "insertCustomer" <update../> in the "tutorial" namespace.
				.sqlId("tutorial.insertCustomer")
				//Provide arguments for the parameters in name-value pairs
				.param("custID", custID).param("name", name).param("address", address).param("phoneNo", phoneNo).param("email", email).param("credit", credit)
				.param("userID", userID).param("password", password).param("now", now)
				.execute();

			Dataset dataset = db.query()
				.sqlId("tutorial.getCustomer")
				.param("id", custID)
				.getDataset();
			println(dataset);

			email = custID + "@acme.com";
			db.update()
				.sqlId("tutorial.updateCustomer")
				.param("email", email).param("custID", custID)
				.execute();

			dataset = db.query()
				.sqlId("tutorial.getCustomer")
				.param("id", custID)
				.getDataset();
			println(dataset);

			db.update()
				.sqlId("tutorial.deleteCustomer")
				.param("custID", custID)
				.execute();

			dbaccess.update()
				.sqlId("tutorial.sp_insert_customer")
				.param("custID", custID).param("name", name)
				.execute();
			dataset = db.query()
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
	void item2_3_2_transact() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.transact(db -> {
			Date now = new Date();
			long time = now.getTime();

			Customer customer = new Customer();
			customer.setName("Customer " + time);
			customer.setAddress("Somewhere you may know");
			customer.setPhoneNumber("09-009-0009");
			customer.setEmail("c" + time + "@acme.com");
			customer.setCredit(90000);

			db.update()
				//The 'tutorial.insertCustomer2' <update../> element has named parameters
				//where the Java expression language is used in the '#{parameter expression}' format
				//as in #{object.property}.
				//The expression is evaluated to resolve arguments for parameters.
				.sqlId("tutorial.insertCustomer2")
				//The 'customer' is provided with the name 'cust',
				//which is used in evaluation of the parameter expression to provide arguments for the SQL statement.
				.param("cust", customer).param("now", new Date())
				.execute();

			String custID = customer.getId();
			Dataset dataset = db.query()
				.sqlId("tutorial.getCustomer")
				.param("id", custID)
				.getDataset();
			println(dataset);

			Optional<Customer> loaded = db.query()
				.sqlId("tutorial.getCustomer")
				.param("id", custID)
				.getObject(rs -> new Customer());
			loaded.ifPresent(System.out::println);

			customer.setEmail(custID + "@acme.com");
			db.update()
				//The 'tutorial.updateCustomer2' <update../> element has named parameters
				//where the Java expression language is used in the '#{parameter expression}' format
				//as in #{object.property}.
				//The expression is evaluated to resolve arguments for parameters.
				.sqlId("tutorial.updateCustomer2")
				//The 'customer' is provided with the name 'cust',
				//which is used in evaluation of the parameter expression to provide arguments for the SQL statement.
				.param("cust", customer).param("now", new Date())
				.execute();

			dataset = db.query()
				.sqlId("tutorial.getCustomer")
				.param("id", custID)
				.getDataset();
			println(dataset);

			db.update()
				.sqlId("tutorial.deleteCustomer2")
				.param("cust", customer)
				.execute();
		});
	}

	@Test
	void item2_3_3_transact() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.transact(db -> {
			Date now = new Date();
			long time = now.getTime();

			Customer customer = new Customer();
			customer.setName("Customer " + time);
			customer.setAddress("Somewhere you may know");
			customer.setPhoneNumber("09-009-0009");
			customer.setEmail("c" + time + "@acme.com");

			db.update()
				.param("now", now)
				//For this to work, an ORM instruction must be configured for the Customer,
				//which is in 'sql/sqlsheet-tutorial.xml
				.create(customer);
			Assertions.assertEquals(10000, customer.getCredit());

			String custID = customer.getId();
			Dataset dataset = db.query()
				.sqlId("tutorial.getCustomer")
				.param("id", custID)
				.getDataset();
			println(dataset);

			Optional<Customer> loaded = db.query()
				.sqlId("tutorial.getCustomer")
				.param("id", custID)
				.getObject(rs -> new Customer());
			customer = loaded.get();
			System.out.println(customer);

			customer.setEmail(custID + "@acme.com");
			db.update()
				.param("now", new Date())
				//For this to work, an ORM instruction must be configured for the Customer,
				//which is in 'sql/sqlsheet-tutorial.xml
				.update(customer);

			dataset = db.query()
				.sqlId("tutorial.getCustomer")
				.param("id", custID)
				.getDataset();
			println(dataset);

			db.update().delete(customer);
		});
	}

	@Test
	void item2_3_4_autoInc() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.transact(db -> {
			String name = "OneBook",
				   type = "Standard",
				   vendor = "Omazon";
			int unitPrice = 1000;

			Product product = new Product();
			product.setName(name);
			product.setType(type);
			product.setVendor(vendor);
			product.setUnitPrice(unitPrice);

			//For this to work, an ORM instruction must be configured for the Product,
			//which is in 'sql/sqlsheet-tutorial.xml.
			//After execution, the auto-incremented value of the PRODUCT.PROD_ID column
			//is set to the product's id property.
			db.update().create(product);
			System.out.println(product);
			Assertions.assertTrue(product.getId() > 0);

			Optional<Product> loaded = db.query()
				.sqlId("tutorial.getProduct")
				.param("prodID", product.getId())
				.getObject(rs -> new Product());
			product = loaded.get();
			System.out.println(product);
			Assertions.assertEquals(name, product.getName());
			Assertions.assertEquals(type, product.getType());
			Assertions.assertEquals(vendor, product.getVendor());
			Assertions.assertEquals(unitPrice, product.getUnitPrice());

			product.setType(type = "Professional");
			product.setUnitPrice(unitPrice = 2000);
			//For this to work, an ORM instruction must be configured for the Product,
			//which is in 'sql/sqlsheet-tutorial.xml.
			db.update().update(product);

			loaded = db.query()
				.sqlId("tutorial.getProduct")
				.param("prodID", product.getId())
				.getObject(rs -> new Product());
			product = loaded.get();
			System.out.println(product);
			Assertions.assertEquals(type, product.getType());
			Assertions.assertEquals(unitPrice, product.getUnitPrice());

			//For this to work, an ORM instruction must be configured for the Product,
			//which is in 'sql/sqlsheet-tutorial.xml.
			db.update().delete(product);

		});
	}

	@Test
	void item2_3_9_putTogether() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.transact(db -> {
			item2_3_1_transact();
			item2_3_2_transact();
			item2_3_3_transact();
			item2_3_4_autoInc();
		});
	}
}