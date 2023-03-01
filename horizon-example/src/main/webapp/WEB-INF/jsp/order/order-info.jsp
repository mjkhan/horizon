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
						<tbody id="order-info">
							<tr><th><label for="orderID">Order ID</label></th>
								<td><input id="orderID" type="text" readonly class="form-control" placeholder="To be assigned on save" /></td>
							</tr>
							<tr><th><label for="orderDate">Order date</label></th>
								<td><input id="orderDate" type="text" readonly class="form-control" placeholder="To be assigned on save" /></td>
							</tr>
							<tr><th><label for="CUST_ID" onclick="orderManager.setCustomer();">Customer</label></th>
								<td><input id="custName" type="text" readonly class="form-control" placeholder="" /></td>
							</tr>
							<tr><th><label for="amount">Total amount</label></th>
								<td><input id="amount" type="text" readonly class="form-control" /></td>
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
						<template id="lineRow"><tr data-key="{LINE_ID}" onclick="orderManager.setCurrentLine('{LINE_ID}');">
							<td><input name="lineID" value="{LINE_ID}" type="checkbox" onchange="orderManager.selectLine('{LINE_ID}', this.checked);"/></td>
							<td onclick="orderManager.setProduct('{LINE_ID}');">{PROD_NAME} {PROD_TYPE}</td>
							<td class="text-right">{UNIT_PRICE}</td>
							<td><input name="QNTY" value="{QNTY}" type="text" onchange="orderManager.setLineValue('{LINE_ID}', 'QNTY', this.value);" class="form-control text-right"/></td>
							<td class="text-right">{PRICE}</td>
						</tr></template>
						<%-- html for no row of information. See lineList.onDatasetChange(..) below --%>
						<template id="noLineList"><tr><td colspan="5" class="text-center">No line items found.</td></tr></template>
						<tbody id="line-list" />
					</table>
				</div>
<c:set var="functions" scope="request">${functions} <%-- To be written at ${functions} of order-main.jsp --%>
orderManager.onCurrentOrderChange = function(item) { <%--called when the user changes the current order information--%>
	var found = !isEmpty(item), 
		info = found ? item.data : {};
	
	$("#custName").prop("placeholder", !found ? "" : "Click 'Customer' to select a customer");

	$("#order-list").setCurrentRow(info.ORD_ID); <%-- See header.jsp --%>
	
	var orderID = info.ORD_ID;
	$("#orderID").val(!orderID.startsWith("new order") ? orderID : "To be assigned on save");
	$("#orderDate").val(found ? item.getValue("ORD_DATE") : "");
	$("#custID").val(info.CUST_ID);
	$("#custName").val(info.CUST_NAME);
	$("#amount").val(found ? item.getValue("ORD_AMT") : "");

	$("#btnAddLine").prop("disabled", !found);
}

orderManager.onOrderModify = orderManager.onLineModify = function(changed, item, current){
	drawOrderList();
	orderManager.orders.setState();
};

orderManager.onLineRemove = function(removed) {
	drawLineList();
};

function saveOrder() {
	let order = orderManager.orders.getCurrent(),
		lineList = orderManager.getOrderLines();
	let	validity = orderManager
		.validate()				<%-- See horizon.js --%>
		.onInvalid(val => {
			let lineKey = lineList.getKey(lineList.getDataset()[val.index]);
			switch (val.id) {
			case "CUST_ID": return orderManager.setCustomer();
			case "PROD_ID":
				lineList.setCurrent(lineKey);
				return orderManager.setProduct(lineKey);
			case "QNTY":
				lineList.setCurrent(lineKey);
				return document.getElementsByName("QNTY")[val.index].focus();
			}
		});
	if (!validInput(validity)) return;
	
	orderManager.save().then(resp => {
		if (!resp.saved)
			return alert("Failed to save the information.");
		
		alert("Information saved successfully.");
	});
}

function drawLineList(lineList) {
	if (!lineList)
		lineList = orderManager.getOrderLines();
	var trs = [document.getElementById("noLineList").innerHTML]; <%-- from template#noLineList --%>
		
	<%-- Query result is populated to the table --%>
	if (lineList && !lineList.empty) {
		var rowTemplate = document.getElementById("lineRow").innerHTML; <%-- from template#lineRow --%>
		trs = lineList.getDataset("item").map(row => {
			var added = !row.getValue("PROD_ID");
			return rowTemplate
				.replace(/{LINE_ID}/gi, row.getValue("LINE_ID"))
				.replace(/{PROD_ID}/gi, row.getValue("PROD_ID"))
				.replace(/{PROD_NAME}/gi, "<a onclick='orderManager.setProduct(\"" + row.getValue("LINE_ID") + "\");'>" + (!added ? row.getValue("PROD_NAME") : "Select a product") + "</a>")
				.replace(/{PROD_TYPE}/gi, row.getValue("PROD_TYPE"))
				.replace(/{UNIT_PRICE}/gi, row.getValue("UNIT_PRICE"))
				.replace(/{QNTY}/gi, row.getValue("QNTY"))
				.replace(/{PRICE}/gi, row.getValue("PRICE"));
		});
	}
	$("#line-list").html(trs.join());
}

orderManager.onOrderLinesChange = function(lineList) {
	drawLineList(lineList);
	if (lineList)
		lineList.setState();
};
orderManager.onCurrentLineChange = function(item){
	if (item)
		$("#line-list").setCurrentRow(item.data.LINE_ID);
};

orderManager.onDirtyOrder = function(dirty) {
	$("#btnSave").prop("disabled", !dirty);
}

orderManager.onLineSelectionChange = function(selected){
	var lineList = orderManager.getOrderLines(),
		selectedKeys = lineList ? selected.map(s => lineList.getKey(s)) : [];
	$("#line-list input[name='lineID']").each(function(){
		var checkbox = $(this),
			value = checkbox.val();
		checkbox.prop("checked", selectedKeys.indexOf(value) > -1);
	});
	$("#btnRemoveLines").prop("disabled", selectedKeys.length < 1);
};
orderManager.onNewLine = function(appended){
	drawLineList();
};

orderManager.onCurrentChange = function(info) {
	if (!info) return;
	
	$("#line-list").setCurrentRow(info.LINE_ID);
};</c:set>
<c:set var="onload" scope="request">${onload} <%-- To be written at ${onload} of order-main.jsp --%>
onEnterPress("#order-info input", () => saveOrder());</c:set>