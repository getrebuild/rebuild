<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>添加实体</title>
<style type="text/css">
#metaschemas,#metaschemas .rb-loading{min-height:256px}
</style>
</head>
<body class="dialog">
<div class="main-content pt-1">
	<div class="tab-container">
		<ul class="nav nav-tabs">
			<li class="nav-item"><a class="nav-link active" href="#MANUAL" data-toggle="tab">手动</a></li>
			<li class="nav-item"><a class="nav-link J_imports" href="#IMPORTS" data-toggle="tab">导入公共数据</a></li>
		</ul>
		<div class="tab-content m-0 pb-0">
			<div class="tab-pane active" id="MANUAL">
				<div class="form">
					<div class="form-group row">
						<label class="col-sm-3 col-form-label text-sm-right">实体名称</label>
						<div class="col-sm-7">
							<input class="form-control form-control-sm" type="text" id="entityLabel" maxlength="40">
						</div>
					</div>
					<div class="form-group row">
						<label class="col-sm-3 col-form-label text-sm-right">备注</label>
						<div class="col-sm-7">
							<textarea class="form-control form-control-sm row2x" id="comments" maxlength="100" placeholder="可选"></textarea>
						</div>
					</div>
					<div class="form-group row pt-2">
						<label class="col-sm-3 col-form-label text-sm-right"></label>
						<div class="col-sm-7">
							<label class="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
								<input class="custom-control-input" type="checkbox" id="nameField"><span class="custom-control-label"> 添加名称字段</span>
							</label>
						</div>
					</div>
					<div class="form-group row pt-0">
						<label class="col-sm-3 col-form-label text-sm-right"></label>
						<div class="col-sm-7">
							<label class="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
								<input class="custom-control-input" type="checkbox" id="isSlave">
								<span class="custom-control-label"> 这是一个明细实体 <i class="zmdi zmdi-help zicon" data-toggle="tooltip" title="通过明细实体可以更好的组织业务关系。例如订单明细通常依附于订单，而非独立存在"></i></span>
							</label>
						</div>
					</div>
					<div class="form-group row J_masterEntity hide">
						<label class="col-sm-3 col-form-label text-sm-right">选择主实体</label>
						<div class="col-sm-7">
							<select class="form-control form-control-sm" id="masterEntity">
							</select>
						</div>
					</div>
					<div class="form-group row footer">
						<div class="col-sm-7 offset-sm-3">
							<button class="btn btn-primary" type="button" data-loading-text="请稍后">确定</button>
							<a class="btn btn-link" onclick="parent.RbModal.hide()">取消</a>
						</div>
					</div>
				</div>
			</div>
			<div class="tab-pane" id="IMPORTS">
				<div id="metaschemas">
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/babel" src="${baseUrl}/assets/js/entityhub/entity-new.jsx"></script>
</body>
</html>
