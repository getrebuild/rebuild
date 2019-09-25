<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page import="com.rebuild.server.Application"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>通用配置</title>
<style type="text/css">
.syscfg a.img-thumbnail{display:inline-block;padding:0.3rem 0;background-color:#fff;line-height:1;font-size:0;cursor:default;}
.syscfg a.img-thumbnail .logo-img{transform: scale(0.8);}
.syscfg h5{background-color:#eee;margin:0;padding:10px;}
.syscfg .table td{padding:10px;}
.syscfg .table td p{margin:0;color:#999;font-weight:normal;font-size:12px;}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
<jsp:include page="/_include/NavTop.jsp">
	<jsp:param value="通用配置" name="pageTitle"/>
</jsp:include>
<jsp:include page="/_include/NavLeftAdmin.jsp">
	<jsp:param value="systems" name="activeNav"/>
</jsp:include>
<div class="rb-content">
	<div class="main-content container-fluid syscfg">
		<div class="row">
			<div class="col-lg-9 col-12">
				<div class="card">
					<div class="card-header card-header-divider">通用配置</div>
					<div class="card-body">
						<h5>通用</h5>
						<table class="table">
						<tbody>
							<tr>
								<td width="40%">名称<p>这将完全替换 REBUILD 的品牌名称</p></td>
								<td>${appName}</td>
							</tr>
							<tr>
								<td>LOGO</td>
								<td>
									<a class="img-thumbnail"><i class="logo-img"></i></a>
									<a class="img-thumbnail bg-primary"><i class="logo-img white"></i></a>
								</td>
							</tr>
							<tr>
								<td>域名/主页地址</td>
								<td><a href="${HomeURL}" class="link" target="_blank">${HomeURL}</a></td>
							</tr>
							<tr>
								<td>公开注册</td>
								<td>${OpenSignUp ? "是" : "否"}</td>
							</tr>
							<tr>
								<td>登录页每日一图</td>
								<td>${LiveWallpaper ? "是" : "否"}</td>
							</tr>
						</tbody>
						</table>
						<h5>安全性</h5>
						<table class="table">
						<tbody>
							<tr>
								<td width="40%">登录密码安全策略</td>
								<td>
								<c:choose>
									<c:when test="${PasswordPolicy >= 3}">高 (最低8位，必须同时包含数字、字母、特殊字符)</c:when>
									<c:when test="${PasswordPolicy == 2}">中 (最低6位，必须同时包含数字、字母)</c:when>
									<c:otherwise>低 (最低6位，无字符类型限制)</c:otherwise>
								</c:choose>
								</td>
							</tr>
							<tr>
								<td>回收站数据保留时间</td>
								<td>${RecycleBinKeepingDays}天</td>
							</tr>
						</tbody>
						</table>
					</div>
				</div>
			</div>
			<div class="col-lg-3 col-12">
				<div class="card">
					<div class="card-header card-header-divider">关于 REBUILD</div>
					<div class="card-body">
						<p class="mb-1">系统版本 <a class="link" target="_blank" href="https://getrebuild.com/download?v=<%=Application.VER%>"><%=Application.VER%></a></p>
						<p class="mb-2">许可类型 <a class="link" target="_blank" href="https://getrebuild.com/authority?sn=${SN}">开源社区版</a></p>
						<ul style="line-height:2">
							<li><a class="link" target="_blank" href="${baseUrl}/gw/server-status">系统状态</a></li>
							<li><a class="link" target="_blank" href="https://getrebuild.com/docs/">帮助文档</a></li>
							<li><a class="link" target="_blank" href="https://getrebuild.com/">技术支持</a></li>
						</ul>
						<div class="text-muted">
							&copy; REBUILD 使用开源 <a class="link" href="https://raw.githubusercontent.com/getrebuild/rebuild/master/LICENSE" target="_blank">GPL3.0</a> 和 <a class="link" href="https://raw.githubusercontent.com/getrebuild/rebuild/master/COMMERCIAL" target="_blank">商用</a> 双重授权许可，请遵守许可协议。
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
</body>
</html>
