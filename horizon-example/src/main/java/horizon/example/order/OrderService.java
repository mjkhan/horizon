package horizon.example.order;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import horizon.data.DataObject;
import horizon.data.Dataset;
import horizon.example.ExampleService;

@Service("orderService")
public class OrderService extends ExampleService {
	public Dataset search(String columnName, String columnValue, int start, int fetch) {
		return dbaccess.perform(db -> {
			Dataset dataset = dbaccess.query()
				.sqlId("example.searchOrders")
				.param("columnName", columnName).param("columnValue", columnValue)
				.param("start", start).param("fetch", fetch)
				.getDataset();

			return getFetch(start, fetch).set(dataset);
		});
	}

	private DataObject getOrderInfo(String orderID) {
		Dataset orderList = search("ORD_ID", orderID, 0, 1);
		return orderList.isEmpty() ? null : orderList.get(0);
	}

	public Dataset getLineItems(String orderID) {
		return dbaccess.query()
			.sqlId("example.getLineItems")
			.param("orderID", orderID)
			.getDataset();
	}

	public Map<String, Object> create(SalesOrder order) {
		return dbaccess.transact(db -> {
			int affected =
				dbaccess.update().create(order)
			  + dbaccess.update().create(order.getLineItems());

			String orderID = order.getId();
			return Map.of(
				"affected", affected,
				"saved", affected > 0,
				"orderInfo", getOrderInfo(orderID),
				"lineList", getLineItems(orderID)
			);
		});
	}

	public Map<String, Object> update(SalesOrder order, Map<String, List<LineItem>> lineItems) {
		List<LineItem> items = lineItems.entrySet().stream()
			.flatMap(entry -> entry.getValue().stream())
			.collect(Collectors.toList());
		order.setLineItems(items);

		return dbaccess.transact(db -> {
			int affected =
				dbaccess.update().update(order)
			  + dbaccess.update().create(lineItems.get("added"))
			  + dbaccess.update().update(lineItems.get("modified"))
			  + dbaccess.update().delete(lineItems.get("removed"));

			String orderID = order.getId();
			return Map.of(
				"affected", affected,
				"saved", affected > 0,
				"orderInfo", getOrderInfo(orderID),
				"lineList", getLineItems(orderID)
			);
		});
	}

	public int remove(String... orderIDs) {
		if (isEmpty(orderIDs))
			return 0;

		return dbaccess.transact(db -> {
			return dbaccess.update().sqlId("example.deleteLineItems")
				.param("orderIDs", orderIDs)
				.execute()
			 + dbaccess.update().sqlId("example.deleteOrders")
				.param("orderIDs", orderIDs)
				.execute();
		});
	}
}