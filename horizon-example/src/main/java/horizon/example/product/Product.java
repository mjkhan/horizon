package horizon.example.product;

public class Product {
	private int id;
	private String
		name,
		type;
	private int unitPrice;
	private String vendor;

	/**Returns the id.
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**Sets the id.
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**Returns the name.
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**Sets the name.
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**Returns the type.
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**Sets the type.
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**Returns the unitPrice.
	 * @return the unitPrice
	 */
	public int getUnitPrice() {
		return unitPrice;
	}

	/**Sets the unitPrice.
	 * @param unitPrice the unitPrice to set
	 */
	public void setUnitPrice(int unitPrice) {
		this.unitPrice = unitPrice;
	}

	/**Returns the vendor.
	 * @return the vendor
	 */
	public String getVendor() {
		return vendor;
	}

	/**Sets the vendor.
	 * @param vendor the vendor to set
	 */
	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	@Override
	public String toString() {
		return String.format("%s(id:%d, name:\"%s\", type:\"%s\")", getClass().getSimpleName(), id, name, type);
	}
}