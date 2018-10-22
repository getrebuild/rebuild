<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>相关项</title>
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
		<button class="btn btn-secondary" onclick="parent.RbViewPage.hideModal()" type="button">取消</button>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/sortable.js"></script>
<script type="text/javascript">
$(document).ready(function(){
	const entity = $urlp('entity')
	$.get(rb.baseUrl + '/app/' + entity + '/viewtab-settings', function(res){
		$(res.data.refs).each(function(){ render_unset(this) })
		$(res.data.config).each(function(){
			$('.unset-list li[data-key="' + this + '"]').trigger('click')
		})
	});
	
	$('.J_save').click(function(){
		let config = [];
		$('.J_config>li').each(function(){
			let _this = $(this)
			config.push(_this.data('key'))
		});
		
		let btn = $(this).button('loading')
		$.post(rb.baseUrl + '/app/' + entity + '/viewtab-settings', JSON.stringify(config), function(res){
			btn.button('reset')
			if (res.error_code == 0) parent.location.reload()
		});
	});
});

</script>
</body>
</html>
