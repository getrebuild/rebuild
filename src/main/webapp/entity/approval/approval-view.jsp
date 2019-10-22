<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/view-page.css">
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/approvals.css">
<title>审批流程图</title>
</head>
<body class="view-body" style="background-color:#f5f5f7">
<div class="view-header">
	<i class="header-icon zmdi zmdi-usb zmdi-hc-rotate-180"></i>
	<h3 class="title">审批流程图</h3>
	<span>
		<a class="close J_close"><i class="zmdi zmdi-close"></i></a>
	</span>
</div>
<div class="main-content container-fluid">
    <div class="rbflow-design preview" id="rbflow">
    </div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/entityhub/approval-design.jsx" type="text/babel"></script>
<script>
window.__PageConfig = {
    id: '${approvalId}'
}
</script>
<script type="text/babel">
window.resize_handler = function () {}
$(document).ready(function() {
    const ph = (parent && parent.RbViewModal) ? parent.RbViewModal.holder(window.__PageConfig.id) : null
    if (ph) $('.J_close').click(() => ph.hide())
    else $('.J_close').remove()

	$.get(rb.baseUrl + '/app/entity/approval/flow-definition?id=' + window.__PageConfig.id, function(res) {
		if (res.error_code !== 0) {
			RbHighbar.error(res.error_msg)
			return
		}
		wpc = { ...res.data, preview: true }
		renderRbcomp(<RbFlowCanvas />, 'rbflow', function() {
            ph && ph.hideLoading()
		})
	})
})
</script>
</body>
</html>
