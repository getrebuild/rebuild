<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>快速查询字段</title>
</head>
<body class="dialog">
<div class="main-content">
	<div class="row m-0">
		<div class="col-6 sortable-swap">
			<h5 class="sortable-box-title">查询字段</h5>
			<div class="sortable-box rb-scroller">
				<ol class="dd-list J_config"></ol>
			</div>
			<i class="zmdi zmdi-swap"></i>
		</div>
		<div class="col-6">
			<h5 class="sortable-box-title">未设置</h5>
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
		<button class="btn btn-secondary" onclick="parent.QuickFilter.hideModal()" type="button">取消</button>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/sortable.js"></script>
<script type="text/javascript">
$(document).ready(function(){
	const entity = $urlp('entity')
	let cfgid = null
	$.get(rb.baseUrl + '/app/' + entity + '/advfilter/quick-fields', function(res){
		$(res.data['fieldList']).each(function(){ render_unset([this.field, this.label]) })
		if (res.data['config'] && res.data['config'].items) {
			$(res.data['config'].items).each(function(){
				$('.unset-list li[data-key="' + this.field + '"]').trigger('click')
			})
		}
		cfgid = res.data['configId'] || ''
	})
	
	$('.J_save').click(function(){
		let config = []
		$('.J_config>li').each(function(){
			let _this = $(this);
			config.push({ field: _this.data('key'), op: 'lk', value: '{1}' });
		});
		if (config.length == 0){ rb.notice('请至少设置1个查询字段'); return }
		if (config.length > 5){ rb.notice('最多设置5个查询字段'); return }
		config = { items: config }
		
		let btn = $(this).button('loading')
		let url = rb.baseUrl + '/app/' + entity + '/advfilter/quick-fields?cfgid=' + cfgid + '&toAll=' + $('#applyTo').prop('checked')
		$.post(url, JSON.stringify(config), function(res){
			if (res.error_code == 0) parent.location.reload()
			btn.button('reset')
		});
	});
})
</script>
</body>
</html>
