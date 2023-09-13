<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="initial-scale=1.0, width=device-width">
<link href="<c:url value='/asset/image/favicon.ico'/>" rel="shortcut icon" type="image/x-icon" />
<link href="<c:url value='/asset/image/favicon.ico'/>" rel="icon" type="image/x-icon" />

<link href="<c:url value='/asset/css/bootstrap.min.css'/>" rel="stylesheet" type="text/css" />
<link href="<c:url value='/asset/css/bootstrap-grid.min.css'/>" rel="stylesheet" type="text/css" />
<link href="<c:url value='/asset/css/bootstrap-reboot.min.css'/>" rel="stylesheet" type="text/css" />

<link href="<c:url value='/asset/css/fontawesome.min.css'/>" rel="stylesheet" type="text/css" />

<link href="<c:url value='/asset/css/example.css'/>" rel="stylesheet" type="text/css" />
<title>Horizon Example</title>
</head>
<body class="d-flex flex-column">
<img class="wait" src="<c:url value='/asset/image/spinner.gif'/>"/>
<div class="d-flex flex-column flex-grow-1" style="padding:0 1em;">
	<jsp:include page="header.jsp"/>
	<div id="main">
	</div>
</div>
<footer class="d-flex justify-content-between">
	<span>Horizon Example</span>
	<span>Copyright &copy; 2021 mjkhan, All rights reserved</span>
</footer>

<!--[if lt IE 9]><script src="<c:url value='/asset/js/html5shiv.js'/>"></script><![endif]-->
<script src="<c:url value='/asset/js/bluebird.min.js'/>"></script>
<script src="<c:url value='/asset/js/jquery-3.3.1.min.js'/>" type="text/javascript"></script>
<script src="<c:url value='/asset/js/bootstrap/bootstrap.min.js'/>" type="text/javascript"></script>
<script src="<c:url value='/asset/js/bootstrap/bootstrap.bundle.min.js'/>" type="text/javascript"></script>
<script src="<c:url value='/asset/js/example/horizon.js'/>" type="text/javascript"></script>
<script src="<c:url value='/asset/js/example/dataset.js'/>" type="text/javascript"></script>
<script src="<c:url value='/asset/js/example/example.js'/>" type="text/javascript"></script>
<script type="text/javascript">
${functions}<%--Placeholder for functions and variables. Intended to place Javascript codes at the bottom of the body element--%>

$(function(){
${onload}<%--Placeholder for codes to execute when the document is loaded. Intended to place Javascript codes at the bottom of the body element--%>
});
//# sourceURL=index.jsp
</script>
</body>
</html>