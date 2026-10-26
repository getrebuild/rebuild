/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const __MODELS = ['qwen3.8-max', 'glm-5.2', 'deepseek-v4-flash', 'gpt-5']

// eslint-disable-next-line no-undef, react/display-name
useEditComp = function (name) {
  if ('AibotBasePrompt' === name) {
    return <textarea className="form-control form-control-sm row2x" maxLength="2000" />
  } else if ('AibotSuggestQuestions' === name || 'AibotWelcome' === name) {
    return <textarea className="form-control form-control-sm row2x" maxLength="2000" />
  } else if ('AibotBaseDefModel' === name) {
    setTimeout(() => {
      let __modelContextMap = {}
      let option = {
        options: __MODELS,
        onSelect: (v) => {
          // eslint-disable-next-line no-undef
          changeValue({ target: { value: v, name: 'AibotBaseDefModel' } })
          if (__modelContextMap[v]) {
            // eslint-disable-next-line no-undef
            changeValue({ target: { value: __modelContextMap[v].toString(), name: 'AibotContextCompressThreshold' } })
            $('input[name=AibotContextCompressThreshold]').val(__modelContextMap[v])
          }
        },
      }
      $autoComplete($('input[name="AibotBaseDefModel"]'), null, option)

      // 聚焦时按页面当前参数拉取可用模型
      let fetching = false
      const fetchModels = () => {
        if (fetching) return
        fetching = true

        const baseUrl = ($('input[name="AibotDSUrl"]').val() || '').trim()
        const apiKey = ($('input[name="AibotDSSecret"]').val() || '').trim()
        const origKey = ($('td[data-id="AibotDSSecret"]').data('value') || '').trim()

        const params = []
        if (baseUrl) params.push(`baseUrl=${encodeURIComponent(baseUrl)}`)
        if (apiKey && apiKey !== origKey) params.push(`apiKey=${encodeURIComponent(apiKey)}`)
        const qs = params.length > 0 ? `?${params.join('&')}` : ''

        $.get(`./aibot/models${qs}`, (res) => {
          if (res.error_code === 0 && res.data && res.data.length) {
            option.options = res.data.map((m) => m.id)
            __modelContextMap = {}
            res.data.forEach((m) => {
              if (m.contextWindow) __modelContextMap[m.id] = m.contextWindow
            })
          } else {
            option.options = [...__MODELS]
          }
        }).always(() => (fetching = false))
      }

      $('input[name="AibotBaseDefModel"]').on('focus', fetchModels)
    }, 500)
  }
}

// eslint-disable-next-line no-undef
class AiBotUserRank extends ChartRank {
  componentDidMount() {
    const users = (this.props.users || []).slice(0, 10)
    if (users.length === 0) {
      this.renderError($L('暂无数据'))
      return
    }
    this.renderChart({
      xAxis: users.map((u) => u[1]),
      yyyAxis: [{ data: users.map((u) => u[2]), name: $L('万/Token') }],
      _renderOption: { showNumerical: true, dataFlags: [] },
    })
  }

  render() {
    return (
      <div ref={(c) => (this._$body = c)} className="chart-body" style={{ minHeight: 320 }}>
        {this.state.chartdata || <div className="text-muted text-center pt-4">{$L('加载中...')}</div>}
      </div>
    )
  }

  _initRankScroll() {}

  _renderChartBarBefore(option, data) {
    option.grid.top = 8
    option.grid.bottom = 8
    super._renderChartBarBefore(option, data)
    option.grid.right = 10
    return option
  }
}

// ~~ 所有会话
class AiBotChatsModal extends RbModal {
  constructor(props) {
    super(props)
    this.state = { ...props, total: -1, data: null, user: '', showMore: false }
  }

  componentDidMount() {
    super.componentDidMount()
    this._loadNext(true)
  }

  renderContent() {
    let body
    if (this.state.data === null) {
      body = (
        <div className="rb-loading rb-loading-active">
          <RbSpinner />
        </div>
      )
    } else if (this.state.data.length === 0) {
      body = (
        <div className="list-nodata">
          <span className="zmdi zmdi-info-outline" />
          <p>{$L('暂无数据')}</p>
        </div>
      )
    } else {
      body = (
        <RF>
          <table className="table table-hover table-striped mb-0">
            <thead>
              <tr>
                <th>{$L('标题')}</th>
                <th className="text-right">{$L('Token 用量')}</th>
                <th>{$L('所属用户')}</th>
                <th>{$L('使用时间')}</th>
              </tr>
            </thead>
            <tbody>
              {this.state.data.map((item) => (
                <tr key={item.id}>
                  <td>
                    <a href={`${rb.baseUrl}/aibot/chat?frame=1#chatid=${item.id}`} title={item.subject} target="_blank" className="link">
                      {item.subject}
                    </a>
                  </td>
                  <td className="text-right">{(item.token || 0).toLocaleString()}</td>
                  <td className="text-muted">{item.userName}</td>
                  <td className="text-muted">
                    <DateShow date={item.modifiedOn} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {this.state.showMore && (
            <div className="text-center mt-4 mb-4">
              <a className="show-more-pill" onClick={() => this._loadNext()}>
                {$L('显示更多')}
              </a>
            </div>
          )}
        </RF>
      )
    }

    return (
      <div className="modal-body m-0 p-0">
        <div className="card card-table shadow-none mb-0">
          <div className="card-body">
            <div className="dataTables_wrapper container-fluid">
              <div className="row rb-datatable-header bg-eee">
                <div className="col-8">
                  <div className="dataTables_filter d-flex align-items-center">
                    <div className="input-group input-search">
                      <input
                        className="form-control"
                        type="text"
                        placeholder={$L('查询')}
                        maxLength="40"
                        onKeyDown={(e) => {
                          if (e.keyCode === 13) this._loadNext(true, e.target.value.trim())
                        }}
                      />
                      <span className="input-group-btn">
                        <button className="btn btn-secondary" type="button" onClick={(e) => this._loadNext(true, $(e.target).closest('.input-search').find('input').val().trim())}>
                          <i className="icon zmdi zmdi-search" />
                        </button>
                      </span>
                    </div>
                    <div className="ml-2" style={{ width: 227 }}>
                      <UserSelector
                        hideDepartment
                        hideRole
                        hideTeam
                        multiple={false}
                        onSelectItem={(s, isRemove) => this._selectUser(isRemove || !s ? '' : s.id)}
                        onClearSelection={() => this._selectUser('')}
                        title={$L('选择所属用户')}
                      />
                    </div>
                  </div>
                </div>
                <div className="col-4">
                  <div className="dataTables_oper pt-2 fs-13">{this.state.total > -1 && <span className="text-muted">{$L('共 %s 条', this.state.total)}</span>}</div>
                </div>
              </div>
              <div className="data-list">{body}</div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  _selectUser(id) {
    this.setState({ user: id || '' }, () => this._loadNext(true))
  }

  _loadNext(reset, q) {
    if (this.state._search) return
    this.setState({ _search: true })

    this.__pageNo = reset ? 1 : (this.__pageNo || 0) + 1
    this.__q = q !== undefined ? q : this.__q

    $.get('./aibot/chat-list', { pageNo: this.__pageNo, user: this.state.user || '', q: this.__q || '' }, (res) => {
      const d = res.data || {}
      const _data = d.data || []
      this.setState({
        _search: false,
        data: reset ? _data : (this.state.data || []).concat(_data),
        showMore: _data.length >= 20,
        total: d.total > -1 ? d.total : this.state.total,
      })
    })
  }
}

$(document).ready(() => {
  $.get('./aibot/stats', (res) => {
    const data = res.data || {}
    let $el = $('.J_stats-aibot')
    $el.find('strong').text(data.aibotCount || 0)
    _renderStats(data.aibot, $el)

    $el = $('.J_stats-chats')
    $el.find('strong').text(data.chats || 0)
    _renderStats(data.aibotDailyChats, $el)

    $el = $('.J_stats-usercount')
    $el.find('strong').text(data.users || 0)
    _renderStats(data.aibotDailyUsers, $el)

    renderRbcomp(<AiBotUserRank id="aibot-rank" users={data.aibotUsers || []} />, $('.J_stats-users')[0])
  })

  $('.J_all-chats').on('click', () => {
    renderRbcomp(<AiBotChatsModal title={$L('所有会话')} width={880} disposeOnHide useWhite useScrollable />)
  })
})

// eslint-disable-next-line no-undef
postBefore = function (data) {
  if (data.__clear__) return data

  const $ds = $('td[data-id="AibotDSSecret"]')
  if (!data['AibotDSSecret'] && !$ds.data('value')) {
    RbHighbar.create($L('%s不能为空', $ds.prev().text()))
    return false
  }
  return data
}

const _renderStats = function (data, $el) {
  const xAxis = []
  const series = []
  data.forEach((item) => {
    xAxis.push(item[0])
    series.push(item[1])
  })

  const option = {
    grid: { left: 0, right: 0, top: 4, bottom: 4 },
    animation: true,
    tooltip: {
      trigger: 'axis',
      formatter: '{b} : <b>{c}</b>',
      textStyle: {
        fontSize: 12,
        lineHeight: 1.3,
        color: '#333',
      },
      axisPointer: {
        lineStyle: { color: '#ddd' },
      },
      backgroundColor: '#fff',
      extraCssText: 'border-radius:0;box-shadow:0 0 6px 0 rgba(0, 0, 0, .1), 0 8px 10px 0 rgba(170, 182, 206, .2);',
      confine: true,
      position: 'top',
    },
    textStyle: {
      fontFamily: '"Hiragina Sans GB", San Francisco, "Helvetica Neue", Helvetica, Arial, PingFangSC-Light, "WenQuanYi Micro Hei", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif',
    },
    xAxis: {
      show: false,
      type: 'category',
      data: xAxis,
    },
    yAxis: {
      show: false,
      type: 'value',
      splitLine: { show: false },
      cursor: 'default',
    },
    series: [
      {
        data: series,
        areaStyle: { opacity: 0.2 },
        itemStyle: {
          normal: {
            color: '#4285f4',
            lineStyle: { color: '#4285f4' },
          },
        },
        type: 'line',
        smooth: true,
        connectNulls: true,
      },
    ],
  }

  const c = echarts.init($el.find('span')[0])
  c.setOption(option)
}
