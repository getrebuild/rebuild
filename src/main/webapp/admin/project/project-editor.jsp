<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>项目管理</title>
<style type="text/css">
#members {
	min-height: 37px;
}
.plan-boxes {
	background-color: #eee;
	white-space: nowrap;
	overflow: auto;
	border-radius: 2px;
	margin: 0;
}
.plan-boxes.card-list .card {
	padding: 0;
	width: 180px;
	display: inline-block;
	margin: 20px 0 15px 20px;
}
.plan-boxes.card-list .card .card-body {
	cursor: move;
}
.plan-boxes.card-list .card:last-child {
	margin-right: 20px;
}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="项目管理" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="projects" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="page-head">
			<div class="float-left"><div class="page-head-title">项目管理<span class="sub-title">${projectName}</span></div></div>
			<div class="float-right pt-1"></div>
			<div class="clearfix"></div>
		</div>
		<div class="main-content container-fluid pt-0">
			<div class="card mb-0">
				<div class="card-body">
					<form class="simple">
						<div class="form-group row">
							<label class="col-12 col-lg-3 col-form-label text-lg-right">项目范围</label>
							<div class="col-12 col-lg-9 pt-1">
								<label class="custom-control custom-control-sm custom-radio custom-control-inline">
									<input class="custom-control-input" type="radio" name="scope" id="scope_1" value="1" checked />
									<span class="custom-control-label">公开 (所有人可见，仅成员可编辑)</span>
								</label>
								<label class="custom-control custom-control-sm custom-radio custom-control-inline">
									<input class="custom-control-input" type="radio" name="scope" id="scope_2" value="2" />
									<span class="custom-control-label">私有 (仅成员可见和编辑)</span>
								</label>
							</div>
						</div>
						<div class="form-group row pt-0">
							<label class="col-12 col-lg-3 col-form-label text-lg-right">项目成员</label>
							<div class="col-12 col-lg-9">
								<div id="members"></div>
							</div>
						</div>
						<div class="form-group row">
							<label class="col-12 col-lg-3 col-form-label text-lg-right">任务面板</label>
							<div class="col-12 col-lg-9">
								<div class="plan-boxes card-list mb-2" id="plans">
									<p class="text-muted m-3">加载中...</p>
								</div>
								<button class="btn btn-secondary btn-sm J_add-plan" type="button"><i class="zmdi zmdi-plus"></i> 添加面板</button>
							</div>
						</div>
						<div class="form-group row footer">
							<label class="col-12 col-lg-3 col-form-label text-lg-right">
							</label>
							<div class="col-12 col-lg-9">
								<button class="btn btn-primary J_save" type="button">保存</button>
							</div>
						</div>
					</form>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = {
	id: '${projectId}',
	scope: ${scope},
	members: '${members}',
}
</script>
<script src="${baseUrl}/assets/js/project/project-editor.jsx" type="text/babel"></script>
</body>
</html>
