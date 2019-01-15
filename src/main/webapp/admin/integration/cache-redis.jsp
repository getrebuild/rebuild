<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>缓存系统配置</title>
<style type="text/css">
.syscfg h5{background-color:#eee;margin:0;padding:10px;}
.syscfg .table td{padding:10px;}
.syscfg .table td p{margin:0;color:#999;font-weight:normal;font-size:12px;}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="缓存系统配置" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="integration-cache" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container-fluid syscfg">
			<div class="row">
				<div class="col-md-9 col-12">
					<div class="card">
						<div class="card-header card-header-divider">缓存系统</div>
						<div class="card-body">
							<h5>Redis</h5>
							<c:choose>
								<c:when test="${cacheAccount != null}">
									<table class="table">
									<tbody>
										<tr>
											<td width="50%">缓存服务器</td>
											<td>${cacheAccount[0]}:${cacheAccount[1]}</td>
										</tr>
										<tr>
											<td>访问秘钥</td>
											<td>${cacheAccount[2]}</td>
										</tr>
									</tbody>
									</table>
								</c:when>
								<c:otherwise>
									<div class="alert alert-warning alert-icon mt-6">
										<div class="icon"><span class="zmdi zmdi-alert-triangle"></span></div>
										<div class="message">REDIS 缓存服务未配置，已启用 EHCACHE 内建缓存，生产环境下建议配置 REDIS</div>
									</div>
								</c:otherwise>
							</c:choose>
						</div>
					</div>
				</div>
				<div class="col-md-3 col-12">
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
</body>
</html>