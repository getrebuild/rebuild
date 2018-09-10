<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="cn.devezhao.rebuild.server.service.entitymanage.DisplayType"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>列表选项</title>
<style type="text/css">
.border-box{border:1px solid #eee;padding:0 3px;position:relative;overflow:hidden;height:257px;}
.dd-list{min-height:250px}
.dd-list .dd-item .dd3-content, .dd-list .dd3-item .dd3-content{margin:3px 0}
.border-box .dd-item .dd-handle{background-color:#eee;border-color:#eee}
.dd-list .dd3-item .dd3-handle::before{color:#999}
.dd-list .dd3-item .dd3-handle:hover::before{color:#fff}
.dd-list .dd3-item .dd3-content{}
.dd-list .dd3-item .dd3-action{position:absolute;right:1px;top:1px;}
.dd-list .dd3-item .dd3-action>a{font-size:0.9rem;display:inline-block;line-height:34px;width:38px;display:none;text-align:center;}
.dd-list .dd3-item:hover .dd3-action>a{display:inline-block;}
.item-option{padding:0;}
.J_showbox .with-hide,.J_hidebox .with-show,.J_showbox .default .J_default{display:none !important;}
.border-box .dd-item.default .dd3-content{background-color:#dedede}
.sortable-placeholder{border:1px dotted #eee;height:36px;margin:3px 0;background-color:#fffa90}
</style>
</head>
<body class="dialog">
<div class="main-content">
	<div class="row" style="margin:0">
		<div class="col-6">
			<h5>已显示</h5>
			<div class="border-box rb-scroller1">
				<ol class="dd-list J_showbox">
				</ol>
			</div>
			<form style="margin-top:9px">
				<div class="input-group input-group-sm">
					<input class="form-control J_text" type="text" maxlength="50">
					<div class="input-group-append">
						<button class="btn btn-secondary J_confirm" type="button" style="min-width:0">确定</button>
					</div>
				</div>
			</form>
		</div>
		<div class="col-6">
			<h5>未显示</h5>
			<div class="border-box rb-scroller1">
				<ol class="dd-list J_hidebox">
				</ol>
			</div>
		</div>
	</div>
	<div class="dialog-footer">
		<button class="btn btn-primary J_save" type="button">保存</button>
		<button class="btn btn-secondary" onclick="parent.rbModal.hide()" type="button">取消</button>
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
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/jquery-ui.min.css">
<script src="${baseUrl}/assets/lib/jquery-ui.min.js"></script>
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
		console.log(JSON.stringify(_data))
		
		//let btn = $(this).button('loading')
		$.post(rb.baseUrl + '/admin/field/picklist-sets?' + query, JSON.stringify(_data), function(res){
			//btn.button('reset')
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
		$('.J_text').val('')
	});
	
	$('.dd-list').sortable({
		connectWith: '.dd-list',
		cursor: 'move',
		placeholder: 'sortable-placeholder',
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
	});
	item.find('.dd3-action .J_default').off('click').click(function(){
		$('.J_showbox li').removeClass('default')
		$(this).parent().parent().addClass('default')
	});
	item.find('.dd3-action .J_del').off('click').click(function(){
		$(this).parent().parent().remove()
	});
	if (data['default'] === true) item.find('.dd3-action .J_default').trigger('click')
};
</script>
</body>
</html>
