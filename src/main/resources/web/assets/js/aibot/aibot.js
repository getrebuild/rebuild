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
              <h3 className="modal-title">{$L('AI 助手')} (LAB)</h3>
              <a className="close fs-17 down-2" href={`${rb.baseUrl}/aibot/chat?chatid=`} target="_blank" title={$L('在新页面打开')}>
                <span className="zmdi zmdi-open-in-new" />
              </a>
              <button className="close" type="button" onClick={() => this.hide()} title={$L('关闭')}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <Chat />
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    setTimeout(() => this.show(), 100)
  }

  hide() {
    this.setState({ hide: true })
  }

  show() {
    this.setState({ hide: false })
  }

  // --

  static init(props) {
    if (window._CurrentAiBot) {
      window._CurrentAiBot.show()
    } else {
      renderRbcomp(<AiBot {...props} />, function () {
        window._CurrentAiBot = this
      })
    }
  }
}
