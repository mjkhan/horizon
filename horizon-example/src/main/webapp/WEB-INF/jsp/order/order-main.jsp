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
<%--
Controls request and response to and from the OrderController.
As a delegate or wrapper of datasets of orders and line items,
interfaces with HTML elements in response to events from both ends.
 --%>
	orders:new Dataset({ <%-- Order dataset from querying orders --%>
		keys: ["ORD_ID"],  <%-- Used to identify current and/or selected DataItems by the key in orders.setState() to refresh the current state --%>
		formats:{
			ORD_DATE:dateFormat,
			ORD_AMT:numberFormat
		},
		
		onDatasetChange: dataset => {
			orderManager.onOrdersChange(dataset);
		},
		onCurrentChange:item => {                    <%-- When the current order is changed --%>
			orderManager.onCurrentOrderChange(item); <%-- Updates the display of the current order --%>
			orderManager.setCurrentLines(item);      <%-- Gets the line items of the current order --%>
			orderManager.setDirtyState();            <%-- Updates the dirty state of the current  --%>
		},
		onSelectionChange:function(selected){orderManager.onOrderSelectionChange(selected);},
		onModify:function(changed, item, current){orderManager.onOrderModify(changed, item, current);},
		onDirtiesChange:function(dirty){},
	}),
	
	orderLines:new Dataset({ <%-- dataset of datasets for line items of each order --%>
		onCurrentChange:function(item){ <%-- when the current dataset of line items is changed --%>
			if (item.empty) return;
			orderManager.onOrderLinesChange(item.data.dataset); <%-- Updates the display of the line items --%>
			item.data.dataset.setState();
		}
	}),
	
	hasDirtyOrders:function(){
		let dirty = this.orders.dirty;
		if (!dirty) {
			for (let orderID in this.orderLines) {
				let lineList = this.getOrderLines(orderID);
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
							currentOrder = orderList.getCurrent("item");
						if (!currentOrder) return;
						
						orderList.modify(
							currentOrder.index,
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
	
	getOrderLines:function(orderIndex){ <%-- Returns line items of the specified order --%>
		var item = orderIndex ? this.orderLines.getItem({orderIndex: orderIndex}) : this.orderLines.getCurrent("item");
		return item ? item.data.dataset : null;
	},

	setCurrentLines:function(item){ <%-- Sets the line items of the order as the current line items. --%>
		let orderIndex = item.index;
		if (!orderIndex) {
			return this.setOrderLines(orderIndex, {lineList:[]});
		}

		let lineList = this.getOrderLines(orderIndex);
		if (lineList) {
			return this.orderLines.setCurrent({orderIndex: orderIndex});
		} else {
			if (item.isNew()) <%-- If the order is new, sets an empty list --%>
				return this.setOrderLines(orderIndex, {lineList:[]});
		}

		json({ <%-- If not loaded, gets line items from the server. --%>
			url:"<c:url value='/order/lines'/>",
			data:{
				orderID: item.getValue("ORD_ID"),
				json:true
			},
			success:function(resp){
				orderManager.setOrderLines(orderIndex, resp);
			}
		});
	},
	
	newLineList:function(lineList) { <%-- Dataset for line items loaded from the server --%>
		return new Dataset({
			keys:["LINE_ID"],
			
			formats:{
				UNIT_PRICE:numberFormat,
				QNTY:numberFormat,
				PRICE:numberFormat
			},
			
			onDatasetChange: dataset => {
				orderManager.onOrderLinesChange(dataset);
				orderManager.setDirtyState();
			},
			onCurrentChange:orderManager.onCurrentLineChange,
			onSelectionChange:orderManager.onLineSelectionChange,
			onModify:orderManager.onLineModify,
			onDirtiesChange:function(dirty){orderManager.setDirtyState();}
		}).setData(lineList);
	},
	
	setOrderLines:function(orderIndex, obj){ <%-- Sets obj.lineList from the server as the line items of the specified order --%>
		var dataset = orderManager.newLineList(obj.lineList);
		if (orderIndex)
			orderManager.orderLines.addData({orderIndex:orderIndex, dataset:dataset});
		else
			dataset.setState();
	},
	
	setProduct:function(index) {
		var lineList = orderManager.getOrderLines();
		
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
							index,
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
		lineList.remove("selected");
		orderManager.setTotalAmount();
	},
	
	setTotalAmount:function(){
		var lines = orderManager.getOrderLines().getData(),
			totalAmount = lines.reduce(function(sum, e){return sum + e.PRICE;}, 0);
		orderManager.orders.setValue("ORD_AMT", totalAmount);
	},
	
	setDirtyState:function(){
		var order = orderManager.orders.getCurrent("item"),
			lineList = order ? orderManager.getOrderLines(order.index) : null,
			dirty = order && lineList && !lineList.empty && (order.dirty || lineList.dirty);
		orderManager.onDirtyOrder(dirty);
	},
	onDirtyOrder:function(dirty){},
	
	_params: {},
	
	params: function(obj) { <%-- Sets parameters to search information --%>
		orderManager._params = obj;
		return orderManager
	},

	search:function(option){
		json({
			url:"<c:url value='/order'/>",
			data:orderManager._params,
			success:function(resp) {
				orderManager.setOrders(resp, option);
			}
		});
	},
	
	reload:function(prev){ <%--Reloads order information after create, update, or remove requests with the last query terms.--%>
		if (prev) {
			let start = (orderManager._params.start || 0) - orderManager.orders.pagination.fetchSize;
			orderManager._params.start = Math.max(start, 0);
		}
		orderManager.search({stateful: true});
	},
	
	setOrders:function(obj, option){
		if (!this.orderLines.empty)
			this.orderLines.clear();
		let orderList = obj.orderList;
		this.orders.pagination = obj.pagination;
		this.orders.setData(orderList, option);
	},
	onOrdersChange:function(obj){},

	setCurrentOrder:function(index){
		this.orders.setCurrent(index);
	},
	onCurrentOrderChange:function(item){},
	
	selectOrder:function(index, selected){
		this.orders.select(index, selected);
	},
	onOrderSelectionChange:function(selected){},
	
	newOrder:function(){
		let order = ["ORD_ID", "ORD_DATE", "CUST_ID", "CUST_NAME", "ORD_AMT"]
			.reduce((obj, col) => {obj[col] = null; return obj;}, {});
		order.ORD_DATE = new Date();
		orderManager.orders.addData(order, {local: true});
	},
	
	getOrder(info) {
		return {
			id:info.ORD_ID,
			date:info.ORD_DATE,
			custID:info.CUST_ID,
			amount:info.ORD_AMT
		};
	},
	
	onOrderModify:function(changed, item, current){},
	
	onOrderLinesChange:function(obj){}, <%-- see order-info.jsp --%>
	
	setCurrentLine:function(index){
		orderManager.getOrderLines().setCurrent(index);
	},
	onCurrentLineChange:function(item){},
	
	selectLine:function(index, selected){
		this.orderLines.getCurrent().dataset.select(index, selected);
	},
	onLineSelectionChange:function(selected){},
	
	newLine:function(){
		let line = ["ORD_ID", "LINE_ID", "PROD_ID", "PROD_NAME", "PROD_TYPE", "UNIT_PRICE", "QNTY", "PRICE"]
			.reduce((obj, col) => {obj[col] = null; return obj;}, {});
			
		line.ORD_ID = this.orders.getCurrent().ORD_ID;
		line.UNIT_PRICE = line.QNTY = line.PRICE = 0;
		
		this.getOrderLines().addData(line, {local: true});
	},
	
	getOrderLine(info) {
		return {
			orderID:info.ORD_ID,
			id:info.LINE_ID,
			prodID:info.PROD_ID,
			quantity:info.QNTY,
			price:info.PRICE
		};
	},
	
	setLineValue:function(index, property, value){
		orderManager.getOrderLines().modify(index, item => {
			item.setValue(property, value);
			orderManager.setPrice(item.data);
		});
	},
	
	setPrice:function(lineInfo){
		lineInfo.PRICE = lineInfo.UNIT_PRICE * (lineInfo.QNTY || 0);
		orderManager.setTotalAmount();
	},

	validate:function(){
		let order = this.orders.getCurrent();
		if (isEmpty(order.CUST_NAME))
			return new Validity().valueMissing("CUST_NAME");
		
		let lineList = this.getOrderLines().getData();
		for (let i = 0, length = lineList.length; i < length; ++i) {
			let line = lineList[i];
			if (isEmpty(line.PROD_ID))
				return new Validity().valueMissing("PROD_ID", i);
			if (isEmpty(line.QNTY) || line.QNTY < 1)
				return new Validity().rangeUnderflow("QNTY", 1, i);
		}

		return new Validity();
	},
	
	save:function(){
		let item = this.orders.getCurrent("item"),
			order = this.getOrder(item.data),
			lineList = this.getOrderLines(),
			dirtyLines = lineList.getData("dirty"),
			orderLines = Object.entries(dirtyLines)
				.reduce((obj, entry) => {
					let prop = entry[0],
						list = entry[1];
					if ("empty" != prop)
						obj[prop] = list.map(info => orderManager.getOrderLine(info));
					return obj;
				}, {});
		let create = item.isNew(),
			url = create ? "<c:url value='/order/create'/>" : "<c:url value='/order/update'/>",
			data = {};
		if (create) {
			order.lineItems = orderLines["added"];
			data = order;
		} else {
			data = {
				order:order,
				lines:orderLines
			};
		}

		return new Promise(function(resolve, reject){
			json({
				url:url,
				type:"POST",
				data:JSON.stringify(data),
				success:function(resp) {
					resolve(resp);
					if (resp.saved) {
						item.replace(resp.orderInfo);
						lineList.setData(resp.lineList);
					}
				}
			});
		});
	},
	
	remove:function(){
		var orderList = this.orders,
			selected = orderList.getItems("selected");
		if (selected.length < 1)
			throw "Select orders to remove.";
		
		<%--When all information on the current page is removed,
		information on the previous page is requested.--%>
		var prev = orderList.length == selected.length,
			removed = orderList.remove(selected.map(item => item.index))
				.getData("dirty").removed;

		if (removed.length < 1) <%-- If removed information are all "added" ones --%>
			return new Promise(function(resolve, reject){
				resolve({local: true});
			});
		
		return new Promise(function(resolve, reject){
			json({
				url:"<c:url value='/order/remove'/>",
				type:"POST",
				data:{orderIDs: removed.map(data => data.ORD_ID).join(",")},
				success:function(resp) {
					resolve(resp);
					if (resp.saved) {
						orderManager.reload(prev);
					}
				}
			});
		});
	}
};

${functions} <%--Placeholder for functions and variables from included JSPs--%>
$(function(){
	${onload} <%--Placeholder for codes from included JSPs to be executed when the page is ready--%>
});
//# sourceURL=order-main.jsp
</script>