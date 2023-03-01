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
var prodList = new Dataset({ <%-- Product dataset from querying products --%>
	keymapper:function(info) {return info ? info.PROD_ID : "";}, <%-- Key to a product data --%>
	dataGetter:function(obj) {return obj.prodList;} <%-- Extracts product dataset named 'prodList' from the server response --%>
});

var productManager = { <%-- Controlls request and response to and from the ProductController. --%>
	search:function(by, terms, start) {
		var _search = function(state, prev) {
			if (prev) {
				start = (start || 0) - ${fetch};
			}
			json({
				url:"<c:url value='/product'/>",
				data:{
					by:by,
					terms:terms,
					start:Math.max(start || 0, 0)
				},
				success:resp => {
					resp.state = state;
					prodList.setData(resp);
				}
			});
		};
		<%-- Sets the current search to the reload method --%>
		productManager.reload = function(prev) {
			_search(prodList.state, prev);
		};
		
		_search();
	},
	<%--Reloads product information after create, update, or remove requests with the last query terms.--%>
	reload:function(prev){
		productManager.search();
	},
	
	save:ignore,
	
	create:function(product) {
		return new Promise(function(resolve, reject) {
			json({
				url:"<c:url value='/product/create'/>",
				type:"POST",
				data:product,
				success:resp => {
					resolve(resp);
					if (resp.saved)
						productManager.reload();
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
					resolve(resp);
					if (resp.saved)
						productManager.reload();
				}
			});
		});
	},
	
	remove:function() {
		var prodIDs = prodList.getKeys("selected");
		if (prodIDs.length < 1)
			throw "Select products to remove.";
		<%--When all information on the current page is removed
		information on the previous page is requested. 
		--%>
		var prev = prodList.length == prodIDs.length;

		return new Promise(function(resolve, reject) {
			json({
				url:"<c:url value='/product/remove'/>",
				type:"POST",
				data:{prodIDs:prodIDs.join(",")},
				success:resp => {
					resolve(resp);
					if (resp.saved)
						productManager.reload(prev);
				}
			});
		});
	}
};

${functions} <%--Placeholder for functions and variables from included JSPs--%>
$(function(){
	${onload} <%--Placeholder for codes from included JSPs to be executed when the page is ready--%>
});
//# sourceURL=prod-main.js
</script>