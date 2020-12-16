/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */

// PAGE INITIAL
$(function () {
  var t = $('.rb-scroller')
  t.perfectScrollbar()

  $addResizeHandler(function () {
    if ($.browser.msie) $('.left-sidebar-scroll').height($('.left-sidebar-spacer').height())
    t.perfectScrollbar('update')
  })()

  // tooltip
  $('[data-toggle="tooltip"]').tooltip()

  // In top-frame
  if ($('.rb-left-sidebar').length > 0) {
    $('.sidebar-elements>li>a').each(function () {
      var $this = $(this)
      $this.tooltip({
        placement: 'right',
        title: $this.find('span').text().trim(),
        delay: 200,
      })
    })
    _initNavs()
    setTimeout(_globalSearch, 200)
  }

  var hasNotification = $('.J_top-notifications')
  if (hasNotification.length > 0) {
    $unhideDropdown(hasNotification).on('shown.bs.dropdown', _loadMessages)
    setTimeout(_checkMessage, 2000)
  }

  var hasUser = $('.J_top-user')
  if (hasUser.length > 0) {
    $unhideDropdown(hasUser)
  }

  if (rb.isAdminUser) {
    var topPopover = function (el, content) {
      var pop_show_timer
      var pop_hide_timer
      var $pop = $(el)
        .popover({
          trigger: 'manual',
          placement: 'bottom',
          html: true,
          content: content,
          delay: { show: 200, hide: 0 },
        })
        .on('mouseenter', function () {
          pop_hide_timer && clearTimeout(pop_hide_timer)
          pop_show_timer = setTimeout(function () {
            $pop.popover('show')
          }, 200)
        })
        .on('mouseleave', function () {
          pop_show_timer && clearTimeout(pop_show_timer)
          pop_hide_timer = setTimeout(function () {
            $pop.popover('hide')
          }, 200)
        })
        .on('shown.bs.popover', function () {
          $('#' + $(this).attr('aria-describedby'))
            .find('.popover-body')
            .off('mouseenter')
            .off('mouseleave')
            .on('mouseenter', function () {
              pop_hide_timer && clearTimeout(pop_hide_timer)
              $pop.popover('show')
            })
            .on('mouseleave', function () {
              pop_show_timer && clearTimeout(pop_show_timer)
              $pop.popover('hide')
            })
        })
      return $pop
    }

    $('html').addClass('admin')
    if (rb.isAdminVerified !== true) $('.admin-verified').remove()
    if (location.href.indexOf('/admin/') > -1) $('.admin-settings').remove()
    else if (rb.isAdminVerified) {
      $('.admin-settings a>.icon').addClass('text-danger')
      topPopover($('.admin-settings a'), '<div class="p-1">' + $L('CancelYourAdminAccess').replace('#', 'javascript:_cancelAdmin()') + '</div>')
    }

    $.get('/user/admin-dangers', function (res) {
      if ((res.data || []).length > 0) {
        $('.admin-danger').removeClass('hide')
        var dd = []
        $(res.data).each(function () {
          dd.push('<div class="p-1">' + this + '</div>')
        })
        topPopover($('.admin-danger a'), dd.join(''))
      }
    })
  } else {
    $('.admin-show, .admin-danger').remove()
  }

  var bosskey = 0
  $(document.body).keydown(function (e) {
    if (e.shiftKey) {
      if (++bosskey === 6) {
        $('.bosskey-show').removeClass('bosskey-show')
        $.cookie('_USEBOSSKEY', 666, { expires: null, httpOnly: true })
      }
    }
  })
  if ($.cookie('_USEBOSSKEY')) $('.bosskey-show').removeClass('bosskey-show')

  // Trigger on window.onresize
  $(window).on('resize', function () {
    $setTimeout(
      function () {
        $addResizeHandler()()
      },
      120,
      'resize-window'
    )
  })

  // Help link in page
  var helpLink = $('meta[name="page-help"]').attr('content')
  if (helpLink) $('.page-help>a').attr('href', helpLink)
})

var $addResizeHandler__calls = []
/**
 * 窗口 RESIZE 回调
 */
var $addResizeHandler = function (call) {
  typeof call === 'function' && $addResizeHandler__calls && $addResizeHandler__calls.push(call)
  return function () {
    if (!$addResizeHandler__calls || $addResizeHandler__calls.length === 0) return
    // eslint-disable-next-line no-console
    if (rb.env === 'dev') console.log('Calls ' + $addResizeHandler__calls.length + ' handlers of resize ...')
    $addResizeHandler__calls.forEach(function (call) {
      call()
    })
  }
}

/**
 * 取消管理员访问
 */
var _cancelAdmin = function () {
  $.post('/user/admin-cancel', function (res) {
    if (res.error_code === 0) {
      $('.admin-settings a>.icon').removeClass('text-danger')
      $('.admin-settings a').popover('dispose')
      rb.isAdminVerified = false
    }
  })
}

/**
 * 初始化导航菜单
 */
var _initNavs = function () {
  var isOffcanvas = $('.rb-offcanvas-menu').length > 0 // Float mode

  // Nav
  if (isOffcanvas) {
    $('.rb-toggle-left-sidebar').click(function () {
      $(document.body).toggleClass('open-left-sidebar')
      return false
    })
    $('.sidebar-elements>li>a').tooltip('disable')
  } else {
    const $el = $('.rb-collapsible-sidebar')
    if (!$el.hasClass('rb-collapsible-sidebar-collapsed')) {
      $('.sidebar-elements>li>a').tooltip('disable')
    }

    $('.rb-toggle-left-sidebar').click(function () {
      $el.toggleClass('rb-collapsible-sidebar-collapsed')
      $.cookie('rb.sidebarCollapsed', $el.hasClass('rb-collapsible-sidebar-collapsed'), { expires: 180 })

      $('.sidebar-elements>li>a').tooltip('toggleEnabled')
      $addResizeHandler()()
    })
  }

  // SubNavs
  var currsntSubnav
  $('.sidebar-elements li.parent').click(function (e) {
    e.preventDefault()
    e.stopPropagation()
    var _this = $(this)
    _this.toggleClass('open')
    var $sub = _this.find('.sub-menu')
    // if (!$sub.hasClass('visible')) {
    //   var subHeight = $sub.height()
    //   $sub.css({ height: 0, overflow: 'hidden' })
    //   $sub.animate({ height: subHeight + 22 }, 200)
    // }
    $sub.toggleClass('visible')
    currsntSubnav = _this
    _this.find('a').eq(0).tooltip('hide')
    $('.left-sidebar-scroll').perfectScrollbar('update')
  })
  $('.sidebar-elements li.parent .sub-menu').click(function (e) {
    e.stopPropagation()
  })
  $(document.body).click(function () {
    // MinNav && SubnavOpen
    if ($('.rb-collapsible-sidebar').hasClass('rb-collapsible-sidebar-collapsed') && currsntSubnav && currsntSubnav.hasClass('open')) {
      currsntSubnav.removeClass('open')
      currsntSubnav.find('.sub-menu').removeClass('visible')
    }
    if (isOffcanvas) {
      $(document.body).removeClass('open-left-sidebar')
    }
  })

  var activeNav = $('.sidebar-elements li.active')
  if (!(activeNav.attr('class') || '').contains('nav_entity-') && activeNav.parents('li.parent').length > 0) {
    activeNav.parents('li.parent').addClass('active').first().trigger('click')
    $(document.body).trigger('click')
  }

  $('.nav-settings').click(function () {
    RbModal.create('/p/settings/nav-settings', $L('SetSome,NavMenu'))
  })

  // WHEN SMALL-WIDTH
  $('.left-sidebar-toggle')
    .click(function () {
      $('.rb-collapsible-sidebar').toggleClass('rb-collapsible-sidebar-collapsed')
      $('.left-sidebar-spacer').toggleClass('open')
    })
    .text($('.rb-right-navbar .page-title').text())

  if ($('.page-aside .aside-header').length > 0) {
    $('.page-aside .aside-header').click(function () {
      $(this).toggleClass('collapsed')
      $('.page-aside .aside-nav').toggleClass('show')
    })
  }
}

var _checkMessage__state = 0
/**
 * 检查新消息
 */
var _checkMessage = function () {
  $.get('/notification/check-state', function (res) {
    if (res.error_code > 0) return

    $('.J_top-notifications .badge').text(res.data.unread)
    if (res.data.unread > 0) $('.J_top-notifications .indicator').removeClass('hide')
    else $('.J_top-notifications .indicator').addClass('hide')

    if (_checkMessage__state !== res.data.unread) {
      _checkMessage__state = res.data.unread
      if (_checkMessage__state > 0) {
        if (!window.__title) window.__title = document.title
        document.title = '(' + _checkMessage__state + ') ' + window.__title
        if (rb.env === 'dev') _showNotification()
      }
      _loadMessages__state = false
    }

    setTimeout(_checkMessage, rb.env === 'dev' ? 60 * 10000 : 2000)
  })
}
var _loadMessages__state = false
var _loadMessages = function () {
  if (_loadMessages__state) return
  var dest = $('.rb-notifications .content ul').empty()
  if (dest.find('li').length === 0) {
    $('<li class="text-center mt-3 mb-3"><i class="zmdi zmdi-refresh zmdi-hc-spin fs-18"></i></li>').appendTo(dest)
  }

  $.get('/notification/messages?pageSize=10&preview=true', function (res) {
    dest.empty()
    $(res.data).each(function (idx, item) {
      var o = $('<li class="notification"></li>').appendTo(dest)
      if (item[3] === true) o.addClass('notification-unread')
      o = $('<a class="a" href="' + rb.baseUrl + '/notifications#' + (item[3] ? 'unread=' : 'all=') + item[4] + '"></a>').appendTo(o)
      $('<div class="image"><img src="' + rb.baseUrl + '/account/user-avatar/' + item[0][0] + '" alt="Avatar"></div>').appendTo(o)
      o = $('<div class="notification-info"></div>').appendTo(o)
      $('<div class="text text-truncate">' + item[1] + '</div>').appendTo(o)
      $('<span class="date">' + $fromNow(item[2]) + '</span>').appendTo(o)
    })
    _loadMessages__state = true
    if (res.data.length === 0) $('<li class="text-center mt-4 mb-4 text-muted">' + $L('NoSome,Notification') + '</li>').appendTo(dest)
  })
}
var _showNotification = function () {
  if ($.cookie('grantedNotification')) return
  var _Notification = window.Notification || window.mozNotification || window.webkitNotification
  if (_Notification) {
    if (_Notification.permission === 'granted') {
      new _Notification($L('HasXNotice').replace('%d', _checkMessage__state), {
        tag: 'rbNotification',
        icon: rb.baseUrl + '/assets/img/favicon.png',
      })
      $.cookie('grantedNotification', 666, { expires: null, httpOnly: true }) // session cookie
    } else {
      _Notification.requestPermission()
    }
  }
}

/**
 * 全局搜索
 */
var _globalSearch = function () {
  $('.sidebar-elements li').each(function (idx, item) {
    if (idx > 40) return false
    var $item = $(item)
    if (!$item.hasClass('parent') && ($item.attr('class') || '').contains('nav_entity-')) {
      var $a = $item.find('>a')
      $('<a class="text-truncate" data-url="' + $a.attr('href') + '">' + $a.text() + '</a>').appendTo('.search-models')
    } else if ($item.hasClass('nav_entity-PROJECT') && $item.hasClass('parent')) {
      var $a = $item.find('>a')
      $('<a class="text-truncate QUERY" data-url="' + rb.baseUrl + '/project/search">' + $a.text() + '</a>').appendTo('.search-models')
    }
  })

  var aModels = $('.search-models a').click(function () {
    var s = $('.search-input-gs').val()
    location.href = $(this).data('url') + ($(this).hasClass('QUERY') ? '?' : '#') + 'gs=' + $encode(s)
  })
  if (aModels.length === 0) return

  $(document).click(function (e) {
    if ($(e.target).parents('.search-container').length === 0) $('.search-models').hide()
  })

  var _tryActive = function ($active, $el) {
    if ($el.length === 1) {
      $active.removeClass('active')
      $el.addClass('active')
    }
  }

  aModels.eq(0).addClass('active')
  $('.search-container input')
    .on('focus', function (e) {
      $('.search-models').show()
    })
    .on('keydown', function (e) {
      if (e.keyCode === 37) {
        var $active = $('.search-models .active')
        _tryActive($active, $active.prev())
      } else if (e.keyCode === 39) {
        var $active = $('.search-models .active')
        _tryActive($active, $active.next())
      } else if (e.keyCode === 13) {
        var s = $('.search-input-gs').val()
        var $active = $('.search-models .active')
        location.href = $active.data('url') + ($active.hasClass('QUERY') ? '?' : '#') + 'gs=' + $encode(s)
      }
    })
}

/**
 * 清理 dropdown 菜单
 */
var $cleanMenu = function (mbg) {
  mbg = $(mbg)
  var mbgMenu = mbg.find('.dropdown-menu')
  var first = mbgMenu.children().first()
  if (first.hasClass('dropdown-divider')) first.remove()
  var last = mbgMenu.children().last()
  if (last.hasClass('dropdown-divider')) last.remove()

  $(mbgMenu.children()).each(function () {
    var item = $(this)
    if (item.hasClass('hide')) item.remove()
  })

  // remove btn
  if (mbgMenu.children().length === 0) mbg.remove()
}

/**
 * 获取附件文件名
 */
var $fileCutName = function (fileName) {
  fileName = fileName.split('?')[0]
  fileName = fileName.split('/')
  fileName = fileName[fileName.length - 1]
  return fileName.substr(fileName.indexOf('__') + 2)
}

/**
 * 获取附件文件扩展名
 */
var $fileExtName = function (fileName) {
  fileName = (fileName || '').toLowerCase()
  fileName = fileName.split('?')[0]
  fileName = fileName.split('.')
  return fileName[fileName.length - 1] || '*'
}

/**
 * 创建 Upload 组件（自动判断使用七牛或本地）
 */
var $createUploader = function (input, next, complete, error) {
  input = $(input).off('change')
  var imgOnly = input.attr('accept') === 'image/*'
  var temp = input.data('temp') // Temp file
  if (window.qiniu && rb.storageUrl && !temp) {
    input.on('change', function () {
      var file = this.files[0]
      if (!file) return

      var putExtra = imgOnly ? { mimeType: ['image/png', 'image/jpeg', 'image/gif', 'image/bmp'] } : null
      $.get('/filex/qiniu/upload-keys?file=' + $encode(file.name), function (res) {
        var o = qiniu.upload(file, res.data.key, res.data.token, putExtra)
        o.subscribe({
          next: function (res) {
            typeof next === 'function' && next({ percent: res.total.percent })
          },
          error: function (err) {
            var msg = (err.message || err.error || 'UnknowError').toUpperCase()
            if (imgOnly && msg.contains('FILE TYPE')) {
              RbHighbar.create($L('PlsUploadImg'))
            } else if (msg.contains('EXCEED FSIZELIMIT')) {
              RbHighbar.create($L('ExceedMaxLimit') + ' (100MB)')
            } else {
              RbHighbar.error($L('ErrorUpload') + ' : ' + msg)
            }
            typeof error === 'function' && error()
            return false
          },
          complete: function (res) {
            if (file.size > 0) $.post('/filex/store-filesize?fs=' + file.size + '&fp=' + $encode(res.key))
            typeof complete === 'function' && complete({ key: res.key })
          },
        })
      })
    })
  } else {
    input.html5Uploader({
      name: input.attr('id') || input.attr('name') || 'H5Upload',
      postUrl: rb.baseUrl + '/filex/upload?type=' + (imgOnly ? 'image' : 'file') + '&temp=' + (temp || ''),
      onSelectError: function (file, err) {
        if (err === 'ErrorType') {
          RbHighbar.create($L(imgOnly ? 'PlsUploadImg' : 'FileTypeError'))
          return false
        } else if (err === 'ErrorMaxSize') {
          RbHighbar.create($L('ExceedMaxLimit'))
          return false
        }
      },
      onClientLoad: function (e, file) {},
      onClientProgress: function (e, file) {
        typeof next === 'function' && next({ percent: (e.loaded * 100) / e.total })
      },
      onSuccess: function (e, file) {
        e = $.parseJSON(e.currentTarget.response)
        if (e.error_code === 0) {
          if (!temp && file.size > 0) $.post('/filex/store-filesize?fs=' + file.size + '&fp=' + $encode(e.data))
          complete({ key: e.data })
        } else {
          RbHighbar.error($L('ErrorUpload'))
          typeof error === 'function' && error()
        }
      },
      onClientError: function (e, file) {
        RbHighbar.error($L('ErrorUpload'))
        typeof error === 'function' && error()
      },
    })
  }
}

/**
 * 卸载 React 组件
 */
var $unmount = function (container, delay, keepContainer) {
  if (container && container[0]) {
    setTimeout(function () {
      ReactDOM.unmountComponentAtNode(container[0])
      if (keepContainer !== true && container.prop('tagName') !== 'BODY') container.remove()
    }, delay || 1000)
  }
}

/**
 * 初始化引用字段（搜索）
 */
var $initReferenceSelect2 = function (el, field) {
  var search_input = null
  return $(el).select2({
    placeholder: field.placeholder || $L('SelectSome').replace('{0}', field.label),
    minimumInputLength: 0,
    maximumSelectionLength: $(el).attr('multiple') ? 999 : 2,
    ajax: {
      url: '/commons/search/' + (field.searchType || 'reference'),
      delay: 300,
      data: function (params) {
        search_input = params.term
        return { entity: field.entity, field: field.name, q: params.term }
      },
      processResults: function (data) {
        return { results: data.data }
      },
    },
    language: {
      noResults: function () {
        return (search_input || '').length > 0 ? $L('NoResults') : $L('InputForSearch')
      },
      inputTooShort: function () {
        return $L('InputForSearch')
      },
      searching: function () {
        return $L('Searching')
      },
      maximumSelected: function () {
        return $L('OnlyXSelected').replace('%d', 1)
      },
      removeAllItems: function () {
        return $L('Clean')
      },
    },
    theme: 'default ' + (field.appendClass || ''),
  })
}

/**
 * 保持模态窗口（如果需要）
 */
var $keepModalOpen = function () {
  if ($('.rbmodal.show, .rbview.show').length > 0) {
    var $body = $(document.body)
    if (!$body.hasClass('modal-open')) $body.addClass('modal-open').css({ 'padding-right': 17 })
    return true
  }
  return false
}

/**
 * 禁用按钮 N 秒（用在一些危险操作上）
 */
var $countdownButton = function (btn, seconds) {
  seconds = seconds || 5
  var text = btn.attr('disabled', true).text()
  btn.text(text + ' (' + seconds + ')')
  var timer = setInterval(function () {
    if (--seconds === 0) {
      clearInterval(timer)
      btn.attr('disabled', false).text(text)
    } else {
      btn.text(text + ' (' + seconds + ')')
    }
  }, 1000)
}

/**
 * 加载状态条（单线程）
 */
var $mp = {
  _timer: null,
  _mp: null,
  // 开始
  start: function () {
    $mp._timer = setTimeout(function () {
      $mp._mp = new Mprogress({ template: 3, start: true })
    }, 600)
  },
  // 结束
  end: function () {
    if ($mp._timer) {
      clearTimeout($mp._timer)
      $mp._timer = null
    }
    if ($mp._mp) {
      $mp._mp.end()
      $mp._mp = null
    }
  },
}

var EMOJIS = {
  '赞': 'rb_zan.png',
  '握手': 'rb_woshou.png',
  '耶': 'rb_ye.png',
  '抱拳': 'rb_baoquan.png',
  'OK': 'rb_ok.png',
  '拍手': 'rb_paishou.png',
  '拜托': 'rb_baituo.png',
  '差评': 'rb_chaping.png',
  '微笑': 'rb_weixiao.png',
  '撇嘴': 'rb_piezui.png',
  '花痴': 'rb_huachi.png',
  '发呆': 'rb_fadai.png',
  '得意': 'rb_deyi.png',
  '大哭': 'rb_daku.png',
  '害羞': 'rb_haixiu.png',
  '闭嘴': 'rb_bizui.png',
  '睡着': 'rb_shuizhao.png',
  '敬礼': 'rb_jingli.png',
  '崇拜': 'rb_chongbai.png',
  '抱抱': 'rb_baobao.png',
  '忍住不哭': 'rb_renzhubuku.png',
  '尴尬': 'rb_ganga.png',
  '发怒': 'rb_fanu.png',
  '调皮': 'rb_tiaopi.png',
  '开心': 'rb_kaixin.png',
  '惊讶': 'rb_jingya.png',
  '呵呵': 'rb_hehe.png',
  '思考': 'rb_sikao.png',
  '哭笑不得': 'rb_kuxiaobude.png',
  '抓狂': 'rb_zhuakuang.png',
  '呕吐': 'rb_outu.png',
  '偷笑': 'rb_touxiao.png',
  '笑哭了': 'rb_xiaokule.png',
  '白眼': 'rb_baiyan.png',
  '傲慢': 'rb_aoman.png',
  '饥饿': 'rb_jie.png',
  '困': 'rb_kun.png',
  '吓': 'rb_xia.png',
  '流汗': 'rb_liuhan.png',
  '憨笑': 'rb_hanxiao.png',
  '悠闲': 'rb_youxian.png',
  '奋斗': 'rb_fendou.png',
  '咒骂': 'rb_zhouma.png',
  '疑问': 'rb_yiwen.png',
  '嘘': 'rb_xu.png',
  '晕': 'rb_yun.png',
  '惊恐': 'rb_jingkong.png',
  '衰': 'rb_shuai.png',
  '骷髅': 'rb_kulou.png',
  '敲打': 'rb_qiaoda.png',
  '再见': 'rb_zaijian.png',
  '无语': 'rb_wuyu.png',
  '抠鼻': 'rb_koubi.png',
  '鼓掌': 'rb_guzhang.png',
  '糗大了': 'rb_qiudale.png',
  '猥琐的笑': 'rb_weisuodexiao.png',
  '哼': 'rb_heng.png',
  '不爽': 'rb_bushuang.png',
  '打哈欠': 'rb_dahaqian.png',
  '鄙视': 'rb_bishi.png',
  '委屈': 'rb_weiqu.png',
  '安慰': 'rb_anwei.png',
  '坏笑': 'rb_huaixiao.png',
  '亲亲': 'rb_qinqin.png',
  '冷汗': 'rb_lenghan.png',
  '可怜': 'rb_kelian.png',
  '生病': 'rb_shengbing.png',
  '愉快': 'rb_yukuai.png',
  '幸灾乐祸': 'rb_xingzailehuo.png',
  '大便': 'rb_dabian.png',
  '干杯': 'rb_ganbei.png',
  '钱': 'rb_qian.png',
}
/**
 * 转换文字 emoji 为 img 标签
 */
var $converEmoji = function (text) {
  var es = text.match(/\[(.+?)\]/g)
  if (!es) return text
  $(es).each(function () {
    var key = this.substr(1, this.length - 2)
    if (EMOJIS[key]) {
      var img = '<img class="emoji" src="' + rb.baseUrl + '/assets/img/emoji/' + EMOJIS[key] + '" />'
      text = text.replace(this, img)
    }
  })
  return text
}

/**
 * Use momentjs
 */
var $moment = function (date) {
  if (!date || !window.moment) return null
  return moment(date.split('UTC')[0].trim())
}
/**
 * 友好时间显示
 */
var $fromNow = function (date) {
  var m = $moment(date)
  return Math.abs(moment().diff(m)) < 6000 ? $L('JustNow'): m.fromNow()
}
/**
 * 是否过期
 */
var $expired = function (date, offset) {
  var m = $moment(date)
  if (offset) m.add(offset, 's')
  return m.isBefore(moment())
}

/**
 * 转义 Thymeleaf 页面的 JSON
 */
var _$unthy = function (text) {
  if (!text) return null
  text = text.replace(/&quot;/g, '"')
  text = text.replace(/\n/g, ' ')
  return $.parseJSON(text)
}

/**
 * 获取语言
 */
var $L = function () {
  var args = arguments.length === 1 ? arguments[0].split(',') : arguments
  var lang = _$L(args[0])
  if (args.length < 2) return lang

  for (var i = 1; i < args.length; i++) {
    if (args[i]) {
      var phLang = _$L(args[i])
      lang = lang.replace('{' + (i - 1) + '}', phLang)
    }
  }
  return lang
}
var _$L = function (key) {
  var lang = (window._LANGBUNDLE || {})[key]
  if (!lang) {
    console.warn('Missing lang-key `' + key + '`')
    lang = '[' + key.toUpperCase() + ']'
  }
  return lang
}

/**
 * 点击 Dropdown-Menu 不隐藏
 */
var $unhideDropdown = function (dp) {
  return $(dp).on({
    'hide.bs.dropdown': function (e) {
      if (!e.clickEvent || !e.clickEvent.target) return
      var $target = $(e.clickEvent.target)
      if ($target.hasClass('dropdown-menu') || $target.parents('.dropdown-menu').length === 1) {
        return false
      }
    },
  })
}
