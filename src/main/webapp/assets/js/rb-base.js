// extend jQuery
(function($) {
	$.fn.extend({
		'button': function(state) {
			let el = $(this);
			if (el.prop('nodeName') != 'BUTTON') return this;
			if (state == 'loading') {
				el.attr('disabled', true);
				let loadingText = el.data('loading-text');
				if (loadingText){
					let that = this;
					this.__loadingTextTimer = setTimeout(function(){
						that.__textHold = el.html();
						el.text(loadingText);
					}, 200);
				}
			} else if (state == 'reset') {
				el.attr('disabled', false);
				if (this.__loadingTextTimer) {
					clearTimeout(this.__loadingTextTimer);
					this.__loadingTextTimer = null;
					if (this.__textHold) {
						el.html(this.__textHold);
					}
				}
			}
			return this;
		}
	});
})(jQuery);
// Init
$(function(){
	let t = $('.rb-scroller');
	t.perfectScrollbar();
	$(window).resize(function(){
		$setTimeout(function(){
			t.perfectScrollbar('update');
		}, 500, 'rb-scroller-update');
	});
	
	$('.rb-toggle-left-sidebar').click(function(){
		$('.rb-collapsible-sidebar').toggleClass('rb-collapsible-sidebar-collapsed');
	})
});

const $__setTimeoutHolds = {};
/* 不会重复执行的 setTimeout
 */
const $setTimeout = function(e, t, id){
	if (id && $__setTimeoutHolds[id]){
		clearTimeout($__setTimeoutHolds[id]);
		$__setTimeoutHolds[id] = null;
	}
	let timer = setTimeout(e, t);
	if (id) $__setTimeoutHolds[id] = timer;
};

/* 获取 URL 参数
 */
const $urlp = function(key, qstr) {
	qstr = qstr || window.location.search;
	if (!qstr) return (!key || key == '*') ? {} : null;
	qstr = qstr.replace(/%20/g, ' ');
	qstr = qstr.substr(1);
	var param = qstr.split('&');
	var map = new Object();
	for (var i = 0, j = param.length; i < j; i++){ var pl=param[i].split('='); map[pl[0]] = pl[1]; }
	return (!key || key == '*') ? map : map[key];
};

/* 获取元素值
 * 如有 data-o 属性：如当前值与原值（data-o）一致，则返回 undefined；如清空了值则返回 null
 */
const $val = function(el){
	el = $(el);
	let nVal = null;
	let oVal = el.data('o');
	let tag = el.prop('tagName');
	if (tag == 'INPUT' || tag == 'TEXTAREA' || tag == 'SELECT'){
		nVal = el.val();
	} else {
		nVal = el.attr('data-val');
		if (!!!nVal) nVal = oVal;
	}
	if (nVal) nVal = $.trim(nVal);
	if (!!!oVal) return nVal;
	
	if ((oVal || null) === (nVal || null)) return undefined;
	if (!!oVal && !!!nVal) return null;
	else return $.trim(nVal);
};