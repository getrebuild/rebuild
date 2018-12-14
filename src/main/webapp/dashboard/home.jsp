<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>首页</title>
<style type="text/css">
.tools-bar{border-bottom:1px solid rgba(0, 0, 0, .1);}
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
	<div class="rb-content">
		<div class="main-content container-fluid">
			<div class="tools-bar">
				<div class="row">
					<div class="col-sm-6">
					</div>
					<div class="col-sm-6 text-right">
						<button type="button" class="btn btn-link J_add-chart"><i class="zmdi zmdi-plus icon"></i> 添加图表</button>
					</div>
				</div>
			</div>
			<div class="chart-grid">
				<div class="gridster">
					<ul>
						<li data-sizey="2" data-sizex="2" data-col="4" data-row="1"><div class="gridster-box"><div class="chart">1</div><div class="handle-resize"></div></div></li>
						<li data-sizey="2" data-sizex="2" data-col="4" data-row="1"><div class="gridster-box"><div class="chart">2</div><div class="handle-resize"></div></div></li>
						<li data-sizey="2" data-sizex="2" data-col="4" data-row="1"><div class="gridster-box"><div class="chart">3</div><div class="handle-resize"></div></div></li>
					</ul>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/gridster/jquery.gridster.min.css">
<script src="${baseUrl}/assets/lib/gridster/jquery.gridster.js"></script>
<script src="${baseUrl}/assets/js/charts/charts.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/charts/dashboard.jsx" type="text/babel"></script>
</body>
</html>
