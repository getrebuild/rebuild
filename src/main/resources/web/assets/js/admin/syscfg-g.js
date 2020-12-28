/* eslint-disable no-undef */
/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig

$(document).ready(() => {
  const $L = $('#_DefaultLanguage')
  $L.text(wpc._LANGS[$L.text()] || 'UNKONWN')
})

useEditComp = function (name, value) {
  if (['OpenSignUp', 'LiveWallpaper', 'FileSharable', 'MarkWatermark', 'DBBackupsEnable', 'MultipleSessions'].includes(name)) {
    return (
      <select name={name} className="form-control form-control-sm" onChange={changeValue} defaultValue={value}>
        <option value="true">{$L('True')}</option>
        <option value="false">{$L('False')}</option>
      </select>
    )
  } else if ('PasswordPolicy' === name) {
    return (
      <select name={name} className="form-control form-control-sm" onChange={changeValue} defaultValue={value}>
        <option value="1">{$L('PasswordPolicyL1')}</option>
        <option value="2">{$L('PasswordPolicyL2')}</option>
        <option value="3">{$L('PasswordPolicyL3')}</option>
      </select>
    )
  } else if ('DefaultLanguage' === name) {
    // 借用贵宝地
    _toggleLogo()

    const options = []
    for (let k in wpc._LANGS)
      options.push(
        <option value={k} key={k}>
          {wpc._LANGS[k]}
        </option>
      )
    return (
      <select name={name} className="form-control form-control-sm" onChange={changeValue} defaultValue={value}>
        {options}
      </select>
    )
  }
}

const _toggleLogo = function () {
  const $logo = $('.applogo')
  $logo.find('p').removeClass('hide')

  let $current
  const $input = $logo.find('input')
  $initUploader($input, null, (res) => {
    const $img = $current.find('i').css('background-image', `url(${rb.baseUrl}/filex/img/${res.key}?temp=true)`)
    changeValue({ target: { name: $img.hasClass('white') ? 'LOGOWhite' : 'LOGO', value: res.key } })
  })

  $logo.find('a').click(function () {
    $current = $(this)
    $input[0].click()
  })
}
