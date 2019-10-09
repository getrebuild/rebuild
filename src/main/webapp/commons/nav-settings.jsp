<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>导航菜单</title>
<style type="text/css">
.dd3-content > .zmdi {
	position: absolute;
	width: 28px;
	height: 28px;
	font-size: 1.45rem;
	margin-left: -20px;
	margin-top: 1px;
}
.dd3-content {
	padding-left: 60px !important;
	cursor: default;
}
.dd-item > ul {
	margin-left: 22px;
	padding-left: 0;
	position: relative;
}
.input-group-prepend .input-group-text {
	width: 37px;
	text-align: center;
	display: inline-block;
	overflow: hidden;
	padding: 9px 0 0;
	background-color: #fff
}
.input-group-prepend .input-group-text:hover {
	background-color: #eee;
	cursor: pointer;
}
.input-group-prepend .input-group-text i.zmdi {
	font-size: 1.5rem;
}
</style>
</head>
<body class="dialog">
<div class="main-content">
	<div class="row m-0">
		<div class="col-5 mt-2 pr-0">
			<div class="sortable-box h380 rb-scroller">
				<ol class="dd-list J_config"></ol>
			</div>
			<div class="actions">
				<button type="button" class="btn btn-secondary btn-sm J_add-menu">+ 添加菜单项</button>
			</div>
		</div>
		<div class="col-7 mt-2 pl-6">
			<div class="text-muted J_edit-tips">点击左侧菜单项编辑</div>
			<div class="J_edit-menu hide" style="margin-top:-6px">
				<div class="tab-container">
					<ul class="nav nav-tabs">
						<li class="nav-item"><a class="nav-link J_menuType active" href="#ENTITY" data-toggle="tab">关联项</a></li>
						<li class="nav-item"><a class="nav-link J_menuType" href="#URL" data-toggle="tab">外部地址</a></li>
					</ul>
					<div class="tab-content m-0" style="padding:20px 0">
						<div class="tab-pane active" id="ENTITY">
							<select class="form-control form-control-sm J_menuEntity">
								<option value="">请选择关联项</option>
								<optgroup label="业务实体"></optgroup>
								<optgroup label="其他">
									<option value="$PARENT$" data-icon="menu">父级菜单</option>
								</optgroup>
							</select>
						</div>
						<div class="tab-pane" id="URL">
							<input type="text" class="form-control form-control-sm J_menuUrl" placeholder="输入 URL">
						</div>
					</div>
				</div>
				<div class="input-group" style="margin-bottom:20px">
					<span class="input-group-prepend">
						<span class="input-group-text J_menuIcon" title="选择图标"><i class="zmdi zmdi-texture"></i></span>
					</span>
					<input type="text" class="form-control form-control-sm J_menuName" placeholder="菜单名称">
				</div>
				<div>
					<button type="button" class="btn btn-primary bordered J_menuConfirm">确定</button>
				</div>
			</div>
		</div>
	</div>
	<div class="dialog-footer">	
		<div class="float-left">
			<div id="shareTo" class="shareTo--wrap"></div>
		</div>
		<button class="btn btn-primary J_save" type="button">保存</button>
		<button class="btn btn-secondary" onclick="parent.RbModal.hide()" type="button">取消</button>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/settings-share2.jsx" type="text/babel"></script>
<script src="${baseUrl}/assets/js/nav-settings.jsx" type="text/babel"></script>
</body>
</html>
