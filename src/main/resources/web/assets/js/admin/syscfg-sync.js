/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable react/display-name */

let _DefaultRole

// eslint-disable-next-line no-undef
useEditComp = function (name) {
  if (['DingtalkSyncUsers', 'WxworkSyncUsers'].includes(name)) {
    return (
      <select className="form-control form-control-sm">
        <option value="true">{$L('是')}</option>
        <option value="false">{$L('否')}</option>
      </select>
    )
  } else if (['DingtalkSyncUsersRole', 'WxworkSyncUsersRole'].includes(name)) {
    const iv = $(`td[data-id="${name}"]`).attr('data-value')
    setTimeout(() => {
      renderRbcomp(<UserSelector hideDepartment={true} hideUser={true} hideTeam={true} multiple={false} defaultValue={iv} />, name, function () {
        _DefaultRole = this
      })
    }, 100)

    return <div id={name} style={{ maxWidth: 400 }} />
  }
}

// eslint-disable-next-line no-undef
postBefore = function (data) {
  const v = _DefaultRole && _DefaultRole.val()[0]
  if ($('#DingtalkSyncUsersRole')[0]) data.DingtalkSyncUsersRole = v || ''
  else if ($('#WxworkSyncUsersRole')[0]) data.WxworkSyncUsersRole = v || ''

  return data
}

$(document).ready(() => {
  const $btn = $('.J_syncUsers').on('click', () => {
    RbAlert.create($L('确认立即同步用户部门？'), {
      onConfirm: function () {
        $btn.button('loading').find('.icon').addClass('zmdi-hc-spin')
        this.hide()

        syncUsers()
      },
    })
  })
})

function syncUsers() {}
