<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true"%>
<%@ page import="cn.devezhao.rebuild.utils.AppUtils"%>
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
<%@ include file="/_include/Head.jsp" %>
<title>提示</title>
</head>
<body>
<div class="page__hd">
	<div style="text-align:center;margin-top:15%;">
		<h4 style="margin:14px;font-size:17px;line-height:1.6;font-weight:normal;color:#444;">
			<%=errorCode%>
			<div class="hide"><%=errorMsg%></div>
		</h4>
	</div>
</div>
</body>
</html>