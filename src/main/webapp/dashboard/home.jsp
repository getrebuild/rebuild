<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>首页</title>
<style type="text/css">
</style>
</head>
<body>
<div class="rb-wrapper rb-collapsible-sidebar">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="首页" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeft.jsp">
		<jsp:param value="dashboard-home" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container-fluid">
			<h3 class="text-center">首页可配置仪表盘</h3>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
</script>
</body>
</html>
