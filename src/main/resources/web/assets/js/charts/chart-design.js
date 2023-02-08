/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig

let dataFilter

$(document).ready(() => {
  $('.chart-type>a, .chart-option .zicon').tooltip({ html: true, container: '.config-aside' })
  if (wpc.chartOwningAdmin !== true) $('.admin-show').remove()

  $('.fields>li>a').each(function () {
    const $this = $(this)
    if ($isSysMask($this.text())) $this.remove()
  })

  // 字段拖动
  let dragIsNum = false
  let dargOnSort = false
  $('.fields>li>a')
    .draggable({
      helper: 'clone',
      appendTo: 'body',
      cursor: 'move',
      cursorAt: { top: 14, left: 75 },
      zIndex: 1999,
      start: function () {
        dragIsNum = $(this).data('type') === 'num'
      },
    })
    .disableSelection()
  $('.axis-target')
    .droppable({
      accept: function () {
        if (dargOnSort === true) return false
        if ($(this).hasClass('J_axis-dim')) return !dragIsNum
        else return true
      },
      drop: function (event, ui) {
        if (dargOnSort !== true) add_axis(this, $(ui.draggable[0]))
      },
    })
    .disableSelection()
  // 字段排序
  $('.axis-target')
    .sortable({
      axis: 'x',
      containment: 'parent',
      cursor: 'move',
      opacity: 0.8,
      start: function () {
        dargOnSort = true
      },
      stop: function () {
        dargOnSort = false
        render_preview()
      },
    })
    .disableSelection()

  const saveFilter = function (filter) {
    dataFilter = filter
    render_preview()
  }

  let _AdvFilter
  $('.J_filter').on('click', (e) => {
    $stopEvent(e, true)
    if (_AdvFilter) {
      _AdvFilter.show()
    } else {
      renderRbcomp(<AdvFilter title={$L('数据过滤条件')} entity={wpc.sourceEntity} filter={dataFilter} onConfirm={saveFilter} inModal canNoFilters />, null, function () {
        _AdvFilter = this
      })
    }
  })

  const $types = $('.chart-type > a').on('click', function () {
    const $this = $(this)
    if ($this.hasClass('active') === false) return
    $types.removeClass('select')
    $this.addClass('select')
    render_option()
  })

  $('.chart-option .custom-control').on('click', () => render_option())

  // 保存按钮
  $('.rb-toggle-left-sidebar')
    .attr('title', $L('保存'))
    .off('click')
    .on('click', () => {
      const cfg = build_config()
      if (!cfg) return RbHighbar.create($L('当前图表无数据'))

      const data = {
        config: JSON.stringify(cfg),
        title: cfg.title,
        belongEntity: cfg.entity,
        chartType: cfg.type,
        metadata: { entity: 'ChartConfig', id: wpc.chartId },
      }

      const dash = $urlp('dashid') || ''
      $.post(`/dashboard/chart-save?dashid=${dash}`, JSON.stringify(data), function (res) {
        if (res.error_code === 0) {
          wpc.chartConfig = cfg
          location.href = (dash ? 'home?d=' + dash : 'home') + '#' + res.data.id
        } else {
          RbHighbar.error(res.error_msg)
        }
      })
    })
    .tooltip({ placement: 'right' })
    .find('.zmdi')
    .addClass('zmdi-arrow-left')

  // Colors
  const $cs = $('.rbcolors')
  RBCOLORS.forEach((c) => {
    $(`<a style="background-color:${c}" data-color="${c}"></a>`).appendTo($cs)
  })
  $cs.find('>a').on('click', function () {
    $cs.find('>a .zmdi').remove()
    $('<i class="zmdi zmdi-check"></i>').appendTo(this)
    render_preview()
  })

  // Load
  if (wpc.chartConfig && wpc.chartConfig.axis) {
    $(wpc.chartConfig.axis.dimension).each((idx, item) => add_axis('.J_axis-dim', item))
    $(wpc.chartConfig.axis.numerical).each((idx, item) => add_axis('.J_axis-num', item))
    $(`.chart-type>a[data-type="${wpc.chartConfig.type}"]`).trigger('click')

    dataFilter = wpc.chartConfig.filter
    if (dataFilter && (dataFilter.items || []).length > 0) {
      $('a.J_filter > span:eq(1)').text(`(${dataFilter.items.length})`)
    }

    const option = wpc.chartConfig.option || {}
    if (typeof option['mergeCell'] === undefined) option.mergeCell = true // fix: 3.1.3
    for (let k in option) {
      const opt = $(`.chart-option input[data-name=${k}]`)
      if (opt.length > 0) {
        if (opt.attr('type') === 'checkbox') {
          if ($isTrue(option[k])) opt.trigger('click')
        } else {
          opt.val(option[k])
        }
      }

      if (k === 'useColor') {
        $cs.find(`a[data-color="${option[k]}"]`).trigger('click')
      }
    }
  }

  if (!wpc.chartId) {
    $(`<h4 class="chart-undata must-center">${$L('当前图表无数据')}</h4>`).appendTo('#chart-preview')
    typeof window.startTour === 'function' && window.startTour(500)
  }

  $addResizeHandler(() => {
    $('#chart-preview').height($(window).height() - 170)
    if (render_preview_chart && render_preview_chart.resize) render_preview_chart.resize()
  })()

  window.onbeforeunload = function () {
    const cfg = build_config()
    if ((!cfg && !wpc.chartId) || $same(cfg, wpc.chartConfig)) return undefined
    return 'CLOSE CONFIRM'
  }
})

const CTs = {
  SUM: $L('求和'),
  AVG: $L('平均值'),
  MAX: $L('最大值'),
  MIN: $L('最小值'),
  COUNT: $L('计数'),
  COUNT2: $L('去重计数'),
  Y: $L('按年'),
  Q: $L('按季'),
  M: $L('按月'),
  D: $L('按日'),
  H: $L('按时'),
  I: $L('按时分'),
  L1: $L('1级'),
  L2: $L('2级'),
  L3: $L('3级'),
  L4: $L('4级'),
}

let dlgAxisProps
// 添加维度
const add_axis = (target, axis) => {
  const $dropdown = $($('#axis-item').html()).appendTo($(target))
  let fieldName = null
  let fieldLabel = null
  let fieldType = null
  let calc = null
  let sort = 'NONE'

  const isNumAxis = $(target).hasClass('J_axis-num')
  // Edit
  if (axis.field) {
    const field = $(`.fields [data-field="${axis.field}"]`)
    fieldName = axis.field
    fieldLabel = field.text()
    fieldType = field.data('type')
    sort = axis.sort
    calc = axis.calc
    $dropdown.attr({ 'data-label': axis.label, 'data-scale': axis.scale })
  }
  // New adds
  else {
    fieldName = axis.data('field')
    fieldLabel = axis.text()
    fieldType = axis.data('type')
    if (isNumAxis) {
      calc = fieldType === 'num' ? 'SUM' : 'COUNT'
    } else if (fieldType === 'date') {
      calc = 'D'
    } else if (fieldType === 'time') {
      calc = 'I'
    } else if (fieldType === 'clazz') {
      calc = 'L1'
    }
  }
  $dropdown.attr({ 'data-calc': calc, 'data-sort': sort })

  fieldLabel = fieldLabel || `[${fieldName.toUpperCase()}]`

  if (isNumAxis) {
    $dropdown.find('.J_date, .J_time, .J_clazz').remove()
    if (fieldType === 'num') $dropdown.find('.J_text').remove()
    else $dropdown.find('.J_num').remove()
  } else {
    $dropdown.find('.J_text, .J_num').remove()
    if (fieldType !== 'date') $dropdown.find('.J_date').remove()
    if (fieldType !== 'time') $dropdown.find('.J_time').remove()
    if (fieldType !== 'clazz') $dropdown.find('.J_clazz').remove()
  }
  if ($dropdown.find('li:eq(0)').hasClass('dropdown-divider')) $dropdown.find('.dropdown-divider').remove()

  // Click option
  const aopts = $dropdown.find('.dropdown-menu .dropdown-item').on('click', function () {
    const $this = $(this)
    if ($this.hasClass('disabled') || $this.parent().hasClass('disabled')) return false

    const calc = $this.data('calc')
    const sort = $this.data('sort')
    if (calc) {
      $dropdown.find('span').text(`${fieldLabel} (${$this.text()})`)
      $dropdown.attr('data-calc', calc)
      aopts.each(function () {
        if ($(this).data('calc')) $(this).removeClass('text-primary')
      })
      $this.addClass('text-primary')
      render_preview()
    } else if (sort) {
      $dropdown.attr('data-sort', sort)
      aopts.each(function () {
        if ($(this).data('sort')) $(this).removeClass('text-primary')
      })
      $this.addClass('text-primary')
      render_preview()
    } else {
      const state = {
        isNumAxis: isNumAxis,
        label: $dropdown.attr('data-label'),
        scale: $dropdown.attr('data-scale'),
      }
      state.callback = (s) => {
        $dropdown.attr({ 'data-label': s.label, 'data-scale': s.scale })
        render_preview()
      }

      if (dlgAxisProps) {
        dlgAxisProps.show(state)
      } else {
        renderRbcomp(<DlgAxisProps {...state} />, null, function () {
          dlgAxisProps = this
        })
      }
    }
  })

  if (calc) $dropdown.find(`.dropdown-menu li[data-calc="${calc}"]`).addClass('text-primary')
  if (sort) $dropdown.find(`.dropdown-menu li[data-sort="${sort}"]`).addClass('text-primary')

  $dropdown.attr({ 'data-type': fieldType, 'data-field': fieldName })
  $dropdown.find('span').text(fieldLabel + (calc ? ` (${CTs[calc]})` : ''))
  $dropdown.find('a.del').on('click', () => {
    $dropdown.remove()
    render_option()
  })
  render_option()
}

// 图表选项
const render_option = () => {
  const cts = $('.chart-type>a').removeClass('active')
  const dimsAxis = $('.J_axis-dim .item').length
  const numsAxis = $('.J_axis-num .item').length

  cts.each(function () {
    const $this = $(this)
    const dims = ($this.data('allow-dims') || '0|0').split('|')
    const nums = ($this.data('allow-nums') || '0|0').split('|')
    if (dimsAxis >= ~~dims[0] && dimsAxis <= ~~dims[1] && numsAxis >= ~~nums[0] && numsAxis <= ~~nums[1]) $this.addClass('active')
  })
  // FUNNEL
  if ((dimsAxis === 1 && numsAxis === 1) || (dimsAxis === 0 && numsAxis > 1));
  else $('.chart-type>a[data-type="FUNNEL"]').removeClass('active')

  // Active
  let $select = $('.chart-type>a.select')
  if (!$select.hasClass('active')) $select.removeClass('select')
  $select = $('.chart-type>a.select')
  if ($select.length === 0) $select = $('.chart-type>a.active').eq(0).addClass('select')

  const ct = $select.data('type')
  // Option
  $('.chart-option>div').addClass('hide')
  const ctOpt = $(`.J_opt-ALL, .J_opt-${ct}`)
  if (ctOpt.length === 0) $('.J_opt-UNDEF').removeClass('hide')
  else ctOpt.removeClass('hide')

  // Sort
  const sorts = $('.axis-editor .J_sort').removeClass('disabled')
  if (ct === 'INDEX') {
    sorts.addClass('disabled')
  } else if (ct === 'FUNNEL') {
    if (numsAxis >= 1 && dimsAxis >= 1) $('.J_numerical .J_sort').addClass('disabled')
    else sorts.addClass('disabled')
  }

  render_preview()
}

// 生成预览
let render_preview_chart = null
const render_preview = () => {
  const $fs = $('a.J_filter > span:eq(1)')
  if (dataFilter && (dataFilter.items || []).length > 0) {
    $fs.text(`(${dataFilter.items.length})`)
  } else {
    $fs.text('')
  }

  $setTimeout(
    () => {
      if (render_preview_chart) {
        ReactDOM.unmountComponentAtNode(document.getElementById('chart-preview'))
        render_preview_chart = null
      }

      const cfg = build_config()
      if (!cfg) {
        $('#chart-preview').html(`<h4 class="chart-undata must-center">${$L('当前图表无数据')}</h4>`)
        return
      }

      $('#chart-preview').empty()
      // eslint-disable-next-line no-undef
      const c = detectChart(cfg)
      if (c) {
        renderRbcomp(c, 'chart-preview', function () {
          render_preview_chart = this
        })
      } else {
        $('#chart-preview').html(`<h4 class="chart-undata must-center">${$L('不支持的图表类型')}</h4>`)
      }
    },
    400,
    'chart-preview'
  )
}

// 构造配置
const build_config = () => {
  const cfg = { entity: wpc.sourceEntity, title: $val('#chart-title') || $L('未命名图表') }
  cfg.type = $('.chart-type>a.select').data('type')
  if (!cfg.type) return

  const dims = []
  const nums = []
  $('.J_axis-dim>span').each((idx, item) => dims.push(__buildAxisItem(item, false)))
  $('.J_axis-num>span').each((idx, item) => nums.push(__buildAxisItem(item, true)))
  if (dims.length === 0 && nums.length === 0) return
  cfg.axis = { dimension: dims, numerical: nums }

  const option = {}
  $('.chart-option input').each(function () {
    const name = $(this).data('name')
    if (name) option[name] = $val(this)
  })

  const color = $('.rbcolors >a>i').parent().data('color') || ''
  option.useColor = color || ''

  cfg.option = option

  if (dataFilter) cfg.filter = dataFilter
  // eslint-disable-next-line no-console
  if (rb.env === 'dev') console.log(cfg)
  return cfg
}

const __buildAxisItem = (item, isNum) => {
  item = $(item)
  const x = {
    field: item.data('field'),
    sort: item.attr('data-sort') || '',
    label: item.attr('data-label') || '',
  }
  if (isNum) {
    x.calc = item.attr('data-calc')
    x.scale = item.attr('data-scale')
  } else if (['date', 'time', 'clazz'].includes(item.data('type'))) {
    x.calc = item.attr('data-calc')
  }
  return x
}

class DlgAxisProps extends RbFormHandler {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <RbModal title={$L('显示样式')} ref={(c) => (this._dlg = c)}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('别名')}</label>
            <div className="col-sm-7">
              <input className="form-control form-control-sm" placeholder={$L('默认')} data-id="label" value={this.state.label || ''} onChange={this.handleChange} />
            </div>
          </div>
          {this.state.isNumAxis && (
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('小数位长度')}</label>
              <div className="col-sm-7">
                <select className="form-control form-control-sm" value={this.state.scale || 2} data-id="scale" onChange={this.handleChange}>
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
          )}
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3">
              <button className="btn btn-primary" type="button" onClick={() => this.saveProps()}>
                {$L('确定')}
              </button>
              <a className="btn btn-link" onClick={() => this.hide()}>
                {$L('取消')}
              </a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  saveProps() {
    this.state.callback(this.state)
    this.hide()
  }
}
