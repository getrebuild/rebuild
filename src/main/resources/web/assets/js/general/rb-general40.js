/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(function () {
  const RbForm_renderAfter = RbForm.renderAfter
  RbForm.renderAfter = function (F) {
    typeof RbForm_renderAfter === 'function' && RbForm_renderAfter(F)

    // 用户初始密码
    if (F.state.entity === 'User' && F.isNew) {
      const newpwd = $random(null, true, 6) + 'rB!8'
      _setFieldValue(F, 'password', newpwd)

      F.onFieldValueChange((o) => {
        if (o.name !== 'fullName' || !o.value) return

        $setTimeout(
          () => {
            $.get(`/user/checkout-name?fullName=${encodeURIComponent(o.value)}`, (res) => _setFieldValue(F, 'loginName', res.data))
          },
          1000,
          'checkout-name'
        )
      })
    }
  }
})

function _setFieldValue(F, name, value) {
  const fieldComp = F.getFieldComp(name)
  if (fieldComp && value) fieldComp.setValue(value)
}
