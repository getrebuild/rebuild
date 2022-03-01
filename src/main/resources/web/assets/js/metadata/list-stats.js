/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(function () {
  const entity = $urlp('entity')
  const settingsUrl = `/admin/entity/${entity}/list-stats`

  $.get(settingsUrl, (res) => {
    const fields = res.data.fields || []

    const $to = $('.set-fields')
    if (fields.length > 0) $to.empty()

    fields.forEach((item) => {
      const $a = $(`<a class="item" data-field="${item.name}">${item.label} +</a>`).appendTo($to)
      $a.on('click', () => {
        render_set(item)
        parent.RbModal.resize()
      })
    })

    if ((res.data.items || []).length > 0) {
      res.data.items.forEach((item) => {
        const field = fields.find((x) => x.name === item.field)
        render_set({ ...item, name: item.field, specLabel: item.label, label: field ? field.label : `[${item.field.toUpperCase()}]` })
      })
    }

    parent.RbModal.resize()
  })

  // // 字段排序
  $('.set-items')
    .sortable({
      containment: 'parent',
      cursor: 'move',
      opacity: 0.8,
      placeholder: 'ui-state-highlight',
    })
    .disableSelection()

  const $btn = $('.J_save').on('click', () => {
    if (rb.commercial < 1) {
      RbHighbar.error(WrapHtml($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }

    const config = { items: [] }
    $('.set-items > span').each(function () {
      const $this = $(this)
      config.items.push({
        field: $this.attr('data-field'),
        calc: $this.attr('data-calc'),
        label2: $this.attr('data-label'),
      })
    })

    $btn.button('loading')
    $.post(settingsUrl, JSON.stringify(config), (res) => {
      $btn.button('reset')
      if (res.error_code === 0) parent.location.reload()
      else RbHighbar.error(res.error_msg)
    })
  })
})

// 支持的计算类型
const CALC_TYPES = {
  'SUM': $L('求和'),
  'AVG': $L('平均值'),
  'MAX': $L('最大值'),
  'MIN': $L('最小值'),
}

const ShowStyles_Comps = {}

const render_set = function (item) {
  const len = $('.set-items > span').length
  if (len >= 3) $('.J_tips').removeClass('hide')
  if (len >= 9) {
    RbHighbar.create($L('最多可添加 9 项'))
    return
  }

  const $to = $('.set-items')
  $to.find('>span.text-muted').remove()

  const calc = item.calc || 'SUM'
  const $item = $(`<span data-field="${item.name}" data-calc="${calc}" data-label="${item.label2 || ''}"></span>`).appendTo($to)

  const $a = $(
    `<div class="item" data-toggle="dropdown"><a><i class="zmdi zmdi-chevron-down"></i></a><span>${item.label} (${CALC_TYPES[calc]})</span><a class="del"><i class="zmdi zmdi-close-circle"></i></a></div>`
  ).appendTo($item)
  $a.find('a.del').on('click', () => {
    $item.remove()
    parent.RbModal.resize()
  })

  const $ul = $('<ul class="dropdown-menu"></div>').appendTo($item)
  for (let k in CALC_TYPES) {
    $(`<li class="dropdown-item" data-calc=${k}>${CALC_TYPES[k]}</li>`).appendTo($ul)
  }
  $('<li class="dropdown-divider"></li>').appendTo($ul)
  $(`<li class="dropdown-item" data-calc='_LABEL'>${$L('显示样式')}</li>`).appendTo($ul)

  $ul.find('.dropdown-item').on('click', function () {
    const calc = $(this).data('calc')
    if (calc === '_LABEL') {
      if (ShowStyles_Comps[item.name]) {
        ShowStyles_Comps[item.name].show()
      } else {
        renderRbcomp(
          // eslint-disable-next-line react/jsx-no-undef
          <ShowStyles
            label={item.label2}
            onConfirm={(s) => {
              $item.attr({
                'data-label': s.label || '',
              })
            }}
          />,
          null,
          function () {
            ShowStyles_Comps[item.name] = this
          }
        )
      }
    } else {
      $item.attr('data-calc', calc).find('.item > span').text(`${item.label} (${CALC_TYPES[calc]})`)
    }
  })
}
