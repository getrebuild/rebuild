<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>首页</title>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/gridster/jquery.gridster.min.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/charts.css">
<style type="text/css">
.tools-bar{height:50px;padding:0 25px;padding-top:7px}
.tools-bar h4{margin:10px 0}
.chart-grid{overflow:scroll;overflow-x:hidden;padding:0 20px;padding-bottom:20px;padding-right:14px}
.gridster ul,.gridster ul>li{margin:0;padding:0}
.gridster ul>li{background-color:#fff;}
.gridster ul>li>div{height:100%}
.gridster ul>li:hover{box-shadow:0 2px 4px 0 rgba(0, 0, 0, .1), 0 16px 24px 0 rgba(81, 129, 228, .1)}
.gridster ul>li:hover .chart-oper{display:block;}
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
		<div class="main-content container-fluid p-0">
			<div class="tools-bar">
				<div class="row">
					<div class="col-sm-6">
						<h4>默认仪表盘</h4>
					</div>
					<div class="col-sm-6 text-right">
						<button type="button" class="btn btn-link J_add-chart pr-0"><i class="zmdi zmdi-plus icon"></i> 添加图表</button>
					</div>
				</div>
			</div>
			<div class="chart-grid">
				<div class="gridster">
					<ul class="list-unstyled"></ul>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/lib/gridster/jquery.gridster.js"></script>
<script src="${baseUrl}/assets/lib/chart/echarts.js"></script>
<script src="${baseUrl}/assets/js/charts/charts.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/charts/dashboard.jsx" type="text/babel"></script>
</body>
</html>
