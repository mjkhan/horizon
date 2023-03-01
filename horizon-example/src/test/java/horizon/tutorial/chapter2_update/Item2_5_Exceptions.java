package horizon.tutorial.chapter2_update;

import java.util.Date;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import horizon.data.Dataset;
import horizon.sql.DBAccess;
import horizon.tutorial.TutorialSupport;

/**Describes how to handle exceptions thrown from the horizon.sql.DBAccess.transact(...) methods.<br />
 * An exception handler is provided as an argument for the <code>onException</code> parameter of the methods.
 */
public class Item2_5_Exceptions extends TutorialSupport {
	private static DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

	@Test
	void item2_5_1_normalUpdate() {
		dbaccess.transact(db -> {
			normalUpdate0();
			normalUpdate1();

			log().debug(() -> "transaction committable: " + dbaccess.transaction().isCommittable());
		});
		Assertions.assertSame(4, select().size());
	}

	@Test
	void item2_5_2_updateExceptionNotHandled() {
		//With no onException provided, the exception is thrown and propagated out.
		//The exception is rethrown wrapped in a RuntimeException.
		//Use the RuntimeException.getCause() to get the actual exception.
		Assertions.assertThrows(
			RuntimeException.class,
			() -> {
				dbaccess.transact(db -> {
					normalUpdate0();
					throwException(false); // the exception is not handled inside the method
					normalUpdate1(); // this and below are not executed because the exception is propagated out
				});// the transaction is rolled back
			}
		);
	}

	@Test
	void item2_5_3_updateExceptionHandled() {
		dbaccess.transact(
			db -> {
				normalUpdate0();
				throwException(false); // the exception is not handled inside the method
				normalUpdate1(); // this is not executed because the exception is propagated out
			},
			// the exception is handled here and not thrown again
			(db, e) -> log().error(() -> e.getMessage() + " and it is handled in the end.")
		); // the transaction is rolled back

		dbaccess.transact(
			db -> {
				normalUpdate0();
				throwException(true); // the exception is handled inside the method
				normalUpdate1(); // this and below are executed
				log().debug(() -> "transaction committable: " + dbaccess.transaction().isCommittable());
			},
			//This exception handler is not called because the exception is handled already
			(db, e) -> log().error(() -> e.getMessage() + " and it is handled in the end.")
		); // the transaction is rolled back

		/* It is advised that if you have methods nested and some of them calling DBAccess.transact(...),
		 * provide the onException(...) handler for the outer most DBAccess.transact(...).
		 */
	}

	private Dataset select() {
		String sql = "SELECT * FROM CUSTOMER WHERE CUST_ID > '80000'";
		return dbaccess.query().sql(sql).getDataset();
	}

	private int insert(String... custIDs) {
		String sql = "INSERT INTO CUSTOMER (CUST_ID, CUST_NAME, ADDRESS, PHONE_NO, EMAIL, CREDIT, INS_TIME, UPD_TIME)\r\n" +
				"VALUES (#{custID}, #{name}, #{address}, #{phoneNo}, #{email}, #{credit}, #{now}, #{now})";
		int result = 0;
		for (String custID: custIDs)
			result += dbaccess.update().sql(sql)
				.param("custID", custID)
				.param("name", "name " + custID)
				.param("address", "address " + custID)
				.param("phoneNo", "phoneNo " + custID)
				.param("email", "email " + custID)
				.param("credit", 10000)
				.param("now", new Date())
				.execute();
		return result;
	}

	void normalUpdate0() {
		dbaccess.transact(db -> {insert("80001", "80002");});
	}

	void normalUpdate1() {
		dbaccess.transact(db -> {insert("80003", "80004");});
	}

	void throwException(boolean handle) {
		if (handle)
			dbaccess.transact(
				(DBAccess.Try) db -> {
					throw new RuntimeException("an exception is thrown");
				},
				(db, e) -> log().debug(() -> e.getMessage() + " and it is handled instantly.")
			);
		else
			dbaccess.transact((DBAccess.Try) db -> {
				throw new RuntimeException("an exception is thrown");
			});
	}

	@AfterAll
	static void delete() {
		dbaccess.update()
			.sql("DELETE FROM CUSTOMER WHERE CUST_ID > '80000'")
			.execute();
	}
}