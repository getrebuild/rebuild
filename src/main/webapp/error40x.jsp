<%@ page isErrorPage="true"%>
<%@ page import="com.rebuild.utils.AppUtils"%>
<%@ page import="cn.devezhao.commons.web.ServletUtils"%>
<%
String errorMsg = AppUtils.getErrorMessage(request, exception);
if (ServletUtils.isAjaxRequest(request)) {
	out.print(errorMsg);
	return;
}
Integer errorCode = (Integer) request.getAttribute(ServletUtils.ERROR_STATUS_CODE);
errorCode = errorCode == null ? 400 : errorCode;
%>
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
<link rel="shortcut icon" href="${pageContext.request.contextPath}/assets/img/favicon.png" type="image/x-icon">
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/assets/css/rb-base.css">
<title>提示</title>
</head>
<body class="rb-splash-screen">
<div class="rb-wrapper rb-error">
	<div class="rb-content m-0">
		<div class="main-content container">
			<div class="error-container">
				<div class="error-number mb-0"><%=errorCode%></div>
				<div class="error-description"><%=errorMsg%></div>
				<div class="error-goback-button">
					<a class="btn btn-xl btn-primary" href="${pageContext.request.contextPath}/dashboard/home">返回首页</a>
				</div>
			</div>
		</div>
	</div>
</div>
</body>
</html>