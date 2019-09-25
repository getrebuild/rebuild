<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/classification.css">
<title>分类数据</title>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="分类数据" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="classifications" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="page-head">
			<div class="float-left">
				<div class="page-head-title">分类数据<span class="sub-title">${name}</span></div>
			</div>
			<div class="float-right pt-1">
				<button class="btn btn-secondary J_imports" type="button"><i class="zmdi zmdi-cloud-download"></i> 导入公共数据</button>
			</div>
			<div class="clearfix"></div>
		</div>
		<div class="main-content container-fluid" style="padding-top:3px">
			<div class="card mb-0">
				<div class="card-body rb-loading rb-loading-active" id="boxes">
					<%@ include file="/_include/spinner.jsp"%>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = {
	id: '${dataId}',
	openLevel: ${openLevel}
}
</script>
<script src="${baseUrl}/assets/js/entityhub/classification-editor.jsx" type="text/babel"></script>
</body>
</html>
