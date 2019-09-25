<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true"%>
<%@ page import="com.rebuild.utils.AppUtils"%>
<%@ page import="cn.devezhao.commons.web.ServletUtils"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils"%>
<%
final String errorMsg = AppUtils.getErrorMessage(request, exception);
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
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<link rel="shortcut icon" href="${baseUrl}/assets/img/favicon.png">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/material-design-iconic-font.min.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/rb-base.css">
<title>系统错误 · ${appName}</title>
</head>
<body class="rb-splash-screen">
<div class="rb-wrapper rb-error">
	<div class="rb-content m-0">
		<div class="main-content container">
			<div class="error-container">
				<div class="error-number mb-0"><i class="zmdi zmdi-close-circle text-danger"></i></div>
				<div class="error-description"><%=errorMsg.split("\n")[0]%></div>
				<div class="error-description text-left <%=AppUtils.devMode() ? "" : "hide"%> ">
					<pre><%=StringEscapeUtils.escapeHtml(errorMsg)%></pre>
				</div>
				<div class="error-goback-button">
					<a class="btn btn-xl btn-space btn-secondary" href="${baseUrl}/dashboard/home">返回首页</a>
					<a class="btn btn-xl btn-space btn-primary" href="javascript:;" onclick="location.reload(true)">重试</a>
					<div class="mt-4">
						<a href="https://github.com/getrebuild/rebuild/issues/new?title=error-500" target="_blank">报告此问题</a>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<script src="${baseUrl}/assets/lib/jquery.min.js"></script>
<script>
if (self != top) $('.btn-secondary').remove()
</script>
</body>
</html>