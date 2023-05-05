/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global introJs */

window.startTour = function (delay) {
  if ($(window).width() < 1000) return
  if ($.cookie('rb.TourEnd')) return
  setTimeout(startTour123, delay || 100)
}

const wpc = window.__PageConfig || {}
const startTour123 = function () {
  let stepName
  let steps
  if (location.href.includes('/dashboard/home')) {
    stepName = 'TourEnd-Dashboard'
    steps = StepRebuild()
    // StepDashboard().forEach((item) => steps.push(item))
  } else if (location.href.includes('/list') && wpc.type === 'RecordList') {
    if ($('.datalist-mode2').length > 0) {
      stepName = 'TourEnd-RecordList2'
      steps = StepDataList2()
    } else {
      stepName = 'TourEnd-RecordList'
      steps = StepDataList()
    }
  } else if (location.href.includes('/view') && wpc.type === 'RecordView') {
    stepName = 'TourEnd-RecordView'
    steps = StepView()
  } else if (location.href.includes('/dashboard/chart-design')) {
    stepName = 'TourEnd-Chart'
    steps = StepChart()
  } else if (location.href.includes('/project/') && location.href.includes('/tasks')) {
    stepName = 'TourEnd-Project'
    steps = StepProject()
  }

  if (!steps) return

  const isEnd = $storage.get(stepName)
  if (isEnd) return // 已经展示完

  const stepsObj = []
  steps.forEach((item) => {
    const $el = item.element ? $(item.element) : [null]
    if ($el.length > 0) stepsObj.push({ ...item, element: $el[0] })
  })
  if (stepsObj.length === 0) return

  window.tourStarted = stepName
  // 隐藏滚动条
  $(document.body).addClass('rebuild-tour-body')

  let _oncomplete = false
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
      doneLabel: $L('知道了'),
    })
    .onchange((target) => {
      const $target = $(target)
      let stepIndex = -1
      for (let i = 0; i < steps.length; i++) {
        if (steps[i].element && $target.hasClass(steps[i].element.substr(1))) {
          stepIndex = i
          break
        }
      }
      if (stepIndex < 0) return

      // hack: 位置更新
      $('.rebuild-tour-highlight').css('box-shadow', 'none')
      const pos = { margin: 0 }
      const s = steps[stepIndex]
      if (s && s.rbLeft) pos.marginLeft = s.rbLeft
      else if (s && s.rbRight) pos.marginRight = s.rbRight
      if (s && s.rbTop) pos.marginTop = s.rbTop
      else if (s && s.rbBottom) pos.marginBottom = s.rbBottom
      setTimeout(() => $('.rebuild-tour-tooltip').css(pos), stepIndex === 0 ? 1 : 360)

      // $introJs.refresh()
    })
    .oncomplete(() => {
      _oncomplete = true
      $storage.set(stepName, 'yes')
    })
    .onexit(() => {
      window.tourStarted = undefined
      $(document.body).removeClass('rebuild-tour-body')

      if (!_oncomplete) {
        $.cookie('rb.TourEnd', 'session', { expires: null })
      }
    })

  // $introJs.goToStep(1)
  $introJs.start()
}

// ~~ 向导步骤

const StepRebuild = () => {
  return [
    {
      title: $L('欢迎使用'),
      intro: $L('本向导将带你了解系统的基本功能使用，让我们开始吧！'),
    },
    {
      element: '.rb-left-sidebar',
      title: $L('导航菜单'),
      intro: $L('使用导航菜单可以在各个功能模块之间切换'),
      rbLeft: -10,
      rbTop: 16,
    },
    {
      element: '.nav-settings',
      title: $L('导航菜单设置'),
      intro: $L('点击此处进行个性化导航菜单设置'),
      rbLeft: -10,
      rbBottom: -10,
    },
    {
      element: '.global-search2',
      title: $L('全局搜索'),
      intro: $L('全局搜索可以帮助你快速查询需要的数据'),
      rbLeft: 5,
    },
    {
      element: '.global-create2',
      title: $L('快速新建'),
      intro: $L('点击此处快速新建业务记录'),
      rbLeft: 5,
    },
    {
      element: '.admin-settings',
      title: $L('管理中心'),
      intro: $L('REBUILD 拥有强大的配置管理中心，你可以根据需求自由搭建系统'),
      rbLeft: 5,
    },
    {
      element: '.page-help',
      title: $L('帮助中心'),
      intro: $L('使用遇到问题可以查阅帮助文档，你也可以通过阅读文档 GET 更多技能'),
      rbLeft: 6,
    },
    {
      element: '.J_top-notifications',
      title: $L('通知'),
      intro: $L('与你相关的通知消息都在这里'),
      rbRight: 9,
    },
    {
      element: '.J_top-user',
      title: $L('个人设置'),
      intro: $L('点击此处设置你的个人信息，或选择界面主题等'),
      rbRight: 11,
    },
  ]
}

const StepDashboard = () => {
  return [
    {
      element: '.dash-head',
      title: $L('切换仪表盘'),
      intro: $L('点击此处切换仪表盘显示，或进行设置/新增仪表盘'),
      rbTop: -6,
    },
    {
      element: '.J_chart-adds',
      title: $L('添加图表'),
      intro: $L('点击此处为当前仪表盘添加图表'),
      rbTop: -1,
      rbRight: 21,
    },
  ]
}

const StepDataList = () => {
  return [
    {
      element: '.widgets',
      title: $L('侧栏工具'),
      intro: $L('侧栏工具帮助你快速切换常用查询，或查看图表'),
      rbTop: 20,
    },
    {
      element: '.nav-tabs-classic',
      title: $L('切换列表'),
      intro: $L('点击切换主记录/明细记录数据列表'),
      rbLeft: 5,
    },
    {
      element: '.adv-search',
      title: $L('高级查询'),
      intro: $L('高级查询是强大数据检索工具，你可以将查询保存起来方便下次使用'),
      rbLeft: 5,
    },
    {
      element: '.input-search',
      title: $L('快速查询'),
      intro: $L('快速查询可以快速检索数据，并把结果展示在数据列表中'),
      rbLeft: 5,
    },
    {
      element: '.J_view',
      title: $L('打开记录'),
      intro: $L('在列表中选中一条记录，点击打开记录详情'),
      rbLeft: 5,
    },
    {
      element: '.J_edit',
      title: $L('编辑记录'),
      intro: $L('在列表中选中一条记录，点击编辑记录'),
      rbLeft: 5,
    },
    {
      element: '.J_new',
      title: $L('新建记录'),
      intro: $L('新建一条业务记录'),
      rbLeft: 5,
    },
    {
      element: '.J_action',
      title: $L('更多操作'),
      intro: $L('你还可以导出数据报表、批量修改等操作'),
      rbRight: 5,
    },
    {
      element: '.dataTables_info',
      title: $L('列表统计'),
      intro: $L('此处显示列表的统计数据，统计项可由管理员自定义设置'),
      position: 'top',
      rbLeft: -10,
    },
    {
      element: '.dataTables_paginate',
      title: $L('翻页'),
      intro: $L('翻页或设置每页显示的记录数量'),
      position: 'top',
      rbRight: 5,
    },
  ]
}

const StepDataList2 = () => {
  return [
    {
      element: '.datalist-scroll',
      title: $L('数据列表'),
      intro: $L('数据列表用于快速查看记录基本信息，点击列表项可打开记录详情'),
      rbTop: 20,
      rbLeft: -5,
    },
    {
      element: '.datalist-footer',
      title: $L('页码'),
      intro: $L('数据列表的记录总数，或进行翻页'),
    },
    {
      element: '.J_switch-list',
      title: $L('切换列表'),
      intro: $L('点击切换主记录/明细记录数据列表'),
      rbLeft: 5,
    },
    {
      element: '.adv-search',
      title: $L('高级查询'),
      intro: $L('高级查询是强大数据检索工具，你可以将查询保存起来方便下次使用'),
      rbLeft: 5,
    },
    {
      element: '.input-search',
      title: $L('快速查询'),
      intro: $L('快速查询可以快速检索数据，并把结果展示在数据列表中'),
      rbLeft: 5,
    },
    {
      element: '.J_new',
      title: $L('新建记录'),
      intro: $L('新建一条业务记录'),
      rbLeft: 5,
    },
    {
      element: '.J_action',
      title: $L('更多操作'),
      intro: $L('你还可以导出数据报表、批量修改等操作'),
      rbRight: 5,
    },
  ]
}

const StepView = () => {
  return [
    {
      element: '.nav-tabs',
      title: $L('相关项'),
      intro: $L('点击切换并查看与当前记录相关的数据'),
      rbLeft: 5,
    },
    {
      element: '.view-action',
      title: $L('操作'),
      intro: $L('对当前记录进行相关操作'),
      position: 'left',
      rbTop: 6,
    },
    {
      element: '.approval-pane',
      title: $L('审批状态'),
      intro: $L('当前记录的审批状态，及审批相关操作'),
      rbLeft: 5,
    },
  ]
}

const StepChart = () => {
  return [
    {
      element: '.J_fields',
      title: $L('加入字段'),
      intro: $L('长按并拖动字段到上方“维度”或“数值”处，加入你想分析的字段'),
      position: 'right',
      rbTop: 5,
    },
    {
      element: '.axis-editor',
      title: $L('字段选项'),
      intro: $L('对加入到图表的字段进行设置，如显示样式、位置顺序等'),
      rbLeft: 5,
    },
    {
      element: '.J_c-type',
      title: $L('图表类型'),
      intro: $L('可以选择不同的图表类型，例如反映月度销量时使用柱状图较为合适'),
      position: 'left',
      rbTop: 5,
    },
    {
      element: '.J_c-option',
      title: $L('图表选项'),
      intro: $L('针对不同图表有不同的选项可供设置'),
      position: 'left',
      rbTop: 5,
    },
    {
      element: '.rb-toggle-left-sidebar',
      title: $L('完成'),
      intro: $L('图表设计完成后别忘记点击保存'),
      rbLeft: 23,
    },
  ]
}

const StepProject = () => {
  return [
    {
      element: '.newbtn',
      title: $L('添加任务'),
      intro: $L('点击快速向当前任务面板添加任务'),
      rbLeft: 5,
    },
    {
      element: '.task-card',
      title: $L('移动任务'),
      intro: $L('鼠标长按可移动任务到其他任务面板'),
      position: 'right',
      rbTop: 5,
    },
    {
      element: '.J_search',
      title: $L('搜索'),
      intro: $L('搜索当前项目中的任务'),
    },
    {
      element: '.J_views',
      title: $L('显示方式'),
      intro: $L('任务排序，或切换任务排列方式'),
      rbRight: 5,
    },
  ]
}
