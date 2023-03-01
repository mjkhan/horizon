package horizon.tutorial.chapter4_sqlsheet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import horizon.data.DataObject;
import horizon.data.Dataset;
import horizon.sql.DBAccess;
import horizon.tutorial.TutorialSupport;

/**Describes how to use a horizon.sql.Query or a horizon.sql.Update
 * to perform database updates with SQL statements loaded from SQL sheets.<br />
 * <p>Specifically, you will see how to
 * <ul><li>use {@code <if...>} elements for conditional expression</li>
 * 	   <li>use the {@code empty} operator in conditional expression</li>
 * 	   <li>use {@code <foreach...>} elements for looping expression</li>
 * 	   <li>use {@code <sql...>} elements for sharing expression</li>
 * 	   <li>configure ORM instruction</li>
 * </ul>
 * </p>
 * <p>The items in this class use the SQL sheet 'sql/sqlsheet-tutorial.xml.'<br />
 * Please see the file along with items of this class.
 * </p>
 */
public class Item4_1_SQLSheet extends TutorialSupport {
	@Test
	void item4_1_1_ifElement() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.perform(db -> {
			Dataset dataset = dbaccess.query()
				.sqlId("tutorial.if")
				.param("custID", null)
				.param("custName", null)
				.getDataset();
			println(dataset);

			dataset = dbaccess.query()
				.param("custID", "00001")
				.getDataset();
			println(dataset);

			dataset = dbaccess.query()
				.param("custName", "Jane")
				.getDataset();
			println(dataset);
		});
	}

	@Test
	void item4_1_2_forEachElement() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.perform(db -> {
			Dataset dataset = dbaccess.query()
				.sqlId("tutorial.foreach")
				.param("custIDs", new String[] {"00001", "00002"})
				.getDataset();
			println(dataset);

			dataset = dbaccess.query()
				.sqlId("tutorial.foreach2")
				.param("names", Arrays.asList("East", "West", "South", "North"))
				.getDataset();
			println(dataset);

			List<DataObject> customers = Arrays.asList(
				new DataObject().set("id", "00001").set("credit", 50000),
				new DataObject().set("id", "00002").set("credit", 60000),
				new DataObject().set("id", "00003").set("credit", 70000)
			);
			dbaccess.update()
				.sqlId("tutorial.foreach3")
				.param("custList", customers)
				.execute();

			HashMap<String, Object> map = new HashMap<>();
			map.put("key0", "value0");
			map.put("key1", "value1");
			map.put("key2", "value2");
			dataset = dbaccess.query()
				.sqlId("tutorial.foreach4")
				.param("map", map)
				.getDataset();
			println(dataset);
		});
	}

	@Test
	void item4_1_3_sqlElement() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.perform(db -> {
			Dataset dataset = dbaccess.query()
				.sqlId("tutorial.sqlElement2")
				.getDataset();
			println(dataset);

			dataset = dbaccess.query()
				.sqlId("tutorial.sqlElement3")
				.getDataset();
			println(dataset);
		});
	}

	@Test
	void item4_1_4_parameterTypes() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.perform(db -> {
			Dataset dataset = dbaccess.query()
				.sqlId("tutorial.parameterTypes")
				.param("table", "CUSTOMER")
				.param("columnName", "CUST_ID")
				.param("columnValue", "00001")
				.getDataset();
			println(dataset);
			dataset = db.query()
				.param("columnValue", "00002")
				.getDataset();
			println(dataset);

			dataset = db.query()
				.param("table", "PRODUCT")
				.param("columnName", "PROD_ID")
				.param("columnValue", 1)
				.getDataset();
			println(dataset);
			dataset = db.query()
				.param("columnValue", 2)
				.getDataset();
			println(dataset);
		});
	}

	@Test
	void item4_1_5_emptyOperator() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.perform(db -> {
			Dataset dataset = dbaccess.query()
				.sqlId("tutorial.emptyOperator")
				.param("custID", null)
				.param("custName", null)
				.getDataset();
			println(dataset);

			dataset = dbaccess.query()
				.param("custID", "00001")
				.getDataset();
			println(dataset);

			dataset = dbaccess.query()
				.param("custName", "Jane")
				.getDataset();
			println(dataset);
		});

	}

	@Test
	void item4_1_9_putTogether() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		dbaccess.perform(db -> {
			item4_1_1_ifElement();
			item4_1_2_forEachElement();
			item4_1_3_sqlElement();
			item4_1_4_parameterTypes();
			item4_1_5_emptyOperator();
		});
	}
}