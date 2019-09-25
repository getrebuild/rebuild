<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>视图配置</title>
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
		<button class="btn btn-primary J_save" type="button">保存</button>
		<button class="btn btn-secondary" onclick="parent.RbModal.hide()" type="button">取消</button>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/sortable.js"></script>
<script type="text/babel">
$(document).ready(function(){
	const entity = $urlp('entity'), type = $urlp('type')
	const _url = rb.baseUrl + '/admin/entity/' + entity + '/view-addons?type=' + type
	
	$.get(_url, function(res){
		$(res.data.refs).each(function(){ render_unset(this) })
		$(res.data.config).each(function(){
			$('.unset-list li[data-key="' + this + '"]').trigger('click')
		})
		if (!res.data.refs || res.data.refs.length == 0) $('<li class="dd-item nodata">无可用相关项</li>').appendTo('.unset-list')
	})
	
	let _btn = $('.J_save').click(function(){
		let config = []
		$('.J_config>li').each(function(){
			config.push($(this).data('key'))
		})
		
		_btn.button('loading')
		$.post(_url, JSON.stringify(config), function(res){
			_btn.button('reset')
			if (res.error_code == 0) parent.location.reload()
		})
	})
})
</script>
</body>
</html>
