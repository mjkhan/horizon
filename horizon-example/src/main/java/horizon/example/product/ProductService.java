package horizon.example.product;

import org.springframework.stereotype.Service;

import horizon.data.Dataset;
import horizon.example.ExampleService;

@Service("productService")
public class ProductService extends ExampleService {
	public Dataset search(String columnName, String columnValue, int start, int fetch) {
		return dbaccess.perform(db -> {
			Dataset result = dbaccess.query()
				.sqlId("example.searchProducts")
				.param("columnName", ifEmpty(columnName, () -> "PROD_ID")).param("columnValue", columnValue)
				.param("start", start).param("fetch", fetch)
				.getDataset();
			return getFetch(start, fetch).set(result);
		});
	}

	public boolean create(Product product) {
		return dbaccess.update().create(product) == 1;
	}

	public boolean update(Product product) {
		return dbaccess.update().update(product) == 1;
	}

	public int remove(Integer... prodIDs) {
		if (isEmpty(prodIDs)) return 0;

		return dbaccess.update()
			.sqlId("example.removeProducts")
			.param("prodIDs", prodIDs)
			.execute();
	}
}