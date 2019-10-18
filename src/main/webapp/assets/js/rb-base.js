/* eslint-disable no-unused-vars */
/*! jQuery Cookie Plugin v1.4.1 - https://github.com/carhartl/jquery-cookie */
// eslint-disable-next-line quotes
(function (factory) { if (typeof define === "function" && define.amd) { define(["jquery"], factory) } else { if (typeof exports === "object") { factory(require("jquery")) } else { factory(jQuery) } } }(function ($) { var pluses = /\+/g; function encode(s) { return config.raw ? s : encodeURIComponent(s) } function decode(s) { return config.raw ? s : decodeURIComponent(s) } function stringifyCookieValue(value) { return encode(config.json ? JSON.stringify(value) : String(value)) } function parseCookieValue(s) { if (s.indexOf('"') === 0) { s = s.slice(1, -1).replace(/\\"/g, '"').replace(/\\\\/g, "\\") } try { s = decodeURIComponent(s.replace(pluses, " ")); return config.json ? JSON.parse(s) : s } catch (e) { } } function read(s, converter) { var value = config.raw ? s : parseCookieValue(s); return $.isFunction(converter) ? converter(value) : value } var config = $.cookie = function (key, value, options) { if (value !== undefined && !$.isFunction(value)) { options = $.extend({}, config.defaults, options); if (typeof options.expires === "number") { var days = options.expires, t = options.expires = new Date(); t.setTime(+t + days * 86400000) } return (document.cookie = [encode(key), "=", stringifyCookieValue(value), options.expires ? "; expires=" + options.expires.toUTCString() : "", options.path ? "; path=" + options.path : "", options.domain ? "; domain=" + options.domain : "", options.secure ? "; secure" : ""].join("")) } var result = key ? undefined : {}; var cookies = document.cookie ? document.cookie.split("; ") : []; for (var i = 0, l = cookies.length; i < l; i++) { var parts = cookies[i].split("="); var name = decode(parts.shift()); var cookie = parts.join("="); if (key && key === name) { result = read(cookie, value); break } if (!key && (cookie = read(cookie)) !== undefined) { result[name] = cookie } } return result }; config.defaults = {}; $.removeCookie = function (key, options) { if ($.cookie(key) === undefined) { return false } $.cookie(key, "", $.extend({}, options, { expires: -1 })); return !$.cookie(key) } }));
// extends jQuery
(function ($) {
  $.fn.extend({
    'button': function (state) {
      return this.each(function () {
        var el = $(this)
        if (el.prop('nodeName') !== 'BUTTON') return
        if (state === 'loading') {
          el.attr('disabled', true)
          var loadingText = el.data('loading-text')
          if (loadingText) {
            var _this = this
            this.__loadingTextTimer = setTimeout(function () {
              _this.__textHold = el.html()
              el.text(loadingText)
            }, 200)
          }
        } else if (state === 'reset') {
          el.attr('disabled', false)
          if (this.__loadingTextTimer) {
            clearTimeout(this.__loadingTextTimer)
            this.__loadingTextTimer = null
            if (this.__textHold) el.html(this.__textHold)
          }
        }
      })
    }
  })

  $.ajaxSetup({
    headers: {
      'Content-Type': 'text/plain;charset=utf-8'
    },
    cache: false,
    complete: function (xhr) {
      // eslint-disable-next-line no-empty
      if (xhr.status === 200 || xhr.status === 0) { } // That's OK
      else if (xhr.status === 403 || xhr.status === 401) RbHighbar.error(xhr.responseText || '未授权访问')
      else {
        var error = xhr.responseText
        if (rb.env !== 'dev' && error && error.contains('Exception : ')) error = error.split('Exception : ')[1]
        RbHighbar.error(error)
      }
    }
  })

  $.cookie.defaults = { expires: 14, path: '/' }

  $.fn.select2.defaults.set('width', '100%')
  $.fn.select2.defaults.set('language', 'zh-CN')
  $.fn.select2.defaults.set('allowClear', true)
  $.fn.select2.defaults.set('placeholder', '')

  window.rb = window.rb || {}
  $('meta[name^="rb."]').each(function (idx, item) {
    var k = $(item).attr('name').substr(3) // remove `rb.`
    var v = $(item).attr('content')
    if (v === 'true') v = true
    else if (v === 'false') v = false
    window.rb[k] = v
  })
  if (rb.appName && rb.appName !== document.title) document.title = document.title + ' · ' + rb.appName

  setTimeout(function () {
    if (rb.env === 'dev') $('html').addClass('dev')
    $(document.body).addClass('rb-animate')
  }, 1000)
})(jQuery)

// extends Array
Array.prototype.remove = function (item) {
  var index = this.indexOf(item)
  if (index >= 0) this.splice(index, 1)
}
Array.prototype.contains = function (item) {
  var i = this.length
  while (i--) {
    if (this[i] === item) return true
  }
  return false
}
Array.prototype.insert = function (index, item) {
  this.splice(index, 0, item)
}
// extends String
String.prototype.startsWith = function (substr) {
  if (!substr) return false
  if (this.length === 0 || this.length < substr.length) return false
  return this.substring(0, substr.length) === substr
}
String.prototype.endsWith = function (substr) {
  if (!substr) return false
  if (this.length === 0 || this.length < substr.length) return false
  return this.substring(this.length - substr.length) === substr
}
String.prototype.contains = function (substr) {
  if (!substr) return false
  return this.indexOf(substr) >= 0
}

var $setTimeout__timers = {}
/**
 * 不会重复执行的 setTimeout
 * @param {Function} e 
 * @param {Number} t 
 * @param {String} id 
 */
var $setTimeout = function (e, t, id) {
  if (id && $setTimeout__timers[id]) {
    clearTimeout($setTimeout__timers[id])
    $setTimeout__timers[id] = null
  }
  var timer = setTimeout(e, t)
  if (id) $setTimeout__timers[id] = timer
}

/**
 * 获取 URL 参数
 * @param {String} key 
 * @param {String} qstr 
 */
var $urlp = function (key, qstr) {
  qstr = qstr || window.location.search
  if (!qstr) return (!key || key === '*') ? {} : null
  qstr = qstr.replace(/%20/g, ' ')
  qstr = qstr.substr(1) // remove first '?'
  var params = qstr.split('&')
  var map = new Object()
  for (var i = 0, j = params.length; i < j; i++) {
    var kv = params[i].split('=')
    map[kv[0]] = kv[1]
  }
  return (!key || key === '*') ? map : map[key]
}

/**
 * 获取元素值
 * 如有 data-o 属性：如当前值与原值（data-o）一致，则返回 undefined；如清空了值则返回 null
 * @param {Element/String} el 
 */
var $val = function (el) {
  el = $(el)
  if (el.length === 0) return null
  var nVal = null
  var tag = el.prop('tagName')
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') {
    if (tag === 'INPUT' && el.attr('type') === 'checkbox') {
      nVal = el.prop('checked') + ''
    } else {
      nVal = el.val()
    }
  } else {
    nVal = el.attr('value')
  }

  var oVal = el.data('o') + ''
  if (!oVal) return $.trim(nVal) || null

  // eslint-disable-next-line no-constant-condition
  if ((oVal || 666) === (nVal || 666)) return null // unmodified
  if (!!oVal && !nVal) return '' // new value is empty
  else return $.trim(nVal) || null
}

/**
 * 清理 Map 中的无效值（null、undefined）
 * @param {Object} map 
 */
var $cleanMap = function (map) {
  if ($.type(map) !== 'object') throw Error('Unsupportted type ' + $.type(map))
  var newMap = {}
  for (var k in map) {
    var v = map[k]
    if (!(v === null || v === undefined)) newMap[k] = v
  }
  return newMap
}

// 常用正则
var $regex = {
  _Date: /^((((1[6-9]|[2-9]\d)\d{2})-(0?[13578]|1[02])-(0?[1-9]|[12]\d|3[01]))|(((1[6-9]|[2-9]\d)\d{2})-(0?[13456789]|1[012])-(0?[1-9]|[12]\d|30))|(((1[6-9]|[2-9]\d)\d{2})-0?2-(0?[1-9]|1\d|2[0-8]))|(((1[6-9]|[2-9]\d)(0[48]|[2468][048]|[13579][26])|((16|[2468][048]|[3579][26])00))-0?2-29-))$/,
  _UTCDate: /^[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}$/, // eg. 2010-01-01, 2010-1-9
  // eslint-disable-next-line no-useless-escape
  _Url: /^(http|https|ftp)\:\/\/[a-z0-9\-\.]+(:[0-9]*)?\/?([a-z0-9\-\._\?\,\'\/\\\+&amp;%\$#\=~!:])*$/i,
  _Mail: /^[a-z0-9._%-]+@[a-z0-9.-]+\.[a-z]{2,4}$/i,
  _Number: /^[-+]?[0-9]+$/, // 数字
  _Decimal: /^[-+]?\d*\.?\d+$/, // 包括小数点的数字
  _Mobile: /^(13[0-9]|15[0-9]|18[0-9]|17[0-9])\d{8}$/,
  _Tel: /^[0-9-]{7,18}$/,
  _Text: /^[a-z\d\u4E00-\u9FA5]+$/i, // 不含特殊字符和标点
  isDate: function (val) {
    return this._Date.test(val)
  },
  isUTCDate: function (val) {
    return this._UTCDate.test(val)
  },
  isUrl: function (val) {
    return this._Url.test(val)
  },
  isMail: function (val) {
    return this._Mail.test(val)
  },
  isNumber: function (val) {
    return this._Number.test(val)
  },
  isDecimal: function (val) {
    return this._Decimal.test(val)
  },
  isMobile: function (val) {
    return this._Mobile.test(val)
  },
  isTel: function (val) {
    return this._Tel.test(val) || this._Mobile.test(val)
  },
  isValidText: function (val) {
    return this._Text.test(val)
  },
  isNotBlank: function (val) {
    return !val || $.trim(val).length === 0
  },
  isId: function (id) {
    return /^([0-9]{3}-[a-z0-9]{16})$/ig.test(id)
  }
}

var $encode = function (s) {
  if (!s) return ''
  return encodeURIComponent(s)
}
var $decode = function (s) {
  if (!s) return ''
  return decodeURIComponent(s)
}

var $storage = {
  get: function (key) {
    if (window.localStorage) return localStorage.getItem(key)
    else return $.cookie(key)
  },
  set: function (key, val) {
    if (window.localStorage) localStorage.setItem(key, val)
    else $.cookie(key, val, {
      expires: 365
    })
  },
  remove: function (key) {
    if (window.localStorage) localStorage.removeItem(key)
    else $.removeCookie(key)
  }
}

var $random__times = 0
var $random = function (prefix) {
  return (prefix || '') + new Date().getTime() + '' + ($random__times++)
}

/**
 * 计算分页
 * @param {Number} tp 总计页面
 * @param {Number} cp 当前页面
 */
var $pages = function (tp, cp) {
  var pages = []
  if (tp <= 8) {
    for (var i = 1; i <= tp; i++) pages.push(i)
    return pages
  }
  if (cp > tp) cp = tp
  if (cp <= 4) cp = 4
  var begin = cp - 2,
    end = cp + 3
  if (begin < 1) begin = 1
  if (end > tp) end = tp
  if (begin > 1) pages.push(1)
  if (begin > 2) pages.push('.')
  for (var j = begin; j < end; j++) pages.push(j)
  if (end <= tp - 1) pages.push('.')
  if (end <= tp) pages.push(tp)
  return pages
}

/**
 * 可以比较对象或数组
 * @param {*} a 
 * @param {*} b 
 */
var $same = function (a, b) {
  if (!a && !b) return true
  if (a && b) {
    if ($.type(a) === 'object' && $.type(b) === 'object') {
      for (var k in a) {
        if (a[k] !== b[k]) return false
      }
      return true
    } else if ($.type(a) === 'array' && $.type(b) === 'array') {
      a = a.join(',')
      b = b.join(',')
    }
  }
  // eslint-disable-next-line eqeqeq
  return a == b
}