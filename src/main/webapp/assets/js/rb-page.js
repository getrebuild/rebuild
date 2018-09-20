// common Init
$(function(){
	
	let t = $('.rb-scroller');
	t.perfectScrollbar();
	$(window).resize(function(){
		$setTimeout(function(){
			t.perfectScrollbar('update');
		}, 500, 'rb-scroller-update');
	});
	
	// Nav
	
	$('.rb-toggle-left-sidebar').click(function(){
		$('.rb-collapsible-sidebar').toggleClass('rb-collapsible-sidebar-collapsed');
	});
	
	navsModal = null;
	$('.nav-settings').click(function(){
		if (navsModal) navsModal.show();
		else navsModal = rb.modal(rb.baseUrl + '/settings/nav-settings.htm', '设置导航菜单', 720);
	});
	
	
});