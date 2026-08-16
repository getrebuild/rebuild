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
    const savedDock = props.draggable ? $storage.get('__AiBotDockMode') : 'false'
    this.state = { dockMode: !savedDock ? true : savedDock === 'true' }
  }

  render() {
    const dockMode = this.state.dockMode
    return (
      <div className={`aibot modal${dockMode ? ' aibot-dock' : ''}`} ref={(c) => (this._$modal = c)} aria-modal="true" tabIndex="-1">
        <div className="modal-dialog">
          <div className="modal-content">
            <div className="modal-header">
              <i className="icon mdi mdi-shimmer" />
              <h3 className="modal-title">{rb._aibotName || $L('AI 助手')}</h3>
              <button className="close" type="button" onClick={() => this.newChat()} title={$L('新会话')}>
                <span className="mdi mdi-chat-plus-outline" />
              </button>
              <button className="close" type="button" onClick={() => this.openChatSidebar()} title={$L('会话列表')}>
                <span className="mdi mdi-segment" />
              </button>
              <button className="close dock-toggle" type="button" onClick={() => this.toggleDockMode()} title={dockMode ? $L('浮动模式') : $L('侧栏模式')}>
                <span className={`mdi ${dockMode ? 'mdi-dock-window' : 'mdi-dock-right'}`} />
              </button>
              <button className="close hide2" type="button" onClick={() => this.hide()} title={`${$L('关闭')} (Esc)`}>
                <span className="mdi mdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <Chat
                chatid={this.props.chatid}
                presetMessage={this.props.presetMessage}
                autoSend={this.props.autoSend}
                onChatidChanged={(id) => {
                  this.setState({ chatid: id })
                  typeof this.props.onChatidChanged === 'function' && this.props.onChatidChanged(id)
                }}
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

    if (this.props.draggable) {
      $modalDraggable(this._$modal, {
        containment: false,
        keepPositionKey: '__AiBotLastChatModalPos',
      })
      $(document).on('keydown.aibot-hide', null, 'esc', (e) => {
        if (e.isDefaultPrevented()) return
        if ($('.modal.show').length <= 1) this.hide()
      })

      if (this.state.dockMode) {
        const $dialog = $modal.find('.modal-dialog')
        $dialog.draggable('disable')
        $dialog.css({ left: '', top: '', right: '', bottom: '' })
      }
    }
  }

  openChatSidebar() {
    this._Chat.toggleSidebar()
  }

  toggleDockMode() {
    const dockMode = !this.state.dockMode
    this.setState({ dockMode }, () => {
      $storage.set('__AiBotDockMode', dockMode ? 'true' : 'false')
      const $dialog = $(this._$modal).find('.modal-dialog')
      if (dockMode) {
        if (this.props.draggable) $dialog.draggable('disable')
        $dialog.css({ left: '', top: '', right: '', bottom: '' })
      } else if (this.props.draggable) {
        $dialog.draggable('enable')
        const last = $storage.get('__AiBotLastChatModalPos')
        if (last) {
          const [l, t] = last.split(',').map((v) => parseInt(v))
          $dialog.css({ left: l, top: Math.max(-22, t), right: 'unset', bottom: 'unset' })
        }
      }
    })
  }

  newChat() {
    this._Chat.initChat()
  }

  hide() {
    $(this._$modal).modal('hide')
  }

  show() {
    $(this._$modal).modal('show')
  }

  // --

  static init(props, toggleShow) {
    var presetMessage = props && props.presetMessage
    var autoSend = props && props.autoSend
    if (window._AiBot) {
      if (toggleShow) {
        if (!window._AiBot._isShown) window._AiBot.show()
        else window._AiBot.hide()
      } else {
        window._AiBot.show()
      }
      // 将搜索词填入已有会话输入框并自动发送
      if (presetMessage) {
        var _Chat = window._AiBot._Chat
        if (_Chat && _Chat._ChatInput) {
          _Chat._ChatInput.setState({ content: presetMessage }, function () {
            if (autoSend && _Chat._ChatInput.state.postState === 0) {
              _Chat._ChatInput.hanldeSend()
            } else {
              _Chat._ChatInput._$textarea && _Chat._ChatInput._$textarea.focus()
            }
          })
        }
      }
    } else {
      renderRbcomp(<AiBot {...props} />, function () {
        window._AiBot = this
      })
    }
  }
}
