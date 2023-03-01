package horizon.example.order;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import horizon.data.Dataset;
import horizon.example.ExampleController;

@RequestMapping("/order")
@Controller
public class OrderController extends ExampleController {
	@Resource(name="orderService")
	private OrderService service;

	@RequestMapping("")
	public ModelAndView search(String by, String terms, @RequestParam(required=false, defaultValue="0") int start, boolean json) {
		int fetch = 5;
		String columnName = "custName".equals(by) ? "CUST_NAME" : "ORD_DATE";

		Dataset orderList = service.search(columnName, terms, start, fetch);
		String orderID = !orderList.isEmpty() ? orderList.get(0).string("ORD_ID") : null;
		ModelAndView mav = getLineItems(orderID, json);

		return new ModelAndView(json ? "jsonView" : "order/order-main")
			.addObject("orderList", json ? orderList : toJson(orderList))
			.addObject("lineList", mav.getModel().get("lineList"))
			.addObject("totalSize", orderList.getTotalSize())
			.addObject("start", orderList.getStart())
			.addObject("fetch", fetch);
	}

	@RequestMapping("/lines")
	public ModelAndView getLineItems(String orderID, boolean json) {
		ModelAndView mav = new ModelAndView("jsonView");
		if (!isEmpty(orderID)) {
			Dataset lineList = service.getLineItems(orderID);
			mav.addObject("lineList", json ? lineList : toJson(lineList));
		} else {
			mav.addObject("lineList", json ? Collections.emptyList() : toJson(Collections.emptyList()));
		}
		return mav;
	}

	@PostMapping("/create")
	public ModelAndView create(@RequestBody SalesOrder order) {
		return new ModelAndView("jsonView").addAllObjects(service.create(order));
	}

	@PostMapping("/update")
	public ModelAndView update(@RequestBody Map<String, Map<String, Object>> orderInfo) {
		SalesOrder order = SalesOrder.create(orderInfo.get("order"));
		Map<String, List<LineItem>> linesByStatus = orderInfo.get("lines").entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, entry -> {
				List<Map<String, Object>> lineList = (List<Map<String, Object>>)entry.getValue();
				return lineList.stream()
					.map(LineItem::create)
					.collect(Collectors.toList());
			}));
		return new ModelAndView("jsonView")
			.addAllObjects(service.update(order, linesByStatus));
	}

	@PostMapping("/remove")
	public ModelAndView remove(String... orderIDs) {
		int affected = service.remove(orderIDs);
		return new ModelAndView("jsonView")
			.addObject("affected", affected)
			.addObject("saved", affected > 0);
	}
}