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
              <AiBotComponent {...this.props} ref={(c) => (this._AiBotComponent = c)} />
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

class AiBotComponent extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    if (!this.state._chatid) return null

    const dcp = _DeepChatProps(true, this.state._chatid)
    const dcp2strs = {}
    for (let k in dcp) {
      dcp2strs[k] = JSON.stringify(dcp[k])
    }

    return (
      <div className="aibot-chat">
        <deep-chat
          {...dcp2strs}
          style={{
            height: 500,
            width: 780,
            backgroundColor: '#fff',
            border: '0 none',
            fontSize: '14px',
            fontFamily: "Roboto, 'Hiragina Sans GB', 'San Francisco', 'Helvetica Neue', Helvetica, Arial, PingFangSC-Light, 'Noto Sans SC', 'Microsoft YaHei UI', 'Microsoft YaHei', sans-serif",
          }}
          history={JSON.stringify(this.state._history)}
        />
      </div>
    )
  }

  componentDidMount() {
    $.get('/aibot/chat-init', (res) => {
      const data = res.data || {}
      this.setState({ _history: data.history || [], _chatid: data.chatid })
    })
  }
}

// https://deepchat.dev/docs/connect
const _DeepChatProps = function (stream, chatid) {
  return {
    connect: {
      'url': `${rb.baseUrl}/aibot/${stream ? 'chat-stream' : 'chat'}`,
      'method': 'POST',
      'stream': !!stream,
      'headers': { 'chatid': chatid },
      'additionalBodyProps': {},
    },
    introMessage: {
      'text': $L('你好，我是 REBUILD AI 助手，有什么问题都可以问我哦'),
    },
    messageStyles: {
      'default': {
        'shared': {
          'bubble': { 'borderRadius': '4px', 'padding': '6px 10px', 'maxWidth': '100%' },
          outerContainer: {},
          interContainer: {},
        },
        'ai': { bubble: { 'backgroundColor': '#eee' } },
        'user': { bubble: { 'backgroundColor': '#4285f4' } },
      },
    },
    avatars: {
      'ai': { 'src': `${rb.baseUrl}/assets/img/icon-192x192.png` },
      'user': { 'src': `${rb.baseUrl}/account/user-avatar` },
      'styles': {},
    },
    textInput: {
      'styles': {
        'container': {
          'margin': '0',
          'border': 'unset',
          'width': '100%',
          'borderRadius': '0',
          'borderTop': '1px solid #e3e3e3',
        },
        'text': {
          'paddingTop': '10px',
          'paddingBottom': '10px',
          'paddingLeft': '16px',
          'paddingRight': '36px',
          'lineHeight': '1.4',
        },
      },
      'placeholder': { 'text': $L('输入你的问题 ...') },
    },
    submitButtonStyles: {
      'submit': {
        'container': {
          'default': {
            'marginBottom': '-4px',
            'marginRight': '7px',
          },
        },
      },
    },
  }
}
