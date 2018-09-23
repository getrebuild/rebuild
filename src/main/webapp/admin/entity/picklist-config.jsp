<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="cn.devezhao.rebuild.server.service.entitymanage.DisplayType"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>列表选项</title>
<style type="text/css">
.sortable-box{height:268px}
.sortable-box .dd-list{height:260px}
.dd-item.default .dd3-content{background-color:#5a95f5 !important;border-color:#5a95f5;color:#fff}
.dd-item.default .dd3-action a{color:#fff !important}
.J_showbox .with-hide,.J_hidebox .with-show,.J_showbox .default .J_default{display:none !important;}
</style>
</head>
<body class="dialog">
<div class="main-content">
	<div class="row" style="margin:0">
		<div class="col-6">
			<h5 class="sortable-box-title">列表选项</h5>
			<div class="sortable-box rb-scroller">
				<ol class="dd-list J_showbox">
				</ol>
			</div>
			<form>
				<div class="input-group input-group-sm">
					<input class="form-control J_text" type="text" maxlength="50">
					<div class="input-group-append">
						<button class="btn btn-secondary J_confirm" type="button" style="min-width:0">添加</button>
					</div>
				</div>
			</form>
		</div>
		<div class="col-6">
			<h5 class="sortable-box-title">已禁用的选项</h5>
			<div class="sortable-box rb-scroller">
				<ol class="dd-list J_hidebox">
				</ol>
			</div>
		</div>
	</div>
	<div class="dialog-footer">
		<button class="btn btn-primary J_save" type="button">保存</button>
		<button class="btn btn-secondary" onclick="parent.picklistModal.hide()" type="button">取消</button>
	</div>
</div>
<script type="text/plain" id="picklist-temp">
<li class="dd-item dd3-item">
	<div class="dd3-content text-3dot">HOLD</div>
	<div class="dd-handle dd3-handle"></div>
	<div class="dd3-action"><a href="javascript:;" class="with-show J_default" title="设为默认">[默认]</a><a href="javascript:;" class="with-show J_edit" title="修改选项">[修改]</a><a href="javascript:;" class="with-hide J_del" title="移除选项">[移除]</a></div>
</li>
</script>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
$(document).ready(function(){
	const entity = $urlp('entity'),
			field = $urlp('field');
	const query = 'entity=' + entity + '&field=' + field;
	
	$.get(rb.baseUrl + '/admin/field/picklist-gets?isAll=true&' + query, function(res){
		$(res.data).each(function(){
			itemRender(this, this.hide === true ? '.J_hidebox' : '.J_showbox')
		});
	});
	
	$('.J_save').click(function(){
		let show_items = [];
		$('.J_showbox>li').each(function(){
			let _this = $(this);
			show_items.push({ id: _this.attr('attr-id'), 'default': _this.hasClass('default'), text: _this.find('.dd3-content').text() });
		});
		let hide_items = [];
		$('.J_hidebox>li').each(function(){
			let _this = $(this);
			hide_items.push({ id: _this.attr('attr-id'), text: _this.find('.dd3-content').text() });
		});
		let _data = { show: show_items, hide: hide_items };
		
		let btn = $(this).button('loading')
		$.post(rb.baseUrl + '/admin/field/picklist-sets?' + query, JSON.stringify(_data), function(res){
			btn.button('reset')
			if (res.error_code > 0) alert(res.error_msg)
			else parent.location.reload();
		});
	})
	
	$('.J_confirm').click(function(){
		let text = $val('.J_text');
		if (!!!text){
			rb.notice('请输入选项文本'); return;
		}
		itemRender({ id: $('.J_text').attr('attr-id'), text: text });
		$('.J_text').val('').attr('attr-id', '')
		$('.J_confirm').text('添加')
	});
	
	$('.dd-list').sortable({
		connectWith: '.dd-list',
		cursor: 'move',
		placeholder: 'dd-placeholder',
	});
});
const itemRender = function(data, append){
	data.id = data.id || new Date().getTime()
	append = append || '.J_showbox';
	let item = $(append).find("li[attr-id='" + data.id + "']")
	if (item.length == 0) item = $($('#picklist-temp').html()).appendTo(append);
	
	item.find('.dd3-content').text(data.text)
	item.attr('attr-id', data.id);
	item.find('.dd3-action .J_edit').off('click').click(function(){
		$('.J_text').val(data.text).attr('attr-id', data.id)
		$('.J_confirm').text('修改')
	});
	item.find('.dd3-action .J_default').off('click').click(function(){
		$('.J_showbox li').removeClass('default')
		$(this).parent().parent().addClass('default')
	});
	item.find('.dd3-action .J_del').off('click').click(function(){
		$(this).parent().parent().remove()
	});
	if (data['default'] === true && append == '.J_showbox') item.find('.dd3-action .J_default').trigger('click')
};
</script>
</body>
</html>
