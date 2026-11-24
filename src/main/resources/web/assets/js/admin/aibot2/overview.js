/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// 以 / 开头的地址会由全局 ajax beforeSend 自动补 rb.baseUrl
const OV_MODELS_URL = '/admin/integration/aibot/model-list'

const ovConfigOf = (row) => (row && row.config) || {}

const ovIsDisabled = (row) => {
  const v = row ? row.isDisabled : false
  return v === true || v === 'T' || v === 't'
}

const ovIsDefault = (row) => {
  const v = ovConfigOf(row).isDefault
  return v === true || v === 'true'
}

const ovModelStatus = (row) => {
  if (ovIsDisabled(row)) return { label: $L('停用'), cls: 'badge-secondary' }
  const result = String(ovConfigOf(row).lastTestResult || '')
  if (result.includes('失败') || result.includes('不可用') || result.includes('未找到')) {
    return { label: $L('异常'), cls: 'badge-danger' }
  }
  return { label: $L('启用'), cls: 'badge-success' }
}

const ovPad2 = (n) => String(n).padStart(2, '0')

const ovFormatDate = (d) => `${d.getFullYear()}-${ovPad2(d.getMonth() + 1)}-${ovPad2(d.getDate())}`

// 词元数以万为单位展示
const ovFormatToken = (token) => {
  const n = Number(token) || 0
  return (n / 10000).toFixed(2).replace(/\.?0+$/, '') + '万'
}

const OvIconAi = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7">
    <rect x="7" y="7" width="10" height="10" rx="2" />
    <path d="M10 3v2M14 3v2M10 19v2M14 19v2M3 10h2M3 14h2M19 10h2M19 14h2" />
  </svg>
)

const ChatRows = ({ rows }) => {
  if (!rows.length) {
    return (
      <tr>
        <td colSpan="4" className="text-muted text-center py-4">
          {$L('暂无会话')}
        </td>
      </tr>
    )
  }

  return rows.map((row) => (
    <tr key={row.id}>
      <td>
        <a
          className="light-link d-inline-block text-truncate align-bottom"
          style={{ maxWidth: 280 }}
          href={`${rb.baseUrl}/aibot/chat?frame=1#chatid=${row.id}`}
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

const ModelRows = ({ rows }) => {
  if (!rows.length) {
    return (
      <tr>
        <td className="text-muted text-center py-4">{$L('暂无模型配置')}</td>
      </tr>
    )
  }

  return rows.map((row) => {
    const st = ovModelStatus(row)
    return (
      <tr key={row.id}>
        <td>
          <div className="kit-cell">
            <span className="kit-tile">
              <OvIconAi />
            </span>
            <div className="kit-text">
              <div className="kit-name">
                {row.name}
                {ovIsDefault(row) && <span className="badge badge-info font-weight-normal ml-1">{$L('默认')}</span>}
              </div>
              <div className="kit-sub text-muted">
                {(ovConfigOf(row).AibotBaseDefModel || '—') + (ovIsDisabled(row) ? ` · ${$L('已停用')}` : '')}
              </div>
            </div>
          </div>
        </td>
        <td width="80" className="text-right">
          <span className={`badge font-weight-normal ${st.cls}`}>{st.label}</span>
        </td>
      </tr>
    )
  })
}

$(document).ready(() => {
  const $modelsList = $('#modelsList')
  const statsDfd = $.get('/admin/integration/aibot/stats').then((res) => res.data || {}, () => ({}))
  const chatsDfd = $.get('/admin/integration/aibot/chat-list', { pageNo: 1 }).then((res) => res.data || {}, () => ({}))
  const modelsDfd = $modelsList.length
    ? $.get(OV_MODELS_URL, { lite: 1 }).then((res) => res.data || [], () => [])
    : $.Deferred().resolve([])

  $.when(statsDfd, chatsDfd, modelsDfd).done((stats, chats, models) => {
    const now = new Date()

    // 近 90 天词元
    const tokens = new Map(stats.aibot || [])
    const tokenBars = Array.from({ length: 90 }, (_, i) => {
      const d = new Date(now.getFullYear(), now.getMonth(), now.getDate() - (89 - i))
      const date = ovFormatDate(d)
      return { date, value: tokens.get(date) || 0 }
    })
    const maxToken = Math.max(0.1, ...tokenBars.map((item) => item.value))
    const $tokenBars = $('#tokenBars').empty()
    tokenBars.forEach((item) => {
      const height = Math.max(2, Math.round((item.value / maxToken) * 100)) + '%'
      $('<i></i>').css('height', height).attr('title', `${item.date.slice(5)} ${item.value} ${$L('万')}`).appendTo($tokenBars)
    })
    $('.J_axis-start').text(tokenBars[0].date.slice(5))
    $('.J_axis-mid').text(tokenBars[44].date.slice(5))
    $('.J_axis-end').text(tokenBars[89].date.slice(5))
    $('.J_tokens').text(stats.aibotCount || 0)

    // 用量统计
    const dailyMap = new Map(stats.aibotDailyChats || [])
    $('.J_chats').text(stats.chats || 0)
    $('.J_users').text(stats.users || 0)
    $('.J_today').text(dailyMap.get(ovFormatDate(now)) || 0)

    // 最近会话
    const chatRows = (chats && chats.data) || []
    $('.J_chat-total').text(chats && chats.total > -1 ? chats.total : chatRows.length)
    renderRbcomp(<ChatRows rows={chatRows.slice(0, 5)} />, 'chatsList')

    const last7 = Array.from({ length: 7 }, (_, i) => {
      const d = new Date(now.getFullYear(), now.getMonth(), now.getDate() - (6 - i))
      return [ovFormatDate(d), dailyMap.get(ovFormatDate(d)) || 0]
    })
    const maxDaily = Math.max(1, ...last7.map((item) => item[1]))
    const $chatBars = $('#chatBars').empty()
    last7.forEach(([date, count]) => {
      const height = Math.max(6, Math.round((count / maxDaily) * 100)) + '%'
      const $col = $('<div class="barcol"></div>').appendTo($chatBars)
      $('<div class="bar"></div>').css('height', height).attr('title', `${date.slice(5)} ${$L('%s 个会话', count)}`).appendTo($col)
      $('<span class="lb"></span>').text(date.slice(5)).appendTo($col)
    })

    // 模型状态
    if ($modelsList.length) {
      const enabled = models.filter((row) => !ovIsDisabled(row))
      $('.J_models').text(models.length)
      $('.J_models-enabled').text(enabled.length)
      renderRbcomp(<ModelRows rows={models.slice(0, 6)} />, 'modelsList')
    }
  })
})
