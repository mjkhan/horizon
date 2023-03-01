<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
		<jsp:include page="cust-list.jsp"/>
		<div class="main-right flex-grow-1">
			<ul class="nav nav-pills mb-3">
				<li class="nav-item"><a href="#tab0" class="nav-link active" data-toggle="pill">Properties</a></li>
			</ul>
			<div class="tab-content" style="width:90%;">
				<jsp:include page="cust-info.jsp"/>
			</div>
		</div>
<script type="text/javascript">
var custList = new Dataset({ <%-- Customer dataset from querying customers --%>
	keymapper:function(info) {return info ? info.id : "";}, <%-- Key to a customer data --%>
	dataGetter:function(obj) {return obj.custList;}, <%-- Extracts customer dataset named 'custList' from the server response --%>
	formats:{ <%-- value formats for a customer's properties of credit, createdAt, lastModified. See horizon.js --%>
		credit:numberFormat,
		createdAt:datetimeFormat,
		lastModified:datetimeFormat
	},
	trace:true
});

var customerManager = { <%-- Controlls request and response to and from the CustomerController. --%>
	<%--Searches customer information with the given terms--%>
	search:function(by, terms, start) {
		var _search = function(state, prev) {
			if (prev) {
				start = (start || 0) - ${fetch};
			}
			json({
				url:"<c:url value='/customer'/>",
				data:{
					by:by,
					terms:terms,
					start:Math.max(start || 0, 0)
				},
				success:function(resp) {
					resp.state = state;
					custList.setData(resp);
				}
			});
		};
		<%-- Sets the current search to the reload method --%>
		customerManager.reload = function(prev) {
			_search(custList.state, prev);
		};
		
		_search();
	},
	
	<%--Reloads customer information after create, update, or remove requests with the last query terms.--%>
	reload:function(){
		customerManager.search();
	},
	
	save:function(customer){
		if (customer.id.startsWith("new customer"))
			return this.create(customer);
		else
			return this.update(customer);
	},
	
	create:function(customer) {
		return new Promise(function(resolve, reject) {
			json({
				url:"<c:url value='/customer/create'/>",
				type:"POST",
				data:customer,
				success:resp => {
					resolve(resp);
					if (resp.saved)
						custList.replace({key:customer.id, data:resp.cust});
				}
			});
		});
	},
	
	update:function(customer) {
		return new Promise(function(resolve, reject) {
			json({
				url:"<c:url value='/customer/update'/>",
				type:"POST",
				data:customer,
				success:resp => {
					resolve(resp);
					if (resp.saved)
						custList.replace({data:resp.cust});
				}
			});
		});
	},
	
	remove:function() {
		var selectedIDs = custList.getKeys("selected");
		if (selectedIDs.length < 1)
			throw "Select customers to remove.";
		
			<%--When all information on the current page is removed
			information on the previous page is requested. 
			--%>
		var prev = custList.length == selectedIDs.length,
			added = selectedIDs.filter(custID => custID.startsWith("new customer")),
			custIDs = selectedIDs.filter(custID => !custID.startsWith("new customer"));
		
		return new Promise(function(resolve, reject){
			if (custIDs.length > 0)
				json({
					url:"<c:url value='/customer/remove'/>",
					type:"POST",
					data:{custIDs:custIDs.join(",")},
					success:resp => {
						resolve(resp);
						if (resp.saved) {
							if (prev)
								customerManager.reload(prev);
							else
								custList.erase(selectedIDs);
						}
					}
				});
			else {
				resolve({saved:true});
				if (prev)
					customerManager.reload(prev);
				else
					custList.erase(selectedIDs);
			}
		});
	}
};

${functions} <%--Placeholder for functions and variables from included JSPs--%>
$(function(){
	${onload} <%--Placeholder for codes from included JSPs to be executed when the page is ready--%>
});
//# sourceURL=cust-main.js
</script>