<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" trimDirectiveWhitespaces="true"%>
<%@ page import="cn.devezhao.rebuild.web.common.SimplePageForward"%>
<%@ page import="cn.devezhao.rebuild.utils.AppUtils"%>
<%@ page import="cn.devezhao.commons.web.ServletUtils"%>
<%
String errorMsg = AppUtils.getErrorMessage(request, exception);
if (ServletUtils.isAjaxRequest(request)) {
	out.print(errorMsg);
	return;
}
SimplePageForward.setPageAttribute(request);
Integer errorCode = (Integer) request.getAttribute(ServletUtils.ERROR_STATUS_CODE);
errorCode = errorCode == null ? 400 : errorCode;
%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>提示</title>
</head>
<body class="rb-splash-screen">
<div class="rb-wrapper rb-error">
	<div class="rb-content" style="margin:0">
		<div class="main-content container-fluid">
			<div class="error-container">
				<div class="error-number"><%=errorCode%></div>
				<div class="error-description"><%=errorMsg%></div>
				<div class="error-goback-button">
					<a class="btn btn-xl btn-primary hide J_home" href="${baseUrl}/dashboard/home">返回首页</a>
					<a class="btn btn-xl btn-primary hide J_reload" href="javascript:;" onclick="_reload()">刷新</a>
				</div>
			</div>
		</div>
	</div>
</div>
<script src="${baseUrl}/assets/lib/jquery.min.js"></script>
<script type="text/javascript">
(function(){
	if (self != top) $('.J_reload').removeClass('hide')
	else $('.J_home').removeClass('hide')
})()
_reload = function(){
	location.reload(true)
}
</script>
</body>
</html>