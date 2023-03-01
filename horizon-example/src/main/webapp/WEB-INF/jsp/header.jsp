<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
	<header>
		<div id="header" class="d-flex">
			<div class="logo">Horizon Example</div>
			<div class="flex-grow-1">
				<div class="d-flex justify-content-end" style="font-size:14px; padding:.5em; border-bottom:1px solid;">
					<span>Thanks for your interest in Horizon</span>
				</div>
				<div id="menus" class="menu">
					<template id="menuRow"><a onclick="openPage({index:{index}, url:'{actionConfig}', name:'{name}'});">{name}</a></template>
				</div>
			</div>
		</div>
		<div class="subtitle">
			<div style="display:inline-block; width:1.2em; height:1.2em; border:5px solid deeppink;"></div>
			<span id="subtitle"></span>
		</div>
 	</header>
<c:set var="functions" scope="request">${functions} <%-- To be written at ${functions} of index.jsp --%>
function setMenu(index) {
	$("#menus a").each(function(ndx, item) {
		if (index == ndx)
			$(item).addClass("current");
		else
			$(item).removeClass("current");
	});
}

function setMenus(menus, current) {
	var menuRow = document.getElementById("menuRow").innerHTML,
		tags = menus.map((menu, index) => menuRow
			.replace(/{index}/g, index)
			.replace(/{actionConfig}/g, menu.actionConfig)
			.replace(/{name}/g, menu.name)
		);
	$("#menus").html(tags.join(""));
	if (!isEmpty(current))
		setMenu(current);
}

function openPage(menu) {
	if (menu.url)
		ajax({
			url:menu.url,
			success:function(resp) {
				$("#main").hide().html(resp).fadeIn();
				$("#subtitle").html(menu.name);
				setMenu(menu.index);
			}
		});
	else 
		setMenu(menu.index);
}

$.fn.setPaging = function(config) {
	config.links = 5;
	config.first = function(index, label) {return "<li onclick='{func};'><a>|◀</a></li>".replace(/{func}/, config.func.replace(/{index}/, 0));};
	config.previous = function(index, label) {return "<li onclick='{func};'><a>◀</a></li>".replace(/{func}/, config.func.replace(/{index}/, index));};
	config.link = function(index, label) {return "<li onclick='{func};'><a>{label}</a></li>".replace(/{func}/, config.func.replace(/{index}/, index)).replace(/{label}/, label);};
	config.current = function(index, label) {return "<li class='current'><span>{label}</span></li>".replace(/{label}/, label);};
	config.next = function(index, label) {return "<li onclick='{func};'><a>▶</a></li>".replace(/{func}/, config.func.replace(/{index}/, index));};
	config.last = function(index, label) {return "<li onclick='{func};'><a>▶|</a></li>".replace(/{func}/, config.func.replace(/{index}/, index));};

	return this.each(function(){
		var tag = paginate(config), <%-- See horizon.js --%>
			container = $(this);
		if (tag)
			container.html(tag.replace(/{func}/g, config.func)).show();
		else {
			if (config.hideIfEmpty != false)
				container.hide();
		}
	});
}

$.fn.setCurrentRow = function(val) {
	if (!val) return;
	
	return this.each(function() {
		var e = $(this);
		e.find("tr").each(function(){
			var tr = $(this),
				current = val == tr.attr("data-key");
			if (current)
				tr.addClass("current");
			else
				tr.removeClass("current");		
		});
	});
}</c:set>
<c:set var="onload" scope="request">${onload} <%-- To be written at ${onload} of index.jsp --%>
setMenus([
	{actionConfig:"<c:url value='/product'/>", name:"Product"},
	{actionConfig:"<c:url value='/customer'/>", name:"Customer"},
	{actionConfig:"<c:url value='/order'/>", name:"Sales Order"},
	{actionConfig:"<c:url value='/organization'/>", name:"Organization"}
]);
$("#menus").children(":first").click();</c:set>