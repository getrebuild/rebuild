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
    this.state = { hide: true }
  }

  render() {
    return (
      <div className={`aibot modal ${this.state.hide ? '' : 'show'}`} ref={(c) => (this._$modal = c)} aria-modal="true" tabIndex="-1">
        <div className="modal-dialog">
          <div className="modal-content">
            <div className="modal-header">
              <i className="icon mdi mdi-shimmer" />
              <h3 className="modal-title">{$L('AI 助手')} (Beta)</h3>
              <button className="close" type="button" onClick={() => this.openChatSidebar()} title={$L('对话列表')}>
                <span className="mdi mdi-segment" />
              </button>
              <button className="close hide2" type="button" onClick={() => this.hide()} title={`${$L('关闭')} (Esc)`}>
                <span className="mdi mdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <Chat
                chatid={this.props.chatid}
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
    setTimeout(() => this.show(), 50)

    if (this.props.draggable) {
      let pos = $storage.get('__LastChatModalPos')
      if (pos) {
        pos = pos.split(',').map((v) => parseInt(v))
        $(this._$modal).css({
          left: Math.max(0, pos[0]),
          top: Math.max(0, pos[1]),
          right: 'unset',
          bottom: 'unset',
        })
      }

      $(this._$modal).draggable({
        handle: '.modal-header',
        containment: document.body,
        start: function () {
          $(this).css({
            right: 'unset',
            bottom: 'unset',
          })
        },
        stop: function (event, ui) {
          const left = ui.position.left
          const top = ui.position.top
          $storage.set('__LastChatModalPos', left + ',' + top)
        },
      })

      $(document).on('keydown.aibot-hide', null, 'esc', () => this.hide())
    }
  }

  openChatSidebar() {
    this._Chat.toggleSidebar()
  }

  hide() {
    this.setState({ hide: true })
  }

  show() {
    this.setState({ hide: false })
  }

  // --

  static init(props, toggleShow) {
    if (window._AiBot) {
      if (toggleShow) {
        if (window._AiBot.state.hide) window._AiBot.show()
        else window._AiBot.hide()
      } else {
        window._AiBot.show()
      }
    } else {
      renderRbcomp(<AiBot {...props} draggable />, function () {
        window._AiBot = this
      })
    }
  }
}
