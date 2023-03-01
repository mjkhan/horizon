package horizon.example.customer;

import java.util.Date;

public class Customer {
	private String
		id,
		name,
		address,
		phoneNumber,
		email;
	private long credit;
	private Date
		createdAt,
		lastModified;

	/**Returns the id.
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**Sets the id.
	 * @param id the id to set
	 */
	public void setId(String id) {
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

	/**Returns the address.
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}
	/**Sets the address.
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}

	/**Returns the phoneNumber.
	 * @return the phoneNumber
	 */
	public String getPhoneNumber() {
		return phoneNumber;
	}
	/**Sets the phoneNumber.
	 * @param phoneNumber the phoneNumber to set
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	/**Returns the email.
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}
	/**Sets the email.
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**Returns the credit.
	 * @return the credit
	 */
	public long getCredit() {
		return credit;
	}
	/**Sets the credit.
	 * @param credit the credit to set
	 */
	public void setCredit(long credit) {
		this.credit = credit;
	}

	/**Returns the createdAt.
	 * @return the createdAt
	 */
	public Date getCreatedAt() {
		return createdAt;
	}
	/**Sets the createdAt.
	 * @param createdAt the createdAt to set
	 */
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	/**Returns the lastModified.
	 * @return the lastModified
	 */
	public Date getLastModified() {
		return lastModified;
	}
	/**Sets the lastModified.
	 * @param lastModified the lastModified to set
	 */
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	@Override
	public String toString() {
		return String.format("%s(id:\"%s\", name:\"%s\")", getClass().getSimpleName(), id, name);
	}
}