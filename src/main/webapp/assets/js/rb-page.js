// page initial
$(function(){
	let t = $('.rb-scroller')
	t.perfectScrollbar()
	$(window).resize(function(){
		$setTimeout(function(){ t.perfectScrollbar('update') }, 500, 'rb-scroller-update')
	})
	
	// tooltip
	$('[data-toggle="tooltip"]').tooltip()
	
	// In top-frame
	if ($('.rb-left-sidebar').length > 0){
		$('.sidebar-elements>li>a').each(function(){
			let _this = $(this)
			_this.tooltip({ placement: 'right', title: _this.find('span').text().trim(), delay: 200 })
		})
		__initNavs()
	}
	
	if (rb.isAdminUser == true) {
		$('.J_for-admin').removeClass('hide')
		if (location.href.indexOf('/admin/') == -1) {
			if (rb.isAdminVerified == true) {
				$('.J_admin-settings a i').addClass('text-primary')
			}
		}
	} else {
		$('.J_for-admin').remove()
	}
	
	if ($('.J_notifications-top').length > 0) {
		setTimeout(__checkMessage, 1500)
		$('.J_notifications-top').on('shown.bs.dropdown', __loadMessages)
	}
	
	let keydown_times = 0
	$(document.body).keydown((e)=>{
		if (e.ctrlKey && e.altKey && e.which == 88) command_exec(++keydown_times)
	})
})
command_exec = function(t){ }

// 导航菜单
const __initNavs = function(){
	let isOffcanvas = $('.rb-offcanvas-menu').length > 0  // 浮动模式
	
	// Nav
	if (isOffcanvas) {
		$('.rb-toggle-left-sidebar').click(function(){
			$(document.body).toggleClass('open-left-sidebar')
			return false
		})
		$('.sidebar-elements>li>a').tooltip('disable')
	} else {
		$('.rb-toggle-left-sidebar').click(function(){
			let el = $('.rb-collapsible-sidebar').toggleClass('rb-collapsible-sidebar-collapsed')
			let collapsed = el.hasClass('rb-collapsible-sidebar-collapsed')
			$storage.set('rb-sidebar-collapsed', collapsed)
			$('.sidebar-elements>li>a').tooltip('toggleEnabled')
			$(window).trigger('resize')
		})
		if ($storage.get('rb-sidebar-collapsed') == 'true'){
			$('.rb-collapsible-sidebar').addClass('rb-collapsible-sidebar-collapsed')
		} else {
			$('.sidebar-elements>li>a').tooltip('disable')
		}
	}
	
	// SubNavs
	let currsntSubnav
	$('.sidebar-elements li.parent').click(function(e){
		let _this = $(this)
		_this.toggleClass('open')
		_this.find('.sub-menu').toggleClass('visible')
		e.stopPropagation()
		currsntSubnav = _this
		_this.find('a').eq(0).tooltip('hide')
	})
	$('.sidebar-elements li.parent .sub-menu').click(function(e){
		e.stopPropagation()
	})
	$(document.body).click(function(){
		// MinNav && SubnavOpen
		if ($('.rb-collapsible-sidebar').hasClass('rb-collapsible-sidebar-collapsed') && currsntSubnav && currsntSubnav.hasClass('open')) {
			currsntSubnav.removeClass('open')
			currsntSubnav.find('.sub-menu').removeClass('visible')
		}
		if (isOffcanvas) {
			$(document.body).removeClass('open-left-sidebar')
		}
	})
	
	let activeNav = $('.sidebar-elements li.active')
	if (activeNav.parents('li.parent').length > 0) {
		activeNav.parents('li.parent').addClass('active').first().trigger('click')
		$(document.body).trigger('click')
	}
	
	// When small-width
	$('.left-sidebar-toggle').click(function(){
		$('.rb-collapsible-sidebar').toggleClass('rb-collapsible-sidebar-collapsed')
		$('.left-sidebar-spacer').toggleClass('open')
	}).text($('.rb-right-navbar .page-title').text())
	
	// aside
	let aside = $('.page-aside')
	if (aside.length > 0) {
		$('.page-aside .aside-header').click(function(){
			$(this).toggleClass('collapsed')
			$('.page-aside .aside-nav').toggleClass('show')
		})
	}
	
	$('.nav-settings').click(function(){
		rb.modal(rb.baseUrl + '/p/commons/nav-settings', '设置导航菜单')
	})
}

// 检查消息
let __checkMessage__state = 0
const __checkMessage = function(){
	$.get(rb.baseUrl + '/app/notification/check-message', function(res){
		$('.J_notifications-top .badge').text(res.data.unread)
		if (res.data.unread > 0){
			$('.J_notifications-top .indicator').removeClass('hide')
		} else {
			$('J_notifications-top .indicator').addClass('hide')
		}
		if (__checkMessage__state != res.data.unread) __loadMessages__state = 0
		__checkMessage__state = res.data.unread
		setTimeout(__checkMessage, 3000 * (rb.env == 'dev' ? 10 : 1))
	})
}
let __loadMessages__state = 0
const __loadMessages = function(){
	if (__loadMessages__state == 1) return
	$.get(rb.baseUrl + '/app/notification/list?pageSize=10', (res)=>{
		let el = $('.rb-notifications .content ul').empty()
		$(res.data).each((idx, item)=>{
			let o = $('<li class="notification"></li>').appendTo(el)
			if (item[3] == true) o.addClass('notification-unread')
			o = $('<a href="' + rb.baseUrl + '/app/notifications#id=' + item[4] + '"></a>').appendTo(o)
			$('<div class="image"><img src="' + item[0][1] + '" alt="Avatar"></div>').appendTo(o)
			o = $('<div class="notification-info"></div>').appendTo(o)
			$('<div class="text text-truncate">' + item[1] + '</div>').appendTo(o)
			$('<span class="date">' + item[2] + '</span>').appendTo(o)
		})
		__loadMessages__state = 1
		if (res.data.length == 0){
			$('<div class="must-center text-muted">暂无消息</div>').appendTo(el)
		}
	})
}

// @mbg = .btn-group
const $cleanMenu = function(mbg){
	mbg = $(mbg)
	let mbgMenu = mbg.find('.dropdown-menu')
	let first = mbgMenu.children().first()
	if (first.hasClass('dropdown-divider')) first.remove()
	let last = mbgMenu.children().last()
	if (last.hasClass('dropdown-divider')) last.remove()
	
	$(mbgMenu.children()).each(function(){
		let item = $(this)
		if (item.hasClass('hide')) item.remove()
	})
	
	// remove btn
	if (mbgMenu.children().length == 0){
		mbg.remove()
	}
}