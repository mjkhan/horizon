<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
		<div class="main-left">
			<div class="search-inputs">
				<select id="by" class="form-control float-left" style="width:110px;">
					<option value="id" selected>By ID</option>
					<option value="name">By name</option>
				</select>
				<input id="terms" type="text" maxlength="32" placeholder="Enter a search term" class="form-control w-auto float-left"/>
				<button onclick="searchProducts();" class="btn btn-primary">Search</button>
			</div>
			<div class="result-inputs">
				<span id="totalProds"></span>
				<span>
					<button onclick="prodList.addData({}, {local:true});" class="btn btn-primary-line">Add</button>
					<button onclick="removeProducts();" class="btn btn-primary-line enable-oncheck">Remove</button>
				</span>
			</div>
			<div>
				<table class="table info-list">
					<thead>
						<tr><th width="15%"><input id="prodToggler" type="checkbox" onchange="prodList.select(this.checked);"/></th>
							<th width="35%">Product ID</th>
							<th width="50%">Product name</th>
						</tr>
					</thead>
					<%-- html for a row of information. See prodList.onDatasetChange(..) below.
					Specify the 'data-field' attribute on elements bound to properties of information.
					Note that placeholder for properties of data are denoted as '{property name}'.
					--%>
					<template id="row"><tr data-field="{index}"><td><input name="prodIndex" value="{index}" type="checkbox" onchange="prodList.select('{index}', this.checked);"></td>
							<td onclick="prodList.setCurrent('{index}')">{PROD_ID}</td>
							<td onclick="prodList.setCurrent('{index}')">{PROD_NAME} {PROD_TYPE}</td>
						</tr></template>
					<%-- html for when no row of information found. See prodList.onDatasetChange(..) below --%>
					<template id="notFound"><tr><td colspan="3" class="text-center">No product information found.</td></template>
					<tbody id="prod-list" /> <%-- product list displayed here --%>
				</table>
				<ul class="list-group list-group-horizontal paging"></ul> <%-- pagination links --%>
			</div>
		</div>
<c:set var="functions" scope="request">${functions} <%-- To be written at ${functions} of prod-main.jsp --%>
function searchProducts(start) {
	$("#prodToggler").prop("checked", false);
	productManager
		.params({
			by: $(".search-inputs #by").val(),
			terms: $(".search-inputs #terms").val(),
			start: start
		})
		.search();
}

prodList.onDatasetChange = dataset => { <%--called when data is set to prodList --%>
	var trs = [document.getElementById("notFound").innerHTML];<%-- from template#notFound --%>
	
	<%-- Query result is populated to the table --%>
	if (!dataset.empty) {
		var rowTemplate = document.getElementById("row").innerHTML; <%-- from template#row --%>
			trs = prodList.getData("item").map(item => {
				return rowTemplate
					.replace(/{index}/gi, item.index)
					.replace(/{PROD_ID}/gi, item.getValue("PROD_ID"))
					.replace(/{PROD_NAME}/gi, item.getValue("PROD_NAME"))
					.replace(/{PROD_TYPE}/gi, item.getValue("PROD_TYPE"))
			});
	}
	$("#prod-list").html(trs.join());

	let pagination = dataset.pagination;
	pagination.func = "searchProducts({index})";
	$("#totalProds").html((pagination.totalSize || "No") + " products found");
	
	<%-- Pagination links are set. See header.jsp. --%>
	$(".paging").setPaging(pagination);
};

prodList.onSelectionChange = selected => { <%--called when the user (un)selects information--%>
	<%-- Sets the checkboxes --%>
	var selectedIndex = selected.map(e => e.index);
	$("#prod-list input[name='prodIndex']").each(function(){
		var checkbox = $(this),
			value = checkbox.val();
		checkbox.prop("checked", selectedIndex.includes(value));
	});

	<%-- Enables or disables the remove button --%>
	$(".enable-oncheck").prop("disabled", selected.length < 1);
};

function removeProducts() {
	if (!confirm("Are you sure to remove the checked information?")) return;
	
	productManager
		.remove()
		.then(function(resp) {
			if (resp.local) return;
			
			alert(resp.saved ? "Information saved successfully." : "Failed to save the information.");
		});
}</c:set>
<c:set var="onload" scope="request">${onload} <%-- To be written at ${onload} of prod-main.jsp --%>
<%--Sets the product list (and pagination information) from the response
and calls the 'prodList.onDatasetChange' handler
and sets a product information as current information.
--%>
productManager.setData({
	prodList: ${prodList},
	pagination: ${pagination}
});

$(".search-inputs #by").change(function(){$(".search-inputs #terms").focus().select();});
onEnterPress(".search-inputs #terms", () => searchProducts());
</c:set>