/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(() => {
  const entity = $urlp('entity')
  const settingsUrl = `/admin/entity/${entity}/list-stats`
  $.get(settingsUrl, (res) => {
    const fields = res.data.fields || []
    const $to = $('.set-fields')
    fields.forEach((item) => {
      const $a = $(`<a class="item" data-field="${item.name}">${item.label} +</a>`).appendTo($to)
      $a.on('click', () => {
        render_set(item)
        parent.RbModal && parent.RbModal.resize()
      })
    })

    if ((res.data.items || []).length > 0) {
      res.data.items.forEach((item) => {
        const field = fields.find((x) => x.name === item.field)
        render_set({ ...item, name: item.field, specLabel: item.label, label: field ? field.label : `[${item.field.toUpperCase()}]` })
      })
      _refreshConfigStar()
    }

    parent.RbModal && parent.RbModal.resize()
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
    $('.set-items>span').each(function () {
      const $this = $(this)
      config.items.push({
        field: $this.attr('data-field'),
        calc: $this.attr('data-calc'),
        label2: $this.attr('data-label'),
        color: $this.attr('data-color'),
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
  const len = $('.set-items>span').length
  if (len >= 99) return RbHighbar.create($L('最多可添加 9 个'))

  // 唯一
  if (!item.key2) item.key2 = $random('stat-')

  const $dest = $('.set-items')
  const calc = item.calc || 'SUM'
  const $item = $(`<span data-field="${item.name}" data-calc="${calc}" data-label="${item.label2 || ''}" data-color="${item.color || ''}"></span>`).appendTo($dest)
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
      if (ShowStyles_Comps[item.key2]) {
        ShowStyles_Comps[item.key2].show()
      } else {
        renderRbcomp(
          <ShowStyles2
            label={item.label2}
            color={item.color}
            onConfirm={(s) => {
              $item.attr({
                'data-label': s.label || '',
                'data-color': s.color || '',
              })
              _refreshConfigStar()
            }}
          />,
          function () {
            ShowStyles_Comps[item.key2] = this
          }
        )
      }
    } else {
      $item.attr('data-calc', calc).find('.item>span').text(`${item.label} (${CALC_TYPES[calc]})`)
    }
  })
}

const _refreshConfigStar = function () {
  $('.set-items>span').each(function () {
    const $this = $(this)
    if ($this.attr('data-label') || $this.attr('data-color')) $this.find('.item').addClass('star')
    else $this.find('.item').removeClass('star')
  })
  parent.RbModal && parent.RbModal.resize()
}

// eslint-disable-next-line no-undef
class ShowStyles2 extends ShowStyles {
  renderExtras() {
    return (
      <div className="form-group row pt-1 pb-0">
        <label className="col-sm-3 col-form-label text-sm-right">{$L('颜色')}</label>
        <div className="col-sm-7">
          <div className="rbcolors mt-1">
            <a className="default" title={$L('默认')} />
            {RBCOLORS.map((c) => {
              return <a style={{ backgroundColor: c }} data-color={c} key={c} />
            })}
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount()

    const $cs = $('.rbcolors')
    $cs.find('>a').on('click', function () {
      $cs.find('>a .zmdi').remove()
      $('<i class="zmdi zmdi-check"></i>').appendTo(this)
    })
    $('<input type="color" />')
      .appendTo($cs)
      .on('change', () => {
        $cs.find('>a .zmdi').remove()
      })

    // init
    if (this.props.color) {
      $cs.find(`a[data-color="${this.props.color}"]`).trigger('click')
      $('.rbcolors>input').val(this.props.color)
    } else {
      $('.rbcolors>a:eq(0)').trigger('click')
    }
  }

  saveProps() {
    let color = $('.rbcolors>a>i')
    if (color[0]) color = color.parent().data('color') || ''
    else color = $('.rbcolors>input').val() || ''

    const data = {
      label: $(this._$label).val() || '',
      color: color === '#000000' ? null : color,
    }
    typeof this.props.onConfirm === 'function' && this.props.onConfirm(data)
    this.hide()
  }
}
