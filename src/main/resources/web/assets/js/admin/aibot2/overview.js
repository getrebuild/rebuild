/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const ovIsDisabled = (row) => {
  const v = row ? row.isDisabled : false
  return v === true || v === 'T' || v === 't'
}

const ovPad2 = (n) => String(n).padStart(2, '0')

const ovFormatDate = (d) => `${d.getFullYear()}-${ovPad2(d.getMonth() + 1)}-${ovPad2(d.getDate())}`

// 词元数以万为单位展示
const ovFormatToken = (token) => {
  const n = Number(token) || 0
  return (n / 10000).toFixed(2).replace(/\.?0+$/, '') + '万'
}

// 与模型列表一致的加载/空状态
const ovSetLoadState = ($body, hasData) => {
  $body.children('.spinner').addClass('hide')
  $body.children('.list-nodata').toggleClass('hide', !!hasData)
}

const ChatRows = ({ rows }) => {
  return rows.map((row) => (
    <tr key={row.id}>
      <td>
        <a
          className="d-inline-block text-truncate align-bottom"
          style={{ maxWidth: 280 }}
          href={`${rb.baseUrl}/aibot/chat#chatid=${row.id}`}
          target="_blank"
          rel="noreferrer"
          title={row.subject}
        >
          {row.subject || $L('（无主题）')}
        </a>
      </td>
      <td className="text-muted">{row.userName || '—'}</td>
      <td>{ovFormatToken(row.token)}</td>
      <td className="text-muted">{row.modifiedOn || '—'}</td>
    </tr>
  ))
}

const ovRenderChatChart = (chatBars) => {
  $useEchart(() => {
    const el = document.getElementById('chatBars')
    if (!el) return
    const old = echarts.getInstanceByDom(el)
    if (old) old.dispose()

    const chart = echarts.init(el)
    chart.setOption({
      animationDuration: 500,
      grid: { left: 0, right: 0, top: 6, bottom: 24 },
      tooltip: {
        trigger: 'axis',
        confine: true,
        backgroundColor: 'rgba(7, 16, 31, 0.92)',
        borderWidth: 0,
        textStyle: { color: '#fff', fontSize: 12 },
        formatter: (params) => {
          const item = chatBars[params[0].dataIndex]
          return `${item[0].slice(5)} ${$L('%s 个会话', item[1])}`
        },
      },
      xAxis: {
        type: 'category',
        data: chatBars.map((item) => item[0].slice(5)),
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: '#8f959e', fontSize: 10.5, interval: 0 },
      },
      yAxis: { type: 'value', show: false },
      series: [
        {
          type: 'bar',
          data: chatBars.map((item) => item[1]),
          barWidth: 24,
          itemStyle: {
            borderRadius: [5, 5, 2, 2],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#69b1ff' },
              { offset: 1, color: '#2f6bff' },
            ]),
          },
        },
      ],
    })

    $(window).on('resize.overview-chat-chart', () => chart.resize())
  })
}

const UsageRows = ({ rows }) => {
  return rows.map((row) => (
    <tr key={`${row.type}-${row.id}`}>
      <td>
        <a className="agent-card-head mb-0" href={row.href} title={row.name}>
          <span className={`agent-icon-tile ${row.type === 'minapp' ? 'tile-minapp' : ''}`}>
            <i className={`icon mdi ${row.icon || 'mdi-shimmer'} ai-color fs-16`} />
          </span>
          <span className="text-truncate">
            <span className="d-block text-truncate">{row.name}</span>
            <span className="d-block fs-12 text-muted">
              {row.type === 'agent' ? $L('智能体') : $L('微应用')}
              {ovIsDisabled(row) ? ` · ${$L('已停用')}` : ''}
            </span>
          </span>
        </a>
      </td>
      <td width="100" className="text-right text-muted">
        {row.type === 'agent' ? $L('%s 次会话', row.count) : $L('%s 次访问', row.count)}
      </td>
    </tr>
  ))
}

$(document).ready(() => {
  const $usageList = $('#usageList')
  const hasSmartApps = $usageList.length || $('.J_app-usage').length
  const getList = (url, data) => $.get(url, data).then((res) => res.data || [], () => [])
  const statsDfd = $.get('/admin/integration/aibot/stats').then((res) => res.data || {}, () => ({}))
  const chatsDfd = $.get('/admin/integration/aibot/chat-list', { pageNo: 1 }).then((res) => res.data || {}, () => ({}))
  const agentsDfd = hasSmartApps || $('.J_cfg-agents').length
    ? getList('/admin/integration/aibot/agent-list')
    : $.Deferred().resolve([])
  const minappsDfd = hasSmartApps || $('.J_cfg-minapps').length
    ? getList('/admin/integration/aibot/minapp-list')
    : $.Deferred().resolve([])
  const modelsDfd = $('.J_cfg-models').length
    ? getList('/admin/integration/aibot/model-list', { lite: 1 })
    : $.Deferred().resolve([])
  const kbsDfd = $('.J_cfg-kb').length
    ? getList('/admin/integration/aibot/kb-list', { lite: 1 })
    : $.Deferred().resolve([])
  const skillsDfd = $('.J_cfg-skills').length
    ? getList('/admin/integration/aibot/skill-list')
    : $.Deferred().resolve([])
  const toolsDfd = $('.J_cfg-tools').length
    ? getList('/admin/integration/aibot/tool-list')
    : $.Deferred().resolve([])

  $.when(statsDfd, chatsDfd, agentsDfd, minappsDfd, modelsDfd, kbsDfd, skillsDfd, toolsDfd).done((stats, chats, agents, minapps, models, kbs, skills, tools) => {
    const now = new Date()

    $('.J_tokens').text(stats.aibotCount || 0)

    // 配置概览
    $('.J_cfg-models').text(models.length)
    $('.J_cfg-agents').text(agents.length)
    $('.J_cfg-minapps').text(minapps.length)
    $('.J_cfg-kb').text(kbs.length)
    $('.J_cfg-skills').text(skills.length)
    $('.J_cfg-tools').text(tools.length)

    // 用量统计
    const dailyMap = new Map(stats.aibotDailyChats || [])
    const dailyUsersMap = new Map(stats.aibotDailyUsers || [])
    $('.J_chats').text(stats.chats || 0)
    $('.J_users').text(stats.users || 0)
    $('.J_today').text(dailyMap.get(ovFormatDate(now)) || 0)
    $('.J_today-users').text(dailyUsersMap.get(ovFormatDate(now)) || 0)

    // 最近会话
    const chatRows = (chats && chats.data) || []
    renderRbcomp(<ChatRows rows={chatRows.slice(0, 5)} />, 'chatsList')
    ovSetLoadState($('#chatsList').closest('.card-body'), chatRows.length > 0)
    $('.J_chat-bars').toggleClass('hide', !chatRows.length)

    if (chatRows.length) {
      const last7 = Array.from({ length: 7 }, (_, i) => {
        const d = new Date(now.getFullYear(), now.getMonth(), now.getDate() - (6 - i))
        return [ovFormatDate(d), dailyMap.get(ovFormatDate(d)) || 0]
      })
      ovRenderChatChart(last7)
    }

    // 智能体、微应用近 90 天使用情况
    if (hasSmartApps) {
      const appUsage = agents.reduce((sum, row) => sum + (Number(row.chatCount) || 0), 0)
        + minapps.reduce((sum, row) => sum + (Number(row.visitCount) || 0), 0)
      $('.J_app-usage').text(appUsage)

      if ($usageList.length) {
        const usageRows = [
          ...agents.map((row) => ({
            id: row.id,
            name: row.name,
            type: 'agent',
            icon: (row.config || {}).icon,
            count: Number(row.chatCount) || 0,
            href: `${rb.baseUrl}/admin/aibot2/agent/${row.id}`,
            isDisabled: row.isDisabled,
          })),
          ...minapps.map((row) => ({
            id: row.id,
            name: row.name,
            type: 'minapp',
            icon: row.icon,
            count: Number(row.visitCount) || 0,
            href: `${rb.baseUrl}/admin/aibot2/minapp/${row.id}`,
            isDisabled: row.isDisabled,
          })),
        ]
          .filter((row) => row.count > 0)
          .sort((a, b) => b.count - a.count)
          .slice(0, 6)
        renderRbcomp(<UsageRows rows={usageRows} />, 'usageList')
        ovSetLoadState($usageList.closest('.card-body'), usageRows.length > 0)
      }
    }
  })
})
