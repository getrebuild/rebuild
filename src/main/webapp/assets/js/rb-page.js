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
		else navsModal = rb.modal(rb.baseUrl + '/page/settings/nav-settings', '设置导航菜单', 720);
	});
	
	
});

// 打开视图
const recordView = function(id){
	console.log(id)
}

// 计算分页
// @tp 总计页面 
// @cp 当前页面
const calcPages = function(tp, cp){
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