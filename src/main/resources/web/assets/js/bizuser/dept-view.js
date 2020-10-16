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
      if (res.data.hasMember > 0) limits.push($L('HasXUsers').replace('%d', res.data.hasMember))
      if (res.data.hasChild > 0) limits.push($L('HasXSubdepts').replace('%d', res.data.hasChild))

      if (limits.length === 0) {
        RbAlert.create($L('DeleteDeptSafeConfirm'), $L('DeleteSome,Department'), {
          icon: 'alert-circle-o',
          type: 'danger',
          confirmText: $L('Delete'),
          confirm: function () {
            deleteDept(this)
          },
        })
      } else {
        RbAlert.create($L('DeleteDeptUnSafeConfirm').replace('%s', limits.join(' / ')), $L('NotDelete'), {
          type: 'danger',
          html: true,
        })
      }
    })
  })
})
