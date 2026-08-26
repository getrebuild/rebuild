/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// eslint-disable-next-line no-undef, react/display-name
useEditComp = function (name) {
  if ('AibotBasePrompt' === name) {
    return <textarea className="form-control form-control-sm row2x" maxLength="2000" />
  } else if ('AibotSuggestQuestions' === name || 'AibotWelcome' === name) {
    return <textarea className="form-control form-control-sm row2x" maxLength="2000" />
  } else if ('AibotBaseDefModel' === name) {
    setTimeout(() => {
      let option = {
        options: 'qwen3.8-max glm-5.2 deepseek-v4-flash gpt-5'.split(' '),
        onSelect: (v) => {
          // eslint-disable-next-line no-undef
          changeValue({ target: { value: v, name: 'AibotBaseDefModel' } })
        },
      }
      $autoComplete($('input[name="AibotBaseDefModel"]'), null, option)

      // 异步获取可用模型
      const $urlInput = $('input[name="AibotDSUrl"]')
      const $keyInput = $('input[name="AibotDSSecret"]')
      const baseUrl = ($urlInput.val() || '').trim()
      const apiKey = ($keyInput.val() || '').trim()

      const origUrl = ($('td[data-id="AibotDSUrl"]').data('value') || '').trim()
      const origKey = ($('td[data-id="AibotDSSecret"]').data('value') || '').trim()
      const params = []
      if (baseUrl && baseUrl !== origUrl) params.push(`baseUrl=${encodeURIComponent(baseUrl)}`)
      if (apiKey && apiKey !== origKey) params.push(`apiKey=${encodeURIComponent(apiKey)}`)
      const qs = params.length > 0 ? `?${params.join('&')}` : ''
      $.get(`./aibot/models${qs}`, (res) => {
        if (res.error_code === 0 && res.data && res.data.length > 0) {
          option.options = res.data.map((m) => {
            // const ctx = m.contextWindow
            // return {
            //   text: ctx ? `${m.id} (${ctx >= 1000 ? Math.round(ctx / 1000) + 'K' : ctx})` : m.id,
            //   name: m.id,
            // }
            return m.id
          })
        }
      })
    }, 500)
  }
}

$(document).ready(() => {
  $.get('./aibot/stats', (res) => {
    let $el = $('.J_stats-aibot')
    $el.find('strong').text(res.data.aibotCount || 0)
    _renderStats(res.data.aibot, $el)
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
