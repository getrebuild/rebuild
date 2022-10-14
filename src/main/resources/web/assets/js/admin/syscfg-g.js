/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-undef, react/display-name */

const wpc = window.__PageConfig

$(document).ready(() => {
  const $dl = $('#_DefaultLanguage')
  $dl.text(wpc._LANGS[$dl.text()] || '中文')

  // 禁用
  // ;['PasswordExpiredDays', 'DBBackupsKeepingDays', 'RevisionHistoryKeepingDays', 'RecycleBinKeepingDays'].forEach((item) => {
  //   const $d = $(`td[data-id=${item}]`)
  //   if (~~$d.attr('data-value') <= 0) $d.text($L('已禁用')).addClass('text-danger')
  // })

  UCenter.query((bindAccount) => {
    if (bindAccount) {
      $('.J_cloudAccount .card-body>div').addClass('hide')
      const $bind = $('.J_cloudAccount .card-body>div:eq(1)').removeClass('hide')
      $bind.find('a').text(bindAccount)
    } else {
      $('.J_cloudAccount .btn').on('click', () => {
        UCenter.bind()
      })
    }
  })
})

useEditComp = function (name) {
  if (['OpenSignUp', 'LiveWallpaper', 'FileSharable', 'MarkWatermark', 'DBBackupsEnable', 'MultipleSessions', 'ShowViewHistory'].includes(name)) {
    return (
      <select className="form-control form-control-sm">
        <option value="true">{$L('是')}</option>
        <option value="false">{$L('否')}</option>
      </select>
    )
  } else if ('PasswordPolicy' === name) {
    return (
      <select className="form-control form-control-sm">
        <option value="1">{$L('低 (最低6位，无字符类型限制)')}</option>
        <option value="2">{$L('中 (最低6位，必须同时包含数字、字母)')}</option>
        <option value="3">{$L('高 (最低8位，必须同时包含数字、字母、特殊字符)')}</option>
      </select>
    )
  } else if ('DefaultLanguage' === name) {
    // 借用贵宝地
    _toggleImage('.applogo')
    _toggleImage('.bgimg')

    const options = []
    for (let k in wpc._LANGS) {
      options.push(
        <option value={k} key={k}>
          {wpc._LANGS[k]}
        </option>
      )
    }
    return <select className="form-control form-control-sm">{options}</select>
  } else if ('LoginCaptchaPolicy' === name) {
    return (
      <select className="form-control form-control-sm">
        <option value="1">{$L('自动')}</option>
        <option value="2">{$L('总是显示')}</option>
      </select>
    )
  } else if ('PageFooter' === name || 'AllowUsesTime' === name || 'AllowUsesIp' === name) {
    return <textarea name={name} className="form-control form-control-sm row3x" maxLength="600" />
  } else if ('Login2FAMode' === name) {
    return (
      <select className="form-control form-control-sm">
        <option value="0">{$L('不启用')}</option>
        <option value="1">{$L('手机或邮箱')}</option>
        <option value="2">{$L('仅手机')}</option>
        <option value="3">{$L('仅邮箱')}</option>
      </select>
    )
  }
}

const _toggleImage = function (el) {
  const $img = $(el).addClass('edit')
  $img.find('p').removeClass('hide')

  let $current
  const $input = $img.find('input')
  $initUploader($input, null, (res) => {
    $current.find('>i').css('background-image', `url(${rb.baseUrl}/filex/img/${res.key.replace(/ /g, '%20')}?local=true)`)
    changeValue({ target: { name: $current.data('id'), value: res.key } })
  })

  $img
    .find('a')
    .attr('title', $L('点击上传'))
    .on('click', function () {
      $current = $(this)
      $input[0].click()
    })
  $img.find('a>b').on('click', function (e) {
    $stopEvent(e, true)
    $current = $(this).parent()

    $current.find('>i').css('background-image', `url(${rb.baseUrl}/assets/img/s.gif)`)
    changeValue({ target: { name: $current.data('id'), value: '' } })
  })
}
