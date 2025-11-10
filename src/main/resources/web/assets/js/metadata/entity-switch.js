/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(() => {
  const entity = window.__PageConfig.entity || window.__PageConfig.entityName
  if (!entity) return

  let href = location.href
  if (href.includes('/field/')) href = href.split('/field/')[0] + '/fields'
  if (href.includes('?id=')) href = href.split('?id=')[0]

  const $menu = $('<div class="dropdown-menu auto-scroller entity-switch"></div>').appendTo('.aside-header')
  $dropdownMenuSearch($menu)

  function renderItem(item) {
    const $item = $(
      `<a class="dropdown-item" href="${href.replace(`/${entity}/`, `/${item.entityName}/`)}" data-name="${item.entityName}" data-pinyin="${item.quickCode || ''}"><i class="icon zmdi zmdi-${
        item.icon
      }"></i> ${item.entityLabel}</a>`
    )
    if (entity === item.entityName) $item.addClass('current')
    $item.appendTo($menu)
  }

  $.get('/admin/entity/entity-list?detail=true&bizz=true', (res) => {
    const _data = res.data || []
    _data.forEach((item) => {
      if (item.builtin) renderItem(item)
    })
    _data.forEach((item) => {
      if (!item.builtin) renderItem(item)
    })

    $menu.perfectScrollbar()
  })

  const $toggle = $('.aside-header .title').addClass('pointer')
  $('<i class="icon zmdi zmdi-chevron-down"></i>').appendTo($toggle)
  $toggle.addClass('dropdown-toggle').attr({
    'data-toggle': 'dropdown',
    'title': $L('切换'),
  })
})
