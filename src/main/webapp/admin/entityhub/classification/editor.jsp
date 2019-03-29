<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<meta name="rb.classificationId" content="${dataId}">
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
			<div class="float-left"><div class="page-head-title">分类数据编辑</div></div>
			<div class="float-right pt-1">
				<button class="btn btn-secondary" type="button"><i class="zmdi zmdi-cloud"></i> 导入</button>
			</div>
			<div class="clearfix"></div>
		</div>
		<div class="main-content container-fluid pt-1">
			<div class="card">
				<div class="card-body" id="boxes">
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/sortable.js"></script>
<script src="${baseUrl}/assets/js/entity/classification-editor.jsx" type="text/babel"></script>
</body>
</html>
