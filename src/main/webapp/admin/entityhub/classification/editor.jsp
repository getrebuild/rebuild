<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>分类数据编辑</title>
<style type="text/css">
.level-box .col-md-3>ol {
	border:2px solid #eee;
	padding: 0 5px;
	margin-bottom: 15px;
}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="审计日志" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="classifications" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="page-head">
			<div class="page-head-title">分类数据编辑</div>
		</div>
		<div class="main-content container-fluid pt-1">
			<div class="card">
				<div class="card-body">
					<div class="row level-box">
						<div class="col-md-3">
							<h5>一级分类</h5>
							<ol class="dd-list unset-list">
							</ol>
							<form>
								<div class="input-group input-group-sm">
									<input class="form-control J_name" type="text" maxlength="40">
									<div class="input-group-append"><button class="btn btn-secondary J_confirm" type="submit">添加</button></div>
								</div>
							</form>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/sortable.js"></script>
<script src="${baseUrl}/assets/js/entity/classification-editor.jsx" type="text/babel"></script>
</body>
</html>
