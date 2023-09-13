<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
		<div class="main-left">
			<div class="search-inputs">
				<select id="by" class="form-control float-left" style="width:110px;">
					<option value="ordDate" selected>Order date</option>
					<option value="custName">Customer name</option>
				</select>
				<input id="terms" type="text" maxlength="32" placeholder="Enter a search term" class="form-control w-auto float-left"/>
				<button onclick="searchOrders();" class="btn btn-primary">Search</button>
			</div>
			<div class="result-inputs">
				<span id="totalOrders"></span>
				<span>
					<button id="btnAdd" onclick="orderManager.newOrder();" class="btn btn-primary-line">Add</button>
					<button id="btnRemove" onclick="removeOrders();" class="btn btn-primary-line">Remove</button>
				</span>
			</div>
			<div>
				<table class="table info-list">
					<thead>
						<tr><th width="15%"><input id="orderToggler" type="checkbox" onchange="orderManager.selectOrder(this.checked);"/></th>
							<th width="35%">Order date</th>
							<th width="50%">Customer name</th>
						</tr>
					</thead>
					<%-- html for a row of information. See orderList.onDatasetChange(..) below.
					Specify the 'data-field' attribute on elements bound to properties of information.
					Note that placeholder for properties of data are denoted as '{property name}'.
					--%>
					<template id="row"><tr data-field="{index}" onclick="orderManager.setCurrentOrder('{index}')">
						<td><input name="orderIndex" value="{index}" type="checkbox" onchange="orderManager.selectOrder('{index}', this.checked);"/></td>
						<td>{ORD_DATE}</td>
						<td>{CUST_NAME}</td>
					</tr></template>
					<%-- html for no row of information. See orderList.onDatasetChange(..) below --%>
					<template id="notFound"><tr><td colspan="3" class="text-center">No order information found.</td></tr></template>
					<tbody id="order-list" /> <%-- order list displayed here --%>
				</table>
				<ul class="list-group list-group-horizontal paging"></ul> <%-- pagination links --%>
			</div>
		</div>
<c:set var="functions" scope="request">${functions} <%-- To be written at ${functions} of cust-main.jsp --%>
function searchOrders(start) {
	if (orderManager.hasDirtyOrders()
	 && !confirm("Do you wish to disregard the change made to order information?"))
	 return;

	$("#orderToggler").prop("checked", false);

	orderManager
		.params({
			by: $(".search-inputs #by").val(),
			terms: $(".search-inputs #terms").val(),
			start: start
		})
		.search();
}

orderManager.onOrdersChange = dataset => { <%--called when orderList is changed--%>
	let trs = [document.getElementById("notFound").innerHTML]; <%-- from template#notFound --%>
	if (!dataset.empty) {
		trs = dataset.inStrings(document.getElementById("row").innerHTML);<%-- from template#row --%>
	}
	
	$("#order-list").html(trs.join()); <%-- Query result is populated to the table --%>
	
	<%-- Pagination links are set. See header.jsp. --%>
	let pagination = dataset.pagination;
	pagination.func = "searchOrders({index})";
	$("#totalOrders").html((pagination.totalSize || "No") + " orders found");
	$(".paging").setPaging(pagination);

	$("#btnAddLine").prop("disabled", dataset.empty);
};

orderManager.onOrderSelectionChange = function(selected) { <%--called when the user (un)selects order information--%>
	var selectedIndex = selected.map(e => e.index);
	$("#order-list input[name='orderIndex']").each(function(){ <%-- Sets the checkboxes --%>
		var checkbox = $(this),
			value = checkbox.val();
		checkbox.prop("checked", selectedIndex.indexOf(value) > -1);
	});

	$("#btnRemove").prop("disabled", selected.length < 1); <%-- Enables or disables the remove button --%>
};

function removeOrders() {
	if (!confirm("Are you sure to remove the checked information?")) return;
	
	orderManager
		.remove()
		.then(function(resp) {
			if (resp.local) return;
			
			if (!resp.saved)
				return alert("Failed to save the information.");
			
			alert("Information saved successfully.");
		});
}</c:set>
<c:set var="onload" scope="request">${onload} <%-- To be written at ${onload} of cust-main.jsp --%>
<%--Sets the order list (and pagination information) from the response
and calls the 'orderList.onDatasetChange' handler
and sets a order information as current information.
--%>
orderManager.setOrders({
	orderList:${orderList},
	pagination:${pagination}
});

$(".search-inputs #by").change(function(){$(".search-inputs #terms").focus().select();});
onEnterPress(".search-inputs #terms", () => searchOrders());</c:set>