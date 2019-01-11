<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>新建实体</title>
</head>
<body class="dialog">
<div class="main-content">
	<form>
		<div class="form-group row">
			<label class="col-sm-3 col-form-label text-sm-right">实体名称</label>
			<div class="col-sm-7">
				<input class="form-control form-control-sm" type="text" id="entityLabel" maxlength="40">
			</div>
		</div>
		<div class="form-group row">
			<label class="col-sm-3 col-form-label text-sm-right">备注</label>
			<div class="col-sm-7">
				<textarea class="form-control form-control-sm row2x" id="comments" maxlength="100" placeholder="可选"></textarea>
			</div>
		</div>
		<div class="form-group row pt-2">
			<label class="col-sm-3 col-form-label text-sm-right"></label>
			<div class="col-sm-7">
				<label class="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
					<input class="custom-control-input" type="checkbox" id="nameField"><span class="custom-control-label"> 添加名称字段（推荐）</span>
				</label>
			</div>
		</div>
		<div class="form-group row pt-1">
			<label class="col-sm-3 col-form-label text-sm-right"></label>
			<div class="col-sm-7">
				<label class="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
					<input class="custom-control-input" type="checkbox" id="isSlave">
					<span class="custom-control-label"> 这是一个明细实体 <i class="zmdi zmdi-help zicon float-right ml-1" data-toggle="tooltip" title="通过明细实体可以更好的组织业务关系。例如订单明细通常依附于订单，而非独立存在"></i></span>
				</label>
			</div>
		</div>
		<div class="form-group row J_masterEntity hide">
			<label class="col-sm-3 col-form-label text-sm-right">选择主实体</label>
			<div class="col-sm-7">
				<select class="form-control form-control-sm" id="masterEntity">
				</select>
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
	let sbtn = $('.btn-primary').click(function(){
		let entityLabel = $val('#entityLabel'),
			comments = $val('#comments')
		if (!entityLabel){ rb.highbar('请输入实体名称'); return }
		let _data = { label: entityLabel, comments: comments }
		if ($val('#isSlave') == 'true'){
			_data.masterEntity = $val('#masterEntity')
			if (!_data.masterEntity){ rb.highbar('请选择选择主实体'); return }
		}
		_data = JSON.stringify(_data)
		
		sbtn.button('loading')
		$.post(rb.baseUrl + '/admin/entity/entity-new?nameField=' + $val('#nameField'), _data, function(res){
			if (res.error_code == 0) parent.location.href = rb.baseUrl + '/admin/entity/' +res.data + '/base'
			else rb.hberror(res.error_msg)
			sbtn.button('reset')
		})
	})
	
	let entitiesLoaded = false
	$('#isSlave').click(function(){
		$('.J_masterEntity').toggleClass('hide')
		parent.rb.modalResize()
		if (entitiesLoaded == false) {
			entitiesLoaded = true
			$.get(rb.baseUrl + '/commons/metadata/entities', function(res){
				$(res.data).each(function(){
					$('<option value="' + this.name + '">' + this.label + '</option>').appendTo('#masterEntity')
				})
			})
		}
	})
})
</script>
</body>
</html>
