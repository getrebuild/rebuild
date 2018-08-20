<%@page import="cn.devezhao.rebuild.server.service.entitymanage.DisplayType"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>用户管理</title>
<style type="text/css">
.footer{padding-bottom:0 !important;}
</style>
</head>
<body class="dialog">
<div class="main-content">
	<form>
		<div class="form-group row">
			<label class="col-12 col-sm-3 col-form-label text-sm-right">字段名称</label>
			<div class="col-12 col-sm-8 col-lg-6">
				<input class="form-control form-control-sm" type="text" id="fieldLabel">
			</div>
		</div>
		<div class="form-group row">
			<label class="col-12 col-sm-3 col-form-label text-sm-right">类型</label>
			<div class="col-12 col-sm-8 col-lg-6">
				<select class="form-control form-control-sm" id="type">
					<% for (DisplayType dt : DisplayType.values()) {
						if (dt != DisplayType.ID) {
					%>
					<option value="<%=dt.name()%>"><%=dt.getDisplayName() %></option>
					<% } } %>
				</select>
			</div>
		</div>
		<div class="form-group row hide J_dt-REFERENCE">
			<label class="col-12 col-sm-3 col-form-label text-sm-right">选择引用实体</label>
			<div class="col-12 col-sm-8 col-lg-6">
				<select class="form-control form-control-sm" id="refEntity">
				</select>
			</div>
		</div>
		<div class="form-group row">
			<label class="col-12 col-sm-3 col-form-label text-sm-right">备注</label>
			<div class="col-12 col-sm-8 col-lg-4">
				<textarea class="form-control form-control-sm row2" id="comments" maxlength="100" placeholder="可选"></textarea>
			</div>
		</div>
		<div class="form-group row footer">
			<label class="col-12 col-sm-3 col-form-label text-sm-right"></label>
			<div class="col-12 col-sm-8 col-lg-6">
            	<button class="btn btn-primary btn-space" type="button">确定</button>
			</div>
		</div>
	</form>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
$(document).ready(function(){
	const entity = $urlp('entity');
	let btn = $('.btn-primary').click(function(){
		let fieldLabel = $val('#fieldLabel'),
			type = $val('#type'),
			comments = $val('#comments'),
			refEntity = $val('#refEntity');
		if (!fieldLabel){
			alert('请输入字段名称'); return;
		}
		if (type == 'REFERENCE' && !refEntity){
			alert('请选择引用实体'); return;
		}
		
		let _data = { entity:entity, label:fieldLabel, type:type, comments:comments, refEntity:refEntity };
		btn.button('loading');
		$.post('field-new', _data, function(res){
			if (res.error_code == 0) parent.location.href = entity + '/field/' + res.data;
			else{
				alert(res.error_msg);
				btn.button('reset');
			}
		});
	});
	
	let referenceLoaded = false;
	$('#type').change(function(){
		if ($(this).val() == 'REFERENCE'){
			$('.J_dt-REFERENCE').removeClass('hide');
			if (referenceLoaded == false){
				referenceLoaded = true;
				$.get('list-entity', function(res){
					$(res.data).each(function(){
						$('<option value="' + this.entityName + '">' + this.entityLabel + '</option>').appendTo('#refEntity');
					})
				});
			}
		}else{
			$('.J_dt-REFERENCE').addClass('hide');
		}
	});
	
});
</script>
</body>
</html>
