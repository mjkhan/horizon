package horizon.example.customer;

import java.util.Date;
import java.util.Optional;

import org.springframework.stereotype.Service;

import horizon.data.DataList;
import horizon.example.ExampleService;

@Service("customerService")
public class CustomerService extends ExampleService {
	public DataList<Customer> search(String columnName, String columnValue, int start, int fetch) {
		return dbaccess.query()
			.sqlId("example.searchCustomers")
			.param("columnName", ifEmpty(columnName, () -> "CUST_ID")).param("columnValue", columnValue)
			.param("start", start).param("fetch", fetch)
			.fetchObjects(lastRow -> getFetch(start, fetch));
	}

	public Optional<Customer> getCustomer(String custID) {
		return dbaccess.query()
			.param("id", custID)
			.getObject(Customer.class);
	}

	public Optional<Customer> create(Customer customer) {
		if (customer == null)
			return Optional.empty();

		return dbaccess.transact(db -> {
			boolean saved = dbaccess.update()
				.param("now", new Date())
				.create(customer) == 1;

			return saved ? getCustomer(customer.getId()) : Optional.empty();
		});
	}

	public Optional<Customer> update(Customer changed) {
		if (changed == null)
			return Optional.empty();

		return dbaccess.transact(db -> {
			Optional<Customer> loaded = getCustomer(changed.getId());
			if (!loaded.isPresent())
				throw new RuntimeException("Customer not found: " + changed.getId());

			Customer customer = loaded.get();
			customer.setName(changed.getName());
			customer.setAddress(changed.getAddress());
			customer.setPhoneNumber(changed.getPhoneNumber());
			customer.setEmail(changed.getEmail());
			customer.setCredit(changed.getCredit());

			boolean saved = dbaccess.update()
					.sqlId("example.updateCustomer")
					.param("cust", customer)
					.param("now", new Date())
					.execute() == 1;
/* Or simply
			saved = dbaccess.update()
				.param("now", new Date())
				.update(loaded) == 1;
*/

			return saved ? getCustomer(changed.getId()) : Optional.empty();
		});
	}

	public int remove(String... custIDs) {
		if (isEmpty(custIDs)) return 0;

		return dbaccess.update()
			.sqlId("example.removeCustomers")
			.param("custIDs", custIDs)
			.execute();
	}
}