/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

class AiBot extends React.Component {
  constructor(props) {
    super(props)
    this.state = { hide: true }
  }

  render() {
    return (
      <div className={`aibot modal ${this.state.hide ? '' : 'show'}`}>
        <div className="modal-dialog">
          <div className="modal-content">
            <div className="modal-header">
              <i className="icon mdi mdi-shimmer" />
              <h3 className="modal-title">{$L('AI 助手')} (Beta)</h3>
              <button className="close" type="button" onClick={() => this.openChatSidebar()} title={$L('对话列表')}>
                <span className="mdi mdi-segment" />
              </button>
              <button className="close hide2" type="button" onClick={() => this.hide()} title={$L('关闭')}>
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
    setTimeout(() => this.show(), 100)
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
      renderRbcomp(<AiBot {...props} />, function () {
        window._AiBot = this
      })
    }
  }
}
