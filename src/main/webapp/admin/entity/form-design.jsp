<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>配置布局</title>
<style type="text/css">
.form-preview{margin:0 auto}
.dd-list .dd-item .dd-handle{cursor:move}
.dd-list .dd-item .dd-action{float:right;cursor:default}
.dd-list .dd-item .dd-action i{font-size:12px;color:#999;font-style:normal;background-color:#eee;color:#999;display:inline-block;padding:2px 3px;line-height:1;border-radius:2px;margin-left:2px}
.nodata{padding:14px 0;color:#999;border:1px dotted #dedede;text-align:center;display:none;margin:5px 0;}
.dd-list{min-height:200px}
.readonly{border-left:3px solid #dedede !important}
.not-nullable{border-left:3px solid #ea4335 !important}
.card-header>div>span{font-size:12px;font-weight:normal;padding-left:4px;margin-right:9px;color:#888}
.dd-placeholder{height:36px}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-aside">
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
							<li><a href="base"><i class="icon mdi mdi-inbox"></i>基本信息</a></li>
							<li><a href="fields"><i class="icon mdi mdi-inbox"></i>管理字段</a></li>
							<li class="active"><a href="form-design"><i class="icon mdi mdi-inbox"></i>配置布局</a></li>
						</ul>
					</div>
				</div>
			</div>
        </aside>
		<div class="main-content container-fluid">
			<div class="row">
				<div class="col-12 col-sm-8">
					<div class="float-right"><button class="btn btn-primary" type="button">保存配置</button></div>
					<div class="tab-container">
                		<ul class="nav nav-tabs nav-tabs-classic">
							<li class="nav-item"><a class="nav-link active" href="form-design">表单布局</a></li>
							<li class="nav-item"><a class="nav-link" href="view-design">视图布局</a></li>
						</ul>
		                <div class="tab-content">
							<div class="tab-pane active">
								<div class="form-preview dd-list connectedSortable">
									<div class="nodata">拖动右侧字段到这里</div>
								</div>
							</div>
						</div>
					</div>
				</div>
				<div class="col-12 col-sm-4">
					<div class="card">
						<div class="card-header">
							字段列表
							<div class="float-right hide"><a href="javascript:;" style="font-size:1rem;font-weight:normal;">+ 布局所有必填字段</a></div>
							<div class="float-right"><span class="not-nullable">必填</span><span class="readonly">只读</span></div>
						</div>
						<div class="card-body" style="padding-top:6px">
							<div class="field-list dd-list connectedSortable">
								<div class="nodata">全部字段已布局</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
$(document).ready(function(){
	const config = JSON.parse('${FormConfig}' || '[]');
	$.get('../list-field?entity=${entityName}', function(res){
		let validFields = {},
			configFields = [];
		$(config.elements).each(function(){
			configFields.push(this.field);
		});
		$(res.data).each(function(){
			validFields[this.fieldName] = this;
			if (configFields.contains(this.fieldName) == false){
				render_item(this, '.field-list');
			}
		});
		
		$(config.elements).each(function(){
			let field = validFields[this.field]
			if (!!!field){
				$('<div class="dd-handle J_field">字段 [' + this.field.toUpperCase() + '] 已删除</div>').appendTo(item);
			} else {
				render_item(field, '.form-preview');
			}
		});
		
	    checkEmpty()
	    $('.form-preview, .field-list').sortable({
			connectWith: '.connectedSortable',
			cursor: 'move',
			placeholder: 'dd-placeholder',
			cancel: '.nodata',
			stop: checkEmpty
		}).disableSelection();
	});
	
	let btn = $('.btn-primary').click(function(){
		let elements = [];
		$('.form-preview .J_field').each(function(){
			elements.push({ field:$(this).data('field') });
		});
		
		let _data = { belongEntity:'${entityName}', type:'FORM', config:JSON.stringify(elements) };
		_data.metadata = { entity:'LayoutConfig', id:config.id || null };
		btn.button('loading');
		$.post('form-update', JSON.stringify(_data), function(res){
			if (res.error_code == 0) location.reload();
			else alert(res.error_msg)
		});
	});
});
const render_item = function(data, append) {
	let item = $('<div class="dd-item"></div>').appendTo(append)
	item = $('<div class="dd-handle J_field" data-field="' + data.fieldName + '">' + data.fieldLabel + '</div>').appendTo(item)
	if (data.builtin == true) item.addClass('readonly')
	else if (data.nullable == false) item.addClass('not-nullable')
	item = $('<div class="dd-action"></div>').appendTo(item)
	$('<i>' + data.displayType.split('(')[0].trim() + '</i>').appendTo(item)
};
const checkEmpty = function(){
	if ($('.field-list .dd-item').length == 0){
		$('.field-list .nodata').show()
	}else{
		$('.field-list .nodata').hide()
	}
	if ($('.form-preview .dd-item').length == 0){
		$('.form-preview .nodata').show()
	}else{
		$('.form-preview .nodata').hide()
	}
}
</script>
</body>
</html>
