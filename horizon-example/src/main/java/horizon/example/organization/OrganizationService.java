package horizon.example.organization;

import java.sql.ResultSet;
import java.util.List;

import org.springframework.stereotype.Service;

import horizon.data.hierarchy.Hierarchy;
import horizon.data.hierarchy.HierarchyBuilder;
import horizon.example.ExampleService;

@Service("organizationService")
public class OrganizationService extends ExampleService {
	public List<Organization> getOrganizations() {
		List<Organization> orgs = dbaccess.query()
			.sqlId("example.getOrganizations")
			.getObjects((ResultSet row) -> {
			switch (row.getString("ORG_TYPE")) {
			case "000": return new Company();
			case "001": return new Division();
			case "002": return new Department();
			default: return null;
			}
		});

		HierarchyBuilder<Organization> builder = new HierarchyBuilder<Organization>()
			.getKey(Organization::getId)
			.getParentKey(Organization::getParentID)
			.addChild((parent, child) -> parent.add(child))
			.atTop(org -> isEmpty(org.getParentID()));

		Hierarchy<Organization> hierarchy = builder
			.setElements(orgs)
			.build();

		return hierarchy.topElements();
	}
}