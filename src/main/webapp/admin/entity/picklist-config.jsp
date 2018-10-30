<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>列表选项</title>
<style type="text/css">
.unset-list .dd-handle{font-style:italic;color:#aaa}
.unset-list .dd-item a.action{position:absolute;right:24px;top:1px;font-style:normal;}
.unset-list .dd-item:hover a.action{color:#fff}
</style>
</head>
<body class="dialog">
<div class="main-content">
	<div class="row m-0">
		<div class="col-6">
			<h5 class="sortable-box-title">列表选项</h5>
			<div class="sortable-box rb-scroller">
				<ol class="dd-list J_config"></ol>
			</div>
			<form>
				<div class="input-group input-group-sm">
					<input class="form-control J_text" type="text" maxlength="50">
					<div class="input-group-append">
						<button class="btn btn-secondary J_confirm" type="submit" style="min-width:0">添加</button>
					</div>
				</div>
			</form>
		</div>
		<div class="col-6">
			<h5 class="sortable-box-title">已禁用的选项</h5>
			<div class="sortable-box rb-scroller">
				<ol class="dd-list unset-list"></ol>
			</div>
		</div>
	</div>
	<div class="dialog-footer">
		<button class="btn btn-primary J_save" type="button">保存</button>
		<button class="btn btn-secondary" onclick="parent.rb.modalHide()" type="button">取消</button>
	</div>
</div>

<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/sortable.js"></script>
<script type="text/javascript">
let tmpid = new Date().getTime()
let default_item
$(document).ready(function(){
	const entity = $urlp('entity'), field = $urlp('field')
	const query = 'entity=' + entity + '&field=' + field
	
	$.get(rb.baseUrl + '/admin/field/picklist-gets?isAll=true&' + query, function(res){
		$(res.data).each(function(){
			if (this.hide === true) render_unset([this.id, this.text])
			else{
				let item = render_item([this.id, this.text])
				if (this['default'] == true) {
					default_item = this.id
					item.addClass('active')
				}
			}
		})
	})
	
	$('.J_confirm').click(function(){
		let text = $val('.J_text');
		if (!!!text){ rb.notice('请输入选项文本'); return; }
		let id = $('.J_text').attr('attr-id')
		$('.J_text').val('').attr('attr-id', '')
		$('.J_confirm').text('添加')
		if (!!!id) render_item([tmpid++, text])
		else{
			let item = $('.J_config li[data-key="' + id + '"]')
			item.attr('data-key', id)
			item.find('.dd3-content').text(text)
		}
		return false
	})
	
	$('.J_save').click(function(){
		let show_items = [];
		$('.J_config>li').each(function(){
			let _this = $(this)
			let id = _this.attr('data-key')
			show_items.push({ id: id, 'default': id == default_item, text: _this.find('.dd3-content').text() })
		})
		let hide_items = [];
		$('.unset-list>li').each(function(){
			let _this = $(this)
			let id = _this.attr('data-key')
			hide_items.push({ id: id, text: _this.find('.dd-handle').text().replace('[删除]', '') })
		})
		let _data = { show: show_items, hide: hide_items }
		_data = JSON.stringify(_data)
		
		let btn = $(this).button('loading')
		$.post(rb.baseUrl + '/admin/field/picklist-sets?' + query, _data, function(res){
			btn.button('reset')
			if (res.error_code > 0) rb.notice(res.error_msg, 'danger')
			else parent.location.reload()
		})
	})
})
render_unset_after = function(item, data){
	let del = $('<a href="javascript:;" class="action">[删除]</a>').appendTo(item.find('.dd-handle'))
	
}
render_item_after = function(item, data){
	let edit = $('<a href="javascript:;">[修改]</a>').appendTo(item.find('.dd3-action'))
	edit.click(function(){
		$('.J_text').val(data[1]).attr('attr-id', data[0])
		$('.J_confirm').text('修改')
	})
	
	let default0 = $('<a href="javascript:;">[默认]</a>').appendTo(item.find('.dd3-action'))
	default0.click(function(){
		$('.J_config li').removeClass('active')
		default0.parent().parent().addClass('active')
		default_item = data[0]
	})
}
</script>
</body>
</html>
