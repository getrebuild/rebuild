<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>配置列显示</title>
<style type="text/css">
.border-box{border:1px solid #eee;padding:0 3px;height:388px;z-index:98}
.border-box .dd-list{min-height:380px;}
.border-box .dd-list .dd-item .dd3-content, .dd-list .dd3-item .dd3-content{margin:3px 0}
.border-box .dd-item .dd-handle{background-color:#eee;border-color:#eee}
.border-box .dd-list .dd3-item .dd3-handle::before{color:#999}
.border-box .dd-list .dd3-item .dd3-handle:hover::before{color:#fff}
.sortable-placeholder{border:1px dotted #eee;height:36px;margin:3px 0;background-color:#fffa90}
</style>
</head>
<body class="dialog">
<div class="main-content">
	<div class="row" style="margin:0">
		<div class="col-6">
			<h5>已显示</h5>
			<div class="border-box rb-scroller">
				<ol class="dd-list J_config">
				</ol>
			</div>
		</div>
		<div class="col-6">
			<h5>未显示</h5>
			<div class="border-box rb-scroller">
				<ol class="dd-list J_fields">
				</ol>
			</div>
		</div>
	</div>
	<div class="dialog-footer">
		<div class="float-left">
			<label class="custom-control custom-checkbox custom-control-inline" style="margin:0;margin-top:6px">
				<input class="custom-control-input" type="checkbox" id="applyFor" value="ALL"><span class="custom-control-label"> 应用到全部</span>
			</label>
		</div>
		<button class="btn btn-primary J_save" type="button">保存</button>
		<button class="btn btn-secondary" onclick="parent.rbModal.hide()" type="button">取消</button>
	</div>
</div>
<script type="text/plain" id="column-temp">
<li class="dd-item dd3-item">
	<div class="dd3-content text-3dot">HOLD</div>
	<div class="dd-handle dd3-handle"></div>
</li>
</script>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
$(document).ready(function(){
	const entity = $urlp('entity');
	
	let cfgid = null;
	$.get(rb.baseUrl + '/app/' + entity + '/list-columns', function(res){
		$(res.data['configList']).each(function(){ itemRender(this, '.J_config') })
		$(res.data['fieldList']).each(function(){ itemRender(this, '.J_fields') })
		cfgid = res.data['configId'] || ''
		
		$('.dd-list').sortable({
			connectWith: '.dd-list',
			cursor: 'move',
			placeholder: 'sortable-placeholder',
		});
	});
	
	$('.J_save').click(function(){
		let config = [];
		$('.J_config>li').each(function(){
			let _this = $(this);
			config.push({ field: _this.data('field') });
		});
		
		let btn = $(this).button('loading')
		$.post(rb.baseUrl + '/app/' + entity + '/list-columns?cfgid=' + cfgid, JSON.stringify(config), function(res){
			btn.button('reset')
			if (res.error_code == 0) parent.location.reload()
			else alert(res.eror_msg)
		});
	});
});
const itemRender = function(data, append){
	if ($(".dd-list li[data-field='" + data.field + "']").length > 0) return;
	let item = $($('#column-temp').html()).appendTo(append)
	item.attr('data-field', data.field)
	item.find('.dd3-content').text(data.label)
};
</script>
</body>
</html>
