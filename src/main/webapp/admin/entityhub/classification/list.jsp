<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>分类数据</title>
<style type="text/css">
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="审计日志" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="classifications" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="page-head">
			<div class="float-left"><div class="page-head-title">分类数据</div></div>
			<div class="float-right pt-1">
				<button class="btn btn-light J_add" type="button">新建</button>
			</div>
			<div class="clearfix"></div>
		</div>
		<div class="main-content container-fluid">
			<button class="J_add">添加</button>
			<div>
			<c:forEach items="${classifications}" var="item">
				<a href="classification/${item[0]}"><strong>${item[1]}</strong><p>${item[2]}</p></a>
			</c:forEach>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/entity/classification.jsx" type="text/babel"></script>
</body>
</html>
