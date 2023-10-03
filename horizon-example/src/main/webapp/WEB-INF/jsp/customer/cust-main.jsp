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
var custList = new Dataset({ <%-- dataset from querying customers --%>
		keys:["id"], <%-- Used to identify current and/or selected DataItems by the key in custList.setState() to refresh the current state --%>
		formats:{ <%-- value formats for information properties of credit, createdAt, lastModified. See horizon.js --%>
			credit: numberFormat,
			createdAt: datetimeFormat,
			lastModified: datetimeFormat
		},
		trace:true
	});

var	customerManager = { <%-- Controls request and response to and from the CustomerController. --%>
	_params: {
		by: "",
		terms: "",
		start: 0
	},

	params: (obj) => { <%-- Sets parameters to search information --%>
		customerManager._params = obj;
		return customerManager;
	},
	
	search:function(option) { <%-- Sends request to search information and receives the response --%>
		json({
			url:"<c:url value='/customer'/>",
			data:customerManager._params,
			success:resp => customerManager.setData(resp, option)
		});
	},

	setData: (obj, option) => { <%-- Sets the query result to custList via customerManager --%>
		custList.pagination = obj.pagination;
		custList.setData(obj.custList, option);
	},
	
	reload:function(prev){ <%--Reloads information after create, update, or remove requests with the last query terms.--%>
		if (prev) {
			let start = (customerManager._params.start || 0) - custList.pagination.fetchSize;
			customerManager._params.start = Math.max(start, 0);
		}
		customerManager.search({stateful: true});
	},
	
	save:function(){
		let current = custList.getCurrent("item");
		if (!current)
			return console.log("WARNING", "current item not found");
		
		let customer = current.data;
		
		return current.isNew() ?
			customerManager.create(customer) :
			customerManager.update(customer);
	},

	create:function(customer) {
		return new Promise(function(resolve, reject) {
			json({
				url:"<c:url value='/customer/create'/>",
				type:"POST",
				data:customer,
				success:resp => {
					if (resp.saved)
						customerManager.reload();
					resolve(resp);
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
					if (resp.saved)
						customerManager.reload();
					resolve(resp);
				}
			});
		});
	},
	
	remove:function() {
		let selected = custList.getItems("selected");
		if (selected.length < 1)
			throw "Select customers to remove.";

		<%--When all information on the current page is removed
		information on the previous page is requested. 
		--%>
		let prev = custList.length == selected.length,
			removed = custList.remove(selected.map(item => item.index))
				.getData("dirty").removed;

		if (removed.length < 1) <%-- If removed information are all "added" ones --%>
			return new Promise(function(resolve, reject){
				resolve({local: true});
			});

		return new Promise(function(resolve, reject) {
			json({
				url:"<c:url value='/customer/remove'/>",
				type:"POST",
				data:{custIDs: removed.map(data => data.id).join(",")},
				success:resp => {
					if (resp.saved)
						customerManager.reload(prev);
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
//# sourceURL=cust-main.jsp
</script>