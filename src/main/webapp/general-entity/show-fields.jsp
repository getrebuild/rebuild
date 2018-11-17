<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>列显示</title>
</head>
<body class="dialog">
<div class="main-content">
	<div class="row m-0">
		<div class="col-6 sortable-swap">
			<h5 class="sortable-box-title">已显示</h5>
			<div class="sortable-box rb-scroller">
				<ol class="dd-list J_config"></ol>
			</div>
			<i class="zmdi zmdi-swap"></i>
		</div>
		<div class="col-6">
			<h5 class="sortable-box-title">未显示</h5>
			<div class="sortable-box rb-scroller">
				<ol class="dd-list unset-list"></ol>
			</div>
		</div>
	</div>
	<div class="dialog-footer">
		<div class="float-left hide J_for-admin">
			<label class="custom-control custom-checkbox custom-control-inline">
				<input class="custom-control-input" type="checkbox" id="applyTo" value="ALL" checked="checked">
				<span class="custom-control-label">应用到全部用户</span>
			</label>
		</div>
		<button class="btn btn-primary J_save" type="button">保存</button>
		<button class="btn btn-secondary" onclick="parent.rb.modalHide()" type="button">取消</button>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/sortable.js"></script>
<script type="text/babel">
$(document).ready(function(){
	const entity = $urlp('entity')
	let cfgid = null
	$.get(rb.baseUrl + '/app/' + entity + '/list-fields', function(res){
		$(res.data['fieldList']).each(function(){ render_unset([this.field, this.label]) })
		$(res.data['configList']).each(function(){ $('.unset-list li[data-key="' + this.field + '"]').trigger('click') })
		cfgid = res.data['configId'] || ''
	});
	
	$('.J_save').click(function(){
		let config = [];
		$('.J_config>li').each(function(){
			let _this = $(this)
			config.push({ field: _this.data('key') })
		});
		if (config.length == 0){ rb.notice('请至少设置 1 个显示列'); return }
		
		let btn = $(this).button('loading')
		let url = rb.baseUrl + '/app/' + entity + '/list-fields?cfgid=' + cfgid + '&toAll=' + $('#applyTo').prop('checked')
		$.post(url, JSON.stringify(config), function(res){
			if (res.error_code == 0) parent.location.reload()
			btn.button('reset')
		})
	})
})
</script>
</body>
</html>
