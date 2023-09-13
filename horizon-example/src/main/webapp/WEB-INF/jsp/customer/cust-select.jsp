<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<div class="search-inputs">
	<select id="_by" class="form-control float-left" style="width:110px;">
		<option value="id" selected>By ID</option>
		<option value="name">By name</option>
	</select>
	<input id="_terms" type="text" maxlength="32" placeholder="Enter a search term" class="form-control w-auto float-left"/>
	<button onclick="searchCustomers();" class="btn btn-primary">Search</button>
</div>
<div class="result-inputs">
	<span id="totalCusts"></span>
</div>
<div>
	<table class="table info-list">
		<thead>
			<tr><th width="35%">Customer ID</th>
				<th width="50%">Customer name</th>
			</tr>
		</thead>
		<%-- html for a row of information. See custList.onDatasetChange(..) below
		Note that placeholder for properties of data are denoted as '{property name}'. --%>
		<template id="custRow"><tr data-field="{index}" onclick="custList.setCurrent('{index}')"><td>{id}</td>
			<td>{name}</td>
		</tr></template>
		<%-- html for when no row of information found. See custList.onDatasetChange(..) below --%>
		<template id="custNotFound"><tr><td colspan="2" class="text-center">No customer information found.</td></tr></template>
		<tbody id="cust-list" /> <%-- customer list displayed here --%>
	</table>
	<ul id="_cust-paging" class="list-group list-group-horizontal paging"></ul> <%-- pagination links --%>
</div>
<script type="text/javascript">
var custList = new Dataset({
	onDatasetChange: dataset => {
		let trs = [document.getElementById("custNotFound").innerHTML]; <%-- from template#custNotFound --%>
			
		if (!dataset.empty) {
			trs = dataset.inStrings(document.getElementById("custRow").innerHTML); <%-- from template#custRow --%>
		}
		$("#cust-list").html(trs.join());
		
		let pagination = dataset.pagination;
		pagination.func = "searchCustomers({index})";
		$("#totalCusts").html((pagination.totalSize || "No") + " customers found");
		
		$("#_cust-paging").setPaging(pagination);
	},
	onCurrentChange:function(item){
		$("#cust-list").setCurrentRow(item.index);
	}
});

function setCustData(obj) {
	custList.pagination = obj.pagination;
	custList.setData(obj.custList);
}

function searchCustomers(start) {
	json({
		url:"<c:url value='/customer'/>",
		data:{
			by:$(".search-inputs #_by").val(),
			terms:$(".search-inputs #_terms").val(),
			start:Math.max(start || 0, 0),
			json:true
		},
		success:resp => setCustData(resp)
	});
}

<%-- Called to return the current information --%>
function selectedCustomer() {
	return custList.getCurrent();
}

setCustData({
	custList:${custList},
	pagination:${pagination}
});

$(".search-inputs #_by").change(function(){$(".search-inputs #_terms").focus().select();});
onEnterPress(".search-inputs #_terms", () => searchCustomers());
</script>