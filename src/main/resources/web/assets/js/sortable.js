/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */

$(document).ready(function () {
  $('.J_config')
    .sortable({
      placeholder: 'dd-placeholder',
      handle: '.dd3-handle',
      axis: 'y',
    })
    .disableSelection()
})

const render_unset = function (data, target) {
  const $item = $(`<li class="dd-item" data-key="${data[0]}"><div class="dd-handle"><span>${data[1]}</span></div></li>`).appendTo(target || '.unset-list')
  $item.click(function () {
    render_item(data)
    $item.remove()
  })
  render_unset_after($item, data)
  return $item
}

// 复写更多实现
var render_unset_after = function (item, data) {}

const render_item = function (data, target) {
  const $item = $(`<li class="dd-item dd3-item" data-key="${data[0]}"><div class="dd-handle dd3-handle"></div><div class="dd3-content">${data[1]}</div></li>`).appendTo(target || '.J_config')
  const $del = $(`<div class="dd3-action"><a title="${$lang('Remove')}" class="J_del"><i class="zmdi zmdi-close"></i></a></div>`).appendTo($item)
  $del.find('a').click(function () {
    data[1] = $item.find('.dd3-content').text()
    render_unset(data)
    $item.remove()
  })
  render_item_after($item, data)
  return $item
}

// 复写更多实现
var render_item_after = function (item, data) {}
