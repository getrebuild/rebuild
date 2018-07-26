<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>通用配置</title>
<style type="text/css">
</style>
</head>
<body>
<div class="rb-wrapper rb-collapsible-sidebar">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="通用设置" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="general" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container-fluid">
			首页可配置仪表盘
		</div>
	</div>
</div>
<script type="text/javascript">
</script>
</body>
</html>
