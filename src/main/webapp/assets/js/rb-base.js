$(function(){
	let t = $('.rb-scroller');
	t.perfectScrollbar();
	$(window).resize(function(){
		setTimeoutDelay(function(){
			t.perfectScrollbar('update');
		}, 500, 'rb-scroller-update');
	});
});

var __setTimeoutDelayHolds = {};
var setTimeoutDelay = function(e, t, id){
	if (id && __setTimeoutDelayHolds[id]){
		clearTimeout(__setTimeoutDelayHolds[id]);
		__setTimeoutDelayHolds[id] = null;
	}
	let ttt = setTimeout(e, t);
	if (id) __setTimeoutDelayHolds[id] = ttt;
};