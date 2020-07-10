<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/view-page.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/project-tasks.css">
<title>任务视图</title>
</head>
<body class="view-body">
<div class="view-header">
    <i class="header-icon zmdi zmdi-${projectIcon}"></i>
    <h3 class="title">任务视图</h3>
    <span>
		<a class="close J_close"><i class="zmdi zmdi-close"></i></a>
		<a class="close sm J_reload"><i class="zmdi zmdi-refresh"></i></a>
	</span>
</div>
<div class="main-content container-fluid">
    <div id="task-contents"></div>
</div>
<div id="task-comments"></div>
<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = {
    type: 'TaskView',
    taskId: '${id}',
}
</script>
<script src="${baseUrl}/assets/js/project/task-view.jsx" type="text/babel"></script>
</body>
</html>
