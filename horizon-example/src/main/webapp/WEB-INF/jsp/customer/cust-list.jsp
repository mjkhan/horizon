<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
		<div class="main-left">
			<div class="search-inputs">
				<select id="by" class="form-control float-left" style="width:110px;">
					<option value="id" selected>By ID</option>
					<option value="name">By name</option>
				</select>
				<input id="terms" type="text" maxlength="32" placeholder="Enter a search term" class="form-control w-auto float-left"/>
				<button onclick="searchCustomers();" class="btn btn-primary">Search</button>
			</div>
			<div class="result-inputs">
				<span id="totalCusts"></span>
				<span>
					<button id="btnAdd" onclick="newCustomer();" class="btn btn-primary-line">Add</button>
					<button onclick="removeCustomers();" class="btn btn-primary-line enable-oncheck">Remove</button>
				</span>
			</div>
			<div>
				<table class="table info-list">
					<thead>
						<tr><th width="15%"><input id="custToggler" type="checkbox" onchange="custList.select(this.checked);"/></th>
							<th width="35%">Customer ID</th>
							<th width="50%">Customer name</th>
						</tr>
					</thead>
					<%-- html for a row of information. See custList.onDatasetChange(..) below.
					Note that placeholder for properties of data are denoted as '{property name}'. --%>
					<template id="row"><tr data-key="{id}"><td><input name="custID" value="{id}" type="checkbox" onchange="custList.select('{id}', this.checked);"/></td>
						<td><a onclick="custList.setCurrent('{id}')">{id}</a></td>
						<td><a onclick="custList.setCurrent('{id}')">{name}</a></td>
					</tr></template>
					<%-- html for when no row of information found. See custList.onDatasetChange(..) below --%>
					<template id="notFound"><tr><td colspan="3" class="text-center">No customer information found.</td></tr></template>
					<tbody id="cust-list" /> <%-- customer list displayed here --%>
				</table>
				<ul class="list-group list-group-horizontal paging"></ul> <%-- pagination links --%>
			</div>
		</div>
<c:set var="functions" scope="request">${functions} <%-- To be written at ${functions} of cust-main.jsp --%>
function searchCustomers(start) {
	if (custList.dirty
	&& !confirm("Do you wish to disregard the change made to customer information?")) return;
	
	$("#custToggler").prop("checked", false);
	customerManager.search(
		$(".search-inputs #by").val(),
		$(".search-inputs #terms").val(),
		start || 0
	);
}

function drawCustList() {
	var trs = [document.getElementById("notFound").innerHTML]; <%-- from template#notFound --%>
		
	<%-- Query result is populated to the table --%>
	if (!custList.empty) {
		trs = custList.inStrings(document.getElementById("row").innerHTML); <%-- from template#row --%>
	}
	$("#cust-list").html(trs.join());
}

custList.onDatasetChange = function(resp) { <%-- called when data is set to custList --%>
	drawCustList();
	$("#totalCusts").html((resp.totalSize || "No") + " customers found");
	
	<%-- Pagination links are set. See header.jsp. --%>
	$(".paging").setPaging({
		start:resp.start,
		fetchSize:resp.fetch,
		totalSize:resp.totalSize,
		func:"searchCustomers({index})"
	});
};

custList.onAppend = drawCustList; <%--called when a new info is added to custList--%>

custList.onSelectionChange = function(selected) { <%--called when the user (un)checks customer information--%>
	<%-- Sets the checkboxes --%>
	var selectedKeys = selected.map(e => custList.getKey(e));
	$("#cust-list input[name='custID']").each(function(){
		var checkbox = $(this),
			value = checkbox.val();
		checkbox.prop("checked", selectedKeys.indexOf(value) > -1);
	});

	<%-- Enables or disables the remove button --%>
	$(".enable-oncheck").prop("disabled", selected.length < 1);
};

custList.onRemove = drawCustList;

function newCustomer() {
	var now = new Date().getTime(),
		cust = {id:"new customer " + now, name:"", createdAt:now, lastModified:now};
	custList.append(cust);
}

function removeCustomers() {
	if (!confirm("Are you sure to remove the checked information?")) return;
	
	customerManager
		.remove()
		.then(resp => {
			if (!resp.saved)
				return alert("Failed to save the information.");
			
			alert("Information saved successfully.");
		});
}</c:set>
<c:set var="onload" scope="request">${onload} <%-- To be written at ${onload} of cust-main.jsp --%>
<%--Sets the customer list (and pagination information) from the response
and calls the 'custList.onDatasetChange' handler
and sets a customer information as current information.
--%>
custList.setData({
	custList:${custList},
	start:0,
	fetch:${fetch},
	totalSize:${totalSize}
});

$(".search-inputs #by").change(function(){$(".search-inputs #terms").focus().select();});
onEnterPress(".search-inputs #terms", () => searchCustomers());</c:set>