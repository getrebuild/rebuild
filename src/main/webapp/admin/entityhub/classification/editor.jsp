<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/classification.css">
<title>分类数据编辑</title>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="分类数据编辑" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="classifications" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="page-head">
			<div class="float-left">
				<nav class="mt-1">
					<ol class="breadcrumb page-head-nav">
						<li class="breadcrumb-item"><a href="../classifications">分类数据</a></li>
						<li class="breadcrumb-item active">${name}</li>
					</ol>
				</nav>
			</div>
			<div class="float-right" style="margin-top:-3px">
				<button class="btn btn-secondary J_imports" type="button"><i class="zmdi zmdi-cloud-download"></i> 导入</button>
			</div>
			<div class="clearfix"></div>
		</div>
		<div class="main-content container-fluid pt-1">
			<div class="card">
				<div class="card-body rb-loading rb-loading-active" id="boxes">
					<%@ include file="/_include/spinner.jsp"%>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script>
var dataId = '${dataId}', name = '${name}', openLevel = ${openLevel}
</script>
<script src="${baseUrl}/assets/js/entity/classification-editor.jsx" type="text/babel"></script>
</body>
</html>
