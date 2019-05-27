<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/classification.css">
<title>分类数据</title>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="分类数据" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="classifications" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="page-head">
			<div class="float-left"><div class="page-head-title">分类数据</div></div>
			<div class="float-right pt-1">
				<button class=" btn btn-primary J_add" type="button"><i class="icon zmdi zmdi-plus"></i> 添加</button>
			</div>
			<div class="clearfix"></div>
		</div>
		<div class="main-content container-fluid pt-0">
			<div class="card-list row">
			<c:forEach items="${classifications}" var="item">
				<div class="col-xl-2 col-lg-3 col-md-4 col-sm-6">
					<div class="card" data-id="${item[0]}" data-disabled="${item[2]}">
						<div class="card-body">
							<a href="classification/${item[0]}">${item[1]}</a>
							<p class="text-muted m-0 fs-12">${item[3]}级 · ${item[4]} 项数据</p>
						</div>
						<div class="card-footer card-footer-contrast text-muted">
							<div class="float-left">
								<a class="J_edit" href="javascript:;"><i class="zmdi zmdi-edit"></i></a>
								<a class="J_del" href="javascript:;"><i class="zmdi zmdi-delete"></i></a>
							</div>
							<div class="float-right fs-12 text-warning">${item[2] ? "已禁用" : ""}</div>
							<div class="clearfix"></div>
						</div>
					</div>
				</div>
			</c:forEach>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/entity/classification.jsx" type="text/babel"></script>
</body>
</html>
