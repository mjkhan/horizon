package horizon.example.order;

import java.sql.Date;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import horizon.data.Convert;

public class SalesOrder {
	public static SalesOrder create(Map<String, Object> map) {
		SalesOrder obj = new SalesOrder();
		obj.id = Convert.toString(map.get("id"));
		obj.date = new Date(Convert.toLong(map.get("date")));
		obj.custID = Convert.toString(map.get("custID"));
		obj.amount = Convert.toInt(map.get("amount"));
		return obj;
	}

	private String id;
	private Date date;
	private String custID;
	private int amount;
	private List<LineItem> lineItems;

	/**Returns the id.
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**Sets id to the id.
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
		setOrderIDs();
	}

	/**Sets the order's ID to the lineItems.
	 */
	private void setOrderIDs() {
		getLineItems().forEach(lineItem -> lineItem.setOrderID(id));
	}

	/**Returns the date.
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}

	/**Sets date to the date.
	 * @param date the date to set
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**Returns the custID.
	 * @return the custID
	 */
	public String getCustID() {
		return custID;
	}

	/**Sets custID to the custID.
	 * @param custID the custID to set
	 */
	public void setCustID(String custID) {
		this.custID = custID;
	}

	/**Returns the amount.
	 * @return the amount
	 */
	public int getAmount() {
		return amount;
	}

	/**Sets amount to the amount.
	 * @param amount the amount to set
	 */
	public void setAmount(int amount) {
		this.amount = amount;
	}

	/**Returns the lineItems.
	 * @return the lineItems
	 */
	public List<LineItem> getLineItems() {
		return lineItems != null ? lineItems : Collections.emptyList();
	}

	/**Sets lineItems to the lineItems.
	 * @param lineItems the lineItems to set
	 */
	public void setLineItems(List<LineItem> lineItems) {
		this.lineItems = lineItems;
		setOrderIDs();
	}
}