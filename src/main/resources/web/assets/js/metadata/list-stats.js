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
      RbAlertFree43.create($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)'))
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
        scale: $this.attr('data-scale'),
        unit: $this.attr('data-unit'),
      })
    })

    $btn.button('loading')
    $.post(settingsUrl, JSON.stringify(config), (res) => {
      if (res.error_code === 0) {
        parent.location.reload()
      } else {
        $btn.button('reset')
        RbHighbar.error(res.error_msg)
      }
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
  if (len >= 9) return RbHighbar.create($L('最多可添加 9 个'))

  // 唯一
  if (!item.key2) item.key2 = $random('stat-')

  const $dest = $('.set-items')
  const calc = item.calc || 'SUM'
  const $item = $(
    `<span data-field="${item.name}" data-calc="${calc}" data-label="${item.label2 || ''}" data-color="${item.color || ''}" data-scale="${item.scale || ''}" data-unit="${item.unit || ''}"></span>`,
  ).appendTo($dest)
  const $a = $(
    `<div class="item" data-toggle="dropdown"><a><i class="zmdi zmdi-chevron-down"></i></a><span>${item.label} (${CALC_TYPES[calc]})</span><a class="del"><i class="zmdi zmdi-close-circle"></i></a></div>`,
  ).appendTo($item)
  $a.find('a.del').on('click', () => {
    $item.remove()
    parent.RbModal.resize()
  })

  const $ul = $('<ul class="dropdown-menu dropdown-menu-sm"></div>').appendTo($item)
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
            scale={item.scale}
            unit={item.unit}
            onConfirm={(s) => {
              $item.attr({
                'data-label': s.label || '',
                'data-color': s.color || '',
                'data-scale': s.scale || '',
                'data-unit': s.unit || '',
              })
              _refreshConfigStar()
            }}
          />,
          function () {
            ShowStyles_Comps[item.key2] = this
          },
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
    if ($this.attr('data-label') || $this.attr('data-color') || $this.attr('data-scale') || $this.attr('data-unit')) $this.find('.item').addClass('star')
    else $this.find('.item').removeClass('star')
  })
  parent.RbModal && parent.RbModal.resize()
}

// eslint-disable-next-line no-undef
class ShowStyles2 extends ShowStyles {
  renderExtras() {
    return (
      <RF>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('小数位长度')}</label>
          <div className="col-sm-7">
            <select className="form-control form-control-sm" defaultValue={this.props.scale || ''} ref={(c) => (this._$scale = c)}>
              <option value="">{$L('默认')}</option>
              <option value="0">0</option>
              <option value="1">1</option>
              <option value="2">2</option>
              <option value="3">3</option>
              <option value="4">4</option>
              <option value="5">5</option>
              <option value="6">6</option>
            </select>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('数字单位')}</label>
          <div className="col-sm-7">
            <select className="form-control form-control-sm" defaultValue={this.props.unit || ''} ref={(c) => (this._$unit = c)}>
              <option value="">{$L('默认')}</option>
              <option value="1000">{$L('千')}</option>
              <option value="10000">{$L('万')}</option>
              <option value="100000">{$L('十万')}</option>
              <option value="1000000">{$L('百万')}</option>
              <option value="10000000">{$L('千万')}</option>
              <option value="100000000">{$L('亿')}</option>
            </select>
          </div>
        </div>
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
      </RF>
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
      scale: $(this._$scale).val() || null,
      unit: $(this._$unit).val() || null,
    }
    typeof this.props.onConfirm === 'function' && this.props.onConfirm(data)
    this.hide()
  }
}
