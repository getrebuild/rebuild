/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// in `chart-design`
const __PREVIEW = !!(window.__PageConfig || {}).chartConfig

// 图表基类
class BaseChart extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    let showSource = !this.props.builtin
    let showFilter = !this.props.builtin
    if (this.props.id === '017-9000000000000007' || this.props.id === '017-9000000000000002') showSource = true
    if (this.props.type === 'HeadingText' || this.props.type === 'EmbedFrame') showFilter = false

    const opActions = (
      <div className="chart-oper">
        {showFilter && (
          <a title={$L('过滤条件')} className="J_chart-filter" onClick={() => this.showChartFilter()}>
            <i className="mdi mdi-filter" />
          </a>
        )}
        {showSource && (
          <a title={$L('查看来源数据')} href={`${rb.baseUrl}/dashboard/view-chart-source?id=${this.props.id}`} target="_blank" className="J_source">
            <i className="zmdi zmdi-rss" />
          </a>
        )}
        <a title={$L('刷新')} onClick={() => this.loadChartData()}>
          <i className="zmdi zmdi-refresh" />
        </a>
        <a className="d-none d-md-inline-block J_fullscreen" title={$L('全屏')} onClick={() => this.toggleFullscreen()}>
          <i className={`zmdi zmdi-${this.state.fullscreen ? 'fullscreen-exit' : 'fullscreen'}`} />
        </a>
        <a className="d-none d-md-inline-block J_mores" data-toggle="dropdown">
          <i className="icon zmdi zmdi-more-vert" style={{ width: 16 }} />
        </a>
        <div className="dropdown-menu dropdown-menu-right dropdown-menu-sm">
          {this.props.isManageable && !this.props.builtin && (
            <a className="dropdown-item J_chart-edit" href={`${rb.baseUrl}/dashboard/chart-design?id=${this.props.id}`}>
              {$L('编辑')}
            </a>
          )}
          {this.props.editable && (
            <a className="dropdown-item" onClick={() => this.remove()}>
              {$L('移除')}
            </a>
          )}
          <a className="dropdown-item J_export" onClick={() => this.export()}>
            {$L('导出')}
          </a>
        </div>
      </div>
    )

    return (
      <div
        className={`chart-box ${this.props.type} ${this.state.useBgcolor && `gradient-bg gradient-bg-${this.state.useBgcolor}`} ${this.props.type === 'DATALIST2' && 'DataList'}`}
        ref={(c) => (this._$box = c)}>
        <div className="chart-head">
          <div className="chart-title text-truncate">{this.state.title}</div>
          {opActions}
        </div>
        <div ref={(c) => (this._$body = c)} className={`chart-body rb-loading ${!this.state.chartdata && 'rb-loading-active'}`}>
          {this.state.chartdata || <RbSpinner />}
        </div>
      </div>
    )
  }

  componentDidMount() {
    this.loadChartData()
  }

  componentWillUnmount() {
    if (this._echarts) this._echarts.dispose()
  }

  showChartFilter() {
    if (this._ChartFilter) {
      this._ChartFilter.show()
    } else {
      const that = this
      const config = this.props.config || {}
      renderRbcomp(
        <AdvFilter
          title={$L('图表过滤条件')}
          entity={config.entity}
          filter={config.filter}
          onConfirm={(s) => {
            that._ChartFilter__data = s
            that.loadChartData()
          }}
          inModal
          canNoFilters
        />,
        function () {
          that._ChartFilter = this
        }
      )
    }
  }

  loadChartData(notClear) {
    if (notClear !== true) this.setState({ chartdata: null })

    const conf = this.state.config || {}
    conf.extconfig = {
      ...conf.extconfig,
      dash_filter_user: window.dash_filter_user || null,
      dash_filter_date: window.dash_filter_date || null,
      dash_filter_custom: window.dash_filter_custom || null,
    }
    // 优先级高
    if (this._ChartFilter__data) conf.extconfig.chart_filter = this._ChartFilter__data

    $.post(this.buildDataUrl(), JSON.stringify(conf), (res) => {
      if (this._echarts) this._echarts.dispose()

      if (res.error_code === 0) {
        this.renderChart(res.data)
        // v4.2
        res.data._renderOption && res.data._renderOption.useBgcolor && this.setState({ useBgcolor: res.data._renderOption.useBgcolor })
      } else {
        this.renderError(res.error_msg)
      }
    })
  }

  buildDataUrl() {
    return `${this.state.id ? '/dashboard/chart-data' : '/dashboard/chart-preview'}?id=${this.state.id || ''}`
  }

  resize() {
    if (this._echarts) {
      $setTimeout(() => this._echarts.resize(), 400, `resize-chart-${this.state.id}`)
    }
  }

  toggleFullscreen(forceFullscreen) {
    const is = forceFullscreen === true ? true : !this.state.fullscreen
    this.setState({ fullscreen: is }, () => {
      // in Dashboard
      const $stack = $('.chart-grid>.grid-stack')
      if (!$stack[0]) {
        // in DataList
        const $wrap = $(this._$box).parent()
        $wrap.toggleClass('fullscreen')
        this.resize()
        return
      }

      const $boxParent = $(this._$box).parents('.grid-stack-item')
      if (this.state.fullscreen) {
        BaseChart.currentFullscreen = this
        if (!this.__chartStackHeight) this.__chartStackHeight = $stack.height()

        $boxParent.addClass('fullscreen')
        let height = $(window).height() - ($(document.body).hasClass('fullscreen') ? 75 : 135)
        if (rb.shareKey) height += 95 // v4.2:in share
        height -= $('.announcement-wrapper').height() || 0
        $stack.css({ height: Math.max(height, 300), overflow: 'hidden' })
      } else {
        $boxParent.removeClass('fullscreen')
        $stack.css({ height: this.__chartStackHeight, overflow: 'unset' })

        BaseChart.currentFullscreen = null
        this.__chartStackHeight = 1
      }
      this.resize()
    })
  }

  remove() {
    const that = this
    RbAlert.create($L('确认移除此图表？'), {
      confirm: function () {
        if (window.gridstack) window.gridstack.removeWidget($(that._$box).parent().parent())
        else if (window.chart_remove) window.chart_remove($(that._$box))
        this.hide()
      },
    })
  }

  export() {
    if (this._echarts) {
      const base64 = this._echarts.getDataURL({
        type: 'png',
        pixelRatio: 2,
        backgroundColor: '#fff',
      })

      const $a = document.createElement('a')
      $a.href = base64
      $a.download = `${this.state.title}.png`
      $a.click()
    } else {
      const table = $(this._$body).find('table.table')[0]
      if (table) {
        this._exportTable(table)
      } else {
        RbHighbar.createl('该图表暂不支持导出')
      }
    }
  }

  _exportTable(table) {
    const _rmLinks = function (table, a, b) {
      $(table)
        .find('a')
        .each(function () {
          $(this)
            .attr(a, `${$(this).attr(b)}`)
            .removeAttr(b)
        })
    }

    const name = `${this.state.title}.xls`
    const _export = function () {
      // remove
      _rmLinks(table, '__href', 'href')
      // export
      // https://docs.sheetjs.com/docs/api/utilities/html#html-table-input
      // https://docs.sheetjs.com/docs/api/write-options
      const wb = window.XLSX.utils.table_to_book(table, { raw: true, wrapText: true })
      window.XLSX.writeFile(wb, name)
      // restore
      setTimeout(() => _rmLinks(table, 'href', '__href'), 499)
    }

    if (window.XLSX && window.XLSX.utils) _export()
    else {
      $getScript('/assets/lib/charts/xlsx.full.min.js', () => {
        setTimeout(_export, 1000)
      })
    }
  }

  renderError(msg, cb) {
    this.setState({ chartdata: <div className="chart-undata must-center">{msg || $L('加载失败')}</div> }, cb)
  }

  renderChart(data) {
    this.setState({ chartdata: <div>{JSON.stringify(data)}</div> })
  }

  // 当前全屏 CHART
  static currentFullscreen = null
}

// 指标卡
class ChartIndex extends BaseChart {
  constructor(props) {
    super(props)
    this.label = this.state.title
    this.state.title = null
  }

  renderChart(data) {
    const showGrowthRate = data._renderOption && data._renderOption.showGrowthRate
    const color = __PREVIEW ? this.props.config.option.useColor : this.props.config.color
    const style2 = { color: color || null }
    const _index = data.index

    let clazz2, rate2
    if (_index.label2) {
      const N1 = parseFloat(_index.data)
      const N2 = parseFloat(_index.data2)
      clazz2 = N1 >= N2 ? 'ge' : 'le'
      // eslint-disable-next-line eqeqeq
      if (N2 == 0) {
        rate2 = '100.00%'
      } else {
        rate2 = (((N1 - N2) * 1.0) / N2) * 100
        rate2 = `${Math.abs(rate2).toFixed(2)}%`
      }
    }

    const chartdata = (
      <div className="chart index" ref={(c) => (this._$chart = c)}>
        <div className="data-item must-center text-truncate w-auto">
          <p style={style2}>{_index.label || this.label}</p>
          <strong style={style2}>
            <a
              title={$L('查看来源数据')}
              href={window.render_preview_chart ? null : `${rb.baseUrl}/dashboard/view-chart-source?id=${this.props.id}&axis=N1`}
              target={window.render_preview_chart ? null : '_blank'}>
              {formatThousands(_index.data, _index.dataFlag)}
            </a>
            {showGrowthRate && clazz2 && <span className={clazz2}>{rate2}</span>}
          </strong>
          {_index.label2 && (
            <div className="with">
              <p>{_index.label2}</p>
              <strong>
                <a
                  title={$L('查看来源数据')}
                  href={window.render_preview_chart ? null : `${rb.baseUrl}/dashboard/view-chart-source?id=${this.props.id}&axis=N2`}
                  target={window.render_preview_chart ? null : '_blank'}>
                  {formatThousands(_index.data2, _index.dataFlag2)}
                </a>
              </strong>
            </div>
          )}
        </div>
      </div>
    )
    this.setState({ chartdata: chartdata }, () => this.resize(1))
  }

  resize(delay) {
    $setTimeout(
      () => {
        const ch = $(this._$chart).height()
        const zoom = ch > 100 ? (ch > 330 ? 2 : 1.3) : 1
        $(this._$chart).find('strong').css('zoom', zoom)
      },
      delay || 200,
      `resize-chart-${this.props.id}`
    )
  }
}

// 统计表
class ChartTable extends BaseChart {
  renderChart(data) {
    if (!data.html) {
      this.renderError($L('暂无数据'))
      return
    }

    const chartdata = (
      <div className="chart ctable">
        <div dangerouslySetInnerHTML={{ __html: data.html }} />
      </div>
    )

    this.setState({ chartdata: chartdata }, () => {
      const $tb = $(this._$body)
      $tb
        .find('.ctable')
        .css('height', $tb.height() - 20)
        .perfectScrollbar()

      try {
        $tb.find('table').tableCellsSelection()
      } catch (ignored) {
        // 未引入
      }

      // a _blank
      $tb.find('tbody td>a').each(function () {
        const $a = $(this)
        $a.attr({ href: `${rb.baseUrl}${$a.attr('href')}`, target: '_blank' })
      })

      this._$tb = $tb
    })
  }

  resize() {
    $setTimeout(
      () => {
        if (this._$tb) this._$tb.find('.ctable').css('height', this._$tb.height() - 20)
      },
      400,
      `resize-chart-${this.state.id}`
    )
  }
}

// for ECharts
const COLOR_AXIS = '#ddd'
const COLOR_LABEL = '#555'
// 可用调色板
const COLOR_PALETTES = {
  shine: ['#c12e34', '#e6b600', '#0098d9', '#2b821d', '#005eaa', '#339ca8', '#cda819', '#32a487'],
  techblue: ['#3a5897', '#007bb6', '#7094db', '#0080ff', '#b3b3ff', '#00bdec', '#33ccff', '#ccddff', '#eeeeee'],
  mint: ['#8aedd5', '#93bc9e', '#cef1db', '#7fe579', '#a6d7c2', '#bef0bb', '#99e2vb', '#94f8a8', '#7de5b8', '#4dfb70'],
  fruit: ['#ffcb6a', '#ffa850', '#ffe2c4', '#e5834e', '#ffb081', '#f7826e', '#faac9e', '#fcd5cf'],
  sakura: ['#e52c3c', '#f7b1ab', '#fa506c', '#f59288', '#f8c4d8', '#e54f5c', '#f06d5c', '#e54f80', '#f29c9f', '#eeb5b7'],
  jazz: ['#e9e0d1', '#91a398', '#33605a', '#070001', '#68462b', '#58a79c', '#abd3ce', '#eef6f5'],
  wonderland: ['#4ea397', '#22c3aa', '#7bd9a5', '#d0648a', '#f58db2', '#f2b3c9'],
  westeros: ['#516b91', '#59c4e6', '#edafda', '#93b7e3', '#a5e7f0', '#cbb0e3'],
  infographic: ['#c1232b', '#27727b', '#fcce10', '#e87c25', '#b5c334', '#fe8463', '#9bca63', '#fad860', '#f3a43b', '#60c0dd', '#d7504b', '#c6e579', '#f4e001', '#f0805a', '#26c0c0'],
  macarons: ['#2ec7c9', '#b6a2de', '#5ab1ef', '#ffb980', '#d87a80', '#8d98b3', '#e5cf0d', '#97b552', '#95706d', '#dc69aa', '#07a2a4', '#9a7fd1', '#588dd5', '#f5994e', '#c05050', '#59678c', '#c9ab00'],
}

const ECHART_BASE = {
  grid: { left: 60, right: 30, top: 30, bottom: 30 },
  animation: window.__LAB_CHARTANIMATION || false,
  tooltip: {
    trigger: 'item',
    textStyle: {
      fontSize: 12,
      lineHeight: 1.2,
      color: '#333',
    },
    axisPointer: {
      type: 'line', // line, cross, shadow
      lineStyle: { color: COLOR_AXIS },
      crossStyle: { color: COLOR_AXIS },
      label: {
        color: '#222',
        backgroundColor: COLOR_AXIS,
        padding: [7, 7, 5, 7],
      },
    },
    backgroundColor: '#fff',
    extraCssText: 'border-radius:0;box-shadow:0 0 6px 0 rgba(0, 0, 0, .1), 0 8px 10px 0 rgba(170, 182, 206, .2);',
    confine: true,
    position: 'top',
    borderWidth: 0,
    padding: [5, 10],
  },
  toolbox: {
    show: false,
  },
  textStyle: {
    fontFamily: '"Hiragina Sans GB", San Francisco, "Helvetica Neue", Helvetica, Arial, PingFangSC-Light, "WenQuanYi Micro Hei", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif',
  },
  color: RBCOLORS,
}

const ECHART_AXIS_LABEL = {
  textStyle: {
    color: COLOR_LABEL,
    fontSize: 12,
    fontWeight: '400',
  },
}

const ECHART_MARK_LINE2 = function (showLabel = false, unitFlag) {
  return {
    data: [{ type: 'average', name: $L('均线') }],
    symbol: 'none',
    silent: true,
    emphasis: { disabled: true },
    label: {
      show: showLabel,
      formatter: function (o) {
        return formatThousands(o.value, unitFlag)
      },
    },
  }
}

const ECHART_VALUE_LABEL2 = function (dataFlags = []) {
  return {
    show: true,
    formatter: function (a) {
      return formatThousands(a.data, dataFlags[a.seriesIndex])
    },
  }
}

const ECHART_TOOLTIP_FORMATTER = function (i, dataFlags = []) {
  if (!Array.isArray(i)) i = [i] // Object > Array
  const tooltip = [`<b>${i[0].name}</b>`]
  i.forEach((a) => {
    tooltip.push(`${a.marker} ${a.seriesName} : ${formatThousands(a.value, dataFlags[a.seriesIndex])}`)
  })
  return tooltip.join('<br>')
}

// 横排
const ECHART_LEGEND_HOPT = {
  type: 'plain',
  orient: 'horizontal',
  top: 10,
  right: 0,
  padding: 0,
  textStyle: { fontSize: 12 },
}
// 竖排
const ECHART_LEGEND_VOPT = {
  type: 'scroll',
  orient: 'vertical',
  top: 10,
  right: 0,
  padding: 0,
  textStyle: { fontSize: 12 },
}

// K=千 M=百万 B=亿
const shortNumber = function (num) {
  if (rb.locale === 'zh_CN') {
    if (num > 100000000 || num < -100000000) return (num / 100000000).toFixed(1) + '亿'
    else if (num > 1000000 || num < -1000000) return (num / 1000000).toFixed(1) + '百万'
    else if (num > 10000 || num < -10000) return (num / 10000).toFixed(1) + '万'
    return num
  }
  if (num > 100000000 || num < -100000000) return (num / 100000000).toFixed(1) + 'B'
  else if (num > 1000000 || num < -1000000) return (num / 1000000).toFixed(1) + 'M'
  else if (num > 10000 || num < -10000) return (num / 1000).toFixed(1) + 'K'
  return num
}

// 千分位
const formatThousands = function (num, flag) {
  if (flag === '0:0') flag = null // Unset
  num += '' // string
  let n = num
  // v3.9 unit
  let flagUnit = ''
  if (flag) {
    const flags = flag.split(':')
    flag = flags[0] === '0' ? null : flags[0]

    let unit = ~~flags[1]
    if (unit && unit > 0) {
      let scale = 0
      if (num && num.includes('.')) scale = num.split('.')[1].length
      n = (parseFloat(n) / unit).toFixed(scale)
      flagUnit = _FLAG_UNITS()[unit + ''] || ''
    }
  }

  if (Math.abs(~~n) > 1000) {
    const nd = (n + '').split('.')
    nd[0] = nd[0].replace(/\d{1,3}(?=(\d{3})+$)/g, '$&,')
    n = nd.join('.')
  }

  // v3.2.1
  if (flag === '%') n += '%' + flagUnit
  else if (flag && flag.includes('%s')) n = flag.replace('%s', n) + flagUnit
  else if (flag) n = `${flag} ${n}` + flagUnit
  else if (flagUnit) n += flagUnit
  return n
}
let _FLAG_UNITS_c
const _FLAG_UNITS = () => {
  if (_FLAG_UNITS_c) return _FLAG_UNITS_c
  let c = {
    '1000': $L('千'),
    '10000': $L('万'),
    '100000': $L('十万'),
    '1000000': $L('百万'),
    '10000000': $L('千万'),
    '100000000': $L('亿'),
  }
  _FLAG_UNITS_c = c
  return c
}

// 多轴显示
const reOptionMutliYAxis = function (option) {
  const yAxisBase = option.yAxis
  const yAxisMutli = []
  for (let i = 0; i < option.series.length; i++) {
    let c = $clone(yAxisBase)
    if (i > 0) {
      c.position = 'right'
      c.offset = i * 45 - 45
    }
    c.axisLabel.textStyle.color = option.color[i] || COLOR_AXIS
    // c.axisLine = { show: true, lineStyle: { color: option.color[i] || COLOR_AXIS } }
    option.series[i].yAxisIndex = i
    yAxisMutli.push(c)
  }
  option.yAxis = yAxisMutli
  option.grid.right = 60 + (option.series.length - 2) * 45
}

const renderEChart = function (option, $target) {
  $target = document.getElementById($target)
  const c = echarts.init($target, 'light', {
    renderer: navigator.userAgent.match(/(iPhone|iPod|Android|ios|SymbianOS)/i) ? 'svg' : 'canvas',
  })
  // v4.2 禁用右键
  $target.addEventListener('contextmenu', function (e) {
    e.preventDefault()
  })
  if (rb.env === 'dev') console.log(option)
  c.setOption(option)
  return c
}

// 折线图
class ChartLine extends BaseChart {
  renderChart(data) {
    if (data.xAxis.length === 0) {
      this.renderError($L('暂无数据'))
      return
    }

    const elid = `echarts-line-${this.state.id || 'id'}`
    this.setState({ chartdata: <div className="chart line" id={elid} /> }, () => {
      const showGrid = data._renderOption && data._renderOption.showGrid
      const showNumerical = data._renderOption && data._renderOption.showNumerical
      const showLegend = data._renderOption && data._renderOption.showLegend
      const showMutliYAxis = data._renderOption && data._renderOption.showMutliYAxis
      const showAreaColor = data._renderOption && data._renderOption.showAreaColor
      const dataFlags = data._renderOption.dataFlags || []
      const themeStyle = data._renderOption ? data._renderOption.themeStyle : null
      const showMarkLine = data._renderOption ? data._renderOption.showMarkLine : null

      for (let i = 0; i < data.yyyAxis.length; i++) {
        const yAxis = data.yyyAxis[i]
        yAxis.type = 'line'
        yAxis.smooth = true
        yAxis.lineStyle = { width: 3 }
        yAxis.itemStyle = {
          normal: { borderWidth: 2 },
          emphasis: { borderWidth: 6 },
        }
        if (showAreaColor) yAxis.areaStyle = { opacity: 0.2 }
        if (showNumerical) yAxis.label = ECHART_VALUE_LABEL2(dataFlags)
        yAxis.cursor = 'default'
        if (showMarkLine) yAxis.markLine = ECHART_MARK_LINE2(showNumerical, dataFlags[i])
        data.yyyAxis[i] = yAxis
      }

      const option = {
        ...$clone(ECHART_BASE),
        xAxis: {
          type: 'category',
          data: data.xAxis,
          axisLabel: ECHART_AXIS_LABEL,
          axisLine: {
            lineStyle: { color: COLOR_AXIS },
          },
        },
        yAxis: {
          type: 'value',
          splitLine: { show: showGrid, lineStyle: { color: COLOR_AXIS } },
          axisLabel: {
            ...ECHART_AXIS_LABEL,
            formatter: shortNumber,
          },
          axisLine: {
            lineStyle: { color: COLOR_AXIS, width: showGrid ? 1 : 0 },
          },
        },
        series: data.yyyAxis,
      }
      option.tooltip.trigger = 'axis'
      option.tooltip.formatter = (a) => ECHART_TOOLTIP_FORMATTER(a, dataFlags)
      if (showLegend) {
        option.legend = ECHART_LEGEND_HOPT
        option.grid.top = 50
      }
      if (showMarkLine) option.grid.right = 60
      if (themeStyle && COLOR_PALETTES[themeStyle]) option.color = COLOR_PALETTES[themeStyle]
      if (showMutliYAxis && option.series.length > 1) reOptionMutliYAxis(option)

      this._echarts = renderEChart(option, elid)
    })
  }
}

// 柱状图
class ChartBar extends BaseChart {
  renderChart(data) {
    if (data.xAxis.length === 0) {
      this.renderError($L('暂无数据'))
      return
    }

    const elid = `echarts-bar-${this.state.id || 'id'}`
    this.setState({ chartdata: <div className="chart bar" id={elid} /> }, () => {
      const showGrid = data._renderOption && data._renderOption.showGrid
      const showNumerical = data._renderOption && data._renderOption.showNumerical
      const showLegend = data._renderOption && data._renderOption.showLegend
      const showHorizontal = data._renderOption && data._renderOption.showHorizontal // v3.7
      const showMutliYAxis = data._renderOption && data._renderOption.showMutliYAxis // v3.7
      const dataFlags = data._renderOption.dataFlags || [] // 小数符号
      const themeStyle = data._renderOption ? data._renderOption.themeStyle : null
      const showMarkLine = data._renderOption ? data._renderOption.showMarkLine : null

      for (let i = 0; i < data.yyyAxis.length; i++) {
        const yAxis = data.yyyAxis[i]
        yAxis.type = 'bar'
        if (showNumerical) yAxis.label = ECHART_VALUE_LABEL2(dataFlags)
        yAxis.cursor = 'default'
        // v3.7
        if (this._stack) {
          yAxis.stack = 'a'
        } else if (this._overLine && i > 0) {
          yAxis.type = 'line'
          yAxis.smooth = true
          yAxis.lineStyle = { width: 3 }
        }
        if (showMarkLine) yAxis.markLine = ECHART_MARK_LINE2(showNumerical, dataFlags[i])
        data.yyyAxis[i] = yAxis
      }

      const option = {
        ...$clone(ECHART_BASE),
        xAxis: {
          type: showHorizontal ? 'value' : 'category',
          data: showHorizontal ? null : data.xAxis,
          axisLabel: ECHART_AXIS_LABEL,
          axisLine: {
            lineStyle: { color: COLOR_AXIS },
          },
        },
        yAxis: {
          type: showHorizontal ? 'category' : 'value',
          data: showHorizontal ? data.xAxis : null,
          splitLine: { show: showGrid, lineStyle: { color: COLOR_AXIS } },
          axisLabel: {
            ...ECHART_AXIS_LABEL,
            formatter: shortNumber,
          },
          axisLine: {
            lineStyle: { color: COLOR_AXIS, width: showGrid ? 1 : 0 },
          },
        },
        series: data.yyyAxis,
      }
      option.tooltip.trigger = 'axis'
      option.tooltip.formatter = (a) => {
        if (this._stack) {
          let clone = [...a]
          clone.reverse()
          a = clone
        }
        return ECHART_TOOLTIP_FORMATTER(a, dataFlags)
      }
      if (showLegend) {
        option.legend = ECHART_LEGEND_HOPT
        option.grid.top = 50
      }
      if (showMarkLine) option.grid.right = 60
      if (themeStyle && COLOR_PALETTES[themeStyle]) option.color = COLOR_PALETTES[themeStyle]
      // 加大左侧距离
      if (showHorizontal) option.grid.left = 100
      // 排他
      else if (showMutliYAxis && option.series.length > 1 && !this._stack) reOptionMutliYAxis(option)

      this._echarts = renderEChart(this.renderChartBefore(option, data), elid)
    })
  }

  renderChartBefore(option) {
    return option
  }
}

// 堆叠柱状图
class ChartBar2 extends ChartBar {
  constructor(props) {
    super(props)
    this._stack = true
  }
}

// 折线柱状图
class ChartBar3 extends ChartBar {
  constructor(props) {
    super(props)
    this._overLine = true
  }
}

// 柏拉图/帕累托图
class ChartPareto extends ChartBar3 {
  constructor(props) {
    super(props)
  }

  renderChartBefore(option, data) {
    const dataFlags = data._renderOption.dataFlags || []
    let sums = 0
    data.yyyAxis[0].data.forEach((v) => {
      sums += parseFloat(v)
    })

    option.tooltip.formatter = (a) => {
      let aa = [a[0], { ...a[0], seriesName: '本项占比', seriesIndex: 1, value: ((a[0].value * 100) / sums).toFixed(2) }, { ...a[1], seriesIndex: 2 }]
      let dataFlags2 = [dataFlags[0], dataFlags[1], dataFlags[1]]
      return ECHART_TOOLTIP_FORMATTER(aa, dataFlags2)
    }
    return option
  }
}

// 饼图
class ChartPie extends BaseChart {
  renderChart(data) {
    if (data.data.length === 0) {
      this.renderError($L('暂无数据'))
      return
    }

    const elid = `echarts-pie-${this.state.id || 'id'}`
    this.setState({ chartdata: <div className="chart pie" id={elid} /> }, () => {
      const showNumerical = data._renderOption && data._renderOption.showNumerical
      const showLegend = data._renderOption && data._renderOption.showLegend
      const dataFlags = data._renderOption.dataFlags || []
      const themeStyle = data._renderOption ? data._renderOption.themeStyle : null

      data = { ...data, type: 'pie', radius: '71%', cursor: 'default' }
      if (showNumerical) {
        data.label = {
          formatter: function (a) {
            return `${a.data.name} (${formatThousands(a.data.value, dataFlags[0])})`
          },
        }
      }
      const option = {
        ...$clone(ECHART_BASE),
        series: [data],
      }
      option.tooltip.trigger = 'item'
      option.tooltip.formatter = function (a) {
        return `<b>${a.data.name}</b> <br/> ${a.marker} ${a.seriesName} : ${formatThousands(a.data.value, dataFlags[0])} (${a.percent}%)`
      }
      if (showLegend) option.legend = ECHART_LEGEND_VOPT
      if (themeStyle && COLOR_PALETTES[themeStyle]) option.color = COLOR_PALETTES[themeStyle]

      this._echarts = renderEChart(option, elid)
    })
  }
}

// 漏斗图
class ChartFunnel extends BaseChart {
  renderChart(data) {
    if (data.data.length === 0) {
      this.renderError($L('暂无数据'))
      return
    }

    const elid = `echarts-funnel-${this.state.id || 'id'}`
    this.setState({ chartdata: <div className="chart funnel" id={elid} /> }, () => {
      const showNumerical = data._renderOption && data._renderOption.showNumerical
      const showLegend = data._renderOption && data._renderOption.showLegend
      const dataFlags = data._renderOption.dataFlags || []
      const themeStyle = data._renderOption ? data._renderOption.themeStyle : null

      const option = {
        ...$clone(ECHART_BASE),
        series: [
          {
            type: 'funnel',
            sort: 'none',
            gap: 1,
            top: 30,
            bottom: 20,
            data: data.data,
            cursor: 'default',
            label: {
              show: true,
              position: 'inside',
              formatter: function (a) {
                let text = a.data.name
                if (showNumerical) text += ` (${formatThousands(a.data.value, dataFlags[a.dataIndex])})`
                if (a.data.cvr) text += `\n${$L('转化率')} ${a.data.cvr}%`
                return text
              },
              lineHeight: 16,
            },
          },
        ],
      }
      // option.grid.right = 60
      option.tooltip.trigger = 'item'
      option.tooltip.formatter = function (a) {
        if (data.xLabel) return `<b>${a.name}</b> <br/> ${a.marker} ${data.xLabel} : ${formatThousands(a.value, dataFlags[a.dataIndex])}`
        else return `<b>${a.name}</b> <br/> ${a.marker} ${formatThousands(a.value, dataFlags[a.dataIndex])}`
      }
      if (showLegend) option.legend = ECHART_LEGEND_VOPT
      if (themeStyle && COLOR_PALETTES[themeStyle]) option.color = COLOR_PALETTES[themeStyle]

      this._echarts = renderEChart(option, elid)
    })
  }
}

// 树图
const LEVELS_SPLIT = '--------'
class ChartTreemap extends BaseChart {
  renderChart(data) {
    if (data.data.length === 0) {
      this.renderError($L('暂无数据'))
      return
    }

    const elid = `echarts-treemap-${this.state.id || 'id'}`
    this.setState({ chartdata: <div className="chart treemap" id={elid} /> }, () => {
      const showNumerical = data._renderOption && data._renderOption.showNumerical
      const dataFlags = data._renderOption.dataFlags || []
      const themeStyle = data._renderOption ? data._renderOption.themeStyle : null

      const option = {
        ...$clone(ECHART_BASE),
        series: [
          {
            data: data.data,
            type: 'treemap', // sunburst
            width: '100%',
            height: '100%',
            top: window.render_preview_chart ? 0 : 15, // In preview
            breadcrumb: { show: false },
            roam: false, // Disabled drag and mouse wheel
            levels: [
              {
                itemStyle: {
                  gapWidth: 1,
                },
              },
              {
                itemStyle: {
                  gapWidth: 0,
                },
              },
              {
                itemStyle: {
                  gapWidth: 0,
                },
              },
            ],
          },
        ],
      }
      option.tooltip.trigger = 'item'
      option.tooltip.formatter = function (a) {
        const p = a.value > 0 ? ((a.value * 100) / data.xAmount).toFixed(2) : 0
        return `<b>${a.name.split(LEVELS_SPLIT).join('<br/>')}</b> <br/> ${a.marker} ${data.xLabel} : ${formatThousands(a.value, dataFlags[0])} (${p}%)`
      }
      option.label = {
        formatter: function (a) {
          const ns = a.name.split(LEVELS_SPLIT)
          return ns[ns.length - 1] + (showNumerical ? ` (${formatThousands(a.value, dataFlags[0])})` : '')
        },
      }
      if (themeStyle && COLOR_PALETTES[themeStyle]) option.color = COLOR_PALETTES[themeStyle]

      this._echarts = renderEChart(option, elid)
    })
  }
}

// ~ 审批列表
const APPROVAL_STATES = {
  1: ['warning', $L('待审批')],
  10: ['success', $L('通过')],
  11: ['danger', $L('驳回')],
}
class ApprovalList extends BaseChart {
  constructor(props) {
    super(props)
    this.__approvalForms = {}
  }

  renderChart(data, viewState) {
    viewState = viewState || 1

    let stats = []
    let statsTotal = 0
    Object.keys(APPROVAL_STATES).forEach((state) => {
      let len = (data['state' + state] || []).length
      stats.push([~~state, len])
      statsTotal += len
    })

    if (statsTotal === 0) {
      this.renderError($L('暂无数据'))
      return
    }

    const statsComp = (
      <div className="progress-wrap sticky">
        <div className="progress">
          {stats.map((item) => {
            const s = APPROVAL_STATES[item[0]]
            if (!s || s[1] <= 0) return null

            const p = ((item[1] * 100) / statsTotal).toFixed(1) + '%'
            return (
              <div
                key={s[0]}
                className={`progress-bar bg-${s[0]} ${viewState === item[0] && 'active'}`}
                style={{ width: p }}
                onClick={() => {
                  this.renderChart(data, item[0])
                }}>
                {s[1]} ({item[1]})
              </div>
            )
          })}
        </div>
      </div>
    )

    const viewData = data['state' + viewState] || []
    const tableComp =
      viewData.length === 0 ? (
        <div className="chart-undata must-center">
          <i className="zmdi zmdi-check icon text-success" /> {$L('你已完成所有审批')}
        </div>
      ) : (
        <div>
          <table className="table table-striped table-hover">
            <thead>
              <tr>
                <th style={{ minWidth: 140 }}>{$L('提交人')}</th>
                <th style={{ minWidth: 140 }}>{$L('审批记录')}</th>
                <th width="90" />
              </tr>
            </thead>
            <tbody>
              {viewData.map((item, idx) => {
                return (
                  <tr key={`approval-${idx}`}>
                    <td className="user-avatar cell-detail user-info">
                      <img src={`${rb.baseUrl}/account/user-avatar/${item[0]}`} alt="Avatar" />
                      <span>{item[1]}</span>
                      <span className="cell-detail-description">
                        <div className="float-left">
                          <DateShow date={item[2]} />
                        </div>
                        {item[8] > 0 && <span className="float-left text-danger ml-1">({$L('已超时 %s', $sec2Time(item[8]))})</span>}
                        {item[8] < 0 && <span className="float-left text-warning ml-1">({$L('%s 后超时', $sec2Time(Math.abs(item[8])))})</span>}
                      </span>
                    </td>
                    <td className="cell-detail">
                      <a href={`${rb.baseUrl}/app/redirect?id=${item[3]}&type=newtab`} target="_blank" title={$L('查看记录')} onClick={(e) => this._listenerStateChanged42(e)}>
                        {item[4]}
                      </a>
                      <span className="cell-detail-description">{item[6]}</span>
                    </td>
                    <td className="actions text-right text-nowrap">
                      {viewState === 1 && (
                        <button className="btn btn-secondary btn-sm" onClick={() => this.approve(item[3], item[5], item[7])}>
                          {$L('审批')}
                        </button>
                      )}
                      {viewState === 10 && <span className="text-success">{$L('通过')}</span>}
                      {viewState === 11 && <span className="text-danger">{$L('驳回')}</span>}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
          {viewData.length >= 500 && (
            <div className="m-2 text-center text-warning">
              <i className="mdi mdi-information-outline" /> {$L('最多显示最近 500 条')}
            </div>
          )}
        </div>
      )

    const chartdata = (
      <div className="chart ApprovalList">
        {statsComp}
        {tableComp}
      </div>
    )
    this.setState({ chartdata: chartdata }, () => {
      const $tb = $(this._$body)
      $tb &&
        $tb
          .find('.ApprovalList')
          .css('height', $tb.height() - 5)
          .perfectScrollbar()
    })
  }

  resize() {
    $setTimeout(
      () => {
        const $tb = $(this._$body)
        $tb && $tb.find('.ApprovalList').css('height', $tb.height() - 5)
      },
      400,
      `resize-chart-${this.state.id}`
    )
  }

  approve(record, approval, entity) {
    window.event && window.event.preventDefault()
    if (this.__approvalForms[record]) {
      this.__approvalForms[record].show()
    } else {
      const that = this
      renderRbcomp(
        // eslint-disable-next-line react/jsx-no-undef
        <ApprovalApproveForm
          id={record}
          approval={approval}
          entity={entity}
          call={() => {
            if (that.__approvalForms[record]) that.__approvalForms[record].hide(true)
            that.loadChartData()
          }}
        />,
        function () {
          that.__approvalForms[record] = this
        }
      )
    }
  }

  // 打开记录后操作审批，这里自动刷新
  _listenerStateChanged42() {
    $(document)
      .off('visibilitychange._listenerStateChanged42')
      .on('visibilitychange._listenerStateChanged42', () => {
        if (document.visibilityState === 'visible') {
          if ($storage.get('_listenerStateChanged42')) {
            $storage.remove('_listenerStateChanged42')
            $(document).off('visibilitychange._listenerStateChanged42')
            this.loadChartData()
          }
        }
      })
  }
}

// ~ 我的日程
class FeedsSchedule extends BaseChart {
  renderChart(data) {
    const table =
      (data || []).length === 0 ? (
        <div className="chart-undata must-center" style={{ marginTop: -15 }}>
          <i className="zmdi zmdi-check icon text-success" /> {$L('暂无待办日程')}
          <br />
          {$L('过期超过 30 天的日程将不再显示')}
        </div>
      ) : (
        <div>
          <table className="table table-striped table-hover">
            <thead>
              <tr>
                <th style={{ minWidth: 140 }}>{$L('日程内容')}</th>
                <th style={{ minWidth: 140 }}>{$L('日程时间')}</th>
                <th width="90" />
              </tr>
            </thead>
            <tbody>
              {data.map((item, idx) => {
                // 过期
                let scheduleTimeTip
                if ($expired(item.scheduleTime)) {
                  scheduleTimeTip = <span className="badge badge-danger">{$L('已过期')}</span>
                } else if ($expired(item.scheduleTime, -60 * 60 * 24 * 3)) {
                  scheduleTimeTip = <span className="badge badge-warning">{$fromNow(item.scheduleTime)}</span>
                } else {
                  scheduleTimeTip = <span className="badge badge-info">{$fromNow(item.scheduleTime)}</span>
                }

                return (
                  <tr key={`schedule-${idx}`}>
                    <td className="cell-detail">
                      <a href={`${rb.baseUrl}/app/redirect?id=${item.id}`} className="content text-break" dangerouslySetInnerHTML={{ __html: item.content }} />
                      {item.relatedRecord && (
                        <span className="cell-detail-description fs-12">
                          {item.relatedRecord.entityLabel || $L('关联记录')} :&nbsp;
                          <a href={`${rb.baseUrl}/app/redirect?id=${item.relatedRecord.id}&type=newtab`} target="_blank" title={$L('查看记录')}>
                            {item.relatedRecord.text}
                          </a>
                        </span>
                      )}
                    </td>
                    <td className="cell-detail">
                      <div>{item.scheduleTime.substr(0, 16)}</div>
                      <span className="cell-detail-description">{scheduleTimeTip}</span>
                    </td>
                    <td className="actions text-right text-nowrap">
                      <button className="btn btn-secondary btn-sm" onClick={() => this.handleFinish(item.id)}>
                        {$L('完成')}
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )

    const chartdata = <div className="chart FeedsSchedule">{table}</div>
    this.setState({ chartdata: chartdata }, () => {
      const $tb = $(this._$body)
      $tb
        .find('.FeedsSchedule')
        .css('height', $tb.height() - 13)
        .perfectScrollbar()
      $tb.find('.content').each(function () {
        $(this).attr('title', $(this).text())
      })
      this._$tb = $tb
    })
    return table
  }

  resize() {
    $setTimeout(
      () => {
        if (this._$tb) this._$tb.find('.FeedsSchedule').css('height', this._$tb.height() - 13)
      },
      400,
      `resize-chart-${this.state.id}`
    )
  }

  handleFinish(id) {
    const that = this
    RbAlert.create($L('确认完成此日程？'), {
      confirm: function () {
        this.disabled(true)
        $.post(`/feeds/post/finish-schedule?id=${id}`, (res) => {
          if (res.error_code === 0) {
            this.hide()
            RbHighbar.success($L('日程已完成'))
            that.loadChartData()
          } else {
            RbHighbar.error(res.error_msg)
            this.disabled()
          }
        })
      },
    })
  }
}

// 雷达图
class ChartRadar extends BaseChart {
  renderChart(data) {
    if (data.indicator.length === 0) {
      this.renderError($L('暂无数据'))
      return
    }

    const elid = `echarts-radar-${this.state.id || 'id'}`
    this.setState({ chartdata: <div className="chart radar" id={elid} /> }, () => {
      const showNumerical = data._renderOption && data._renderOption.showNumerical
      const showLegend = data._renderOption && data._renderOption.showLegend
      const dataFlags = data._renderOption.dataFlags || []
      const themeStyle = data._renderOption ? data._renderOption.themeStyle : null

      const option = {
        ...$clone(ECHART_BASE),
        radar: {
          indicator: data.indicator,
          name: {
            textStyle: {
              color: COLOR_LABEL,
              fontSize: 12,
            },
          },
          splitNumber: 4,
          splitArea: {
            areaStyle: {
              // color: ['#fff', '#fff', '#fff', '#fff', '#fff'],
            },
          },
          splitLine: {
            lineStyle: {
              color: COLOR_AXIS,
            },
          },
          axisLine: {
            lineStyle: {
              color: COLOR_AXIS,
            },
          },
        },
        series: [
          {
            type: 'radar',
            symbol: 'circle',
            symbolSize: 6,
            label: {
              show: showNumerical,
              formatter: function (a) {
                return formatThousands(a.value, dataFlags[a.dataIndex])
              },
            },
            lineStyle: {
              normal: { width: 2 },
              emphasis: { width: 3 },
              cursor: 'default',
            },
            data: data.series,
          },
        ],
      }
      option.grid.left = 30
      option.tooltip.trigger = 'item'
      option.tooltip.formatter = function (a) {
        const tooltip = [`<b>${a.name}</b>`]
        a.value.forEach((item, idx) => {
          tooltip.push(`${data.indicator[idx].name} : ${formatThousands(item, dataFlags[a.dataIndex])}`)
        })
        return tooltip.join('<br/>')
      }
      if (showLegend) option.legend = ECHART_LEGEND_VOPT
      if (themeStyle && COLOR_PALETTES[themeStyle]) option.color = COLOR_PALETTES[themeStyle]

      this._echarts = renderEChart(option, elid)
    })
  }
}

// 散点图
class ChartScatter extends BaseChart {
  renderChart(data) {
    if (data.series.length === 0) {
      this.renderError($L('暂无数据'))
      return
    }

    const elid = `echarts-scatter-${this.state.id || 'id'}`
    this.setState({ chartdata: <div className="chart scatter" id={elid} /> }, () => {
      const showGrid = data._renderOption && data._renderOption.showGrid
      const showNumerical = data._renderOption && data._renderOption.showNumerical
      const showLegend = data._renderOption && data._renderOption.showLegend
      const dataFlags = data._renderOption.dataFlags || []
      const themeStyle = data._renderOption ? data._renderOption.themeStyle : null

      const axisOption = {
        splitLine: {
          lineStyle: { color: COLOR_AXIS, width: showGrid ? 1 : 0, type: 'solid' },
        },
        axisLabel: {
          ...ECHART_AXIS_LABEL,
          formatter: shortNumber,
        },
        axisLine: {
          lineStyle: { color: COLOR_AXIS },
        },
        scale: false,
      }

      const seriesData = []
      data.series.forEach((item) => {
        seriesData.push({
          ...item,
          type: 'scatter',
          symbolSize: function (data) {
            const s = Math.sqrt(~~data[0])
            return Math.max(Math.min(s, 120), 8)
          },
          cursor: 'default',
          label: {
            show: showNumerical,
            position: 'top',
            formatter: function (a) {
              return a.data.length === 3 ? a.data[2] : `(${a.data[0]}) (${a.data[1]})`
            },
          },
        })
      })

      const option = {
        ...$clone(ECHART_BASE),
        xAxis: { ...axisOption },
        yAxis: { ...axisOption },
        series: seriesData,
      }
      option.tooltip.trigger = 'item'
      option.tooltip.formatter = function (a) {
        const tooltip = []
        if (a.value.length === 3) {
          tooltip.push(`<b>${a.value[2]}</b>`)
        }
        tooltip.push(`${data.dataLabel[0]} : ${formatThousands(a.value[0], dataFlags[0])}`)
        tooltip.push(`${data.dataLabel[1]} : ${formatThousands(a.value[1], dataFlags[1])}`)
        return tooltip.join('<br>')
      }
      if (showLegend) {
        option.legend = ECHART_LEGEND_HOPT
        option.grid.top = 50
      }
      if (themeStyle && COLOR_PALETTES[themeStyle]) option.color = COLOR_PALETTES[themeStyle]

      this._echarts = renderEChart(option, elid)
    })
  }
}

// ~ 我的任务
class ProjectTasks extends BaseChart {
  renderChart(data) {
    const table =
      (data || []).length === 0 ? (
        <div className="chart-undata must-center">
          <i className="zmdi zmdi-check icon text-success" /> {$L('你已完成所有任务')}
        </div>
      ) : (
        <div>
          <table className="table table-striped table-hover">
            <thead>
              <tr>
                <th width="44" />
                <th>{$L('任务')}</th>
                <th style={{ minWidth: 150 }}>{$L('时间')}</th>
              </tr>
            </thead>
            <tbody>
              {data.map((item) => {
                let deadlineClass
                if (item.deadline && !item.endTime) {
                  if ($expired(item.deadline)) {
                    deadlineClass = 'badge-danger'
                  } else if ($expired(item.deadline, -60 * 60 * 24 * 3)) {
                    deadlineClass = 'badge-warning'
                  } else {
                    deadlineClass = 'badge-info'
                  }
                }

                return (
                  <tr key={item.id} className={`status-${item.status} priority-${item.priority}`}>
                    <td className="align-text-top pr-0">
                      <label className="custom-control custom-control-sm custom-checkbox custom-control-inline ptask" title={$L('完成')}>
                        <input className="custom-control-input" type="checkbox" disabled={item.planFlow === 2} onClick={(e) => this._toggleStatus(item, e)} />
                        <span className="custom-control-label" />
                      </label>
                    </td>
                    <td className="cell-detail">
                      <a title={item.taskName} href={`${rb.baseUrl}/app/redirect?id=${item.id}`} className="content">
                        <p className="text-break">
                          [{item.taskNumber}] {item.taskName}
                        </p>
                      </a>
                      <span className="cell-detail-description">{item.projectName}</span>
                    </td>
                    <td className="text-muted align-text-top">
                      <div>
                        <span className="mr-1">{$L('创建时间')}</span>
                        <DateShow date={item.createdOn} />
                      </div>
                      {item.endTime && (
                        <div>
                          <span className="mr-1">{$L('完成时间')}</span>
                          <DateShow date={item.endTime} />
                        </div>
                      )}
                      {deadlineClass && (
                        <span className={`badge ${deadlineClass}`} title={item.deadline}>
                          {$L('到期时间')} {$fromNow(item.deadline)}
                        </span>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )

    const chartdata = <div className="chart ProjectTasks">{table}</div>
    this.setState({ chartdata: chartdata }, () => {
      const $tb = $(this._$body)
      $tb
        .find('.ProjectTasks')
        .css('height', $tb.height() - 13)
        .perfectScrollbar()
      this._$tb = $tb
    })
    return table
  }

  resize() {
    $setTimeout(
      () => {
        if (this._$tb) this._$tb.find('.ProjectTasks').css('height', this._$tb.height() - 13)
      },
      400,
      `resize-chart-${this.state.id}`
    )
  }

  _toggleStatus(item, e) {
    const $target = $(e.currentTarget)
    const data = {
      status: $target.prop('checked') ? 1 : 0,
      metadata: { id: item.id },
    }

    $.post('/app/entity/common-save', JSON.stringify(data), (res) => {
      if (res.error_code > 0) return RbHighbar.error(res.error_msg)
      $target.parents('tr').removeClass('status-0 status-1').addClass(`status-${data.status}`)
    })
  }
}

// ~~ 数据列表
class DataList extends BaseChart {
  componentDidMount() {
    super.componentDidMount()

    if (this.props.type === 'DataList') {
      const $op = $(this._$box).find('.chart-oper')
      $op.find('.J_chart-edit').on('click', (e) => {
        $stopEvent(e, true)
        RbHighbar.create('[DEPRECATED] 该功能将在下一版本禁用')

        // const config2 = this.state.config
        // renderRbcomp(
        //   // eslint-disable-next-line react/jsx-no-undef
        //   <DataListSettings
        //     chart={config2.chart}
        //     {...config2.extconfig}
        //     onConfirm={(s) => {
        //       if (typeof window.save_dashboard === 'function') {
        //         config2.extconfig = s
        //         this.setState({ config: config2 }, () => this.loadChartData())
        //       } else {
        //         console.log('No `save_dashboard` found :', s)
        //       }
        //     }}
        //   />
        // )
      })
    }
  }

  renderChart(data) {
    if (data.error === 'UNSET') {
      super.renderError(
        <RF>
          <span>{$L('当前图表无数据')}</span>
          {this.props.isManageable && rb.currentUser && <div>{WrapHtml($L('请先 [编辑图表](###)'))}</div>}
        </RF>,
        () => {
          $(this._$body)
            .find('a')
            .on('click', (e) => {
              $stopEvent(e, true)
              $(this._$box).find('.chart-oper .J_chart-edit').trigger('click')
            })
        }
      )
      return
    }

    const extconfig2 = this.state.config.extconfig
    const listFields = data.fields
    const listData = data.data
    const lastIndex = listFields.length

    const table =
      listData.length === 0 ? (
        <div className="chart-undata must-center">{$L('暂无数据')}</div>
      ) : (
        <table className="table table-hover table-bordered table-striped table-sortable">
          <thead>
            <tr ref={(c) => (this._$head = c)}>
              {listFields.map((item) => {
                let sortClazz = null
                if (extconfig2 && extconfig2.sort) {
                  const s = extconfig2.sort.split(':')
                  if (s[0] === item.field && s[1] === 'asc') sortClazz = 'sort-asc'
                  if (s[0] === item.field && s[1] === 'desc') sortClazz = 'sort-desc'
                }

                return (
                  <th
                    key={item.field}
                    data-field={item.field}
                    className={sortClazz}
                    onClick={(e) => {
                      if (window.UNSORT_FIELDTYPES.includes(item.type)) return

                      const $th = $(e.currentTarget)
                      const hasAsc = $th.hasClass('sort-asc'),
                        hasDesc = $th.hasClass('sort-desc')

                      $(this._$head).find('th').removeClass('sort-asc sort-desc')
                      if (hasDesc) $th.addClass('sort-asc')
                      else if (hasAsc) $th.addClass('sort-desc')
                      else $th.addClass('sort-asc')

                      // refresh
                      const config2 = this.state.config
                      if (!config2.extconfig) config2.extconfig = {}
                      config2.extconfig.sort = `${item.field}:${$th.hasClass('sort-desc') ? 'desc' : 'asc'}`
                      this.setState({ config: config2 }, () => this.loadChartData(true))
                    }}>
                    <div>{item.label}</div>
                  </th>
                )
              })}
              <th width="40" className="no-sort" />
            </tr>
          </thead>
          <tbody>
            {listData.map((row) => {
              const lastCell = row[lastIndex]
              const rkey = `tr-${lastCell.id}`
              const viewUrl = `${rb.baseUrl}/app/redirect?id=${lastCell.id}&type=newtab`
              return (
                <tr key={rkey} data-id={lastCell.id} onDoubleClick={() => window.open(viewUrl)}>
                  {row.map((c, idx) => {
                    if (idx === lastIndex) return null // Last is ID
                    return this.renderCell(c, listFields[idx])
                  })}
                  <td className="open-newtab">
                    <a href={viewUrl} target="_blank" title={$L('查看记录')}>
                      <i className="zmdi zmdi-open-in-new icon" />
                    </a>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      )

    this.setState({ chartdata: <div className="chart ctable">{table}</div> }, () => {
      this._$tb = $(this._$body)
      this._$tb
        .find('.ctable')
        .css('height', this._$tb.height() - 20)
        .perfectScrollbar()

      const $trs = this._$tb.find('tbody tr').on('mousedown', function () {
        $trs.removeClass('highlight')
        $(this).addClass('highlight')
      })
    })
  }

  renderCell(cellVal, field) {
    return CellRenders.render(cellVal, field.type, 'auto', `cell-${field.field}`)
  }

  resize() {
    $setTimeout(
      () => {
        if (this._$tb) this._$tb.find('.ctable').css('height', this._$tb.height() - 20)
      },
      400,
      `resize-chart-${this.state.id}`
    )
  }

  export() {
    const table = $(this._$body).find('table.table')[0]
    if (table) {
      this._exportTable(table)
    } else {
      RbHighbar.createl('暂无数据')
    }
  }
}

// 地图（点）
class ChartCNMap extends BaseChart {
  renderChart(data) {
    this.__dataLast = data
    if (data.data.length === 0) {
      this.renderError($L('暂无数据'))
      return
    }

    const elid = `echarts-cnmap-${this.state.id || 'id'}`
    this.setState({ chartdata: <div className="chart cnmap" id={elid} /> }, () => {
      const data4map = []
      data.data.forEach((item) => {
        let lnglat = item[1].split(',')
        data4map.push([lnglat[0], lnglat[1], item[2] || null, item[0]])
      })

      const hasNumAxis = data.name ? true : false
      const mapTheme = data._renderOption && data._renderOption.themeStyle
      let mapStyle = []
      if (mapTheme === 'dark') mapStyle = window.MAP_STYLE_DARK
      else if (mapTheme === 'light') mapStyle = window.MAP_STYLE_LIGHT

      // https://github.com/apache/echarts/tree/master/extension-src/bmap
      const option = {
        ...$clone(ECHART_BASE),
        bmap: {
          zoom: 10,
          roam: true,
          mapOptions: {
            enableMapClick: false,
          },
          mapStyle: {
            styleJson: mapStyle || [],
          },
        },
        series: [
          {
            type: hasNumAxis ? 'effectScatter' : 'scatter',
            coordinateSystem: 'bmap',
            symbol: data.name ? 'circle' : 'pin',
            symbolSize: function () {
              return hasNumAxis ? 14 : 20
            },
            data: data4map,
            encode: {
              value: 2,
            },
            showEffectOn: 'emphasis',
            rippleEffect: {
              brushType: 'stroke',
            },
          },
        ],
      }

      option.color = ['#ea4335', '#4285f4']
      option.tooltip.trigger = 'item'
      option.tooltip.formatter = function (a) {
        if (data.name) {
          return `<b>${a.data[3]}</b> <br/> ${a.marker} ${data.name} : ${formatThousands(a.data[2])}`
        } else {
          return `<b>${a.data[3]}</b>`
        }
      }

      $useMap(() => {
        // https://github.com/apache/echarts/tree/master/extension-src/bmap
        $getScript('/assets/lib/charts/bmap.min.js', () => {
          this._resizeBody()
          this._echarts = renderEChart(option, elid)
        })
      }, true)
    })
  }

  resize() {
    $setTimeout(
      () => {
        const resize = this._resizeBody()
        // resize
        if (resize !== false && this._echarts) {
          this._echarts.dispose()
          this.renderChart(this.__dataLast)
        }
      },
      400,
      `resize-chart-${this.state.id}`
    )
  }

  _resizeBody() {
    const H = $(this._$box).height()
    const W = $(this._$box).width()
    if (this.__lastHW && this.__lastHW[0] === H && this.__lastHW[1] === W) return false

    $(this._$box)
      .find('.chart-body')
      .height(H - (window.render_preview_chart ? 0 : 20))
    this.__lastHW = [H, W]
  }

  export() {
    RbHighbar.createl('该图表暂不支持导出')
  }
}

class HeadingText extends BaseChart {
  constructor(props) {
    super(props)
    this.state.title = null
  }

  renderChart() {
    const config2 = this.state.config.extconfig || {}
    let style2 = { ...config2.style }
    if (style2.fontSize) style2.fontSize = ~~style2.fontSize
    // bg
    style2.bgcolor = style2.bgcolor || 'transparent'
    $(this._$box).parent().css({ backgroundColor: style2.bgcolor })
    delete style2.bgcolor

    const H = (
      <div className="must-center">
        <h1 className="m-0 text-ellipsis" style={style2}>
          {this._flashContent(config2.title)}
        </h1>
      </div>
    )
    this.setState({ chartdata: H }, () => {
      if (this.__timer) {
        clearInterval(this.__timer)
        this.__timer = null
      }

      if (config2.title && config2.title.includes('{NOW}')) {
        this.__timer = setInterval(() => {
          $(this._$box).find('h1').text(this._flashContent(config2.title))
        }, 1000)
      }

      // v42 radius
      if ((config2.style || {}).bgradius) {
        $(this._$box).parents('.chart-box').css('border-radius', ~~config2.style.bgradius)
        $(this._$box).parents('.grid-stack-item-content').css('border-radius', ~~config2.style.bgradius)
      }
    })
  }

  _flashContent(text) {
    if (text) {
      if (text.includes('{NOW}')) text = text.replace('{NOW}', moment().format('YYYY-MM-DD HH:mm:ss'))
      if (text.includes('{USER}')) text = text.replace('{USER}', $('.rb-user-nav .user-name:eq(0)').text())
      return text
    }
    return $L('标题文字')
  }

  componentDidMount() {
    super.componentDidMount()

    // class
    $(this._$box).parent().parent().addClass('HeadingText')

    // action
    const $op = $(this._$box).find('.chart-oper')
    $op.find('.J_fullscreen, .J_source').remove()
    $op.find('.J_chart-edit').on('click', (e) => {
      $stopEvent(e, true)

      let config2 = this.state.config.extconfig || {}
      renderRbcomp(
        // eslint-disable-next-line react/jsx-no-undef
        <HeadingTextSettings
          chart={this.props.id}
          {...config2}
          onConfirm={(s) => {
            const c = { ...this.state.config }
            c.extconfig = { ...config2, ...s }
            if (typeof window.save_dashboard === 'function') {
              this.setState({ config: c }, () => this.loadChartData())
            } else {
              console.log('No `save_dashboard` found :', s)
            }
          }}
        />
      )
    })
  }
}

class EmbedFrame extends BaseChart {
  renderChart(data) {
    const config2 = this.state.config.extconfig || {}
    if (!config2.url) {
      super.renderError(
        <RF>
          <span>{$L('当前图表无数据')}</span>
          {this.props.isManageable && rb.currentUser && <div>{WrapHtml($L('请先 [编辑图表](###)'))}</div>}
        </RF>,
        () => {
          $(this._$box)
            .find('.chart-undata a')
            .on('click', (e) => {
              $stopEvent(e, true)
              $(this._$box).find('.chart-oper .J_chart-edit').trigger('click')
            })
        }
      )
      return
    }

    const url = data.url || config2.url
    const F = (
      <div className="iframe">
        <iframe src={url} frameBorder="0" width="100%" height="100%" sandbox="allow-scripts allow-same-origin allow-forms allow-popups" />
      </div>
    )
    config2.title = config2.title || this.state.title || $L('嵌入页面')
    this.setState({ chartdata: F, title: config2.title }, () => {})
  }

  componentDidMount() {
    super.componentDidMount()

    let config2 = this.state.config.extconfig || {}
    // action
    const $op = $(this._$box).find('.chart-oper')
    $op.find('.J_source').attr('href', config2.url || 'about:blank')
    $op.find('.J_chart-edit').on('click', (e) => {
      $stopEvent(e, true)

      config2 = this.state.config.extconfig || {}
      renderRbcomp(
        // eslint-disable-next-line react/jsx-no-undef
        <EmbedFrameSettings
          chart={this.props.id}
          {...config2}
          onConfirm={(s) => {
            const c = { ...this.state.config }
            c.extconfig = { ...config2, ...s }
            if (typeof window.save_dashboard === 'function') {
              this.setState({ config: c, title: s.title }, () => this.loadChartData())
            } else {
              console.log('No `save_dashboard` found :', s)
            }
          }}
        />
      )
    })
  }
}

// ~~ 我的通知
// @see notifications.js
class MyNotification extends BaseChart {
  renderChart(data, type = 1) {
    const opComp = (
      <div className="notification-op">
        <div className="float-left">
          <div className="btn-group btn-group-sm btn-group-toggle" data-toggle="buttons">
            <label className="btn btn-secondary active" onClick={() => this.renderChart(data, 1)}>
              <input type="radio" name="__typeOfMyNotification" defaultChecked={type === 1} />
              {$L('未读')} ({data.unread.length})
            </label>
            <label className="btn btn-secondary" onClick={() => this.renderChart(data, 2)}>
              <input type="radio" name="__typeOfMyNotification" defaultChecked={type === 2} />
              {$L('已读')} ({data.readed.length})
            </label>
          </div>
        </div>
        <div className="float-right">
          <button
            className="btn btn-sm btn-secondary"
            type="button"
            onClick={() => {
              const that = this
              RbAlert.create($L('确认已读全部通知？'), {
                onConfirm: function () {
                  this.hide()
                  that.makeRead('ALL')
                },
              })
            }}>
            <i className="zmdi zmdi-check-all icon"></i> {$L('已读全部')}
          </button>
        </div>
        <div className="clearfix"></div>
      </div>
    )

    const dd = type === 1 ? data.unread : data.readed
    const chartdata = (
      <RF>
        {opComp}
        <div className="rb-notifications notification-list rb-scroller">
          {dd.length === 0 ? (
            <div className="chart-undata must-center" style={{ marginTop: -15 }}>
              <i className="zmdi zmdi-notifications icon" /> {$L('暂无通知')}
            </div>
          ) : (
            <ul className="list-unstyled m-0">{dd.map((item) => this.renderItem(item))}</ul>
          )}
          {dd.length >= 500 && (
            <div className="m-2 text-center text-warning">
              <i className="mdi mdi-information-outline" /> {$L('最多显示最近 500 条')}
            </div>
          )}
        </div>
      </RF>
    )

    this.setState({ chartdata: chartdata }, () => {
      const $scroller = $(this._$box).find('.rb-scroller').perfectScrollbar('destroy')
      $scroller.css('height', $(this._$body).height() - 34)
      dd.length > 0 && $scroller.perfectScrollbar()
    })
  }

  resize() {
    $setTimeout(
      () => {
        const $scroller = $(this._$box).find('.rb-scroller').perfectScrollbar('destroy')
        $scroller.css('height', $(this._$body).height() - 34)
        $scroller.perfectScrollbar()
      },
      400,
      `resize-chart-${this.state.id}`
    )
  }

  renderItem(item) {
    // const append = item[6] === 30
    const append = item[5] && ~~item[5].substr(0, 3) !== 29 // 过滤审批步骤ID
    let clazz = 'notification'
    if (item[3]) clazz += ' notification-unread'
    if (append) clazz += ' append'

    return (
      <li id={item[4]} className={clazz} key={item[4]} onClick={(e) => this.makeRead(item[4], e.currentTarget)}>
        <span className="a">
          <div className="image">
            <img src={`${rb.baseUrl}/account/user-avatar/${item[0][0]}`} title={item[0][1]} alt="Avatar" />
          </div>
          <div className="notification-info">
            <div className="text" dangerouslySetInnerHTML={{ __html: item[1] }} />
            <div className="date">
              <DateShow date={item[2]} />
            </div>
          </div>
          {append && (
            <a title={$L('查看记录')} className="badge link" href={`${rb.baseUrl}/app/redirect?id=${item[5]}&type=newtab`} target="_blank">
              {$L('查看')}
            </a>
          )}
          {item[3] && (
            <a className="read-mark text-muted">
              <i className="icon zmdi zmdi-check text-bold" /> {$L('设为已读')}
            </a>
          )}
        </span>
      </li>
    )
  }

  makeRead(id, $el) {
    $.post(`/notification/make-read?id=${id}`, () => {
      if (id === 'ALL') {
        RbHighbar.success($L('全部通知已设为已读'))
        this.loadChartData()
      } else {
        $($el).removeClass('notification-unread')
      }
    })
  }
}

// 确定图表类型
// eslint-disable-next-line no-unused-vars
const detectChart = function (conf, id) {
  // isManageable = 图表可编辑
  // editable = 仪表盘可编辑
  const props = { config: conf, id: id, title: conf.title, type: conf.type, isManageable: conf.isManageable, editable: conf.editable }

  if (conf.type === 'INDEX') {
    return <ChartIndex {...props} />
  } else if (conf.type === 'TABLE') {
    return <ChartTable {...props} />
  } else if (conf.type === 'LINE') {
    return <ChartLine {...props} />
  } else if (conf.type === 'BAR') {
    return <ChartBar {...props} />
  } else if (conf.type === 'BAR2') {
    return <ChartBar2 {...props} />
  } else if (conf.type === 'BAR3') {
    return <ChartBar3 {...props} />
  } else if (conf.type === 'PARETO') {
    return <ChartPareto {...props} />
  } else if (conf.type === 'PIE') {
    return <ChartPie {...props} />
  } else if (conf.type === 'FUNNEL') {
    return <ChartFunnel {...props} />
  } else if (conf.type === 'TREEMAP') {
    return <ChartTreemap {...props} />
  } else if (conf.type === 'ApprovalList') {
    return <ApprovalList {...props} builtin />
  } else if (conf.type === 'FeedsSchedule') {
    return <FeedsSchedule {...props} builtin />
  } else if (conf.type === 'RADAR') {
    return <ChartRadar {...props} />
  } else if (conf.type === 'SCATTER') {
    return <ChartScatter {...props} />
  } else if (conf.type === 'ProjectTasks') {
    return <ProjectTasks {...props} builtin />
  } else if (conf.type === 'DataList' || conf.type === 'DATALIST2') {
    return <DataList {...props} builtin={false} />
  } else if (conf.type === 'CNMAP') {
    return <ChartCNMap {...props} />
  } else if (conf.type === 'HeadingText') {
    return <HeadingText {...props} builtin={false} />
  } else if (conf.type === 'EmbedFrame') {
    return <EmbedFrame {...props} builtin={false} />
  } else if (conf.type === 'MyNotification') {
    return <MyNotification {...props} builtin />
  } else {
    return <h4 className="chart-undata must-center">{`${$L('未知图表')} [${conf.type}]`}</h4>
  }
}

// 从已有图表中选择图表
// 添加的图表会在多个仪表盘共享（本身就是一个），修改时会同步修改
// eslint-disable-next-line no-unused-vars
class ChartSelect extends RbModalHandler {
  constructor(props) {
    super(props)
    this.state = { appended: props.appended || [], tabActive: props.entity ? '#entity' : '#all' }
  }

  render() {
    const chartList = this.state.chartList || []

    return (
      <RbModal ref={(c) => (this._dlg = c)} title={$L('添加已有图表')} useScrollable>
        <div className="m-1 mb-4">
          <div className="row chart-select-wrap">
            <div className="col-3">
              <div className="nav flex-column nav-pills">
                <a href="#all" onClick={this.switchTab} className={`nav-link ${this.state.tabActive === '#all' ? 'active' : ''}`}>
                  {$L('全部')}
                </a>
                {this.props.entity && (
                  <a href="#entity" onClick={this.switchTab} className={`nav-link ${this.state.tabActive === '#entity' ? 'active' : ''}`}>
                    {$L('当前实体')}
                  </a>
                )}
                <a href="#myself" onClick={this.switchTab} className={`nav-link ${this.state.tabActive === '#myself' ? 'active' : ''}`}>
                  {$L('我自己的')}
                </a>
                <a href="#builtin" onClick={this.switchTab} className={`nav-link ${this.state.tabActive === '#builtin' ? 'active' : ''}`}>
                  {$L('内置图表')}
                </a>
              </div>
            </div>
            <div className="col-9 pl-0">
              <div className="chart-list">
                {chartList.length === 0 && <p className="text-muted">{$L('无可用图表')}</p>}
                {chartList.map((item) => {
                  return (
                    <div key={item.id}>
                      <span className="float-left chart-icon">
                        <i className={`${item.type} ${item.type === 'DataList' && item.id !== '017-9000000000000004' && 'custom'}`} />
                      </span>
                      <span className="float-left title">
                        <strong>{item.title}</strong>
                        <p className="text-muted fs-12">{item.entityLabel && <span>{item.entityLabel}</span>}</p>
                      </span>
                      <span className="float-right">
                        {this.state.appended.includes(item.id) ? (
                          <a className="btn disabled" data-id={item.id}>
                            {$L('已添加')}
                          </a>
                        ) : (
                          <a className="btn" onClick={() => this.selectChart(item)}>
                            {$L('添加')}
                          </a>
                        )}
                      </span>
                      {item.isManageable && !this.props.entity && (
                        <span className="float-right">
                          <a className="delete danger-hover" onClick={() => this.deleteChart(item.id)} title={$L('删除')}>
                            <i className="zmdi zmdi-delete" />
                          </a>
                        </span>
                      )}
                      <div className="clearfix" />
                    </div>
                  )
                })}
              </div>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount = () => this._loadCharts()
  _loadCharts() {
    $.get(`/dashboard/chart-list?type=${this.state.tabActive.substr(1)}&entity=${this.props.entity || ''}`, (res) => {
      this.setState({ chartList: res.data })
    })
  }

  selectChart(item) {
    const s = this.state.appended
    s.push(item.id)
    this.setState({ appended: s })
    typeof this.props.select === 'function' && this.props.select({ ...item, chart: item.id })
  }

  deleteChart(id) {
    const that = this
    RbAlert.create(<b>{$L('确认删除此图表？')}</b>, {
      type: 'danger',
      confirmText: $L('删除'),
      confirm: function () {
        this.disabled(true)
        $.post(`/dashboard/chart-delete?id=${id}`, (res) => {
          if (res.error_code === 0) {
            this.hide()
            that._loadCharts()
          } else {
            RbHighbar.error(res.error_msg)
            this.disabled()
          }
        })
      },
    })
  }

  switchTab = (e) => {
    $stopEvent(e, true)
    const h = $(e.target).attr('href')
    this.setState({ tabActive: h }, () => this._loadCharts())
  }
}
