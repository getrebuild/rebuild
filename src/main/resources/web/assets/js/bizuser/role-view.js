/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(() => {
  $('.J_delete-role').click(() => deleteRole(window.__PageConfig.recordId))
})

// 删除角色
const deleteRole = function (id) {
  const alertExt = {
    type: 'danger',
    confirmText: $L('删除'),
    confirm: function () {
      this.disabled(true)

      $.post(`/admin/bizuser/role-delete?transfer=&id=${id}`, (res) => {
        if (res.error_code === 0) location.replace(rb.baseUrl + '/admin/bizuser/role-privileges')
        else RbHighbar.error(res.error_msg)
      })
    },
  }

  $.get(`/admin/bizuser/delete-checks?id=${id}`, function (res) {
    if (res.data.hasMember === 0) {
      RbAlert.create($L('此角色可以被安全的删除'), $L('删除角色'), { ...alertExt, icon: 'alert-circle-o' })
    } else {
      RbAlert.create($L('有 **%d** 个用户使用了此角色 [] 删除可能导致这些用户被禁用，直到你为他们指定了新的角色', res.data.hasMember), $L('删除角色'), {
        ...alertExt,
        html: true,
      })
    }
  })
}
