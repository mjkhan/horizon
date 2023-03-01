package horizon.tutorial.chapter0_dbaccess;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import horizon.sql.DBAccess;
import horizon.tutorial.TutorialSupport;

public class Item0_1_DBAccess extends TutorialSupport {
	@Test
	void item0_1_1_getDBAccess() {
		//By default, a DBAccess loads the configuration file 'dbaccess.xml' from the classpath.
		DBAccess dbaccess = new DBAccess()
			//Sets to use the named configuration from the file.
			.setConnectionName("jdbcAccess");
		dbaccess.query().sql("SELECT * FROM CUSTOMER").getDataset();
	}

	@Test
	void item0_1_2_getDBAccess() {
		DBAccess dbaccess = new DBAccess()
			//Sets the path to the configuration file.
			//The path refers to a file either on the classpath or on the file system.
			.setConfigLocation("dbaccess.xml")
			//Sets to use the named configuration from the file.
			.setConnectionName("jdbcAccess");
		dbaccess.query().sql("SELECT * FROM CUSTOMER").getDataset();
	}

	@Test
	void item0_1_3_shareConnection() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");
		DBAccess dbaccess2 = new DBAccess().setConnectionName("jdbcAccess");

		Assertions.assertFalse(dbaccess.isOpen());
		Assertions.assertFalse(dbaccess2.isOpen());

		dbaccess.open();
		//A java.sql.Connection obtained with a configuration
		//is shared by DBAccesses in the current thread with the same configuration.
		Assertions.assertTrue(dbaccess.isOpen());
		Assertions.assertFalse(dbaccess2.open());
		dbaccess.close();

		Assertions.assertFalse(dbaccess.isOpen());
		Assertions.assertFalse(dbaccess2.isOpen());
	}
}