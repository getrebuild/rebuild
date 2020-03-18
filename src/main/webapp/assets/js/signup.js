/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(function () {
  $('.J_vcode-btn').click(function () {
    let email = $val('#sEmail')
    if (!email) { RbHighbar.create($lang('InputPls', 'Email')); return }

    let _btn = $(this).button('loading')
    $.post('/user/signup-email-vcode?email=' + $encode(email), function (res) {
      if (res.error_code === 0) resend_countdown(true)
      else {
        RbHighbar.create(res.error_msg)
        _btn.button('reset')
      }
    })
  })

  $('#sFullName').blur(function () {
    let v = $(this).val()
    if (!v || $('#sName').val()) return
    $.get('/user/checkout-name?fullName=' + $encode(v), (res) => {
      if (res.error_code === 0 && res.data) $('#sName').val(res.data)
    })
  })

  $('.J_confirm-btn').click(function () {
    let fullName = $val('#sFullName'),
      name = $val('#sName'),
      email = $val('#sEmail'),
      vcode = $val('#sVcode')
    if (!fullName) { RbHighbar.create($lang('InputPls', 'FullName')); return }
    if (!name) { RbHighbar.create($lang('InputPls', 'Username')); return }
    if (!email) { RbHighbar.create($lang('InputPls', 'Email')); return }
    if (!vcode) { RbHighbar.create($lang('InputPls', 'Vcode')); return }
    let _data = { loginName: name, fullName: fullName, email: email, vcode: vcode }

    let _btn = $(this).button('loading')
    $.post('/user/signup-confirm', JSON.stringify(_data), function (res) {
      if (res.error_code === 0) {
        _btn.text($lang('Successful', 'Signup'))
        $('.alert.hide').removeClass('hide')
      } else {
        RbHighbar.create(res.error_msg)
        _btn.button('reset')
      }
    })
  })
})
let countdown_timer
let countdown_seconds = 60
let resend_countdown = function (first) {
  if (first === true) {
    $('.J_vcode-btn').attr('disabled', true)
    if (countdown_timer) clearTimeout(countdown_timer)
    countdown_seconds = 60
  }
  $('.J_vcode-btn').text($lang('GetVcode2') + ' (' + (--countdown_seconds) + ')')
  if (countdown_seconds === 0) {
    $('.J_vcode-btn').attr('disabled', false).text($lang('GetVcode2'))
  } else {
    countdown_timer = setTimeout(resend_countdown, 1000)
  }
}