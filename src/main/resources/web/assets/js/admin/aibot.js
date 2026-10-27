/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global AiBotChatsModal */

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

$(document).ready(() => {
  $.get('./aibot/stats', (res) => {
    const data = res.data || {}
    let $el = $('.J_stats-aibot')
    $el.find('strong').text(data.aibotCount || 0)
    $renderStats(data.aibot, $el)

    $el = $('.J_stats-chats')
    $el.find('strong').text(data.chats || 0)
    $renderStats(data.aibotDailyChats, $el)

    $el = $('.J_stats-usercount')
    $el.find('strong').text(data.users || 0)
    $renderStats(data.aibotDailyUsers, $el)

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
