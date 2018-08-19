<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true"%>
<%@ page import="cn.devezhao.rebuild.web.commons.PageForward"%>
<%@ page import="cn.devezhao.rebuild.utils.AppUtils"%>
<%@ page import="cn.devezhao.commons.web.ServletUtils"%>
<%
PageForward.setPageAttribute(request);
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
<%@ include file="/_include/Head.jsp"%>
<title>提示</title>
</head>
<body class="rb-splash-scree">
<div class="rb-wrapper rb-error rb-error-404">
	<div class="rb-content" style="margin:0">
		<div class="main-content container-fluid">
			<div class="error-container">
				<div class="error-number"><%=errorCode%></div>
				<div class="error-description"><%=errorMsg%></div>
				<div class="error-goback-button">
					<a class="btn btn-xl btn-primary" href="${baseUrl}/dashboard/home.htm">返回首页</a>
				</div>
				<div class="footer">&copy; 2018 Rebuild</div>
			</div>
		</div>
	</div>
</div>
</body>
</html>