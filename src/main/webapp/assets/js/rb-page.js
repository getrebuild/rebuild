/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

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
      var _this = $(this)
      _this.tooltip({
        placement: 'right',
        title: _this.find('span').text().trim(),
        delay: 200
      })
    })
    __initNavs()
    setTimeout(__globalSearch, 200)
  }

  if (rb.isAdminUser) {
    var topPopover = function (el, content) {
      var pop_show_timer
      var pop_hide_timer
      var pop = $(el).popover({
        trigger: 'manual',
        placement: 'bottom',
        html: true,
        content: content,
        delay: { show: 200, hide: 0 }
      }).on('mouseenter', function () {
        pop_hide_timer && clearTimeout(pop_hide_timer)
        pop_show_timer = setTimeout(function () { pop.popover('show') }, 200)
      }).on('mouseleave', function () {
        pop_show_timer && clearTimeout(pop_show_timer)
        pop_hide_timer = setTimeout(function () { pop.popover('hide') }, 200)
      }).on('shown.bs.popover', function (e) {
        $('#' + $(this).attr('aria-describedby')).find('.popover-body')
          .off('mouseenter')
          .off('mouseleave')
          .on('mouseenter', function () {
            pop_hide_timer && clearTimeout(pop_hide_timer)
            pop.popover('show')
          })
          .on('mouseleave', function () {
            pop_show_timer && clearTimeout(pop_show_timer)
            pop.popover('hide')
          })
      })
    }

    $('html').addClass('admin')
    if (rb.isAdminVerified !== true) $('.admin-verified').remove()
    if (location.href.indexOf('/admin/') > -1) $('.admin-settings').remove()
    else if (rb.isAdminVerified) {
      $('.admin-settings a>.icon').addClass('text-danger')
      topPopover($('.admin-settings a'), '<div class="p-1">当前已启用管理员访问功能，如不再使用建议你 <a href="javascript:;" onclick="__cancelAdmin()">取消访问</a></div>')
    }

    $.get('/user/admin-dangers', function (res) {
      if ((res.data || []).length > 0) {
        $('.admin-danger').removeClass('hide')
        var dd = []
        $(res.data).each(function () { dd.push('<div class="p-1">' + this + '</div>') })
        topPopover($('.admin-danger a'), dd.join(''))
      }
    })

  } else {
    $('.admin-show, .admin-danger').remove()
  }

  if ($('.J_notifications-top').length > 0) {
    setTimeout(__checkMessage, 2000)
    $('.J_notifications-top').on('shown.bs.dropdown', __loadMessages)
  }

  var bkeydown_times = 0
  $(document.body).keydown(function (e) {
    if (e.shiftKey) {
      if (++bkeydown_times === 6) $('.bosskey-show').show()
      command_exec(bkeydown_times)
    }
  })

  // Trigger on window.onresize
  $(window).on('resize', function () {
    $setTimeout(function () { $addResizeHandler()() }, 120, 'resize-window')
  })

  // Help link in page
  var helpLink = $('meta[name="page-help"]').attr('content')
  if (helpLink) $('.page-help>a').attr('href', helpLink)

})
// @t - trigger times
var command_exec = function (t) { }

var __ONRESIZE_CALLS = []
var $addResizeHandler = function (call) {
  (typeof call === 'function' && __ONRESIZE_CALLS) && __ONRESIZE_CALLS.push(call)
  return function () {
    if (!__ONRESIZE_CALLS || __ONRESIZE_CALLS.length === 0) return
    // eslint-disable-next-line no-console
    if (rb.env === 'dev') console.log('Calls ' + __ONRESIZE_CALLS.length + ' handlers of resize ...')
    __ONRESIZE_CALLS.forEach(function (call) { call() })
  }
}

// 取消管理员访问
var __cancelAdmin = function () {
  $.post('/user/admin-cancel', function (res) {
    if (res.error_code === 0) {
      // location.reload()
      $('.admin-settings a>.icon').removeClass('text-danger')
      $('.admin-settings a').popover('dispose')
      rb.isAdminVerified = false
    }
  })
}

// MainNav
var __initNavs = function () {
  var isOffcanvas = $('.rb-offcanvas-menu').length > 0 // Float mode

  // Nav
  if (isOffcanvas) {
    $('.rb-toggle-left-sidebar').click(function () {
      $(document.body).toggleClass('open-left-sidebar')
      return false
    })
    $('.sidebar-elements>li>a').tooltip('disable')
  } else {
    $('.rb-toggle-left-sidebar').click(function () {
      var el = $('.rb-collapsible-sidebar').toggleClass('rb-collapsible-sidebar-collapsed')
      $storage.set('rb-sidebar-collapsed', el.hasClass('rb-collapsible-sidebar-collapsed'))
      $('.sidebar-elements>li>a').tooltip('toggleEnabled')
      $(window).trigger('resize')
    })
    if ($storage.get('rb-sidebar-collapsed') === 'true') {
      $('.rb-collapsible-sidebar').addClass('rb-collapsible-sidebar-collapsed')
    } else {
      $('.sidebar-elements>li>a').tooltip('disable')
    }
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
    RbModal.create(rb.baseUrl + '/p/commons/nav-settings', '设置导航菜单')
  })

  // WHEN SMALL-WIDTH
  $('.left-sidebar-toggle').click(function () {
    $('.rb-collapsible-sidebar').toggleClass('rb-collapsible-sidebar-collapsed')
    $('.left-sidebar-spacer').toggleClass('open')
  }).text($('.rb-right-navbar .page-title').text())

  if ($('.page-aside .aside-header').length > 0) {
    $('.page-aside .aside-header').click(function () {
      $(this).toggleClass('collapsed')
      $('.page-aside .aside-nav').toggleClass('show')
    })
  }
}

// Notification
var __checkMessage__state = 0
var __checkMessage = function () {
  $.get('/notification/check-state', function (res) {
    if (res.error_code > 0) return
    $('.J_notifications-top .badge').text(res.data.unread)
    if (res.data.unread > 0) $('.J_notifications-top .indicator').removeClass('hide')
    else $('.J_notifications-top .indicator').addClass('hide')

    if (__checkMessage__state !== res.data.unread) {
      __checkMessage__state = res.data.unread
      if (__checkMessage__state > 0) {
        if (!window.__doctitle) window.__doctitle = document.title
        document.title = '(' + __checkMessage__state + ') ' + window.__doctitle
        if (rb.env === 'dev') __showNotification()
      }
      __loadMessages_state = false
    }

    setTimeout(__checkMessage, rb.env === 'dev' ? 60 * 10000 : 2000)
  })
}
var __loadMessages_state = false
var __loadMessages = function () {
  if (__loadMessages_state) return
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
      $('<span class="date">' + item[2] + '</span>').appendTo(o)
    })
    __loadMessages_state = true
    if (res.data.length === 0) $('<li class="text-center mt-4 mb-4 text-muted">暂无消息</li>').appendTo(dest)
  })
}
var __showNotification = function () {
  if ($.cookie('rb.showNotification')) return
  var _Notification = window.Notification || window.mozNotification || window.webkitNotification
  if (_Notification) {
    if (_Notification.permission === 'granted') {
      new _Notification('你有 ' + __checkMessage__state + ' 条未读消息', {
        tag: 'rbNotification',
        icon: rb.baseUrl + '/assets/img/favicon.png',
      })
      $.cookie('rb.showNotification', 1, { expires: null })  // session cookie
    } else {
      _Notification.requestPermission()
    }
  }
}

// Global searchs
var __globalSearch = function () {
  $('.sidebar-elements li').each(function (idx, item) {
    if (idx > 40) return false
    if (!$(item).hasClass('parent') && ($(item).attr('class') || '').contains('nav_entity-')) {
      var $a = $(item).find('a')
      $('<a class="text-truncate" data-url="' + $a.attr('href') + '">' + $a.text() + '</a>').appendTo('.search-models')
    }
  })

  var activeModel
  var aModels = $('.search-models a').click(function () {
    var s = $('.search-input-gs').val()
    location.href = $(this).data('url') + '#gs=' + $encode(s)
  })
  if (aModels.length === 0) return
  activeModel = aModels.eq(0).addClass('active')

  $(document).click(function (e) {
    if ($(e.target).parents('.search-container').length === 0) $('.search-models').hide()
  })
  $('.search-container input').on('focus', function (e) {
    $('.search-models').show()
  }).on('keydown', function (e) {
    var s = $('.search-input-gs').val()
    if (e.keyCode === 13 && s) location.href = activeModel.data('url') + '#gs=' + $encode(s)
  })
}

// @mbg = .btn-group
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

var $fileCutName = function (fileName) {
  fileName = fileName.split('?')[0]
  fileName = fileName.split('/')
  fileName = fileName[fileName.length - 1]
  return fileName.substr(fileName.indexOf('__') + 2)
}
var $fileExtName = function (fileName) {
  fileName = (fileName || '').toLowerCase()
  fileName = fileName.split('?')[0]
  fileName = fileName.split('.')
  return fileName[fileName.length - 1] || '*'
}

// Use H5 or Qiuniu
var $createUploader = function (input, next, complete, error) {
  input = $(input).off('change')
  var imgOnly = input.attr('accept') === 'image/*'
  var temp = input.data('temp')  // 临时文件
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
              RbHighbar.create('请上传图片')
            } else if (msg.contains('EXCEED FSIZELIMIT')) {
              RbHighbar.create('超出文件大小限制 (100M)')
            } else {
              RbHighbar.error('上传失败: ' + msg)
            }
            typeof error === 'function' && error()
            return false
          },
          complete: function (res) {
            $.post('/filex/store-filesize?fs=' + file.size + '&fp=' + $encode(res.key))
            typeof complete === 'function' && complete({ key: res.key })
          }
        })
      })
    })
  } else {
    input.html5Uploader({
      name: input.attr('id') || input.attr('name') || 'H5Upload',
      postUrl: rb.baseUrl + '/filex/upload?type=' + (imgOnly ? 'image' : 'file') + '&temp=' + (temp || ''),
      onSelectError: function (file, err) {
        if (err === 'ErrorType') {
          RbHighbar.create(imgOnly ? '请上传图片' : '文件格式错误')
          return false
        } else if (err === 'ErrorMaxSize') {
          RbHighbar.create('超出文件大小限制')
          return false
        }
      },
      onClientLoad: function (e, file) { },
      onClientProgress: function (e, file) {
        typeof next === 'function' && next({ percent: e.loaded * 100 / e.total })
      },
      onSuccess: function (e, file) {
        e = $.parseJSON(e.currentTarget.response)
        if (e.error_code === 0) {
          if (!temp) $.post('/filex/store-filesize?fs=' + file.size + '&fp=' + $encode(e.data))
          complete({ key: e.data })
        } else {
          RbHighbar.error('上传失败，请稍后重试')
          typeof error === 'function' && error()
        }
      },
      onClientError: function (e, file) {
        RbHighbar.error('网络错误，请稍后重试')
        typeof error === 'function' && error()
      }
    })
  }
}

// Clear React node
var $unmount = function (container, delay, keepContainer) {
  if (container && container[0]) {
    setTimeout(function () {
      ReactDOM.unmountComponentAtNode(container[0])
      if (keepContainer !== true && container.prop('tagName') !== 'BODY') container.remove()
    }, delay || 1000)
  }
}

// 初始化 select2 用户选择
var $initUserSelect2 = function (el, multiple) {
  var s_input = null
  var s = $(el).select2({
    placeholder: '选择用户',
    minimumInputLength: 0,
    multiple: multiple === true,
    ajax: {
      url: '/commons/search/search',
      delay: 300,
      data: function (params) {
        var query = {
          entity: 'User',
          qfields: 'loginName,fullName,email,quickCode',
          q: params.term,
          type: 'UDR'
        }
        s_input = params.term
        return query
      },
      processResults: function (data) {
        return {
          results: data.data
        }
      }
    },
    language: {
      noResults: function () {
        return (s_input || '').length > 0 ? '未找到结果' : '输入用户名/邮箱搜索'
      },
      inputTooShort: function () {
        return '输入用户名/邮箱搜索'
      },
      searching: function () {
        return '搜索中...'
      }
    }
  })
  s.on('change.select2', function (e) {
    var v = e.target.value
    if (v) $.post('/commons/search/recently-add?type=UDR&id=' + v)
  })
  return s
}

// 初始化引用字段搜索
var $initReferenceSelect2 = function (el, field) {
  var search_input = null
  return $(el).select2({
    placeholder: '选择' + field.label,
    minimumInputLength: 0,
    maximumSelectionLength: 2,
    ajax: {
      url: '/commons/search/' + (field.searchType || 'reference'),
      delay: 300,
      data: function (params) {
        search_input = params.term
        return { entity: field.entity, field: field.name, q: params.term }
      },
      processResults: function (data) {
        return { results: data.data }
      }
    },
    language: {
      noResults: function () { return (search_input || '').length > 0 ? '未找到结果' : '输入关键词搜索' },
      inputTooShort: function () { return '输入关键词搜索' },
      searching: function () { return '搜索中...' },
      maximumSelected: function () { return '只能选择 1 项' },
      removeAllItems: function () { return '清除' }
    },
    theme: `default ${field.appendClass || ''}`
  })
}

// 保持模态窗口（如果需要）
var $keepModalOpen = function () {
  if ($('.rbmodal.show, .rbview.show').length > 0) {
    var $body = $(document.body)
    if (!$body.hasClass('modal-open')) $body.addClass('modal-open').css({ 'padding-right': 17 })
    return true
  }
  return false
}

// 禁用按钮 X 秒，用在一些危险操作上
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

// 加载状态条（单线程）
var $mp = {
  __timer: null,
  __mp: null,
  // 开始
  start: function () {
    $mp.__timer = setTimeout(function () {
      $mp.__mp = new Mprogress({ template: 3, start: true })
    }, 600)
  },
  // 结束
  end: function () {
    if ($mp.__timer) {
      clearTimeout($mp.__timer)
      $mp.__timer = null
    }
    if ($mp.__mp) {
      $mp.__mp.end()
      $mp.__mp = null
    }
  }
}

var EMOJIS = { '赞': 'rb_zan.png', '握手': 'rb_woshou.png', '耶': 'rb_ye.png', '抱拳': 'rb_baoquan.png', 'OK': 'rb_ok.png', '拍手': 'rb_paishou.png', '拜托': 'rb_baituo.png', '差评': 'rb_chaping.png', '微笑': 'rb_weixiao.png', '撇嘴': 'rb_piezui.png', '花痴': 'rb_huachi.png', '发呆': 'rb_fadai.png', '得意': 'rb_deyi.png', '大哭': 'rb_daku.png', '害羞': 'rb_haixiu.png', '闭嘴': 'rb_bizui.png', '睡着': 'rb_shuizhao.png', '敬礼': 'rb_jingli.png', '崇拜': 'rb_chongbai.png', '抱抱': 'rb_baobao.png', '忍住不哭': 'rb_renzhubuku.png', '尴尬': 'rb_ganga.png', '发怒': 'rb_fanu.png', '调皮': 'rb_tiaopi.png', '开心': 'rb_kaixin.png', '惊讶': 'rb_jingya.png', '呵呵': 'rb_hehe.png', '思考': 'rb_sikao.png', '哭笑不得': 'rb_kuxiaobude.png', '抓狂': 'rb_zhuakuang.png', '呕吐': 'rb_outu.png', '偷笑': 'rb_touxiao.png', '笑哭了': 'rb_xiaokule.png', '白眼': 'rb_baiyan.png', '傲慢': 'rb_aoman.png', '饥饿': 'rb_jie.png', '困': 'rb_kun.png', '吓': 'rb_xia.png', '流汗': 'rb_liuhan.png', '憨笑': 'rb_hanxiao.png', '悠闲': 'rb_youxian.png', '奋斗': 'rb_fendou.png', '咒骂': 'rb_zhouma.png', '疑问': 'rb_yiwen.png', '嘘': 'rb_xu.png', '晕': 'rb_yun.png', '惊恐': 'rb_jingkong.png', '衰': 'rb_shuai.png', '骷髅': 'rb_kulou.png', '敲打': 'rb_qiaoda.png', '再见': 'rb_zaijian.png', '无语': 'rb_wuyu.png', '抠鼻': 'rb_koubi.png', '鼓掌': 'rb_guzhang.png', '糗大了': 'rb_qiudale.png', '猥琐的笑': 'rb_weisuodexiao.png', '哼': 'rb_heng.png', '不爽': 'rb_bushuang.png', '打哈欠': 'rb_dahaqian.png', '鄙视': 'rb_bishi.png', '委屈': 'rb_weiqu.png', '安慰': 'rb_anwei.png', '坏笑': 'rb_huaixiao.png', '亲亲': 'rb_qinqin.png', '冷汗': 'rb_lenghan.png', '可怜': 'rb_kelian.png', '生病': 'rb_shengbing.png', '愉快': 'rb_yukuai.png', '幸灾乐祸': 'rb_xingzailehuo.png', '大便': 'rb_dabian.png', '干杯': 'rb_ganbei.png', '钱': 'rb_qian.png' }
// 转换文字 emoji 为 img 标签
var converEmoji = function (text) {
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

// Use momentjs
var $fromNow = function (date) {
  if (!date || !window.moment) return
  return moment(date.split('UTC')[0].trim()).fromNow()
}