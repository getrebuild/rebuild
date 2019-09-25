<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>云存储配置</title>
<style type="text/css">
.syscfg h5{background-color:#eee;margin:0;padding:10px;}
.syscfg .table td{padding:10px;}
.syscfg .table td p{margin:0;color:#999;font-weight:normal;font-size:12px;}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="云存储配置" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="integration-storage" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container-fluid syscfg">
			<div class="row">
				<div class="col-md-9 col-12">
					<div class="card">
						<div class="card-header card-header-divider">云存储</div>
						<div class="card-body">
							<h5><a class="cl-base" href="https://portal.qiniu.com/signup?utm_source=getrebuild.com&code=3letk048wdsnm" target="_blank" rel="noopener noreferrer">七牛云</a></h5>
							<table class="table">
							<tbody>
								<tr>
									<td width="40%">访问域名</td>
									<td>${storageAccount == null ? "未配置" : storageAccount[3]}</td>
								</tr>
								<tr>
									<td>存储空间</td>
									<td>${storageAccount == null ? "未配置" : storageAccount[2]}</td>
								</tr>
								<tr>
									<td>秘钥 AK</td>
									<td>${storageAccount == null ? "未配置" : storageAccount[0]}</td>
								</tr>
								<tr>
									<td>秘钥 SK</td>
									<td>${storageAccount == null ? "未配置" : storageAccount[1]}</td>
								</tr>
							</tbody>
							</table>
							<c:if test="${!storageStatus}">
								<div class="alert alert-warning alert-icon mt-6">
									<div class="icon"><span class="zmdi zmdi-alert-triangle"></span></div>
									<div class="message">七牛云存储账户配置有误，当前已启用本地文件存储</div>
								</div>
							</c:if>
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