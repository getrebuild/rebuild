/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global Chat, mermaid */

const _chatMarked = new marked.Marked({
  renderer: {
    code({ text, lang }) {
      // ```echarts
      if (lang === 'echarts' || lang === 'echart') {
        const safe = String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        return `<div class="echarts-to-render">${safe}</div>`
      }
      // ```mermaid
      if (lang === 'mermaid') {
        const safe = String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        return `<div class="mermaid-to-render">${safe}</div>`
      }
      // ```html
      if (lang === 'html' || lang === 'htm') {
        const safe = String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        return `<div class="html-to-render">${safe}</div>`
      }
      return false
    },
    link({ href, title, tokens }) {
      const text = this.parser.parseInline(tokens)
      // 非安全协议（javascript:/data: 等）渲染为纯文本，防止 XSS；站内相对路径（/ 开头）放行
      if (!/^https?:\/\//i.test(href) && !href.startsWith('/')) return text

      let safeHref = href
      try {
        const url = new URL(href, window.location.href)
        if (url.hostname !== window.location.hostname) {
          safeHref = `${rb.baseUrl}/commons/url-safe?url=${encodeURIComponent(href)}`
        }
      } catch (e) {
        safeHref = `${rb.baseUrl}/commons/url-safe?url=${encodeURIComponent(href)}`
      }

      const escAttr = (s) => String(s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;')
      let out = `<a href="${escAttr(safeHref)}"`
      if (title) out += ` title="${escAttr(title)}"`
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
    // 预设输入对象（仅首次使用）
    this._preset = props.preset
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
    this._stopPendingPoll()
    $(this._$chat).off('click.chat-hide')
  }

  initChat(chatid) {
    this._stopPendingPoll()

    // 如果当前正在对话，仅关闭前端连接，后端继续完成并保存完整内容
    if (this._ChatInput && this._ChatInput.state.postState !== 0) {
      __evt_StreamCancel = true
    }

    this.setState({ chatid: chatid || null })
    this._ChatMessages.setMessages([], false, null)
    this._ChatInput.reset(true)
    const _preset = this._preset
    this._preset = null
    this._ChatSidebar.setState({ current: null })

    $.get(`/aibot2/post/chat-init?chatid=${chatid || ''}`, (res) => {
      if (res.error_code === 0) {
        const d = res.data || {}
        if (d._chatid) {
          this.setState({ chatid: d._chatid })
          this._ChatSidebar.setState({ current: d._chatid })
        }
        let messages = d.messages || []
        // 最后一条是用户消息，说明 AI 还在回答中（刷新中断场景）
        if (messages.length > 0 && messages[messages.length - 1].role === 'user') {
          messages = [...messages, { role: 'assistant', sendResp: () => {}, pending: true }]
          this._startPendingPoll(d._chatid || chatid)
          this._ChatInput.setState({ postState: 1 })
        }

        this._ChatMessages.setMessages(messages, true, d.suggestQuestions || null)

        // 应用预设输入
        if (_preset) {
          let newState = {}
          if (_preset.content) newState.content = _preset.content
          if (_preset.attach) newState.attach = _preset.attach
          if (_preset.skill) newState.activeSkill = _preset.skill
          this._ChatInput.setState(newState, () => {
            if (_preset.autoSend && this._ChatInput.state.content) {
              this._ChatInput.hanldeSend()
            }
          })
        }
      } else {
        this._ChatMessages.setMessages([{ error: res.error_msg }])
      }
    })
  }

  toggleSidebar(showOrHide) {
    this._ChatSidebar.toggleShow(showOrHide)
  }

  send(data, onDone) {
    this._stopPendingPoll()
    scrollToBottom(true)
    this._ChatMessages.appendMessage(data)

    setTimeout(() => {
      this._ChatMessages.appendMessage({
        role: 'assistant',
        sendResp: (onChunk) => {
          $.post(`/aibot2/post/chat?chatid=${this.state.chatid || ''}&model=&noload`, JSON.stringify(data), (res) => {
            if (res._chatid) this.setState({ chatid: res._chatid })
            typeof onChunk === 'function' && onChunk({ ...res })
            typeof onChunk === 'function' && onChunk({ type: '_done' })
            typeof onDone === 'function' && onDone()
          })
        },
      })
    }, 40)
  }

  sendStream(data, onDone) {
    this._stopPendingPoll()
    scrollToBottom(true)
    this._ChatMessages.appendMessage(data)

    setTimeout(() => {
      this._ChatMessages.appendMessage({
        role: 'assistant',
        sendResp: (onChunk) => {
          fetchStream(`${rb.baseUrl}/aibot2/post/chat-stream?chatid=${this.state.chatid || ''}&model=&noload`, data, onChunk, () => {
            typeof onChunk === 'function' && onChunk({ type: '_done' })
            typeof onDone === 'function' && onDone()
          })
        },
      })
    }, 20)
  }

  // 轮询检测 AI 回答是否已落库（刷新中断场景）
  _startPendingPoll(chatid) {
    this._stopPendingPoll()
    if (!chatid) return
    this._pendingChatid = chatid

    this._pendingTimer = setInterval(() => {
      $.get(`/aibot2/post/chat-init?chatid=${chatid}`, (res) => {
        if (res.error_code !== 0) return
        const newMessages = res.data.messages || []
        const last = newMessages[newMessages.length - 1]
        // 最后一条变成 AI 消息，说明回答已落库
        if (last && (last.role === 'assistant' || last.role === 'ai')) {
          this._stopPendingPoll()
          this._ChatMessages.setMessages(newMessages, true, null)
          this._ChatInput.setState({ postState: 0 })
        }
      })
    }, 3000)
  }

  _stopPendingPoll() {
    if (this._pendingTimer) {
      clearInterval(this._pendingTimer)
      this._pendingTimer = null
    }
    this._pendingChatid = null
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
    if (this.state.postState !== 0) return
    if ($empty(this.state.content)) return

    const data = {
      role: 'user',
      content: this.state.content,
      attach: this.state.attach,
      skill: this.state.activeSkill,
      sendTime: Date.now(),
    }
    const onDone = () => this.setState({ postState: 0 })
    const _Chat = this.props._Chat
    _Chat && (_Chat.props.sendMode === 'post' ? _Chat.send(data, onDone) : _Chat.sendStream(data, onDone))

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
    renderRbcomp(<RecordSelectorModal2 {...ps} />)
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
                <a key={idx} className="badge badge-pill" onClick={() => this._handleSuggestClick(q)}>
                  <i className="mdi mdi-chat-plus-outline mr-1" />
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
    this.setState(state, () => scrollToBottom(forceScroll))
  }

  componentDidMount() {
    // scrollToBottom
    let _lastScroll = 0

    const $ms = $(this._$messages)
    $ms.on('scroll', function () {
      let currentScroll = $(this).scrollTop()
      if (_lastScroll - currentScroll > 20) {
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

  componentWillUnmount() {}
}

class ChatMessage extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, waitResp: props.sendResp ? 1 : 0, reasoningOpen: !!props.sendResp }
  }

  componentDidMount() {
    const sendResp = this.props.sendResp
    sendResp &&
      sendResp((data) => {
        data = data || {}
        if (data.type === '_done') {
          // 输出完成后自动收起思考过程
          this.setState({ waitResp: -1, reasoningOpen: false })
          return
        }
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
    // 占位消息被真实消息替换（sendResp 消失），同步 state
    if (props.sendResp && !this.props.sendResp) {
      this.setState({
        content: this.props.content,
        reasoning: this.props.reasoning,
        waitResp: 0,
        reasoningOpen: false,
      })
      return
    }

    const contentChanged = prevState.content !== this.state.content || prevState.reasoning !== this.state.reasoning
    if (contentChanged) scrollToBottom()
  }

  _feedbackable() {
    const chatid = this.props._ChatMessages.props._Chat.state.chatid
    return chatid && (!this.props.sendResp || this.state.waitResp === -1)
  }

  _feedback(type) {
    if (this.state.feedback) return
    const chatid = this.props._ChatMessages.props._Chat.state.chatid

    if (type === 'dislike') {
      renderRbcomp(<DlgFeedbackInput onConfirm={(comment) => this._submitFeedback(type, chatid, comment)} />)
    } else {
      this._submitFeedback(type, chatid, '')
    }
  }

  _submitFeedback(type, chatid, comment) {
    $.post(`/aibot2/post/chat-feedback?chatid=${chatid}&type=${type}&comment=${encodeURIComponent(comment)}`, () => {
      this.setState({ feedback: type })
    })
  }

  render() {
    let c = null
    if (this.props.role === 'user') c = this.renderUser()
    else if (this.props.role === 'assistant' || this.props.role === 'ai') c = this.renderAi()
    else if (this.props.role === 'system') c = this.renderSystem()
    else c = this.renderError()

    return (
      <div className="chat-message" ref={(c) => (this._$message = c)}>
        {c}
        <div className="msg-action">
          {this.props.role === 'user' && this.state.sendTime && (
            <span className="fs-12 text-muted mr-1">
              <DateShow date={moment(Number(this.state.sendTime)).format('YYYY-MM-DD HH:mm:ss')} showOrigin />
            </span>
          )}

          <a
            title={$L('复制')}
            onClick={(e) => {
              $clipboard(this.state.content || '')
              const $a = $(e.currentTarget)
              $a.addClass('copied-check')
              setTimeout(() => $a.removeClass('copied-check'), 1500)
            }}>
            <i className="icon mdi mdi-content-copy" />
          </a>
          {(this.props.role === 'assistant' || this.props.role === 'ai') && this._feedbackable() && (
            <RF>
              <a title={$L('有帮助')} onClick={() => this._feedback('like')} className={this.state.feedback === 'like' ? 'text-primary' : this.state.feedback ? 'text-disabled' : ''}>
                <i className="icon mdi mdi-thumb-up-outline fs-15" />
              </a>
              <a title={$L('没帮助')} onClick={() => this._feedback('dislike')} className={this.state.feedback === 'dislike' ? 'text-primary' : this.state.feedback ? 'text-disabled' : ''}>
                <i className="icon mdi mdi-thumb-down-outline fs-15" />
              </a>
            </RF>
          )}
          {(this.props.role === 'assistant' || this.props.role === 'ai') && this._feedbackable() && rb.fileSharable && (
            <a
              title={$L('分享')}
              onClick={() => {
                const chatid = this.props._ChatMessages.props._Chat.state.chatid
                // eslint-disable-next-line react/jsx-no-undef
                renderRbcomp(<FileShare file={chatid} title={$L('分享会话')} />)
              }}>
              <i className="icon zmdi zmdi-share fs-15" />
            </a>
          )}
        </div>
      </div>
    )
  }

  renderUser() {
    return (
      <div className="msg-user">
        <div className="msg-content">
          <RichContent content={this.state.content} md={false} />
        </div>
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
    const ready = !this.props.sendResp || this.state.waitResp === -1
    const busy = !ready
    const thinking = this.state.waitResp === 2
    return (
      <div className="msg-ai">
        <div className={`avatar${busy ? ' avatar-busy' : ''}`}>
          <img src={`${rb.baseUrl}/assets/img/icon-256x256.png`} alt="AI" />
        </div>
        <div className="msg-content">
          {this.state.waitResp === 1 && this.props.pending && (
            <div className="reasoning">
              <div className="reasoning-toggle cursor-default">
                <i className="mdi-spin mdi mdi-loading" style={{ marginLeft: 3, marginRight: 5 }} />
                <span>{$L('回答中...')}</span>
              </div>
            </div>
          )}
          {this.state.waitResp === 1 && !this.props.pending && (
            <div className="wait-resp">
              <i className="mdi-spin mdi mdi-loading fs-20" />
            </div>
          )}
          {this.state.reasoning && (
            <div className="reasoning">
              <div className="reasoning-toggle hover-opacity" onClick={() => this.setState({ reasoningOpen: !this.state.reasoningOpen })}>
                <i className={`fs-17 mdi mdi-chevron-${this.state.reasoningOpen ? 'down' : 'right'}`} />
                {thinking && <i className="mdi-spin mdi mdi-loading" style={{ marginLeft: 3, marginRight: 5 }} />}
                <span>{thinking ? $L('思考中...') : $L('思考过程')}</span>
              </div>
              {this.state.reasoningOpen && (
                <div className="reasoning-body">
                  <RichContent content={this.state.reasoning} ready={ready} />
                </div>
              )}
            </div>
          )}
          <RichContent content={this.state.content} ready={ready} />
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
}

// 富内容渲染组件
class RichContent extends React.Component {
  render() {
    const { content, md } = this.props
    if (!content) return null
    if (md === false) return <div className="msg-text">{content}</div>

    return (
      <div className="msg-text" ref={(c) => (this._$el = c)}>
        <span className="markdown-body" dangerouslySetInnerHTML={{ __html: _chatMarked.parse(this._fixMd(content)) }}></span>
      </div>
    )
  }

  componentDidMount() {
    this._renderRich()
  }

  componentDidUpdate(prev) {
    if (prev.content !== this.props.content || prev.ready !== this.props.ready) this._renderRich()
  }

  componentWillUnmount() {
    this._dispose()
  }

  _renderRich() {
    const $el = this._$el && $(this._$el)
    if (!$el || this.props.md === false) return
    const ready = this.props.ready !== false

    // echarts：流式中也渲染，done=ready 控制失败是否回退源码
    if ($el.find('.echarts-to-render:not(.echarts-rendered)').length) {
      if (!this._seq) this._seq = $random('rc-', true)
      $setTimeout(() => this._renderEcharts($el, ready), 200, 'render-echarts-' + this._seq)
    }
    // mermaid/html：流式中跳过，避免渲染半截内容
    if (ready && $el.find('.mermaid-to-render').length) {
      this._renderMermaid($el)
    }
    if (ready && $el.find('.html-to-render:not(.html-rendered)').length) {
      if (!this._seq) this._seq = $random('rc-', true)
      $setTimeout(() => this._renderHtml($el), 200, 'render-html-' + this._seq)
    }
  }

  _renderMermaid($el) {
    const self = this
    $useMermaid(() => {
      const checks = []
      $el.find('.mermaid-to-render').each(function () {
        const $node = $(this)
        checks.push(
          Promise.resolve()
            .then(() => mermaid.parse($node.text(), { suppressErrors: true }))
            .catch(() => false)
            .then((ok) => {
              if (!ok) self._fallbackSource($node, 'mermaid')
            }),
        )
      })
      Promise.all(checks).then(() => {
        $renderMermaid($el)
        // mermaid.run 异步渲染，延迟添加全屏按钮
        $setTimeout(
          () => {
            $el.find('.mermaid:not(.has-fs-btn)').each(function () {
              self._attachFullscreenBtn($(this))
            })
          },
          300,
          'mermaid-fs-btn',
        )
      })
    })
  }

  _renderHtml($container) {
    const $nodes = $container.find('.html-to-render:not(.html-rendered)')
    if (!$nodes.length) return
    const self = this
    $nodes.each(function () {
      const $node = $(this)
      $node.addClass('html-rendered')
      const html = $node.text() // textContent 自动解码 &lt; 等
      const $iframe = $('<iframe></iframe>').attr({ sandbox: 'allow-scripts' })
      $node.empty().append($iframe)
      $iframe[0].srcdoc = html
      self._attachFullscreenBtn($node)
    })
  }

  _renderEcharts($container, done) {
    if (!$container || !$container.length) return
    if ($container.find('.echarts-to-render:not(.echarts-rendered)').length === 0) return

    const self = this
    $useEchart(() => {
      $container.find('.echarts-to-render:not(.echarts-rendered)').each(function () {
        const $node = $(this)
        let option
        try {
          option = JSON.parse($node.text())
        } catch (err) {
          if (done) {
            self._fallbackSource($node, 'echarts')
          } else {
            console.warn('ECharts option parse failed :', err)
          }
          return
        }

        $node.addClass('echarts-rendered').empty()
        try {
          const chart = echarts.init($node[0])
          const base = { ...ECHART_BASE }
          delete base.grid
          const opt = { ...base, ...option }
          opt.tooltip = { ...base.tooltip, ...(option.tooltip || {}) }
          opt.textStyle = { ...base.textStyle, ...(option.textStyle || {}) }
          if (opt.title) opt.title = { ...opt.title, top: 10 }
          if (opt.legend) opt.legend = { ...opt.legend, top: opt.title ? 40 : 10 }
          opt.grid = { ...(opt.grid || {}), top: opt.title ? 80 : opt.legend ? 50 : 40, bottom: 50 }
          chart.setOption(opt)
          $node.data('echarts-instance', chart)
          self._attachFullscreenBtn($node)
        } catch (err) {
          console.error('ECharts render error :', err)
          $node.removeClass('echarts-rendered')
        }
      })
    })
  }

  _attachFullscreenBtn($node) {
    if (!$node || !$node.length || $node.hasClass('has-fs-btn')) return
    $node.addClass('has-fs-btn')
    const $btn = $('<a class="rich-fullscreen-btn"><i class="mdi mdi-fullscreen"></i></a>')
    $btn.attr('title', $L('全屏'))
    $node.append($btn)
    $btn.on('click', (e) => {
      $stopEvent(e, true)
      RbPreview.create($node)
    })
  }

  _fallbackSource($node, lang) {
    const source = $node.text()
    $node.removeClass('echarts-to-render mermaid-to-render').empty()
    $('<pre></pre>')
      .append($('<code></code>').addClass(`language-${lang}`).text(source))
      .appendTo($node)
  }

  _dispose() {
    const $el = this._$el && $(this._$el)
    if (!$el || !$el.length) return
    $el.find('.echarts-rendered').each(function () {
      const chart = $(this).data('echarts-instance')
      if (chart && typeof chart.dispose === 'function') chart.dispose()
    })
  }

  // 修复 AI 回复中常见的 Markdown 语法问题
  _fixMd(md) {
    if (!md) return md
    if (window.__LAB45_NOTFIXAIMD) return md

    // 0. 代码块 fence：AI 有时把 fence 接在文字行尾（CommonMark 要求 fence 行首，否则闭合 fence 会被误作开启吞噬后续内容），或把内容紧贴在语言标记后
    md = md.replace(/([^\s`])[ \t]*(`{3,}|~{3,})([A-Za-z0-9_+-]*)[ \t]*$/gm, '$1\n$2$3')
    md = md.replace(/(`{3,}|~{3,})(html|mermaid|echarts)(\S)/g, '$1$2\n$3')

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
    md = md.replace(/(\*\*[^\n*]+\*\*)(?=[^\s)\]}>.,;:!?，。；：！？、）】])/g, '$1 ')

    // 3. 标题：CommonMark 要求 # 后必须有空格
    md = md.replace(/^(#{1,6})(?=[^\s#])/gm, '$1 ')

    return md
  }
}

function scrollToBottom(forceScroll) {
  if (forceScroll) __evt_ScrollToBottomStop = false
  if (__evt_ScrollToBottomStop) return

  $setTimeout(
    () => {
      const $el = $('.chat-messages')
      if ($el.length === 0) return
      $el.scrollTop($el[0].scrollHeight + 20)
    },
    40,
    'chat-scrollToBottom',
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
  }

  componentWillUnmount() {}

  componentDidUpdate(props, prevState) {
    if (prevState.current !== this.state.current) {
      $storage.set('__AiBotLastChatId', this.state.current)
    }
  }

  _loadChatList() {
    $.get('/aibot2/post/chat-list', (res) => {
      const data = res.data || []
      this.setState({ list: data }, () => {})

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

// 反馈输入弹窗（可选输入）
// eslint-disable-next-line no-unused-vars
class DlgFeedbackInput extends RbAlert {
  constructor(props) {
    super(props)
    this.state = { ...props, comment: '' }
  }

  renderContent() {
    const presets = ['回答不准确', '缺少内容', '实现不了我的要求', '回答中断了', '内容格式有问题']
    return (
      <div className="ml-6 mr-6">
        <h5 className="mb-2 text-bold">{$L('能告诉我们哪里不对吗')}</h5>
        <div className="mb-2 d-flex flex-wrap">
          {presets.map((p, idx) => (
            <a key={idx} className="badge badge-pill cursor-pointer mr-1 mb-1" onClick={() => this.setState({ comment: p })}>
              {p}
            </a>
          ))}
        </div>
        <textarea
          className="form-control form-control-sm"
          maxLength="200"
          placeholder={$L('您的反馈非常重要')}
          value={this.state.comment || ''}
          onChange={(e) => this.setState({ comment: e.target.value })}
          autoFocus
        />
        <div className="mt-3 mb-1">
          <button type="button" className="btn btn-primary" onClick={this._onConfirm}>
            {$L('提交')}
          </button>
        </div>
      </div>
    )
  }

  _onConfirm = () => {
    typeof this.props.onConfirm === 'function' && this.props.onConfirm(this.state.comment || '')
    this.hide()
    RbHighbar.success($L('感谢您的反馈'))
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
