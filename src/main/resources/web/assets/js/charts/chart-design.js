/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig

let dataFilter
// v3.6 数据列表
let type_DATALIST2

$(document).ready(() => {
  if (wpc.chartOwningAdmin !== true) $('.admin-show').remove()

  function _clear2(n) {
    let x = n.split('|')
    if (x[0] === x[1]) return x[0]
    if (~~x[0] === 0 && ~~x[1] === 0) return 0
    return x[0] + '~' + x[1]
  }
  $('.chart-type>a').each((idx, item) => {
    const $item = $(item)
    const dims = _clear2($item.data('allow-dims'))
    const nums = _clear2($item.data('allow-nums'))
    let title = $L('%s个维度 %s个数值', dims, nums)
    if (['LINE', 'BAR', 'BAR2', 'BAR3'].includes($item.data('type'))) {
      title = $L('%s个维度 %s个数值', 2, 1) + '<br/>' + $L('%s个维度 %s个数值', 1, '1~9')
    } else if (['FUNNEL'].includes($item.data('type'))) {
      title = $L('%s个维度 %s个数值', 1, 1) + '<br/>' + $L('%s个维度 %s个数值', 0, '2~9')
    }
    $item.attr('title', $item.attr('title') + '<br/>' + title)
  })
  $('.chart-type>a, .chart-option .zicon').tooltip({ html: true, container: '.config-aside' })

  $('.fields>li>a').each(function () {
    const $this = $(this)
    if ($isSysMask($this.text())) $this.remove()
  })

  // 字段拖动
  let dragIsNum = false
  let dargOnSort = false
  setTimeout(() => {
    $('.fields>li>a')
      .draggable({
        helper: 'clone',
        appendTo: 'body',
        cursor: 'move',
        zIndex: 1999,
        start: function () {
          dragIsNum = $(this).data('type') === 'num'
        },
        stop: function () {
          dragIsNum = false
        },
        classes: {
          'ui-draggable-dragging': 'ui-draggable-dragging field',
        },
      })
      .disableSelection()
    $('.axis-target')
      .droppable({
        accept: function () {
          if (dargOnSort === true) return false
          const isDim = $(this).hasClass('J_axis-dim')
          if (type_DATALIST2) return isDim
          if (isDim) return !dragIsNum
          return true
        },
        drop: function (e, ui) {
          if (dargOnSort !== true) {
            add_axis(this, $(ui.draggable[0]))
            $('.axis-target').sortable('refresh')
          }
        },
      })
      .disableSelection()
    // 字段排序
    $('.axis-target')
      .sortable({
        axis: 'x',
        containment: 'parent',
        cursor: 'move',
        forcePlaceholderSize: true,
        forceHelperSize: true,
        start: function () {
          dargOnSort = true
        },
        stop: function () {
          dargOnSort = false
          render_preview()
        },
      })
      .disableSelection()
  }, 1000)

  let _AdvFilter
  $('.J_filter').on('click', (e) => {
    $stopEvent(e, true)
    if (_AdvFilter) {
      _AdvFilter.show()
    } else {
      renderRbcomp(
        <AdvFilter
          title={$L('附加过滤条件')}
          entity={wpc.sourceEntity}
          filter={dataFilter}
          onConfirm={(s) => {
            dataFilter = s
            render_preview()
          }}
          inModal
          canNoFilters
        />,
        function () {
          _AdvFilter = this
        }
      )
    }
  })

  const $chTypes = $('.chart-type>a').on('click', function () {
    const $this = $(this)
    if ($this.hasClass('active') === false) return
    $chTypes.removeClass('select')
    $this.addClass('select')

    type_DATALIST2 = $this.data('type') === 'DATALIST2'
    if (!type_DATALIST2) $('.J_axis-dim span[data-type="num"]').remove()
    $('.rb-content').attr('class', `rb-content ${type_DATALIST2 ? 'DATALIST2' : ''}`)

    render_option()
  })

  $('.chart-option .custom-control').on('click', () => render_option())
  $('.chart-option input[type="text"]').on('blur', () => render_option())
  $('.chart-option select').on('change', () => render_option())

  // 保存按钮
  $('.rb-toggle-left-sidebar')
    .attr('title', $L('保存并返回'))
    .off('click')
    .on('click', () => {
      const hasError = $('#chart-preview .has-error').text()
      if (hasError) return RbHighbar.create(hasError)

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

  // 颜色
  const $cs = $('.rbcolors')
  RBCOLORS.forEach((c) => {
    $(`<a style="background-color:${c}" data-color="${c}"></a>`).appendTo($cs)
  })
  $cs.find('>a').on('click', function () {
    $cs.find('>a .zmdi').remove()
    $('<i class="zmdi zmdi-check"></i>').appendTo(this)
    render_preview()
  })
  $('<input type="color" />')
    .appendTo($cs)
    .on('change', () => {
      $cs.find('>a .zmdi').remove()
      render_preview()
    })

  // init
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
      let $o = $(`.chart-option input[data-name=${k}]`)
      if (!$o[0]) $o = $(`.chart-option select[data-name=${k}]`)

      if ($o.length > 0) {
        if ($o.attr('type') === 'checkbox') {
          if ($isTrue(option[k])) $o.trigger('click')
        } else {
          $o.val(option[k])
        }
      }

      if (k === 'useColor' && option[k]) {
        $cs.find(`a[data-color="${option[k]}"]`).trigger('click')
        $('.rbcolors >input').val(option[k])
      }
    }
  }

  if (!wpc.chartId) {
    render_preview_error($L('当前图表无数据'))
    typeof window.startTour === 'function' && window.startTour(500)
  }

  $addResizeHandler(() => {
    $('#chart-preview').height($(window).height() - 170)
    if (render_preview_chart && render_preview_chart.resize) render_preview_chart.resize()
  })()

  window.onbeforeunload = function () {
    const cfg = build_config()
    if ((!cfg && !wpc.chartId) || $same(cfg, wpc.chartConfig)) return undefined
    return 'SHOW-CLOSE-CONFIRM'
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

let _dlgAxisProps
let _axisAdvFilters = {}
let _axisAdvFilters__data = {}
// 添加维度
function add_axis($target, axis) {
  const $dd = $($('#axis-item').html()).appendTo($($target))
  let fieldName = null
  let fieldLabel = null
  let fieldType = null
  let calc = null
  let sort = 'NONE'
  let fkey = null

  const isNumAxis = $($target).hasClass('J_axis-num')
  // exists
  if (axis.field) {
    const copyField = $(`.fields [data-field="${axis.field}"]`)
    fieldName = axis.field
    fieldLabel = copyField.text()
    fieldType = copyField.data('type')
    calc = axis.calc
    sort = axis.sort
    fkey = axis.fkey
    if (axis.filter) _axisAdvFilters__data[axis.fkey] = axis.filter
    $dd.attr({ 'data-label': axis.label, 'data-scale': axis.scale, 'data-unit': axis.unit })
  }
  // New
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
  if (!fkey) fkey = $random('AXIS')
  $dd.attr({ 'data-calc': calc, 'data-sort': sort, 'data-fkey': fkey })

  fieldLabel = fieldLabel || `[${fieldName.toUpperCase()}]`

  if (isNumAxis) {
    $dd.find('.J_date, .J_time, .J_clazz').remove()
    if (fieldType === 'num') $dd.find('.J_text').remove()
    else $dd.find('.J_num').remove()
  } else {
    $dd.find('.J_text, .J_num, .J_filter').remove()
    if (fieldType !== 'date') $dd.find('.J_date').remove()
    if (fieldType !== 'time') $dd.find('.J_time').remove()
    if (fieldType !== 'clazz') $dd.find('.J_clazz').remove()
  }
  if ($dd.find('li:eq(0)').hasClass('dropdown-divider')) $dd.find('.dropdown-divider').remove()

  // Click option
  const aopts = $dd.find('.dropdown-menu .dropdown-item').on('click', function () {
    const $this = $(this)
    if ($this.hasClass('disabled') || $this.parent().hasClass('disabled')) return false

    const calc = $this.data('calc')
    const sort = $this.data('sort')
    if (calc) {
      $dd.find('span').text(`${fieldLabel} (${$this.text()})`)
      $dd.attr('data-calc', calc)
      aopts.each(function () {
        if ($(this).data('calc')) $(this).removeClass('text-primary')
      })
      $this.addClass('text-primary')
      render_preview()
    } else if (sort) {
      $dd.attr('data-sort', sort)
      aopts.each(function () {
        if ($(this).data('sort')) $(this).removeClass('text-primary')
      })
      $this.addClass('text-primary')
      render_preview()
    } else if ($this.hasClass('J_filter')) {
      // v3.7
      if (_axisAdvFilters[fkey]) {
        _axisAdvFilters[fkey].show()
      } else {
        renderRbcomp(
          <AdvFilter
            title={$L('过滤条件')}
            entity={wpc.sourceEntity}
            filter={_axisAdvFilters__data[fkey] || null}
            onConfirm={(s) => {
              _axisAdvFilters__data[fkey] = s
              render_preview()
            }}
            inModal
            canNoFilters
          />,
          function () {
            _axisAdvFilters[fkey] = this
          }
        )
      }
    } else {
      const state = {
        isNumAxis: isNumAxis,
        label: $dd.attr('data-label'),
        scale: $dd.attr('data-scale'),
        unit: $dd.attr('data-unit'),
      }
      state.callback = (s) => {
        $dd.attr({ 'data-label': s.label, 'data-scale': s.scale, 'data-unit': s.unit })
        render_preview()
      }

      if (_dlgAxisProps) {
        _dlgAxisProps.show(state)
      } else {
        renderRbcomp(<DlgAxisProps {...state} />, function () {
          _dlgAxisProps = this
        })
      }
    }
  })

  if (calc) $dd.find(`.dropdown-menu li[data-calc="${calc}"]`).addClass('text-primary')
  if (sort) $dd.find(`.dropdown-menu li[data-sort="${sort}"]`).addClass('text-primary')

  $dd.attr({ 'data-type': fieldType, 'data-field': fieldName })
  $dd.find('span').html(fieldLabel + (calc ? `<em>(${CTs[calc]})</em>` : ''))
  $dd.find('a.del').on('click', () => {
    $dd.remove()
    render_option()
  })
  render_option()
}

// 图表选项
function render_option() {
  const $types = $('.chart-type>a').removeClass('active')
  const dimsAxis = $('.J_axis-dim .item').length
  const numsAxis = $('.J_axis-num .item').length

  $types.each(function () {
    const $this = $(this)
    const dims = ($this.data('allow-dims') || '0|0').split('|')
    const nums = ($this.data('allow-nums') || '0|0').split('|')
    if (dimsAxis >= ~~dims[0] && dimsAxis <= ~~dims[1] && numsAxis >= ~~nums[0] && numsAxis <= ~~nums[1]) $this.addClass('active')
  })
  // FUNNEL
  if ((dimsAxis === 1 && numsAxis === 1) || (dimsAxis === 0 && numsAxis > 1));
  else $('.chart-type>a[data-type="FUNNEL"]').removeClass('active')
  // LINE/BAR/BAR2/BAR3
  if ((dimsAxis === 2 && numsAxis === 1) || (dimsAxis === 1 && numsAxis >= 1));
  else $('.chart-type>a[data-type="LINE"],.chart-type>a[data-type="BAR"],.chart-type>a[data-type="BAR2"],.chart-type>a[data-type="BAR3"]').removeClass('active')

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
  const $sort = $('.axis-editor .J_sort').removeClass('disabled')
  if (['INDEX', 'CNMAP'].includes(ct)) {
    $sort.addClass('disabled')
  } else if (ct === 'FUNNEL') {
    if (numsAxis >= 1 && dimsAxis >= 1) $('.J_numerical .J_sort').addClass('disabled')
    else $sort.addClass('disabled')
  }
  // v3.7 Filter
  const $filter = $('.axis-editor .J_filter').addClass('disabled')
  if (['INDEX', 'FUNNEL', 'TABLE', 'LINE', 'BAR', 'BAR2', 'BAR3'].includes(ct)) {
    $filter.removeClass('disabled')
  }

  render_preview()
}

let render_preview_chart = null
// 生成预览
function render_preview() {
  const $filterLen = $('a.J_filter > span:eq(1)')
  if (dataFilter && (dataFilter.items || []).length > 0) {
    $filterLen.text(`(${dataFilter.items.length})`)
  } else {
    $filterLen.text('')
  }

  $setTimeout(
    () => {
      if (render_preview_chart) {
        ReactDOM.unmountComponentAtNode(document.getElementById('chart-preview'))
        render_preview_chart = null
      }

      const conf = build_config()
      if (!conf) {
        render_preview_error($L('当前图表无数据'))
        return
      }

      if (!(conf.type === 'CNMAP' || conf.type === 'DATALIST2')) {
        if ($('.axis-editor span[data-type="map"]')[0]) {
          render_preview_error($L('选择的字段仅适用于“地图”、“数据列表”图表'))
          return
        }
        if ($('.axis-editor span[data-type="list"]')[0]) {
          render_preview_error($L('选择的字段仅适用于“数据列表”图表'))
          return
        }
      }

      $('#chart-preview').empty()
      // eslint-disable-next-line no-undef
      const c = detectChart(conf)
      if (c) {
        renderRbcomp(c, 'chart-preview', function () {
          render_preview_chart = this
        })
      } else {
        render_preview_error($L('不支持的图表类型'))
      }
    },
    400,
    'chart-preview'
  )
}
function render_preview_error(err) {
  $('#chart-preview').html(`<h4 class="chart-undata must-center has-error">${err}</h4>`)
}

// 构造配置
function build_config() {
  const cfg = { entity: wpc.sourceEntity, title: $val('#chart-title') || $L('未命名图表') }
  cfg.type = $('.chart-type>a.select').data('type')
  if (!cfg.type) return

  const dims = []
  const nums = []
  $('.J_axis-dim>span').each((idx, item) => dims.push(_buildAxisItem(item, false)))
  $('.J_axis-num>span').each((idx, item) => nums.push(_buildAxisItem(item, true)))
  if (dims.length === 0 && nums.length === 0) return
  cfg.axis = { dimension: dims, numerical: nums }

  const option = {}
  $('.chart-option input, .chart-option select').each(function () {
    if ($(this).parents('div').hasClass('hide')) return
    const name = $(this).data('name')
    if (name) option[name] = $val(this)
  })

  let color = $('.rbcolors >a>i')
  if (color[0]) color = color.parent().data('color') || ''
  else color = $('.rbcolors >input').val() || ''
  option.useColor = color === '#000000' ? null : color
  cfg.option = option

  // 排他
  $('input[data-name="showMutliYAxis"]').attr('disabled', option.showHorizontal === true)

  if (dataFilter) cfg.filter = dataFilter
  if (rb.env === 'dev') console.log(cfg)
  return cfg
}
function _buildAxisItem(item, isNum) {
  item = $(item)
  const x = {
    field: item.data('field'),
    sort: item.attr('data-sort') || '',
    label: item.attr('data-label') || '',
    fkey: item.attr('data-fkey'),
  }
  if (isNum) {
    x.calc = item.attr('data-calc')
    x.scale = item.attr('data-scale')
    x.unit = item.attr('data-unit')
    x.filter = _axisAdvFilters__data[x.fkey] || null
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
      <RbModal title={$L('显示样式')} ref={(c) => (this._dlg = c)} className="sm-height">
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('别名')}</label>
            <div className="col-sm-7">
              <input className="form-control form-control-sm" placeholder={$L('默认')} data-id="label" value={this.state.label || ''} onChange={this.handleChange} />
            </div>
          </div>
          {this.state.isNumAxis && (
            <RF>
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
              <div className="form-group row">
                <label className="col-sm-3 col-form-label text-sm-right">{$L('数字单位')}</label>
                <div className="col-sm-7">
                  <select className="form-control form-control-sm" value={this.state.unit || 0} data-id="unit" onChange={this.handleChange}>
                    <option value="0">{$L('默认')}</option>
                    <option value="1000">{$L('千')}</option>
                    <option value="10000">{$L('万')}</option>
                    <option value="100000">{$L('十万')}</option>
                    <option value="1000000">{$L('百万')}</option>
                    <option value="10000000">{$L('千万')}</option>
                    <option value="100000000">{$L('亿')}</option>
                  </select>
                </div>
              </div>
            </RF>
          )}
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3">
              <button className="btn btn-primary" type="button" onClick={() => this.saveProps()}>
                {$L('确定')}
              </button>
              <button type="button" className="btn btn-link" onClick={() => this.hide()}>
                {$L('取消')}
              </button>
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
