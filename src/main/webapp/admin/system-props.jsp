<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>通用配置</title>
<style type="text/css">
a.img-thumbnail{display:inline-block;padding:0.6rem;background-color:#fff;line-height:1;font-size:0;}
a.img-thumbnail img{max-height:40px;}
h5{background-color:#eee;margin:0;padding:10px;}
.table td{padding:9px 10px;}
.table td p{margin:0;color:#999;font-weight:normal;font-size:12px;}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="通用配置" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="systems" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container-fluid">
			<div class="row">
				<div class="col-9">
					<div class="card">
						<div class="card-header card-header-divider">通用配置</div>
						<div class="card-body">
							<h5>通用</h5>
							<table class="table">
							<tbody>
								<tr>
									<td width="50%">名称<p>这将完全替换 REBUILD 的品牌名称</p></td>
									<td>REBUILD</td>
								</tr>
								<tr>
									<td>LOGO</td>
									<td>
										<a class="img-thumbnail"><img alt="深色 LOGO" src="../assets/img/logo.png"></a>
										<a class="img-thumbnail bg-primary"><img alt="浅色 LOGO" src="../assets/img/logo-white.png"></a>
									</td>
								</tr>
								<tr>
									<td>主页地址</td>
									<td><a href="http://getrebuild.com/" class="link" target="_blank">http://getrebuild.com/</a></td>
								</tr>
								<tr>
									<td>公开注册</td>
									<td>否</td>
								</tr>
							</tbody>
							</table>
							<h5>安全</h5>
							<table class="table">
							<tbody>
								<tr>
									<td width="50%">登录时需要验证码</td>
									<td>3次以后</td>
								</tr>
								<tr>
									<td>登录密码安全策略</td>
									<td>简单</td>
								</tr>
							</tbody>
							</table>
						</div>
					</div>
				</div>
				<div class="col-3">
					<div class="card">
						<div class="card-header card-header-divider">REBUILD 版本信息</div>
						<div class="card-body">
							<p>REBUILD V1.0.0-SNAPSHOT</p>
							<ul style="line-height:2">
								<li><a href="http://getrebuild.com/" target="_blank">帮助文档</a></li>
								<li><a href="mailto:getrebuild@sina.com?subject=技术支持">技术支持</a></li>
								<li><a href="mailto:getrebuild@sina.com?subject=定制开发与实施部署">定制开发与实施部署</a></li>
								<li><a href="https://github.com/getrebuild/rebuild" target="_blank">View on GitHub</a></li>
							</ul>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
</script>
</body>
</html>
