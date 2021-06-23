/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global introJs */

$(document).ready(() => {
  setTimeout(startTour, 1000)
})

const startTour = function () {
  _dloadLibs(() => {
    const steps = _getIntroSteps()
    if (steps) {
      const steps2 = steps.map((item) => {
        return { ...item, element: $(`.${item.element}`)[0] }
      })

      const $intro = introJs()
        .setOptions({
          steps: steps2,
          overlayOpacity: 0.4,
          disableInteraction: true,
          exitOnOverlayClick: false,
          exitOnEsc: false,
          tooltipClass: 'rebuild-tour-tooltip',
          highlightClass: 'rebuild-tour-highlight',
        })
        .onchange((target) => {
          const $target = $(target)
          let idx = -1
          for (let i = 0; i < steps.length; i++) {
            if ($target.hasClass(steps[i].element || '-unset')) {
              idx = i
              break
            }
          }
          if (idx < 0) return

          $('.rebuild-tour-highlight').css('box-shadow', 'rgb(0 0 0 / 0%) 0px 0px 1px 2px, rgb(0 0 0 / 40%) 0px 0px 0px 5000px')
          $intro.refresh()

          console.log(target, steps[idx])
        })
        .start()
    }
  })
}

const _getIntroSteps = function () {
  if (location.href.includes('/dashboard/home')) {
    return [
      {
        title: '欢迎使用',
        intro: '本指引将带你了解 REBUILD 基本功能的使用',
      },
      {
        element: 'rb-left-sidebar',
        title: '导航菜单',
        intro: '导航菜单',
      },
      {
        element: 'nav-settings',
        title: '导航菜单配置',
        intro: '导航菜单配置',
      },
      {
        element: 'admin-settings',
        title: '管理中心',
        intro: '管理中心',
      },
      {
        element: 'page-help',
        title: '帮助中心',
        intro: '帮助中心',
      },
      {
        element: 'J_top-notifications',
        title: '消息通知',
        intro: '消息通知',
      },
      {
        element: 'J_top-user',
        title: '个人设置',
        intro: '个人设置',
      },
    ]
  }
}

// 加载 introjs
const _dloadLibs = function (onLoad) {
  const $link = document.createElement('link')
  $link.type = 'text/css'
  $link.rel = 'stylesheet'
  $link.href = `${rb.baseUrl}/assets/lib/introjs.min.css?v=2.5.0`
  document.getElementsByTagName('head')[0].appendChild($link)

  $.ajax({
    url: '/assets/lib/intro.min.js?v=2.5.0',
    type: 'GET',
    dataType: 'script',
    cache: true,
    success: () => typeof onLoad === 'function' && onLoad(),
  })
}
