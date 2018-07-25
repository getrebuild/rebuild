<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="cn.devezhao.rebuild.server.Startup"%>
<%@ page import="cn.devezhao.commons.CalendarUtils"%>
<% final String baseUrl = Startup.getContextPath(); %>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<!--[if IE]>
<script src="https://as.alipayobjects.com/g/component/??es6-shim/0.35.1/es6-sham.min.js,es6-shim/0.35.1/es6-shim.min.js"></script>
<![endif]-->
<link href="//cdn.bootcss.com/material-design-icons/3.0.1/iconfont/material-icons.min.css" rel="stylesheet">
<link href="//cdn.bootcss.com/material-design-lite/1.3.0/material.min.css" rel="stylesheet">
<link href="/re-build/assets/css/base.css" rel="stylesheet">
<script src="//cdn.bootcss.com/babel-standalone/6.26.0/babel.min.js"></script>
<script src="//cdn.bootcss.com/react/16.4.0/umd/react.production.min.js"></script>
<script src="//cdn.bootcss.com/react-dom/16.4.0/umd/react-dom.production.min.js"></script>
<script src="//cdn.bootcss.com/material-design-lite/1.3.0/material.min.js"></script>
<script src="//cdn.bootcss.com/jquery/3.3.1/jquery.min.js"></script>
<script>__baseUrl='<%=baseUrl%>'; __serverTime='<%=CalendarUtils.now().getTime()%>';</script>
<script src="<%=baseUrl%>/assets/js/base.js"></script>