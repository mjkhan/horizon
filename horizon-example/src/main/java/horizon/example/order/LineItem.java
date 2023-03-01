package horizon.example.order;

import java.util.Map;

import horizon.data.Convert;

public class LineItem {
	public static LineItem create(Map<String, Object> map) {
		LineItem obj = new LineItem();
		obj.id = Convert.toString(map.get("id"));
		obj.orderID = Convert.toString(map.get("orderID"));
		obj.prodID = Convert.toInt(map.get("prodID"));
		obj.quantity = Convert.toInt(map.get("quantity"));
		obj.price = Convert.toInt(map.get("price"));
		return obj;
	}

	private String
		id,
		orderID;
	private int
		prodID,
		quantity,
		price;

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
	}

	/**Returns the orderID.
	 * @return the orderID
	 */
	public String getOrderID() {
		return orderID;
	}

	/**Sets orderID to the orderID.
	 * @param orderID the orderID to set
	 */
	public void setOrderID(String orderID) {
		this.orderID = orderID;
	}

	/**Returns the prodID.
	 * @return the prodID
	 */
	public int getProdID() {
		return prodID;
	}

	/**Sets prodID to the prodID.
	 * @param prodID the prodID to set
	 */
	public void setProdID(int prodID) {
		this.prodID = prodID;
	}

	/**Returns the quantity.
	 * @return the quantity
	 */
	public int getQuantity() {
		return quantity;
	}

	/**Sets quantity to the quantity.
	 * @param quantity the quantity to set
	 */
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	/**Returns the price.
	 * @return the price
	 */
	public int getPrice() {
		return price;
	}

	/**Sets price to the price.
	 * @param price the price to set
	 */
	public void setPrice(int price) {
		this.price = price;
	}
}