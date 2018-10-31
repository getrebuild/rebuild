// common Init
$(function(){
	let t = $('.rb-scroller');
	t.perfectScrollbar()
	$(window).resize(function(){
		$setTimeout(function(){
			t.perfectScrollbar('update')
		}, 500, 'rb-scroller-update');
	});
	
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
});

const __initNavs = function(){
	// Nav
	$('.rb-toggle-left-sidebar').click(function(){
		let el = $('.rb-collapsible-sidebar').toggleClass('rb-collapsible-sidebar-collapsed')
		let collapsed = el.hasClass('rb-collapsible-sidebar-collapsed')
		$storage.set('rb-sidebar-collapsed', collapsed)
		$('.sidebar-elements>li>a').tooltip('toggleEnabled')
	});
	if ($storage.get('rb-sidebar-collapsed') == 'true'){
		$('.rb-collapsible-sidebar').addClass('rb-collapsible-sidebar-collapsed')
	} else {
		$('.sidebar-elements>li>a').tooltip('disable')
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
		if ($('.rb-collapsible-sidebar').hasClass('rb-collapsible-sidebar-collapsed') && currsntSubnav.hasClass('open')) {
			currsntSubnav.removeClass('open')
			currsntSubnav.find('.sub-menu').removeClass('visible')
		}
	})
	
	let activeNav = $('.sidebar-elements li.active')
	if (activeNav.parents('li.parent').length > 0) {
		activeNav.parents('li.parent').addClass('active').first().trigger('click')
		$(document.body).trigger('click')
	}
	
	// At small-width
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
		rb.modal(rb.baseUrl + '/page/settings/nav-settings', '设置导航菜单');
	});
}

// 是否在管理员页
const $inAdminPage = location.href.indexOf('/admin/') > -1

// 计算分页
// @tp 总计页面 
// @cp 当前页面
const $calcPages = function(tp, cp){
	tp = ~~tp; cp = ~~cp;
	let pages = [];
	if (tp <= 8){
		for (var i = 1; i <= tp; i++) pages.push(i);
		return pages;
	}
	if (cp > tp) cp = tp;
	if (cp <= 4) cp = 4;
	var begin = cp - 2, end = cp + 3;
	if (begin < 1) begin = 1;
	if (end > tp) end = tp;
	if (begin > 1) pages.push(1);
	if (begin > 2) pages.push('.');
	for (var i = begin; i < end; i++) pages.push(i);
	if (end <= tp - 1) pages.push('.');
	if (end <= tp) pages.push(tp);
	return pages;
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