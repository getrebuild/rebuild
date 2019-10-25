<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<link rel="shortcut icon" href="${baseUrl}/assets/img/favicon.png">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/rb-base.css">
<title>API 列表</title>
</head>
<body>
<div class="container">
    <h5 class="text-bold">API 列表</h5>
    <ul>
        <c:forEach var="api" items="${list}">
            <li><a href="api/${api}" class="text-uppercase">${api}</a></li>
        </c:forEach>
    </ul>
</div>
</body>
</html>