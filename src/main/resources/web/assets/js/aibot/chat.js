/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global Chat */

const _chatMarked = new marked.Marked({
  renderer: {
    link({ href, title, tokens }) {
      const text = this.parser.parseInline(tokens)
      let safeHref = href
      if (/^https?:\/\//i.test(href)) {
        try {
          const url = new URL(href, window.location.href)
          if (url.hostname !== window.location.hostname) {
            safeHref = `${rb.baseUrl}/commons/url-safe?url=${encodeURIComponent(href)}`
          }
        } catch (e) {
          safeHref = `${rb.baseUrl}/commons/url-safe?url=${encodeURIComponent(href)}`
        }
      }

      let out = `<a href="${safeHref}"`
      if (title) out += ` title="${title}"`
      out += ` target="_blank" rel="noopener noreferrer">${text}</a>`
      return out
    },
  },
})

let __evt_ScrollToBottomStop = false
let __evt_StreamCancel = false

// eslint-disable-next-line no-unused-vars
class Chat extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      ...props,
      messages: [],
    }
    // 搜索框问AI时传入的预设消息（仅首次使用）
    this._presetMessage = props.presetMessage
    this._autoSend = props.autoSend
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

    $(this._$chat).on('click.sidebar-hide', (e) => {
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
    // 如果当前正在对话，仅关闭前端连接，后端继续完成并保存完整内容
    if (this._ChatInput && this._ChatInput.state.postState !== 0) {
      __evt_StreamCancel = true
    }

    this.setState({ chatid: chatid || null })
    this._ChatMessages.setMessages([], false, null)
    this._ChatInput.reset(true, this._presetMessage)
    this._presetMessage = null
    var _autoSend = this._autoSend
    this._autoSend = null
    this._ChatSidebar.setState({ current: null })

    $.get(`/aibot2/post/chat-init?chatid=${chatid || ''}`, (res) => {
      if (res.error_code === 0) {
        const d = res.data || {}
        if (d._chatid) {
          this.setState({ chatid: d._chatid })
          this._ChatSidebar.setState({ current: d._chatid })
        }
        this._ChatMessages.setMessages(d.messages || [], true, d.suggestQuestions || null)

        if (_autoSend && this._ChatInput.state.content) {
          this._ChatInput.hanldeSend()
        }
      } else {
        this._ChatMessages.setMessages([{ error: res.error_msg }])
      }
    })
  }

  toggleSidebar(showOrHide) {
    this._ChatSidebar.toggleShow(showOrHide)
  }

  send(data) {
    scrollToBottom(true)
    this._ChatMessages.appendMessage(data)

    setTimeout(() => {
      this._ChatMessages.appendMessage({
        role: 'assistant',
        sendResp: (cb) => {
          $.post(`/aibot2/post/chat?chatid=${this.state.chatid || ''}&model=&noload`, JSON.stringify(data), (res) => {
            if (res._chatid) this.setState({ chatid: res._chatid })
            typeof cb === 'function' && cb({ ...res })
          })
        },
      })
    }, 40)
  }

  sendStream(data, onDone) {
    scrollToBottom(true)
    this._ChatMessages.appendMessage(data)

    setTimeout(() => {
      this._ChatMessages.appendMessage({
        role: 'assistant',
        sendResp: (onChunk) => {
          fetchStream(`${rb.baseUrl}/aibot2/post/chat-stream?chatid=${this.state.chatid || ''}&model=&noload`, data, onChunk, onDone)
        },
      })
    }, 20)
  }
}

class ChatInput extends React.Component {
  constructor(props) {
    super(props)
    this.state = { postState: 0, attach: [], skills: [], activeSkill: null }
  }

  render() {
    const skills = this.state.skills || []
    return (
      <div className="chat-input-container">
        <div className={`chat-input ${this.state.active && 'active'}`}>
          <div className="chat-input-input">
            <div className="chat-input-attach">
              {this.state.activeSkill && <Attach key={`skill-${this.state.activeSkill}`} skill={this.state.activeSkill} _ChatInput={this} id={`skill-${this.state.activeSkill}`} />}
              {this.state.attach && this.state.attach.length > 0 && (
                <RF>
                  {this.state.attach.map((item, idx) => {
                    return <Attach {...item} _ChatInput={this} key={idx} />
                  })}
                </RF>
              )}
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
            {skills.length > 0 && (
              <span className="dropup">
                <button type="button" className="btn btn-sm" data-toggle="dropdown" disabled={this.state.postState !== 0} title={$L('技能')}>
                  <i className="mdi mdi-flash-outline" style={{ paddingTop: 3 }} />
                </button>
                <div className="dropdown-menu auto-scroller dropdown-menu-right" style={{ width: 300 }} ref={(c) => (this._$skills = c)}>
                  {skills.map((s, idx) => (
                    <a
                      key={idx}
                      className="dropdown-item"
                      onClick={() => {
                        this.setState({ activeSkill: s.name })
                      }}>
                      {s.name}
                      {s.description && <div className="text-muted fs-12 text-break">{s.description}</div>}
                    </a>
                  ))}
                </div>
              </span>
            )}
            <span className="dropup">
              <button type="button" className="btn btn-sm" data-toggle="dropdown" disabled={this.state.postState !== 0} title={$L('数据')}>
                <i className="mdi mdi-attachment-plus" />
              </button>
              <div className="dropdown-menu dropdown-menu-right">
                <a className="dropdown-item" onClick={() => this.attachFile()}>
                  {$L('选择文件')}
                </a>
                <a className="dropdown-item" onClick={() => this.attachRecord()}>
                  {$L('选择记录')}
                </a>
                <a className="dropdown-item" onClick={() => this.attachPageData()}>
                  {$L('选择当前页数据')}
                </a>
              </div>
            </span>
            <button
              type="button"
              className="btn btn-sm ml-1"
              title={this.state.postState === 0 ? $L('发送') : this.state.postState === 2 ? $L('中断中') : $L('停止')}
              disabled={this.state.postState === 2 || (this.state.postState === 0 && $empty(this.state.content))}
              onClick={() => {
                if (this.state.postState === 0) this.hanldeSend()
                else if (this.state.postState === 1) this.handleCancel()
              }}>
              <i className={this.state.postState === 0 ? 'mdi mdi-arrow-up' : this.state.postState === 2 ? 'mdi mdi-spin mdi-loading' : 'mdi mdi-stop'} />
            </button>
          </div>
          <input ref={(c) => (this._$file = c)} type="file" className="inputfile" data-local="temp" data-maxsize="52428800" multiple />
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
      skill: this.state.activeSkill,
    }
    this.props._Chat &&
      this.props._Chat.sendStream(data, () => {
        this.setState({ postState: 0 })
      })

    this.reset()
    this.setState({ postState: 1 })
  }

  handleCancel() {
    this.setState({ postState: 2 })
    __evt_StreamCancel = true
    // 通知后端中断流式输出
    const chatid = this.props._Chat.state.chatid
    if (chatid) {
      $.post(`/aibot2/post/chat-stream-stop?chatid=${chatid}`)
    }
  }

  reset(autoFocus, presetContent) {
    this.setState({ content: presetContent || '', attach: [], postState: 0, activeSkill: null }, () => {
      if (autoFocus) this._$textarea.focus()
    })
  }

  removeAttach(id) {
    if (id && id.startsWith('skill-')) {
      this.setState({ activeSkill: null })
    } else {
      const attach = this.state.attach.filter((item) => item.id !== id)
      this.setState({ attach })
    }
  }

  _loadSkills() {
    $.get('/aibot2/skills', (res) => {
      this.setState({ skills: res.data || [] }, () => {
        $(this._$skills).perfectScrollbar()
      })
    })
  }

  componentDidMount() {
    this._loadSkills()
    $multipleUploader(this._$file, (res) => {
      const attach = [...this.state.attach, { file: res.key, id: $random('attach-', true) }]
      this.setState({ attach })
    })
  }

  attachFile() {
    if (rb.commercial < 1) {
      RbAlertFree43.create($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)'))
      return false
    }
    this._$file.click()
  }

  attachRecord() {
    const ps = {
      onConfirm: (v) => {
        let attach = [...this.state.attach]
        if (typeof v === 'object') {
          v.forEach((id) => {
            attach.push({ record: id, id: $random('attach-', true) })
          })
        } else {
          attach.push({ record: v, id: $random('attach-', true) })
        }
        this.setState({ attach })
      },
      allowMultiple: true,
      allowEntities: window.__LAB_AIALLOWENTITIES435 || null,
      allowBizz: false,
    }
    renderRbcomp(<RecordSelectorModal2 {...ps} zIndex="1050" />)
  }

  attachPageData() {
    if (typeof window.attachAibotPageData === 'function') {
      window.attachAibotPageData((data) => {
        const attach = [...this.state.attach, { ...data, id: $random('attach-', true) }]
        this.setState({ attach })
      })
    } else {
      RbHighbar.createl('当前页没有可使用数据')
    }
  }
}

class ChatMessages extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      messages: [],
      suggestQuestions: null,
    }
  }

  render() {
    const showSuggest = this.state.suggestQuestions && this.state.suggestQuestions.length > 0 && !this._hasUserMessage()
    return (
      <div className="chat-messages" ref={(c) => (this._$messages = c)}>
        {this.state.messages.map((item, idx) => {
          return <ChatMessage {...item} key={idx} _ChatMessages={this} />
        })}
        {showSuggest && (
          <div className="chat-suggest">
            <div className="text-muted mb-1 fs-13">{$L('你可以问我')}</div>
            <div className="d-flex flex-wrap">
              {this.state.suggestQuestions.map((q, idx) => (
                <a key={idx} className="badge badge-pill mr-1 mb-1" onClick={() => this._handleSuggestClick(q)}>
                  <i className="mdi mdi-chat-processing-outline mr-1" />
                  {q}
                </a>
              ))}
            </div>
          </div>
        )}
      </div>
    )
  }

  _hasUserMessage() {
    return this.state.messages.some((m) => m.role === 'user')
  }

  _handleSuggestClick(question) {
    const _Chat = this.props._Chat
    const _ChatInput = _Chat._ChatInput
    if (!_ChatInput || _ChatInput.state.postState !== 0) return
    _ChatInput.setState({ content: question }, () => {
      _ChatInput.hanldeSend()
    })
  }

  appendMessage(data) {
    this.setMessages([...this.state.messages, data])
  }

  setMessages(messages, forceScroll, suggestQuestions) {
    const state = { messages: messages }
    if (suggestQuestions !== undefined) state.suggestQuestions = suggestQuestions
    this.setState(state, () => {
      $(this._$messages).perfectScrollbar('update')
      scrollToBottom(forceScroll)
    })
  }

  componentDidMount() {
    const $ms = $(this._$messages).perfectScrollbar()

    // scrollToBottom
    let _lastScroll = 0
    $ms.on('scroll', function () {
      let currentScroll = $(this).scrollTop()
      if (_lastScroll - currentScroll > 60) {
        __evt_ScrollToBottomStop = true
      } else {
        if (__evt_ScrollToBottomStop) {
          let isAtBottom = $ms.scrollTop() + $ms.innerHeight() >= $ms[0].scrollHeight - 10
          if (isAtBottom) __evt_ScrollToBottomStop = false
        }
      }
      _lastScroll = currentScroll
    })
  }

  componentWillUnmount() {
    $(this._$messages).perfectScrollbar('destroy')
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
    if (prevState.content !== this.state.content || prevState.reasoning !== this.state.reasoning) {
      scrollToBottom()
    }
  }

  render() {
    let c = null
    if (this.props.role === 'user') c = this.renderUser()
    else if (this.props.role === 'assistant' || this.props.role === 'ai') c = this.renderAi()
    else if (this.props.role === 'system') c = this.renderSystem()
    else c = this.renderError()

    return (
      <div className="chat-message">
        {c}
        <div className="msg-action">
          <a title={$L('复制')} onClick={() => $clipboard2(this.state.content || '')}>
            <i className="mdi mdi-content-copy icon" />
          </a>
        </div>
      </div>
    )
  }

  renderUser() {
    return (
      <div className="msg-user">
        <div className="msg-content">{this.renderContent()}</div>
        {this.state.skill && (
          <div className="msg-attach">
            <Attach skill={this.state.skill} _chatid={this.props._chatid} />
          </div>
        )}
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
          {this.state.waitResp === 1 && (
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
    let md = content || this.state.content
    if (!md) return null

    md = fixMd(md)
    return (
      <div className="msg-text">
        <span className="markdown-body" dangerouslySetInnerHTML={{ __html: _chatMarked.parse(md) }}></span>
      </div>
    )
  }
}

function scrollToBottom(forceScroll) {
  if (forceScroll) __evt_ScrollToBottomStop = false
  if (__evt_ScrollToBottomStop) return

  $setTimeout(
    () => {
      const $el = $('.chat-messages')
      if ($el.length === 0) return
      $el.scrollTop($el[0].scrollHeight)
      $el.perfectScrollbar('update')
    },
    40,
    'scrollToBottom',
  )
}

function fetchStream(url, data, onChunk, onDone) {
  const decoder = new TextDecoder('utf-8')
  const controller = new AbortController()
  const signal = controller.signal

  let buffer = ''
  fetch(url, {
    signal: signal,
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  })
    .then((response) => {
      const reader = response.body.getReader()
      function readChunk() {
        // 前端中止 + 后端中断
        if (__evt_StreamCancel) {
          controller.abort()
          __evt_StreamCancel = false
          typeof onDone === 'function' && onDone(null, true)
          return
        }

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
      typeof onChunk === 'function' && onChunk({ error: err })
      typeof onDone === 'function' && onDone(null, true)
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
    $(this._$list).perfectScrollbar()
  }

  componentWillUnmount() {
    $(this._$list).perfectScrollbar('destroy')
  }

  componentDidUpdate(props, prevState) {
    if (prevState.current !== this.state.current) {
      $storage.set('__LastChatId', this.state.current)
    }
  }

  _loadChatList() {
    $.get('/aibot2/post/chat-list', (res) => {
      const data = res.data || []
      this.setState({ list: data }, () => {
        $(this._$list).perfectScrollbar('update')
      })

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
            {$L('新会话')}
          </a>
        </div>
        <div className="chat-list" ref={(c) => (this._$list = c)}>
          <ul className="list-unstyled m-0">
            {this.state.list.map((item) => {
              return (
                <li key={item.chatid} className={this.state.current === item.chatid ? 'active' : ''}>
                  <div
                    className="text-ellipsis"
                    title={item.subject}
                    onClick={() => {
                      this.props._Chat.initChat(item.chatid)
                      this.setState({ current: item.chatid })
                      // this.toggleShow(false)
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
    $.post('/aibot2/post/chat-delete?chatid=' + item.chatid, () => this._loadChatList())
  }

  handleRename(item) {
    renderRbcomp(
      <DlgChatRename
        name={item.subject}
        onConfirm={(s) => {
          if (!$empty(s)) {
            $.post(`/aibot2/post/chat-rename?chatid=${item.chatid}&s=${$encode(s)}`, () => this._loadChatList())
          }
        }}
      />,
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
        <span className="text-ellipsis">
          {this.state.name}
          <a className="close" onClick={() => this.props._ChatInput.removeAttach(this.props.id)}>
            &times;
          </a>
        </span>
      )
    }

    // View
    if (this.state.viewUrl) {
      return (
        <a href={this.state.viewUrl} target="_blank" title={$L('查看')}>
          {this.state.name}
        </a>
      )
    }
    return <a>{this.state.name}</a>
  }

  componentDidMount() {
    const props = this.props
    if (props.record) {
      $.get(`/commons/search/read-labels?id=${props.record}`, (res) => {
        const d = res.data || {}
        this.setState({
          name: `[${$L('记录')}] ${d[props.record] || '[DELETED]'}`,
          viewUrl: `${rb.baseUrl}/app/redirect?id=${props.record}&type=newtab`,
        })
      })
    } else if (props.listFilter) {
      this.setState({
        name: props.name || $L('列表数据'),
        viewUrl: `${rb.baseUrl}/app/${props.listFilter.entity}/list?via=`,
      })
    } else if (props.file) {
      this.setState({
        name: `[${$L('文件')}] ${$fileCutName(props.file)}`,
        viewUrl: `${rb.baseUrl}/commons/file-view?src=` + $encode(`/temp/${props.file}`),
      })
    } else if (props.skill) {
      this.setState({
        name: (
          <RF>
            <i className="mdi mdi-flash-outline" />
            <span>{props.skill + ''}</span>
          </RF>
        ),
      })
    }
  }

  val() {
    const props = this.props
    let rest = { id: props.id }
    if (props.record) {
      return { ...rest, record: props.record }
    }
    if (props.listFilter) {
      return { ...rest, listFilter: props.listFilter }
    }
    if (props.skill) {
      return { ...rest, skill: props.skill }
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
          <label className="text-dark text-bold">{$L('重命名会话')}</label>
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

// 选择列表数据
// eslint-disable-next-line no-unused-vars
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

// ~~ 选择记录
class RecordSelectorModal2 extends RecordSelectorModal {
  renderContent() {
    return (
      <div className="form ml-3 mr-3">
        <div className="form-group">
          <label className="text-bold">{this.props.title || $L('选择记录')}</label>
          <AnyRecordSelector ref={(c) => (this._AnyRecordSelector = c)} allowEntities={this.props.allowEntities} allowBizz={this.props.allowBizz} allowMultiple={this.props.allowMultiple} />
        </div>

        <div className="form-group mb-2">
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => {
              typeof this.props.onConfirm === 'function' && this.props.onConfirm(this._AnyRecordSelector.val())
              this.hide()
            }}>
            {$L('确定')}
          </button>
        </div>
      </div>
    )
  }
}

// 修复 AI 回复中常见的 Markdown 语法问题，确保 marked 正确渲染
function fixMd(md) {
  if (!md) return md

  // 1. 表格：GFM 要求表格前有空行，AI 有时忽略此规则
  if (md.indexOf('|') !== -1 && /\|[\s:]*-{2,}/.test(md)) {
    const lines = md.split('\n')
    for (let i = 0; i < lines.length; i++) {
      let line = lines[i]
      if (/\|[\s:]*-{2,}/.test(line) && /(\|)\s*(\|)/.test(line)) {
        const firstPipe = line.indexOf('|')
        let work = firstPipe > 0 ? line.substring(0, firstPipe).trimEnd() + '\n\n' + line.substring(firstPipe) : line
        work = work.replace(/(\|)\s*(\|)/g, '$1\n$2')
        lines[i] = work
        continue
      }

      if (i + 1 < lines.length && /\|[\s:]*-{2,}/.test(lines[i + 1])) {
        const pipeIdx = line.indexOf('|')
        if (pipeIdx > 0) {
          lines[i] = line.substring(0, pipeIdx).trimEnd() + '\n\n' + line.substring(pipeIdx)
        } else if (pipeIdx === 0 && i > 0 && lines[i - 1].trim() !== '') {
          lines[i] = '\n' + line
        }
      }
    }
    md = lines.join('\n')
  }

  // 2. 粗体：去除开启/闭合 ** 内侧多余空格（仅同行，避免跨行吞表格结构）
  md = md.replace(/(\*\*)[ \t]+([^\n*]+?)[ \t]*(\*\*)/g, '$1$2$3')
  md = md.replace(/(\*\*[^\n*]+\*\*)(?=[^\s\)\]}>.,;:!?，。；：！？、）】])/g, '$1 ')

  // 3. 标题：CommonMark 要求 # 后必须有空格
  md = md.replace(/^(#{1,6})(?=[^\s#])/gm, '$1 ')

  return md
}
