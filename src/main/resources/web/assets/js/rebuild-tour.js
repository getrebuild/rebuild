/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global introJs */

$(document).ready(() => {
  setTimeout(startTour, 2000)
})

const startTour = function () {
  let steps
  if (location.href.includes('/dashboard/home')) {
    steps = StepRebuild()
    StepDashboard().forEach((item) => steps.push(item))
  }
  if (!steps) return

  const stepsObj = []
  steps.forEach((item) => {
    const $el = item.element ? $(item.element) : [null]
    if ($el.length > 0) stepsObj.push({ ...item, element: $el[0] })
  })
  if (stepsObj.length === 0) return

  // 取消滚动条
  $(document.body).addClass('rebuild-tour-body')

  const $introJs = introJs()
    .setOptions({
      steps: stepsObj,
      overlayOpacity: 0,
      disableInteraction: true,
      exitOnOverlayClick: false,
      exitOnEsc: false,
      scrollToElement: false,
      tooltipClass: 'rebuild-tour-tooltip',
      highlightClass: 'rebuild-tour-highlight',
      prevLabel: '<i class="zmdi zmdi-arrow-left"></i>',
      nextLabel: '<i class="zmdi zmdi-arrow-right"></i>',
      doneLabel: $L('完成'),
    })
    .onchange((target) => {
      const $target = $(target)
      let idx = -1
      for (let i = 1; i < steps.length; i++) {
        if ($target.hasClass(steps[i].element.substr(1))) {
          idx = i
          break
        }
      }
      if (idx < 0) return

      // hack: 位置更新
      $('.rebuild-tour-highlight').css('box-shadow', 'none')
      const pos = { margin: 0 }
      const s = steps[idx]
      if (s && s.rbLeft) pos.marginLeft = s.rbLeft
      else if (s && s.rbRight) pos.marginRight = s.rbRight
      if (s && s.rbTop) pos.marginTop = s.rbTop
      else if (s && s.rbBottom) pos.marginBottom = s.rbBottom
      setTimeout(() => $('.rebuild-tour-tooltip').css(pos), 360)

      $introJs.refresh()
    })
    .oncomplete(() => {
      $(document.body).removeClass('rebuild-tour-body')
    })
    .start()
}

// ~~ 向导步骤

const StepRebuild = () => {
  return [
    {
      title: '欢迎使用',
      intro: '使用向导将带你了解 REBUILD 基本功能的使用',
    },
    {
      element: '.rb-left-sidebar',
      title: '导航菜单',
      intro: '使用导航菜单可以在各个功能模块之间切换',
      rbLeft: -10,
      rbTop: 16,
    },
    {
      element: '.nav-settings',
      title: '导航菜单配置',
      intro: '可以在此处进行个性化的导航菜单设置',
      rbLeft: -10,
      rbBottom: -10,
    },
    {
      element: '.global-search',
      title: '全局搜索',
      intro: '全局搜索可以帮助你快速查询所需要的数据',
      rbLeft: 5,
    },
    {
      element: '.global-create',
      title: '快速新建',
      intro: '可以在此快速新建记录',
      rbLeft: 8,
    },
    {
      element: '.admin-settings',
      title: '管理中心',
      intro: '管理员配置中心',
      rbLeft: 5,
    },
    {
      element: '.page-help',
      title: '帮助中心',
      intro: '使用中遇到问题可以查阅帮助文档，你也可以通过阅读帮助文档 GET 更多技能',
      rbLeft: 5,
    },
    {
      element: '.J_top-notifications',
      title: '消息通知',
      intro: '与你相关的通知都在这里',
      rbRight: 9,
    },
    {
      element: '.J_top-user',
      title: '个人设置',
      intro: '可以在此处设置你的个人信息、修改邮箱/密码等',
      rbRight: 11,
    },
  ]
}

const StepDashboard = () => {
  return [
    {
      element: '.dash-head',
      title: '切换仪表盘',
      intro: '可以在此处切换仪表盘显示，或进行设置/新增',
      rbTop: -6
    },
  ]
}
