/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const VF_ACU = window.VF_ACU || '$APPROVALCURRENTUSER$'

$(document).ready(() => {
  const entity = $urlp('entity')
  const settingsUrl = `/admin/entity/${entity}/list-filterpane`

  $.get(settingsUrl, (res) => {
    const fields = []
    res.data.fields &&
      res.data.fields.forEach((item) => {
        fields.push(item)
        if (item.type === 'REFERENCE' && item.name === 'approvalLastUser') {
          const item2 = { ...item, name: VF_ACU, label: $L('当前审批人') }
          fields.push(item2)
        }
      })

    fields.forEach((item) => render_unset(item))

    if ((res.data.items || []).length > 0) {
      res.data.items.forEach((item) => {
        const field = fields.find((x) => x.name === item.field)
        render_set({ name: item.field, label: field ? field.label : `[${item.field.toUpperCase()}]` })
      })
    }

    parent.RbModal.resize()
  })

  // 字段排序
  $('.set-items')
    .sortable({
      containment: 'parent',
      placeholder: 'ui-state-highlight',
      opacity: 0.8,
    })
    .disableSelection()

  const $btn = $('.J_save').on('click', () => {
    if (rb.commercial < 1) {
      RbHighbar.error(WrapHtml($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }

    const config = { items: [] }
    $('.set-items > span').each(function () {
      config.items.push({
        field: $(this).attr('data-field'),
        op: null,
      })
    })

    if (config.items.length < 1) {
      RbHighbar.create(WrapHtml($L('请至少添加 1 个查询字段')))
      return
    }

    $btn.button('loading')
    $.post(settingsUrl, JSON.stringify(config), (res) => {
      $btn.button('reset')
      if (res.error_code === 0) parent.location.reload()
      else RbHighbar.error(res.error_msg)
    })
  })
})

const render_set = function (item) {
  const len = $('.set-items > span').length
  if (len >= 9) {
    RbHighbar.create($L('最多可添加 9 项'))
    return false
  }

  const $to = $('.set-items')

  const $item = $(`<span data-field="${item.name}" data-op=""></span>`).appendTo($to)
  const $a = $(`<div class="item"><span>${item.label}</span><a class="del"><i class="zmdi zmdi-close-circle"></i></a></div>`).appendTo($item)
  $a.find('a.del').on('click', () => {
    $item.remove()
    render_unset(item)
    parent.RbModal.resize()
  })

  $(`.set-fields a[data-field="${item.name}"]`).remove()
}

const render_unset = function (item) {
  const $item = $(`<a class="item" data-field="${item.name}">${item.label} +</a>`).appendTo('.set-fields')
  $item.on('click', () => {
    if (render_set(item) !== false) {
      $item.remove()
      parent.RbModal.resize()
    }
  })
}
