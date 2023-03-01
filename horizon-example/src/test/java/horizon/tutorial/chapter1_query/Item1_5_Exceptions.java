package horizon.tutorial.chapter1_query;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import horizon.data.Dataset;
import horizon.sql.DBAccess;
import horizon.sql.DBAccess.Try;
import horizon.tutorial.TutorialSupport;

/**Describes how to handle exceptions thrown from the horizon.sql.DBAccess.perform(...) methods.<br />
 * An exception handler is provided as an argument for the <code>onException</code> parameter of the methods.
 */
public class Item1_5_Exceptions extends TutorialSupport {
	private DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

	@Test
	void item1_5_1_normalQuery() {
		Dataset custList = dbaccess.perform(db -> {
			Dataset result = new Dataset();

			result.addAll(selectCustomer("00001"));
			result.addAll(selectCustomer("00002"));

			return result;
		});
		println(custList);
	}

	@Test
	void item1_5_2_queryExceptionNotHandled() {
		//With no onException provided, the exception is thrown and propagated out.
		//The exception is rethrown wrapped in a RuntimeException.
		//Use the RuntimeException.getCause() to get the actual exception.
		Assertions.assertThrows(
			RuntimeException.class,
			() -> {
				Dataset custList = dbaccess.perform(db -> {
					Dataset result = new Dataset();

					result.addAll(selectCustomer("00001"));
					throwException(false); // the exception is not handled inside the method
					result.addAll(selectCustomer("00002")); //this and below are not executed because the exception is propagated out

					return result;
				});
				println(custList);
			}
		);
	}

	@Test
	void item1_5_3_queryExceptionHandled() {
		//With an onException provided, the exception is handled and not thrown again.
		Dataset custList = dbaccess.perform(
			db -> {
				Dataset result = new Dataset();

				result.addAll(selectCustomer("00001"));
				throwException(false); // the exception is not handled inside the method
				result.addAll(selectCustomer("00002")); // this and below are not executed because the exception is propagated out

				return result;
			},
			(db, e) -> { // the exception is handled here and returns an alternate result
				log().debug(() -> "An empty dataset is returned when " + e.getMessage());
				return new Dataset();
			}
		);
		println(custList);

		custList = dbaccess.perform(
			db -> {
				Dataset result = new Dataset();

				result.addAll(selectCustomer("00001"));
				throwException(true); // the exception is handled inside the method
				result.addAll(selectCustomer("00002")); // this and below are executed

				return result;
			},
			(db, e) -> { //This exception handler is not called because the exception is handled already
				log().debug(() -> "An empty dataset is returned when " + e.getMessage());
				return new Dataset();
			}
		);
		println(custList);
	}

	private Dataset selectCustomer(String custID) {
		return dbaccess.query()
			.sql("SELECT * FROM CUSTOMER WHERE CUST_ID = #{custID}")
			.param("custID", custID)
			.getDataset();
	}

	/**Throws an exception.
	 * @param handle
	 * <ul><li>true to handle the exception</li>
	 * 	   <li>false to not handle the exception and propagate it out</li>
	 * </ul>
	 */
	private void throwException(boolean handle) {
		if (handle)
			dbaccess.perform(
				(Try)db -> {
					throw new RuntimeException("an exception is thrown");
				},
				(db, e) -> log().debug(() -> e.getMessage() + " and it is handled instantly.")
			);
		else
			dbaccess.perform((Try) db -> {
				throw new RuntimeException("an exception is thrown");
			});
	}
}