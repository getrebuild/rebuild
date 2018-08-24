<%@page import="cn.devezhao.rebuild.server.service.entitymanage.DisplayType"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>新建字段</title>
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
					<option value="NUMBER">整数</option>
					<option value="DECIMAL">货币</option>
					<option value="DATETIME">日期时间</option>
					<option value="TEXT">文本</option>
					<option value="PHONE">电话</option>
					<option value="EMAIL">邮箱</option>
					<option value="URL">链接</option>
					<option value="IMAGE">图片</option>
					<option value="FILE">文件</option>
					<!--
					<option value="LOCATION">位置</option>
					-->
					<option value="PICKLIST">列表</option>
					<option value="REFERENCE">引用</option>
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
				<textarea class="form-control form-control-sm row2x" id="comments" maxlength="100" placeholder="可选"></textarea>
			</div>
		</div>
		<div class="form-group row footer">
			<div class="col-12 col-sm-8 col-lg-6 offset-sm-3">
            	<button class="btn btn-primary" type="button" data-loading-text="请稍后">确定</button>
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
		if (parent && parent.rbModal) parent.rbModal.loaded()
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
