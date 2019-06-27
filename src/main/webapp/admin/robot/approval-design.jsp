<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/css/approvals.css">
<title>编辑审核流程</title>
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
			<div class="float-left"><div class="page-head-title">编辑审核流程</div></div>
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
							<div class="step-content">
								<div data-step="1" class="step-pane">
								</div>
								<div data-step="2" class="step-pane active">
									<div class="rbflow-design">
										<div class="zoom">
											<div class="zoom-out"></div>
											<span>100%</span>
											<div class="zoom-in"></div>
										</div>
										<div class="box-scale" id="box-scale">
											<div class="node-wrap">
												<div class="node-wrap-box node_sid-startevent start-node">
													<div>
														<div class="title"
															style="background: rgb(87, 106, 149) none repeat scroll 0% 0%;">
															<span class="">发起人</span>
														</div>
														<div class="content">
															<div class="text">所有人</div>
															<i class="anticon anticon-right arrow"></i>
														</div>
													</div>
												</div>
												<div class="add-node-btn-box">
													<div class="add-node-btn">
														<button type="button"><i class="zmdi zmdi-plus"></i></button>
													</div>
												</div>
											</div>
											<div class="end-node">
												<div class="end-node-circle"></div>
												<div class="end-node-text">流程结束</div>
											</div>
										</div>
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>
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
<script src="${baseUrl}/assets/lib/charts/raphael.min.js"></script>
<script src="${baseUrl}/assets/lib/charts/flowchart.min.js"></script>
<script src="${baseUrl}/assets/js/rb-advfilter.jsx" type="text/babel"></script>
</body>
</html>
