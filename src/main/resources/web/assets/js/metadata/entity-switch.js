/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(() => {
  const entity = window.__PageConfig.entity || window.__PageConfig.entityName
  if (!entity) return

  let href = location.href
  if (href.includes('/field/')) {
    href = href.split('/field/')[0] + '/fields'
  }

  const $ul = $('<ul class="dropdown-menu auto-scroller"></ul>').appendTo('.aside-header')
  $ul.perfectScrollbar()

  function _render(item) {
    $(`<a class="dropdown-item" href="${href.replace(`/${entity}/`, `/${item.entityName}/`)}"><i class="icon zmdi zmdi-${item.icon}"></i> ${item.entityLabel}</a>`).appendTo($ul)
  }

  $.get('/admin/entity/entity-list?detail=true&bizz=true', (res) => {
    $(res.data).each((idx, item) => {
      if (item.builtin === true) _render(item)
    })
    $(res.data).each((idx, item) => {
      if (item.builtin === false) _render(item)
    })
  })

  $('<i class="icon zmdi zmdi-caret-down ml-1 text-muted"></i>').appendTo('.aside-header .title')
  $('.aside-header .title')
    .addClass('dropdown-toggle')
    .attr({
      'data-toggle': 'dropdown',
      'title': $L('切换'),
    })
})
