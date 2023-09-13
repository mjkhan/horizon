<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
				<div id="tab0" class="tab-pane fade show active">
						<table class="table info-inputs">
						<colgroup>
							<col width="200">
							<col width="500">
						</colgroup>
						<tbody id="cust-info"><%-- element IDs are named after properties of information --%>
							<tr><th><label for="id">Customer ID</label></th>
								<td><input id="id" type="text" readonly class="form-control" placeholder="To be assigned on save" /></td>
							</tr>
							<tr><th><label for="name">Name</label></th>
								<td><input id="name" type="text" required maxlength="30" class="form-control" placeholder="" /></td>
							</tr>
							<tr><th><label for="address">Address</label></th>
								<td><input id="address" type="text" required maxlength="50" class="form-control" /></td>
							</tr>
							<tr><th><label for="phoneNumber">Phone number</label></th>
								<td><input id="phoneNumber" type="text" required pattern="\d{3}-\d{3}-\d{4}" maxlength="15" class="form-control" /></td>
							</tr>
							<tr><th><label for="email">Email</label></th>
								<td><input id="email" type="email" required maxlength="30" class="form-control" /></td>
							</tr>
							<tr><th><label for="credit">Credit</label></th>
								<td><input id="credit" type="text" required class="form-control" /></td>
							</tr>
							<tr><th><label for="createdAt">Created at</label></th>
								<td><input id="createdAt" type="text" readonly class="form-control" /></td>
							</tr>
							<tr><th><label for="lastModified">Last modified</label></th>
								<td><input id="lastModified" type="text" readonly class="form-control" /></td>
							</tr>
						</tbody>
					</table>
					<button id="btnSave" onclick="saveCustomer();" class="btn btn-primary float-right">Save</button>
				</div>
<c:set var="functions" scope="request">${functions} <%-- To be written at ${functions} of cust-main.jsp --%>
custList.onCurrentChange = function(item) { <%--called when the user changes the current information--%>
	$("#cust-list").setCurrentRow(item.index); <%-- See header.jsp --%>

	$("#cust-info input").each(function(){
		let input = $(this),
			property = input.attr("id");
		
		input.val(item.getValue(property))
			 .off("change")
			 .change(function(){
				custList.setValue(property, input.val());
			 });
	});
	$("#credit").prop("readonly", item.isNew());

	<%-- Enables or disables the 'Save' button depending on
	whether the current information is dirty or not --%>
	$("#btnSave").prop("disabled", !item.dirty);
}

custList.onModify = function(changed, item, current) { <%-- called when the user changes information --%>
	if (changed.includes("name")) {
		custList.setState();
	}
	if (!current) return;
	
	if (changed.includes("credit")) {
		$("#credit").val(item.getValue("credit"));
	}
	$("#btnSave").prop("disabled", false);
};

function newCustomer() {
	let customer = Array
		.from(document.querySelectorAll("#cust-info input"))
		.map(input => input.id)
		.reduce((obj, id) => {obj[id] = null; return obj;}, {});
		
	customer.createdAt = customer.lastModified = new Date().getTime();
	custList.addData(customer, {local:true});
	$("#name").focus();
}

function saveCustomer() {
	if (!validInputs("#cust-info input")) return; <%-- See horizon.js --%>

	customerManager
		.save()
		.then(function(resp){
			if (!resp.saved)
				return alert("Failed to save the information.");
			
			alert("Information saved successfully.");
		});
}</c:set>
<c:set var="onload" scope="request">${onload} <%-- To be written at ${onload} of cust-main.jsp --%>
onEnterPress("#cust-info input", input => {
	$(input).change();
	saveCustomer();
});</c:set>