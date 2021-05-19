/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global RbForm */

let RbForm_postAfter = RbForm.postAfter
RbForm.postAfter = function () {
  RbForm_postAfter()
  if (parent && parent.loadDeptTree) parent.loadDeptTree()
}

const deleteDept = function (alert) {
  alert && alert.disabled(true)
  $.post(`/admin/bizuser/dept-delete?transfer=&id=${dept_id}`, (res) => {
    if (res.error_code === 0) {
      parent.location.hash = '!/View/'
      parent.location.reload()
    } else {
      RbHighbar.error(res.error_msg)
    }
  })
}

const dept_id = window.__PageConfig.recordId
$(document).ready(function () {
  $('.J_delete-dept').click(() => {
    $.get(`/admin/bizuser/delete-checks?id=${dept_id}`, (res) => {
      const limits = []
      if (res.data.hasMember > 0) limits.push($L('**%d** 个用户', res.data.hasMember))
      if (res.data.hasChild > 0) limits.push($L('**%d** 个子部门', res.data.hasChild))

      if (limits.length === 0) {
        RbAlert.create($L('此部门可以被安全的删除'), $L('删除部门'), {
          icon: 'alert-circle-o',
          type: 'danger',
          confirmText: $L('删除'),
          confirm: function () {
            deleteDept(this)
          },
        })
      } else {
        RbAlert.create($L('此部门下有 %s [] 需要先将他们转移至其他部门，然后才能安全删除', limits.join(' / ')), $L('无法删除选中记录'), {
          type: 'danger',
          html: true,
        })
      }
    })
  })
})
