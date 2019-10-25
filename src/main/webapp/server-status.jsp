<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.rebuild.utils.AppUtils"%>
<%@ page import="com.rebuild.server.helper.SysConfiguration"%>
<%@ page import="org.apache.commons.lang.SystemUtils"%>
<%@ page import="cn.devezhao.commons.CalendarUtils"%>
<%@ page import="com.rebuild.server.ServerListener"%>
<%@ page import="com.rebuild.server.Application"%>
<%@ page import="com.rebuild.server.ServerStatus"%>
<%@ page import="com.rebuild.server.ServerStatus.Status"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<link rel="shortcut icon" href="${baseUrl}/assets/img/favicon.png">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/rb-base.css">
<title>系统状态</title>
<style type="text/css">
.block{max-width:1000px;padding:0 14px;margin:30px auto 0;}
.error{background-color:#ea4335;color:#fff;padding:18px 0;}
.error a{color:#fff;text-decoration:underline;}
</style>
</head>
<body>
<% if (!ServerStatus.isStatusOK()) { %>
<div class="error">
<div class="block mt-0">
	<h2 class="mt-0">系统故障</h2>
	<div>部分服务未能正常启动，请通过快速检查列表排除故障，故障排除后建议重启服务。你也可以获取 <a href="https://getrebuild.com/contact?sn=#tech-supports">技术支持</a></div>
</div>
</div>
<% } %>
<div class="block">
	<h5 class="text-bold">快速检查</h5>
	<table class="table table-bordered table-sm table-hover">
	<tbody>
		<% for (Status s : ServerStatus.getLastStatus()) { %>
		<tr>
			<th width="30%"><%=s.name%></th>
			<td class="text-danger"><%=s.success ? "<span class='text-success'>OK<span>" : ("ERROR : " + s.error)%></td>
		</tr>
		<% } %>
		<tr>
			<th>Memory Usage</th>
			<% double memoryUsed[] = ServerStatus.getHeapMemoryUsed(); %>
			<td><%=memoryUsed[1]%>% (<%=memoryUsed[0]%>M)</td>
		</tr>
		<tr>
			<th>CPU Load</th>
			<td>n/a</td>
		</tr>
	</tbody>
	</table>
</div>
<% if (AppUtils.getRequestUser(request) != null) { %>
<div class="block">
	<h5 class="text-bold">系统信息</h5>
	<table class="table table-bordered table-sm table-hover">
	<tbody>
		<tr>
			<th width="30%">App Version</th>
			<td><%=Application.VER%></td>
		</tr>
		<tr>
			<th>Startup Time</th>
			<td><%=ServerListener.getStartupTime()%></td>
		</tr>
		<tr>
			<th>System Time</th>
			<td><%=CalendarUtils.now()%></td>
		</tr>
		<tr>
			<th>OS</th>
			<td><%=SystemUtils.OS_NAME%> (<%=SystemUtils.OS_ARCH%>)</td>
		</tr>
		<tr>
			<th>JVM</th>
			<td><%=SystemUtils.JAVA_VERSION%> (<%=SystemUtils.JAVA_VENDOR%>)</td>
		</tr>
		<tr>
			<th>Data Directory</th>
			<td><%=SysConfiguration.getFileOfData("/")%></td>
		</tr>
		<tr>
			<th>Temp Directory</th>
			<td><%=SysConfiguration.getFileOfTemp("/")%></td>
		</tr>
	</tbody>
	</table>
</div>
<% } %>
<div class="block">
	<div class="text-muted">
		&copy; 2019 <a href="https://getrebuild.com/?utm_source=rebuild">${appName}</a>
		<% if (AppUtils.getRequestUser(request) != null) { %>
		&nbsp;·&nbsp;
		<a href="server-status.json">Status Api</a>
		<% } %>
	</div>
</div>
</body>
</html>