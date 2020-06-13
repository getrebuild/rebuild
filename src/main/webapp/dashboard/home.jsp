<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>首页</title>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/charts/gridstack.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/charts.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/dashboard.css">
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
		<%@ include file="/_include/Spinner.jsp"%>
	</div>
	<div class="rb-content">
		<div class="announcement-wrapper"></div>
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
					<div class="col-sm-6 text-right d-none d-md-block dash-right">
                        <div class="btn-group">
                            <button type="button" class="btn btn-link pr-0 text-right J_dash-fullscreen" title="全屏模式"><i class="zmdi zmdi-fullscreen icon up-1" style="font-size:1.45rem"></i></button>
                        </div>
                        <div class="btn-group J_dash-refresh">
                            <button type="button" class="btn btn-link pr-0 text-right" data-toggle="dropdown"><i class="zmdi zmdi-time-countdown icon"></i> <span>自动刷新</span></button>
                            <div class="dropdown-menu dropdown-menu-right" style="min-width:140px">
                                <a class="dropdown-item" data-time="30">30秒</a>
                                <a class="dropdown-item" data-time="60">1分钟</a>
                                <a class="dropdown-item" data-time="300">5分钟</a>
                                <a class="dropdown-item" data-time="600">10分钟</a>
                                <a class="dropdown-item" data-time="1800">30分钟</a>
                            </div>
                        </div>
						<div class="btn-group">
							<button type="button" class="btn btn-link pr-0 text-right J_chart-adds" data-toggle="dropdown"><i class="zmdi zmdi-plus icon"></i> 添加图表</button>
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
<script src="${baseUrl}/assets/js/rb-forms.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-approval.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/charts/dashboard.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/settings-share2.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/feeds/announcement.jsx" type="text/babel"></script>
</body>
</html>
