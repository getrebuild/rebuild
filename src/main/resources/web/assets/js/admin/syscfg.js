/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// 系统参数设置
// 公共部分

$(document).ready(() => {
  $('.card-header>a').click((e) => {
    e.preventDefault()
    editMode()
  })
  $('.edit-footer>.btn-link').click(() => location.reload())
  $('.edit-footer>.btn-primary').click(() => post(__data))
})

const __data = {}
const changeValue = function (e) {
  const name = e.target.name
  __data[name] = e.target.value
}

// 激活编辑模式
const editMode = function () {
  $('.syscfg table td[data-id]').each(function () {
    const $item = $(this)
    const name = $item.data('id')
    const value = $item.data('value')

    const comp = useEditComp(name, value)
    if (comp) {
      renderRbcomp(comp, $item)
    } else {
      renderRbcomp(<input defaultValue={value} name={name} className="form-control form-control-sm" onChange={changeValue} />, $item)
    }
  })
  $('.syscfg').addClass('edit')
}

// 复写
// eslint-disable-next-line no-unused-vars
var useEditComp = function (name, value) {
  return null
}

// 提交
const post = function (data) {
  for (let k in data) {
    if (!data[k]) {
      const field = $('td[data-id=' + k + ']')
        .prev()
        .text()
      RbHighbar.create($L('SomeNotEmpty').replace('{0}', field))
      return false
    }
  }

  const btn = $('.edit-footer>.btn-primary').button('loading')
  $.post(location.href, JSON.stringify(data), (res) => {
    btn.button('reset')
    if (res.error_code === 0) location.reload()
    else RbHighbar.error(res.error_msg)
  })
}
