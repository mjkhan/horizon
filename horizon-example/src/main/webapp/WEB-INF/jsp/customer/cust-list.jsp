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
					Specify the 'data-field' attribute on elements bound to properties of information.
					Note that placeholder for properties of data are denoted as '{property name}'.
					Like {onclick}, you can also specify placeholder for custom processing.
					--%>
					<template id="row"><tr data-field="{index}"><td><input name="custIndex" value="{index}" type="checkbox" onchange="custList.select('{index}', this.checked);"/></td>
						<td {onclick}>{id}</td>
						<td {onclick}>{name}</td>
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
	
	customerManager
		.params({
			by: $(".search-inputs #by").val(),
			terms: $(".search-inputs #terms").val(),
			start: start || 0
		})
		.search();
}

custList.onDatasetChange = dataset => { <%-- called when data is set to custList --%>
	var trs = [document.getElementById("notFound").innerHTML]; <%-- from template#notFound --%>
		
	<%-- Query result is populated to the table --%>
	if (!custList.empty) {
		trs = custList.inStrings(
			document.getElementById("row").innerHTML, <%-- from template#row --%>
			(template, item) => {
				return template.replace(/{onclick}=""/gi, "onclick=\"custList.setCurrent('{index}');\"")
			}
		); 
	}
	$("#cust-list").html(trs.join());
	
	let pagination = dataset.pagination;
	pagination.func = "searchCustomers({index})";
	$("#totalCusts").html((pagination.totalSize || "No") + " customers found");
	
	<%-- Pagination links are set. See header.jsp. --%>
	$(".paging").setPaging(pagination);
};

custList.onSelectionChange = function(selected) { <%--called when the user (un)selects information--%>
	<%-- Sets the checkboxes --%>
	var selectedIndex = selected.map(e => e.index);
	$("#cust-list input[name='custIndex']").each(function(){
		var checkbox = $(this),
			value = checkbox.val();
		checkbox.prop("checked", selectedIndex.includes(value));
	});

	<%-- Enables or disables the remove button --%>
	$(".enable-oncheck").prop("disabled", selected.length < 1);
};

function removeCustomers() {
	if (!confirm("Are you sure to remove the checked information?")) return;
	
	customerManager
		.remove()
		.then(resp => {
			if (resp.local) return;
			
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
customerManager.setData({
	custList:${custList},
	pagination:${pagination}
});

$(".search-inputs #by").change(function(){$(".search-inputs #terms").focus().select();});
onEnterPress(".search-inputs #terms", () => searchCustomers());</c:set>