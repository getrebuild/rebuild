<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>查询字段</title>
<style type="text/css">
.sortable-box{height:288px}
.sortable-box .dd-list{height:280px}
.dd-list .dd-item .dd-handle{margin:3px 0;position:relative;cursor:pointer;}
.dd-list .dd-item .zmdi{position:absolute;right:9px;top:7px;font-size:1.6rem;color:#fff;display:none;}
.J_fields .dd-item .dd-handle:hover .zmdi-plus{display:block;}
.J_config .dd-item .dd-handle:hover .zmdi-minus{display:block;}
</style>
</head>
<body class="dialog">
<div class="main-content" style="overflow:hidden;">
	<div class="row margin-0">
		<div class="col-6">
			<h5 class="sortable-box-title">已设置</h5>
			<div class="sortable-box rb-scroller">
				<ol class="dd-list J_config">
				</ol>
			</div>
		</div>
		<div class="col-6">
			<h5 class="sortable-box-title">未设置</h5>
			<div class="sortable-box rb-scroller">
				<ol class="dd-list J_fields">
				</ol>
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
		<button class="btn btn-secondary" onclick="parent.QuickFilter.hideQFieldsModal()" type="button">取消</button>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
$(document).ready(function(){
	const entity = $urlp('entity');
	
	let cfgid = null;
	$.get(rb.baseUrl + '/app/' + entity + '/advfilter/quick-fields', function(res){
		let fieldNames = {}
		$(res.data['fieldList']).each(function(){
			item_render(this, '.J_fields')
			fieldNames[this.field] = this.label
		})
		if (res.data['config'] && res.data['config'].items) {
			$(res.data['config'].items).each(function(){
				this.label = fieldNames[this.field]
				item_render(this, '.J_config')
				$('.J_fields>li[data-field=\'' + this.field + '\']').remove()
			})
		}
		cfgid = res.data['configId'] || ''
	});
	
	$('.J_save').click(function(){
		let config = [];
		$('.J_config>li').each(function(){
			let _this = $(this);
			config.push({ field: _this.data('field'), op: 'lk', value: '{1}' });
		});
		if (config.length == 0){ rb.notice('请至少设置一个查询字段'); return }
		config = { items: config }
		
		let btn = $(this).button('loading')
		$.post(rb.baseUrl + '/app/' + entity + '/advfilter/quick-fields?cfgid=' + cfgid + '&toAll=' + $('#applyTo').prop('checked'), JSON.stringify(config), function(res){
			btn.button('reset')
			if (res.error_code == 0){
				if (parent.QuickFilter) {
					parent.QuickFilter.loadFilter();
					parent.QuickFilter.hideQFieldsModal();
				}
			}
		});
	});
});
const item_render = function(data, append){
	let item = $('<li class="dd-item" data-field="' + data.field + '"><div class="dd-handle J_field">' + data.label + '<i class="zmdi zmdi-plus"></i><i class="zmdi zmdi-minus"></i></div></li>').appendTo(append);
	item.click(function(){
		let _this = $(this)
		let clone = _this.clone(true)
		if (_this.parent().hasClass('J_fields')){
			if ($('.J_config>li').length >= 5) {
				rb.notice('最多设置5个查询字段');
				return;
			}
			clone.appendTo('.J_config');
		} else {
			clone.appendTo('.J_fields');
		}
		_this.remove()
	});
};
</script>
</body>
</html>
