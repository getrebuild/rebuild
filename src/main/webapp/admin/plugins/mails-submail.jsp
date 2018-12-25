<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.rebuild.utils.StringsUtils"%>
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
		<jsp:param value="plugins-mails" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="main-content container-fluid syscfg">
			<div class="row">
				<div class="col-9">
					<div class="card">
						<div class="card-header card-header-divider">赛邮 SUBMAIL</div>
						<div class="card-body">
							<% 
							String smsAccount[] = SystemConfiguration.getSmsAccount();
							%>
							<h5>短信服务</h5>
							<table class="table">
							<tbody>
								<tr>
									<td width="50%">APPID</td>
									<td><%=smsAccount == null ? "" : smsAccount[0]%></td>
								</tr>
								<tr>
									<td>APPKEY</td>
									<td><%=smsAccount == null ? "" : StringsUtils.stars(smsAccount[1])%></td>
								</tr>
								<tr>
									<td>短信签名</td>
									<td><%=smsAccount == null ? "" : smsAccount[2]%></td>
								</tr>
							</tbody>
							</table>
							<% 
							String mailAccount[] = SystemConfiguration.getEmailAccount();
							%>
							<h5>邮件服务</h5>
							<table class="table">
							<tbody>
								<tr>
									<td width="50%">APPID</td>
									<td><%=mailAccount == null ? "" : mailAccount[0]%></td>
								</tr>
								<tr>
									<td>APPKEY</td>
									<td><%=mailAccount == null ? "" : StringsUtils.stars(mailAccount[1])%></td>
								</tr>
								<tr>
									<td>发件人地址</td>
									<td><%=mailAccount == null ? "" : mailAccount[2]%></td>
								</tr>
								<tr>
									<td>发件人名称</td>
									<td><%=mailAccount == null ? "" : mailAccount[3]%></td>
								</tr>
							</tbody>
							</table>
						</div>
					</div>
				</div>
				<div class="col-3">
				</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
</body>
</html>
