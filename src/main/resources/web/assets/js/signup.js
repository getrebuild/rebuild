/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(function () {
  $('.J_vcode-btn').click(function () {
    const email = $val('#sEmail')
    if (!email) return RbHighbar.create($L('PlsInputSome,Email'))

    const $btn = $(this).button('loading')
    $.post('/user/signup-email-vcode?email=' + $encode(email), function (res) {
      if (res.error_code === 0) resend_countdown(true)
      else {
        RbHighbar.create(res.error_msg)
        $btn.button('reset')
      }
    })
  })

  $('#sFullName').blur(function () {
    const v = $(this).val()
    if (!v || $('#sName').val()) return

    $.get('/user/checkout-name?fullName=' + $encode(v), (res) => {
      if (res.error_code === 0 && res.data) $('#sName').val(res.data)
    })
  })

  $('.J_confirm-btn').click(function () {
    const fullName = $val('#sFullName'),
      name = $val('#sName'),
      email = $val('#sEmail'),
      vcode = $val('#sVcode')
    if (!fullName) return RbHighbar.create($L('PlsInputSome,FullName'))
    if (!name) return RbHighbar.create($L('PlsInputSome,LoginName'))
    if (!email) return RbHighbar.create($L('PlsInputSome,Email'))
    if (!vcode) return RbHighbar.create($L('PlsInputSome,EmailVcode'))

    const _data = {
      loginName: name,
      fullName: fullName,
      email: email,
      vcode: vcode,
    }

    const $btn = $(this).button('loading')
    $.post('/user/signup-confirm', JSON.stringify(_data), function (res) {
      if (res.error_code === 0) {
        $btn.text($L('SignupSubmit'))
        $('.alert.hide').removeClass('hide')
      } else {
        RbHighbar.create(res.error_msg)
        $btn.button('reset')
      }
    })
  })
})

let countdown_timer
let countdown_seconds = 60
const resend_countdown = function (first) {
  if (first === true) {
    $('.J_vcode-btn').attr('disabled', true)
    if (countdown_timer) clearTimeout(countdown_timer)
    countdown_seconds = 60
  }
  $('.J_vcode-btn').text(`${$L('ReGet')} (${--countdown_seconds})`)
  if (countdown_seconds === 0) {
    $('.J_vcode-btn').attr('disabled', false).text($L('ReGet'))
  } else {
    countdown_timer = setTimeout(resend_countdown, 1000)
  }
}
