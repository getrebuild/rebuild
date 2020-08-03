/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// 图表基类
class BaseChart extends React.Component {

  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    const opers = (
      <div className="chart-oper">
        {!this.props.builtin && <a title="查看来源数据" href={`${rb.baseUrl}/dashboard/view-chart-sources?id=${this.props.id}`}><i className="zmdi zmdi-rss" /></a>}
        <a onClick={() => this.loadChartData()}><i className="zmdi zmdi-refresh" /></a>
        <a onClick={() => this.toggleFullscreen()}><i className={`zmdi zmdi-${this.state.fullscreen ? 'fullscreen-exit' : 'fullscreen'}`} /></a>
        {this.props.editable && (
          <React.Fragment>
            {!this.props.builtin && <a className="chart-edit" href={`${rb.baseUrl}/dashboard/chart-design?id=${this.props.id}`}><i className="zmdi zmdi-edit" /></a>}
            <a onClick={() => this.remove()}><i className="zmdi zmdi-close" /></a>
          </React.Fragment>
        )}
      </div>
    )

    return (
      <div className={`chart-box ${this.props.type}`} ref={(c) => this._box = c}>
        <div className="chart-head">
          <div className="chart-title text-truncate">{this.state.title}</div>
          {opers}
        </div>
        <div ref={(c) => this._body = c} className={'chart-body rb-loading ' + (!this.state.chartdata && 'rb-loading-active')}>{this.state.chartdata || <RbSpinner />}</div>
      </div>
    )
  }

  componentDidMount() {
    this.loadChartData()
  }

  componentWillUnmount() {
    if (this.__echarts) this.__echarts.dispose()
  }

  loadChartData() {
    this.setState({ chartdata: null })
    const that = this
    $.post(this.buildDataUrl(), JSON.stringify(this.state.config || {}), (res) => {
      if (res.error_code === 0) that.renderChart(res.data)
      else that.renderError(res.error_msg)
    })
  }

  buildDataUrl() {
    return (this.state.id ? '/dashboard/chart-data' : '/dashboard/chart-preview') + '?id=' + (this.state.id || '')
  }

  resize() {
    if (this.__echarts) {
      $setTimeout(() => this.__echarts.resize(), 400, 'resize-chart-' + this.state.id)
    }
  }

  toggleFullscreen() {
    this.setState({ fullscreen: !this.state.fullscreen }, () => {
      const $box = $(this._box).parents('.grid-stack-item')
      const $stack = $('.chart-grid>.grid-stack')
      const wh = $(window).height() - ($(document.body).hasClass('fullscreen') ? 80 : 140)
      if (this.state.fullscreen) {
        this.__chartStackHeight = $stack.height()
        $stack.css({ height: wh, overflow: 'hidden' })
        $box.addClass('fullscreen')
      } else {
        $stack.css({ height: this.__chartStackHeight, overflow: 'unset' })
        $box.removeClass('fullscreen')
      }
      this.resize()
    })
  }

  remove() {
    const that = this
    RbAlert.create('确认移除此图表？', {
      confirm: function () {
        if (window.gridstack) window.gridstack.removeWidget($(that._box).parent().parent())
        else if (window.chart_remove) window.chart_remove($(that._box))
        this.hide()
      }
    })
  }

  renderError(msg) {
    this.setState({ chartdata: (<div className="chart-undata must-center">{msg || '图表加载失败'}</div>) })
  }

  renderChart(data) {
    this.setState({ chartdata: (<div>{JSON.stringify(data)}</div>) })
  }
}

// 指标卡
class ChartIndex extends BaseChart {
  constructor(props) {
    super(props)
    this.label = this.state.title
    this.state.title = null
  }

  renderChart(data) {
    const chartdata = (<div className="chart index" ref={(c) => this._chart = c}>
      <div className="data-item must-center text-truncate w-auto">
        <p>{data.index.label || this.label}</p>
        <strong>{data.index.data}</strong>
      </div>
    </div>)
    this.setState({ chartdata: chartdata }, () => this._resize())
  }

  resize() {
    $setTimeout(() => this._resize(), 200, 'resize-chart-index')
  }

  _resize() {
    const ch = $(this._chart).height()
    const $text = $(this._chart).find('strong')
    let zoom = $(this._chart).width() / $text.width() / 3
    if (zoom < 1 || ch < 120) zoom = 1
    if (zoom > 2 && ch < 200) zoom = 2
    $text.css('zoom', Math.min(zoom, 3))
  }
}

// 表格
class ChartTable extends BaseChart {
  constructor(props) {
    super(props)
  }

  renderChart(data) {
    if (!data.html) {
      this.renderError('暂无数据')
      return
    }

    const chartdata = (
      <div className="chart ctable">
        <div dangerouslySetInnerHTML={{ __html: data.html }}></div>
      </div>
    )

    const that = this
    let colLast = null
    this.setState({ chartdata: chartdata }, () => {
      const $tb = $(that._body)
      $tb.find('.ctable').css('height', $tb.height() - 20)
        .perfectScrollbar()

      const cols = $tb.find('tbody td').click(function () {
        if (colLast === this) {
          $(this).toggleClass('active')
          return
        }
        colLast = this
        cols.removeClass('active')
        $(this).addClass('active')
      })
      this.__tb = $tb
    })
  }

  resize() {
    $setTimeout(() => {
      if (this.__tb) this.__tb.find('.ctable').css('height', this.__tb.height() - 20)
    }, 400, 'resize-chart-' + this.state.id)
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
      fontSize: 12, lineHeight: 1.3, color: '#333'
    },
    axisPointer: {
      lineStyle: { color: COLOR_AXIS }
    },
    backgroundColor: '#fff',
    extraCssText: 'border-radius:0;box-shadow:0 0 6px 0 rgba(0, 0, 0, .1), 0 8px 10px 0 rgba(170, 182, 206, .2);',
    confine: true,
    position: 'top'
  },
  textStyle: {
    fontFamily: 'Roboto, "Hiragina Sans GB", San Francisco, "Helvetica Neue", Helvetica, Arial, PingFangSC-Light, "WenQuanYi Micro Hei", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif'
  }
}

const ECHART_AXIS_LABEL = {
  textStyle: {
    color: COLOR_LABEL,
    fontSize: 12,
    fontWeight: '400'
  }
}

const ECHART_VALUE_LABEL = {
  show: true,
  formatter: function (a) {
    return formatThousands(a.data)
  }
}

const ECHART_TOOLTIP_FORMATTER = function (i) {
  if (!Array.isArray(i)) i = [i]  // Object > Array
  const tooltip = [`<b>${i[0].name}</b>`]
  i.forEach((item) => {
    tooltip.push(`${item.marker} ${item.seriesName} : ${formatThousands(item.value)}`)
  })
  return tooltip.join('<br>')
}

const ECHART_RENDER_OPT = {
  renderer: navigator.userAgent.match(/(iPhone|iPod|Android|ios|SymbianOS)/i) ? 'svg' : 'canvas'
}

// 横排
const ECHART_LEGEND_HOPT = {
  type: 'plain',
  orient: 'horizontal',
  top: 10,
  right: 0,
  padding: 0,
  textStyle: { fontSize: 12 }
}
// 竖排
const ECHART_LEGEND_VOPT = {
  type: 'scroll',
  orient: 'vertical',
  top: 10,
  right: 0,
  padding: 0,
  textStyle: { fontSize: 12 }
}

const shortNumber = function (num) {
  if (num > 1000000 || num < -1000000) return (num / 1000000).toFixed(0) + 'W'
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

// 折线图
class ChartLine extends BaseChart {
  constructor(props) {
    super(props)
  }

  renderChart(data) {
    if (this.__echarts) this.__echarts.dispose()
    if (data.xAxis.length === 0) { this.renderError('暂无数据'); return }

    const that = this
    const elid = 'echarts-line-' + (this.state.id || 'id')
    this.setState({ chartdata: (<div className="chart line" id={elid}></div>) }, () => {
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
          emphasis: { borderWidth: 6 }
        }
        if (showNumerical) yAxis.label = ECHART_VALUE_LABEL
        yAxis.cursor = 'default'
        data.yyyAxis[i] = yAxis
      }

      const opt = {
        ...ECHART_BASE,
        xAxis: {
          type: 'category',
          data: data.xAxis,
          axisLabel: ECHART_AXIS_LABEL,
          axisLine: {
            lineStyle: { color: COLOR_AXIS }
          }
        },
        yAxis: {
          type: 'value',
          splitLine: { show: showGrid, lineStyle: { color: COLOR_AXIS } },
          axisLabel: {
            ...ECHART_AXIS_LABEL,
            formatter: shortNumber
          },
          axisLine: {
            lineStyle: { color: COLOR_AXIS, width: showGrid ? 1 : 0 }
          }
        },
        series: data.yyyAxis
      }
      opt.tooltip.trigger = 'axis'
      opt.tooltip.formatter = ECHART_TOOLTIP_FORMATTER
      if (showLegend) {
        opt.legend = ECHART_LEGEND_HOPT
        opt.grid.top = 40
      }

      const c = echarts.init(document.getElementById(elid), 'light', ECHART_RENDER_OPT)
      c.setOption(opt)
      that.__echarts = c
    })
  }
}

// 柱状图
class ChartBar extends BaseChart {
  constructor(props) {
    super(props)
  }

  renderChart(data) {
    if (this.__echarts) this.__echarts.dispose()
    if (data.xAxis.length === 0) { this.renderError('暂无数据'); return }

    const that = this
    const elid = 'echarts-bar-' + (this.state.id || 'id')
    this.setState({ chartdata: (<div className="chart bar" id={elid}></div>) }, () => {
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

      const opt = {
        ...ECHART_BASE,
        xAxis: {
          type: 'category',
          data: data.xAxis,
          axisLabel: ECHART_AXIS_LABEL,
          axisLine: {
            lineStyle: { color: COLOR_AXIS }
          }
        },
        yAxis: {
          type: 'value',
          splitLine: { show: showGrid, lineStyle: { color: COLOR_AXIS } },
          axisLabel: {
            ...ECHART_AXIS_LABEL,
            formatter: shortNumber
          },
          axisLine: {
            lineStyle: { color: COLOR_AXIS, width: showGrid ? 1 : 0 }
          }
        },
        series: data.yyyAxis
      }
      opt.tooltip.trigger = 'axis'
      opt.tooltip.formatter = ECHART_TOOLTIP_FORMATTER
      if (showLegend) {
        opt.legend = ECHART_LEGEND_HOPT
        opt.grid.top = 40
      }

      const c = echarts.init(document.getElementById(elid), 'light', ECHART_RENDER_OPT)
      c.setOption(opt)
      that.__echarts = c
    })
  }
}

// 饼图
class ChartPie extends BaseChart {
  constructor(props) {
    super(props)
  }

  renderChart(data) {
    if (this.__echarts) this.__echarts.dispose()
    if (data.data.length === 0) {
      this.renderError('暂无数据')
      return
    }

    const that = this
    const elid = 'echarts-pie-' + (this.state.id || 'id')
    this.setState({ chartdata: (<div className="chart pie" id={elid}></div>) }, () => {
      const showNumerical = data._renderOption && data._renderOption.showNumerical
      const showLegend = data._renderOption && data._renderOption.showLegend

      data = { ...data, type: 'pie', radius: '71%', cursor: 'default' }
      if (showNumerical) {
        data.label = {
          formatter: function (a) {
            return `${a.data.name} (${formatThousands(a.data.value)})`
          }
        }
      }
      const opt = {
        ...cloneOption(ECHART_BASE),
        series: [data],
      }
      opt.tooltip.trigger = 'item'
      opt.tooltip.formatter = ECHART_TOOLTIP_FORMATTER
      opt.tooltip.formatter = function (i) {
        return `<b>${i.data.name}</b> <br/> ${i.marker} ${i.seriesName} : ${formatThousands(i.data.value)} (${i.percent}%)`
      }
      if (showLegend) opt.legend = ECHART_LEGEND_VOPT

      const c = echarts.init(document.getElementById(elid), 'light', ECHART_RENDER_OPT)
      c.setOption(opt)
      that.__echarts = c
    })
  }
}

// 漏斗图
class ChartFunnel extends BaseChart {
  constructor(props) {
    super(props)
  }

  renderChart(data) {
    if (this.__echarts) this.__echarts.dispose()
    if (data.data.length === 0) { this.renderError('暂无数据'); return }

    const that = this
    const elid = 'echarts-funnel-' + (this.state.id || 'id')
    this.setState({ chartdata: (<div className="chart funnel" id={elid}></div>) }, () => {
      const showNumerical = data._renderOption && data._renderOption.showNumerical
      const showLegend = data._renderOption && data._renderOption.showLegend

      const opt = {
        ...cloneOption(ECHART_BASE),
        series: [{
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
            }
          }
        }]
      }
      opt.tooltip.trigger = 'item'
      opt.tooltip.formatter = function (i) {
        if (data.xLabel) return `<b>${i.name}</b> <br/> ${i.marker} ${data.xLabel} : ${formatThousands(i.value)}`
        else return `<b>${i.name}</b> <br/> ${i.marker} ${formatThousands(i.value)}`
      }
      if (showLegend) opt.legend = ECHART_LEGEND_VOPT

      const c = echarts.init(document.getElementById(elid), 'light', ECHART_RENDER_OPT)
      c.setOption(opt)
      that.__echarts = c
    })
  }
}

// 树图
const LEVELS_SPLIT = '--------'
class ChartTreemap extends BaseChart {
  constructor(props) {
    super(props)
  }

  renderChart(data) {
    if (this.__echarts) this.__echarts.dispose()
    if (data.data.length === 0) { this.renderError('暂无数据'); return }

    const that = this
    const elid = 'echarts-treemap-' + (this.state.id || 'id')
    this.setState({ chartdata: (<div className="chart treemap" id={elid}></div>) }, () => {
      const showNumerical = data._renderOption && data._renderOption.showNumerical

      const opt = {
        ...cloneOption(ECHART_BASE),
        series: [{
          data: data.data,
          type: 'treemap',
          width: '100%',
          height: '100%',
          top: window.render_preview_chart ? 0 : 15,  // In preview
          breadcrumb: { show: false },
          roam: false,  // Disabled drag and mouse wheel
          levels: [
            {
              itemStyle: {
                gapWidth: 1
              }
            }, {
              itemStyle: {
                gapWidth: 0
              }
            }, {
              itemStyle: {
                gapWidth: 0
              }
            },
          ]
        }]
      }
      opt.tooltip.trigger = 'item'
      opt.tooltip.formatter = function (i) {
        const p = i.value > 0 ? (i.value * 100 / data.xAmount).toFixed(2) : 0
        return `<b>${i.name.split(LEVELS_SPLIT).join('<br/>')}</b> <br/> ${i.marker} ${data.xLabel} : ${formatThousands(i.value)} (${p}%)`
      }
      opt.label = {
        formatter: function (a) {
          const ns = a.name.split(LEVELS_SPLIT)
          return ns[ns.length - 1] + (showNumerical ? ` (${formatThousands(a.value)})` : '')
        }
      }

      const c = echarts.init(document.getElementById(elid), 'light', ECHART_RENDER_OPT)
      c.setOption(opt)
      that.__echarts = c
    })
  }
}

// ~ 审批列表
const APPROVAL_STATES = {
  1: ['warning', '待审批'], 10: ['success', '通过'], 11: ['danger', '驳回']
}
class ApprovalList extends BaseChart {
  constructor(props) {
    super(props)
    this.__approvalForms = {}
    this.state.viewState = 1
  }

  renderChart(data) {
    let statsTotal = 0
    this.__lastStats = this.__lastStats || data.stats
    this.__lastStats.forEach((item) => statsTotal += item[1])

    const stats = <div className="progress-wrap sticky">
      <div className="progress">
        {this.__lastStats.map((item) => {
          const s = APPROVAL_STATES[item[0]]
          if (!s || s[1] <= 0) return null
          const sp = (item[1] * 100 / statsTotal).toFixed(2) + '%'
          return <div key={`state-${s[0]}`}
            className={`progress-bar bg-${s[0]} ${this.state.viewState === item[0] ? 'text-bold' : ''}`}
            title={`${s[1]} : ${item[1]} (${sp})`}
            style={{ width: sp }}
            onClick={() => this._changeState(item[0])}>{s[1]} ({item[1]})</div>
        })}
      </div>
      <p className="m-0 mt-1 fs-11 text-muted text-right hide">审批统计</p>
    </div>

    if (statsTotal === 0) {
      this.renderError('暂无数据')
      return
    }

    const table = (!data.data || data.data.length === 0) ?
      <div className="chart-undata must-center"><i className="zmdi zmdi-check icon text-success"></i> 你已完成所有审批</div>
      :
      <div>
        <table className="table table-striped table-hover">
          <thead>
            <tr>
              <th style={{ minWidth: 150 }}>提交人</th>
              <th style={{ minWidth: 150 }}>审批记录</th>
              <th width="90"></th>
            </tr>
          </thead>
          <tbody>
            {data.data.map((item, idx) => {
              return <tr key={'approval-' + idx}>
                <td className="user-avatar cell-detail user-info">
                  <img src={`${rb.baseUrl}/account/user-avatar/${item[0]}`} />
                  <span>{item[1]}</span>
                  <span className="cell-detail-description">{item[2]}</span>
                </td>
                <td className="cell-detail">
                  <a href={`${rb.baseUrl}/app/list-and-view?id=${item[3]}`}>{item[4]}</a>
                  <span className="cell-detail-description">{item[6]}</span>
                </td>
                <td className="actions text-right">
                  {this.state.viewState === 1 && <button className="btn btn-secondary btn-sm" onClick={() => this.approve(item[3], item[5], item[7])}>审批</button>}
                  {this.state.viewState === 10 && <span className="text-success">通过</span>}
                  {this.state.viewState === 11 && <span className="text-danger">驳回</span>}
                </td>
              </tr>
            })}
          </tbody>
        </table>
      </div>

    const chartdata = <div className="chart ApprovalList">
      {stats}
      {table}
    </div>
    this.setState({ chartdata: chartdata }, () => {
      const $tb = $(this._body)
      $tb.find('.ApprovalList').css('height', $tb.height() - 5).perfectScrollbar()
      this.__tb = $tb
    })
  }

  resize() {
    $setTimeout(() => {
      if (this.__tb) this.__tb.find('.ApprovalList').css('height', this.__tb.height() - 5)
    }, 400, 'resize-chart-' + this.state.id)
  }

  approve(record, approval, entity) {
    event.preventDefault()
    const that = this
    if (this.__approvalForms[record]) this.__approvalForms[record].show()
    else {
      const close = function () {
        if (that.__approvalForms[record]) that.__approvalForms[record].hide(true)
        that.loadChartData()
      }
      // eslint-disable-next-line react/jsx-no-undef
      renderRbcomp(<ApprovalApproveForm id={record} approval={approval} entity={entity} call={close} />, null, function () { that.__approvalForms[record] = this })
    }
  }

  _changeState(state) {
    this.setState({ viewState: state }, () => this.loadChartData())
  }

  buildDataUrl() {
    return super.buildDataUrl() + '&state=' + this.state.viewState
  }
}

// ~ 我的日程
class FeedsSchedule extends BaseChart {
  constructor(props) {
    super(props)
  }

  renderChart(data) {
    const table = (!data || data.length === 0) ?
      <div className="chart-undata must-center" style={{ marginTop: -15 }}>
        <i className="zmdi zmdi-check icon text-success"></i> 暂无待办日程<br />过期超过 30 天的日程将不再显示
      </div>
      :
      <div>
        <table className="table table-striped table-hover">
          <thead>
            <tr>
              <th>日程内容</th>
              <th width="140">日程时间</th>
              <th width="90"></th>
            </tr>
          </thead>
          <tbody>
            {data.map((item, idx) => {
              // 超时
              const timeover = item.scheduleLeft && item.scheduleLeft.substr(0, 1) === '-'
              if (timeover) item.scheduleLeft = item.scheduleLeft.substr(1)

              return <tr key={'schedule-' + idx}>
                <td>
                  <a title="查看详情" href={`${rb.baseUrl}/app/list-and-view?id=${item.id}`} className="content" dangerouslySetInnerHTML={{ __html: item.content }} />
                </td>
                <td className="cell-detail">
                  <div>{item.scheduleTime}</div>
                  <span className={`cell-detail-description ${timeover ? 'text-warning' : ''}`}>{item.scheduleLeft}{timeover ? ' (过期)' : ''}</span>
                </td>
                <td className="actions text-right">
                  <button className="btn btn-secondary btn-sm" onClick={() => this.handleFinish(item.id)}>完成</button>
                </td>
              </tr>
            })}
          </tbody>
        </table>
      </div>

    const chartdata = <div className="chart FeedsSchedule">
      {table}
    </div>
    this.setState({ chartdata: chartdata }, () => {
      const $tb = $(this._body)
      $tb.find('.FeedsSchedule').css('height', $tb.height() - 13).perfectScrollbar()
      this.__tb = $tb
    })
    return table
  }

  resize() {
    $setTimeout(() => {
      if (this.__tb) this.__tb.find('.FeedsSchedule').css('height', this.__tb.height() - 13)
    }, 400, 'resize-chart-' + this.state.id)
  }

  handleFinish(id) {
    const that = this
    RbAlert.create('确认完成该日程？', {
      confirm: function () {
        this.disabled(true)
        $.post(`/feeds/post/finish-schedule?id=${id}`, (res) => {
          if (res.error_code === 0) {
            this.hide()
            RbHighbar.success('日程已完成')
            that.loadChartData()
          } else RbHighbar.error(res.error_msg)
        })
      }
    })
  }
}

// 雷达图
class ChartRadar extends BaseChart {
  constructor(props) {
    super(props)
  }

  renderChart(data) {
    if (this.__echarts) this.__echarts.dispose()
    if (data.indicator.length === 0) { this.renderError('暂无数据'); return }

    const that = this
    const elid = 'echarts-radar-' + (this.state.id || 'id')
    this.setState({ chartdata: (<div className="chart radar" id={elid}></div>) }, () => {
      const showNumerical = data._renderOption && data._renderOption.showNumerical
      const showLegend = data._renderOption && data._renderOption.showLegend

      const opt = {
        ...cloneOption(ECHART_BASE),
        radar: {
          indicator: data.indicator,
          name: {
            textStyle: {
              color: COLOR_LABEL, fontSize: 12
            }
          },
          splitNumber: 4,
          splitArea: {
            areaStyle: {
              color: ['#fff', '#fff', '#fff', '#fff', '#fff']
            }
          },
          splitLine: {
            lineStyle: {
              color: COLOR_AXIS
            }
          },
          axisLine: {
            lineStyle: {
              color: COLOR_AXIS
            }
          }
        },
        series: [{
          type: 'radar',
          symbol: 'circle',
          symbolSize: 6,
          label: {
            show: showNumerical,
            formatter: function (a) {
              return formatThousands(a.value)
            }
          },
          lineStyle: {
            normal: { width: 2 },
            emphasis: { width: 3 },
            cursor: 'default',
          },
          data: data.series
        }]
      }
      opt.grid.left = 30
      opt.tooltip.trigger = 'item'
      opt.tooltip.formatter = function (a) {
        const tooltip = [`<b>${a.name}</b>`]
        a.value.forEach((item, idx) => {
          tooltip.push(`${data.indicator[idx].name} : ${formatThousands(item)}`)
        })
        return tooltip.join('<br/>')
      }
      if (showLegend) opt.legend = ECHART_LEGEND_VOPT

      const c = echarts.init(document.getElementById(elid), 'light', ECHART_RENDER_OPT)
      c.setOption(opt)
      that.__echarts = c
    })
  }
}

// 散点图
class ChartScatter extends BaseChart {
  constructor(props) {
    super(props)
  }

  renderChart(data) {
    if (this.__echarts) this.__echarts.dispose()
    if (data.series.length === 0) { this.renderError('暂无数据'); return }

    const that = this
    const elid = 'echarts-scatter-' + (this.state.id || 'id')
    this.setState({ chartdata: (<div className="chart scatter" id={elid}></div>) }, () => {
      const showGrid = data._renderOption && data._renderOption.showGrid
      const showNumerical = data._renderOption && data._renderOption.showNumerical
      const showLegend = data._renderOption && data._renderOption.showLegend

      const axisOption = {
        splitLine: {
          lineStyle: { color: COLOR_AXIS, width: showGrid ? 1 : 0, type: 'solid' }
        },
        axisLabel: {
          ...ECHART_AXIS_LABEL,
          formatter: shortNumber
        },
        axisLine: {
          lineStyle: { color: COLOR_AXIS }
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
            let s = Math.sqrt(~~data[0])
            s = Math.min(s, 120)
            s = Math.max(s, 8)
            return s
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
            }
          }
        })
      })

      const opt = {
        ...cloneOption(ECHART_BASE),
        xAxis: { ...axisOption },
        yAxis: { ...axisOption },
        series: seriesData
      }
      opt.tooltip.trigger = 'item'
      opt.tooltip.formatter = function (a) {
        const tooltip = []
        if (a.value.length === 3) {
          tooltip.push(`<b>${a.value[2]}</b>`)
        }
        tooltip.push(`${data.dataLabel[1]} : ${formatThousands(a.value[1])}`)
        tooltip.push(`${data.dataLabel[0]} : ${formatThousands(a.value[0])}`)
        return tooltip.join('<br>')
      }
      if (showLegend) {
        opt.legend = ECHART_LEGEND_HOPT
        opt.grid.top = 40
      }

      const c = echarts.init(document.getElementById(elid), 'light', ECHART_RENDER_OPT)
      c.setOption(opt)
      that.__echarts = c
    })
  }
}

// 确定图表类型
// eslint-disable-next-line no-unused-vars
const detectChart = function (cfg, id, editable) {
  const props = { config: cfg, id: id, title: cfg.title, editable: editable !== false, type: cfg.type }
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
  } else {
    return <h4 className="chart-undata must-center">{`未知图表 [${cfg.type}]`}</h4>
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
    return <RbModal ref={(c) => this._dlg = c} title="添加已有图表">
      <div className="row chart-select-wrap">
        <div className="col-3">
          <div className="nav flex-column nav-pills">
            <a href="#all" onClick={this.switchTab} className={`nav-link ${this.state.tabActive === '#all' ? 'active' : ''}`}>全部</a>
            {this.props.entity && <a href="#entity" onClick={this.switchTab} className={`nav-link ${this.state.tabActive === '#entity' ? 'active' : ''}`}>当前实体</a>}
            <a href="#myself" onClick={this.switchTab} className={`nav-link hide ${this.state.tabActive === '#myself' ? 'active' : ''}`}>我自己的</a>
            <a href="#builtin" onClick={this.switchTab} className={`nav-link ${this.state.tabActive === '#builtin' ? 'active' : ''}`}>内置图表</a>
          </div>
        </div>
        <div className="col-9 pl-0">
          <div className="chart-list">
            {(this.state.chartList && this.state.chartList.length === 0) && <p className="text-muted">无可用图表</p>}
            {(this.state.chartList || []).map((item) => {
              return (<div key={'k-' + item[0]}>
                <span className="float-left chart-icon"><i className={item[2]}></i></span>
                <span className="float-left title">
                  <strong>{item[1]}</strong>
                  <p className="text-muted fs-12">{item[4] && <span>{item[4]}</span>} <DateShow date={item[3]} /></p>
                </span>
                <span className="float-right">
                  {this.state.appended.includes(item[0])
                    ? <a className="btn disabled" data-id={item[0]}>已添加</a>
                    : <a className="btn" onClick={() => this.selectChart(item)}>添加</a>}
                </span>
                {(!this.props.entity && item[4]) && <span className="float-right"><a className="delete" onClick={() => this.deleteChart(item[0])}><i className="zmdi zmdi-delete"></i></a></span>}
                <div className="clearfix"></div>
              </div>)
            })}
          </div>
        </div>
      </div>
    </RbModal>
  }

  componentDidMount = () => this.__loadCharts()
  __loadCharts() {
    $.get(`/dashboard/chart-list?type=${this.state.tabActive.substr(1)}&entity=${this.props.entity || ''}`, (res) => {
      this.setState({ chartList: res.data })
    })
  }

  selectChart(item) {
    const s = this.state.appended
    s.push(item[0])
    this.setState({ appended: s })
    typeof this.props.select === 'function' && this.props.select({ chart: item[0], title: item[1], type: item[2] })
  }

  deleteChart(id) {
    const that = this
    RbAlert.create('确认删除此图表吗？', {
      type: 'danger',
      confirmText: '删除',
      confirm: function () {
        this.disabled(true)
        $.post(`/dashboard/chart-delete?id=${id}`, (res) => {
          if (res.error_code > 0) RbHighbar.error(res.error_msg)
          else {
            that.__loadCharts()
            this.hide()
          }
        })
      }
    })
  }

  switchTab = (e) => {
    e.preventDefault()
    const t = $(e.target).attr('href')
    this.setState({ tabActive: t }, () => this.__loadCharts())
  }
}