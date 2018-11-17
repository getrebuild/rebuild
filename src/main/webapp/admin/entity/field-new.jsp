<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.rebuild.server.entityhub.DisplayType"%>
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
			<label class="col-sm-3 col-form-label text-sm-right">字段名称</label>
			<div class="col-sm-7">
				<input class="form-control form-control-sm" type="text" id="fieldLabel">
			</div>
		</div>
		<div class="form-group row">
			<label class="col-sm-3 col-form-label text-sm-right">字段类型</label>
			<div class="col-sm-7">
				<select class="form-control form-control-sm" id="type">
					<option value="NUMBER">整数</option>
					<option value="DECIMAL">货币</option>
					<option value="DATE">日期</option>
					<option value="DATETIME">日期时间</option>
					<option value="TEXT">文本</option>
					<option value="NTEXT">超大文本</option>
					<option value="PHONE">电话</option>
					<option value="EMAIL">邮箱</option>
					<option value="URL">链接</option>
					<option value="PICKLIST">列表</option>
					<option value="IMAGE">图片</option>
					<option value="FILE">文件</option>
					<option value="REFERENCE">引用</option>
				</select>
			</div>
		</div>
		<div class="form-group row hide J_dt-REFERENCE">
			<label class="col-sm-3 col-form-label text-sm-right">选择引用实体</label>
			<div class="col-sm-7">
				<select class="form-control form-control-sm" id="refEntity">
				</select>
			</div>
		</div>
		<div class="form-group row">
			<label class="col-sm-3 col-form-label text-sm-right">备注</label>
			<div class="col-sm-7">
				<textarea class="form-control form-control-sm row2x" id="comments" maxlength="100" placeholder="可选"></textarea>
			</div>
		</div>
		<div class="form-group row footer">
			<div class="col-sm-7 offset-sm-3">
            	<button class="btn btn-primary" type="button" data-loading-text="请稍后">确定</button>
            	<a class="btn btn-link" onclick="parent.rb.modalHide()">取消</a>
			</div>
		</div>
	</form>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/babel">
$(document).ready(function(){
	const entity = $urlp('entity');
	let btn = $('.btn-primary').click(function(){
		let fieldLabel = $val('#fieldLabel'),
			type = $val('#type'),
			comments = $val('#comments'),
			refEntity = $val('#refEntity');
		if (!fieldLabel){
			rb.notice('请输入字段名称'); return;
		}
		if (type == 'REFERENCE' && !refEntity){
			rb.notice('请选择引用实体'); return;
		}
		
		let _data = { entity:entity, label:fieldLabel, type:type, comments:comments, refEntity:refEntity };
		_data = JSON.stringify(_data)
		
		btn.button('loading');
		$.post(rb.baseUrl + '/admin/entity/field-new', _data, function(res){
			btn.button('reset')
			if (res.error_code == 0) parent.location.href = rb.baseUrl + '/admin/entity/' + entity + '/field/' + res.data;
			else rb.notice(res.error_msg, 'danger')
		});
	});
	
	let referenceLoaded = false;
	$('#type').change(function(){
		parent.rb.modalResize()
		if ($(this).val() == 'REFERENCE'){
			$('.J_dt-REFERENCE').removeClass('hide');
			if (referenceLoaded == false){
				referenceLoaded = true;
				$.get(rb.baseUrl + '/admin/entity/entity-list', function(res){
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
