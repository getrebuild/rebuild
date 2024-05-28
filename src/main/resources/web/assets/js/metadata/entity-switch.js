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

  const $menu = $('<div class="dropdown-menu auto-scroller entity-switch"></div>').appendTo('.aside-header')

  // v3.7 search
  $(`<div class="searchbox"><input placeholder="${$L('搜索')}" maxlength="40"/></div>`)
    .appendTo($menu)
    .find('input')
    .on('input', function (e) {
      const s = $trim(e.target.value).toLowerCase()
      $setTimeout(
        () => {
          $menu.find('.dropdown-item').each(function () {
            const $item = $(this)
            if (!s || $item.data('name').toLowerCase().includes(s) || $item.text().toLowerCase().includes(s)) {
              $item.removeClass('hide')
            } else {
              $item.addClass('hide')
            }
          })
        },
        200,
        '_SearchEntities'
      )
    })

  function renderItem(item) {
    const $item = $(
      `<a class="dropdown-item" href="${href.replace(`/${entity}/`, `/${item.entityName}/`)}" data-name="${item.entityName}"><i class="icon zmdi zmdi-${item.icon}"></i> ${item.entityLabel}</a>`
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
  $('<i class="icon zmdi zmdi-caret-down ml-1 text-muted fs-16"></i>').appendTo($toggle)
  $toggle.addClass('dropdown-toggle').attr({
    'data-toggle': 'dropdown',
    'title': $L('切换'),
  })
  $toggle.parent().on('shown.bs.dropdown', () => {
    setTimeout(() => $menu.find('input')[0].focus(), 100)
  })
})
