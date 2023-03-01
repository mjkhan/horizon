package horizon.tutorial.chapter6_spring;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import horizon.example.customer.Customer;
import horizon.sql.DBAccess;
import horizon.tutorial.TutorialSupport;

@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/spring/tutorial-context.xml")
public class Item6_1_Spring extends TutorialSupport {
	private DBAccess dbaccess;

	@Resource(name="datasourceAccess")
//	@Resource(name="jdbcAccess")
	public void setDBAccess(DBAccess dbaccess) {
		this.dbaccess = dbaccess;
	}

	@Test
	void test() {
		dbaccess.transact(db -> {
			List<Customer> customers = dbaccess.query()
				.sqlId("tutorial.allCustomers")
				.getObjects(row -> new Customer());
			println(customers);

			Customer customer = new Customer();
			customer.setName("Customer created in Spring");
			customer.setPhoneNumber("999-9999-9999");
			customer.setEmail("999@acme.com");
			customer.setAddress("Somewhere you may know");

			dbaccess.update()
				.param("now", new Date())
				.create(customer);

			String custID = customer.getId();

			Optional<Customer> found = dbaccess.query()
				.param("id", custID)
				.getObject(Customer.class);

			found.ifPresent(cust -> {
				cust.setName("New customer name");
				cust.setAddress("Somewhere else you may know");
				dbaccess.update().update(cust);
			});

			dbaccess.update().delete(customer);
		});
	}
}