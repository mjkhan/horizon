<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
				<div id="tab0" class="tab-pane fade show active">
						<table class="table info-inputs">
						<colgroup>
							<col width="200">
							<col width="500">
						</colgroup>
						<tbody id="cust-info">
							<tr><th><label for="custID">Customer ID</label></th>
								<td><input id="custID" type="text" readonly class="form-control" placeholder="To be assigned on save" /></td>
							</tr>
							<tr><th><label for="custName">Name</label></th>
								<td><input id="custName" type="text" required maxlength="30" onchange="custList.setValue('name', this.value);" class="form-control" placeholder="" /></td>
							</tr>
							<tr><th><label for="address">Address</label></th>
								<td><input id="address" type="text" required maxlength="50" onchange="custList.setValue('address', this.value);" class="form-control" /></td>
							</tr>
							<tr><th><label for="phoneNo">Phone number</label></th>
								<td><input id="phoneNo" type="text" required pattern="[0-9-]+" maxlength="15" onchange="custList.setValue('phoneNumber', this.value);" class="form-control" /></td>
							</tr>
							<tr><th><label for="email">Email</label></th>
								<td><input id="email" type="email" required maxlength="30" onchange="custList.setValue('email', this.value);" class="form-control" /></td>
							</tr>
							<tr><th><label for="credit">Credit</label></th>
								<td><input id="credit" type="text" required onchange="custList.setValue('credit', this.value);" class="form-control" /></td>
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
custList.onModify = function(changed, item, current) { <%-- called when the user changes fields of a customer information --%>
	if (changed.includes("name")) {
		drawCustList();
		custList.setState();
		$("#address").focus();
	}
	if (!current) return;
	
	if (changed.includes("credit")) {
		$("#credit").val(item.getValue("credit"));
	}
	$("#btnSave").prop("disabled", false);
};

custList.onCurrentChange = function(item) { <%--called when the user changes the current customer information--%>
	var info = item ? item.data : {},
		custID = info.id;
	$("#cust-list").setCurrentRow(custID); <%-- See header.jsp --%>
		
	$("#custID").val(custID);
	$("#custName").val(info.name);
	$("#address").val(info.address);
	$("#phoneNo").val(info.phoneNumber);
	$("#email").val(info.email);
	$("#credit")
		.val(item ? item.getValue("credit") : null)
		.prop("readonly", item ? "added" == item.state : false);

	$("#createdAt").val(item ? item.getValue("createdAt") : null);
	$("#lastModified").val(item ? item.getValue("lastModified") : null);

	$("#custName").focus();
	
	<%-- Enables or disables the 'Save' button depending on
	whether the current information is dirty or not --%>
	$("#btnSave").prop("disabled", !item || !item.dirty);
}

custList.onReplace = custList.onErase = function(){  <%--called when customer information is either erased or replaced --%>
	drawCustList();
	custList.setState();
};

function saveCustomer() {
	if (!validInputs("#cust-info input")) return; <%-- See horizon.js --%>

	var customer = Object.assign({}, custList.getCurrent());

	customerManager
		.save(customer)
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