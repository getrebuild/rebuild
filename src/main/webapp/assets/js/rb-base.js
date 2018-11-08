/*! jQuery Cookie Plugin v1.4.1 - https://github.com/carhartl/jquery-cookie */
(function(factory){if(typeof define==="function"&&define.amd){define(["jquery"],factory)}else{if(typeof exports==="object"){factory(require("jquery"))}else{factory(jQuery)}}}(function($){var pluses=/\+/g;function encode(s){return config.raw?s:encodeURIComponent(s)}function decode(s){return config.raw?s:decodeURIComponent(s)}function stringifyCookieValue(value){return encode(config.json?JSON.stringify(value):String(value))}function parseCookieValue(s){if(s.indexOf('"')===0){s=s.slice(1,-1).replace(/\\"/g,'"').replace(/\\\\/g,"\\")}try{s=decodeURIComponent(s.replace(pluses," "));return config.json?JSON.parse(s):s}catch(e){}}function read(s,converter){var value=config.raw?s:parseCookieValue(s);return $.isFunction(converter)?converter(value):value}var config=$.cookie=function(key,value,options){if(value!==undefined&&!$.isFunction(value)){options=$.extend({},config.defaults,options);if(typeof options.expires==="number"){var days=options.expires,t=options.expires=new Date();t.setTime(+t+days*86400000)}return(document.cookie=[encode(key),"=",stringifyCookieValue(value),options.expires?"; expires="+options.expires.toUTCString():"",options.path?"; path="+options.path:"",options.domain?"; domain="+options.domain:"",options.secure?"; secure":""].join(""))}var result=key?undefined:{};var cookies=document.cookie?document.cookie.split("; "):[];for(var i=0,l=cookies.length;i<l;i++){var parts=cookies[i].split("=");var name=decode(parts.shift());var cookie=parts.join("=");if(key&&key===name){result=read(cookie,value);break}if(!key&&(cookie=read(cookie))!==undefined){result[name]=cookie}}return result};config.defaults={};$.removeCookie=function(key,options){if($.cookie(key)===undefined){return false}$.cookie(key,"",$.extend({},options,{expires:-1}));return !$.cookie(key)}}));

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
		headers:{
			'Content-Type':'text/plain;charset=utf-8'
		},
		cache : false,
		complete : function(xhr) {
			if (xhr.status == 200){ }  // OK
			else if (xhr.status == 403) rb.notice(xhr.responseText || '无权访问', 'danger')
			else rb.notice((xhr.responseText || '系统繁忙，请稍后重试') + ' [' + xhr.status + ']', 'danger', { timeout: 6000 })
		}
	});
	
	$.cookie.defaults = { expires:14, path: '/' }
	
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
// extend String
String.prototype.startsWith = function(substr) {
	if (!!!substr) return false;
	if (this.length == 0 || this.length < substr.length) return false;
	return this.substring(0, substr.length) == substr;
}
String.prototype.endsWith = function(substr) {
	if (!!!substr) return false;
	if (this.length == 0 || this.length < substr.length) return false;
	return this.substring(this.length - substr.length) == substr;
}
String.prototype.contains = function(substr) {
	if (!!!substr) return false;
	return this.indexOf(substr) >=0;
}

const __$setTimeoutHolds = {};
/* 不会重复执行的 setTimeout
 */
const $setTimeout = function(e, t, id){
	if (id && __$setTimeoutHolds[id]){
		clearTimeout(__$setTimeoutHolds[id]);
		__$setTimeoutHolds[id] = null;
	}
	let timer = setTimeout(e, t);
	if (id) __$setTimeoutHolds[id] = timer;
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
	_UTCDate:/^[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}$/,  // 2010-01-01
	_Url:/^(http|https|ftp)\:\/\/[a-z0-9\-\.]+(:[0-9]*)?\/?([a-z0-9\-\._\?\,\'\/\\\+&amp;%\$#\=~!:])*$/i,
	_Mail:/^[a-z0-9._%-]+@[a-z0-9.-]+\.[a-z]{2,4}$/i,
	_Number:/^[-+]?[0-9]+$/,  // 数字
	_Decimal:/^[-+]?\d*\.?\d+$/,  // 包括小数点的数字
	_Mobile:/^(13[0-9]|15[0-9]|18[0-9]|17[0-9])\d{8}$/,
	_Tel:/^[0-9-]{4,18}$/,
	_Text:/^[a-z\d\u4E00-\u9FA5]+$/i,  // 不含特殊字符和标点
	isDate:function(val){
		return this._Date.test(val);
	},
	isUTCDate:function(val){
		return this._UTCDate.test(val);
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

const $encode = function(s) {
	if (!!!s) return null
	return encodeURIComponent(s)
}
const $decode = function(s) {
	if (!!!s) return null
	return decodeURIComponent(s)
}

const $storage = {
	get(key){
		if (window.localStorage) return localStorage.getItem(key)
		else return $.cookie(key)
	},
	set(key, val){
		if (window.localStorage) localStorage.setItem(key, val)
		else $.cookie(key, val, { expires:365 })
	},
	remove(key){
		if (window.localStorage) localStorage.removeItem(key)
		else $.removeCookie(key)
	}
}

let random_times = 0
const $random = function(){
	return new Date().getTime() + '-' + random_times++
}