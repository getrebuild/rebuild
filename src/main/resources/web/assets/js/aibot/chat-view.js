/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global Chat */

const _darkPref = () => (document.cookie.match(/(?:^|;\s*)rb\.ThemeDark2=(dark|light)/) || [])[1] || ''

class AiBotPage extends React.Component {
  constructor(props) {
    super(props)
    this.state = { dark: this._isDark(_darkPref()) }
  }

  render() {
    const canChat = rb.commercial >= 1 && rb.currentUser
    let chatBody
    if (rb.commercial < 1) {
      chatBody = (
        <div className="aibot-nologin">
          <i className="mdi mdi-lock-outline" />
          <h4>{WrapHtml($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)'))}</h4>
        </div>
      )
    } else if (canChat) {
      chatBody = (
        <Chat
          standalone
          chatid={this.props.chatid}
          onChatidChanged={(id) => {
            location.hash = 'chatid=' + (id || '')
          }}
          ref={(c) => (this._Chat = c)}
        />
      )
    } else {
      chatBody = (
        <div className="aibot-nologin">
          <i className="mdi mdi-account-circle-outline" />
          <h4>{$L('请登录后使用')}</h4>
          <a className="btn btn-primary" href={`${rb.baseUrl}/user/login?nexturl=${encodeURIComponent(location.pathname + location.search)}`}>
            {$L('去登录')}
          </a>
        </div>
      )
    }

    return (
      <div className="aibot-page" ref={(c) => (this._$page = c)}>
        <div className="aibot-main">
          <div className="aibot-header">
            {canChat && (
              <button type="button" className="aibot-header-toggle" onClick={() => this._toggleSidebar()} title={$L('会话列表')}>
                <i className="mdi mdi-menu" />
              </button>
            )}
            <i className="mdi mdi-shimmer ai-color icon mr-1" />
            <h3>{rb._aibotName || $L('REBUILD AI 助手')}</h3>
            <div className="ml-auto">
              <button type="button" className="aibot-header-toggle" onClick={() => this._toggleDark()} title={this.state.dark ? $L('浅色模式') : $L('深色模式')}>
                <i className={`mdi ${this.state.dark ? 'mdi-white-balance-sunny' : 'mdi-weather-night'}`} />
              </button>
            </div>
          </div>

          {chatBody}

          <div className="aibot-footer" ref={(c) => (this._$footer = c)} />
        </div>
      </div>
    )
  }

  componentDidMount() {
    const footer = document.querySelector('.page-footer')
    if (footer && this._$footer) this._$footer.appendChild(footer)

    if (this._Chat && window.innerWidth >= 900 && $storage.get('__AiBotSidebarCollapsed') !== 'true') {
      this._Chat._ChatSidebar.toggleShow(true)
    }

    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      if (_darkPref()) return
      this.setState({ dark: e.matches }, () => this._syncMdTheme())
    })

    this._syncMdTheme()
    this._mdObserver = new MutationObserver(() => this._syncMdTheme())
    this._mdObserver.observe(this._$page, { childList: true, subtree: true })
  }

  _isDark(pref) {
    return pref ? pref === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches
  }

  _toggleDark() {
    const dark = !this.state.dark
    this.setState({ dark }, () => this._syncMdTheme())
    document.cookie = 'rb.ThemeDark2=' + (dark ? 'dark' : 'light') + ';path=/;max-age=31536000'
  }

  _syncMdTheme() {
    const theme = this.state.dark ? 'dark' : null
    document.documentElement.classList.toggle('theme-dark2', this.state.dark)
    this._$page.querySelectorAll('.markdown-body').forEach((el) => {
      if (theme) el.setAttribute('data-theme', theme)
      else el.removeAttribute('data-theme')
    })
  }

  _toggleSidebar() {
    const _Sidebar = this._Chat._ChatSidebar
    const show = !_Sidebar.state.show
    _Sidebar.toggleShow(show)
    if (window.innerWidth >= 900) $storage.set('__AiBotSidebarCollapsed', show ? 'false' : 'true')
  }
}

$(document).ready(() => {
  renderRbcomp(<AiBotPage chatid={$urlp('chatid', location.hash)} />, 'chat-wrapper')
})
