<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>云存储配置</title>
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
				<div class="col-lg-9 col-12">
					<div class="card">
						<div class="card-header card-header-divider">
                            七牛云 QINIU
                            <a href="#modfiy" class="float-right"><i class="icon zmdi zmdi-edit"></i> 修改</a>
                        </div>
						<div class="card-body">
							<h5><a class="cl-base" href="https://portal.qiniu.com/signup?utm_source=getrebuild.com&code=3letk048wdsnm" target="_blank" rel="noopener noreferrer">云存储</a></h5>
							<table class="table">
							<tbody>
								<tr>
									<td width="40%">访问域名</td>
									<td data-id="StorageURL">${storageAccount == null ? "未配置" : storageAccount[3]}</td>
								</tr>
								<tr>
									<td>存储空间<p>存储空间变更需你自行迁移原有数据</p></td>
									<td data-id="StorageBucket">${storageAccount == null ? "未配置" : storageAccount[2]}</td>
								</tr>
								<tr>
									<td>秘钥 AK</td>
									<td data-id="StorageApiKey">${storageAccount == null ? "未配置" : storageAccount[0]}</td>
								</tr>
								<tr>
									<td>秘钥 SK</td>
									<td data-id="StorageApiSecret">${storageAccount == null ? "未配置" : storageAccount[1]}</td>
								</tr>
							</tbody>
							</table>
							<c:if test="${!storageStatus}">
                            <div class="alert alert-warning alert-icon mt-6">
                                <div class="icon"><span class="zmdi zmdi-alert-triangle"></span></div>
                                <div class="message">七牛云存储账户未配置或配置有误，当前已启用本地文件存储</div>
                            </div>
							</c:if>
                            <div class="edit-footer">
                                <button class="btn btn-link">取消</button>
                                <button class="btn btn-primary">保存</button>
                            </div>
						</div>
					</div>
				</div>
				<div class="col-lg-3 col-12">
					<div class="card">
						<div class="card-header card-header-divider">存储大小</div>
						<div class="card-body">
							<strong>${_StorageSize}</strong>
							<c:choose>
								<c:when test="${storageStatus}"> (七牛云)</c:when>
								<c:otherwise> (本地存储)</c:otherwise>
							</c:choose>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/admin/syscfg.jsx" type="text/babel"></script>
</body>
</html>