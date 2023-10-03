<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
		<jsp:include page="prod-list.jsp"/>
		<div class="main-right flex-grow-1">
			<ul class="nav nav-pills mb-3">
				<li class="nav-item"><a href="#tab0" class="nav-link active" data-toggle="pill">Properties</a></li>
			</ul>
			<div class="tab-content" style="width:90%;">
				<jsp:include page="prod-info.jsp"/>
			</div>
		</div>
<script type="text/javascript">
var prodList = new Dataset({ <%-- dataset from querying products --%>
		keys:["PROD_ID"] <%-- Used to identify current and/or selected DataItems by the key in prodList.setState() to refresh the current state --%>
	}),

	productManager = { <%-- Controls request and response to and from the ProductController. --%>
	_params: {
		by: "",
		terms: "",
		start: 0
	},
	
	toProduct: (item) => { <%-- Converts a DataItem to a product --%>
		let info = item && !item.unreachable ? item.data : null;

		return info ? Object.entries({"id":"PROD_ID", "type":"PROD_TYPE", "name":"PROD_NAME", "vendor":"VENDOR", "unitPrice":"UNIT_PRICE"})
			.reduce(
				(obj, entry) => {
					obj[entry[0]] = info[entry[1]];
					return obj;
				},
				{}
			) : null;
	},

	params: (obj) => { <%-- Sets parameters to search information --%>
		productManager._params = obj;
		return productManager;
	},
	search:(option) => { <%-- Sends request to search information and receives the response --%>
		json({
			url:"<c:url value='/product'/>",
			data:productManager._params,
			success:resp => productManager.setData(resp, option)
		});
	},

	setData: (obj, option) => { <%-- Sets the query result to prodList via productManager --%>
		prodList.pagination = obj.pagination;
		prodList.setData(obj.prodList, option);
	},

	reload:function(prev){ <%--Reloads information after create, update, or remove requests with the last query terms.--%>
		if (prev) {
			let start = (productManager._params.start || 0) - prodList.pagination.fetchSize;
			productManager._params.start = Math.max(start, 0);
		}
		productManager.search({stateful: true}); <%-- To preserve the current state --%>
	},
	
	save:function() {
		let current = prodList.getCurrent("item");
		if (!current)
			return console.log("WARNING", "current item not found");
		
		let product = productManager.toProduct(current);
		
		return current.isNew() ?
			productManager.create(product) :
			productManager.update(product);
	},	
	create:function(product) {
		return new Promise(function(resolve, reject) {
			json({
				url:"<c:url value='/product/create'/>",
				type:"POST",
				data:product,
				success:resp => {
					if (resp.saved)
						productManager.reload();
					resolve(resp);
				}
			});
		});
	},	
	update:function(product) {
		return new Promise(function(resolve, reject) {
			json({
				url:"<c:url value='/product/update'/>",
				type:"POST",
				data:product,
				success:resp => {
					if (resp.saved)
						productManager.reload();
					resolve(resp);
				}
			});
		});
	},	
	remove:function() {
		let selected = prodList.getItems("selected");
		if (selected.length < 1)
			throw "Select products to remove.";

		<%--When all information on the current page is removed
		information on the previous page is requested. 
		--%>
		let prev = prodList.length == selected.length,
			removed = prodList.remove(selected.map(item => item.index))
				.getData("dirty").removed;

		if (removed.length < 1) <%-- If removed information are all "added" ones --%>
			return new Promise(function(resolve, reject){
				resolve({local: true});
			});

		return new Promise(function(resolve, reject) {
			json({
				url:"<c:url value='/product/remove'/>",
				type:"POST",
				data:{prodIDs: removed.map(data => data.PROD_ID).join(",")},
				success:resp => {
					if (resp.saved)
						productManager.reload(prev);
					resolve(resp);
				}
			});
		});
	}
};

${functions} <%--Placeholder for functions and variables from included JSPs--%>
$(function(){
	${onload} <%--Placeholder for codes from included JSPs to be executed when the page is ready--%>
});
//# sourceURL=prod-main.jsp
</script>