/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

class Chat extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      ...props,
      messages: [],
    }
  }

  render() {
    return (
      <RF>
        <div className="chat">
          <ChatMessages _Chat={this} ref={(c) => (this._ChatMessages = c)} />
          <ChatInput _Chat={this} ref={(c) => (this._ChatInput = c)} />
        </div>
        <ChatSidebar _Chat={this} ref={(c) => (this._ChatSidebar = c)} />
      </RF>
    )
  }

  componentDidMount() {
    // $('.chat-messages').perfectScrollbar()
    this.initChat(this.state.chatid)
  }

  componentDidUpdate(props, prevState) {
    if (this.state.chatid !== prevState.chatid) {
      typeof this.props.onChatidChanged === 'function' && this.props.onChatidChanged(this.state.chatid)
    }
  }

  initChat(chatid) {
    this.setState({ chatid: chatid || null })
    this._ChatMessages.setMessages([])
    this._ChatInput.reset()

    $.get(`/aibot/post/chat-init?chatid=${chatid || ''}`, (res) => {
      const _data = res.data || {}
      if (_data._chatid) this.setState({ chatid: _data._chatid })
      this._ChatMessages.setMessages(_data.messages || [])
    })
  }

  toggleSidebar() {
    this._ChatSidebar.toggleShow()
  }

  send(data) {
    this._ChatMessages.appendMessage(data)
    // FIXME 不延迟会覆盖?
    setTimeout(() => {
      this._ChatMessages.appendMessage({
        role: 'assistant',
        sendResp: (cb) => {
          $.post(`/aibot/post/chat?chatid=${this.state.chatid || ''}&noload`, JSON.stringify(data), (res) => {
            if (res._chatid) this.setState({ chatid: res._chatid })
            typeof cb === 'function' && cb({ ...res })
          })
        },
      })
    }, 20)
  }

  sendStream(data, onDone) {
    this._ChatMessages.appendMessage(data)
    // FIXME 不延迟会覆盖
    setTimeout(() => {
      this._ChatMessages.appendMessage({
        role: 'assistant',
        sendResp: (onChunk) => {
          fetchStream(`${rb.baseUrl}/aibot/post/chat-stream?chatid=${this.state.chatid || ''}`, data, onChunk, onDone)
        },
      })
    }, 20)
  }
}

class ChatInput extends React.Component {
  constructor(props) {
    super(props)
    this.state = { postState: 0, attach: [{ name: '记录:123' }] }
  }

  render() {
    return (
      <div className="chat-input-container">
        <div className={`chat-input ${this.state.active && 'active'}`}>
          <div className="chat-input-input">
            <div className="chat-input-attach">
              <ul className="m-0 list-unstyled">
                {this.state.attach.map((item, idx) => {
                  return (
                    <li key={idx}>
                      <Attach {...item} />
                    </li>
                  )
                })}
              </ul>
            </div>
            <textarea
              rows="2"
              value={this.state.content}
              onInput={(e) => this.setState({ content: e.target.value })}
              onKeyDown={(e) => {
                if (e.keyCode === 13 && !e.shiftKey) {
                  $stopEvent(e, true)
                  this.hanldeSend()
                }
              }}
              onBlur={() => this.setState({ active: false })}
              onFocus={() => this.setState({ active: true })}
            />
          </div>
          <div className="chat-input-action">
            <button type="button" className="btn btn-sm" data-toggle="dropdown" disabled={this.state.postState !== 0}>
              <i className="mdi mdi-attachment-plus" />
            </button>
            <div className="dropdown-menu dropdown-menu-right">
              <a className="dropdown-item" onClick={() => this.attachRecord()}>
                {$L('选择记录')}
              </a>
              <a className="dropdown-item" onClick={() => this.attachFile()}>
                {$L('选择文件')}
              </a>
            </div>
            <button type="button" className="btn btn-sm ml-1" onClick={() => this.hanldeSend()} disabled={this.state.postState !== 0}>
              <i className="mdi mdi-arrow-up" />
            </button>
          </div>
        </div>
      </div>
    )
  }

  hanldeSend() {
    if ($empty(this.state.content)) return
    const data = {
      role: 'user',
      content: this.state.content,
      attach: this.state.attach,
    }
    this.props._Chat &&
      this.props._Chat.sendStream(data, () => {
        this.setState({ postState: 0 })
      })

    this.reset()
  }

  reset() {
    this.setState({ content: '', attach: [], postState: 1 })
  }

  attachRecord() {}
  attachFile() {}
}

class ChatMessages extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      messages: [],
    }
  }

  render() {
    return (
      <div className="chat-messages">
        {this.state.messages.map((item, idx) => {
          return <ChatMessage {...item} key={idx} _ChatMessages={this} />
        })}
      </div>
    )
  }

  appendMessage(data) {
    this.setMessages([...this.state.messages, data])
  }

  setMessages(messages) {
    this.setState({ messages: messages }, () => scrollToBottom())
  }
}

class ChatMessage extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, waitResp: !!props.sendResp }
  }

  componentDidMount() {
    const sendResp = this.props.sendResp
    sendResp &&
      sendResp((data) => {
        data = data || {}
        if (data._chatid) {
          this.props._ChatMessages.props._Chat.setState({ chatid: data._chatid })
          return
        }

        if (data.error) {
          data.content = `<span class="text-danger">${data.error}</span>`
        }

        if (data.content) {
          data.content = (this.state.content || '') + data.content
          this.setState({ ...data, waitResp: false })
        }
      })
  }

  componentDidUpdate(props, prevState) {
    if (prevState.content !== this.state.content) scrollToBottom()
  }

  render() {
    let c = null
    if (this.props.role === 'user') c = this.renderUser()
    else if (this.props.role === 'assistant' || this.props.role === 'ai') c = this.renderAi()
    else c = this.renderError()

    return <div className="chat-message">{c}</div>
  }

  renderUser() {
    return (
      <div className="msg-user">
        <div className="msg-content">{this._renderText()}</div>
      </div>
    )
  }

  renderAi() {
    return (
      <div className="msg-ai">
        <div className="avatar">
          <img src={`${rb.baseUrl}/assets/img/icon-192x192.png`} alt="AI" />
        </div>
        <div className="msg-content">{this.state.waitResp ? <div className="wait-resp">{$L('思考中...')}</div> : this._renderText()}</div>
      </div>
    )
  }

  renderError() {
    return (
      <div className="msg-error">
        <div className="msg-content">{this.state.error ? this.state.error : JSON.stringify(this.state)}</div>
      </div>
    )
  }

  _renderText() {
    let md = this.state.content
    return (
      <div className="msg-text">
        <span className="mdedit-content" dangerouslySetInnerHTML={{ __html: SimpleMDE.prototype.markdown(md) }}></span>
      </div>
    )
  }
}

function scrollToBottom() {
  $setTimeout(
    () => {
      const el = document.querySelector('.chat-messages')
      el && el.scrollTo(0, el.scrollHeight)
    },
    40,
    'scrollToBottom'
  )
}

function fetchStream(url, data, onChunk, onDone) {
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  })
    .then((response) => {
      const reader = response.body.getReader()
      function readChunk() {
        return reader.read().then(({ done, value }) => {
          if (done) {
            typeof onDone === 'function' && onDone(null, true)
            return
          }

          buffer += decoder.decode(value, { stream: true })
          const parts = buffer.split('\n\n')
          buffer = parts.pop()

          parts.forEach((part) => {
            const lines = part.split('\n')
            lines.forEach((line) => {
              if (line.startsWith('data:')) {
                const c = line.slice(5).trim()
                typeof onChunk === 'function' && onChunk(JSON.parse(c))
              }
            })
          })
          return readChunk()
        })
      }

      return readChunk()
    })
    .catch((err) => {
      console.error('Error on stream :', err)
      typeof cb === 'function' && onChunk({ error: err })
    })
}

class Attach extends React.Component {
  render() {
    return <a>{this.props.name}</a>
  }
}

// ~~

class ChatSidebar extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, list: [] }
  }

  componentDidMount() {
    this.setState({
      list: [
        {
          name: '给我表格',
          chatid: 'chat-4041e892fbb6443994a39bce619700c4',
        },
        {
          name: '123',
          chatid: 'chat-c6417ff74f92436bbb1068549c7ec863',
        },
      ],
    })
  }

  render() {
    return (
      <div className={`chat-sidebar ${this.state.show && 'show'}`}>
        <div className="chat-new">
          <a
            className="btn"
            onClick={() => {
              this.props._Chat.initChat()
              this.toggleShow(true)
              this.setState({ current: null })
            }}>
            <i className="mdi mdi-chat-plus-outline mr-1 icon" />
            {$L('新对话')}
          </a>
        </div>
        <ul className="chat-list list-unstyled">
          {this.state.list.map((item) => {
            return (
              <li className={this.state.current === item.chatid ? 'active' : ''}>
                <div
                  onClick={() => {
                    this.props._Chat.initChat(item.chatid)
                    this.toggleShow(true)
                    this.setState({ current: item.chatid })
                  }}>
                  {item.name}
                </div>
                <span className="dropdown" data-toggle="dropdown">
                  <a>
                    <i className="icon zmdi zmdi-more fs-18" />
                  </a>
                  <div className="dropdown-menu dropdown-menu-right">
                    <a className="dropdown-item">{$L('删除')}</a>
                    <a className="dropdown-item">{$L('重命名')}</a>
                  </div>
                </span>
              </li>
            )
          })}
        </ul>
      </div>
    )
  }

  toggleShow(forceHide) {
    this.setState({ show: forceHide === true ? false : !this.state.show })
  }
}
