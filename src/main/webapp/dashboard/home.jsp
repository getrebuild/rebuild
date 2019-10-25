<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>首页</title>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/charts/gridstack.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/charts.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/dashboard.css">
<style type="text/css">
.shareTo--wrap .custom-control {
    margin-top: 0;
}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="首页" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeft.jsp">
		<jsp:param value="dashboard-home" name="activeNav"/>
	</jsp:include>
	<div class="rb-loading rb-loading-active must-center J_dash-load">
		<%@ include file="/_include/spinner.jsp"%>
	</div>
	<div class="rb-content">
		<div class="main-content container-fluid p-0">
			<div class="tools-bar">
				<div class="row">
					<div class="col-sm-6 dash-list">
						<div class="dash-head">
							<h4 class="J_dash-select">仪表盘</h4>
							<div class="dash-action">
								<a class="zicon J_dash-edit"><i class="zmdi zmdi-settings"></i></a>
								<a class="zicon J_dash-new" title="添加仪表盘"><i class="zmdi zmdi-plus-circle-o"></i></a>
							</div>
						</div>
					</div>
					<div class="col-sm-6 text-right">
						<div class="btn-group" style="margin-top:7px;margin-right:3px">
							<button type="button" class="btn btn-link pr-0 text-right J_chart-adds" data-toggle="dropdown" style="min-height:24px;line-height:24px;"><i class="zmdi zmdi-plus icon"></i> 添加图表</button>
							<div class="dropdown-menu dropdown-menu-right">
								<a class="dropdown-item J_chart-new">添加新图表</a>
								<a class="dropdown-item J_chart-select">从已有图表中添加</a>
							</div>
						</div>
					</div>
				</div>
			</div>
			<div class="chart-grid invisible">
				<div class="grid-stack">
					<!-- grid-stack-item in here -->
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/lib/charts/lodash.min.js"></script>
<script src="${baseUrl}/assets/lib/charts/gridstack.all.js"></script>
<script src="${baseUrl}/assets/lib/charts/echarts.min.js"></script>
<script src="${baseUrl}/assets/js/charts/charts.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-approval.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/charts/dashboard.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/settings-share2.jsx" type="text/babel"></script>
</body>
</html>
