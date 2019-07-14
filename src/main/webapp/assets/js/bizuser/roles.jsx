// eslint-disable-next-line no-undef
RbForm.postAfter = function (data) {
  location.href = rb.baseUrl + '/admin/bizuser/role/' + data.id
}
const role_id = window.__PageConfig.recordId
$(document).ready(function () {
  $('.J_new-role').click(function () {
    RbFormModal.create({ title: '新建角色', entity: 'Role', icon: 'lock' })
  })

  if (role_id) {
    $('.J_save').attr('disabled', false).click(updatePrivileges)
    loadPrivileges()
  }
  loadRoles()

  // ENTITY

  // 单个操作
  $('#priv-entity tbody .priv').click(function () {
    let _this = $(this)
    clickPriv(_this, _this.data('action'))
  })
  // 批量操作
  $('#priv-entity thead th>a').click(function () {
    let _this = $(this)
    let action = _this.data('action')
    let privAll = $('#priv-entity tbody .priv[data-action="' + action + '"]')
    clickPriv(privAll, action)
  })
  // 批量操作
  $('#priv-entity tbody .name>a').click(function () {
    let privAll = $(this).parent().parent().find('.priv')
    let clz = 'R0'
    if (privAll.eq(0).hasClass('R0')) clz = 'R4'
    privAll.removeClass('R0 R1 R2 R3 R4').addClass(clz)
  })

  // ZERO

  $('#priv-zero tbody .priv').click(function () {
    clickPriv($(this), 'Z')
  })
  $('#priv-zero thead th>a').click(function () {
    let privAll = $('#priv-zero tbody .priv[data-action="Z"]')
    clickPriv(privAll, 'Z')
  })
  $('#priv-zero tbody .name>a').click(function () {
    let el = $(this).parent().next().find('i.priv')
    clickPriv(el, 'Z')
  })

})
const clickPriv = function (elements, action) {
  if (action === 'C' || action === 'Z') {
    if (elements.first().hasClass('R0')) elements.removeClass('R0').addClass('R4')
    else elements.removeClass('R4').addClass('R0')
  } else {
    let clz = 'R0'
    if (elements.hasClass('R0')) clz = 'R1'
    else if (elements.hasClass('R1')) clz = 'R2'
    else if (elements.hasClass('R2')) clz = 'R3'
    else if (elements.hasClass('R3')) clz = 'R4'
    elements.removeClass('R0 R1 R2 R3 R4').addClass(clz)
  }
}
const loadRoles = function () {
  $.get(rb.baseUrl + '/admin/bizuser/role-list', function (res) {
    $('.dept-tree .ph-item').remove()
    $('.dept-tree ul').empty()
    $(res.data).each(function () {
      let _id = this.id
      let item = $('<li><a class="text-truncate' + (this.disabled ? ' text-disabled' : '') + '" href="' + rb.baseUrl + '/admin/bizuser/role/' + _id + '">' + this.name + (this.disabled ? '<small></small>' : '') + '</a></li>').appendTo('.dept-tree ul')
      let action = $('<div class="action"><a class="J_edit"><i class="zmdi zmdi-edit"></i></a><a class="J_del"><i class="zmdi zmdi-delete"></i></a></div>').appendTo(item)
      if (role_id === this.id) item.addClass('active')
      if (this.id === '003-0000000000000001') action.remove()

      action.find('a.J_edit').click(function () {
        RbFormModal.create({ title: '编辑角色', entity: 'Role', icon: 'lock', id: _id })
      })

      action.find('a.J_del').click(function () {
        let alertExt = {
          type: 'danger', confirmText: '删除', confirm: function () {
            deleteRole(_id, this)
          }
        }
        $.get(rb.baseUrl + '/admin/bizuser/delete-checks?id=' + _id, function (res) {
          if (res.data.hasMember === 0) {
            RbAlert.create('此角色可以被安全的删除', '删除角色', alertExt)
          } else {
            let url = rb.baseUrl + '/admin/bizuser/users#!/Filter/roleId=' + _id
            let msg = '有 <a href="' + url + '" target="_blank"><b>' + res.data.hasMember + '</b></a> 个用户使用了此角色<br>删除将导致这些用户被禁用，直到你为他们指定了新的角色'
            alertExt.html = true
            RbAlert.create(msg, '删除角色', alertExt)
          }
        })
      })
    })
  })
}
const loadPrivileges = function () {
  $.get(rb.baseUrl + '/admin/bizuser/privileges-list?role=' + role_id, function (res) {
    if (res.error_code === 0) {
      $(res.data).each(function () {
        let etr = $('.table-priv tbody td.name>a[data-name="' + this.name + '"]')
        etr = etr.parent().parent()
        let defi = JSON.parse(this.definition)
        for (let k in defi) {
          etr.find('.priv[data-action="' + k + '"]').removeClass('R0 R1 R2 R3 R4').addClass('R' + defi[k])
        }
      })
    } else {
      $('.J_save').attr('disabled', true)
      $('.J_tips').removeClass('hide').find('.message p').text(res.error_msg)
    }
  })
}
const updatePrivileges = function () {
  let privEntity = {}
  $('#priv-entity tbody>tr').each(function () {
    let etr = $(this)
    let name = etr.find('td.name a').data('name')
    let definition = {}
    etr.find('i.priv').each(function () {
      let _this = $(this)
      let action = _this.data('action')
      let deep = 0
      if (_this.hasClass('R1')) deep = 1
      else if (_this.hasClass('R2')) deep = 2
      else if (_this.hasClass('R3')) deep = 3
      else if (_this.hasClass('R4')) deep = 4
      definition[action] = deep
    })
    privEntity[name] = definition
  })
  let privZero = {}
  $('#priv-zero tbody>tr').each(function () {
    let etr = $(this)
    let name = etr.find('td.name a').data('name')
    let definition = etr.find('i.priv').hasClass('R0') ? { Z: 0 } : { Z: 4 }
    privZero[name] = definition
  })

  let _data = { entity: privEntity, zero: privZero }
  $.post(rb.baseUrl + '/admin/bizuser/privileges-update?role=' + role_id, JSON.stringify(_data), (res) => {
    if (res.error_code === 0) location.reload()
    else RbHighbar.error(res.error_msg)
  })
}
const deleteRole = function (id, dlg) {
  dlg.disabled(true)
  $.post(rb.baseUrl + '/admin/bizuser/role-delete?transfer=&id=' + id, (res) => {
    if (res.error_code === 0) location.replace(rb.baseUrl + '/admin/bizuser/role-privileges')
    else RbHighbar.error(res.error_msg)
  })
}