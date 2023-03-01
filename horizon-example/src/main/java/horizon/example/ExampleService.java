package horizon.example;

import javax.annotation.Resource;

import horizon.base.AbstractComponent;
import horizon.data.DataList;
import horizon.sql.DBAccess;

public class ExampleService extends AbstractComponent {
	protected DBAccess dbaccess;

	/**Sets the dbaccess.
	 * @param dbaccess the dbaccess to set
	 */
	@Resource(name="dbaccess")
	public void setDbaccess(DBAccess dbaccess) {
		this.dbaccess = dbaccess;
	}

	protected DataList.Fetch getFetch(int start, int fetch) {
		Number number = dbaccess.query().sqlId("example.foundRows").getValue();
		return DataList.getFetch(number, start, fetch);
	}
}