<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.rebuild.utils.AppUtils"%>
<%@ page import="com.rebuild.server.helper.SystemConfiguration"%>
<%@ page import="org.apache.commons.lang.SystemUtils"%>
<%@ page import="cn.devezhao.commons.CalendarUtils"%>
<%@ page import="com.rebuild.server.ServerListener"%>
<%@ page import="com.rebuild.server.Application"%>
<%@ page import="com.rebuild.server.ServerStatus"%>
<%@ page import="com.rebuild.server.ServerStatus.State"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/rb-base.css">
<title>系统运行状态</title>
<style type="text/css">
.block{margin:0 auto;max-width:1000px;margin-bottom:32px;padding:0 14px}
</style>
</head>
<body>
<div class="block">
	<h5 class="text-bold">快速检查</h5>
	<table class="table table-bordered table-sm">
	<tbody>
		<% for (State e : ServerStatus.getLastStatus()) { %>
		<tr>
			<th width="30%"><%=e.name%></th>
			<td><%=e.success ? "OK" : e.error%></td>
		</tr>
		<% }%>
		<tr>
			<th>Memory Usage</th>
			<td>n/a</td>
		</tr>
		<tr>
			<th>CPU Usage</th>
			<td>n/a</td>
		</tr>
	</tbody>
	</table>
</div>
<% if (AppUtils.getRequestUser(request) != null) { %>
<div class="block">
	<h5 class="text-bold">系统信息</h5>
	<table class="table table-bordered table-sm">
	<tbody>
		<tr>
			<th width="30%">Application Version</th>
			<td><a href="https://github.com/getrebuild/rebuild/releases"><%=Application.VER%></a></td>
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
			<th>Catalina Base</th>
			<td><%=System.getProperty("catalina.base")%></td>
		</tr>
		<tr>
			<th>Temp Directory</th>
			<td><%=SystemConfiguration.getFileOfTemp("/")%></td>
		</tr>
	</tbody>
	</table>
</div>
<% } %>
<div class="block">
<div class="text-muted">
	&copy; 2019 <a href="https://getrebuild.com/">rebuild</a>
	&nbsp;·&nbsp;
	<a href="server-status.json">Status Api</a>
	<a href="?check=1" style="color:transparent;">CHECK</a>
</div>
</div>
</body>
</html>