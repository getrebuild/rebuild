/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// 系统参数设置
// 公共部分

$(document).ready(() => {
  $('.card-header .J_edit').on('click', (e) => {
    $stopEvent(e, true)
    enableEditMode()

    $('.card-header .J_edit').addClass('hide')
    $('.card-header .J_save').removeClass('hide')
  })

  $('.edit-footer>.btn-link').on('click', () => location.reload())
  $('.edit-footer>.btn-primary, .card-header .J_save').on('click', (e) => {
    $stopEvent(e, true)
    post(__data)
  })

  $('a[data-clipboard-text]').each((idx, item) => $clipboard($(item)))
})

const __data = {}
const changeValue = function (e) {
  const name = e.target.name
  __data[name] = (e.target.value || '').trim()
}

// 激活编辑模式
const enableEditMode = function () {
  $('.syscfg table td[data-id]').each(function () {
    const $item = $(this)
    const name = $item.data('id')
    const value = $item.data('value')
    const optional = $item.data('optional')
    const formText = $item.data('form-text')

    let c = useEditComp(name, value)
    if (!c) c = <input className="form-control form-control-sm" />

    renderRbcomp(
      <RF>
        {React.cloneElement(c, { name: name, onChange: changeValue, defaultValue: value, placeholder: optional ? $L('(选填)') : null })}
        {formText && <p className="mt-2 text-dark" dangerouslySetInnerHTML={{ __html: formText }} />}
      </RF>,
      $item
    )
  })

  $('.syscfg').addClass('edit')
}

// 提交
const post = function (data) {
  for (let name in data) {
    if (!data[name]) {
      const $field = $(`.syscfg td[data-id=${name}]`)
      if ($field.length === 0 || $field.data('optional')) continue // 可选值

      const $c = $field.prev().clone()
      $c.find('p').remove()

      // `submail.html`
      if ($('.email-set')[0]) {
        if ($('.email-set').hasClass('smtp')) $c.find('.smtp-hide').remove()
        else $c.find('.smtp-show').remove()
      }

      RbHighbar.create($L('%s不能为空', $c.text()))
      return false
    }
  }

  if (!(data = postBefore(data))) return false

  const $btn = $('.edit-footer>.btn-primary, .card-header .J_save').button('loading')
  $.post(location.href, JSON.stringify(data), (res) => {
    $btn.button('reset')
    if (res.error_code === 0) location.reload()
    else RbHighbar.error(res.error_msg)
  })
}

// 复写-指定编辑控件
// eslint-disable-next-line no-unused-vars
var useEditComp = function (name, value) {
  return null
}

// 复写-保存前检查
var postBefore = function (data) {
  return data
}
