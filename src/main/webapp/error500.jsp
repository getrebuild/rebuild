<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" trimDirectiveWhitespaces="true"%>
<%@ page import="com.rebuild.utils.AppUtils"%>
<%@ page import="cn.devezhao.commons.web.ServletUtils"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils"%>
<%
String errorMsg = AppUtils.getErrorMessage(request, exception);
if (ServletUtils.isAjaxRequest(request)) {
	out.print(errorMsg);
	return;
}
%>
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
<link rel="shortcut icon" href="${pageContext.request.contextPath}/assets/img/favicon.png" type="image/x-icon">
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/assets/css/rb-base.css">
<title>错误</title>
</head>
<body class="rb-splash-screen">
<div class="rb-wrapper rb-error">
	<div class="rb-content m-0">
		<div class="main-content container">
			<div class="error-container">
				<div class="error-number mb-0">系统错误</div>
				<div class="error-description"><%=errorMsg.split("\n")[0]%></div>
				<div class="error-description text-left <%=AppUtils.devMode() ? "" : "hide"%> "><pre><%=StringEscapeUtils.escapeHtml(errorMsg)%></pre></div>
				<div class="error-goback-button">
					<a class="btn btn-xl btn-primary" href="${pageContext.request.contextPath}/dashboard/home">返回首页</a>
					&nbsp;
					<a class="btn btn-xl btn-primary" href="javascript:;" onclick="location.reload(true)">重试</a>
				</div>
			</div>
		</div>
	</div>
</div>
</body>
</html>