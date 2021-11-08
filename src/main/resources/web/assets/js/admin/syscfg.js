/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// 系统参数设置
// 公共部分

$(document).ready(() => {
  $('.card-header>a').click((e) => {
    $stopEvent(e, true)
    enableEditMode()
  })

  $('.edit-footer>.btn-link').click(() => location.reload())
  $('.edit-footer>.btn-primary').click(() => post(__data))

  if (window.ClipboardJS) {
    $('a[data-clipboard-text]').each(function () {
      const $copy = $(this).on('mouseenter', () => $(this).removeClass('copied-check'))
      // eslint-disable-next-line no-undef
      new ClipboardJS(this).on('success', () => $copy.addClass('copied-check'))
    })
  }
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

    const c = useEditComp(name, value)
    if (c) {
      renderRbcomp(React.cloneElement(c, { name: name, onChange: changeValue, defaultValue: value, placeholder: optional ? $L('(选填)') : null }), $item)
    } else {
      renderRbcomp(<input className="form-control form-control-sm" name={name} onChange={changeValue} defaultValue={value} placeholder={optional ? $L('(选填)') : null} />, $item)
    }
  })
  $('.syscfg').addClass('edit')
}

// 提交
const post = function (data) {
  for (let name in data) {
    if (!data[name]) {
      const $field = $('td[data-id=' + name + ']')
      if ($field.data('optional')) continue // 可选值

      const $c = $field.prev().clone()
      $c.find('p').remove()

      RbHighbar.create($L('%s 不能为空', $c.text()))
      return false
    }
  }

  if (!(data = postBefore(data))) return false

  const $btn = $('.edit-footer>.btn-primary').button('loading')
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
