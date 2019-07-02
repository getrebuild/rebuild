<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>触发器</title>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-aside rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="触发器" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="robot-trigger" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<aside class="page-aside">
			<div class="rb-scroller">
				<div class="dept-tree">
					<h5 class="text-muted" style="margin-bottom:19px;margin-top:17px;border-bottom:1px solid #eee;padding-bottom:10px;">源实体</h5>
					<ul class="list-unstyled">
						<li class="active"><a>所有实体</a></li>
					</ul>
				</div>
			</div>
		</aside>
		<div class="page-head">
			<div class="float-left"><div class="page-head-title">触发器</div></div>
			<div class="float-right pt-1">
				<button class=" btn btn-primary J_add" type="button"><i class="icon zmdi zmdi-plus"></i> 添加</button>
			</div>
			<div class="clearfix"></div>
		</div>
		<div class="main-content container-fluid pt-0" id="list">
			<%@ include file="/_include/phitem.jsp"%>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/entity/trigger-list.jsx" type="text/babel"></script>
</body>
</html>
