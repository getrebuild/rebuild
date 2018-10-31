<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" trimDirectiveWhitespaces="true"%>
<%@ page import="com.rebuild.web.common.SimplePageForward"%>
<%@ page import="com.rebuild.utils.AppUtils"%>
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
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/rb-base.css">
<title>提示</title>
</head>
<body class="rb-splash-screen">
<div class="rb-wrapper rb-error">
	<div class="rb-content m-0">
		<div class="main-content container-fluid">
			<div class="error-container">
				<div class="error-number"><%=errorCode%></div>
				<div class="error-description"><%=errorMsg%></div>
				<div class="error-goback-button">
					<a class="btn btn-xl btn-primary hide J_home" href="${baseUrl}/dashboard/home">返回首页</a>
					&nbsp;
					<a class="btn btn-xl btn-primary hide J_reload" href="javascript:;" onclick="location.reload(true)">重试</a>
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
	if ($('.error-number').text() == '404') $('.J_reload').remove()
})()
</script>
</body>
</html>