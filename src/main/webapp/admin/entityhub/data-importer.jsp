<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>数据导入</title>
<style type="text/css">
.fuelux .wizard .step-content{padding:30px}
.fuelux .wizard>.steps-container>.steps li.complete:hover{cursor:default;}
#fieldsMapping th, #fieldsMapping td{padding:6px 0;vertical-align:middle;border-bottom:1px dotted #dee2e6;border-top:0 none;}
#fieldsMapping thead th{border-bottom:1px solid #dee2e6;padding-top:9px;}
#fieldsMapping td>em{font-style:normal;background-color:#eee;display:inline-block;min-width:30px;font-size:12px;text-align:center;margin-right:4px;padding-top:1px;color:#777}
#fieldsMapping td>i.zmdi{float:right;color:#aaa;font-size:1.4rem;margin-right:10px}
#ouser-warn .alert{margin-top:10px;margin-bottom:0}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="数据导入" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="data-importer" name="activeNav"/>
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
												<div class="float-left">
													<div class="file-select">
														<input type="file" class="inputfile" id="upload-input" accept=".xlsx,.xls" data-maxsize="20971520">
														<label for="upload-input" class="btn-secondary"><i class="zmdi zmdi-upload"></i><span>选择文件</span></label>
													</div>
												</div>
												<div class="float-left ml-2" style="padding-top:7px">
													<div class="text-bold text-italic J_upload-input"></div>
												</div>
												<div class="clearfix"></div>
								                <div class="form-text mb-0">
									                <ul class="mb-0 pl-4">
														<li>支持上传 Excel/CSV 文件，文件大小不超过 20M</li>
														<li>有合并单元格的数据请处理过后再上传，否则可能出现表头识别有误</li>
														<li>系统默认仅识别第一个 SHEET，且会将首行识别为表头</li>
														<li>更多导入帮助请 <a href="https://getrebuild.com/docs/admin/data-import" target="_blank">参考文档</a></li>
													</ul>
								                </div>
											</div>
										</div>
										<div class="form-group row">
											<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">遇到重复记录时</label>
											<div class="col-md-12 col-xl-6 col-lg-8">
												<div style="margin-top:6px;">
													<label class="custom-control custom-control-sm custom-radio custom-control-inline">
														<input class="custom-control-input" type="radio" name="repeatOpt" value="1" checked="checked"><span class="custom-control-label">覆盖 (更新)</span>
													</label>
													<label class="custom-control custom-control-sm custom-radio custom-control-inline">
														<input class="custom-control-input" type="radio" name="repeatOpt" value="2"><span class="custom-control-label">跳过</span>
													</label>
													<label class="custom-control custom-control-sm custom-radio custom-control-inline">
														<input class="custom-control-input" type="radio" name="repeatOpt" value="3"><span class="custom-control-label">仍旧导入</span>
													</label>
												</div>
												<div class="J_repeatFields">
													<label>重复判断字段<i class="zmdi zmdi-help zicon" data-toggle="tooltip" title="选择的字段必须存在字段映射，否则会导致重复判断有误"></i></label>
													<select class="form-control form-control-sm" id="repeatFields" multiple="multiple">
													</select>
												</div>
											</div>
										</div>
										<div class="form-group row">
											<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">记录所属用户</label>
											<div class="col-md-12 col-xl-6 col-lg-8">
												<select class="form-control form-control-sm" id="toUser">
												</select>
												<div class="form-text mb-0">
													不选择则默认为当前用户，如字段映射中指定了用户则以映射为准
								                </div>
								                <div id="ouser-warn"></div>
											</div>
										</div>
										<div class="form-group row footer">
											<div class="col-md-12 col-xl-6 col-lg-8 offset-xl-3 offset-lg-4">
												<button class="btn btn-primary J_step1-btn" type="button" data-loading-text="正在预处理">下一步</button>
											</div>
										</div>
									</form>
								</div>
								<div data-step="2" class="step-pane">
									<form class="simple">
										<div class="form-group row pt-0">
											<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">设置字段映射</label>
											<div class="col-md-12 col-xl-9 col-lg-8">
												<table id="fieldsMapping" class="table table-hover">
												<thead>
													<tr>
														<th width="240">数据列</th>
														<th width="240">导入到字段</th>
														<th></th>
													</tr>
												</thead>
												<tbody>
												</tbody>
												</table>
											</div>
										</div>
										<div class="form-group row footer">
											<div class="col-md-12 col-xl-6 col-lg-8 offset-xl-3 offset-lg-4">
												<button class="btn btn-primary J_step2-btn" type="button">开始导入</button>
												&nbsp;&nbsp;
												<button class="btn btn-link J_step2-return" type="button">返回上一步</button>
											</div>
										</div>
									</form>
								</div>
								<div data-step="3" class="step-pane">
									<form class="simple" style="margin:30px auto">
										<div class="row mb-2">
											<div class="col-6"><h5 class="text-bold m-0 p-0 J_import_state">正在准备数据 ...</h5></div>
											<div class="col-6 text-right text-muted">耗时 <span class="J_import_time">00:00:00</span></div>
										</div>
										<div class="progress">
											<div class="progress-bar progress-bar-animated J_import-bar" style="width:0"></div>
										</div>
										<div class="mt-3">
											<button class="btn btn-danger J_step3-cancel" type="button">终止导入</button>
											<a class="btn btn-link J_step3-logs hide" href="data-importer">继续导入</a>
										</div>
									</form>
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
<script src="${baseUrl}/assets/js/entityhub/data-importer.jsx" type="text/babel"></script>
</body>
</html>
