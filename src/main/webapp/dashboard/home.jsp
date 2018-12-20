<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>首页</title>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/charts/jquery.gridster.min.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/charts.css">
<style type="text/css">
.tools-bar{height:44px;padding:0 25px;padding-top:7px}
.tools-bar h4{margin:10px 0}
.chart-grid{overflow:scroll;overflow-x:hidden;padding:15px;padding-top:0;padding-right:2px}
.gridster ul,.gridster ul>li{margin:0;padding:0}
.gridster ul>li{background-color:#fff;}
.gridster ul>li>div{height:100%}
.gridster ul>li:hover{box-shadow:0 2px 4px 0 rgba(0, 0, 0, .1), 0 16px 24px 0 rgba(81, 129, 228, .1)}
.gridster ul>li:hover .chart-oper{display:block;}
a.chart-add{display:block;text-align:center;height:100%;padding-top:50px;}
a.chart-add i.zmdi{font-size:71px;color:#ddd;font-weight:lighter;}
.dash-list .dash{display:inline-block;padding-right:71px;position:relative;}
.dash .dash-action{position:absolute;top:0;right:0;padding-top:9px;padding-left:6px;display:none;text-align:left;width:70px;}
.dash:hover .dash-action{display:block;}
.dash .dash-action a{padding:3px;margin-left:3px}
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
		<div class="rb-spinner">
	        <svg width="40px" height="40px" viewBox="0 0 66 66" xmlns="http://-www.w3.org/2000/svg">
	            <circle fill="none" stroke-width="4" stroke-linecap="round" cx="33" cy="33" r="30" class="circle"></circle>
	        </svg>
	    </div>
	</div>
	<div class="rb-content">
		<div class="main-content container-fluid p-0">
			<div class="tools-bar">
				<div class="row">
					<div class="col-sm-6 dash-list">
						<div class="dash">
							<h4>仪表盘</h4>
							<div class="dash-action">
								<a class="zicon J_dash-edit"><i class="zmdi zmdi-settings"></i></a>
								<a class="zicon J_dash-add" title="添加仪表盘"><i class="zmdi zmdi-plus-circle-o"></i></a>
							</div>
						</div>
					</div>
					<div class="col-sm-6 text-right">
						<button type="button" class="btn btn-link J_add-chart pr-0"><i class="zmdi zmdi-plus icon"></i> 添加图表</button>
					</div>
				</div>
			</div>
			<div class="chart-grid invisible">
				<div class="gridster">
					<ul class="list-unstyled"></ul>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/lib/charts/jquery.gridster.min.js"></script>
<script src="${baseUrl}/assets/lib/charts/echarts.min.js"></script>
<script src="${baseUrl}/assets/js/charts/charts.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/charts/dashboard.jsx" type="text/babel"></script>
</body>
</html>
