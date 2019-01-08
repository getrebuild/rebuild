<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>数据导入</title>
<style type="text/css">
.fuelux .wizard .step-content{padding:30px}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="数据导入" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="data-imports" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="page-head">
			<div class="page-head-title">数据导入</div>
		</div>
		<div class="main-content container-fluid pt-1">
			<div class="row wizard-row">
				<div class="col-md-12 fuelux">
					<div class="wizard wizard-ux rounded">
						<div class="steps-container">
							<ul class="steps">
								<li data-step="1" class="active">上传文件 <i class="chevron"></i></li>
								<li data-step="2">预览数据 <i class="chevron"></i></li>
								<li data-step="3">开始导入 <i class="chevron"></i></li>
							</ul>
							<div class="step-content">
								<div data-step="1" class="step-pane active">
									<form class="dropzone" action="${baseUrl}/filex/upload">
										<div class="icon"><span class="zmdi zmdi-cloud-upload"></span></div>
										<h4>拖动文件到此或点击选择文件</h4>
										<span class="note">支持上传 Excel/CSV 文件，文件大小不超过 20M</span>
									</form>
									<h5>表格说明</h5>
									<ul class="mb-0">
										<li>请上传标准行列的数据表，有合并单元格的数据请处理过后再上传，否则可能出现表头识别有误</li>
										<li>系统默认仅识别第一个 SHEET，且会将首行识别为表头</li>
									</ul>
								</div>
								<div data-step="2" class="step-pane">
								2
								</div>
								<div data-step="3" class="step-pane">
								3
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
<script type="text/babel">
</script>
</body>
</html>
