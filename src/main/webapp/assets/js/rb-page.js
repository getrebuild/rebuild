// Init
$(function(){
	// Nav
	
	let t = $('.rb-scroller');
	t.perfectScrollbar();
	$(window).resize(function(){
		$setTimeout(function(){
			t.perfectScrollbar('update');
		}, 500, 'rb-scroller-update');
	});
	
	$('.rb-toggle-left-sidebar').click(function(){
		$('.rb-collapsible-sidebar').toggleClass('rb-collapsible-sidebar-collapsed');
	});
	
	let nsModal;
	$('.nav-settings').click(function(){
		if (nsModal) nsModal.show();
		else nsModal = rb.modal(rb.baseUrl + '/settings/nav-settings.htm', '导航设置', 720);
	});
});