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
							<h5>七牛云</h5>
							<c:choose>
								<c:when test="${storageAccount != null}">
									<table class="table">
									<tbody>
										<tr>
											<td width="50%">访问域名</td>
											<td><a href="${storageAccount[3]}" class="link" target="_blank">${storageAccount[3]}</a></td>
										</tr>
										<tr>
											<td>存储空间</td>
											<td>${storageAccount[2]}</td>
										</tr>
										<tr>
											<td>秘钥 AK</td>
											<td>${storageAccount[0]}</td>
										</tr>
										<tr>
											<td>秘钥 SK</td>
											<td>${storageAccount[1]}</td>
										</tr>
									</tbody>
									</table>
								</c:when>
								<c:otherwise>
									<div class="alert alert-danger alert-icon mt-6">
										<div class="icon"><span class="zmdi zmdi-close-circle-o"></span></div>
										<div class="message">云存储账户未配置，文件/图片上传（下载）功能不可用</div>
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
