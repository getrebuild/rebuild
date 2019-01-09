<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>数据导入</title>
<style type="text/css">
.fuelux .wizard .step-content{padding:30px}
form.dropzone{position:relative;}
form.dropzone #upload-input{opacity:0;z-index:-1;width:0;height:0;}
form.dropzone>label{cursor:pointer;}
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
										<input type="file" id="upload-input" accept="数据表; .xls,.xlsx,.csv" alt="数据表">
										<label for="upload-input" class="m-0 p-0">
											<div class="icon"><span class="zmdi zmdi-cloud-upload"></span></div>
											<h4>拖动文件到此或点击选择文件</h4>
											<span class="note">支持上传 Excel/CSV 文件，文件大小不超过 20M</span>
										</label>
									</form>
									<h5>数据表说明</h5>
									<ul class="mb-0">
										<li>请上传标准行列的数据表，有合并单元格的数据请处理过后再上传，否则可能出现表头识别有误</li>
										<li>系统默认仅识别第一个 SHEET，且会将首行识别为表头</li>
									</ul>
								</div>
								<div data-step="2" class="step-pane">
									<div class="rb-spinner text-center">
								        <svg width="40px" height="40px" viewBox="0 0 66 66" xmlns="http://www.w3.org/2000/svg">
								            <circle fill="none" stroke-width="4" stroke-linecap="round" cx="33" cy="33" r="30" class="circle"></circle>
								        </svg>
								    </div>
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
<script src="${baseUrl}/assets/js/entity/datas-imports.jsx" type="text/babel"></script>
</body>
</html>
