<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<div class="search-inputs">
	<select id="_by" class="form-control float-left" style="width:110px;">
		<option value="id" selected>By ID</option>
		<option value="name">By name</option>
	</select>
	<input id="_terms" type="text" maxlength="32" placeholder="Enter a search term" class="form-control w-auto float-left"/>
	<button onclick="searchProducts();" class="btn btn-primary">Search</button>
</div>
<div>
	<table class="table info-list">
		<thead>
			<tr><th width="35%">Product ID</th>
				<th width="50%">Product name</th>
			</tr>
		</thead>
		<%-- html for a row of information. See prodList.onDatasetChange(..) below
		Note that placeholder for properties of data are denoted as '{property name}'. --%>
		<template id="prodRow"><tr data-key="{PROD_ID}" onclick="prodList.setCurrent({PROD_ID})"><td><a>{PROD_ID}</a></td>
			<td><a>{PROD_NAME} {PROD_TYPE}</a></td>
		</tr></template>
		<%-- html for when no row of information found. See prodList.onDatasetChange(..) below --%>
		<template id="prodNotFound"><tr><td colspan="2" class="text-center">No product information found.</td>
		</tr></template>
		<tbody id="prod-list"> <%-- prouct list displayed here --%>
		</tbody>
	</table>
	<ul id="_prod-paging" class="list-group list-group-horizontal paging"></ul> <%-- pagination links --%>
</div>
<script type="text/javascript">
var prodList = new Dataset({
	keymapper:function(info) {return info ? info.PROD_ID : "";},
	dataGetter:function(obj) {return obj.prodList;},
	
	onDatasetChange:resp => {
		var trs = [document.getElementById("prodNotFound").innerHTML]; <%-- from template#prodNotFound --%>
		
		if (!prodList.empty) {
			var rowTemplate = document.getElementById("prodRow").innerHTML; <%-- from template#prodRow --%>
			trs = prodList.inStrings(rowTemplate);
		}
		$("#prod-list").html(trs.join());
		$("#totalProds").html((resp.totalSize || "No") + " products found");
		
		$("#_prod-paging").setPaging({
			start:resp.start,
			fetchSize:resp.fetch,
			totalSize:resp.totalSize,
			func:"searchProducts({index})"
		});
	},
	onCurrentChange:item => {
		if (item)
			$("#prod-list").setCurrentRow(item.getValue("PROD_ID"));
	}
});

function searchProducts(start) {
	json({
		url:"<c:url value='/product'/>",
		data:{
			by:$(".search-inputs #_by").val(),
			terms:$(".search-inputs #_terms").val(),
			start:Math.max(start || 0, 0)
		},
		success:resp => prodList.setData(resp)
	});
}

<%-- Called to return the current information --%>
function selectedProduct() {
	return prodList.getCurrent();
}

prodList.setData({
	prodList:${prodList},
	start:0,
	fetch:${fetch},
	totalSize:${totalSize}
});

$(".search-inputs #_by").change(function(){$(".search-inputs #_terms").focus().select();});
onEnterPress(".search-inputs #_terms", () => searchProducts());
</script>