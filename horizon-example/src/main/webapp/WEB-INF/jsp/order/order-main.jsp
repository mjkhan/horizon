<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
		<jsp:include page="order-list.jsp"/>
		<div class="main-right flex-grow-1">
			<ul class="nav nav-pills mb-3 justify-content-between" style="width:90%;">
				<li class="nav-item"><a href="#tab0" class="nav-link active" data-toggle="pill">Properties</a></li>
			</ul>
			
			<div class="tab-content" style="width:90%;">
				<jsp:include page="order-info.jsp"/>
			</div>
		</div>
<script type="text/javascript">
var orderManager = {
	orders:new Dataset({ <%-- Order dataset from querying orders --%>
		keymapper:function(info) {return info ? info.ORD_ID : "";},
		dataGetter:function(obj) {return obj.orderList;},
		
		formats:{
			ORD_DATE:dateFormat,
			ORD_AMT:numberFormat
		},
		
		onDatasetChange:function(obj){
			orderManager.onOrdersChange(obj);
			orderManager.setOrderLines(orderManager.currentOrderID(), obj);
		},
		onCurrentChange:function(item){
			orderManager.onCurrentOrderChange(item);
			var orderID = orderManager.orders.getKey(item);
			orderManager.setCurrentLines(orderID);
			orderManager.setDirtyOrder();
		},
		onSelectionChange:function(selected){orderManager.onOrderSelectionChange(selected);},
		
		onAppend:function(appended){orderManager.onNewOrder();},
		onModify:function(changed, item, current){orderManager.onOrderModify(changed, item, current);},
		onReplace:function(){orderManager.onOrderModify();},
		onErase:function(erased){
			orderManager.onOrderModify();
		},
		onDirtiesChange:function(dirty){},
	}),
	
	orderLines:new Dataset({ <%-- dataset of datasets for line items of each order --%>
		keymapper:function(info){return info.orderID;},
		dataGetter:function(obj){},
		
		onCurrentChange:function(item){
			if (!item) return;
			orderManager.onOrderLinesChange(item.data.dataset);
		}
	}),
	
	hasDirtyOrders:function(){
		var dirty = this.orders.dirty;
		if (!dirty) {
			for (var orderID in this.orderLines) {
				var lineList = this.getOrderLines(orderID);
				dirty = lineList && lineList.dirty;
				if (dirty)
					break;
			}
		}
		return dirty;
	},

	setCustomer:function() {
		if (orderManager.orders.empty) return;
		
		ajax({
			url:"<c:url value='/customer/select'/>",
			success:function(resp) {
				dialog.open({
					id:"select-customer",
					content:resp,
					getData:function() {
						return selectedCustomer();
					},
					onOK:function(selected) {
						var orderList = orderManager.orders,
							currentOrder = orderList.getCurrent();
						if (!currentOrder) return;
						
						orderList.modify(
							orderList.getKey(currentOrder),
							function(item) {
								var info = item.data;
								info.CUST_ID = selected.id;
								info.CUST_NAME = selected.name;
							}
						);
					}
				});
			}
		});
	},
	
	getOrderLines:function(orderID){
		var lines = orderID ? this.orderLines.getData(orderID) : this.orderLines.getCurrent();
		return lines ? lines.dataset : null;
	},

	setCurrentLines:function(orderID){
		this.orderLines.setCurrent(orderID);
		var lineList = this.getOrderLines();
		if (lineList)
			return orderManager.onOrderLinesChange(lineList);

		if (!orderID || orderID.startsWith("new order"))
			return orderManager.setOrderLines(orderID, {lineList:[]});

		json({
			url:"<c:url value='/order/lines'/>",
			data:{
				orderID:orderID,
				json:true
			},
			success:function(resp){
				orderManager.setOrderLines(orderID, resp);
			}
		});
	},
	
	getLineList:function(obj) {
		return new Dataset({
			keymapper:function(info){return info.LINE_ID;},
			dataGetter:function(obj){return obj.lineList;},
			
			formats:{
				UNIT_PRICE:numberFormat,
				QNTY:numberFormat,
				PRICE:numberFormat
			},
			
			onDatasetChange:function(obj){
				orderManager.onOrderLinesChange();
			},
			onCurrentChange:orderManager.onCurrentLineChange,
			onSelectionChange:orderManager.onLineSelectionChange,
			onAppend:orderManager.onNewLine,
			onModify:orderManager.onLineModify,
			onReplace:orderManager.onLineModify,
			onDirtiesChange:function(dirty){orderManager.setDirtyOrder();},
			onRemove:function(removed){orderManager.onLineRemove(removed);},
			onErase:function(erased){orderManager.onLineRemove(erased);}
		}).setData(obj);
	},
	
	setOrderLines:function(orderID, obj){
		var dataset = orderManager.getLineList(obj);
		orderManager.orderLines.append({orderID:orderID, dataset:dataset});
	},

	setProduct:function(key) {
		var lineList = orderManager.getOrderLines(),
			current = lineList.getData(key);
		if (!current) return;
		
		ajax({
			url:"<c:url value='/product/select'/>",
			success:function(resp) {
				dialog.open({
					id:"select-product",
					content:resp,
					getData:function() {
						return selectedProduct();
					},
					onOK:function(selected) {
						lineList.modify(
							lineList.getKey(current),
							function(item) {
								var info = item.data;
								info.PROD_ID = selected.PROD_ID;
								info.PROD_NAME = selected.PROD_NAME;
								info.PROD_TYPE = selected.PROD_TYPE;
								info.UNIT_PRICE = selected.UNIT_PRICE;
								orderManager.setPrice(info);
							}
						);
					}
				});
			}
		});
	},
	
	removeLines:function() {
		var lineList = orderManager.getOrderLines();
		lineList.remove(lineList.getKeys("selected"));
		orderManager.setTotalAmount();
	},
	
	setTotalAmount:function(){
		var lines = orderManager.getOrderLines().getDataset(),
			totalAmount = lines.reduce(function(sum, e){return sum + e.PRICE;}, 0);
		orderManager.orders.setValue("ORD_AMT", totalAmount);
	},
	
	setDirtyOrder:function(){
		var order = orderManager.orders.getCurrent("item"),
			lineList = orderManager.getOrderLines(),
			dirty = order && lineList && !lineList.empty && (order.dirty || lineList.dirty);
		orderManager.onDirtyOrder(dirty);
	},
	onDirtyOrder:function(dirty){},

	search:function(by, terms, start){
		var _search = function(state, prev){
			if (prev) {
				start = (start || 0) - ${fetch};
			}
			json({
				url:"<c:url value='/order'/>",
				data:{
					by:by,
					terms:terms,
					start:Math.max(start || 0, 0)
				},
				success:function(resp) {
					resp.state = state;
					orderManager.setOrders(resp);
				}
			});
		};
		orderManager.reload = function(prev) {
			_search(orderManager.orders.state, prev);
		};
		_search();
	},
	
	setOrders:function(obj){
		this.orderLines.setData();
		this.orders.setData(obj);
	},
	onOrdersChange:function(obj){},
	
	currentOrderID:function(){
		return this.orders.getKey(this.orders.getCurrent()) || "orderNotFound";
	},
	setCurrentOrder:function(key){
		this.orders.setCurrent(key);
	},
	onCurrentOrderChange:function(item){},
	
	selectOrder:function(key, selected){
		this.orders.select(key, selected);
	},
	onOrderSelectionChange:function(selected){},
	
	newOrder:function(){
		var orderList = this.orders,
			now = new Date().getTime(),
			key = "new order " + now,
			order = {ORD_ID:key, ORD_DATE:now, CUST_NAME:""};
		orderList.append(order);
	},
	onNewOrder:function(appended){},
	onOrderModify:function(changed, item, current){},
	
	onOrderLinesChange:function(obj){},
	
	setCurrentLine:function(key){
		orderManager.getOrderLines().setCurrent(key);
	},
	onCurrentLineChange:function(item){},
	
	selectLine:function(key, selected){
		this.getOrderLines().select(key, selected);
	},
	onLineSelectionChange:function(selected){},
	
	newLine:function(){
		var	orderID = this.orders.getCurrent().ORD_ID, 
			lineList = this.getOrderLines(),
			lineID = "new line " + new Date().getTime(),
			line = {ORD_ID:orderID, LINE_ID:lineID, PROD_ID:"", PROD_NAME:"", PROD_TYPE:"", UNIT_PRICE:0, QNTY:0, PRICE:0};
		lineList.append(line);
	},
	onNewLine:function(appended){},
	
	setLineValue:function(key, property, value){
		var lineList = this.getOrderLines();
		lineList.modify(key, function(item){
			item.setValue(property, value);
			
			orderManager.setPrice(item.data);
		});
	},
	
	setPrice:function(lineInfo){
		lineInfo.PRICE = lineInfo.UNIT_PRICE * (lineInfo.QNTY || 0);
		orderManager.setTotalAmount();
	},
	onLineModify:function(changed, item, current){},
	
	onLineRemove:function(removed) {},
	
	validate:function(){
		var order = this.orders.getCurrent(),
			lineList = this.getOrderLines().getDataset();
		if (isEmpty(order.CUST_ID))
			return new Validity().valueMissing("CUST_ID");
		
		for (var i = 0, length = lineList.length; i < length; ++i) {
			var line = lineList[i];
			if (isEmpty(line.PROD_ID))
				return new Validity().valueMissing("PROD_ID", i);
			if (isEmpty(line.QNTY) || line.QNTY < 1)
				return new Validity().rangeUnderflow("QNTY", 1, i);
		}

		return new Validity();
	},
	
	order(info) {
		return {
			id:info.ORD_ID,
			date:info.ORD_DATE,
			custID:info.CUST_ID,
			amount:info.ORD_AMT
		};
	},
	
	orderLine(info) {
		return {
			orderID:info.ORD_ID,
			id:info.LINE_ID,
			prodID:info.PROD_ID,
			quantity:info.QNTY,
			price:info.PRICE
		};
	},
	
	save:function(){
		let order = this.order(this.orders.getCurrent()),
			lineList = this.getOrderLines().getDataset("dirty"),
			orderLines = {};
		for (let status in lineList) {
			orderLines[status] = lineList[status].map(this.orderLine);
		}

		if (order.id.startsWith("new order")) {
			order.lineItems = orderLines["added"];
			return new Promise(function(resolve, reject){
				json({
					url:"<c:url value='/order/create'/>",
					type:"POST",
					data:JSON.stringify(order),
					success:function(resp) {
						resolve(resp);
						if (resp.saved) {
							var orderInfo = resp.orderInfo,
								lineList = orderManager.getLineList(resp);
							orderManager.orders.replace({key:order.id, data:orderInfo});
							orderManager.orderLines.replace({key:order.id, data:{orderID:orderInfo.ORD_ID, dataset:lineList}});
						}
					}
				});
			});
		} else {
			return new Promise(function(resolve, reject){
				json({
					url:"<c:url value='/order/update'/>",
					type:"POST",
					data:JSON.stringify({
						order:order,
						lines:orderLines
						}),
					success:function(resp) {
						resolve(resp);
						if (resp.saved) {
							var orderInfo = resp.orderInfo,
								lineList = orderManager.getLineList(resp);
							orderManager.orders.replace({data:orderInfo});
							orderManager.orderLines.replace({data:{orderID:orderInfo.ORD_ID, dataset:lineList}});
						}
					}
				});
			});
		}
	},
	
	remove:function(){
		var orderList = this.orders,
			selectedIDs = orderList.getKeys("selected");
		if (selectedIDs.length < 1)
			throw "Select orders to remove.";
		
		<%--When all information on the current page is removed,
		information on the previous page is requested.--%>
		var prev = orderList.length == selectedIDs.length,
			added = selectedIDs.filter(function(orderID){return orderID.startsWith("new order");}),
			orderIDs = selectedIDs.filter(function(orderID){return !orderID.startsWith("new order");}),
			eraseOrders = function(){
				orderManager.orderLines.erase(selectedIDs);
				orderList.erase(selectedIDs);
			};
		
		return new Promise(function(resolve, reject){
			if (orderIDs.length > 0)
				json({
					url:"<c:url value='/order/remove'/>",
					type:"POST",
					data:{orderIDs:orderIDs.join(",")},
					success:function(resp) {
						resolve(resp);
						if (resp.saved) {
							if (prev)
								orderManager.reload(prev);
							else
								eraseOrders();
						}
					}
				});
			else {
				resolve({saved:true});
				if (prev)
					orderManager.reload(prev);
				else
					eraseOrders();
			}
		});
	}
};

${functions} <%--Placeholder for functions and variables from included JSPs--%>
$(function(){
	${onload} <%--Placeholder for codes from included JSPs to be executed when the page is ready--%>
});
//# sourceURL=order-main.js
</script>