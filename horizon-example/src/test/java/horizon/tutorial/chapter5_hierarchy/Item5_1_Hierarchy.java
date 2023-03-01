package horizon.tutorial.chapter5_hierarchy;

import java.util.List;

import org.junit.jupiter.api.Test;

import horizon.data.hierarchy.Hierarchy;
import horizon.data.hierarchy.HierarchyBuilder;
import horizon.example.organization.Company;
import horizon.example.organization.Department;
import horizon.example.organization.Division;
import horizon.example.organization.Organization;
import horizon.sql.DBAccess;
import horizon.tutorial.TutorialSupport;

/**Describes how to use a HierarchyBuilder to create an object hierarchy.
 */
public class Item5_1_Hierarchy extends TutorialSupport {
	/**Typical use case of HierarchyBuilder to create an object hierarchy.
	 */
	@Test
	void item5_1_1_build() {
		//SELECTs and loads organizations from the database
		List<Organization> orgs = selectOrganizations();

		HierarchyBuilder<Organization> builder = new HierarchyBuilder<Organization>()
			//Sets an instruction on how to get an ID of an object.
			//The ID may or may not be the actual ID of the object.
			//It is good enough as long as the HierarchyBuilder can use it to identify an object.
			.getKey(Organization::getId)
			//Sets an instruction on how to get an ID of a parent object
			.getParentKey(Organization::getParentID)
			//Sets an instruction on how to add a child to a parent
			.addChild((parent, child) -> parent.add(child))
			//Sets an instruction on how to determine whether an object is at a top node or not.
			.atTop(org -> isEmpty(org.getParentID()));

		Hierarchy<Organization> hierarchy = builder
			//Sets the elements for the builder to work with
			.setElements(orgs)
			//Builds the organization hierarchy
			.build();

		//Gets a string representation of the hierarchy.
		String str = Hierarchy.toString(hierarchy.topElements(), Organization::getChildren);
		System.out.println(str);
	}

	/**Explicitly creates a set of instructions to build an object hierarchy
	 * and has a HiearchyBuilder to use it.
	 */
	@Test
	void item5_1_2_buildWithInstruction() {
		List<Organization> orgs = selectOrganizations();

		//Creates a HierarchyBuilder.Instruction for a set of instructions to build an object hierarchy.
		HierarchyBuilder.Instruction<Organization> instruction = new HierarchyBuilder.Instruction<Organization>()
			//Sets an instruction on how to get an ID of an object.
			//The ID may or may not be the actual ID of the object.
			//It is good enough as long as the HierarchyBuilder can use it to identify an object.
			.getKey(Organization::getId)
			//Sets an instruction on how to get an ID of a parent object
			.getParentKey(Organization::getParentID)
			//Sets an instruction on how to add a child to a parent
			.addChild((parent, child) -> parent.add(child))
			//Sets an instruction on how to determine whether an object is at a top node or not.
			.atTop(org -> isEmpty(org.getParentID()));

		//Sets the instruction to a HierarchyBuilder.
		HierarchyBuilder<Organization> builder = new HierarchyBuilder<Organization>()
			.setInstruction(instruction);

		Hierarchy<Organization> hierarchy = builder
			//Sets the elements for the builder to work with
			.setElements(orgs)
			//Builds the organization hierarchy
			.build();

		//Gets a string representation of the hierarchy.
		String str = Hierarchy.toString(hierarchy.topElements(), Organization::getChildren);
		System.out.println(str);
	}

	private static HierarchyBuilder.Instruction<Organization> createInstruction() {
		return new HierarchyBuilder.Instruction<Organization>()
			.getKey(Organization::getId)
			.getParentKey(Organization::getParentID)
			.addChild((parent, child) -> parent.add(child))
			.atTop(org -> isEmpty(org.getParentID()));
	}

	/**Creates a set of instructions from a factory method
	 * and has a HiearchyBuilder to use it.
	 */
	@Test
	void item5_1_3_buildWithInstruction() {
		List<Organization> orgs = selectOrganizations();

		//Creates a HierarchyBuilder.Instruction from a factory method.
		HierarchyBuilder.Instruction<Organization> instruction = createInstruction();
		//Sets the instruction to a HierarchyBuilder.
		HierarchyBuilder<Organization> builder = new HierarchyBuilder<Organization>()
			.setInstruction(instruction);

		Hierarchy<Organization> hierarchy = builder
			//Sets the elements for the builder to work with
			.setElements(orgs)
			//Builds the organization hierarchy
			.build();

		//Gets a string representation of the hierarchy.
		String str = Hierarchy.toString(hierarchy.topElements(), Organization::getChildren);
		System.out.println(str);
	}

	private static class OrganizationInstruction extends HierarchyBuilder.Instruction<Organization> {
		public OrganizationInstruction() {
			getKey(Organization::getId);
			getParentKey(Organization::getParentID);
			addChild((parent, child) -> parent.add(child));
			atTop(org -> isEmpty(org.getParentID()));
		}
	}

	@Test
	void item5_1_4_buildWithExtendedInstruction() {
		List<Organization> orgs = selectOrganizations();

		//Creates an OrganizationInstruction for a set of instructions to build an object hierarchy.
		HierarchyBuilder.Instruction<Organization> instruction = new OrganizationInstruction();

		//Sets the instruction to a HierarchyBuilder.
		HierarchyBuilder<Organization> builder = new HierarchyBuilder<Organization>()
			.setInstruction(instruction);

		Hierarchy<Organization> hierarchy = builder
			//Sets the elements for the builder to work with
			.setElements(orgs)
			//Builds the organization hierarchy
			.build();

		//Gets a string representation of the hierarchy.
		String str = Hierarchy.toString(hierarchy.topElements(), Organization::getChildren);
		System.out.println(str);
	}

	private List<Organization> selectOrganizations() {
		DBAccess dbaccess = new DBAccess().setConnectionName("jdbcAccess");

		List<Organization> orgs = dbaccess.query()
			.sql("SELECT * FROM BUSI_ORG ORDER BY ORG_TYPE, PRNT_ORG, ORG_ID")
			.getObjects(row -> {
				switch (row.getString("ORG_TYPE")) {
				case "000": return new Company();
				case "001": return new Division();
				case "002": return new Department();
				default: return null;
				}
			});
		println(orgs);
		return orgs;
	}
}