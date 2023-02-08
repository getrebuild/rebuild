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
    const opActions = (
      <div className="chart-oper">
        {!this.props.builtin && (
          <a className="J_view-source" title={$L('查看来源数据')} href={`${rb.baseUrl}/dashboard/view-chart-source?id=${this.props.id}`} target="_blank">
            <i className="zmdi zmdi-rss" />
          </a>
        )}
        <a title={$L('刷新')} onClick={() => this.loadChartData()}>
          <i className="zmdi zmdi-refresh" />
        </a>
        <a className="J_fullscreen d-none d-md-inline-block" title={$L('全屏')} onClick={() => this.toggleFullscreen()}>
          <i className={`zmdi zmdi-${this.state.fullscreen ? 'fullscreen-exit' : 'fullscreen'}`} />
        </a>
        {this.props.isManageable && !this.props.builtin && (
          <a className="J_chart-edit d-none d-md-inline-block" title={$L('编辑')} href={`${rb.baseUrl}/dashboard/chart-design?id=${this.props.id}`}>
            <i className="zmdi zmdi-edit" />
          </a>
        )}
        {this.props.editable && (
          <a title={$L('移除')} onClick={() => this.remove()}>
            <i className="zmdi zmdi-close" />
          </a>
        )}
      </div>
    )

    return (
      <div className={`chart-box ${this.props.type}`} ref={(c) => (this._$box = c)}>
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

  loadChartData(notClear) {
    if (notClear !== true) this.setState({ chartdata: null })
    $.post(this.buildDataUrl(), JSON.stringify(this.state.config || {}), (res) => {
      if (this._echarts) this._echarts.dispose()

      if (res.error_code === 0) this.renderChart(res.data)
      else this.renderError(res.error_msg)
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
    const use = forceFullscreen === true ? true : !this.state.fullscreen
    this.setState({ fullscreen: use }, () => {
      const $box = $(this._$box).parents('.grid-stack-item')
      const $stack = $('.chart-grid>.grid-stack')

      if (this.state.fullscreen) {
        BaseChart.currentFullscreen = this
        if (!this.__chartStackHeight) this.__chartStackHeight = $stack.height()

        $box.addClass('fullscreen')
        let height = $(window).height() - ($(document.body).hasClass('fullscreen') ? 80 : 140)
        height -= $('.announcement-wrapper').height() || 0
        $stack.css({ height: Math.max(height, 300), overflow: 'hidden' })
      } else {
        $box.removeClass('fullscreen')
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
    const color = __PREVIEW ? this.props.config.option.useColor : this.props.config.color
    const style2 = { color: color || null }

    const chartdata = (
      <div className="chart index" ref={(c) => (this._$chart = c)}>
        <div className="data-item must-center text-truncate w-auto">
          <p style={style2}>{data.index.label || this.label}</p>
          <a href={__PREVIEW ? null : `${rb.baseUrl}/dashboard/view-chart-source?id=${this.props.id}`}>
            <strong style={style2}>{data.index.data}</strong>
          </a>
        </div>
      </div>
    )
    this.setState({ chartdata: chartdata }, () => this._resize())
  }

  resize() {
    $setTimeout(() => this._resize(), 200, `resize-chart-${this.props.id}`)
  }

  _resize() {
    const ch = $(this._$chart).height()
    const zoom = ch > 100 ? (ch > 330 ? 2 : 1.3) : 1
    $(this._$chart).find('strong').css('zoom', zoom)

    // const $text = $(this._$chart).find('strong')
    // zoom = $(this._$chart).width() / $text.width()
    // console.log(this.props.id, zoom)
    // if (zoom < 1 || ch < 120) zoom = 1
    // if (zoom > 2 && ch < 200) zoom = 2
    // $text.css('zoom', Math.min(zoom, 3))
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

      let tdActive = null
      const $els = $tb.find('tbody td').on('mousedown', function () {
        if (tdActive === this) {
          $(this).toggleClass('highlight')
          return
        }
        tdActive = this
        $els.removeClass('highlight')
        $(this).addClass('highlight')
      })

      if (window.render_preview_chart) {
        $tb.find('tbody td>a').removeAttr('href')
      } else {
        $tb.find('tbody td>a').each(function () {
          const $a = $(this)
          $a.attr({ href: `${rb.baseUrl}${$a.attr('href')}`, target: '_blank' })
        })
      }

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

const ECHART_BASE = {
  grid: { left: 60, right: 30, top: 30, bottom: 30 },
  animation: false,
  tooltip: {
    trigger: 'item',
    textStyle: {
      fontSize: 12,
      lineHeight: 1.3,
      color: '#333',
    },
    axisPointer: {
      lineStyle: { color: COLOR_AXIS },
    },
    backgroundColor: '#fff',
    extraCssText: 'border-radius:0;box-shadow:0 0 6px 0 rgba(0, 0, 0, .1), 0 8px 10px 0 rgba(170, 182, 206, .2);',
    confine: true,
    position: 'top',
  },
  textStyle: {
    fontFamily: 'Roboto, "Hiragina Sans GB", San Francisco, "Helvetica Neue", Helvetica, Arial, PingFangSC-Light, "WenQuanYi Micro Hei", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif',
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

const ECHART_VALUE_LABEL = {
  show: true,
  formatter: function (a) {
    return formatThousands(a.data)
  },
}

const ECHART_TOOLTIP_FORMATTER = function (i) {
  if (!Array.isArray(i)) i = [i] // Object > Array
  const tooltip = [`<b>${i[0].name}</b>`]
  i.forEach((item) => {
    tooltip.push(`${item.marker} ${item.seriesName} : ${formatThousands(item.value)}`)
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

// K=千 M=百万
const shortNumber = function (num) {
  if (rb.locale === 'zh_CN' && (num > 10000 || num < -10000)) return (num / 10000).toFixed(0) + '万'

  if (num > 1000000 || num < -1000000) return (num / 1000000).toFixed(0) + 'M'
  else if (num > 10000 || num < -10000) return (num / 1000).toFixed(0) + 'K'
  else return num
}

const formatThousands = function (num) {
  if (Math.abs(~~num) < 1000) return num
  const nums = (num + '').split('.')
  nums[0] = nums[0].replace(/\d{1,3}(?=(\d{3})+$)/g, '$&,')
  return nums.join('.')
}

const cloneOption = function (opt) {
  opt = JSON.stringify(opt)
  return JSON.parse(opt)
}

const renderEChart = function (option, $target) {
  const c = echarts.init(document.getElementById($target), 'light', {
    renderer: navigator.userAgent.match(/(iPhone|iPod|Android|ios|SymbianOS)/i) ? 'svg' : 'canvas',
  })
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

      for (let i = 0; i < data.yyyAxis.length; i++) {
        const yAxis = data.yyyAxis[i]
        yAxis.type = 'line'
        yAxis.smooth = true
        yAxis.lineStyle = { width: 3 }
        yAxis.itemStyle = {
          normal: { borderWidth: 2 },
          emphasis: { borderWidth: 6 },
        }
        if (showNumerical) yAxis.label = ECHART_VALUE_LABEL
        yAxis.cursor = 'default'
        data.yyyAxis[i] = yAxis
      }

      const option = {
        ...ECHART_BASE,
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
      option.tooltip.formatter = ECHART_TOOLTIP_FORMATTER
      if (showLegend) {
        option.legend = ECHART_LEGEND_HOPT
        option.grid.top = 40
      }

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

      for (let i = 0; i < data.yyyAxis.length; i++) {
        const yAxis = data.yyyAxis[i]
        yAxis.type = 'bar'
        if (showNumerical) yAxis.label = ECHART_VALUE_LABEL
        yAxis.cursor = 'default'
        data.yyyAxis[i] = yAxis
      }

      const option = {
        ...ECHART_BASE,
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
      option.tooltip.formatter = ECHART_TOOLTIP_FORMATTER
      if (showLegend) {
        option.legend = ECHART_LEGEND_HOPT
        option.grid.top = 40
      }

      this._echarts = renderEChart(option, elid)
    })
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

      data = { ...data, type: 'pie', radius: '71%', cursor: 'default' }
      if (showNumerical) {
        data.label = {
          formatter: function (a) {
            return `${a.data.name} (${formatThousands(a.data.value)})`
          },
        }
      }
      const option = {
        ...cloneOption(ECHART_BASE),
        series: [data],
      }
      option.tooltip.trigger = 'item'
      option.tooltip.formatter = ECHART_TOOLTIP_FORMATTER
      option.tooltip.formatter = function (i) {
        return `<b>${i.data.name}</b> <br/> ${i.marker} ${i.seriesName} : ${formatThousands(i.data.value)} (${i.percent}%)`
      }
      if (showLegend) option.legend = ECHART_LEGEND_VOPT

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

      const option = {
        ...cloneOption(ECHART_BASE),
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
                return showNumerical ? `${a.data.name} (${formatThousands(a.data.value)})` : a.data.name
              },
            },
          },
        ],
      }
      option.tooltip.trigger = 'item'
      option.tooltip.formatter = function (i) {
        if (data.xLabel) return `<b>${i.name}</b> <br/> ${i.marker} ${data.xLabel} : ${formatThousands(i.value)}`
        else return `<b>${i.name}</b> <br/> ${i.marker} ${formatThousands(i.value)}`
      }
      if (showLegend) option.legend = ECHART_LEGEND_VOPT

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

      const option = {
        ...cloneOption(ECHART_BASE),
        series: [
          {
            data: data.data,
            type: 'treemap',
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
      option.tooltip.formatter = function (i) {
        const p = i.value > 0 ? ((i.value * 100) / data.xAmount).toFixed(2) : 0
        return `<b>${i.name.split(LEVELS_SPLIT).join('<br/>')}</b> <br/> ${i.marker} ${data.xLabel} : ${formatThousands(i.value)} (${p}%)`
      }
      option.label = {
        formatter: function (a) {
          const ns = a.name.split(LEVELS_SPLIT)
          return ns[ns.length - 1] + (showNumerical ? ` (${formatThousands(a.value)})` : '')
        },
      }

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
    this.state.viewState = 1
  }

  renderChart(data) {
    let statsTotal = 0
    this._lastStats = this._lastStats || data.stats
    this._lastStats.forEach((item) => (statsTotal += item[1]))

    const stats = (
      <div className="progress-wrap sticky">
        <div className="progress">
          {this._lastStats.map((item) => {
            const s = APPROVAL_STATES[item[0]]
            if (!s || s[1] <= 0) return null

            const p = ((item[1] * 100) / statsTotal).toFixed(2) + '%'
            return (
              <div
                key={s[0]}
                className={`progress-bar bg-${s[0]} ${this.state.viewState === item[0] && 'active'}`}
                title={`${s[1]} : ${item[1]} (${p})`}
                style={{ width: p }}
                onClick={() => this._changeState(item[0])}>
                {s[1]} ({item[1]})
              </div>
            )
          })}
        </div>
        <p className="m-0 mt-1 fs-11 text-muted text-right hide">{$L('审批统计')}</p>
      </div>
    )

    if (statsTotal === 0) {
      this.renderError($L('暂无数据'))
      return
    }

    const table =
      (data.data || []).length === 0 ? (
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
              {data.data.map((item, idx) => {
                return (
                  <tr key={'approval-' + idx}>
                    <td className="user-avatar cell-detail user-info">
                      <img src={`${rb.baseUrl}/account/user-avatar/${item[0]}`} alt="Avatar" />
                      <span>{item[1]}</span>
                      <span className="cell-detail-description">
                        <DateShow date={item[2]} />
                      </span>
                    </td>
                    <td className="cell-detail">
                      <a href={`${rb.baseUrl}/app/redirect?id=${item[3]}`}>{item[4]}</a>
                      <span className="cell-detail-description">{item[6]}</span>
                    </td>
                    <td className="actions text-right text-nowrap">
                      {this.state.viewState === 1 && (
                        <button className="btn btn-secondary btn-sm" onClick={() => this.approve(item[3], item[5], item[7])}>
                          {$L('审批')}
                        </button>
                      )}
                      {this.state.viewState === 10 && <span className="text-success">{$L('通过')}</span>}
                      {this.state.viewState === 11 && <span className="text-danger">{$L('驳回')}</span>}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )

    const chartdata = (
      <div className="chart ApprovalList">
        {stats}
        {table}
      </div>
    )
    this.setState({ chartdata: chartdata }, () => {
      const $tb = $(this._$body)
      $tb
        .find('.ApprovalList')
        .css('height', $tb.height() - 5)
        .perfectScrollbar()
      this._$tb = $tb
    })
  }

  resize() {
    $setTimeout(
      () => {
        if (this._$tb) this._$tb.find('.ApprovalList').css('height', this._$tb.height() - 5)
      },
      400,
      `resize-chart-${this.state.id}`
    )
  }

  approve(record, approval, entity) {
    event.preventDefault()
    if (this.__approvalForms[record]) {
      this.__approvalForms[record].show()
    } else {
      const that = this
      const close = function () {
        if (that.__approvalForms[record]) that.__approvalForms[record].hide(true)
        that.loadChartData()
      }
      // eslint-disable-next-line react/jsx-no-undef
      renderRbcomp(<ApprovalApproveForm id={record} approval={approval} entity={entity} call={close} />, null, function () {
        that.__approvalForms[record] = this
      })
    }
  }

  _changeState(state) {
    if (state === this.state.viewState) state = 1
    this.setState({ viewState: state }, () => this.loadChartData())
  }

  buildDataUrl() {
    return `${super.buildDataUrl()}&state=${this.state.viewState}`
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
                  scheduleTimeTip = <span className="badge badge-primary">{$fromNow(item.scheduleTime)}</span>
                }

                return (
                  <tr key={'schedule-' + idx}>
                    <td>
                      <a href={`${rb.baseUrl}/app/redirect?id=${item.id}`} className="content text-break" dangerouslySetInnerHTML={{ __html: item.content }} />
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

      const option = {
        ...cloneOption(ECHART_BASE),
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
              color: ['#fff', '#fff', '#fff', '#fff', '#fff'],
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
                return formatThousands(a.value)
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
          tooltip.push(`${data.indicator[idx].name} : ${formatThousands(item)}`)
        })
        return tooltip.join('<br/>')
      }
      if (showLegend) option.legend = ECHART_LEGEND_VOPT

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
          // symbolSize: 20,
          symbolSize: function (data) {
            const s = Math.sqrt(~~data[0])
            return Math.max(Math.min(s, 120), 8)
          },
          // itemStyle: {
          //   shadowBlur: 10,
          //   shadowColor: 'rgba(120, 36, 50, 0.5)',
          //   shadowOffsetY: 5,
          //   color: new echarts.graphic.RadialGradient(0.4, 0.3, 1, [{
          //     offset: 0,
          //     color: 'rgb(251, 118, 123)'
          //   }, {
          //     offset: 1,
          //     color: 'rgb(204, 46, 72)'
          //   }])
          // },
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
        ...cloneOption(ECHART_BASE),
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
        tooltip.push(`${data.dataLabel[1]} : ${formatThousands(a.value[1])}`)
        tooltip.push(`${data.dataLabel[0]} : ${formatThousands(a.value[0])}`)
        return tooltip.join('<br>')
      }
      if (showLegend) {
        option.legend = ECHART_LEGEND_HOPT
        option.grid.top = 40
      }

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
                <th width="40" />
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
                    deadlineClass = 'badge-primary'
                  }
                }

                return (
                  <tr key={item.id} className={`status-${item.status} priority-${item.priority}`}>
                    <td className="align-text-top">
                      <label className="custom-control custom-control-sm custom-checkbox custom-control-inline ptask" title={$L('完成')}>
                        <input className="custom-control-input" type="checkbox" disabled={item.planFlow === 2} onClick={(e) => this._toggleStatus(item, e)} />
                        <span className="custom-control-label" />
                      </label>
                    </td>
                    <td>
                      <a title={item.taskName} href={`${rb.baseUrl}/app/redirect?id=${item.id}`} className="content">
                        <p className="text-break">
                          [{item.taskNumber}] {item.taskName}
                        </p>
                      </a>
                      <p className="text-muted fs-12 m-0" style={{ lineHeight: 1 }}>
                        {item.projectName}
                      </p>
                    </td>
                    <td className="text-muted">
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
      $target
        .parents('tr')
        .removeClass('status-0 status-1')
        .addClass('status-' + data.status)
    })
  }
}

// ~~ 通用数据列表
class DataList extends BaseChart {
  componentDidMount() {
    super.componentDidMount()

    const $op = $(this._$box).find('.chart-oper')
    $op.find('.J_chart-edit').on('click', (e) => {
      $stopEvent(e, true)

      const config2 = this.state.config
      renderRbcomp(
        <DataListSettings
          chart={config2.chart}
          {...config2.extconfig}
          onConfirm={(s) => {
            if (typeof window.save_dashboard === 'function') {
              config2.extconfig = s
              this.setState({ config: config2 }, () => this.loadChartData())
            } else {
              console.log('No `save_dashboard` found :', s)
            }
          }}
        />
      )
    })
  }

  renderChart(data) {
    if (data.error === 'UNSET') {
      super.renderError(
        <RF>
          <span>{$L('当前图表无数据')}</span>
          {this.props.isManageable && <div>{WrapHtml($L('请先 [编辑图表](###)'))}</div>}
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

    const extconfig = this.state.config.extconfig
    extconfig && this.setState({ title: extconfig.title || $L('数据列表') })

    const listFields = data.fields
    const listData = data.data
    const lastIndex = listFields.length

    const table = (
      <RF>
        <table className="table table-hover">
          <thead>
            <tr ref={(c) => (this._$head = c)}>
              {listFields.map((item) => {
                let sortClazz = null
                if (extconfig && extconfig.sort) {
                  const s = extconfig.sort.split(':')
                  if (s[0] === item.field && s[1] === 'asc') sortClazz = 'sort-asc'
                  if (s[0] === item.field && s[1] === 'desc') sortClazz = 'sort-desc'
                }

                return (
                  <th
                    key={item.field}
                    data-field={item.field}
                    className={sortClazz}
                    onClick={(e) => {
                      // eslint-disable-next-line no-undef
                      if (COLUMN_UNSORT.includes(item.type)) return

                      const $th = $(e.target)
                      const hasAsc = $th.hasClass('sort-asc'),
                        hasDesc = $th.hasClass('sort-desc')

                      $(this._$head).find('th').removeClass('sort-asc sort-desc')
                      if (hasDesc) $th.addClass('sort-asc')
                      else if (hasAsc) $th.addClass('sort-desc')
                      else $th.addClass('sort-asc')

                      // refresh
                      const config2 = this.state.config
                      config2.extconfig.sort = `${item.field}:${$th.hasClass('sort-desc') ? 'desc' : 'asc'}`
                      this.setState({ config: config2 }, () => this.loadChartData(true))
                    }}>
                    {item.label}
                  </th>
                )
              })}
            </tr>
          </thead>
          <tbody>
            {listData.map((row) => {
              const lastCell = row[lastIndex]
              const rkey = `tr-${lastCell.id}`
              return (
                <tr
                  key={rkey}
                  data-id={lastCell.id}
                  onDoubleClick={(e) => {
                    $stopEvent(e, true)
                    window.open(`${rb.baseUrl}/app/redirect?id=${lastCell.id}`)
                  }}>
                  {row.map((c, idx) => {
                    if (idx === lastIndex) return null // Last is ID
                    return this.renderCell(c, listFields[idx])
                  })}
                </tr>
              )
            })}
          </tbody>
        </table>
        {listData.length === 0 && <div className="chart-undata must-center">{$L('暂无数据')}</div>}
      </RF>
    )

    this.setState({ chartdata: <div className="chart ctable">{table}</div> }, () => {
      this._$tb = $(this._$body)
      this._$tb
        .find('.ctable')
        .css('height', this._$tb.height() - 20)
        .perfectScrollbar()

      let trActive
      const $els = this._$tb.find('tbody tr').on('mousedown', function () {
        if (trActive === this) {
          $(this).toggleClass('highlight')
          return
        }
        trActive = this
        $els.removeClass('highlight')
        $(this).addClass('highlight')
      })
    })
  }

  renderCell(cellVal, field) {
    const c = CellRenders.render(cellVal, field.type, 'auto', `cell-${field.field}`)
    return c
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

// ~~ 数据列表配置
class DataListSettings extends RbModalHandler {
  render() {
    const state = this.state || {}
    const filterLen = state.filterData ? (state.filterData.items || []).length : 0

    return (
      <RbModal title={$L('设置数据列表')} disposeOnHide ref={(c) => (this._dlg = c)}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('图表数据来源')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" ref={(c) => (this._$entity = c)}>
                {(state.entities || []).map((item) => {
                  return (
                    <option key={item.name} value={item.name}>
                      {item.entityLabel}
                    </option>
                  )
                })}
              </select>
            </div>
          </div>
          <div className="form-group row pb-0 DataList-showfields">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('显示字段')}</label>
            <div className="col-sm-7">
              <div className="sortable-box rb-scroller h200" ref={(c) => (this._$showfields = c)}>
                <ol className="dd-list" _title={$L('无')}></ol>
              </div>
              <div>
                <select className="form-control form-control-sm" ref={(c) => (this._$afields = c)}>
                  <option value=""></option>
                  {(state.afields || []).map((item) => {
                    return (
                      <option key={item.field} value={item.field}>
                        {item.label}
                      </option>
                    )
                  })}
                </select>
              </div>
            </div>
          </div>

          <div className="form-group row pb-1">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('附加过滤条件')}</label>
            <div className="col-sm-7">
              <a className="btn btn-sm btn-link pl-0 text-left down-2" onClick={() => this._showFilter()}>
                {filterLen > 0 ? $L('已设置条件') + ` (${filterLen})` : $L('点击设置')}
              </a>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('最大显示条数')}</label>
            <div className="col-sm-7">
              <input type="number" className="form-control form-control-sm" placeholder="20" ref={(c) => (this._$pageSize = c)} />
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('图表名称')}</label>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" placeholder={$L('数据列表')} ref={(c) => (this._$chartTitle = c)} />
            </div>
          </div>
          {rb.isAdminUser && (
            <div className="form-group row pb-2 pt-1">
              <label className="col-sm-3 col-form-label text-sm-right"></label>
              <div className="col-sm-7">
                <label className="custom-control custom-control-sm custom-checkbox mb-0">
                  <input className="custom-control-input" type="checkbox" ref={(c) => (this._$shareChart = c)} />
                  <span className="custom-control-label">
                    {$L('共享此图表')}
                    <i className="zmdi zmdi-help zicon" title={$L('共享后其他用户也可以使用 (不能修改)')} />
                  </span>
                </label>
              </div>
            </div>
          )}

          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._$btn = c)}>
              <button className="btn btn-primary" type="button" onClick={() => this.handleConfirm()}>
                {$L('保存')}
              </button>
              <a className="btn btn-link" onClick={this.hide}>
                {$L('取消')}
              </a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    let $showfields = $(this._$showfields).perfectScrollbar()
    $showfields = $showfields
      .find('ol')
      .sortable({
        placeholder: 'dd-placeholder',
        handle: '.dd3-handle',
        axis: 'y',
      })
      .disableSelection()

    const that = this
    const props = this.props

    let $afields2
    function _loadFields() {
      if (!that._entity) {
        $(that._$afields).select2({
          placeholder: $L('无可用字段'),
        })
        return
      }

      $.get(`/app/${that._entity}/list-fields`, (res) => {
        // clear last
        if ($afields2) {
          $(that._$afields).select2('destroy')
          $showfields.empty()
          that.setState({ filterData: null })
        }

        that._afields = (res.data || {}).fieldList || []
        that.setState({ afields: that._afields }, () => {
          $afields2 = $(that._$afields)
            .select2({
              placeholder: $L('添加显示字段'),
              allowClear: false,
            })
            .val('')
            .on('change', (e) => {
              let name = e.target.value
              $showfields.find('li').each(function () {
                if ($(this).data('key') === name) {
                  name = null
                }
              })

              const x = name ? that._afields.find((x) => x.field === name) : null
              if (!x) return

              const $item = $(
                `<li class="dd-item dd3-item" data-key="${x.field}"><div class="dd-handle dd3-handle"></div><div class="dd3-content">${x.label}</div><div class="dd3-action"></div></li>`
              ).appendTo($showfields)

              // eslint-disable-next-line no-undef
              if (!COLUMN_UNSORT.includes(x.type)) {
                $(`<a title="${$L('默认排序')}"><i class="zmdi mdi mdi-sort-alphabetical-ascending sort"></i></a>`)
                  .appendTo($item.find('.dd3-action'))
                  .on('click', () => {
                    const hasActive = $item.hasClass('active')
                    $showfields.find('.dd-item').removeClass('active')
                    $item.addClass('active')
                    if (hasActive) $item.find('.sort').toggleClass('desc')
                  })

                // init
                if (props.entity === that._entity && props.sort) {
                  const s = props.sort.split(':')
                  if (s[0] === name) {
                    $item.addClass('active')
                    if (s[1] === 'desc') $item.find('.sort').toggleClass('desc')
                  }
                }
              }

              $(`<a title="${$L('移除')}"><i class="zmdi zmdi-close"></i></a>`)
                .appendTo($item.find('.dd3-action'))
                .on('click', () => $item.remove())
            })

          // init
          if (props.entity === that._entity && props.fields) {
            props.fields.forEach((name) => {
              $afields2.val(name).trigger('change')
            })
            $afields2.val('').trigger('change')
          }
        })
      })
    }

    $.get('/commons/metadata/entities?detail=yes', (res) => {
      this.setState({ entities: res.data || [] }, () => {
        const $s = $(this._$entity).select2({
          allowClear: false,
        })

        if (props.entity && props.entity !== 'User') $s.val(props.entity || null)
        $s.on('change', (e) => {
          this._entity = e.target.value
          _loadFields()
        }).trigger('change')
      })
    })

    // init
    $(this._$pageSize).val(props.pageSize || null)
    $(this._$chartTitle).val(props.title || null)
    if (props.filter) this.setState({ filterData: props.filter })
    if ((props.option || {}).shareChart) $(this._$shareChart).attr('checked', true)
  }

  _showFilter() {
    renderRbcomp(
      <AdvFilter
        entity={this._entity}
        filter={this.state.filterData || null}
        title={$L('附加过滤条件')}
        inModal
        canNoFilters
        onConfirm={(s) => {
          this.setState({ filterData: s })
        }}
      />
    )
  }

  handleConfirm() {
    const fields = []
    let sort = null
    $(this._$showfields)
      .find('li')
      .each(function () {
        const $this = $(this)
        fields.push($this.data('key'))

        if ($this.hasClass('active')) {
          sort = $this.data('key') + `:${$this.find('.desc')[0] ? 'desc' : 'asc'}`
        }
      })

    const post = {
      type: 'DataList',
      entity: $(this._$entity).val(),
      title: $(this._$chartTitle).val() || $L('数据列表'),
      option: {
        shareChart: $val(this._$shareChart) && rb.isAdminUser,
      },
      fields: fields,
      pageSize: $(this._$pageSize).val(),
      filter: this.state.filterData || null,
      sort: sort,
    }

    if (!post.entity) return RbHighbar.create($L('请选择图表数据来源'))
    if (post.fields.length === 0) return RbHighbar.create($L('请添加显示字段'))
    if (post.pageSize && post.pageSize > 500) post.pageSize = 500

    const $btn = $(this._$btn).find('.btn').button('loading')
    $.post(`/dashboard/builtin-chart-save?id=${this.props.chart}`, JSON.stringify(post), (res) => {
      $btn.button('reset')
      if (res.error_code === 0) {
        typeof this.props.onConfirm === 'function' && this.props.onConfirm(post)
        this.hide()
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

// 确定图表类型
// eslint-disable-next-line no-unused-vars
const detectChart = function (cfg, id) {
  // isManageable = 图表可编辑
  // editable = 仪表盘可编辑
  const props = { config: cfg, id: id, title: cfg.title, type: cfg.type, isManageable: cfg.isManageable, editable: cfg.editable }

  if (cfg.type === 'INDEX') {
    return <ChartIndex {...props} />
  } else if (cfg.type === 'TABLE') {
    return <ChartTable {...props} />
  } else if (cfg.type === 'LINE') {
    return <ChartLine {...props} />
  } else if (cfg.type === 'BAR') {
    return <ChartBar {...props} />
  } else if (cfg.type === 'PIE') {
    return <ChartPie {...props} />
  } else if (cfg.type === 'FUNNEL') {
    return <ChartFunnel {...props} />
  } else if (cfg.type === 'TREEMAP') {
    return <ChartTreemap {...props} />
  } else if (cfg.type === 'ApprovalList') {
    return <ApprovalList {...props} builtin={true} />
  } else if (cfg.type === 'FeedsSchedule') {
    return <FeedsSchedule {...props} builtin={true} />
  } else if (cfg.type === 'RADAR') {
    return <ChartRadar {...props} />
  } else if (cfg.type === 'SCATTER') {
    return <ChartScatter {...props} />
  } else if (cfg.type === 'ProjectTasks') {
    return <ProjectTasks {...props} builtin={true} />
  } else if (cfg.type === 'DataList') {
    return <DataList {...props} builtin={false} />
  } else {
    return <h4 className="chart-undata must-center">{`${$L('未知图表')} [${cfg.type}]`}</h4>
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
      <RbModal ref={(c) => (this._dlg = c)} title={$L('添加已有图表')}>
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
                        <a className="delete danger-hover" onClick={() => this.deleteChart(item.id)}>
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
    RbAlert.create($L('确认删除此图表？'), {
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
