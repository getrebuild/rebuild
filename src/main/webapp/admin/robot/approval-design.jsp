<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/approvals.css">
<title>编辑审批流程</title>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="编辑触发器" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="robot-approval" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="page-head">
			<div class="float-left">
				<div class="page-head-title">编辑审批流程<span class="sub-title">${name}</span></div>
			</div>
			<div class="float-right pt-1">
				<button class="btn btn-primary J_save" type="button">保存并发布</button>
			</div>
			<div class="clearfix"></div>
		</div>
		<div class="main-content container-fluid pt-1">
			<div class="row wizard-row">
				<div class="col-md-12 fuelux">
					<div class="wizard wizard-ux rounded">
						<div class="steps-container">
							<ul class="steps">
								<li data-step="1">基本配置 <i class="chevron"></i></li>
								<li data-step="2" class="active">流程设计 <i class="chevron"></i></li>
							</ul>
							<div class="step-content p-0">
								<div data-step="1" class="step-pane"></div>
								<div data-step="2" class="step-pane active">
									<div class="rbflow-design rb-loading rb-loading-active" id="rbflow">
										<%@ include file="/_include/spinner.jsp"%>
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
	<div class="rb-right-sidebar">
		<div class="sb-content">
			<div id="config-side">
				配置项
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = {
	configId: '${configId}',
	name: '${name}',
	sourceEntity: '${applyEntity}'
}
</script>
<script src="${baseUrl}/assets/js/rb-advfilter.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/entity/approval-design.jsx" type="text/babel"></script>
</body>
</html>
