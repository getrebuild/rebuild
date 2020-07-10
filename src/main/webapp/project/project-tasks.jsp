<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/project-tasks.css">
<title>${projectName} · 项目</title>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="${projectName}" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeft.jsp">
		<jsp:param value="nav_project-${projectId}" name="activeNav"/>
	</jsp:include>
	<div class="rb-loading rb-loading-active must-center J_project-load">
		<%@ include file="/_include/Spinner.jsp"%>
	</div>
	<div class="rb-content">
		<div class="page-head page-head-sm">
			<div class="page-head-title">${projectName}</div>
		</div>
		<div class="main-content container-fluid p-0">
			<div id="plan-boxes"></div>
		</div>
	</div>
</div>
<script src="${baseUrl}/assets/lib/moment-with-locales.min.js?v=2.27.0"></script>
<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = {
	id: '${projectId}',
	icon: '${iconName}',
	projectCode: '${projectCode}',
	projectName: '${projectName}',
	projectPlans: ${projectPlans},
}
</script>
<script src="${baseUrl}/assets/js/project/project-tasks.jsx" type="text/babel"></script>
</body>
</html>
