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
      if (res.error_code === 0) {
        const d = res.data || {}
        if (d._chatid) {
          this.setState({ chatid: d._chatid })
          this._ChatSidebar.setState({ current: d._chatid })
        }
        this._ChatMessages.setMessages(d.messages || [])
      } else {
        this._ChatMessages.setMessages([{ error: res.error_msg }])
      }
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
              placeholder={$L('输入问题')}
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
              <a className="dropdown-item hide" onClick={() => this.attachFile()}>
                {$L('选择文件')}
              </a>
              <a className="dropdown-item" onClick={() => this.attachRecord()}>
                {$L('选择当前页数据')}
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
    this.setState({ content: '', attach: [], postState: 0 })
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
    this.setState({ messages: messages }, () => {
      setTimeout(scrollToBottom, 100)
    })
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
          const _Chat = this.props._ChatMessages.props._Chat
          _Chat.setState({ chatid: data._chatid })
          _Chat._ChatSidebar.setState({ current: data._chatid })
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
    else if (this.props.role === 'system') c = this.renderSystem()
    else c = this.renderError()

    return <div className="chat-message">{c}</div>
  }

  renderUser() {
    return (
      <div className="msg-user">
        <div className="msg-content">{this.renderContent()}</div>
      </div>
    )
  }

  renderAi() {
    return (
      <div className="msg-ai">
        <div className="avatar">
          <img src={`${rb.baseUrl}/assets/img/icon-192x192.png`} alt="AI" />
        </div>
        <div className="msg-content">{this.state.waitResp ? <div className="wait-resp">{$L('思考中...')}</div> : this.renderContent()}</div>
      </div>
    )
  }

  renderSystem() {
    // TODO 不渲染
  }

  renderError() {
    return (
      <div className="msg-error">
        <div className="msg-content">{this.state.error || 'UNKNOW ERROR'}</div>
      </div>
    )
  }

  renderContent() {
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
    this._loadChatList()
  }

  componentDidUpdate(props, prevState) {
    if (prevState.current !== this.state.current) {
      $storage.set('__LastChatId', this.state.current)
    }
  }

  _loadChatList() {
    $.get('/aibot/post/chat-list', (res) => {
      const data = res.data || []
      this.setState({ list: data })

      if (this.state.current) {
        const delIf = data.find((x) => x.chatid === this.state.current)
        if (!delIf) {
          this.props._Chat.initChat()
          this.setState({ current: null })
        }
      }
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
              this.setState({ current: null })
              this.toggleShow(true)
            }}>
            <i className="mdi mdi-chat-plus-outline mr-1 icon" />
            {$L('新对话')}
          </a>
        </div>
        <ul className="chat-list list-unstyled">
          {this.state.list.map((item) => {
            return (
              <li key={item.chatid} className={this.state.current === item.chatid ? 'active' : ''}>
                <div
                  className="text-ellipsis"
                  title={item.subject}
                  onClick={() => {
                    this.props._Chat.initChat(item.chatid)
                    this.toggleShow(true)
                    this.setState({ current: item.chatid })
                  }}>
                  {item.subject}
                </div>
                <span>
                  <a data-toggle="dropdown">
                    <i className="icon zmdi zmdi-more fs-18" />
                  </a>
                  <div className="dropdown-menu dropdown-menu-right">
                    <a className="dropdown-item" onClick={() => this.handleDelete(item)}>
                      {$L('删除')}
                    </a>
                    <a className="dropdown-item hide" onClick={() => this.handleRename(item)}>
                      {$L('重命名')}
                    </a>
                    <a className="dropdown-item" href={`${rb.baseUrl}/aibot/chat#chatid=${item.chatid}`} target="_blank">
                      {$L('新窗口打开')}
                    </a>
                  </div>
                </span>
              </li>
            )
          })}
        </ul>
      </div>
    )
  }

  handleDelete(item) {
    $.post('/aibot/post/chat-delete?chatid=' + item.chatid, () => this._loadChatList())
  }

  handleRename(item) {
    console.log('TODO', item)
  }

  toggleShow(forceHide) {
    this.setState({ show: forceHide === true ? false : !this.state.show }, () => {
      this.state.show && this._loadChatList()
    })
  }
}
