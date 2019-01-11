<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>导航菜单</title>
<style type="text/css">
.dd3-content>.zmdi{position:absolute;width:28px;height:28px;font-size:1.45rem;margin-left:-20px;margin-top:1px;}
.dd3-content{padding-left:60px !important;cursor:default;}
.dd-item>ul{margin-left:22px;padding-left:0;position:relative;}
.input-group-prepend .input-group-text{padding:0;width:37px;text-align:center;display:inline-block;overflow:hidden;padding-top:9px;background-color:#fff}
.input-group-prepend .input-group-text:hover{background-color:#eee;cursor:pointer;}
.input-group-prepend .input-group-text i.zmdi{font-size:1.5rem;}
</style>
</head>
<body class="dialog">
<div class="main-content">
	<div class="row m-0">
		<div class="col-5 mt-2 pr-0">
			<div class="sortable-box h380 rb-scroller">
				<ol class="dd-list J_config"></ol>
			</div>
			<div class="actions">
				<button type="button" class="btn btn-secondary btn-sm J_add-menu">+ 添加菜单项</button>
			</div>
		</div>
		<div class="col-7 mt-2 pl-6">
			<div class="text-muted J_edit-tips">点击左侧菜单项编辑</div>
			<div class="J_edit-menu hide" style="margin-top:-6px">
				<div class="tab-container">
					<ul class="nav nav-tabs">
						<li class="nav-item"><a class="nav-link J_menuType active" href="#ENTITY" data-toggle="tab">关联项</a></li>
						<li class="nav-item"><a class="nav-link J_menuType" href="#URL" data-toggle="tab">外部地址</a></li>
					</ul>
					<div class="tab-content m-0" style="padding:20px 0">
						<div class="tab-pane active" id="ENTITY">
							<select class="form-control form-control-sm J_menuEntity">
								<option value="">请选择关联项</option>
								<optgroup label="业务实体"></optgroup>
								<optgroup label="其他">
									<option value="$PARENT$" data-icon="menu">父级菜单</option>
								</optgroup>
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
					<button type="button" class="btn btn-primary bordered J_menuConfirm">确定</button>
				</div>
			</div>
		</div>
	</div>
	<div class="dialog-footer">	
		<div class="float-left hide J_for-admin">
			<label class="custom-control custom-checkbox custom-control-inline">
				<input class="custom-control-input" type="checkbox" id="shareTo" value="ALL" checked="checked">
				<span class="custom-control-label">共享给全部用户</span>
			</label>
		</div>
		<button class="btn btn-primary J_save" type="button">保存</button>
		<button class="btn btn-secondary" onclick="parent.rb.modalHide()" type="button">取消</button>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/babel">
const UNICON_NAME = 'texture'
$(document).ready(function(){
	$('.J_add-menu').click(function(){ render_item({}, true) })

	$.get(rb.baseUrl + '/commons/metadata/entities', function(res){
		$(res.data).each(function(){
			$('<option value="' + this.name + '" data-icon="' + this.icon + '">' + this.label + '</option>').appendTo('.J_menuEntity optgroup:eq(0)')
		})
	})
	$('.J_menuEntity').change(function(){
		if (item_current_isNew == true) {
			let icon = $('.J_menuEntity option:selected').data('icon')
			$('.J_menuIcon .zmdi').attr('class', 'zmdi zmdi-' + icon)
			let name = $('.J_menuEntity option:selected').text()
			$('.J_menuName').val(name)
		}
	})
	$('.J_menuIcon').click(function(){
		let url = rb.baseUrl + '/p/commons/search-icon'
		parent.clickIcon = function(s){
			$('.J_menuIcon .zmdi').attr('class', 'zmdi zmdi-' + s)
			parent.rb.modalHide(url)
		}
		parent.rb.modal(url, '选择图标')
	})
	
	$('.J_menuConfirm').click(function(){
		let name = $val('.J_menuName')
		if (!!!name) { rb.highbar('请输入菜单名称'); return }
		let type = $('.J_menuType.active').attr('href').substr(1)
		let value;
		if (type == 'ENTITY'){
			value = $val('.J_menuEntity')
			if (!!!value){ rb.highbar('请选择关联项'); return }
		} else {
			value = $val('.J_menuUrl')
			if (!!!value){ rb.highbar('请输入 URL'); return }
			else if (!!value && !$regex.isUrl(value)){ rb.highbar('请输入有效的 URL'); return }
		}
		let icon = $('.J_menuIcon i').attr('class').replace('zmdi zmdi-', '')
		render_item({ id: item_currentid, text: name, type: type, value: value, icon: icon })
		
		item_currentid = null
		$('.J_config li').removeClass('active')
		$('.J_edit-tips').removeClass('hide')
		$('.J_edit-menu').addClass('hide')
	})
	
	var cfgid = null
	$('.J_save').click(function(){
		let navs = []
		$('.J_config>.dd-item').each(function(){
			let item = build_item($(this), navs)
			if (!!item) navs.push(item)
		})
		if (navs.length == 0) { rb.highbar('请至少设置一个菜单项'); return }
		
		let btn = $(this).button('loading')
		$.post(rb.baseUrl + '/app/settings/nav-settings?cfgid=' + cfgid + '&toAll=' + $('#shareTo').prop('checked'), JSON.stringify(navs), function(res){
			btn.button('reset')
			if (res.error_code == 0) parent.location.reload()
		})
	})
	
	add_sortable('.J_config')
	$.get(rb.baseUrl + '/app/settings/nav-settings', function(res){
		if (res.data){
			cfgid = res.data.id
			$(res.data.config).each(function(){
				let item = render_item(this)
				if (!!this.sub) {
					let subUl = $('<ul></ul>').appendTo(item)
					$(this.sub).each(function(){
						render_item(this, false, subUl)
					})
					add_sortable(subUl)
				}
			})
			$('#shareTo').attr('checked', res.data.shareTo == 'ALL')
		}
	})
})
const add_sortable = function(el){
	$(el).sortable({
		placeholder: 'dd-placeholder',
		handle: '>.dd3-handle',
		axis: 'y'
	}).disableSelection()
}

const build_item = function(item){
	let data = { text: $.trim(item.find('.dd3-content').eq(0).text()), type: item.attr('attr-type'), value: item.attr('attr-value'), icon: item.attr('attr-icon') }
	if (!!!data.value) return null
	
	let subNavs = item.find('ul>li')
	if (subNavs.length > 0) {
		data.sub = []
		subNavs.each(function(){
			let sub = build_item($(this))
			if (sub) data.sub.push(sub)
		})
	}
	return data
}
let item_currentid
let item_current_isNew
let item_randomid = new Date().getTime()
const render_item = function(data, isNew, append2) {
	data.id = data.id || item_randomid++
	data.text = data.text || '未命名菜单'
	data.icon = data.icon || UNICON_NAME
	append2 = append2 || '.J_config'
	
	let item = $('.J_config').find("li[attr-id='" + data.id + "']")
	if (item.length == 0){
		item = $('<li class="dd-item dd3-item"><div class="dd-handle dd3-handle"></div><div class="dd3-content"><i class="zmdi"></i><span></span></div></li>').appendTo(append2)
		let action = $('<div class="dd3-action"><a class="J_addsub" title="添加子菜单"><i class="zmdi zmdi-plus"></i></a><a class="J_del" title="移除"><i class="zmdi zmdi-close"></i></a></div>').appendTo(item)
		action.find('a.J_del').off('click').click(function() {
			item.remove()
		})
		action.find('a.J_addsub').off('click').click(function() {
			let subUl = item.find('ul')
			if (subUl.length == 0) {
				subUl = $('<ul></ul>').appendTo(item)
				add_sortable(subUl)
			}
			render_item({}, true, subUl)
		})
		if (!$(append2).hasClass('J_config')) {
			action.find('a.J_addsub').remove()
		}
	}
	
	let content3 = item.find('.dd3-content').eq(0)
	content3.find('.zmdi').attr('class', 'zmdi zmdi-' + data.icon)
	content3.find('span').text(data.text)
	item.attr({
		'attr-id': data.id,
		'attr-type': data.type || 'ENTITY',
		'attr-value': data.value || '',
		'attr-icon': data.icon,
	})
	
	// Event
	content3.off('click').click(function(){
		$('.J_config li').removeClass('active')
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
	
	if (isNew == true){
		content3.trigger('click')
		$('.J_menuName').focus()
	}
	item_current_isNew = isNew
	return item
}
</script>
</body>
</html>
