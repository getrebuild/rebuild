<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.rebuild.server.service.entitymanage.DisplayType"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>列表选项</title>
<style type="text/css">
.sortable-box{padding:0 1px;padding-top:1px;}
.dd-list .dd3-item .dd3-content,.dd-placeholder{margin:0 0 1px !important;cursor:default;}
.sortable-box .dd-list .dd3-item.ui-sortable-helper .dd3-handle{top:0;height:36px}
.dd-list .dd3-item .dd3-content{position:relative;padding-left:66px}
.dd-list .dd3-item .dd3-content i.zmdi{font-size:1.4rem;width:22px;overflow:hidden;position:absolute;left:43px;margin-top:1px}
.dd-placeholder{height:36px !important}
.dd-list .dd3-item:hover .dd3-content,.dd-list .dd3-item.active .dd3-content{background-color:#5a95f5;border-color:#5a95f5;color:#fff}
.dd-list .dd3-item:hover a{color:#fff !important;display:inline-block;width:24px !important}
.input-group-prepend .input-group-text{padding:0;width:37px;text-align:center;display:inline-block;overflow:hidden;padding-top:9px;background-color:#fff}
.input-group-prepend .input-group-text:hover{background-color:#eee;cursor:pointer;}
.input-group-prepend .input-group-text i.zmdi{font-size:1.5rem;}
</style>
</head>
<body class="dialog">
<div class="main-content">
	<div class="row margin-0">
		<div class="col-5" style="padding-top:9px">
			<div class="sortable-box rb-scroller">
				<ol class="dd-list J_configbox">
				</ol>
			</div>
			<div class="actions">
				<button type="button" class="btn btn-secondary btn-sm J_add-menu">+ 添加菜单项</button>
			</div>
		</div>
		<div class="col-7" style="padding-top:9px">
			<div class="text-muted J_edit-tips">点击左侧菜单项编辑</div>
			<div class="J_edit-menu hide">
				<div class="tab-container">
					<ul class="nav nav-tabs">
						<li class="nav-item"><a class="nav-link J_menuType active" href="#ENTITY" data-toggle="tab">关联实体</a></li>
						<li class="nav-item"><a class="nav-link J_menuType" href="#URL" data-toggle="tab">外部地址</a></li>
					</ul>
					<div class="tab-content margin-0" style="padding:20px 0">
						<div class="tab-pane active" id="ENTITY">
							<select class="form-control form-control-sm J_menuEntity">
								<option value="">请选择实体</option>
							</select>
						</div>
						<div class="tab-pane" id="URL">
							<input type="text" class="form-control form-control-sm J_menuUrl" placeholder="输入 URL">
						</div>
					</div>
				</div>
				<div class="input-group" style="margin-bottom:20px">
					<span class="input-group-prepend">
						<span class="input-group-text J_menuIcon" title="选择图标"><i class="zmdi zmdi-texture"></i></span>
					</span>
					<input type="text" class="form-control form-control-sm J_menuName" placeholder="菜单名称">
				</div>
				<div>
					<button type="button" class="btn btn-secondary J_menuConfirm">确定</button>
				</div>
			</div>
		</div>
	</div>
	<div class="dialog-footer">	
		<div class="float-left">
			<label class="custom-control custom-checkbox custom-control-inline" style="margin:0;margin-top:6px">
				<input class="custom-control-input" type="checkbox" id="applyFor" value="ALL"><span class="custom-control-label"> 应用到全部用户</span>
			</label>
		</div>
		<button class="btn btn-primary J_save" type="button">保存</button>
		<button class="btn btn-secondary" onclick="parent.navsModal.hide()" type="button">取消</button>
	</div>
</div>
<script type="text/plain" id="item-temp">
<li class="dd-item dd3-item">
	<div class="dd3-content text-3dot">HOLD</div>
	<div class="dd-handle dd3-handle"></div>
	<div class="dd3-action"><a href="javascript:;" class="J_del" title="移除"><i class="zmdi zmdi-close"></i></a></div>
</li>
</script>
<%@ include file="/_include/Foot.jsp"%>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/jquery-ui.min.css">
<script src="${baseUrl}/assets/lib/jquery-ui.min.js"></script>
<script type="text/javascript">
const UNICON_NAME = 'texture'
$(document).ready(function(){
	$('.J_add-menu').click(function(){
		itemRender({}, true)
	});
	
	$('.dd-list').sortable({
		connectWith: '.dd-list',
		cursor: 'move',
		placeholder: 'dd-placeholder',
		handle: '.dd-handle',
		axis: 'y'
	})

	$.get(rb.baseUrl + '/app/common/metadata/entities', function(res){
		$(res.data).each(function(){
			$('<option value="' + this.name + '" data-icon="' + this.icon + '">' + this.label + '</option>').appendTo('.J_menuEntity');
		})
	})
	$('.J_menuEntity').change(function(){
		let zmdi = $('.J_menuIcon .zmdi')
		//if (zmdi.hasClass('zmdi-' + UNICON_NAME)){
			let s = $('.J_menuEntity option:selected').data('icon')
			zmdi.attr('class', 'zmdi zmdi-' + s)
		//}
	})
	iconModal = null
	$('.J_menuIcon').click(function(){
		if (iconModal) iconModal.show()
		else{
			parent.icon_call = function(s){
				$('.J_menuIcon .zmdi').attr('class', 'zmdi zmdi-' + s)
				iconModal.hide()
			}
			iconModal = parent.rb.modal(rb.baseUrl + '/common/search-icon.htm', '选择图标')
		}
	})
	
	$('.J_menuConfirm').click(function(){
		let name = $val('.J_menuName')
		if (!!!name) { rb.notice('请输入菜单名称'); return }
		let type = $('.J_menuType.active').attr('href').substr(1)
		let value;
		if (type == 'ENTITY'){
			value = $val('.J_menuEntity')
			if (!!!value){ rb.notice('请选择实体'); return }
		} else {
			value = $val('.J_menuUrl')
			if (!!!value){ rb.notice('请输入 URL'); return }
			else if (!!value && !$regex.isUrl(value)){ rb.notice('请输入有效的 URL'); return }
		}
		itemRender({ id:item_currentid, text:name, type:type, value:value, icon:$('.J_menuIcon i').attr('class').replace('zmdi zmdi-', '') })
		
		item_currentid = null;
		$('.J_configbox li').removeClass('active')
		$('.J_edit-tips').removeClass('hide')
		$('.J_edit-menu').addClass('hide')
	})
	
	var cfgid = null
	$('.J_save').click(function(){
		let navs = []
		$('.J_configbox .dd-item').each(function(){
			let _this = $(this)
			let item = { text:_this.find('.dd3-content').text(), type:_this.attr('attr-type'), value:_this.attr('attr-value'), icon:_this.attr('attr-icon') }
			if (!!item.value) navs.push(item)
		})
		console.log(JSON.stringify(navs))
		if (navs.length == 0) { rb.notice('请至少设置一个菜单项'); return }
		
		let btn = $(this).button('loading')
		$.post(rb.baseUrl + '/app/common/nav-settings?cfgid=' + cfgid, JSON.stringify(navs), function(res){
			btn.button('reset')
			if (res.error_code == 0) parent.location.reload()
			else alert(res.error_msg)
		});
	})
	
	$.get(rb.baseUrl + '/app/common/nav-settings', function(res){
		if (res.data){
			cfgid = res.data.id
			$(res.data.config).each(function(){
				itemRender(this)
			})
		}
	})
});
var item_currentid;
var item_randomid = new Date().getTime();
const itemRender = function(data, fromAdd){
	data.id = data.id || item_randomid++
	data.text = data.text || '未命名菜单'
	data.icon = data.icon || UNICON_NAME
	
	let item = $('.J_configbox').find("li[attr-id='" + data.id + "']")
	if (item.length == 0) item = $($('#item-temp').html()).appendTo('.J_configbox');
	item.find('.dd3-content').html('<i class="zmdi zmdi-' + data.icon + '"></i> ' + data.text)
	item.attr({
		'attr-id': data.id,
		'attr-type': data.type || 'ENTITY',
		'attr-value': data.value || '',
		'attr-icon': data.icon,
	});
	item.find('.J_del').click(function(){
		$(this).parent().parent().remove();
		return false
	})
	item.find('.dd3-content').click(function(){
		$('.J_configbox li').removeClass('active')
		item.addClass('active')
		
		$('.J_edit-tips').addClass('hide')
		$('.J_edit-menu').removeClass('hide')
		
		$('.J_menuName').val(data.text)
		$('.J_menuIcon i').attr('class', 'zmdi zmdi-' + data.icon)
		$('.J_menuUrl, .J_menuEntity').val('')
		if (data.type == 'URL'){
			$('.J_menuType').eq(1).click()
			$('.J_menuUrl').val(data.value)
		}else{
			$('.J_menuType').eq(0).click()
			$('.J_menuEntity').val(data.value)
		}
		item_currentid = data.id
	})
	if (fromAdd == true){
		item.find('.dd3-content').trigger('click')
		$('.J_menuName').focus()
	}
};
</script>
</body>
</html>
