/* eslint-disable no-unused-vars */
var loadDeptTree = function () {
  $.get(rb.baseUrl + '/admin/bizuser/dept-tree', function (res) {
    $('.dept-tree').empty()
    let root = $('<ul class="list-unstyled"></ul>').appendTo('.dept-tree')
    renderDeptTree({ id: '$ALL$', name: '所有部门' }, root).addClass('active')
    $(res.data).each(function () {
      renderDeptTree(this, root)
    })
  })
}

const renderDeptTree = function (dept, target) {
  let child = $(`<li data-id="${dept.id}"><a href="#dept=${dept.id}" class="text-truncate ${dept.disabled && ' text-disabled'}">${dept.name} ${dept.disabled ? '<small></small>' : ''}</a></li>`).appendTo(target)
  child.find('a').click(function () {
    $('.dept-tree li').removeClass('active')
    child.addClass('active')

    let ids = [child.data('id')]
    child.find('li').each(function () {
      ids.push($(this).data('id'))
    })
    clickDept(ids)
    return false
  })
  //let action = $('<div class="action"><a><i class="zmdi zmdi-close"></i></a><a><i class="zmdi zmdi-edit"></i></a></div>').appendTo(child)

  if (dept.children && dept.children.length > 0) {
    let parent = $('<ul class="list-unstyled"></ul>').appendTo(child)
    $(dept.children).each(function () {
      renderDeptTree(this, parent)
    })
  }
  return child
}

// To Override
var clickDept = function (depts) { }