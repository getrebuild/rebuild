<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>配置布局</title>
<style type="text/css">
.form-preview{max-width:630px;margin:0 auto;border:1px solid #4285f4;min-height:100px;padding:20px}
.sortable-placeholder{border:1px dotted #dedede;height:36px;}
</style>
</head>
<body>
<div class="rb-wrapper rb-collapsible-sidebar rb-fixed-sidebar rb-aside">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="实体管理" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="entity-list" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<aside class="page-aside">
			<div class="rb-scroller">
				<div class="aside-content">
					<div class="content">
						<div class="aside-header">
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
					<div class="card">
						<div class="card-header card-header-divider">
							<div class="float-left">表单预览</div>
							<div class="float-right">
								<button class="btn btn-primary btn-space" type="button">保存</button>
							</div>
							<div class="clearfix"></div>
						</div>
						<div class="card-body">
							<div class="form-preview dd-list connectedSortable">
							</div>
						</div>
					</div>
				</div>
				<div class="col-12 col-sm-4">
					<div class="card">
						<div class="card-header">字段列表</div>
						<div class="card-body">
							<div class="field-list dd-list connectedSortable">
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/jquery-ui.min.css">
<script src="${baseUrl}/assets/lib/jquery-ui.min.js"></script>
<script type="text/babel">
</script>
<script type="text/javascript">
$(document).ready(function(){
	 const config = JSON.parse('${config}');
	 
	$.get('../list-field?entity=${entityName}', function(res){
		let fieldNames = {};
		$(res.data).each(function(){
			$('<div class="dd-item"><div class="dd-handle J_field" data-field="' + this.fieldName + '">' + this.fieldLabel + '</div></div>').appendTo('.field-list');
			fieldNames[this.fieldName] = this.fieldLabel;
		});
		
	    $('.form-preview, .field-list').sortable({
			connectWith: '.connectedSortable',
			cursor: 'move',
			placeholder: 'sortable-placeholder',
		});//.disableSelection();
		
		$(config.elements).each(function(){
			$('<div class="dd-item"><div class="dd-handle J_field" data-field="' + this.field + '">' + (fieldNames[this.field] || ('[' + this.field.toUpperCase() + '] 已删除')) + '</div></div>').appendTo('.form-preview');
		});
	});
	
	let btn = $('.btn-primary').click(function(){
		let elements = [];
		$('.form-preview .J_field').each(function(){
			elements.push({ field:$(this).data('field') });
		});
		
		let _data = { entity:'${entityName}', config:JSON.stringify(elements) };
		_data.metadata = { entity:'Layout', id:config.id || null };
		btn.button('loading');
		$.post('form-update', JSON.stringify(_data), function(res){
			if (res.error_code == 0) location.reload();
			else alert(res.error_msg)
		});
	});
});
</script>
</body>
</html>
