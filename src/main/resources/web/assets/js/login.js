/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(() => {
  // eslint-disable-next-line eqeqeq
  if (top != self) {
    parent.location.reload()
    return
  }

  $('.h5-mobile img').attr('src', `${rb.baseUrl}/commons/barcode/render-qr?t=${$encode($('.h5-mobile a').attr('href'))}`)

  if ($.browser.mobile) {
    setTimeout(() => {
      $(`<div class="bg-info"><i class="icon zmdi zmdi-smartphone-iphone"></i><p>${$L('点击切换到手机版访问')}</p></div>`)
        .appendTo('.announcement-wrapper')
        .on('click', () => (location.href = $('.h5-mobile>a').attr('href')))
    }, 500)
  }

  $.get('/user/live-wallpaper', (res) => {
    if (res.error_code !== 0 || !res.data) return

    const bgimg = new Image()
    bgimg.src = res.data
    bgimg.onload = function () {
      $('.rb-bgimg').animate({ opacity: 0 })
      setTimeout(() => {
        $('.rb-bgimg').css('background-image', `url(${res.data})`).animate({ opacity: 1 })
      }, 400)
    }
  })

  $('.vcode-row img').on('click', function () {
    $(this).attr('src', `captcha?${$random()}`)
  })

  $('#login-form').on('submit', function (e) {
    e.preventDefault()

    const user = $val('#user'),
      passwd = $val('#passwd'),
      vcode = $val('.vcode-row input')

    if (!user || !passwd) return RbHighbar.create($L('请输入用户名和密码'))
    if ($('.vcode-row img').length > 0 && !vcode) return RbHighbar.create($L('请输入验证码'))

    const $btn = $('.login-submit button').button('loading')
    const url = `/user/user-login?user=${$encode(user)}&passwd=******&autoLogin=${$val('#autoLogin')}&vcode=${vcode || ''}`
    $.post(url, passwd, (res) => {
      if (res.error_code === 0) {
        const nexturl = $decode($urlp('nexturl'))
        let to = nexturl && nexturl.startsWith('http') ? null : nexturl
        if (res.data && res.data.login2FaMode) {
          to = `${rb.baseUrl}/user/login-2fa?token=${res.data.login2FaUserToken}`
          if (nexturl) to += `&nexturl=${$encode(nexturl)}`
        } else if (res.data && res.data.passwdExpiredDays) {
          to = `${rb.baseUrl}/settings/passwd-expired?d=${res.data.passwdExpiredDays}`
          if (nexturl) to += `&nexturl=${$encode(nexturl)}`
        } else if (!to) {
          to = `${rb.baseUrl}/dashboard/home`
        }

        location.replace(to)
      } else if (res.error_msg === 'VCODE') {
        location.reload()
      } else {
        $('.vcode-row img').trigger('click')
        $('.vcode-row input').val('')

        RbHighbar.create(res.error_msg)
        $btn.button('reset')
      }
    })
  })
})
