<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="false" session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
				<link href="<c:url value='/asset/js/jstree/themes/proton/style.min.css'/>" rel="stylesheet" type="text/css" />
					<div class="d-flex flex-column flex-grow-1">
						<div class="d-flex justify-content-start" style="padding-top:.5em; padding-bottom:.5em; border-top:1px solid #dfdfdf; border-bottom:1px solid #dfdfdf;">
							<button id="treeToggler" onclick="toggleTree();" class="btn btn-primary"></button>
						</div>
						<div id="tree" style="padding-top:1em; height:37em; overflow:auto;">
						</div>
					</div>
<script src="<c:url value='/asset/js/jstree/jstree.js'/>" type="text/javascript"></script>
<script src="<c:url value='/asset/js/jstree/jstree-support.js'/>" type="text/javascript"></script>
<script type="text/javascript">
var support = treeSupport({
		selector:"#tree",
		trace:true,
		plugins:["checkbox", "dnd"],
		core:{check_callback:true,
		      multiple:false,
		      themes:{name:"proton"},
		      data:treeHtml(${orgs})<%-- Organization information --%>
			 },
		checkbox:{
			whole_node:false,
			tie_selection:false
		}
	});

function toggleTree() {
	$("#treeToggler").text(support.toggleFolding() == "collapsed" ? "+ Expand" : "- Collapse");
}

toggleTree();
support.selectNode("00000");
</script>