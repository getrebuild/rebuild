/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global Chat */

// 独立聊天页（双栏布局：左侧会话列表 + 右侧聊天区）
class AiBotPage extends React.Component {
  constructor(props) {
    super(props)
    // 明暗偏好：'' 跟随系统 | 'light' | 'dark'
    let pref = $storage.get('__AiBotPageDark') || ''
    if (pref === 'true') pref = 'dark'
    if (pref === 'false') pref = 'light'
    this.state = { dark: this._isDark(pref) }
  }

  render() {
    return (
      <div className={`aibot-page ${this.state.dark ? 'chat-dark' : ''}`} ref={(c) => (this._$page = c)}>
        <div className="aibot-main">
          <div className="aibot-header">
            <button type="button" className="aibot-header-toggle" onClick={() => this._toggleSidebar()} title={$L('会话列表')}>
              <i className="mdi mdi-menu" />
            </button>
            <i className="mdi mdi-shimmer ai-color icon mr-1" />
            <h3>{rb._aibotName || $L('REBUILD AI 助手')}</h3>
            <div className="ml-auto">
              <button type="button" className="aibot-header-toggle" onClick={() => this._toggleDark()} title={this.state.dark ? $L('浅色模式') : $L('深色模式')}>
                <i className={`mdi ${this.state.dark ? 'mdi-white-balance-sunny' : 'mdi-weather-night'}`} />
              </button>
            </div>
          </div>

          <Chat
            standalone
            chatid={this.props.chatid}
            onChatidChanged={(id) => {
              location.hash = 'chatid=' + (id || '')
            }}
            ref={(c) => (this._Chat = c)}
          />

          <div className="aibot-footer" ref={(c) => (this._$footer = c)} />
        </div>
      </div>
    )
  }

  componentDidMount() {
    // 服务端渲染的页脚移入布局槽位（参与 grid 定位）
    const footer = document.querySelector('.page-footer')
    if (footer && this._$footer) this._$footer.appendChild(footer)

    // 桌面端默认展开侧栏（记忆折叠状态；小屏为抽屉模式，默认收起）
    if (window.innerWidth >= 900 && $storage.get('__AiBotSidebarCollapsed') !== 'true') {
      this._Chat._ChatSidebar.toggleShow(true)
    }

    // 跟随系统时，系统偏好变化即时同步（手动切换后不再跟随）
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      if ($storage.get('__AiBotPageDark')) return
      this.setState({ dark: e.matches }, () => this._syncMdTheme())
    })

    // markdown 正文同步 dark 色板（复用全局 .markdown-body[data-theme='dark']），含动态新增的消息
    this._syncMdTheme()
    this._mdObserver = new MutationObserver(() => this._syncMdTheme())
    this._mdObserver.observe(this._$page, { childList: true, subtree: true })
  }

  // 偏好为 '' 时跟随系统，否则以偏好为准
  _isDark(pref) {
    return pref ? pref === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches
  }

  _toggleDark() {
    const dark = !this.state.dark
    this.setState({ dark }, () => this._syncMdTheme())
    $storage.set('__AiBotPageDark', dark ? 'dark' : 'light')
  }

  _syncMdTheme() {
    const theme = this.state.dark ? 'dark' : null
    this._$page.querySelectorAll('.markdown-body').forEach((el) => {
      if (theme) el.setAttribute('data-theme', theme)
      else el.removeAttribute('data-theme')
    })
  }

  _toggleSidebar() {
    const _Sidebar = this._Chat._ChatSidebar
    const show = !_Sidebar.state.show
    _Sidebar.toggleShow(show)
    // 仅桌面端记忆折叠状态（小屏抽屉不持久化）
    if (window.innerWidth >= 900) $storage.set('__AiBotSidebarCollapsed', show ? 'false' : 'true')
  }
}

$(document).ready(() => {
  renderRbcomp(<AiBotPage chatid={$urlp('chatid', location.hash)} />, 'chat-wrapper')
})
