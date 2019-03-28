<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>分类数据编辑</title>
<style type="text/css">
.level-boxes .col-md-3{margin-top:15px}
.level-boxes .col-md-3 ol{background-color:#eee;padding:1px 5px;border-radius:3px}
.level-boxes .col-md-3 h5{margin-top:3px}
.level-boxes .col-md-3.off>form,.level-boxes .col-md-3.off>ol{display:none;}
.level-boxes .col-md-3:first-child .turn-on{display:none;}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="分类数据编辑" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="classifications" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<div class="page-head">
			<div class="float-left"><div class="page-head-title">分类数据编辑</div></div>
			<div class="float-right pt-1">
				<button class="btn btn-primary J_save" type="button">保存</button>
			</div>
			<div class="clearfix"></div>
		</div>
		<div class="main-content container-fluid pt-1">
			<div class="card">
				<div class="card-body">
					<div class="row level-boxes">
						<div class="col-md-3">
							<div class="float-left"><h5>一级分类</h5></div>
							<div class="float-right turn-on">
								<div class="switch-button switch-button-xs">
	                          		<input type="checkbox" id="trunOn1">
	                          		<span><label for="trunOn1" title="启用/禁用"></label></span>
	                        	</div>
							</div>
							<div class="clearfix"></div>
							<form class="mt-1">
								<div class="input-group input-group-sm">
									<input class="form-control J_name" type="text" maxlength="50">
									<div class="input-group-append"><button class="btn btn-secondary J_confirm" type="submit">添加</button></div>
								</div>
							</form>
							<ol class="dd-list unset-list mt-3"></ol>
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
