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
		<div class="main-content container-fluid p-0">
			<div class="tools-bar">
				<div class="row">
					<div class="col-sm-6">
						<div class="project-head">
							<h4>${projectName}</h4>
							<div class="project-action">
								<span class="admin-show"><a href="${baseUrl}/admin/project/${projectId}" title="项目设置" class="zicon"><i class="zmdi zmdi-settings"></i></a></span>
							</div>
						</div>
					</div>
					<div class="col-sm-6 text-right d-none d-md-block project-right">
						<div class="btn-group J_sorts">
							<button type="button" class="btn btn-link pr-0 text-right" data-toggle="dropdown"><i class="zmdi zmdi-sort-asc icon"></i> <span>排序</span></button>
							<div class="dropdown-menu dropdown-menu-right">
								<a class="dropdown-item" data-sort="seq">手动拖动</a>
								<a class="dropdown-item" data-sort="deadline">最近截至</a>
								<a class="dropdown-item" data-sort="modifiedOn">最近更新</a>
							</div>
						</div>
						<div class="btn-group J_search">
							<button type="button" class="btn btn-link pr-0 text-right" data-toggle="dropdown">
								<i class="zmdi zmdi-search icon"></i>
								搜索
								<i class="indicator-primary bg-warning hide" style="top:1px;right:-7px"></i>
							</button>
							<div class="dropdown-menu dropdown-menu-right">
								<div class="px-4 py-2">
									<div class="input-group input-search m-0">
										<input class="form-control" type="text" placeholder="输入关键词搜索" maxlength="40">
										<span class="input-group-btn">
											<button class="btn btn-secondary" type="button"><i class="icon zmdi zmdi-search"></i></button>
										</span>
									</div>
								</div>
								<div class="dropdown-divider"></div>
								<a class="dropdown-item J_filter"><i class="icon zmdi zmdi-filter-list"></i> 高级查询</a>
							</div>
						</div>
					</div>
				</div>
			</div>
			<div id="plan-boxes"></div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = {
	id: '${projectId}',
	icon: '${iconName}',
	projectCode: '${projectCode}',
	projectName: '${projectName}',
	projectPlans: ${projectPlans},
    isMember: ${isMember},
}
</script>
<script src="${baseUrl}/assets/js/rb-advfilter.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/project/project-tasks.jsx" type="text/babel"></script>
</body>
</html>
