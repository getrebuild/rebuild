/* eslint-disable no-unused-vars */
// Page initial
$(function () {
  var t = $('.rb-scroller')
  t.perfectScrollbar()
  $(window).resize(function () {
    $setTimeout(function () {
      if (window.ltIE11 === true) $('.left-sidebar-scroll').height($('.left-sidebar-spacer').height())
      t.perfectScrollbar('update')
    }, 500, 'rb-scroller-update')
  })
  if (window.ltIE11 === true) $('.left-sidebar-scroll').height($('.left-sidebar-spacer').height())

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
var $fileDetectingIcon = function (fileName) {
  fileName = fileName.toLowerCase()
  if (fileName.endsWith('.png') || fileName.endsWith('.gif') || fileName.endsWith('.jpg') || fileName.endsWith('.jpeg') || fileName.endsWith('.bmp')) return 'png'
  else if (fileName.endsWith('.doc') || fileName.endsWith('.docx')) return 'word'
  else if (fileName.endsWith('.ppt') || fileName.endsWith('.pptx')) return 'ppt'
  else if (fileName.endsWith('.xls') || fileName.endsWith('.xlsx')) return 'excel'
  else if (fileName.endsWith('.pdf')) return 'pdf'
  else if (fileName.endsWith('.mp4') || fileName.endsWith('.rmvb') || fileName.endsWith('.rm') || fileName.endsWith('.avi') || fileName.endsWith('.flv')) return 'mp4'
  return ''
}