<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.rebuild.web.admin.AdminEntryControll"%>
<%@ page import="com.rebuild.server.helper.SystemConfigurer"%>
<%@ page import="com.rebuild.utils.AppUtils"%>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
<link rel="shortcut icon" href="${baseUrl}/assets/img/favicon.png" type="image/x-icon">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/material-design-iconic-font.min.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/widget/perfect-scrollbar.min.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/widget/bootstrap-datetimepicker.min.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/animate.min.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/widget/select2.min.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/widget/mprogress.min.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/jquery-ui.min.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/rb-base.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/rb-page.css">
<script>
var rb = rb || {}
rb.baseUrl = '${baseUrl}'
rb.storageUrl = '<%=SystemConfigurer.getStorageUrl()%>'
rb.isAdminUser = <%=AppUtils.isAdminUser(request)%>
rb.isAdminVerified = <%=AdminEntryControll.isAdminVerified(request)%>
</script>