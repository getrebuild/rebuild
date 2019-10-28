/* eslint-disable no-unused-vars */
// Page initial
$(function () {
  var t = $('.rb-scroller')
  t.perfectScrollbar()
  $(window).resize(function () {
    $setTimeout(function () {
      if (window.lessIE11) $('.left-sidebar-scroll').height($('.left-sidebar-spacer').height())
      t.perfectScrollbar('update')
    }, 500, 'rb-scroller-update')
  })
  if (window.lessIE11) {
    $('.left-sidebar-scroll').height($('.left-sidebar-spacer').height())
    $('html').addClass('ie10')
  }

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

  if (rb.isAdminUser === true) {
    $('html').addClass('admin')
    if (rb.isAdminVerified !== true) $('.admin-verified').remove()
    if (location.href.indexOf('/admin/') > -1) $('.admin-settings').remove()
    else if (rb.isAdminVerified === true) $('.admin-settings a i').addClass('text-primary')
  } else {
    $('.admin-show').remove()
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

  $(window).on('resize', function () {
    $setTimeout(resize_handler, 100, 'resize-window')
  })
})
// @t - trigger times
var command_exec = function (t) { }
// Trigger on window.onresize
var resize_handler = function () { }

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
  {
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
}

// Notification
var __checkMessage__state = 0
var __checkMessage = function () {
  $.get(rb.baseUrl + '/notification/check-state', function (res) {
    if (res.error_code > 0) return
    $('.J_notifications-top .badge').text(res.data.unread)
    if (res.data.unread > 0) $('.J_notifications-top .indicator').removeClass('hide')
    else $('.J_notifications-top .indicator').addClass('hide')

    if (__checkMessage__state !== res.data.unread) {
      if (__checkMessage__state > 0) {
        if (!window.__doctitle) window.__doctitle = document.title
        document.title = '(' + __checkMessage__state + ') ' + window.__doctitle
        // __showNotification()
      }
      __loadMessages__state = 0
    }
    __checkMessage__state = res.data.unread

    setTimeout(__checkMessage, rb.env === 'dev' ? 60 * 10000 : 2000)
  })
}
var __loadMessages__state = 0
var __loadMessages = function () {
  if (__loadMessages__state === 1) return
  var dest = $('.rb-notifications .content ul').empty()
  if (dest.find('li').length === 0) {
    $('<li class="text-center mt-3 mb-3"><i class="zmdi zmdi-refresh zmdi-hc-spin fs-18"></i></li>').appendTo(dest)
  }
  $.get(rb.baseUrl + '/notification/messages?pageSize=10', function (res) {
    dest.empty()
    $(res.data).each(function (idx, item) {
      var o = $('<li class="notification"></li>').appendTo(dest)
      if (item[3] === true) o.addClass('notification-unread')
      o = $('<a href="' + rb.baseUrl + '/notifications#id=' + item[4] + '"></a>').appendTo(o)
      $('<div class="image"><img src="' + rb.baseUrl + '/account/user-avatar/' + item[0][0] + '" alt="Avatar"></div>').appendTo(o)
      o = $('<div class="notification-info"></div>').appendTo(o)
      $('<div class="text text-truncate">' + item[1] + '</div>').appendTo(o)
      $('<span class="date">' + item[2] + '</span>').appendTo(o)
    })
    __loadMessages__state = 1
    if (res.data.length === 0) $('<li class="text-center mt-4 mb-4 text-muted">暂无消息</li>').appendTo(dest)
  })
}
var __showNotification = function () {
  if (window.Notification) {
    if (window.Notification.permission === 'granted') {
      var n = new Notification('你有 ' + __checkMessage__state + ' 条未读消息', {
        tag: 'rbNotification'
      })
    } else {
      window.Notification.requestPermission()
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
    var s = $('.search-input').val()
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
    var s = $('.search-input').val()
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

var $gotoSection = function (top, target) {
  $(target || 'body').animate({
    scrollTop: top || 0
  }, 600)
}

// Use H5 or Qiuniu
var $createUploader = function (input, next, complete, error) {
  input = $(input).off('change')
  var imgOnly = input.attr('accept') === 'image/*'
  var temp = input.data('temp')
  if (window.qiniu && rb.storageUrl && !temp) {
    input.on('change', function () {
      var file = this.files[0]
      if (!file) return
      var putExtra = imgOnly ? {
        mimeType: ['image/png', 'image/jpeg', 'image/gif', 'image/bmp']
      } : null
      $.get(rb.baseUrl + '/filex/qiniu/upload-keys?file=' + $encode(file.name), function (res) {
        var o = qiniu.upload(file, res.data.key, res.data.token, putExtra)
        o.subscribe({
          next: function (res) {
            typeof next === 'function' && next({
              percent: res.total.percent
            })
          },
          error: function (err) {
            var msg = (err.message || 'UnknowError').toUpperCase()
            if (imgOnly && msg.contains('FILE TYPE')) {
              RbHighbar.create('请上传图片')
              return false
            } else if (msg.contains('EXCEED FSIZELIMIT')) {
              RbHighbar.create('超出文件大小限制 (20M)')
              return false
            }
            if (error) error({
              error: msg
            })
            else RbHighbar.error('上传失败: ' + msg)
          },
          complete: function (res) {
            typeof complete === 'function' && complete({
              key: res.key
            })
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
          RbHighbar.create('请上传图片')
          return false
        } else if (err === 'ErrorMaxSize') {
          RbHighbar.create('超出文件大小限制')
          return false
        }
      },
      onClientLoad: function (e, file) { },
      onClientProgress: function (e, file) {
        typeof next === 'function' && next({
          percent: e.loaded * 100 / e.total
        })
      },
      onSuccess: function (d) {
        d = $.parseJSON(d.currentTarget.response)
        if (d.error_code === 0) {
          complete({
            key: d.data
          })
        } else {
          var msg = d.error_msg || '上传失败，请稍后重试'
          if (error) error({
            error: msg
          })
          else RbHighbar.error(msg)
        }
      },
      onClientError: function (e, file) {
        var msg = '上传失败，请稍后重试'
        if (error) error({
          error: msg
        })
        else RbHighbar.error(msg)
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
      url: rb.baseUrl + '/commons/search/search',
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
    if (v) $.post(rb.baseUrl + '/commons/search/recently-add?type=UDR&id=' + v)
  })
  return s
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

// 页面类型
var $pgt = {
  RecordView: 'RecordView',
  RecordList: 'RecordList',
  SlaveView: 'SlaveView',
  SlaveList: 'SlaveList'
}