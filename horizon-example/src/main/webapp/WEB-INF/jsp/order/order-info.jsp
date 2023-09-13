<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<style>
#tab0 table {margin-bottom:0;}
#tab0 table th, #tab0 table td {padding:.35rem;}
</style>
				<div id="tab0" class="tab-pane fade show active">
					<table class="table info-inputs">
						<colgroup>
							<col width="200">
							<col width="500">
						</colgroup>
						<tbody id="order-info"><%-- Specify the 'data-field' attribute on elements bound to properties of information --%>
							<tr><th><label for="orderID">Order ID</label></th>
								<td><input id="orderID" data-field="ORD_ID" type="text" readonly class="form-control" placeholder="To be assigned on save" /></td>
							</tr>
							<tr><th><label for="orderDate">Order date</label></th>
								<td><input id="orderDate" data-field="ORD_DATE" type="text" readonly class="form-control" placeholder="To be assigned on save" /></td>
							</tr>
							<tr><th><label for="custName" onclick="orderManager.setCustomer();">Customer</label></th>
								<td><input id="custName" data-field="CUST_NAME" type="text" readonly class="form-control" placeholder="Click 'Customer' to select a customer" /></td>
							</tr>
							<tr><th><label for="amount">Total amount</label></th>
								<td><input id="amount" data-field="ORD_AMT" type="text" readonly class="form-control" /></td>
							</tr>
						</tbody>
					</table>
					<div class="d-flex justify-content-between" style="padding:.35em 0;">
						<span>
							<button id="btnAddLine" onclick="orderManager.newLine();" class="btn btn-primary-line">Add</button>
							<button id="btnRemoveLines" onclick="orderManager.removeLines();" class="btn btn-primary-line">Remove</button>
						</span>
						<button id="btnSave" onclick="saveOrder();" class="btn btn-primary">Save</button>
					</div>
					<table class="table info-list">
						<thead>
							<tr><th width="15%"><input type="checkbox" onchange="orderManager.selectLine(this.checked);"/></th>
								<th width="35%"><label for="PROD_ID">Product</label></th>
								<th width="50%" class="text-right">Unit price</th>
								<th width="50%" class="text-right"><label for="QNTY">Quantity</label></th>
								<th width="50%" class="text-right">Price</th>
							</tr>
						</thead>
						<%-- html for a row of information. See lineList.onDatasetChange(..) below.
						Note that placeholder for properties of data are denoted as '{property name}'. --%>
						<template id="lineRow"><tr data-field="{index}" onclick="orderManager.setCurrentLine('{index}');">
							<td><input name="lineIndex" value="{index}" type="checkbox" onchange="orderManager.selectLine('{index}', this.checked);"/></td>
							<td onclick="orderManager.setProduct('{index}');">{PROD_NAME} {PROD_TYPE}</td>
							<td class="text-right">{UNIT_PRICE}</td>
							<td><input name="QNTY" value="{QNTY}" type="text" onchange="orderManager.setLineValue('{index}', 'QNTY', this.value);" class="form-control text-right"/></td>
							<td class="text-right">{PRICE}</td>
						</tr></template>
						<%-- html for no row of information. See lineList.onDatasetChange(..) below --%>
						<template id="noLineList"><tr><td colspan="5" class="text-center">No line items found.</td></tr></template>
						<tbody id="line-list" />
					</table>
				</div>
<c:set var="functions" scope="request">${functions} <%-- To be written at ${functions} of order-main.jsp --%>
orderManager.onCurrentOrderChange = item => { <%--called when the user changes the current order information--%>
	$("#order-list").setCurrentRow(item.index); <%-- See header.jsp --%>

	$("#order-info input[data-field]").each(function() {
		let input = $(this),
			dataField = input.attr("data-field");
		input.val(item.getValue(dataField));
	});
}

orderManager.onOrderModify = (changed, item, current) => {
	orderManager.orders.setState();
};

orderManager.onLineModify = (changed, item, current) => {
	orderManager.getOrderLines().setState();
};

function saveOrder() {
	let order = orderManager.orders.getCurrent(),
		lineList = orderManager.getOrderLines();
	let	validity = orderManager
		.validate()			<%-- See horizon.js --%>
		.onInvalid(v => {
			if ("CUST_NAME" == v.id)
				return orderManager.setCustomer();
				
			let lineItem = lineList.getData("item")[v.index],
				lineIndex = lineItem.index;
			switch (v.id) {
			case "PROD_ID":
				lineList.setCurrent(lineIndex);
				return orderManager.setProduct(lineIndex);
			case "QNTY":
				lineList.setCurrent(lineIndex);
				return document.getElementsByName("QNTY")[v.index].focus();
			}
		});
	if (!validInput(validity)) return;
	
	orderManager.save().then(resp => {
		if (!resp.saved)
			return alert("Failed to save the information.");
		
		alert("Information saved successfully.");
	});
}

orderManager.onOrderLinesChange = lineList => {
	let trs = [document.getElementById("noLineList").innerHTML]; <%-- from template#noLineList --%>
		
	<%-- Query result is populated to the table --%>
	if (!lineList.empty) {
		trs = lineList.inStrings(
			document.getElementById("lineRow").innerHTML, <%-- from template#lineRow --%>
			(template, item) => template.replace(
				/{PROD_NAME}/gi,
				"<a onclick='orderManager.setProduct(\"{index}\");'>" + (item.getValue("PROD_NAME") ? "{PROD_NAME}" : "Select a product") + "</a>"
			)
		);
	}
	$("#line-list").html(trs.join());
	orderManager.setDirtyState();
}

orderManager.onCurrentLineChange = item => {
	$("#line-list").setCurrentRow(item.index);
};

orderManager.onDirtyOrder = function(dirty) {
	$("#btnSave").prop("disabled", !dirty);
}

orderManager.onLineSelectionChange = function(selected){
	var selectedIndex = selected.map(e => e.index);
	$("#line-list input[name='lineIndex']").each(function(){
		var checkbox = $(this),
			value = checkbox.val();
		checkbox.prop("checked", selectedIndex.includes(value));
	});
	$("#btnRemoveLines").prop("disabled", selectedIndex.length < 1);
};

orderManager.onCurrentChange = item => {
	$("#line-list").setCurrentRow(item.index);
};</c:set>
<c:set var="onload" scope="request">${onload} <%-- To be written at ${onload} of order-main.jsp --%>
onEnterPress("#order-info input", () => saveOrder());</c:set>