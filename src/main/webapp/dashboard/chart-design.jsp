<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/charts.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/chart-design.css">
<title>图表设计器</title>
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
					<ul class="list-unstyled esource">
						<li><a><i class="zmdi zmdi-${entityIcon} icon"></i>${entityLabel}<span class="J_filter" title="设置过滤条件"><i class="zmdi zmdi-settings"></i></span></a></li>
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
						<a title="表格" data-type="TABLE" data-allow-dims="0|3" data-allow-nums="0|9"><i class="C200"></i></a>
						<a title="指标卡" data-type="INDEX" data-allow-dims="0|0" data-allow-nums="1|1"><i class="C310"></i></a>
						<a title="折线图" data-type="LINE" data-allow-dims="1|1" data-allow-nums="1|9"><i class="C220"></i></a>
						<a title="柱状图" data-type="BAR" data-allow-dims="1|1" data-allow-nums="1|9"><i class="C210"></i></a>
						<a title="饼图" data-type="PIE" data-allow-dims="1|1" data-allow-nums="1|1"><i class="C230"></i></a>
						<a title="漏斗图" data-type="FUNNEL" data-allow-dims="0|1" data-allow-nums="1|9"><i class="C330"></i></a>
						<a title="树图" data-type="TREEMAP" data-allow-dims="1|3" data-allow-nums="1|1"><i class="C370"></i></a>
					</div>
				</div>
				<div class="data-info mt-3">
					<h5>图表选项</h5>
					<div class="pl-1 mt-3 chart-option">
						<div class="J_opt-UNDEF active">
							此图表无选项
						</div>
						<div class="admin-show J_opt-TABLE J_opt-INDEX J_opt-LINE J_opt-BAR J_opt-PIE J_opt-FUNNEL J_opt-TREEMAP">
							<label class="custom-control custom-control-sm custom-checkbox mb-2">
								<input class="custom-control-input" type="checkbox" data-name="noPrivileges">
								<span class="custom-control-label"> 使用全部数据 <i class="zmdi zmdi-help zicon" title="不启用则仅能使用权限范围内的数据"></i></span>
							</label>
						</div>
						<div class="J_opt-TABLE">
							<label class="custom-control custom-control-sm custom-checkbox mb-2">
								<input class="custom-control-input" type="checkbox" data-name="showLineNumber">
								<span class="custom-control-label"> 显示行号</span>
							</label>
							<label class="custom-control custom-control-sm custom-checkbox">
								<input class="custom-control-input" type="checkbox" data-name="showSums">
								<span class="custom-control-label"> 显示汇总</span>
							</label>
						</div>
					</div>
				</div>
			</div>
		</aside>
		<div class="main-content container-fluid">
			<div class="axis-editor">
				<div class="axis J_dimension">
					<div class="axis-head">
						<span>纬度</span>
						<a><i class="zmdi zmdi-edit"></i></a>
					</div>
					<div class="axis-target J_axis-dim"></div>
				</div>
				<div class="axis J_numerical">
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
	<li class="dropdown-item J_date" data-calc="M">按月</li>
	<li class="dropdown-item J_date" data-calc="D">按日</li>
	<li class="dropdown-item J_date" data-calc="H">按时</li>
	<li class="dropdown-divider"></li>
	<li class="dropdown-submenu J_sort">
		<a class="dropdown-item">排序</a>
		<ul class="dropdown-menu">
			<li class="dropdown-item" data-sort="NONE">默认</li>
			<li class="dropdown-item" data-sort="ASC">升序</li>
			<li class="dropdown-item" data-sort="DESC">降序</li>
		</ul>
	</li>
	<li class="dropdown-item">显示样式</li>
</ul>
</span>
</script>
<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = {
	sourceEntity: '${entityName}',
	chartId: '${chartId}',
	chartConfig: ${chartConfig} || {},
	chartOwningAdmin: ${chartOwningAdmin}
}
</script>
<script src="${baseUrl}/assets/lib/charts/echarts.min.js"></script>
<script src="${baseUrl}/assets/js/charts/charts.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/rb-advfilter.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/charts/chart-design.jsx" type="text/babel"></script>
</body>
</html>
