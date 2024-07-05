/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */
/* !!! KEEP IT ES5 COMPATIBLE !!! */

// GA
(function () {
  var gaScript = document.createElement('script')
  gaScript.src = 'https://www.googletagmanager.com/gtag/js?id=G-ZCZHJPMEG7'
  gaScript.async = true
  gaScript.onload = function () {
    window.dataLayer = window.dataLayer || []
    function gtag() {
      window.dataLayer.push(arguments)
    }
    gtag('js', new Date())
    gtag('config', 'G-ZCZHJPMEG7')
  }
  var s = document.getElementsByTagName('script')[0]
  s.parentNode.insertBefore(gaScript, s)
})()

// PAGE INITIAL
$(function () {
  // scroller
  var $t = $('.rb-scroller')
  $t.perfectScrollbar()
  $addResizeHandler(function () {
    if ($.browser.msie) $('.left-sidebar-scroll').height($('.left-sidebar-spacer').height())
    $t.perfectScrollbar('update')
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

    _initNav()
    setTimeout(_initGlobalSearch, 500)
    setTimeout(_initGlobalCreate, 500)
  }

  var $hasNotification = $('.J_top-notifications')
  if ($hasNotification.length > 0) {
    $unhideDropdown($hasNotification).on('shown.bs.dropdown', _loadMessages)
    setTimeout(_checkMessage, 2000)
    // NEED: service-worker.js
    document.addEventListener('notificationclick', function () {})
  }

  var $hasUser = $('.J_top-user')
  if ($hasUser.length > 0) {
    $unhideDropdown($hasUser)
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
    if (location.href.indexOf('/admin/') > -1) {
      $('.admin-settings').remove()
    } else if (rb.isAdminVerified) {
      $('.admin-settings a>.icon').addClass('text-danger')
      topPopover($('.admin-settings a'), '<div class="p-1">' + $L('当前已启用管理中心访问功能，如不再使用建议你 [取消访问](#)').replace('#', 'javascript:_cancelAdmin()') + '</div>')
    }

    $.get('/user/admin-dangers', function (res) {
      if (res.data && res.data.length > 0) {
        $('.admin-danger').removeClass('hide')
        var dd = ['<div class="admin-danger-list">']
        $(res.data).each(function () {
          dd.push('<div>' + this + '</div>')
        })
        dd.push('</div>')
        topPopover($('.admin-danger a'), dd.join(''))
      }
    })
  } else {
    $('.admin-show, .admin-danger').remove()
  }

  var bosskey = 0
  var bosskey_timer
  $(document).on('keydown', function (e) {
    if (e.shiftKey) {
      if (++bosskey === 6) {
        $('.bosskey-show').removeClass('bosskey-show')
        typeof window.bosskeyTrigger === 'function' && window.bosskeyTrigger()
        window.__BOSSKEY = true
      }
      if (bosskey_timer) {
        clearTimeout(bosskey_timer)
        bosskey_timer = null
      }
      bosskey_timer = setTimeout(function () {
        bosskey = 0
      }, 500) // clean
    }
  })
  window.__BOSSKEY = location.href.includes('bosskey=show')

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
  else if (location.href.indexOf('/admin/') === -1) $('.page-help>a').attr('href', 'https://getrebuild.com/docs/manual/')

  // 内容区自适应高度
  $('div[data-fullcontent]').each(function () {
    var $this = $(this)
    var offset = ~~$this.data('fullcontent')
    if (offset > 0) {
      $addResizeHandler(function () {
        $this.css('min-height', $(window).height() - offset)
      })()
    }
  })

  // Theme
  $('.use-theme a').on('click', function () {
    if (rb.commercial < 10) {
      RbHighbar.error(WrapHtml($L('免费版不支持选择主题功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }

    var theme = $(this).data('theme')
    $.get('/commons/theme/set-use-theme?theme=' + theme, function () {
      location.reload(true)
    })
  })

  var $topNavs = $('.navbar .navbar-collapse>.navbar-nav a')
  if ($topNavs.length > 1) {
    document.onvisibilitychange = function () {
      if (document.visibilityState !== 'visible') return

      var active = $('.navbar .navbar-collapse>.navbar-nav li.active>a').attr('href')
      if (active) {
        active = active.split('?')[1].split('&')
        $(active).each(function () {
          var nv = this.split('=')
          if (nv[0] === 'n') $.cookie('AppHome.Nav', nv[1], { expires: 30 })
          if (nv[0] === 'd') $.cookie('AppHome.Dash', nv[1], { expires: 30 })
        })
      }
      console.log('Switch on visibilityState ...', $.cookie('AppHome.Nav'), $.cookie('AppHome.Dash'))
    }
  }

  // if (rb.commercial === 11) {
  //   $('a[target="_blank"]').each(function () {
  //     if (($(this).attr('href') || '').indexOf('getrebuild.com') > -1) $(this).removeAttr('href')
  //   })
  //   $('.commercial11').addClass('hide')
  // }
})

// 取消管理中心访问
var _cancelAdmin = function () {
  $.post('/user/admin-cancel', function (res) {
    if (res.error_code === 0) {
      $('.admin-settings a>.icon').removeClass('text-danger')
      $('.admin-settings a').popover('dispose')
      rb.isAdminVerified = false
    }
  })
}
// 初始化导航菜单
var _initNav = function () {
  var isOffcanvas = $('.rb-offcanvas-menu').length > 0 // Float mode

  // Nav
  if (isOffcanvas) {
    $('.rb-toggle-left-sidebar').on('click', function () {
      $(document.body).toggleClass('open-left-sidebar')
      return false
    })
    $('.sidebar-elements>li>a').tooltip('disable')
  } else {
    var $sidebar = $('.rb-collapsible-sidebar')
    if (!$sidebar.hasClass('rb-collapsible-sidebar-collapsed')) {
      $('.sidebar-elements>li>a').tooltip('disable')
    }

    $('.rb-toggle-left-sidebar').on('click', function () {
      $sidebar.toggleClass('rb-collapsible-sidebar-collapsed')
      $('.sidebar-elements>li>a').tooltip('toggleEnabled')

      var collapsed = $sidebar.hasClass('rb-collapsible-sidebar-collapsed')
      $.cookie('rb.sidebarCollapsed', collapsed, { expires: 180 })

      if (collapsed) {
        $sidebar.find('.parent.open').each(function () {
          $(this).find('>a')[0].click()
        })
      }

      $addResizeHandler()()
    })
  }

  if (!$('.rb-collapsible-sidebar').hasClass('rb-collapsible-sidebar-collapsed')) {
    $('.sidebar-elements>li.parent.open').each(function () {
      var $sm = $(this).find('.sub-menu')
      if (!$sm.hasClass('visible')) $sm.addClass('visible')
    })
  }

  // SubNavs
  var $currsntSubnav

  // MinNav && SubnavOpen
  function closeSubnav() {
    if ($('.rb-collapsible-sidebar').hasClass('rb-collapsible-sidebar-collapsed') && $currsntSubnav && $currsntSubnav.hasClass('open')) {
      $currsntSubnav.removeClass('open')
      $currsntSubnav.find('.sub-menu').removeClass('visible')
    }
  }

  $('.sidebar-elements li.parent').on('click', function (e) {
    $stopEvent(e, true)
    var $this = $(this)
    var $sub = $this.find('.sub-menu')
    // eslint-disable-next-line eqeqeq
    if (!($currsntSubnav && $currsntSubnav.data('id') === $this.data('id'))) closeSubnav()

    $sub.toggleClass('visible')
    $currsntSubnav = $this
    $this.find('a').eq(0).tooltip('hide')

    if ($sub.hasClass('visible')) $this.addClass('open')
    else $this.removeClass('open')

    $('.left-sidebar-scroll').perfectScrollbar('update')
  })

  $('.sidebar-elements li.parent .sub-menu').on('click', function (e) {
    e.stopPropagation()
  })

  $(document.body).on('click', function () {
    closeSubnav()
    if (isOffcanvas) {
      $(document.body).removeClass('open-left-sidebar')
    }
  })

  var $activeNav = $('.sidebar-elements li.active')
  if (!($activeNav.attr('class') || '').contains('nav_entity-') && $activeNav.parents('li.parent').length > 0) {
    $activeNav.parents('li.parent').addClass('active').first().trigger('click')
    $(document.body).trigger('click')
  }

  $('.nav-settings').on('click', function () {
    RbModal.create('/p/settings/nav-settings', $L('设置导航菜单'))
  })

  // WHEN SMALL-WIDTH
  $('.left-sidebar-toggle')
    .on('click', function () {
      $('.rb-collapsible-sidebar').toggleClass('rb-collapsible-sidebar-collapsed')
      $('.left-sidebar-spacer').toggleClass('open')
    })
    .text($('.left-sidebar-content li.active>a:last').text() || 'REBUILD')

  if ($('.page-aside .aside-header').length > 0) {
    $('.page-aside .aside-header').on('click', function () {
      $(this).toggleClass('collapsed')
      $('.page-aside .aside-nav').toggleClass('show')
    })
  }

  setTimeout(function () {
    $('sup.rbv').attr('title', $L('增值功能'))
  }, 400)

  // Active Outer-URL Nav
  var urls = location.href.split('/')
  var navUrl = '/' + urls.slice(3).join('/')
  var $navHit = $('.sidebar-elements a[href="' + navUrl + '"]')
  if ($navHit.length > 0 && !$navHit.parent().hasClass('active')) {
    $('.sidebar-elements li.active:not(.parent)').removeClass('active')
    $navHit.parent().addClass('active')
    // Nav parent
    var $parent = $navHit.parents('li.parent:not(.active)')
    if ($parent.length > 0) {
      $parent.addClass('active')
      // Not default open
      if (!$parent.hasClass('open')) $parent.first().trigger('click')
    }
  }

  // Active Top
  var topnav = $.cookie('AppHome.Nav')
  if (topnav) {
    $('.navbar-collapse .nav-item[data-id="' + topnav + '"]').addClass('active')
  }
}
var _checkMessage__state = 0
// 检查新消息
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
        _showNotification(_checkMessage__state)
      }
      _loadMessages__state = false
    }

    _showStateMM(res.data.mm)
    setTimeout(_checkMessage, rb.env === 'dev' ? 9000 : 2000)
  })
}
var _loadMessages__state = false
var _loadMessages = function () {
  if (_loadMessages__state) return

  var $ul = $('.rb-notifications .content ul').empty()
  if ($ul.find('li').length === 0) {
    $('<li class="text-center mt-3 mb-3"><i class="zmdi zmdi-refresh zmdi-hc-spin fs-18"></i></li>').appendTo($ul)
  }

  $.get('/notification/messages?pageSize=10&preview=true', function (res) {
    $ul.empty()
    $(res.data).each(function (idx, item) {
      var $o = $('<li class="notification"></li>').appendTo($ul)
      if (item[3] === true) $o.addClass('notification-unread')
      $o = $('<a class="a" href="' + rb.baseUrl + '/notifications#' + (item[3] ? 'unread=' : 'all=') + item[4] + '"></a>').appendTo($o)
      $('<div class="image"><img src="' + rb.baseUrl + '/account/user-avatar/' + item[0][0] + '" alt="Avatar"></div>').appendTo($o)
      $o = $('<div class="notification-info"></div>').appendTo($o)
      var $m = $('<div class="text text-truncate"></div>').appendTo($o)
      $m.attr('title', item[1]).text(item[1])
      $('<span class="date">' + $fromNow(item[2]) + '</span>').appendTo($o)
    })

    _loadMessages__state = true
    if (res.data.length === 0) $('<li class="text-center mt-4 mb-4 text-muted">' + $L('暂无通知') + '</li>').appendTo($ul)
  })
}
var _showNotification = function (state) {
  if (location.href.indexOf('/admin/') > -1 || location.href.indexOf('/notifications') > -1) return
  if ($.cookie('rb.NotificationShow')) return

  var _Notification = window.Notification || window.mozNotification || window.webkitNotification
  if (_Notification) {
    if (_Notification.permission === 'granted') {
      var n = new _Notification($L('你有 %d 条未读消息', state), {
        body: window.rb.appName,
        icon: rb.baseUrl + '/assets/img/icon-192x192.png',
        tag: 'rbNotification',
        renotify: true,
        silent: false,
        requireInteraction: true,
      })
      n.onshow = function () {
        // 30m
        var expires = moment()
          .add(30 * 60, 'seconds')
          .toDate()
        $.cookie('rb.NotificationShow', 1, {
          expires: expires,
        })
      }
      n.onclick = function () {
        location.href = rb.baseUrl + '/notifications'
      }
      n.onclose = function () {}
      n.onerror = function () {}
    } else {
      _Notification.requestPermission()
    }
  }
}
var _showStateMM = function (mm) {
  if ($.cookie('mm_gritter_cancel')) return

  if (mm) {
    var $mm = $('#mm_gritter')
    if ($mm[0]) {
      $mm.html(mm.msg)
    } else {
      $mm = $('<div id="mm_gritter"></div>')
      $mm.html(mm.msg)
      RbGritter.create(WrapHtml($mm.prop('outerHTML')), {
        timeout: (mm.time + 60) * 1000,
        type: 'danger',
        icon: 'mdi-server-off',
        onCancel: function () {
          var expires = moment()
            .add(Math.min(mm.time - 30, 300), 'seconds')
            .toDate()
          $.cookie('mm_gritter_cancel', mm.time, { expires: expires })
        },
      })
    }
  } else {
    RbGritter.destory()
  }
}
// 全局搜索
var _initGlobalSearch = function () {
  $('.global-search2>a').on('click', function () {
    _showGlobalSearch($storage.get('GlobalSearch-gs'))
    $('.search-container input')[0].focus()
    setTimeout(function () {
      $('.search-container .dropdown-toggle').dropdown('toggle')
    }, 100)
  })

  // $unhideDropdown('.global-search')
  var $gs = $('.global-search .dropdown-menu')

  $('.sidebar-elements li').each(function (idx, item) {
    var $item = $(item)
    var $a = $item.find('>a')
    if (!$item.hasClass('parent') && ($item.attr('class') || '').contains('nav_entity-')) {
      $('<a class="badge" data-url="' + $a.attr('href') + '">' + $escapeHtml($a.text()) + '</a>').appendTo($gs)
    } else if ($item.hasClass('nav_entity-PROJECT') && $item.hasClass('parent')) {
      $('<a class="badge QUERY" data-url="' + rb.baseUrl + '/project/search">' + $escapeHtml($a.text()) + '</a>').appendTo($gs)
    }
  })

  var $es = $gs.find('a').on('click', function () {
    var s = $('.search-input-gs').val()
    $storage.set('GlobalSearch-gs', s || '')
    location.href = $(this).data('url') + ($(this).hasClass('QUERY') ? '?' : '#') + 'gs=' + $encode(s)
  })
  if ($es.length === 0) return

  var _tryActive = function ($active, $el) {
    if ($el.length === 1) {
      $active.removeClass('active')
      $el.addClass('active')
    }
  }

  $es.eq(0).addClass('active')
  $('.global-search input').on('keydown', function (e) {
    var $active = $('.global-search a.active')
    if (e.keyCode === 37 || e.keyCode === 38) {
      _tryActive($active, $active.prev())
    } else if (e.keyCode === 39 || e.keyCode === 40) {
      _tryActive($active, $active.next())
    } else if (e.keyCode === 13) {
      var s = $('.search-input-gs').val()
      location.href = $active.data('url') + ($active.hasClass('QUERY') ? '?' : '#') + 'gs=' + $encode(s)
    }
  })
}
var _showGlobalSearch = function (gs) {
  $('.global-search2>a').hide()
  $('.search-container').removeClass('hide')
  gs && $('.search-container input').val($decode(gs))
}
// 全局新建
var _initGlobalCreate = function () {
  var entities = []
  $('.sidebar-elements li').each(function () {
    var e = $(this).data('entity')
    if (e && e !== '$PARENT$' && !entities.contains(e)) entities.push(e)
  })
  if (entities.length === 0) return

  $.get('/app/entity/extras/check-creates?entity=' + entities.join(','), function (res) {
    var $gc = $('.global-create2 .dropdown-menu')
    $gc.perfectScrollbar()
    $(res.data || []).each(function () {
      var $item = $('<a class="dropdown-item"><i class="icon zmdi zmdi-' + this.icon + '"></i>' + this.entityLabel + '</a>').appendTo($gc)
      var _this = this
      $item.on('click', function () {
        RbFormModal.create({ title: $L('新建%s', _this.entityLabel), entity: _this.entity, icon: _this.icon })
      })
    })
  })
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

/**
 * 清理 dropdown 菜单
 */
var $cleanMenu = function (mbg) {
  var $mbg = $(mbg)
  var $mbgMenu = $mbg.find('.dropdown-menu')

  var $first = $mbgMenu.children().first()
  if ($first.hasClass('dropdown-divider')) $first.remove()
  var $last = $mbgMenu.children().last()
  if ($last.hasClass('dropdown-divider')) $last.remove()

  $($mbgMenu.children()).each(function () {
    var $this = $(this)
    if ($this.hasClass('hide')) $this.remove()
  })

  // remove btn
  if ($mbgMenu.children().length === 0) $mbg.remove()
}
var $cleanDropdown = $cleanMenu

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

/**
 * 获取附件文件名
 */
var $fileCutName = function (fileName) {
  fileName = fileName.split('?')[0]
  fileName = fileName.split('/')
  fileName = fileName[fileName.length - 1]
  var splitIndex = fileName.indexOf('__')
  return splitIndex === -1 ? fileName : fileName.substr(splitIndex + 2)
}

/**
 * 获取附件文件扩展名
 */
var $fileExtName = function (fileName) {
  fileName = (fileName || '').toLowerCase()
  fileName = fileName.split('?')[0]
  fileName = fileName.split('.')
  return fileName.length < 2 ? '?' : fileName[fileName.length - 1]
}

/**
 * 创建 Upload 组件（自动判断使用七牛或本地）
 */
var $createUploader = function (input, next, complete, error) {
  var $input = $(input).off('change')
  var imageType = $input.attr('accept') === 'image/*' // 仅图片
  var upLocal = $input.data('local') // 上传本地
  var noname = $input.data('noname') || false // 不保持名称
  if (!$input.attr('data-maxsize')) $input.attr('data-maxsize', 1048576 * (rb._uploadMaxSize || 200)) // default 200MB

  var useToken = rb.csrfToken ? '&_csrfToken=' + rb.csrfToken : ''
  var putExtra = imageType ? { mimeType: ['image/*'] } : null

  function _qiniuUpload(file) {
    const over200M = file.size / 1048576 >= 200
    $.get('/filex/qiniu/upload-keys?file=' + $encode(file.name) + '&noname=' + noname + useToken, function (res) {
      var o = qiniu.upload(file, res.data.key, res.data.token, putExtra, { forceDirect: !over200M })
      o.subscribe({
        next: function (res) {
          typeof next === 'function' && next({ percent: res.total.percent, file: file })
        },
        error: function (err) {
          var msg = (err.message || err.error || 'UnknowError').toUpperCase()
          if (imageType && msg.contains('FILE TYPE')) {
            RbHighbar.create($L('请上传图片'))
          } else if (msg.contains('EXCEED FSIZELIMIT')) {
            RbHighbar.create($L('超出文件大小限制'))
          } else {
            RbHighbar.error($L('上传失败，请稍后重试') + ': ' + msg)
          }
          console.log('Upload error :', err)
          typeof error === 'function' && error({ error: msg, file: file })
          return false
        },
        complete: function (res) {
          if (file.size > 0 && upLocal !== 'temp') {
            $.post('/filex/store-filesize?fs=' + file.size + '&fp=' + $encode(res.key) + useToken)
          }
          typeof complete === 'function' && complete({ key: res.key, file: file })
        },
      })
    })
  }

  function _onH5UploadError(err, file) {
    console.log('Upload error :', err)
    if (err && err.status === 413) {
      RbHighbar.error($L('上传失败，请稍后重试') + ': ' + err.status + '#' + err.statusText)
    } else {
      RbHighbar.error($L('上传失败，请稍后重试'))
    }
    typeof error === 'function' && error({ error: err, file: file })
    $input.val(null) // reset
  }

  // Qiniu-Cloud
  if (window.qiniu && rb.storageUrl && !upLocal) {
    var acceptType = $input.attr('accept')
    $input.on('change', function () {
      for (var i = 0; i < this.files.length; i++) {
        // @see jquery.html5uploader.js
        // eslint-disable-next-line no-undef
        if (html5Uploader_checkAccept(this.files[i], acceptType)) {
          _qiniuUpload(this.files[i])
        } else {
          RbHighbar.create(imageType ? $L('请上传图片') : $L('上传文件类型错误'))
        }
      }
    })
  }
  // Local-Disk
  else {
    var idname = $input.attr('id') || $input.attr('name') || $random('H5UP-')
    $input.html5Uploader({
      name: idname,
      postUrl: rb.baseUrl + '/filex/upload?temp=' + (upLocal === 'temp') + '&noname=' + noname + useToken,
      onSelectError: function (file, err) {
        if (err === 'ErrorType') {
          RbHighbar.create(imageType ? $L('请上传图片') : $L('上传文件类型错误'))
          return false
        } else if (err === 'ErrorMaxSize') {
          RbHighbar.create($L('超出文件大小限制'))
          return false
        }
      },
      onServerProgress: function (e, file) {
        typeof next === 'function' && next({ percent: (e.loaded * 100) / e.total, file: file })
      },
      onSuccess: function (e, file) {
        e = $.parseJSON(e.currentTarget.response)
        if (e.error_code === 0) {
          if (file.size > 0 && upLocal !== 'temp') {
            $.post('/filex/store-filesize?fs=' + file.size + '&fp=' + $encode(e.data) + useToken)
          }
          complete({ key: e.data, file: file })
        } else {
          RbHighbar.error($L('上传失败，请稍后重试'))
          typeof error === 'function' && error({ error: e.error_msg, file: file })
        }

        $input.val(null) // reset
      },
      onClientError: _onH5UploadError,
      onClientAbort: _onH5UploadError,
      onServerError: _onH5UploadError,
      onServerAbort: _onH5UploadError,
    })
  }
}
var $initUploader = $createUploader

// 多文件上传
// FIXME 有并发上传问题
var $multipleUploader = function (input, complete) {
  let mp
  let mp_inpro = []
  const mp_end = function (name) {
    if (mp_inpro === 0) mp_inpro = []
    else mp_inpro.remove(name)
    if (mp_inpro.length > 0) return
    setTimeout(() => {
      if (mp) mp.end()
      mp = null
    }, 510)
  }

  $createUploader(
    input,
    (res) => {
      if (!mp_inpro.includes(res.file.name)) mp_inpro.push(res.file.name)
      if (!mp) mp = new Mprogress({ template: 2, start: true })
      mp.set(res.percent / 100) // 0.x
    },
    (res) => {
      mp_end(res.file.name)
      complete(res)
    },
    () => mp_end(0)
  )
}

// 拖拽上传
var $dropUpload = function (dropArea, pasteAreaOrCb, cb) {
  if (typeof pasteAreaOrCb === 'function') {
    cb = pasteAreaOrCb
    pasteAreaOrCb = null
  }

  const $da = $(dropArea)
    .on('dragenter', (e) => {
      $stopEvent(e, true)
    })
    .on('dragover', (e) => {
      $stopEvent(e, true)
      if (e.originalEvent.dataTransfer) e.originalEvent.dataTransfer.dropEffect = 'copy'
      $da.addClass('drop')
    })
    .on('dragleave', (e) => {
      $stopEvent(e, true)
      $da.removeClass('drop')
    })
    .on('drop', function (e) {
      $stopEvent(e, true)
      const files = e.originalEvent.dataTransfer ? e.originalEvent.dataTransfer.files : null
      cb(files)
      $da.removeClass('drop')
    })

  // Ctrl+V
  if (pasteAreaOrCb) {
    $(pasteAreaOrCb).on('paste.file', (e) => {
      const data = e.clipboardData || e.originalEvent.clipboardData || window.clipboardData
      if (data && data.items && data.files && data.files.length > 0) {
        $stopEvent(e, true)
        cb(data.files)
      }
    })
  }
}

/**
 * 卸载 React 组件（顶级组件才能卸载）
 */
var $unmount = function (container, delay, keepContainer, root18) {
  if (!container) return
  var $c = $(container)
  setTimeout(function () {
    if (root18) root18.unmount()
    else ReactDOM.unmountComponentAtNode($c[0]) // return is unmounted

    if (keepContainer !== true && $c.prop('tagName') !== 'BODY') $c.remove()
  }, delay || 1000)
}

/**
 * 初始化引用字段（搜索）
 */
var $initReferenceSelect2 = function (el, option) {
  var search_input = null
  var $el = $(el).select2({
    placeholder: option.placeholder || $L('选择%s', option.label),
    minimumInputLength: 0,
    maximumSelectionLength: $(el).attr('multiple') ? 999 : 2,
    ajax: {
      url: '/commons/search/' + (option.searchType || 'reference'),
      delay: 300,
      data: function (params) {
        search_input = params.term
        var query = {
          entity: option.entity,
          field: option.name,
          q: params.term,
        }
        if (option && typeof option.wrapQuery === 'function') return option.wrapQuery(query)
        else return query
      },
      processResults: function (data) {
        return { results: data.data }
      },
    },
    language: {
      noResults: function () {
        return $trim(search_input).length > 0 ? $L('未找到结果') : $L('输入关键词搜索')
      },
      inputTooShort: function () {
        return $L('输入关键词搜索')
      },
      searching: function () {
        return $L('搜索中')
      },
      maximumSelected: function () {
        return $L('只能选择 1 项')
      },
      removeAllItems: function () {
        return $L('清除')
      },
    },
    theme: 'default ' + (option.appendClass || ''),
  })
  return $el
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
  start: function (_parent) {
    if ($mp._timer || $mp._mp) {
      console.log('Element `$mp._mp` exists')
      return
    }
    $mp._timer = setTimeout(function () {
      $mp._mp = new Mprogress({ template: 3, start: true, parent: _parent || undefined })
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
  // 状态
  isStarted() {
    return $mp._timer || $mp._mp ? true : false
  },
}

var RBEMOJIS = {
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
    if (RBEMOJIS[key]) {
      var img = '<img class="emoji" src="' + rb.baseUrl + '/assets/img/emoji/' + RBEMOJIS[key] + '" title="' + key + '" />'
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
 * 是否过期
 */
var $expired = function (date, offset) {
  var m = $moment(date)
  if (offset) m.add(offset, 's')
  return m.isBefore(moment())
}
/**
 * 友好时间显示
 */
var $fromNow = function (date) {
  var m = $moment(date)
  return Math.abs(moment().diff(m)) < 6000 ? $L('刚刚') : m.fromNow()
}
/**
 * 友好时间显示
 */
var $toNow = function (date) {
  var m = $moment(date)
  return Math.abs(moment().diff(m)) < 6000 ? $L('刚刚') : m.toNow()
}

/**
 * 获取语言（PH_KEY）
 */
var $L = function () {
  var args = arguments
  var lang = _getLang(args[0])
  if (args.length <= 1) return lang
  // 替换占位符 %s %d
  for (var i = 1; i < args.length; i++) lang = lang.replace(/%[sd]/, args[i])
  return lang
}
var _getLang = function (key) {
  var lang = (window._LANGBUNDLE || {})[key]
  if (!lang) {
    console.warn('Missing lang-key `' + key + '`')
    return key
  }
  return lang
}

/**
 * 加载地图脚本
 * https://lbsyun.baidu.com/index.php?title=jspopularGL/guide/helloworld
 */
var $useMap__Loaded
var $useMap = function (cb, v3) {
  var _BMap = v3 ? window.BMap : window.BMapGL
  if ($useMap__Loaded === 2 && _BMap) {
    typeof cb === 'function' && cb()
  } else if ($useMap__Loaded === 1) {
    var _timer = setInterval(function () {
      if ($useMap__Loaded === 2 && _BMap) {
        typeof cb === 'function' && cb()
        clearInterval(_timer)
      }
    }, 500)
  } else {
    $useMap__Loaded = 1
    window['$useMap__callback'] = function () {
      $useMap__Loaded = 2
      typeof cb === 'function' && cb()
    }

    let apiUrl = 'https://api.map.baidu.com/api?v=1.0&type=webgl&ak=' + (rb._baiduMapAk || 'YQKHNmIcOgYccKepCkxetRDy8oTC28nD') + '&callback=$useMap__callback'
    if (v3) apiUrl = apiUrl.replace('v=1.0&type=webgl&', 'v=3.0&')
    $getScript(apiUrl)
  }
}

// 自动定位（有误差）
var $autoLocation = function (callback) {
  $useMap(function () {
    var geo = new window.BMapGL.Geolocation()
    geo.enableSDKLocation()
    geo.getCurrentPosition(function (e) {
      if (this.getStatus() === window.BMAP_STATUS_SUCCESS) {
        var geoc = new window.BMapGL.Geocoder()
        geoc.getLocation(e.point, function (r) {
          var v = {
            lat: e.latitude,
            lng: e.longitude,
            text: r ? r.address : null,
          }
          typeof callback === 'function' && callback(v)
        })
      } else {
        console.log('Geolocation failed :', this.getStatus())
      }
    })
  })
}

// $.getScript use cache
var $getScript = function (url, callback) {
  $.ajax({
    type: 'GET',
    url: url,
    success: callback,
    dataType: 'script',
    cache: true,
  })
}

// 搜索 text/id
// https://select2.org/searching#customizing-how-results-are-matched
var $select2MatcherAll = function (params, data) {
  if ($trim(params.term) === '') {
    return data
  }
  if (typeof data.text === 'undefined') {
    return null
  }

  function _matcher(item, s) {
    s = s.toLowerCase()
    return (item.text || '').toLowerCase().indexOf(s) > -1 || (item.id || '').toLowerCase().indexOf(s) > -1
  }

  if (data.children) {
    var ch = data.children.filter(function (item) {
      return _matcher(item, params.term)
    })
    if (ch.length === 0) return null

    var data2 = $.extend({}, data, true)
    data2.children = ch
    return data2
  } else {
    if (_matcher(data, params.term)) {
      return data
    }
  }

  return null
}

// 绝对 URL
var $isFullUrl = function (url) {
  return url && (url.startsWith('http://') || url.startsWith('https://'))
}

// Mask prefix `SYS `
var $isSysMask = function (label) {
  return label && (label.startsWith('SYS ') || label.contains('.SYS ')) && location.href.indexOf('/admin/') === -1
}

// 颜色
var RBCOLORS = ['#4285f4', '#34a853', '#6a70b8', '#009c95', '#fbbc05', '#ea4335', '#7500ea', '#eb2f96']

// 分页计算
var $pages = function (tp, cp) {
  var pages = []
  if (tp <= 8) {
    for (var i = 1; i <= tp; i++) pages.push(i)
    return pages
  }
  if (tp - cp <= 5) {
    pages.push(1)
    pages.push('.')
    for (var i = tp - 5; i <= tp; i++) pages.push(i)
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

// 格式化代码
var $formattedCode = function (c, type) {
  if (typeof c === 'object') c = JSON.stringify(c)
  if (!window.prettier) return c

  try {
    // eslint-disable-next-line no-undef
    return prettier.format(c, {
      parser: type || 'json',
      // eslint-disable-next-line no-undef
      plugins: prettierPlugins,
      printWidth: 10,
    })
  } catch (err) {
    console.log('Cannot format code :', err)
    return c
  }
}

// 复制
var $clipboard = function ($el, text) {
  if (!window.ClipboardJS) {
    console.log('No `ClipboardJS` defined')
    return
  }

  var oTitle = $el.attr('title') || $L('点击复制')
  var $b = $el.attr('title', oTitle).on('mouseleave', function () {
    $b.attr('data-original-title', oTitle)
  })
  $b.tooltip()
  new window.ClipboardJS($b[0], {
    text: function () {
      return text || $el.data('clipboard-text')
    },
  }).on('success', function () {
    $b.attr('data-original-title', $L('已复制'))
    $b.tooltip('hide')
    setTimeout(function () {
      $b.tooltip('show')
    }, 20)
  })
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

  var time = `${hh}:${mm}:${ss}`
  if (days) return $L('%d天', days) + ' ' + time
  else if (hh === '00') return time.substr(3)
  return time
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
    RbAlert.create(WrapHtml(`<p class="text-bold pb-3">${$L('文件已就绪。')}<a class="link" href="${url}" target="_blank">${$L('点击下载')}</a></p>`), { type: 'clear' })
  }
}

// 不支持排序的字段（联系）
var UNSORT_FIELDTYPES = ['N2NREFERENCE', 'ANYREFERENCE', 'MULTISELECT', 'TAG', 'FILE', 'IMAGE', 'AVATAR', 'SIGN']
