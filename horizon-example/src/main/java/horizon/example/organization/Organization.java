package horizon.example.organization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Organization {
	private String
		id,
		type,
		name,
		parentID;
	private ArrayList<Organization> children;

	public List<Organization> getChildren() {
		return children != null ? children : Collections.emptyList();
	}

	public void add(Organization child) {
		if (child == null || this.equals(child)) return;

		if (children == null)
			children = new ArrayList<>();
		if (!children.contains(child))
			children.add(child);
	}

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

	/**Returns the type.
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**Sets the type.
	 * @param type the type to set
	 */
	public void setType(String type) {}

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

	/**Returns the parentID.
	 * @return the parentID
	 */
	public String getParentID() {
		return parentID;
	}

	/**Sets the parentID.
	 * @param parentID the parentID to set
	 */
	public void setParentID(String parentID) {
		this.parentID = parentID;
	}

	@Override
	public String toString() {
		return String.format("%s{id:\"%s\", name:\"%s\"}", getClass().getSimpleName(), getId(), getName());
	}
}