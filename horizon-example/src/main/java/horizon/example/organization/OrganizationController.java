package horizon.example.organization;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import horizon.example.ExampleController;

@Controller
public class OrganizationController extends ExampleController {
	@Resource(name="organizationService")
	private OrganizationService service;

	@RequestMapping("/organization")
	public ModelAndView getOrganizations() {
		return new ModelAndView("organization/org-main")
			.addObject("orgs", toJson(service.getOrganizations()));
	}
}