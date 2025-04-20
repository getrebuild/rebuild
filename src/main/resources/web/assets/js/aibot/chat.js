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
        <div className="chat" ref={(c) => (this._$chat = c)}>
          <ChatMessages _Chat={this} ref={(c) => (this._ChatMessages = c)} />
          <ChatInput _Chat={this} ref={(c) => (this._ChatInput = c)} />
        </div>
        <ChatSidebar _Chat={this} ref={(c) => (this._ChatSidebar = c)} />
      </RF>
    )
  }

  componentDidMount() {
    this.initChat(this.state.chatid)

    $(this._$chat).on('click.chat-hide', (e) => {
      const $e = $(e.target)
      if ($e.hasClass('chat-sidebar') || $e.parents('.chat-sidebar')[0]);
      else {
        this.toggleSidebar(false)
      }
    })
  }

  componentDidUpdate(props, prevState) {
    if (this.state.chatid !== prevState.chatid) {
      typeof this.props.onChatidChanged === 'function' && this.props.onChatidChanged(this.state.chatid)
    }
  }

  componentWillUnmount() {
    $(this._$chat).off('click.chat-hide')
  }

  initChat(chatid) {
    this.setState({ chatid: chatid || null })
    this._ChatMessages.setMessages([])
    this._ChatInput.reset(true)

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

  toggleSidebar(showOrHide) {
    this._ChatSidebar.toggleShow(showOrHide)
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
          fetchStream(`${rb.baseUrl}/aibot/post/chat-stream?chatid=${this.state.chatid || ''}&model=&noload`, data, onChunk, onDone)
        },
      })
    }, 20)
  }
}

class ChatInput extends React.Component {
  constructor(props) {
    super(props)
    this.state = { postState: 0, attach: [] }
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
                      <Attach {...item} _ChatInput={this} />
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
              autoFocus
              ref={(c) => (this._$textarea = c)}
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
              <a className="dropdown-item" onClick={() => this.attachPageData()}>
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
    this.setState({ postState: 1 })
  }

  reset(autoFocus) {
    this.setState({ content: '', attach: [], postState: 0 }, () => {
      if (autoFocus) this._$textarea.focus()
    })
  }

  removeAttach(id) {
    const attach = this.state.attach.filter((item) => item.id !== id)
    this.setState({ attach })
  }

  attachRecord() {
    renderRbcomp(
      <DlgAttachRecord
        zIndex="1050"
        onConfirm={(v) => {
          const attach = [...this.state.attach, { record: v, id: $random('attach-', true) }]
          this.setState({ attach })
        }}
      />
    )
  }
  attachFile() {
    RbHighbar.createl('暂不支持')
  }
  attachPageData() {
    if (typeof window.attachAibotPageData === 'function') {
      window.attachAibotPageData((data) => {
        const attach = [...this.state.attach, { ...data, id: $random('attach-', true) }]
        this.setState({ attach })
      })
    } else {
      RbHighbar.createl('当前页面暂无可使用数据')
    }
  }
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
    this.state = { ...props, waitResp: props.sendResp ? 1 : 0 }
  }

  componentDidMount() {
    const sendResp = this.props.sendResp
    sendResp &&
      sendResp((data) => {
        data = data || {}
        if (data.error) {
          data.content = `<span class="text-danger">${data.error}</span>`
        }

        if (data.type === '_chatid') {
          const _Chat = this.props._ChatMessages.props._Chat
          _Chat.setState({ chatid: data.content })
          _Chat._ChatSidebar.setState({ current: data.content })
          return
        }

        if (data.content) {
          if (data.type === '_reasoning') {
            data.reasoning = (this.state.reasoning || '') + data.content
            delete data.content
            this.setState({ ...data, waitResp: 2 })
          } else {
            data.content = (this.state.content || '') + data.content
            this.setState({ ...data, waitResp: 0 })
          }
        }
      })
  }

  componentDidUpdate(props, prevState) {
    if (prevState.content !== this.state.content || prevState.reasoning !== this.state.reasoning) scrollToBottom()
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
        {this.state.attach && (
          <div className="msg-attach">
            {this.state.attach.map((item, idx) => {
              return <Attach {...item} _chatid={this.props._chatid} key={idx} />
            })}
          </div>
        )}
      </div>
    )
  }

  renderAi() {
    return (
      <div className="msg-ai">
        <div className="avatar">
          <img src={`${rb.baseUrl}/assets/img/icon-192x192.png`} alt="AI" />
        </div>
        <div className="msg-content">
          {this.state.waitResp == 1 && (
            <div className="wait-resp">
              <i className="mdi-spin mdi mdi-loading fs-20" />
            </div>
          )}
          {this.state.reasoning && <div className="reasoning">{this.renderContent(this.state.reasoning)}</div>}
          {this.renderContent(this.state.content)}
        </div>
      </div>
    )
  }

  renderSystem() {
    // TODO 不渲染
    console.log('renderSystem', this.state.content)
  }

  renderError() {
    return (
      <div className="msg-error">
        <div className="msg-content">{this.state.error || 'UNKNOW ERROR'}</div>
      </div>
    )
  }

  renderContent(content) {
    const md = content || this.state.content
    if (!md) return null
    return (
      <div className="msg-text">
        <span className="mdedit-content" dangerouslySetInnerHTML={{ __html: marked.parse(md) }}></span>
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
              this.toggleShow(false)
            }}>
            <i className="mdi mdi-chat-plus-outline mr-1 icon" />
            {$L('新对话')}
          </a>
        </div>
        <div className="chat-list">
          <ul className="list-unstyled m-0">
            {this.state.list.map((item) => {
              return (
                <li key={item.chatid} className={this.state.current === item.chatid ? 'active' : ''}>
                  <div
                    className="text-ellipsis"
                    title={item.subject}
                    onClick={() => {
                      this.props._Chat.initChat(item.chatid)
                      // this.toggleShow(false)
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
                      <a className="dropdown-item" onClick={() => this.handleRename(item)}>
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
      </div>
    )
  }

  handleDelete(item) {
    $.post('/aibot/post/chat-delete?chatid=' + item.chatid, () => this._loadChatList())
  }

  handleRename(item) {
    renderRbcomp(
      <DlgChatRename
        name={item.subject}
        onConfirm={(s) => {
          if (!$empty(s)) {
            $.post(`/aibot/post/chat-rename?chatid=${item.chatid}&s=${$encode(s)}`, () => this._loadChatList())
          }
        }}
      />
    )
  }

  toggleShow(showOrHide) {
    let show = !this.state.show
    if (showOrHide === true) show = true
    else if (showOrHide === false) show = false

    this.setState({ show: show }, () => {
      this.state.show && this._loadChatList()
    })
  }
}

// ~~

class Attach extends React.Component {
  render() {
    if (!this.state) return null
    if (this.props._ChatInput) {
      return (
        <span>
          {this.state.name}
          <a className="close" onClick={() => this.props._ChatInput.removeAttach(this.props.id)}>
            &times;
          </a>
        </span>
      )
    }
    // View
    return (
      <a href={`${rb.baseUrl}/aibot/redirect?id=${this.props._chatid}:${this.props.id}`} target={'_blank'}>
        {this.state.name}
      </a>
    )
  }

  componentDidMount() {
    const props = this.props
    if (props.record) {
      $.get(`/commons/search/read-labels?id=${props.record}`, (res) => {
        const d = res.data || {}
        this.setState({ name: `[${$L('记录')}] ${d[props.record]}` })
      })
    } else if (props.listFilter) {
      this.setState({ name: props.name || $L('列表数据') })
    }
  }

  val() {
    const props = this.props
    let res = { id: props.id }
    if (props.record) {
      return { ...res, record: props.record }
    }
    if (props.listFilter) {
      return { ...res, listFilter: props.listFilter }
    }
    return null
  }
}

// 选择记录
class DlgChatRename extends RbAlert {
  renderContent() {
    return (
      <div className="form ml-3 mr-3">
        <div className="form-group">
          <label className="text-bold">{$L('重命名会话')}</label>
          <input type="text" className="form-control form-control-sm" placeholder={this.props.name || ''} defaultValue={this.props.name || ''} ref={(c) => (this._$name = c)} autoFocus />
        </div>
        <div className="form-group mb-2">
          <button type="button" className="btn btn-primary" onClick={this._onConfirm}>
            {$L('确定')}
          </button>
        </div>
      </div>
    )
  }

  _onConfirm = () => {
    typeof this.props.onConfirm === 'function' && this.props.onConfirm($(this._$name).val())
    this.hide()
  }
}

// 选择记录
class DlgAttachRecord extends RbAlert {
  renderContent() {
    return (
      <div className="form ml-3 mr-3">
        <div className="form-group">
          <label className="text-bold">{$L('选择记录')}</label>
          <AnyRecordSelector ref={(c) => (this._AnyRecordSelector = c)} />
        </div>
        <div className="form-group mb-2">
          <button type="button" className="btn btn-primary" onClick={this._onConfirm}>
            {$L('确定')}
          </button>
        </div>
      </div>
    )
  }

  _onConfirm = () => {
    typeof this.props.onConfirm === 'function' && this.props.onConfirm(this._AnyRecordSelector.val())
    this.hide()
  }
}

// 选择列表
class DlgAttachRecordList extends RbAlert {
  renderContent() {
    return (
      <div className="form ml-3 mr-3">
        <div className="form-group">
          <label className="text-bold">{$L('选择数据范围')}</label>
          <div ref={(c) => (this._$select = c)}>
            <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" name="dataRange" type="radio" value="2" defaultChecked />
              <span className="custom-control-label">{$L('当前页的记录')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" name="dataRange" type="radio" value="3" />
              <span className="custom-control-label">{$L('查询后的记录')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio mb-1">
              <input className="custom-control-input" name="dataRange" type="radio" value="10" />
              <span className="custom-control-label">{$L('全部数据')}</span>
            </label>
          </div>
        </div>
        <div className="form-group mb-2">
          <button type="button" className="btn btn-primary" onClick={this._onConfirm}>
            {$L('确定')}
          </button>
        </div>
      </div>
    )
  }

  _onConfirm = () => {
    const s = $(this._$select).find('input:checked').val()
    typeof this.props.onConfirm === 'function' && this.props.onConfirm(s)
    this.hide()
  }
}
