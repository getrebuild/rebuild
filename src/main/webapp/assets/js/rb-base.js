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
	
	$.ajaxSetup({
		dataType : 'json',
		cache : false,
		headers : {
		},
		xhrFields : {
		},
		complete : function(xhr) {
			
		}
	});
})(jQuery);
// extend Array
Array.prototype.remove = function(item) {
	var index = this.indexOf(item);
	if(index >= 0){
		this.splice(index, 1);
	}
}
Array.prototype.contains = function(item) {
    let i = this.length;
    while (i--) {
        if (this[i] === item) {
            return true;
        }
    }
    return false;
}

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
	let tag = el.prop('tagName');
	if (tag == 'INPUT' || tag == 'TEXTAREA' || tag == 'SELECT'){
		if (tag == 'INPUT' && el.attr('type') == 'checkbox'){
			nVal = el.prop('checked') + '';
		} else {
			nVal = el.val();
		}
	} else {
		nVal = el.attr('value');
	}
	
	let oVal = el.data('o') + '';
	if (!!!oVal) return nVal || null;
	
	if ((oVal || 666) === (nVal || 666)) return null;  // unmodified
	if (!!oVal && !!!nVal) return '';  // new value is empty
	else return nVal || null;
};

/* 清理 Map 中的无效值（null、undefined）
 */
const $cleanMap = function(map) {
	if ($.type(map) != 'object') throw Error('Unsupportted type ' + $.type(map))
	
	let newMap = {};
	for (let k in map) {
		let v = map[k];
		if (v === null || v === undefined);
		else newMap[k] = v;
	}
	return newMap;
}

// 常用正则
const $regex = {
	_Date:/^((((1[6-9]|[2-9]\d)\d{2})-(0?[13578]|1[02])-(0?[1-9]|[12]\d|3[01]))|(((1[6-9]|[2-9]\d)\d{2})-(0?[13456789]|1[012])-(0?[1-9]|[12]\d|30))|(((1[6-9]|[2-9]\d)\d{2})-0?2-(0?[1-9]|1\d|2[0-8]))|(((1[6-9]|[2-9]\d)(0[48]|[2468][048]|[13579][26])|((16|[2468][048]|[3579][26])00))-0?2-29-))$/,
	_Url:/^(http|https|ftp)\:\/\/[a-z0-9\-\.]+(:[0-9]*)?\/?([a-z0-9\-\._\?\,\'\/\\\+&amp;%\$#\=~!:])*$/i,
	_Mail:/^[a-z0-9._%-]+@[a-z0-9.-]+\.[a-z]{2,4}$/i,
	_Number:/^[-+]?[0-9]+$/,  // 数字
	_Decimal:/^[-+]?\d*\.?\d+$/,  // 包括小数点的数字
	_Mobile:/^(13[0-9]|15[0-9]|18[0-9]|17[0-9])\d{8}$/,
	_Tel:/^[0-9-]{4,20}$/,
	_Text:/^[a-z\d\u4E00-\u9FA5]+$/i,  // 不含特殊字符和标点
	isDate:function(val){
		return this._Date.test(val);
	},
	isUrl:function(val){
		return this._Url.test(val);
	},
	isMail:function(val){
		return this._Mail.test(val);
	},
	isNumber:function(val){
		return this._Number.test(val);
	},
	isDecimal:function(val){
		return this._Decimal.test(val);
	},
	isMobile:function(val){
		return this._Mobile.test(val);
	},
	isTel:function(val){
		return this._Tel.test(val) || this._Mobile.test(val);
	},
	isValidText:function(val){
		return this._Text.test(val);
	},
	isNotBlank:function(val){
		return !val || $.trim(val).length == 0;
	},
};