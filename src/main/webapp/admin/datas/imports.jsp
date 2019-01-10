<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>数据导入</title>
<style type="text/css">
.fuelux .wizard .step-content{padding:30px}
#preview-table .table th,#preview-table .table td{white-space:nowrap;font-size:12px;font-weight:normal;}
#preview-table .table th{padding:7px;padding-top:9px;background-color:#eceff1;border-color:#eceff1}
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
								<li data-step="2">字段映射 <i class="chevron"></i></li>
								<li data-step="3">开始导入 <i class="chevron"></i></li>
							</ul>
							<div class="step-content">
								<div data-step="1" class="step-pane active">
									<form class="simple">
										<div class="form-group row">
											<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">选择导入实体</label>
											<div class="col-md-12 col-xl-6 col-lg-8">
												<select class="form-control form-control-sm" id="toEntity">
												</select>
											</div>
										</div>
										<div class="form-group row">
											<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">上传数据文件</label>
											<div class="col-md-12 col-xl-6 col-lg-8">
												<div class="file-select">
								                    <input type="file" class="inputfile" ref="upload-input" id="upload-input" />
								                    <label for="upload-input" class="btn-secondary"><i class="zmdi zmdi-upload"></i><span>选择文件</span></label>
								                </div>
								                <div class="form-text mb-0">
									                <ul class="mb-0 pl-4">
														<li>支持上传 Excel/CSV 文件，文件大小不超过 20M</li>
														<li>有合并单元格的数据请处理过后再上传，否则可能出现表头识别有误</li>
														<li>系统默认仅识别第一个 SHEET，且会将首行识别为表头</li>
													</ul>
								                </div>
											</div>
										</div>
										<div class="form-group row pt-0 pb-0">
											<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">遇到重复记录时</label>
											<div class="col-md-12 col-xl-6 col-lg-8" style="padding-top:6px;">
												<label class="custom-control custom-control-sm custom-radio custom-control-inline">
													<input class="custom-control-input" type="radio" name="repeatOpt" value="1" checked="checked"><span class="custom-control-label">覆盖更新</span>
												</label>
												<label class="custom-control custom-control-sm custom-radio custom-control-inline">
													<input class="custom-control-input" type="radio" name="repeatOpt" value="2"><span class="custom-control-label">忽略导入</span>
												</label>
												<label class="custom-control custom-control-sm custom-radio custom-control-inline">
													<input class="custom-control-input" type="radio" name="repeatOpt" value="2"><span class="custom-control-label">仍旧导入</span>
												</label>
											</div>
										</div>
										<div class="form-group row">
											<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">重复记录判断字段</label>
											<div class="col-md-12 col-xl-6 col-lg-8">
												<select class="form-control form-control-sm" id="repeatField">
												</select>
											</div>
										</div>
										<div class="form-group row footer">
											<div class="col-md-12 col-xl-6 col-lg-8 offset-xl-3 offset-lg-4">
												<button class="btn btn-primary bordered" type="button">下一步</button>
											</div>
										</div>
									</form>
								</div>
								<div data-step="2" class="step-pane">
									<div class="rb-spinner text-center">
								        <svg width="40px" height="40px" viewBox="0 0 66 66" xmlns="http://www.w3.org/2000/svg">
								            <circle fill="none" stroke-width="4" stroke-linecap="round" cx="33" cy="33" r="30" class="circle"></circle>
								        </svg>
								    </div>
								    <div class="preview-data rb-scroller">
								    	<div id="preview-table"></div>
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
