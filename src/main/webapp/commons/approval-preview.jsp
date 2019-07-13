<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/approvals.css">
<style type="text/css">
body{background-color:#f5f5f7}
</style>
<title>审批流程</title>
</head>
<body>
<div class="rbflow-design preview" id="rbflow">
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/entity/approval-design.jsx" type="text/babel"></script>
<script type="text/babel">
window.resize_handler = function () {}
$(document).ready(function() {
	$.get(rb.baseUrl + '/app/entity/approval/flow-definition?id=' + $urlp('id'), function(res) {
		wpc = { ...res.data, preview: true }
		renderRbcomp(<RbFlowCanvas />, 'rbflow')
	})
})
</script>
</body>
</html>
