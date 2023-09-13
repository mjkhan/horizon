package horizon.example.customer;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import horizon.data.DataList;
import horizon.example.ExampleController;

@RequestMapping("/customer")
@Controller
public class CustomerController extends ExampleController {
	@Resource(name="customerService")
	private CustomerService service;

	@RequestMapping("")
	public ModelAndView search(
		@RequestParam(required=false, defaultValue="") String by
	  , @RequestParam(required=false, defaultValue="") String terms
	  , @RequestParam(required=false, defaultValue="0") int start
	  , @RequestParam(required=false, defaultValue="false") boolean json) {
		int fetch = 5;
		String columnName = null;
		switch (by) {
		case "id": columnName = "CUST_ID"; break;
		case "name": columnName = "CUST_NAME"; break;
		default: columnName = null;
		}

		DataList<Customer> customers = service.search(columnName, terms, start, fetch);
		Map<String, Integer> pagination = Map.of(
			"start", customers.getStart(),
			"totalSize", customers.getTotalSize(),
			"fetchSize", fetch
		);

		return new ModelAndView(json ? "jsonView" : "customer/cust-main")
			.addObject("custList", json ? customers : toJson(customers))
			.addObject("pagination", json ? pagination : toJson(pagination));
	}

	@RequestMapping("/select")
	public ModelAndView select() {
		ModelAndView mav = search("", "", 0, false);
		mav.setViewName("customer/cust-select");
		return mav;
	}

	@PostMapping("/create")
	public ModelAndView create(@ModelAttribute Customer customer) {
		Optional<Customer> saved = service.create(customer);

		return new ModelAndView("jsonView")
			.addObject("saved", saved.isPresent())
			.addObject("cust", saved.isPresent() ? saved.get() : null);
	}

	@PostMapping("/update")
	public ModelAndView update(@ModelAttribute Customer customer) {
		Optional<Customer> saved = service.update(customer);
		return new ModelAndView("jsonView")
			.addObject("saved", saved.isPresent())
			.addObject("cust", saved.isPresent() ? saved.get() : null);
	}

	@PostMapping("/remove")
	public ModelAndView remove(String... custIDs) {
		int affected = service.remove(custIDs);
		return new ModelAndView("jsonView")
			.addObject("affected", affected)
			.addObject("saved", affected > 0);
	}
}