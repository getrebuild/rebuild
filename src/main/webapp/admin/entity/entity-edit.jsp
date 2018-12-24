<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
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
							<li class="active"><a href="base">基本信息</a></li>
							<li><a href="fields">管理字段</a></li>
							<li><a href="form-design">设计布局</a></li>
							<li><a href="advanced">高级配置</a></li>
						</ul>
					</div>
				</div>
			</div>
        </aside>
		<div class="page-head">
			<div class="page-head-title">基本信息</div>
		</div>
		<div class="main-content container-fluid pt-1">
			<c:if test="${slaveEntity != null}">
			<ul class="nav nav-tabs nav-tabs-classic">
				<li class="nav-item J_tab-${masterEntity}"><a href="../${masterEntity}/base" class="nav-link">主实体</a></li>
				<li class="nav-item J_tab-${slaveEntity}"><a href="../${slaveEntity}/base" class="nav-link">明细实体</a></li>
			</ul>
			</c:if>
			<div class="card mb-0">
				<div class="card-body pt-4">
					<form>
						<div class="form-group row">
							<label class="col-sm-2 col-form-label text-sm-right">图标</label>
							<div class="col-lg-5 col-sm-10">
								<a id="entityIcon" data-o="${icon}" title="更换图标"><i class="icon zmdi zmdi-${icon}"></i></a>
							</div>
						</div>
						<div class="form-group row">
							<label class="col-sm-2 col-form-label text-sm-right">实体名称</label>
							<div class="col-lg-5 col-sm-10">
								<input class="form-control form-control-sm" type="text" id="entityLabel" value="${entityLabel}" data-o="${entityLabel}">
							</div>
						</div>
						<div class="form-group row">
							<label class="col-sm-2 col-form-label text-sm-right">内部标识</label>
							<div class="col-lg-5 col-sm-10">
								<input class="form-control form-control-sm" type="text" readonly="readonly" id="entityName" value="${entityName}">
							</div>
						</div>
						<div class="form-group row">
							<label class="col-sm-2 col-form-label text-sm-right">名称字段</label>
							<div class="col-lg-5 col-sm-10">
								<select class="form-control form-control-sm" id="nameField" data-o="${nameField}">
								</select>
								<p class="form-text mb-0">名称字段应能清晰的标识记录本身，如客户中的客户名称或订单中的订单编号</p>
							</div>
						</div>
						<div class="form-group row">
							<label class="col-sm-2 col-form-label text-sm-right">备注</label>
							<div class="col-lg-5 col-sm-10">
								<textarea class="form-control form-control-sm row2x" id="comments" data-o="${comments}">${comments}</textarea>
							</div>
						</div>
						<div class="form-group row footer">
							<div class="col-lg-5 col-sm-10 offset-sm-2">
								<div class="J_action hide">
									<button class="btn btn-primary J_save" type="button" data-loading-text="请稍后">保存</button>
								</div>
								<div class="alert alert-warning alert-icon mb-0 hide">
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
<script type="text/babel">
let clickIcon = function(icon){
	$('#entityIcon').attr('value', icon).find('i').attr('class', 'icon zmdi zmdi-' + icon)
	rb.modalHide()
}
$(document).ready(function(){
	const metaId = '${entityMetaId}'
	if (!!!metaId) $('.footer .alert').removeClass('hide')
	else $('.footer .J_action').removeClass('hide')

	$('.J_tab-${entityName} a').addClass('active')
	
	let _btn = $('.J_save').click(function(){
		if (!!!metaId) return
		let icon = $val('#entityIcon'),
			label = $val('#entityLabel'),
			comments = $val('#comments'),
			nameField = $val('#nameField')
		let _data = { entityLabel:label, comments:comments, nameField:nameField }
		if (!!icon) _data.icon = icon
		_data = $cleanMap(_data)
		if (Object.keys(_data) == 0){ location.reload(); return }
		
		_data.metadata = { entity:'MetaEntity', id:metaId }
		_btn.button('loading')
		$.post('../entity-update', JSON.stringify(_data), function(res){
			if (res.error_code == 0) location.reload()
			else rb.notice(res.error_msg, 'danger')
		})
	})
	
	$('#entityIcon').click(function(){
		rb.modal(rb.baseUrl + '/p/commons/search-icon', '选择图标')
	})
	
	$.get(rb.baseUrl + '/commons/metadata/fields?entity=${entityName}', function(d){
		let rs = d.data.map((item) => {
			let unName = item.type == 'REFERENCE' || item.type == 'NTEXT'
			return {
				id: item.name,
				text: item.label,
				disabled: unName,
				title: unName ? '此字段（类型）不能作为主显字段' : ''
			}
		})
		$('#nameField').select2({
			language: 'zh-CN',
			placeholder: '选择字段',
			data: rs
		}).val('${nameField}').trigger('change')
	})
})
</script>
</body>
</html>
