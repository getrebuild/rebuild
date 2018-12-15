<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/charts.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/chart-design.css">
<title>图表设计器</title>
<style type="text/css">
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-offcanvas-menu">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="图表设计器" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeft.jsp">
		<jsp:param value="chart-design" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<aside class="data-aside">
			<div class="rb-scroller">
				<div class="data-info">
					<h5>数据来源</h5>
					<ul class="list-unstyled">
						<li><span><i class="zmdi zmdi-${entityIcon} icon"></i>${entityLabel}</span></li>
					</ul>
				</div>
				<div class="data-info">
					<h5>字段</h5>
					<ul class="list-unstyled fields">
						<c:forEach items="${fields}" var="e">
						<li class="${e[2]}"><a data-type="${e[2]}" data-field="${e[0]}">${e[1]}</a></li>
						</c:forEach>
					</ul>
				</div>
			</div>
		</aside>
		<aside class="config-aside">
			<div class="rb-scroller">
				<div class="data-info">
					<h5>图表标题</h5>
					<div class="input">
						<input class="form-control form-control-sm" placeholder="未命名图表" id="chart-title" value="${chartTitle}">
					</div>
				</div>
				<div class="data-info">
					<h5>图表类型</h5>
					<div class="chart-type">
						<a title="表格" data-type="TABLE" data-allow-dims="0|3" data-allow-nums="1|9"><i class="C200"></i></a>
						<a title="指标卡" data-type="INDEX" data-allow-dims="0|0" data-allow-nums="1|1"><i class="C310"></i></a>
						<a title="折线图" data-type="LINE" data-allow-dims="1|1" data-allow-nums="1|9"><i class="C220"></i></a>
						<a title="柱状图" data-type="BAR" data-allow-dims="1|1" data-allow-nums="1|9"><i class="C210"></i></a>
						<a title="饼图" data-type="PIE" data-allow-dims="1|1" data-allow-nums="1|1"><i class="C230"></i></a>
						<a title="漏斗图" data-toggle="tooltip" data-type="FUNNEL" data-allow-dims="1|1" data-allow-nums="1|1"><i class="C330"></i></a>
					</div>
				</div>
			</div>
		</aside>
		<div class="main-content container-fluid">
			<div class="axis-warp">
				<div class="axis">
					<div class="axis-head">
						<span>纬度</span>
						<a><i class="zmdi zmdi-edit"></i></a>
					</div>
					<div class="axis-target J_axis-dim"></div>
				</div>
				<div class="axis">
					<div class="axis-head">
						<span>数值</span>
						<a><i class="zmdi zmdi-edit"></i></a>
					</div>
					<div class="axis-target J_axis-num"></div>
				</div>
			</div>
			<div id="chart-preview">
			</div>
		</div>
	</div>
</div>
<script type="text/plain" id="axis-ietm">
<span>
<div class="item" data-toggle="dropdown">
	<a><i class="zmdi zmdi-chevron-down"></i></a>
	<span></span>
	<a class="del"><i class="zmdi zmdi-close-circle"></i></a>
</div>
<ul class="dropdown-menu">
	<li class="dropdown-item J_num" data-calc="SUM">求和</li>
	<li class="dropdown-item J_num" data-calc="AVG">平均值</li>
	<li class="dropdown-item J_num" data-calc="MAX">最大值</li>
	<li class="dropdown-item J_num" data-calc="MIN">最小值</li>
	<li class="dropdown-item J_text" data-calc="COUNT">计数</li>
	<li class="dropdown-item J_date" data-calc="Y">按年</li>
	<li class="dropdown-item J_date" data-calc="Q">按季</li>
	<li class="dropdown-item J_date" data-calc="M">按月</li>
	<li class="dropdown-item J_date" data-calc="D">按日</li>
	<li class="dropdown-item J_date" data-calc="H">按时</li>
	<li class="dropdown-divider"></li>
	<li class="dropdown-item" data-sort="ASC">排序-升序</li>
	<li class="dropdown-item" data-sort="DESC">排序-降序</li>
</ul>
</span>
</script>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/charts/charts.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/charts/chart-design.jsx" type="text/babel"></script>
<script>
window.__sourceEntity = '${entityName}'
window.__chartId = '${chartId}'
window.__chartConfig = ${chartConfig}
</script>
</body>
</html>
