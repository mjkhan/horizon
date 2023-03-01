<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
				<div id="tab0" class="tab-pane fade show active">
						<table class="table info-inputs">
						<colgroup>
							<col width="200">
							<col width="500">
						</colgroup>
						<tbody id="prod-info">
							<tr><th><label for="prodID">Product ID</label></th>
								<td><input id="prodID" type="text" readonly class="form-control" placeholder="To be assigned on save" /></td>
							</tr>
							<tr><th><label for="prodName">Name</label></th>
								<td><input id="prodName" type="text" required maxlength="32" class="form-control" placeholder="" /></td>
							</tr>
							<tr><th><label for="prodType">Type</label></th>
								<td><select id="prodType" class="form-control">
										<option value="Standard">Standard</option>
										<option value="Professional">Professional</option>
										<option value="Suite">Suite</option>
									</select>
								</td>
							</tr>
							<tr><th><label for="vendor">Vendor</label></th>
								<td><input id="vendor" type="text" required class="form-control" /></td>
							</tr>
							<tr><th><label for="unitPrice">Unit price</label></th>
								<td><input id="unitPrice" type="number" required min="1" placeholder="Equal to or greater than 1" class="form-control" /></td>
							</tr>
						</tbody>
					</table>
					<button onclick="saveProduct();" class="btn btn-primary float-right">Save</button>
				</div>
<c:set var="functions" scope="request">${functions} <%-- To be written at ${functions} of prod-main.jsp --%>
prodList.onCurrentChange = item => { <%--called when the user changes the current product information--%>
	var info = item ? item.data : null;
	if (!info)
		info = {};
	var prodID = info ? info.PROD_ID : null;
	$("#prod-list").setCurrentRow(prodID); <%-- See header.jsp --%>

	$("#prodID").val(prodID);
	$("#prodName").val(info.PROD_NAME);
	$("#prodType").val(info.PROD_TYPE)
	$("#vendor").val(info.VENDOR);
	$("#unitPrice").val(info.UNIT_PRICE);
	
	$("#prodName").focus();
	
	<%-- Sets productManager.save depending on
	whether the current information is for a new product or an existing product --%>
	productManager.save = !prodID ? productManager.create : productManager.update; 
}	

function saveProduct() {
	if (!validInputs("#prod-info input")) return; <%-- See horizon.js --%>
	
	var product = {
		id:$("#prodID").val() || 0,
		name:$("#prodName").val(),
		type:$("#prodType").val(),
		vendor:$("#vendor").val(),
		unitPrice:$("#unitPrice").val()
	};
	productManager
		.save(product)
		.then(function(resp){
			if (!resp.saved)
				return alert("Failed to save the information.");
			
			alert("Information saved successfully.");
		});
}
</c:set>
<c:set var="onload" scope="request">${onload} <%-- To be written at ${onload} of prod-main.jsp --%>
onEnterPress("#prod-info input", saveProduct);
</c:set>