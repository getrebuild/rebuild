/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global Chat */

// eslint-disable-next-line no-unused-vars
class AiBot extends React.Component {
  constructor(props) {
    super(props)
    this._isShown = false
    const savedDock = $storage.get('__AiBotDockMode')
    this.state = { dockMode: !savedDock ? true : savedDock === 'true', chatid: props.chatid, agent: null }
  }

  render() {
    return (
      <div className="aibot modal" ref={(c) => (this._$modal = c)} aria-modal="true" tabIndex="-1">
        <div className="modal-dialog">
          <div className="modal-content">
            <div className="modal-header">
              <i className={`icon mdi ${(this.state.agent && this.state.agent.icon) || 'mdi-shimmer'} ai-color`} />
              <h3 className="modal-title">{(this.state.agent && this.state.agent.name) || rb._aibotName || $L('Rex AI 助手')}</h3>
              <button
                className="close fs-19"
                type="button"
                onClick={() => {
                  window.open(`${rb.baseUrl}/aibot/chat#chatid=${this.state.chatid || ''}`, '_blank')
                  this.hide()
                }}
                title={$L('独立窗口')}>
                <span className="mdi mdi-open-in-new down-1" />
              </button>
              <button
                className="close"
                type="button"
                onClick={() => {
                  this._Chat.initChat()
                  $storage.remove('__AiBotLastChatId')
                }}
                title={$L('新会话')}>
                <span className="mdi mdi-chat-plus-outline" />
              </button>
              <button className="close" type="button" onClick={() => this.openChatSidebar()} title={$L('会话列表')}>
                <span className="mdi mdi-segment" />
              </button>
              <button className="close dock-return" type="button" onClick={() => this.setDockMode()} title={$L('停靠侧栏')}>
                <span className="mdi mdi-dock-right" />
              </button>
              <button className="close hide2" type="button" onClick={() => this.hide()} title={`${$L('关闭')} (Esc)`}>
                <span className="mdi mdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <Chat
                chatid={this.props.chatid}
                preset={this.props.preset}
                sendMode={this.props.sendMode}
                agentId={this.props.agentId}
                onChatidChanged={(id) => {
                  this.setState({ chatid: id })
                  typeof this.props.onChatidChanged === 'function' && this.props.onChatidChanged(id)
                }}
                onAgentChanged={(agent) => this.setState({ agent })}
                ref={(c) => (this._Chat = c)}
              />
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    const $modal = $(this._$modal)
    $modal
      .modal({ show: false, backdrop: false, keyboard: false })
      .on('shown.bs.modal', () => {
        this._isShown = true
      })
      .on('hidden.bs.modal', () => {
        this._isShown = false
      })

    setTimeout(() => this.show(), 50)

    $modalDraggable(this._$modal, {
      containment: false,
      keepPositionKey: '__AiBotLastChatModalPos',
    })

    $modal.find('.modal-header').on('mousedown', (e) => {
      if ($(e.target).closest('.close').length) return
      if (!this.state.dockMode) return

      const startX = e.clientX
      const startY = e.clientY
      const $dialog = $modal.find('.modal-dialog')
      let dockOffset = null
      let baseOffset = null

      const onMove = (ev) => {
        const dx = ev.clientX - startX
        const dy = ev.clientY - startY
        if (Math.abs(dx) < 5 && Math.abs(dy) < 5 && !dockOffset) return

        if (!dockOffset) {
          dockOffset = $dialog.offset()
          $modal.removeClass('aibot-dock')
          $dialog.css({ position: 'relative', right: 'unset', bottom: 'unset', left: '', top: '' })
          baseOffset = $dialog.offset()
          this.setState({ dockMode: false })
          $storage.set('__AiBotDockMode', 'false')
          $dialog.draggable('enable')
          $('iframe').css('pointer-events', 'none')
        }

        $dialog.css({
          left: dockOffset.left - baseOffset.left + dx,
          top: dockOffset.top - baseOffset.top + dy,
        })
      }

      const onUp = () => {
        $(document).off('mousemove', onMove).off('mouseup', onUp)
        $('iframe').css('pointer-events', '')
        if (dockOffset) {
          const pos = $dialog.position()
          $storage.set('__AiBotLastChatModalPos', pos.left + ',' + pos.top)
        }
      }

      $(document).on('mousemove', onMove).on('mouseup', onUp)
    })

    $(document).on('keydown.aibot-hide', null, 'esc', (e) => {
      if (e.isDefaultPrevented()) return
      this.hide()
    })

    if (this.state.dockMode) {
      $modal.addClass('aibot-dock')
      const $dialog = $modal.find('.modal-dialog')
      $dialog.draggable('disable')
      $dialog.css({ left: '', top: '', right: '', bottom: '' })
    }
  }

  openChatSidebar() {
    this._Chat.toggleSidebar()
  }

  setDockMode() {
    if (this.state.dockMode) return
    this.setState({ dockMode: true }, () => {
      $storage.set('__AiBotDockMode', 'true')
      const $modal = $(this._$modal).addClass('aibot-dock')
      const $dialog = $modal.find('.modal-dialog')
      $dialog.draggable('disable')
      $dialog.css({ left: '', top: '', right: '', bottom: '' })
    })
  }

  hide() {
    $(this._$modal).modal('hide')
  }

  show() {
    if (this.state.dockMode) {
      var $dialog = $(this._$modal).find('.modal-dialog')
      $dialog.css({ left: '', top: '', right: '', bottom: '' })
    }
    $(this._$modal).modal('show')
  }

  // --

  static init(props, toggleShow) {
    if (window.top !== window.self) {
      try {
        if (parent.AiBot && parent.AiBot !== AiBot) {
          parent.AiBot.init(props, toggleShow)
          return
        }
      } catch (err) {
        // Ignored
      }
    }

    const preset = props && props.preset
    if (window._AiBot) {
      if (toggleShow) {
        if (!window._AiBot._isShown) window._AiBot.show()
        else window._AiBot.hide()
      } else {
        window._AiBot.show()
      }
      if (preset) {
        window._AiBot._Chat._preset = preset
        window._AiBot._Chat.initChat(null, props.agentId)
      }
    } else {
      var aiBotProps = { ...props }
      if (preset) aiBotProps.preset = preset
      renderRbcomp(<AiBot {...aiBotProps} />, function () {
        window._AiBot = this
      })
    }
  }
}

// eslint-disable-next-line no-unused-vars
class AiCreateButton extends React.Component {
  render() {
    return (
      <a role="button" tabIndex="0" onClick={(e) => this._handleClick(e)} className="ai-create-btn hover-opacity">
        <i className="mdi mdi-shimmer mr-1 fs-14" />
        {$L('用 Rex AI 创建')}
      </a>
    )
  }

  _handleClick(e) {
    $stopEvent(e, true)
    AiCreateButton.openAibot(this.props.presetContent, this.props.agentId)
  }

  // @see AiBot#init
  static openAibot(presetContent, agentId = null) {
    // 创建类指令依赖默认 agent 的管理工具，未指定时不沿用上次选择的 agent
    AiBot.init({ preset: { content: presetContent, autoSend: true }, agentId })
  }
}
