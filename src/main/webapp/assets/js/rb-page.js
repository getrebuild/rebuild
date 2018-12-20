// page initial
$(function(){
	let t = $('.rb-scroller')
	t.perfectScrollbar()
	$(window).resize(function(){
		$setTimeout(function(){
			t.perfectScrollbar('update')
		}, 500, 'rb-scroller-update')
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
	
	// animate class
	$setTimeout(function(){
		$(document.body).addClass('rb-animate')
	}, 1000)
	
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
	
	if ($('.rb-notifications').length > 0) {
		setTimeout(__checkMessage, 1500)
	}
})

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
let __checkMessage_status = 0
const __checkMessage = function(){
	$.get(rb.baseUrl + '/app/notification/check-message', function(res){
		let show = $('.rb-notifications').prev()
		if (res.data.unread > 0){
			if (__checkMessage_status != res.data.unread) {
				show.find('.indicator').removeClass('hide')
				show.tooltip({ title: '有 ' + res.data.unread + ' 条未读消息', offset: -6 })
				$('.rb-notifications span.badge').text(res.data.unread)
				__checkMessage_status = res.data.unread
			}
		} else if (__checkMessage_status > 0) {
			show.find('.indicator').addClass('hide')
			show.attr('title', '').tooltip('dispose')
			$('.rb-notifications span.badge').text('0')
			__checkMessage_status = 0
		}
		setTimeout(__checkMessage, 3000 * (rb.env == 'dev' ? 10 : 1))
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