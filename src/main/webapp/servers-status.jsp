<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.rebuild.server.ServersStatus"%>
<%@ page import="java.util.Map"%>
<%@ page import="com.rebuild.web.common.SimplePageForward"%>
<%
SimplePageForward.setPageAttribute(request);
%>
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/rb-base.css">
<title>服务不可用</title>
<style type="text/css">
h4{padding:16px 32px;padding-bottom:6px}
.error-description{background-color:#fff;border:1px solid #ccc;padding:16px}
.error-description dl{margin:0}
</style>
</head>
<body>
<div>
	<h4>以下服务不可用</h4>
	<div class="error-description">
		<% for (Map.Entry<String, String> e : ServersStatus.getLastStatus().entrySet()) { %>
		<dl class="row">
			<dt class="col-2"><%=e.getKey()%></dt>
			<dd class="col-10"><%=e.getValue()%></dd>
		</dl>
		<% }%>
	</div>
</div>
</body>
</html>