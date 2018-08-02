// extend jQuery
(function($) {
	$.fn.extend({
		'button': function(state) {
			let el = $(this);
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
		setTimeoutDelay(function(){
			t.perfectScrollbar('update');
		}, 500, 'rb-scroller-update');
	});
	
	$('.rb-toggle-left-sidebar').click(function(){
		$('.rb-collapsible-sidebar').toggleClass('rb-collapsible-sidebar-collapsed');
	})
});

const __setTimeoutDelayHolds = {};
const setTimeoutDelay = function(e, t, id){
	if (id && __setTimeoutDelayHolds[id]){
		clearTimeout(__setTimeoutDelayHolds[id]);
		__setTimeoutDelayHolds[id] = null;
	}
	let ttt = setTimeout(e, t);
	if (id) __setTimeoutDelayHolds[id] = ttt;
};