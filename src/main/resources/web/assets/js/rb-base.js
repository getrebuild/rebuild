/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */
/* !!! KEEP IT ES5 COMPATIBLE !!! */

/*! https://github.com/carhartl/jquery-cookie */
// prettier-ignore
!(function(factory){if(typeof define==='function'&&define.amd){define(['jquery'],factory)}else{if(typeof exports==='object'){factory(require('jquery'))}else{factory(jQuery)}}})(function($){var pluses=/\+/g;function encode(s){return config.raw?s:encodeURIComponent(s)}function decode(s){return config.raw?s:decodeURIComponent(s)}function stringifyCookieValue(value){return encode(config.json?JSON.stringify(value):String(value))}function parseCookieValue(s){if(s.indexOf('"')===0){s=s.slice(1,-1).replace(/\\"/g,'"').replace(/\\\\/g,'\\')}try{s=decodeURIComponent(s.replace(pluses,' '));return config.json?JSON.parse(s):s}catch(e){}}function read(s,converter){var value=config.raw?s:parseCookieValue(s);return $.isFunction(converter)?converter(value):value}var config=($.cookie=function(key,value,options){if(value!==undefined&&!$.isFunction(value)){options=$.extend({},config.defaults,options);if(typeof options.expires==='number'){var days=options.expires,t=(options.expires=new Date());t.setTime(+t+days*86400000)}return(document.cookie=[encode(key),'=',stringifyCookieValue(value),options.expires?'; expires='+options.expires.toUTCString():'',options.path?'; path='+options.path:'',options.domain?'; domain='+options.domain:'',options.secure?'; secure':'',].join(''))}var result=key?undefined:{};var cookies=document.cookie?document.cookie.split('; '):[];for(var i=0,l=cookies.length;i<l;i++){var parts=cookies[i].split('=');var name=decode(parts.shift());var cookie=parts.join('=');if(key&&key===name){result=read(cookie,value);break}if(!key&&(cookie=read(cookie))!==undefined){result[name]=cookie}}return result});config.defaults={};$.removeCookie=function(key,options){if($.cookie(key)===undefined){return false}$.cookie(key,'',$.extend({},options,{expires:-1}));return !$.cookie(key)}})
/*! https://github.com/gabceb/jquery-browser-plugin */
// prettier-ignore
!(function(a){'function'===typeof define&&define.amd?define(['jquery'],function(b){return a(b)}):'object'===typeof module&&'object'===typeof module.exports?(module.exports=a(require('jquery'))):a(window.jQuery)})(function(a){function b(a){void 0===a&&(a=window.navigator.userAgent),(a=a.toLowerCase());var b=/(edge)\/([\w.]+)/.exec(a)||/(opr)[\/]([\w.]+)/.exec(a)||/(chrome)[ \/]([\w.]+)/.exec(a)||/(iemobile)[\/]([\w.]+)/.exec(a)||/(version)(applewebkit)[ \/]([\w.]+).*(safari)[ \/]([\w.]+)/.exec(a)||/(webkit)[ \/]([\w.]+).*(version)[ \/]([\w.]+).*(safari)[ \/]([\w.]+)/.exec(a)||/(webkit)[ \/]([\w.]+)/.exec(a)||/(opera)(?:.*version|)[ \/]([\w.]+)/.exec(a)||/(msie) ([\w.]+)/.exec(a)||(a.indexOf('trident')>=0&&/(rv)(?::| )([\w.]+)/.exec(a))||(a.indexOf('compatible')<0&&/(mozilla)(?:.*? rv:([\w.]+)|)/.exec(a))||[],c=/(ipad)/.exec(a)||/(ipod)/.exec(a)||/(windows phone)/.exec(a)||/(iphone)/.exec(a)||/(kindle)/.exec(a)||/(silk)/.exec(a)||/(android)/.exec(a)||/(win)/.exec(a)||/(mac)/.exec(a)||/(linux)/.exec(a)||/(cros)/.exec(a)||/(playbook)/.exec(a)||/(bb)/.exec(a)||/(blackberry)/.exec(a)||[],d={},e={browser:b[5]||b[3]||b[1]||'',version:b[2]||b[4]||'0',versionNumber:b[4]||b[2]||'0',platform:c[0]||'',};if((e.browser&&((d[e.browser]=!0),(d.version=e.version),(d.versionNumber=parseInt(e.versionNumber,10))),e.platform&&(d[e.platform]=!0),(d.android||d.bb||d.blackberry||d.ipad||d.iphone||d.ipod||d.kindle||d.playbook||d.silk||d['windows phone'])&&(d.mobile=!0),(d.cros||d.mac||d.linux||d.win)&&(d.desktop=!0),(d.chrome||d.opr||d.safari)&&(d.webkit=!0),d.rv||d.iemobile)){var f='msie';(e.browser=f),(d[f]=!0)}if(d.edge){delete d.edge;var g='msedge';(e.browser=g),(d[g]=!0)}if(d.safari&&d.blackberry){var h='blackberry';(e.browser=h),(d[h]=!0)}if(d.safari&&d.playbook){var i='playbook';(e.browser=i),(d[i]=!0)}if(d.bb){var j='blackberry';(e.browser=j),(d[j]=!0)}if(d.opr){var k='opera';(e.browser=k),(d[k]=!0)}if(d.safari&&d.android){var l='android';(e.browser=l),(d[l]=!0)}if(d.safari&&d.kindle){var m='kindle';(e.browser=m),(d[m]=!0)}if(d.safari&&d.silk){var n='silk';(e.browser=n),(d[n]=!0)}return(d.name=e.browser),(d.platform=e.platform),d}return((window.jQBrowser=b(window.navigator.userAgent)),(window.jQBrowser.uaMatch=b),a&&(a.browser=window.jQBrowser),window.jQBrowser)})

// extends jQuery
!(function ($) {
  if ($.browser.msie) {
    location.replace(rb.baseUrl + '/error/unsupported-browser')
    return
  }

  $.fn.extend({
    button: function (state) {
      return this.each(function () {
        var $el = $(this)
        if (!($el.prop('nodeName') === 'BUTTON' || $el.prop('nodeName') === 'A')) return
        if (state === 'loading') {
          $el.attr('disabled', true)

          var spinner = $.browser.msie ? undefined : $el.data('spinner')
          var loadingText = $el.data('loading-text')
          this.__textHold = $el.html()

          if (loadingText) {
            this.__loadingTimer = setTimeout(function () {
              $el.text(loadingText)
            }, 200)
          } else if (spinner !== undefined) {
            this.__loadingTimer = setTimeout(function () {
              $el.html('<span class="spinner-' + (spinner === 'grow' ? 'grow' : 'border') + '"></span>')
            }, 200)
          }
        } else if (state === 'reset') {
          $el.attr('disabled', false)
          if (this.__loadingTimer) {
            clearTimeout(this.__loadingTimer)
            this.__loadingTimer = null
            if (this.__textHold) $el.html(this.__textHold)
          }
        }
      })
    },
  })

  // RB metas
  window.rb = window.rb || {}
  $('meta[name^="rb."]').each(function (idx, item) {
    var k = $(item).attr('name').substring(3) // remove `rb.`
    var v = $(item).attr('content')
    if (v === 'true') window.rb[k] = true
    else if (v === 'false') window.rb[k] = false
    else window.rb[k] = v || ''
  })

  var rr_prefix = Math.floor(Math.random() * 88888888) + 10000000 + '-'
  $.ajaxSetup({
    headers: {
      'Content-Type': 'text/plain;charset=utf-8',
      'X-Client': 'RB/WEB',
      'X-CsrfToken': rb.csrfToken || '',
      'X-AuthToken': rb.authToken || '',
    },
    cache: false,
    complete: function (xhr) {
      if (!(xhr.status === 200 || xhr.status === 0)) {
        var err = xhr.responseJSON
        if (!err) {
          try {
            err = $.parseJSON(xhr.responseText || '{}')
          } catch (ignore) {
            // Not JSON
          }
        }

        if (err && err.status) {
          if (err.status === 404) err = $L('访问的页面/资源不存在')
          else if (err.status === 403) err = $L('权限不足，访问被阻止')
          else if (err.status === 401) err = $L('未授权访问')
          else err = $L('系统繁忙，请稍后重试')
        }
        if (rb.env === 'dev') console.log('error on complete :', err)
        typeof err === 'string' && RbHighbar.error(err)
      } else {
        var res = xhr.responseJSON
        if (res && res.error_code >= 500) console.error(JSON.stringify(res))
      }
    },
    beforeSend: function (xhr, settings) {
      // URL prefix
      if (settings.url.substring(0, 1) === '/' && rb.baseUrl) settings.url = rb.baseUrl + settings.url
      // v4.1
      xhr.setRequestHeader('X-ReqRandom', rr_prefix + $random())
      return settings
    },
  })

  window.onerror = function () {
    $.post('/error/jslog', JSON.stringify(arguments))
  }

  rb.commercial = ~~rb.commercial
  if (rb.env === 'dev') $('.dev-show').removeClass('dev-show')
  else if (rb.commercial > 1) $('html').addClass('commercial')

  if (rb.appName && rb.appName !== document.title) document.title = document.title + ' · ' + rb.appName
  setTimeout(function () {
    $(document.body).addClass('rb-animate')
  }, 1000)

  // for `cookie`
  $.cookie.defaults = { expires: 14, path: '/', secure: location.protocol === 'https:' }

  // for `watermark`
  if (window.watermark && self === top) {
    var wt = rb._markWatermarkText
    if (wt) wt = JSON.parse(wt)
    if (!wt) wt = [rb.currentUser ? '***' + rb.currentUser.substr(7) : null, rb.appName]
    wt.push(rb._rbTempAuth ? 'TEMP.AUTH 临时授权' : null)

    window.watermark.init({
      watermark_txt: wt,
      watermark_angle: 30,
      watermark_width: 200,
      watermark_font: 'arial',
      watermark_fontsize: '15px',
      watermark_alpha: rb._rbTempAuth ? 0.2 : 0.1,
      watermark_color: rb._rbTempAuth ? 'red' : 'black',
      watermark_parent_width: $(window).width(),
      watermark_parent_height: $(window).height(),
      monitor: true,
    })
    if (!(location.protocol === 'http:' || location.protocol === 'https:')) location.href = 'https://getrebuild.com/'
  }

  rb.locale = rb.locale || 'zh_CN'
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
Array.prototype.toggle = function (item) {
  if (this.contains(item)) this.remove(item)
  else this.push(item)
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

// bootstrap-datetimepicker.min.js 解决不支持中文日期
window.datetimepicker_clearDate41 = function (i) {
  if (i.indexOf('周') > -1) {
    i = i.replace(/ \([^)]*\)/g, '').trim()
  }
  if (i.indexOf('-') > -1) return $trim(i)
  return i.replace(/[年月]/g, '-').replace('日', '')
}

var $setTimeout__timers = {}
/**
 * 不会重复执行的 setTimeout
 */
var $setTimeout = function (e, t, id) {
  if (id && $setTimeout__timers[id]) {
    if (rb.env === 'dev') console.debug('Clear prev setTimeout : ' + id)
    clearTimeout($setTimeout__timers[id])
    $setTimeout__timers[id] = null
  }
  var timer = setTimeout(e, t)
  if (id) $setTimeout__timers[id] = timer
  return timer
}

/**
 * 获取 URL 参数
 */
var $urlp = function (key, qstr) {
  qstr = qstr || window.location.search
  if (!qstr) return !key || key === '*' ? {} : null
  qstr = qstr.replace(/%20/g, ' ')
  qstr = qstr.substring(1) // remove first '?'
  var params = qstr.split('&')
  var map = {}
  for (var i = 0, j = params.length; i < j; i++) {
    var kv = params[i].split('=')
    map[kv[0]] = kv[1]
  }
  return !key || key === '*' ? map : map[key]
}

/**
 * 获取元素值。兼容旧值比较（根据 data-o 属性），如与旧值一致则返回 null
 */
var $val = function (el) {
  var $el = $(el)
  if ($el.length === 0) return null

  var nVal
  var tagName = $el.prop('tagName')
  var isCheckbox = tagName === 'INPUT' && $el.attr('type') === 'checkbox'
  if (tagName === 'INPUT' || tagName === 'TEXTAREA' || tagName === 'SELECT') {
    nVal = isCheckbox ? $el.prop('checked') : $el.val()
  } else {
    nVal = $el.attr('value')
  }

  // 无 data-o 值
  var oVal = $el.data('o')
  if (oVal === undefined || !(oVal + '')) {
    return isCheckbox ? nVal : $trim(nVal) || null
  }

  if (isCheckbox) {
    return nVal === oVal ? null : nVal
  }

  if ($same(nVal, oVal)) return null // unmodified
  // New value is empty
  if (oVal === 0) oVal = '0'
  if (!!oVal && !nVal) return ''
  else return $trim(nVal) || null
}

/**
 * 清理 Map 中的无效值（null、undefined）
 */
var $cleanMap = function (map) {
  var newMap = {}
  for (var k in map) {
    var v = map[k]
    if (!(v === null || v === undefined)) newMap[k] = v
  }
  return newMap
}

/**
 * 清理数组
 */
var $cleanArray = function (array, isunique) {
  if (!array) return []
  var newArray = []
  $(array).each(function (i, item) {
    var n = $trim(item)
    if (n) {
      if (isunique) newArray.remove(n)
      newArray.push(n)
    }
  })
  return newArray
}

/**
 * 清理数字中的符号
 */
function $cleanNumber(n, forceNumber) {
  if (typeof n === 'number') return n
  var m = (n + '').match(/-?\d+(,\d+)*(.\d+)?/g)
  if (m && m[0]) {
    m = m[0].replace(/,/g, '') // 千分位
    return forceNumber ? parseFloat(m) : m
  }
  return NaN
}

/**
 * 格式化数字
 */
function $formatNumber(n, scale) {
  let [i, d] = n.toFixed(scale).split('.')
  i = i.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return d ? `${i}.${d}` : i
}

/**
 * 常用正则
 */
var $regex = {
  _Date: /^[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}$/, // eg. 2010-01-01, 2010-1-9
  _Time: /^[0-9]{1,2}:[0-9]{1,2}(:[0-9]{1,2})?$/, // eg. 16:01:10, 10:1:9
  // eslint-disable-next-line no-useless-escape
  _Url: /^(http|https|ftp)\:\/\/[a-z0-9\-\.]+(:[0-9]*)?\/?([a-z0-9\-\._\?\,\'\/\\\+&amp;%\$#\=~!:])*$/i,
  _Mail: /^[a-z0-9._-]+@[a-z0-9.-]+\.[a-z]{2,6}$/i,
  _Number: /^[-+]?[0-9]+$/, // 数字
  _Decimal: /^[-+]?\d*\.?\d+$/, // 包括小数点的数字
  _Mobile: /^(1[3456789])\d{9}$/, // CN Mobile
  _Tel: /^(\(\d{1,5}\))?(\d{3,4}-)?\d{7,8}(-\d{1,6})?$/, // (国际码)区号-号码-分机
  _Text: /^[a-z\d\u4E00-\u9FA5]+$/i, // 不含特殊字符和标点
  isDate: function (val) {
    return this._Date.test(val)
  },
  isTime: function (val) {
    return this._Time.test(val)
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
  // 是否 ID
  isId: function (id) {
    return /^([0-9]{3}-[a-z0-9]{16})$/gi.test(id)
  },
}

/**
 * URL 编码
 */
var $encode = function (s) {
  if (!s) return ''
  return encodeURIComponent(s)
}
/**
 * URL 解码
 */
var $decode = function (s) {
  if (!s) return ''
  return decodeURIComponent(s)
}

/**
 * localStorage
 */
var $storage = {
  get: function (key) {
    if (window.localStorage) return localStorage.getItem(key)
    else return $.cookie(key)
  },
  set: function (key, val) {
    if (window.localStorage) localStorage.setItem(key, val)
    else $.cookie(key, val, { expires: 365 })
  },
  remove: function (key) {
    if (window.localStorage) localStorage.removeItem(key)
    else $.removeCookie(key)
  },
}

/**
 * 随机数
 */
var $random = function (prefix, alphabetic, maxLength) {
  if (alphabetic) {
    maxLength = maxLength || 24
    var c = prefix || ''
    while (c.length < maxLength) {
      c += Math.random()
        .toString(36)
        .replace(/[^a-f1-9]+/g, '')
    }
    return c.substring(0, maxLength)
  } else {
    var c = (prefix || '') + (Math.floor(Math.random() * 888888888888) + 100000000000)
    return c.substring(0, Math.min(maxLength || 12, 12))
  }
}

/**
 * 是否相同。兼容对象或数组
 */
var $same = function (a, b) {
  if (Object.is(a, b)) return true
  if (a && b) {
    var aType = $type(a)
    var bType = $type(b)
    if ((aType === 'object' && bType === 'object') || (aType === 'array' && bType === 'array')) {
      a = JSON.stringify(a)
      b = JSON.stringify(b)
    }
  }
  if (a === b) return true
  if (a === 0) a = '0'
  if (b === 0) b = '0'
  if (a === true || a === false) a = a + ''
  if (b === true || b === false) b = b + ''
  // eslint-disable-next-line eqeqeq
  return a == b
}
var $is = $same

/**
 * 值是否为空（兼容对象和数组）
 */
var $empty = function (a) {
  if (a === null || a === '' || typeof a === 'undefined') return true
  var aType = $type(a)
  if (aType === 'number') return false
  if (aType === 'array' && a.length === 0) return true
  if (aType === 'object' && Object.keys(a).length === 0) return true
  return $trim(a) === ''
}

/**
 * 停止事件传播
 */
var $stopEvent = function (e, preventDefault) {
  if (e && e.stopPropagation) e.stopPropagation()
  if (e && e.nativeEvent) e.nativeEvent.stopImmediatePropagation()
  preventDefault && e && e.preventDefault()
  return false
}

/**
 * 是否为 true
 */
var $isTrue = function (a) {
  return a === true || a === 'true' || a === 'T'
}

/**
 * 定位到指定元素
 */
var $gotoSection = function (top, target) {
  $(target || 'html').animate({ scrollTop: top || 0 }, 600)
}

/**
 * 节流函数
 */
var $throttle = function (fn, delay) {
  var __self = fn,
    timer,
    firstTime = true

  return function () {
    var args = arguments,
      __me = this

    if (firstTime) {
      __self.apply(__me, args)
      return (firstTime = false)
    }
    if (timer) return false

    timer = setTimeout(function () {
      clearTimeout(timer)
      timer = null
      __self.apply(__me, args)
    }, delay || 200)
  }
}

/**
 * 分时函数
 */
var $timechunk = function (array, fn, count) {
  var start = function () {
    for (var i = 0; i < Math.min(count || 1, array.length); i++) {
      fn(array.shift())
    }
  }

  var timer
  return function () {
    timer = setInterval(function () {
      if (array.length === 0) return clearInterval(timer)
      start()
    }, 20)
  }
}

/**
 * 对象深拷贝
 */
var $clone = function (o) {
  if (!o) return o
  return $.extend(true, {}, o)
}

/**
 * CommonsUtils#escapeHtml
 */
var $escapeHtml = function (s) {
  if (!s) return ''
  return s.replace(/</gi, '&lt;').replace(/>/gi, '&gt;')
}

// 是否浅色（仅支持 Hex 颜色）
// https://stackoverflow.com/questions/12043187/how-to-check-if-hex-color-is-too-black
var $isLight = function (color) {
  var hex = color.replace('#', '')
  var c_r = parseInt(hex.substring(0, 0 + 2), 16)
  var c_g = parseInt(hex.substring(2, 2 + 2), 16)
  var c_b = parseInt(hex.substring(4, 4 + 2), 16)
  var brightness = (c_r * 299 + c_g * 587 + c_b * 114) / 1000
  return brightness > 155
}

// $.trim
var $trim = function (a) {
  if (a === null || typeof a === 'undefined' || a === '') return ''
  return ((a || '') + '').trim()
}

// $.type
var $type = function (a) {
  if (Array.isArray(a)) return 'array'
  return typeof a // string, object
}

// Hex to RGB
var $hex2rgb = function (hex, alpha) {
  hex = hex.replace('#', '')
  var bigint = parseInt(hex, 16)
  var rgb = {
    r: (bigint >> 16) & 255,
    g: (bigint >> 8) & 255,
    b: bigint & 255,
  }
  if (alpha) return 'rgba(' + [rgb.r, rgb.g, rgb.b, alpha].join(',') + ')'
  return rgb
}

// RGB to Hex
var $rgb2hex = function (r, g, b) {
  return ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)
}

// 取消选中
var $clearSelection = function () {
  if (window.getSelection) {
    window.getSelection().removeAllRanges()
  } else if (document.selection) {
    document.selection.empty()
  }
}

// $.getScript use cache
var $getScript = function (url, callback) {
  $.ajax({
    type: 'GET',
    url: url,
    success: callback,
    dataType: 'script',
    cache: true,
    complete: function (xhr) {
      if (!(xhr.status === 200 || xhr.status === 0)) {
        console.error('Failed to load script:', url, xhr)
      }
    },
  })
}

// 绝对 URL
var $isFullUrl = function (url) {
  return url && (url.startsWith('http://') || url.startsWith('https://'))
}

// 环境 @see LoginChannel.java
var $env = {
  // 钉钉
  isDingTalk: function () {
    return navigator.userAgent.match(/(DINGTALK)/i)
  },
  // 企微
  isWxWork: function () {
    return navigator.userAgent.match(/(WXWORK)/i)
  },
}

// 移除 HTML
function $removeHtml(content) {
  return $('<span></span>').html(content).text()
}

// 打开新窗口下载 `window.open`
function $openWindow(url) {
  var handle = window.open(url)
  if (!handle) {
    // 不允许/被阻止
    RbAlert.create(null, {
      onRendered: function () {
        $(this._element)
          .find('.modal-dialog')
          .css('max-width', 400)
          .find('.text-center')
          .html(
            '<div class="mb-2"><h4 class="m-0 mb-2">' +
              $L('文件已准备就绪') +
              '</h4><a class="link" href="' +
              url +
              '" target="_blank"><i class="zmdi zmdi-download icon mr-1"></i>' +
              $L('点击下载') +
              '</a></div>',
          )
      },
    })
  }
}

// 字段颜色
function $tagStyle2(color) {
  if (!color) return null
  return { backgroundColor: color, borderColor: color, color: $isLight(color) ? '#222' : '#fff' }
}

// 格式化秒显示
function $sec2Time(s) {
  if (!s || ~~s <= 0) return '00:00'

  var days
  var hh = Math.floor(s / 3600)
  var mm = Math.floor(s / 60) % 60
  var ss = ~~(s % 60)
  if (~~hh >= 24) {
    days = ~~(hh / 24)
    hh = hh % 24
  }
  if (hh < 10) hh = '0' + hh
  if (mm < 10) mm = '0' + mm
  if (ss < 10) ss = '0' + ss

  var time = [hh, mm, ss].join(':')
  if (days) return $L('%d天', days) + ' ' + time
  else if (hh === '00') return time.substr(3)
  return time
}

// 定位
function $focus2End(el, delay) {
  if (!el) return
  setTimeout(function () {
    el.focus()
    var len = (el.value || '').length
    el.setSelectionRange(len, len)
  }, delay || 100)
}

var $addResizeHandler__cbs = []
/**
 * 窗口 RESIZE 回调
 */
var $addResizeHandler = function (callback) {
  typeof callback === 'function' && $addResizeHandler__cbs && $addResizeHandler__cbs.push(callback)
  return function () {
    if (!$addResizeHandler__cbs || $addResizeHandler__cbs.length === 0) return
    // eslint-disable-next-line no-console
    if (rb.env === 'dev') console.log('Callbacks ' + $addResizeHandler__cbs.length + ' handlers of resize ...')
    $addResizeHandler__cbs.forEach(function (cb) {
      cb()
    })
  }
}

// BASE64 解码
var $base64Encode = function (str) {
  if (typeof str !== 'string') str = JSON.stringify(str)
  var bytes = new TextEncoder().encode(str)
  var bin = ''
  bytes.forEach(function (b) {
    bin += String.fromCharCode(b)
  })
  return btoa(bin)
}
// BASE64 解码
var $base64Decode = function (b64) {
  var bin = atob(b64)
  var bytes = Uint8Array.from(bin, function (c) {
    return c.charCodeAt(0)
  })
  return new TextDecoder().decode(bytes)
}
