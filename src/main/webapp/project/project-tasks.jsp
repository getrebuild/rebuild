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
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-aside">
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
		<aside class="page-aside widgets">
			<a class="side-toggle" title="展开/收缩面板"><i class="zmdi zmdi-arrow-left"></i></a>
			<div class="tab-container">
				<ul class="nav nav-tabs">
					<li class="nav-item"><a class="nav-link active" href="#asideFilters" data-toggle="tab">常用查询</a></li>
				</ul>
				<div class="tab-content rb-scroller">
					<div class="tab-pane active" id="asideFilters">
						<div class="ph-item rb">
							<div class="ph-col-12 p-0">
								<div class="ph-row">
									<div class="ph-col-12 big"></div>
									<div class="ph-col-12 big"></div>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</aside>
		<div class="page-head">
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
	projectCode: '${projectCode}',
	projectName: '${projectName}',
	projectPlans: ${projectPlans},
}
</script>
<script src="${baseUrl}/assets/js/project/project-tasks.jsx" type="text/babel"></script>
</body>
</html>
