<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>短信/邮件配置</title>
<style type="text/css">
.syscfg h5{background-color:#eee;margin:0;padding:10px;}
.syscfg .table td{padding:10px;}
.syscfg .table td p{margin:0;color:#999;font-weight:normal;font-size:12px;}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="短信/邮件配置" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="integration-submail" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container-fluid syscfg">
			<div class="row">
				<div class="col-md-9 col-12">
					<div class="card">
						<div class="card-header card-header-divider"><a class="cl-base" href="https://www.mysubmail.com/?utm_source=getrebuild.com" target="_blank" rel="noopener noreferrer">赛邮 SUBMAIL</a></div>
						<div class="card-body">
							<h5>邮件服务</h5>
							<c:choose>
								<c:when test="${mailAccount != null}">
									<table class="table">
									<tbody>
										<tr>
											<td width="40%">APPID</td>
											<td>${mailAccount[0]}</td>
										</tr>
										<tr>
											<td>APPKEY</td>
											<td>${mailAccount[1]}</td>
										</tr>
										<tr>
											<td>发件人地址</td>
											<td>${mailAccount[2]}</td>
										</tr>
										<tr>
											<td>发件人名称</td>
											<td>${mailAccount[3]}</td>
										</tr>
									</tbody>
									</table>
								</c:when>
								<c:otherwise>
									<div class="alert alert-danger alert-icon mt-6 mb-6">
										<div class="icon"><span class="zmdi zmdi-close-circle-o"></span></div>
										<div class="message">邮件账号未配置，邮件相关功能不可用</div>
									</div>
								</c:otherwise>
							</c:choose>
							<h5>短信服务</h5>
							<c:choose>
								<c:when test="${smsAccount != null}">
									<table class="table">
									<tbody>
										<tr>
											<td width="40%">APPID</td>
											<td>${smsAccount[0]}</td>
										</tr>
										<tr>
											<td>APPKEY</td>
											<td>${smsAccount[1]}</td>
										</tr>
										<tr>
											<td>短信签名</td>
											<td>${smsAccount[2]}</td>
										</tr>
									</tbody>
									</table>
								</c:when>
								<c:otherwise>
									<div class="alert alert-danger alert-icon mt-6">
										<div class="icon"><span class="zmdi zmdi-close-circle-o"></span></div>
										<div class="message">短信账号未配置，短信相关功能不可用</div>
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
