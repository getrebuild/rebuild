<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
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
				CONFIG
			</div>
		</aside>
		<div class="main-content container">
			<div class="axis-warp">
				<div class="axis">
					<div class="axis-head">
						<span>纬度</span>
						<a><i class="zmdi zmdi-edit"></i></a>
					</div>
					<div class="axis-target"></div>
				</div>
				<div class="axis">
					<div class="axis-head">
						<span>数值</span>
						<a><i class="zmdi zmdi-edit"></i></a>
					</div>
					<div class="axis-target"></div>
				</div>
			</div>
			<div class="preview"></div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/chart-design.jsx" type="text/babel"></script>
<script type="text/babel">
$(document).ready(function(){
})
</script>
</body>
</html>
