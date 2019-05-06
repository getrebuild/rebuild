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
  }

  if (rb.isAdminUser === true) {
    $('html').addClass('admin')
    if (rb.isAdminVerified === true && location.href.indexOf('/admin/') === -1) {
      // TODO
    }
  } else {
    $('.admin-show').remove()
  }

  if ($('.J_notifications-top').length > 0) {
    setTimeout(__checkMessage, 2000)
    $('.J_notifications-top').on('shown.bs.dropdown', __loadMessages)
  }

  var keydown_times = 0
  $(document.body).keydown(function (e) {
    if (e.ctrlKey && e.altKey && e.which === 88) command_exec(++keydown_times)
  })
})
// Trigger on Ctrl+Alt+X
// @t - trigger times
var command_exec = function (t) {}

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
      var collapsed = el.hasClass('rb-collapsible-sidebar-collapsed')
      $storage.set('rb-sidebar-collapsed', collapsed)
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
    var _this = $(this)
    _this.toggleClass('open')
    _this.find('.sub-menu').toggleClass('visible')
    e.stopPropagation()
    currsntSubnav = _this
    _this.find('a').eq(0).tooltip('hide')
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
  if (activeNav.parents('li.parent').length > 0) {
    activeNav.parents('li.parent').addClass('active').first().trigger('click')
    $(document.body).trigger('click')
  }

  // When small-width
  $('.left-sidebar-toggle').click(function () {
    $('.rb-collapsible-sidebar').toggleClass('rb-collapsible-sidebar-collapsed')
    $('.left-sidebar-spacer').toggleClass('open')
  }).text($('.rb-right-navbar .page-title').text())

  // aside
  var aside = $('.page-aside')
  if (aside.length > 0) {
    $('.page-aside .aside-header').click(function () {
      $(this).toggleClass('collapsed')
      $('.page-aside .aside-nav').toggleClass('show')
    })
  }

  $('.nav-settings').click(function () {
    rb.modal(rb.baseUrl + '/p/commons/nav-settings', '设置导航菜单')
  })
}

// Check notification
var __checkMessage__state = 0
var __checkMessage = function () {
  $.get(rb.baseUrl + '/notification/check-message', function (res) {
    if (res.error_code > 0) return
    $('.J_notifications-top .badge').text(res.data.unread)
    if (res.data.unread > 0) $('.J_notifications-top .indicator').removeClass('hide')
    else $('J_notifications-top .indicator').addClass('hide')

    if (__checkMessage__state !== res.data.unread) __loadMessages__state = 0
    __checkMessage__state = res.data.unread
    setTimeout(__checkMessage, rb.env === 'dev' ? 30000 : 2000)
  })
}
var __loadMessages__state = 0
var __loadMessages = function () {
  if (__loadMessages__state === 1) return
  $.get(rb.baseUrl + '/notification/list?pageSize=10', function (res) {
    var el = $('.rb-notifications .content ul').empty()
    $(res.data).each(function (idx, item) {
      var o = $('<li class="notification"></li>').appendTo(el)
      if (item[3] === true) o.addClass('notification-unread')
      o = $('<a href="' + rb.baseUrl + '/notifications#id=' + item[4] + '"></a>').appendTo(o)
      $('<div class="image"><img src="' + item[0][1] + '" alt="Avatar"></div>').appendTo(o)
      o = $('<div class="notification-info"></div>').appendTo(o)
      $('<div class="text text-truncate">' + item[1] + '</div>').appendTo(o)
      $('<span class="date">' + item[2] + '</span>').appendTo(o)
    })
    __loadMessages__state = 1
    if (res.data.length === 0) {
      $('<div class="must-center text-muted">暂无消息</div>').appendTo(el)
    }
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
  if (mbgMenu.children().length === 0) {
    mbg.remove()
  }
}

var $fileCutName = function (fileName) {
  fileName = fileName.split('/')
  fileName = fileName[fileName.length - 1]
  return fileName.substr(fileName.indexOf('__') + 2)
}
var $fileExtName = function (fileName) {
  fileName = (fileName || '').toLowerCase()
  fileName = fileName.split('.')
  return fileName[fileName.length - 1] || ''
}

var $gotoSection = function (top, target) {
  $(target || 'body').animate({ scrollTop: top || 0 }, 600)
}

// Use H5 or Qiuniu
var $createUploader = function (input, next, complete, error) {
  input = $(input).off('change')
  var imgOnly = input.attr('accept') === 'image/*'
  if (window.qiniu && rb.storageUrl) {
    input.on('change', function () {
      var file = this.files[0]
      var putExtra = imgOnly ? { mimeType: ['image/png', 'image/jpeg', 'image/gif', 'image/bmp', 'image/tiff'] } : null
      $.get(rb.baseUrl + '/filex/qiniu/upload-keys?file=' + $encode(file.name), function (res) {
        var o = qiniu.upload(file, res.data.key, res.data.token, putExtra)
        o.subscribe({
          next: function (res) {
            typeof next === 'function' && next({ percent: res.total.percent })
          },
          error: function (err) {
            var msg = (err.message || 'UnknowError').toUpperCase()
            if (imgOnly && msg.contains('FILE TYPE')) {
              rb.highbar('请上传图片')
              return false
            } else if (msg.contains('EXCEED FSIZELIMIT')) {
              rb.highbar('超出文件大小限制')
              return false
            }
            if (error) error({ error: msg })
            else rb.hberror('上传失败: ' + msg)
          },
          complete: function (res) {
            typeof complete === 'function' && complete({ key: res.key })
          }
        })
      })
    })
  }
  else {
    input.html5Uploader({
      name: input.attr('id') || input.attr('name') || 'H5Upload',
      postUrl: rb.baseUrl + '/filex/upload?type=' + (imgOnly ? 'image' : 'file'),
      onSelectError: function (file, err) {
        if (err === 'ErrorType') {
          rb.highbar('请上传图片')
          return false
        } else if (err === 'ErrorMaxSize') {
          rb.highbar('超出文件大小限制')
          return false
        }
      },
      onClientLoad: function (e, file) {},
      onClientProgress: function (e, file) {
        typeof next === 'function' && next({ percent: e.loaded * 100 / e.total })
      },
      onSuccess: function (d) {
        d = $.parseJSON(d.currentTarget.response)
        if (d.error_code === 0) {
          complete({ key: d.data })
        } else {
          var msg = d.error_msg || '上传失败，请稍后重试'
          if (error) error({ error: msg })
          else rb.hberror(msg)
        }
      },
      onClientError: function (e, file) {
        var msg = '上传失败，请稍后重试'
        if (error) error({ error: msg })
        else rb.hberror(msg)
      }
    })
  }
}