/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */

var loadDeptTree = function () {
  $.get('/admin/bizuser/dept-tree', function (res) {
    $('.aside-tree').empty()
    let root = $('<ul class="list-unstyled"></ul>').appendTo('.aside-tree')
    renderDeptTree({ id: '$ALL$', name: $lang('AllSome,Department') }, root).addClass('active')
    $(res.data).each(function () {
      renderDeptTree(this, root)
    })
  })
}

const renderDeptTree = function (dept, target) {
  const $dept = $(
    `<li data-id="${dept.id}"><a href="#dept=${dept.id}" class="text-truncate ${dept.disabled && ' text-disabled'}" title="${dept.disabled ? $lang('Disabled') : ''}">${dept.name}</a></li>`
  ).appendTo(target)

  $dept.find('a').click(function () {
    $('.aside-tree li').removeClass('active')
    $dept.addClass('active')

    let ids = [$dept.data('id')]
    $dept.find('li').each(function () {
      ids.push($(this).data('id'))
    })
    clickDept(ids)
    return false
  })
  //let $action = $('<div class="action"><a><i class="zmdi zmdi-close"></i></a><a><i class="zmdi zmdi-edit"></i></a></div>').appendTo($dept)

  if (dept.children && dept.children.length > 0) {
    const parent = $('<ul class="list-unstyled"></ul>').appendTo($dept)
    $(dept.children).each(function () {
      renderDeptTree(this, parent)
    })
  }
  return $dept
}

// To Override
var clickDept = function (depts) {}
