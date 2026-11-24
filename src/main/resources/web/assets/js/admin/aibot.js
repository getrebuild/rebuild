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
  } else if ('AibotReplyStyle' === name) {
    return (
      <select className="form-control form-control-sm">
        <option value="">{$L('默认')}</option>
        <option value="concise">{$L('简洁')}</option>
        <option value="detailed">{$L('详细')}</option>
        <option value="professional">{$L('专业')}</option>
        <option value="friendly">{$L('亲切')}</option>
      </select>
    )
  } else if ('AibotDefaultDisabled' === name) {
    return (
      <select className="form-control form-control-sm">
        <option value="true">{$L('是')}</option>
        <option value="false">{$L('否')}</option>
      </select>
    )
  } else if ('AibotReasoningEffort' === name) {
    return (
      <select className="form-control form-control-sm">
        <option value="">{$L('默认')}</option>
        <option value="low">{$L('低')}</option>
        <option value="medium">{$L('中')}</option>
        <option value="high">{$L('高')}</option>
      </select>
    )
  } else if ('AibotBaseDefModel' === name) {
    setTimeout(() => {
      let option = {
        options: __MODELS,
        onSelect: (v) => {
          // eslint-disable-next-line no-undef
          changeValue({ target: { value: v, name: 'AibotBaseDefModel' } })
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

        const payload = {}
        if (baseUrl) payload.baseUrl = baseUrl
        if (apiKey && apiKey !== origKey) payload.apiKey = apiKey

        $.post(`./aibot/models`, JSON.stringify(payload), (res) => {
          if (res.error_code === 0 && res.data && res.data.length) {
            option.options = res.data.map((m) => m.id)
          } else {
            option.options = [...__MODELS]
          }
        }).always(() => (fetching = false))
      }

      $('input[name="AibotBaseDefModel"]').on('focus', fetchModels)
    }, 500)
  }
}

// ~~ MCP

const _renderMcpConfig = function () {
  const $mcp = $('.J_mcpConfig')
  const code = {
    mcpServers: {
      rebuild: {
        url: $mcp.data('url') || '',
        headers: {
          Authorization: 'Bearer <' + $L('个人秘钥') + '>',
        },
        disabled: false,
      },
    },
  }

  renderRbcomp(<CodeViewport code={code} type="json" />, $mcp[0])
}

$(document).ready(() => {
  if ($('.J_mcpConfig').length) _renderMcpConfig()

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
