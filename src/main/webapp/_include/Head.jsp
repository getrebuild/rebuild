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
<meta name="rb.env" content="<%=AppUtils.devMode() ? "dev" : "production"%>">
<meta name="rb.baseUrl" content="${baseUrl}">
<%if (AppUtils.isAdminUser(request)){%>
<meta name="rb.isAdminUser" content="true">
<meta name="rb.isAdminVerified" content="<%=AppUtils.isAdminVerified(request)%>">
<%}%>