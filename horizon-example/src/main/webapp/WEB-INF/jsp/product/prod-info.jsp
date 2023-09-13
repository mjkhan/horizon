<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
				<div id="tab0" class="tab-pane fade show active">
						<table class="table info-inputs">
						<colgroup>
							<col width="200">
							<col width="500">
						</colgroup>
						<tbody id="prod-info"><%-- Specify the 'data-field' attribute on elements bound to properties of information --%>
							<tr><th><label for="prodID">Product ID</label></th>
								<td><input id="prodID" type="text" data-field="PROD_ID" readonly class="form-control" placeholder="To be assigned on save" /></td>
							</tr>
							<tr><th><label for="prodName">Name</label></th>
								<td><input id="prodName" type="text" data-field="PROD_NAME" required maxlength="32" class="form-control" placeholder="" /></td>
							</tr>
							<tr><th><label for="prodType">Type</label></th>
								<td><select id="prodType" data-field="PROD_TYPE" class="form-control">
										<option value="Standard">Standard</option>
										<option value="Professional">Professional</option>
										<option value="Suite">Suite</option>
									</select>
								</td>
							</tr>
							<tr><th><label for="vendor">Vendor</label></th>
								<td><input id="vendor" type="text" data-field="VENDOR" required class="form-control" /></td>
							</tr>
							<tr><th><label for="unitPrice">Unit price</label></th>
								<td><input id="unitPrice" type="number" data-field="UNIT_PRICE" required min="1" placeholder="Equal to or greater than 1" class="form-control" /></td>
							</tr>
						</tbody>
					</table>
					<button onclick="saveProduct();" class="btn btn-primary float-right">Save</button>
				</div>
<c:set var="functions" scope="request">${functions} <%-- To be written at ${functions} of prod-main.jsp --%>
prodList.onCurrentChange = item => { <%--called when the user changes the current information--%>
	$("#prod-list").setCurrentRow(item.index); <%-- See header.jsp --%>

	$("#prod-info input[data-field], select[data-field]").each(function(){
		let e = $(this),
			property = e.attr("data-field"); <%-- Gets the name of a property the element is bound to --%>
		e.val(item.getValue(property)).off("change")
		 .change(function(){ <%-- Configures the element, on "onChange" event, to set the value to the bound property of information --%>
		 	prodList.setValue(property, e.val());
		 });
	});
}

prodList.onModify = function(changed, item, current) { <%-- called when the user changes information --%>
	if (changed.includes("PROD_NAME")) <%-- If the 'PROD_NAME' property changed, have prodList refreshes the current state to redraw the "prod-list" --%>
		prodList.setState();
};

function saveProduct() {
	if (!validInputs("#prod-info input")) return; <%-- See horizon.js --%>

	productManager
		.save()
		.then(function(resp){
			alert(resp.saved ? "Information saved successfully." : "Failed to save the information.");
		});
}
</c:set>
<c:set var="onload" scope="request">${onload} <%-- To be written at ${onload} of prod-main.jsp --%>
onEnterPress(
	"#prod-info input",
	function(input) {
		$(input).change();
		saveProduct();
	}
);
</c:set>