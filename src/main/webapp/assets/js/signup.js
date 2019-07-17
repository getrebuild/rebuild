$(document).ready(function () {
  $('.J_vcode-btn').click(function () {
    let email = $val('#sEmail')
    if (!email) { RbHighbar.create('请输入注册邮箱'); return }

    let _btn = $(this).button('loading')
    $.post(rb.baseUrl + '/user/signup-email-vcode?email=' + $encode(email), function (res) {
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
    $.get(rb.baseUrl + '/user/checkout-name?fullName=' + $encode(v), (res) => {
      if (res.error_code === 0 && res.data) $('#sName').val(res.data)
    })
  })

  $('.J_confirm-btn').click(function () {
    let fullName = $val('#sFullName'),
      name = $val('#sName'),
      email = $val('#sEmail'),
      vcode = $val('#sVcode')
    if (!fullName) { RbHighbar.create('请输入姓名'); return }
    if (!name) { RbHighbar.create('请输入登录名'); return }
    if (!email) { RbHighbar.create('请输入注册邮箱'); return }
    if (!vcode) { RbHighbar.create('请输入邮箱验证码'); return }
    let _data = { loginName: name, fullName: fullName, email: email, vcode: vcode }

    let _btn = $(this).button('loading')
    $.post(rb.baseUrl + '/user/signup-confirm', JSON.stringify(_data), function (res) {
      if (res.error_code === 0) {
        _btn.text('注册成功')
        setTimeout(function () { location.href = './login?t=99' }, 1000)
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
  $('.J_vcode-btn').text('重新获取 (' + (--countdown_seconds) + ')')
  if (countdown_seconds === 0) {
    $('.J_vcode-btn').attr('disabled', false).text('重新获取')
  } else {
    countdown_timer = setTimeout(resend_countdown, 1000)
  }
}