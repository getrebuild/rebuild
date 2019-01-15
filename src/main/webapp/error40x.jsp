<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" trimDirectiveWhitespaces="true"%>
<%@ page import="com.rebuild.utils.AppUtils"%>
<%@ page import="cn.devezhao.commons.web.ServletUtils"%>
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
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<link rel="shortcut icon" href="${pageContext.request.contextPath}/assets/img/favicon.png" type="image/x-icon">
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/assets/lib/material-design-iconic-font.min.css">
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/assets/css/rb-base.css">
<title>提示</title>
</head>
<body class="rb-splash-screen">
<div class="rb-wrapper rb-error">
	<div class="rb-content m-0">
		<div class="main-content container">
			<div class="error-container">
				<div class="error-number mb-0"><i class="zmdi zmdi-info text-warning"></i></div>
				<div class="error-description"><%=errorMsg%></div>
				<div class="error-goback-button">
					<a class="btn btn-xl btn-primary" href="${pageContext.request.contextPath}/dashboard/home">返回首页</a>
				</div>
			</div>
		</div>
	</div>
</div>
<script src="${pageContext.request.contextPath}/assets/lib/jquery.min.js"></script>
<script>
if (location.href.indexOf('unsupported-browser') > -1) $('.error-description').text('不支持 IE10 以下的浏览器，请使用 Edge、Chrome 或 Firefox')
</script>
</body>
</html>