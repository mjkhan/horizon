package horizon.example.product;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import horizon.data.Dataset;
import horizon.example.ExampleController;

@RequestMapping("/product")
@Controller
public class ProductController extends ExampleController {
	@Resource(name="productService")
	private ProductService service;

	@RequestMapping("")
	public ModelAndView search(
		@RequestParam(required=false, defaultValue="") String by
	  , @RequestParam(required=false, defaultValue="") String terms
	  , @RequestParam(required=false, defaultValue="0") int start
	  , @RequestParam(required=false, defaultValue="false") boolean json) {
		int fetch = 5;
		String columnName = null;
		switch (by) {
		case "id": columnName = "PROD_ID"; break;
		case "name": columnName = "PROD_NAME"; break;
		default: columnName = null;
		}

		Dataset dataset = service.search(columnName, terms, start, fetch);
		Map<String, Integer> pagination = Map.of(
			"start", dataset.getStart(),
			"totalSize", dataset.getTotalSize(),
			"fetchSize", fetch
		);

		return new ModelAndView(json ? "jsonView" : "product/prod-main")
			.addObject("prodList", json ? dataset : toJson(dataset))
			.addObject("pagination", json ? pagination : toJson(pagination));
	}

	@RequestMapping("/select")
	public ModelAndView select() {
		ModelAndView mav = search("", "", 0, false);
		mav.setViewName("product/prod-select");
		return mav;
	}

	@PostMapping("/create")
	public ModelAndView create(@ModelAttribute Product product) {
		boolean saved = service.create(product);
		return new ModelAndView("jsonView")
			.addObject("saved", saved)
			.addObject("prodID", product.getId());
	}

	@PostMapping("/update")
	public ModelAndView update(@ModelAttribute Product product) {
		boolean saved = service.update(product);
		return new ModelAndView("jsonView")
			.addObject("saved", saved);
	}

	@PostMapping("/remove")
	public ModelAndView remove(Integer... prodIDs) {
		int affected = service.remove(prodIDs);
		return new ModelAndView("jsonView")
			.addObject("affected", affected)
			.addObject("saved", affected > 0);
	}
}