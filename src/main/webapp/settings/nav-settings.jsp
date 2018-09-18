<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="cn.devezhao.rebuild.server.service.entitymanage.DisplayType"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>列表选项</title>
<style type="text/css">
.border-box{border:1px solid #eee;padding:0 3px;position:relative;overflow:hidden;height:367px;}
.border-box .dd-list{min-height:360px}
.border-box .dd-list .dd-item .dd3-content, .dd-list .dd3-item .dd3-content{margin:3px 0}
.border-box .dd-item .dd-handle{background-color:#eee;border-color:#eee}
.border-box .dd-list .dd3-item .dd3-handle::before{color:#999}
.border-box .dd-list .dd3-item .dd3-handle:hover::before{color:#fff}
.border-box .dd-list .dd3-item .dd3-content{}
.border-box .dd-list .dd3-item .dd3-action{position:absolute;right:1px;top:1px;}
.border-box .dd-list .dd3-item .dd3-action>a{font-size:0.9rem;display:inline-block;line-height:34px;width:38px;display:none;text-align:center;}
.border-box .dd-list .dd3-item:hover .dd3-action>a{display:inline-block;}
.item-option{padding:0;}
.J_showbox .with-hide,.J_hidebox .with-show,.J_showbox .default .J_default{display:none !important;}
.border-box .dd-item.default .dd3-content{background-color:#dedede}
.btn-group-sm>.btn{min-width:100px}
.actions{margin:9px 0}
.actions a{display:inline-block;margin-right:9px}
.actions .zmdi{font-size:1.231rem;}
</style>
</head>
<body class="dialog">
<div class="main-content">
	<div class="row margin-0">
		<div class="col-5" style="padding-top:9px">
			<div class="border-box rb-scroller">
				<ol class="dd-list J_configbox">
				</ol>
			</div>
			<div class="actions">
				<a href="javascript:;" class="J_add-menu"><i class="zmdi zmdi-plus "></i> 添加一级菜单</a>
				<a href="javascript:;" class="J_add-line"><i class="zmdi zmdi-plus"></i> 添加分割线</a>
			</div>
		</div>
		<div class="col-7">
			<h5 style="margin-top:9px" class="text-bold">菜单选项</h5>
		</div>
	</div>
	<div class="dialog-footer">
		<button class="btn btn-primary J_save" type="button">保存</button>
		<button class="btn btn-secondary" onclick="parent.rbModal.hide()" type="button">取消</button>
	</div>
</div>
<script type="text/plain" id="item-temp">
<li class="dd-item dd3-item">
	<div class="dd3-content text-3dot">HOLD</div>
	<div class="dd-handle dd3-handle"></div>
	<div class="dd3-action"><a href="javascript:;" class="with-hide J_del" title="移除菜单">[移除]</a></div>
</li>
</script>
<%@ include file="/_include/Foot.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/jquery-ui.min.css">
<script src="${baseUrl}/assets/lib/jquery-ui.min.js"></script>
<script type="text/javascript">
$(document).ready(function(){
	$('.J_add-menu').click(function(){
		itemRender({})
	});
	
	$('.dd-list').sortable({
		connectWith: '.dd-list',
		cursor: 'move',
		placeholder: 'sortable-placeholder',
	});
	
});
const itemRender = function(data){
	data.id = data.id || new Date().getTime()
	data.text = data.text || '未命名菜单'
	let item = $('.J_configbox').find("li[attr-id='" + data.id + "']")
	if (item.length == 0) item = $($('#item-temp').html()).appendTo('.J_configbox');
	item.find('.dd3-content').text(data.text)
	item.attr('attr-id', data.id);
};
</script>
</body>
</html>
