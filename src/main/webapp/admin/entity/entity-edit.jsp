<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>实体管理</title>
<style type="text/css">
a#entityIcon{display:inline-block;width:36px;height:36px;background-color:#e3e3e3;text-align:center;border-radius:2px;}
a#entityIcon .icon{font-size:26px;color:#555;line-height:36px;}
a#entityIcon:hover{opacity:0.8}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-aside rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="实体管理" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="entities" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<aside class="page-aside">
			<div class="rb-scroller-aside rb-scroller">
				<div class="aside-content">
					<div class="content">
						<div class="aside-header">
							<button class="navbar-toggle collapsed" type="button"><span class="icon zmdi zmdi-caret-down"></span></button>
							<span class="title">${entityLabel}</span>
							<p class="description">${comments}</p>
						</div>
					</div>
					<div class="aside-nav collapse">
						<ul class="nav">
							<li class="active"><a href="base"><i class="icon mdi mdi-inbox"></i>基本信息</a></li>
							<li><a href="fields"><i class="icon mdi mdi-inbox"></i>管理字段</a></li>
							<li><a href="form-design"><i class="icon mdi mdi-inbox"></i>配置布局</a></li>
						</ul>
					</div>
				</div>
			</div>
        </aside>
		<div class="page-head">
			<div class="page-head-title">基本信息</div>
		</div>
		<div class="main-content container-fluid" style="padding-top:3px">
			<div class="card">
				<div class="card-body">
					<form>
						<div class="form-group row">
							<label class="col-sm-2 col-form-label text-sm-right">图标</label>
							<div class="col-lg-4 col-sm-10">
								<a id="entityIcon" data-o="${icon}" title="更换图标"><i class="icon zmdi zmdi-${icon}"></i></a>
							</div>
						</div>
						<div class="form-group row">
							<label class="col-sm-2 col-form-label text-sm-right">实体名称</label>
							<div class="col-lg-4 col-sm-10">
								<input class="form-control form-control-sm" type="text" id="entityLabel" value="${entityLabel}" data-o="${entityLabel}">
							</div>
						</div>
						<div class="form-group row">
							<label class="col-sm-2 col-form-label text-sm-right">内部标识</label>
							<div class="col-lg-4 col-sm-10">
								<input class="form-control form-control-sm" type="text" readonly="readonly" id="entityName" value="${entityName}">
							</div>
						</div>
						<div class="form-group row">
							<label class="col-sm-2 col-form-label text-sm-right">主显示字段</label>
							<div class="col-lg-4 col-sm-10">
								<select class="form-control form-control-sm" id="nameField">
								</select>
							</div>
						</div>
						<div class="form-group row">
							<label class="col-sm-2 col-form-label text-sm-right">备注</label>
							<div class="col-lg-4 col-sm-10">
								<textarea class="form-control form-control-sm row2x" id="comments" data-o="${comments}">${comments}</textarea>
							</div>
						</div>
						<div class="form-group row footer">
							<div class="col-lg-4 col-sm-10 offset-sm-2">
								<button class="btn btn-primary" type="button">保存</button>
								<div class="alert alert-warning alert-icon" style="display:none">
									<div class="icon"><span class="zmdi zmdi-alert-triangle"></span></div>
									<div class="message">系统内建实体，不允许修改</div>
								</div>
							</div>
						</div>
					</form>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
clickIcon = function(icon){
	$('#entityIcon').attr('value', icon).find('i').attr('class', 'icon zmdi zmdi-' + icon);
	rb.modalHide()
};
$(document).ready(function(){
	const metaId = '${entityMetaId}';
	const btn = $('.btn-primary').click(function(){
		if (!!!metaId) return;
		let icon = $val('#entityIcon'),
			label = $val('#entityLabel'),
			comments = $val('#comments'),
			nameField = $val('#nameField');
		let _data = { icon:icon, entityLabel:label, comments:comments, nameField:nameField };
		_data = $cleanMap(_data);
		if (Object.keys(_data) == 0){
			location.reload();
			return;
		}
		
		_data.metadata = { entity:'MetaEntity', id:metaId };
		btn.button('loading');
		$.post('../entity-update', JSON.stringify(_data), function(res){
			if (res.error_code == 0) location.reload();
			else rb.notice(res.error_msg, 'danger')
		});
	});
	
	if (!!!metaId){
		btn.next().show();
		btn.remove();
	}
	
	$('#entityIcon').click(function(){
		rb.modal(rb.baseUrl + '/page/commons/search-icon', '选择图标')
	})
	
	$.get(rb.baseUrl + '/admin/entity/list-field?entity=${entityName}', function(d){
		let results = d.data.map((item) => {
			return {
				id: item.fieldName,
				text: item.fieldLabel,
			}
		});
		$('#nameField').select2({
			language: 'zh-CN',
			placeholder: '选择字段',
			allowClear: true,
			data: results
		}).val('${nameField}').trigger('change')
	})
});
</script>
</body>
</html>
