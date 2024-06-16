/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */
/* global SimpleMDE */

// ~~ Modal 兼容子元素和 iFrame
class RbModal extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    const props = this.props
    const style2 = { maxWidth: ~~(props.width || 680) }
    if (props.useWhite || props.maximize) {
      style2.maxWidth = this.state._maximize ? $(window).width() - 60 : null
      if (!style2.maxWidth && props.width) style2.maxWidth = ~~props.width
    }

    const modalClazz = props.useWhite ? 'modal rbmodal use-white' : `modal rbmodal colored-header colored-header-${props.colored || 'primary'}`
    return (
      <div
        className={modalClazz}
        style={props.zIndex ? { zIndex: props.zIndex } : null}
        ref={(c) => {
          this._rbmodal = c
          this._element = c
        }}>
        <div className={`modal-dialog ${props.useWhite && 'modal-xl'} ${props.className || ''}`} style={style2}>
          <div className="modal-content" style={style2}>
            <div
              className={`modal-header ${props.useWhite ? '' : 'modal-header-colored'}`}
              onDoubleClick={(e) => {
                if (this.props.maximize) {
                  $stopEvent(e, true)
                  this.setState({ _maximize: !this.state._maximize })
                }
              }}>
              {props.icon && <i className={`icon zmdi zmdi-${props.icon}`} />}
              <h3 className="modal-title">{props.title || ''}</h3>

              {props.url && props.urlOpenInNew && (
                <a className="close s fs-18" href={props.url} target="_blank" title={$L('在新页面打开')}>
                  <span className="zmdi zmdi-open-in-new" />
                </a>
              )}
              {this.props.maximize && (
                <button
                  className="close md-close J_maximize"
                  type="button"
                  title={this.state._maximize ? $L('向下还原') : $L('最大化')}
                  onClick={() => this.setState({ _maximize: !this.state._maximize })}
                  style={{ marginTop: -9 }}>
                  <span className={`mdi ${this.state._maximize ? 'mdi mdi-window-restore' : 'mdi mdi-window-maximize'}`} />
                </button>
              )}
              <button className="close" type="button" onClick={() => this.hide()} title={$L('关闭')}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>

            {this.renderContent()}
          </div>
        </div>
      </div>
    )
  }

  renderContent() {
    const iframe = !this.props.children // No child
    return (
      <div className={`modal-body ${iframe ? 'iframe rb-loading' : ''} ${iframe && this.state.frameLoad !== false ? 'rb-loading-active' : ''}`}>
        {this.props.children || <iframe src={this.props.url} frameBorder="0" scrolling="no" onLoad={() => this.resize()} />}
        {iframe && <RbSpinner />}
      </div>
    )
  }

  componentDidMount() {
    const $root = $(this._rbmodal)
      .modal({
        show: true,
        backdrop: this.props.backdrop === false ? false : 'static',
        keyboard: false,
      })
      .on('hidden.bs.modal', () => {
        $keepModalOpen()
        if (this.props.disposeOnHide === true) {
          $root.modal('dispose')
          $unmount($root.parent(), 0, null, this.props.__root18)
        }
      })
  }

  show() {
    $(this._rbmodal).modal('show')
    typeof this.props.onShow === 'function' && this.props.onShow(this)
  }

  hide() {
    $(this._rbmodal).modal('hide')
    typeof this.props.onHide === 'function' && this.props.onHide(this)
  }

  resize() {
    if (this.props.children) return

    const $root = $(this._rbmodal)
    $setTimeout(
      () => {
        const $iframe = $root.find('iframe')
        let height = $iframe.contents().find('.main-content').outerHeight()
        if (height === 0) height = $iframe.contents().find('body').height()
        // else height += 45 // .main-content's padding
        $root.find('.modal-body').height(height)
        this.setState({ frameLoad: false })
      },
      20,
      'RbModal-resize'
    )
  }

  // -- Usage
  /**
   * @param {*} url
   * @param {*} title
   * @param {*} option
   */
  static create(url, title, option) {
    // URL prefix
    if (url.substr(0, 1) === '/' && rb.baseUrl) url = rb.baseUrl + url

    option = option || {}
    option.disposeOnHide = option.disposeOnHide === true // default false
    this.__HOLDERs = this.__HOLDERs || {}

    const that = this
    if (option.disposeOnHide === false && !!that.__HOLDERs[url]) {
      that.__HOLDER = that.__HOLDERs[url]
      that.__HOLDER.show()
      that.__HOLDER.resize()
    } else {
      renderRbcomp(<RbModal url={url} urlOpenInNew={option.urlOpenInNew} title={title} width={option.width} disposeOnHide={option.disposeOnHide} zIndex={option.zIndex} />, function () {
        that.__HOLDER = this
        if (option.disposeOnHide === false) that.__HOLDERs[url] = this
      })
    }
  }

  /**
   * @param {*} url
   */
  static hide(url) {
    this.__HOLDERs = this.__HOLDERs || {}
    if (url) {
      const found = this.__HOLDERs[rb.baseUrl + url] || this.__HOLDERs[url]
      found && found.hide()
    } else if (this.__HOLDER) {
      this.__HOLDER.hide()
    }
  }

  /**
   * @param {*} url
   */
  static resize(url) {
    this.__HOLDERs = this.__HOLDERs || {}
    if (url) this.__HOLDERs[url] && this.__HOLDERs[url].resize()
    else if (this.__HOLDER) this.__HOLDER.resize()
  }
}

// ~~ Modal 处理器
class RbModalHandler extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  show = (state, cb) => {
    const callback = () => {
      // eslint-disable-next-line react/no-string-refs
      const dlg = this._dlg || this.refs['dlg']
      if (dlg) dlg.show()
      typeof cb === 'function' && cb(this)
    }
    if (state && $type(state) === 'object') this.setState(state, callback)
    else callback()
  }

  hide = (e) => {
    if (e && e.target && $(e.target).attr('disabled')) return
    // eslint-disable-next-line react/no-string-refs
    const dlg = this._dlg || this.refs['dlg']
    if (dlg) dlg.hide()
  }
}

// ~~ FormModal 处理器
class RbFormHandler extends RbModalHandler {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  handleChange = (e, call) => {
    const target = e.target
    const name = target.dataset.id || target.name
    if (!name) return

    const val = target.type === 'checkbox' ? target.checked : target.value
    const s = {}
    s[name] = val
    this.setState(s, call)
    this.handleChangeAfter(name, val)
  }

  handleChangeAfter(name, value) {
    // NOOP
  }

  componentWillUnmount() {
    // destroy select2
    if (this.__select2) {
      if (Array.isArray(this.__select2)) {
        this.__select2.forEach(function (s) {
          s.select2('destroy')
        })
      } else {
        this.__select2.select2('destroy')
      }
      this.__select2 = null
    }
  }

  disabled(d, preventHide) {
    this._dlg && _preventHide(d, preventHide, this._dlg._element)

    if (d === true) $(this._btns).find('.btn').button('loading')
    else $(this._btns).find('.btn').button('reset')
  }
}

// ~~ 提示框
class RbAlert extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    const style2 = {}
    if (this.props.width) style2.maxWidth = ~~this.props.width

    return (
      <div
        className="modal rbalert"
        ref={(c) => {
          this._dlg = c
          this._element = c
        }}>
        <div className="modal-dialog modal-dialog-centered" style={style2}>
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()} title={`${$L('关闭')} (ESC)`}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">{this.renderContent()}</div>
          </div>
        </div>
      </div>
    )
  }

  renderContent() {
    const type = this.props.type || 'primary'
    let icon = this.props.icon
    if (!icon) icon = type === 'danger' ? 'alert-triangle' : type === 'primary' ? 'help-outline' : 'alert-circle-o'

    const _onCancel = (this.props.onCancel || this.props.cancel || this.hide).bind(this)
    const _onConfirm = (this.props.onConfirm || this.props.confirm || this.hide).bind(this)

    return (
      <div className="text-center ml-6 mr-6">
        {type !== 'clear' && (
          <div className={`text-${type}`}>
            <i className={`modal-main-icon zmdi zmdi-${icon}`} />
          </div>
        )}

        {this.props.title && <h4 className="mb-2 mt-3">{this.props.title}</h4>}
        <div className={this.props.title ? '' : 'mt-3'}>
          <div>{this.props.message}</div>
        </div>

        {type !== 'clear' && (
          <div className="mt-4 mb-3">
            <button disabled={this.state.disable} className="btn btn-space btn-secondary" type="button" onClick={_onCancel}>
              {this.props.cancelText || $L('取消')}
            </button>
            <button disabled={this.state.disable} className={`btn btn-space btn-${type}`} type="button" onClick={_onConfirm} ref={(c) => (this._$btn = c)}>
              {this.props.confirmText || $L('确定')}
            </button>
          </div>
        )}
      </div>
    )
  }

  componentDidMount() {
    const $root = $(this._dlg)
      .modal({ show: true, keyboard: true })
      .on('hidden.bs.modal', function () {
        $keepModalOpen()
        $root.modal('dispose')
        $unmount($root.parent())
      })

    // z-index
    setTimeout(() => {
      const mb = $('.modal-backdrop.show')
      if (mb.length > 1) $(mb[mb.length - 1]).addClass('rbalert')
    }, 0)

    if (this.props.countdown > 0) {
      $countdownButton($(this._$btn), this.props.countdown)
    }
  }

  hide(forceHide) {
    if (forceHide) $(this._dlg).off('hide.bs.modal')
    $(this._dlg).modal('hide')
  }

  disabled(d, preventHide) {
    d = d === true
    // 带有 tabIndex=-1 导致 select2 组件搜索框无法搜索???
    this.setState({ disable: d }, () => _preventHide(d, preventHide, this._dlg))
  }

  // -- Usage
  /**
   * @param {*} message
   * @param {*} titleOrOption
   * @param {*} option
   */
  static create(message, titleOrOption, option) {
    if (typeof titleOrOption === 'object') {
      option = titleOrOption
      titleOrOption = null
    }

    option = option || {}
    const props = { ...option, title: titleOrOption, message: message }
    renderRbcomp(<RbAlert {...props} />, null, option.onRendered || option.call)
  }
}

function _preventHide(d, preventHide, dlg) {
  if (d && preventHide) {
    $(dlg).find('.close').attr('disabled', true)
    $(dlg)
      .off('hide.bs.modal')
      .on('hide.bs.modal', function () {
        if (event && event.target && $(event.target).hasClass('zmdi-close')) {
          RbHighbar.create($L('请等待请求执行完毕'))
        }
        return false
      })
  } else if (!d) {
    $(dlg).find('.close').attr('disabled', false)
    $(dlg)
      .off('hide.bs.modal')
      .on('hide.bs.modal', function () {
        return true
      })
  }
}

// ~~ 顶部提示条
class RbHighbar extends React.Component {
  constructor(props) {
    super(props)
    this.state = { animatedClass: 'slideInDown' }
    const n = $('.rbhighbar').length
    if (n > 0 && n < 5) this._offsetTop = n * 62
  }

  render() {
    let icon = this.props.type === 'success' ? 'check' : 'info-outline'
    icon = this.props.type === 'danger' ? 'close-circle-o' : icon

    return (
      <div ref={(c) => (this._element = c)} className={`rbhighbar animated faster ${this.state.animatedClass}`} style={{ top: this._offsetTop || 0 }}>
        <div className={`alert alert-dismissible alert-${this.props.type || 'warning'} mb-0`}>
          <button className="close" type="button" onClick={this.close} title={$L('关闭')}>
            <i className="zmdi zmdi-close" />
          </button>
          <div className="icon">
            <i className={`zmdi zmdi-${icon}`} />
          </div>
          <div className="message pl-0">{this.props.message}</div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    setTimeout(() => this.close(), this.props.timeout || 3000)
  }

  close = () => {
    this.setState({ animatedClass: 'fadeOut' })
    setTimeout(() => $unmount($(this._element).parent(), 20), 200)
  }

  // -- Usage
  /**
   * @param {*} message
   * @param {*} option
   */
  static create(message, option) {
    if (top !== self && parent.RbHighbar) {
      parent.RbHighbar.create(message, option)
    } else {
      option = option || {}
      renderRbcomp(<RbHighbar message={message} type={option.type} timeout={option.timeout} />)
    }
  }

  /**
   * @param {*} message
   * @param {*} option
   */
  static createl(message, option) {
    return RbHighbar.create($L(message), option)
  }

  /**
   * @param {*} message
   */
  static success(message) {
    RbHighbar.create(message || $L('操作成功'), { type: 'success' })
  }

  /**
   * @param {*} message
   */
  static error(message) {
    RbHighbar.create(message || $L('系统繁忙，请稍后重试'), { type: 'danger', timeout: 5000 })
  }
}

// ~~ 提示条
class RbAlertBox extends React.Component {
  render() {
    const props = this.props
    const type = (props || {}).type || 'warning'
    let icon = props.icon
    if (!icon) icon = type === 'success' ? 'check' : type === 'danger' ? 'close-circle-o' : 'info-outline'

    return (
      <div className={`alert alert-icon alert-icon-border alert-dismissible alert-sm alert-${type} ${props.className || ''}`} ref={(c) => (this._element = c)}>
        <div className="icon">
          <i className={`zmdi zmdi-${icon}`} />
        </div>
        <div className="message">
          <a className="close" onClick={() => this._handleClose()} title={$L('关闭')} data-dismiss="alert">
            <i className="zmdi zmdi-close" />
          </a>
          <div>{props.message || 'INMESSAGE'}</div>
        </div>
      </div>
    )
  }

  _handleClose() {
    // $unmount($(this._element).parent(), 10, true)
    typeof this.props.onClose === 'function' && this.props.onClose()
  }
}

// ~~ 加载动画 @see spinner.html
function RbSpinner(props) {
  const spinner = (
    <div className="rb-spinner">
      {$.browser.msie ? (
        <span className="spinner-border spinner-border-xl text-primary" />
      ) : (
        <svg width="40px" height="40px" viewBox="0 0 66 66" xmlns="http://www.w3.org/2000/svg">
          <circle fill="none" strokeWidth="4" strokeLinecap="round" cx="33" cy="33" r="30" className="circle" />
        </svg>
      )}
    </div>
  )

  if (props && props.fully === true) return <div className="rb-loading rb-loading-active">{spinner}</div>
  return spinner
}

// ~~ 用户选择器
class UserSelector extends React.Component {
  constructor(props) {
    super(props)
    this.state = { selected: props.selected || [] }

    this._cached = {}
    this._useTabs = []
    if (props.hideUser !== true) this._useTabs.push(['User', $L('用户')])
    if (props.hideDepartment !== true) this._useTabs.push(['Department', $L('部门')])
    if (props.hideRole !== true && rb.isAdminUser) this._useTabs.push(['Role', $L('角色')]) // v3.2 Only admin
    if (props.hideTeam !== true) this._useTabs.push(['Team', $L('团队')])

    // 默认显示
    this.state.tabType = this._useTabs[0] ? this._useTabs[0][0] : null
  }

  render() {
    let inResult
    if (!this.state.items) {
      inResult = <li className="select2-results__option un-hover text-muted">{$L('搜索中')}</li>
    } else if (this.state.items.length === 0) {
      inResult = <li className="select2-results__option un-hover">{$L('未找到结果')}</li>
    } else {
      inResult = this.state.items.map((item) => {
        return (
          <li key={item.id} className="select2-results__option" data-id={item.id} onClick={(e) => this.clickItem(e)}>
            <i className={`zmdi ${!this.props.hideSelection && this.containsItem(item.id) ? ' zmdi-check' : ''}`} />
            {this.state.tabType === 'User' && <img src={`${rb.baseUrl}/account/user-avatar/${item.id}`} className="avatar" alt="Avatar" />}
            <span className="text">{item.text}</span>
          </li>
        )
      })
    }

    const dropdownMenu = (
      <div className="dropdown-menu">
        <div className="selector-search">
          <div>
            <input
              type="search"
              className="form-control search"
              placeholder={$L('输入关键词搜索')}
              value={this.state.query || ''}
              ref={(c) => (this._$input = c)}
              onChange={(e) => this.search(e)}
              onKeyDown={(e) => this._keyEvent(e)}
            />
          </div>
        </div>
        <div className="tab-container m-0">
          <ul className={`nav nav-tabs nav-tabs-classic ${this._useTabs.length < 2 ? 'hide' : ''} ${this._useTabs.length > 4 ? 'w5' : ''}`}>
            {this._useTabs.map((item) => {
              return (
                <li className="nav-item" key={`t-${item[0]}`}>
                  <a onClick={() => this.switchTab(item[0])} className={`nav-link ${this.state.tabType === item[0] ? ' active' : ''}`}>
                    {item[1]}
                  </a>
                </li>
              )
            })}
          </ul>
          <div className="tab-content">
            <div className="tab-pane active">
              <div className="rb-scroller" ref={(c) => (this._$scroller = c)}>
                <ul className="select2-results__options">{inResult}</ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    )

    // Mix select2 and dropdown
    // data-reference="parent" data-offset="1,26"
    return (
      <span className="select2 select2-container select2-container--default user-selector">
        <span className="selection">
          {this.props.compToggle ? (
            <span ref={(c) => (this._$dropdownParent = c)}>
              {this.props.compToggle}
              {dropdownMenu}
            </span>
          ) : (
            <div className="select2-selection select2-selection--multiple">
              <div className="select2-selection__rendered" ref={(c) => (this._$dropdownParent = c)}>
                {this.state.selected.length > 0 && (
                  <span className="select2-selection__clear" onClick={() => this.clearSelection()}>
                    &times;
                  </span>
                )}
                {this.state.selected.map((item) => {
                  return (
                    <span key={item.id} className="select2-selection__choice">
                      <span className="select2-selection__choice__remove" data-id={item.id} onClick={(e) => this.removeItem(e)}>
                        &times;
                      </span>
                      {item.text}
                    </span>
                  )
                })}
                <span className="select2-selection__choice abtn" data-toggle="dropdown">
                  <a>
                    <i className="zmdi zmdi-plus" /> {this.props.multiple === false ? $L('选择') : $L('添加')}
                  </a>
                </span>
                {dropdownMenu}
              </div>
            </div>
          )}
        </span>
      </span>
    )
  }

  componentDidMount() {
    $(this._$scroller).perfectScrollbar()

    const that = this
    $(this._$dropdownParent).on({
      'shown.bs.dropdown': function () {
        // 初始化
        if (!that.state.items) that.switchTab()
        that._$input && that._$input.focus()
        $(that._$scroller).find('li.active').removeClass('active')
      },
      'hide.bs.dropdown': function (e) {
        if (!e.clickEvent || !e.clickEvent.target) return
        const $target = $(e.clickEvent.target)
        if ($target.hasClass('dropdown-menu') || $target.parents('.dropdown-menu').length === 1) {
          if (that.props.multiple === false) {
            if (!($target.hasClass('select2-results__options') || $target.parents('.select2-results__options').length === 1)) return false
          } else {
            return false
          }
        }
      },
    })

    this.props.defaultValue && this._renderValue(this.props.defaultValue)
  }

  _renderValue(value) {
    let s = value
    if ($type(s) === 'string') s = s.split(',')

    $.post('/commons/search/user-selector', JSON.stringify(s), (res) => {
      if (res.error_code === 0 && res.data.length > 0) {
        this.setState({ selected: res.data })
      }
    })
  }

  componentWillUnmount() {
    $(this._$scroller).perfectScrollbar('destroy')
  }

  UNSAFE_componentWillReceiveProps(props) {
    this.setState({ selected: props.selected || this.state.selected })
  }

  clearSelection() {
    this.setState({ selected: [] }, () => {
      typeof this.props.onClearSelection === 'function' && this.props.onClearSelection()
    })
  }

  switchTab(type) {
    type = type || this.state.tabType
    const ckey = `${type}-${this.state.query}`

    this.setState({ tabType: type, items: this._cached[ckey] }, () => {
      if (!this._cached[ckey]) {
        $.get(`/commons/search/users?type=${type}&q=${$encode(this.state.query)}&atall=${!!this.props.requestAtAll}`, (res) => {
          this._cached[ckey] = res.data
          this.switchTab(type)
        })
      }
      $(this._$scroller).perfectScrollbar('update')
    })
  }

  _tryActive($active, $el) {
    if ($el && $el.length === 1) {
      $active.removeClass('active')
      $el.addClass('active')

      const st = $(this._$scroller).scrollTop()
      const et = $el.position().top
      if (et >= 0) {
        const top = et + st - (222 - 36) // maxHeight - elementHeight
        if (top > 0) $(this._$scroller).scrollTop(top)
      } else {
        const top = st + et
        if (top >= 0) $(this._$scroller).scrollTop(top)
      }
    }
  }

  _keyEvent(e) {
    if (e.keyCode === 40) {
      // DOWN
      const $active = $(this._$scroller).find('li.active')
      const $next = $active.length === 0 ? $(this._$scroller).find('li:eq(0)') : $active.next()
      this._tryActive($active, $next)
    } else if (e.keyCode === 38) {
      // UP
      const $active = $(this._$scroller).find('li.active')
      const $prev = $active.length === 0 ? null : $active.prev()
      this._tryActive($active, $prev)
    } else if (e.keyCode === 13) {
      // ENTER
      e.preventDefault()
      const $active = $(this._$scroller).find('li.active')
      if ($active.length === 1) {
        $active.trigger('click')
        $stopEvent(e)
      }
    } else if (e.keyCode === 27) {
      // ESC
      e.preventDefault()
      this.toggle() // hide
      // Auto focus for textarea
      this.props.targetInput && this.props.targetInput.focus()
    }
  }

  search(e) {
    this.setState({ query: e.target.value }, () => {
      $setTimeout(() => this.switchTab(), 300, 'us-search-items')
    })
  }

  clickItem(e, isRemove) {
    const $target = $(e.currentTarget)
    const id = $target.data('id') || $target.parents('.select2-results__option').data('id')

    let exists = false
    let ns = []
    // 单选
    if (this.props.multiple !== false || isRemove) {
      ns = this.state.selected.filter((x) => {
        if (x.id === id) {
          exists = true
          return false
        }
        return true
      })
    }

    const selected = { id: id, text: $target.text() }

    if (!exists) ns.push(selected)
    if (ns.length >= 20) {
      RbHighbar.create($L('最多选择 20 项'))
      return false
    }

    this.setState({ selected: ns }, () => {
      typeof this.props.onSelectItem === 'function' && this.props.onSelectItem(selected, isRemove)
    })
  }

  removeItem(e) {
    this.clickItem(e, true)
  }

  containsItem(id) {
    return !!this.state.selected.find((x) => x.id === id)
  }

  getSelected() {
    const ids = []
    this.state.selected.forEach((item) => ids.push(item.id))
    return ids
  }

  val() {
    // v3.7 set
    if (arguments[0]) {
      this._renderValue(arguments[0])
      return
    }
    return this.getSelected()
  }

  toggle() {
    // $(this._$dropdownParent).dropdown('toggle')
    $(this._$dropdownParent).find('[data-toggle="dropdown"]').dropdown('toggle')
  }
}

// ~~ 用户显示
const UserShow = function (props) {
  const viewUrl = props.id && props.noLink !== true ? `#!/View/User/${props.id}` : null
  const avatarUrl = `${rb.baseUrl}/account/user-avatar/${props.id}`

  return (
    <a href={viewUrl} className="user-show" title={props.name} onClick={props.onClick}>
      <div className={`avatar ${props.showName === true ? ' float-left' : ''}`}>{props.icon ? <i className={props.icon} /> : <img src={avatarUrl} alt="Avatar" />}</div>
      {props.showName && (
        <div className={`text-truncate name ${props.deptName ? 'vm' : ''}`}>
          {props.name}
          {props.deptName && <em>{props.deptName}</em>}
        </div>
      )}
    </a>
  )
}

// ~~ 日期显示
const DateShow = function ({ date, title }) {
  return date ? <span title={title || date}>{$fromNow(date)}</span> : null
}

// ~~ 任意记录选择
// @see rb-page.js#$initReferenceSelect2
class AnyRecordSelector extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
    this.__select2 = []
  }

  render() {
    return (
      <div className="row">
        <div className="col-4 pr-0">
          <select className="form-control form-control-sm" ref={(c) => (this._$entity = c)}>
            {(this.state.entities || []).map((item) => {
              if ($isSysMask(item.label)) return null
              return (
                <option key={item.name} value={item.name}>
                  {item.label}
                </option>
              )
            })}
          </select>
        </div>
        <div className="col-8 pl-2">
          <select className="form-control form-control-sm float-left" ref={(c) => (this._$record = c)} />
        </div>
      </div>
    )
  }

  componentDidMount() {
    $.get('/commons/metadata/entities', (res) => {
      if ((res.data || []).length === 0) $(this._$record).attr('disabled', true)

      this.setState({ entities: res.data || [] }, () => {
        const s2 = $(this._$entity)
          .select2({
            placeholder: $L('无可用实体'),
            allowClear: false,
          })
          .on('change', () => {
            $(this._$record).val(null).trigger('change')
          })
        this.__select2.push(s2)

        // 编辑时
        const iv = this.props.initValue
        if (iv) {
          $(this._$entity).val(iv.entity).trigger('change')
          const option = new Option(iv.text, iv.id, true, true)
          $(this._$record).append(option)
        }
      })
    })

    const that = this
    let search_input = null
    const s2 = $(this._$record)
      .select2({
        placeholder: `${$L('选择记录')}`,
        minimumInputLength: 0,
        maximumSelectionLength: 1,
        ajax: {
          url: '/commons/search/search',
          delay: 300,
          data: function (params) {
            search_input = params.term
            return {
              entity: $(that._$entity).val(),
              q: params.term,
            }
          },
          processResults: function (data) {
            return {
              results: data.data,
            }
          },
        },
        language: {
          noResults: () => {
            return $trim(search_input).length > 0 ? $L('未找到结果') : $L('输入关键词搜索')
          },
          inputTooShort: () => {
            return $L('输入关键词搜索')
          },
          searching: () => {
            return $L('搜索中')
          },
          maximumSelected: () => {
            return $L('只能选择 1 项')
          },
        },
      })
      .on('change', (e) => {
        typeof that.props.onSelect === 'function' && that.props.onSelect(e.target.value)
      })
    this.__select2.push(s2)
  }

  // return `id`
  val() {
    return $(this._$record).val()
  }

  // return `{ id:xx, text:xx, entity:xx }`
  value() {
    const val = this.val()
    if (!val) return null

    return {
      entity: $(this._$entity).val(),
      id: val,
      text: $(this._$record).select2('data')[0].text,
    }
  }

  reset() {
    $(this._$record).val(null).trigger('change')
  }

  componentWillUnmount() {
    this.__select2.forEach(function (s) {
      s.select2('destroy')
    })
  }
}

// ~~ 默认 SimpleMDE 工具栏
const DEFAULT_MDE_TOOLBAR = (c) => {
  return [
    {
      name: 'bold',
      action: SimpleMDE.toggleBold,
      className: 'zmdi zmdi-format-bold',
      title: $L('粗体'),
    },
    {
      name: 'italic',
      action: SimpleMDE.toggleItalic,
      className: 'zmdi zmdi-format-italic',
      title: $L('斜体'),
    },
    {
      name: 'strikethrough',
      action: SimpleMDE.toggleStrikethrough,
      className: 'zmdi zmdi-format-strikethrough',
      title: $L('删除线'),
    },
    {
      name: 'heading',
      action: SimpleMDE.toggleHeadingSmaller,
      className: 'zmdi zmdi-format-size',
      title: $L('标题'),
    },
    {
      name: 'unordered-list',
      action: SimpleMDE.toggleUnorderedList,
      className: 'zmdi zmdi-format-list-bulleted',
      title: $L('列表'),
    },
    {
      name: 'ordered-list',
      action: SimpleMDE.toggleOrderedList,
      className: 'zmdi zmdi-format-list-numbered',
      title: $L('数字列表'),
    },
    {
      name: 'link',
      action: SimpleMDE.drawLink,
      className: 'zmdi zmdi-link',
      title: $L('链接'),
    },
    {
      name: 'image',
      action: () => c && c._fieldValue__upload && c._fieldValue__upload.click(),
      className: 'zmdi zmdi-image-o',
      title: $L('图片'),
    },
    {
      name: 'table',
      action: SimpleMDE.drawTable,
      className: 'zmdi zmdi-border-all',
      title: $L('表格'),
    },
    '|',
    {
      name: 'preview',
      action: SimpleMDE.togglePreview,
      className: 'zmdi zmdi-eye no-disable',
      title: $L('预览'),
    },
    {
      name: 'fullscreen',
      action: SimpleMDE.toggleFullScreen,
      className: 'zmdi zmdi-fullscreen no-disable',
      title: $L('全屏'),
    },
    {
      name: 'guide',
      action: () => window.open('https://getrebuild.com/docs/markdown-guide'),
      className: 'zmdi zmdi-help-outline no-disable',
      title: $L('编辑器帮助'),
    },
  ]
}

function UserPopup({ info }) {
  return (
    <div className="user-popup">
      <div className="avatar">
        <img src={`${rb.baseUrl}/account/user-avatar/${info.id}`} alt="Avatar" />
      </div>
      <div className="infos">
        <strong>{info.name}</strong>
        {info.dept && <p className="text-muted fs-12">{info.dept}</p>}
        {info.email && (
          <p className="email text-ellipsis" title={info.email}>
            {info.email}
          </p>
        )}
        {info.phone && (
          <p className="phone text-ellipsis" title={info.phone}>
            {info.phone}
          </p>
        )}
      </div>
    </div>
  )
}

UserPopup.create = function (el) {
  const uid = $(el).data('uid')
  if (!uid) {
    console.warn('No attr `data-id` defined')
    return
  }

  function _clear() {
    if (UserPopup.__timer) {
      clearTimeout(UserPopup.__timer)
      UserPopup.__timer = null
    }
  }

  function _evtLeave() {
    _clear()
    UserPopup.__timer2 = setTimeout(() => {
      if (UserPopup.__$target) {
        $unmount(UserPopup.__$target, 20)
        UserPopup.__$target = null
      }
    }, 200)
  }

  $(el).on({
    mouseover: function (e) {
      _clear()
      const pos = { top: Math.max(e.clientY - 90, 0), left: Math.max(e.clientX - 140, 0), display: 'block' }
      pos.top = $(this).position().top - $(window).scrollTop() - 10

      UserPopup.__timer = setTimeout(function () {
        $.get(`/account/user-info?id=${uid}`, (res) => {
          if (UserPopup.__timer) {
            UserPopup.__$target = renderRbcomp(<UserPopup info={{ ...res.data, id: uid }} />)

            const $popup = $(UserPopup.__$target).find('.user-popup').css(pos)
            $popup.on({
              mouseover: function () {
                if (UserPopup.__timer2) {
                  clearTimeout(UserPopup.__timer2)
                  UserPopup.__timer2 = null
                }
              },
              mouseleave: _evtLeave,
            })
          }
        })
      }, 400)
    },
    mouseleave: _evtLeave,
  })
}

// ~~ HTML 内容
const WrapHtml = (htmlContent) => <span dangerouslySetInnerHTML={{ __html: htmlContent }} />

// ~~ MD > HTML
class Md2Html extends React.Component {
  constructor(props) {
    super(props)
    this.state = { md2html: SimpleMDE.prototype.markdown(props.markdown) }
  }

  render() {
    return <span ref={(c) => (this._$md2html = c)} dangerouslySetInnerHTML={{ __html: this.state.md2html }} />
  }

  componentDidMount() {
    let cHtml = SimpleMDE.prototype.markdown(this.props.markdown)
    cHtml = cHtml.replace(/<img src="([^"]+)"/g, function (s, src) {
      let srcNew = src + (src.includes('?') ? '&' : '?') + 'imageView2/2/w/1000/interlace/1/q/100'
      return s.replace(src, srcNew)
    })

    this.setState({ md2html: cHtml }, () => {
      $(this._$md2html)
        .find('a')
        .each(function () {
          const $this = $(this)
          $this.attr({
            href: `${rb.baseUrl}/commons/url-safe?url=${encodeURIComponent($this.attr('href'))}`,
            target: '_blank',
          })
          $this.on('click', (e) => {
            $stopEvent(e, false)
          })
        })
      // FIXME
      // $(this._$md2html)
      //   .find('img')
      //   .on('click', function () {
      //     const p = parent || window
      //     p.RbPreview.create($(this).attr('src'))
      //   })
    })
  }
}

// ~~ short React.Fragment
const RF = ({ children }) => <React.Fragment>{children}</React.Fragment>

class RbGritter extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    return (
      <div id="gritter-notice-wrapper" className="gritter-main-wrapper">
        {this.state.items}
      </div>
    )
  }

  _addItem(message, options) {
    const itemid = `gritter-item-${$random()}`

    const type = options.type || 'success'
    const icon = options.icon || (type === 'success' ? 'check' : type === 'danger' ? 'close-circle-o' : 'info-outline')

    const item = (
      <div key={itemid} id={itemid} className={`gritter-item-wrapper color ${type} animated faster fadeInRight`}>
        <div className="gritter-item">
          <div className="gritter-img-container">
            <span className="gritter-image">
              <i className={`icon zmdi zmdi-${icon}`} />
            </span>
          </div>
          <div className="gritter-content gritter-with-image">
            <a
              className="gritter-close"
              onClick={(e) => {
                $stopEvent(e, true)
                this._removeItem(itemid)
                typeof options.onCancel === 'function' && options.onCancel()
              }}
              title={$L('关闭')}
            />
            {options.title && <span className="gritter-title">{options.title}</span>}
            <p>{message}</p>
          </div>
        </div>
      </div>
    )

    const itemsNew = this.state.items || []
    itemsNew.push(item)
    this.setState({ items: itemsNew }, () => {
      setTimeout(() => this._removeItem(itemid), options.timeout || 6000)
    })
  }

  _removeItem(itemid) {
    const itemsNew = this.state.items.filter((item) => itemid !== item.key)
    this.setState({ items: itemsNew })
  }

  destory() {
    this.setState({ items: [] }, () => {
      $('#gritter-notice-wrapper').remove()
    })
  }

  // -- Usage
  /**
   * @param {*} message
   * @param {*} options
   */
  static create(message, options = {}) {
    if (top !== self && parent.RbGritter) {
      parent.RbGritter.create(message, options)
    } else {
      if (this._RbGritter) {
        this._RbGritter._addItem(message, options)
      } else {
        const that = this
        renderRbcomp(<RbGritter />, function () {
          that._RbGritter = this
          that._RbGritter._addItem(message, options)
        })
      }
    }
  }

  static destory() {
    if (this._RbGritter) {
      this._RbGritter.destory()
      this._RbGritter = null
    }
  }
}

// ~~ 代码查看
class CodeViewport extends React.Component {
  render() {
    return (
      <div className="code-viewport">
        <pre ref={(c) => (this._$code = c)}>Loading</pre>
        {window.ClipboardJS && (
          <a className="copy" title={$L('复制')} ref={(c) => (this._$copy = c)}>
            <i className="icon zmdi zmdi-copy" />
          </a>
        )}
      </div>
    )
  }

  componentDidMount() {
    this._$code.innerHTML = $formattedCode(this.props.code || '')

    if (this._$copy) {
      const that = this
      const $copy = $(this._$copy).on('mouseenter', () => $(this._$copy).removeClass('copied-check'))
      // eslint-disable-next-line no-undef
      new ClipboardJS($copy[0], {
        text: function () {
          return $(that._$code).text()
        },
      }).on('success', () => $copy.addClass('copied-check'))
    }
  }

  UNSAFE_componentWillReceiveProps(newProps) {
    if (newProps.code) this._$code.innerHTML = $formattedCode(newProps.code)
  }
}

// ~~ 树组件 v2.5 v3.7
// TODO 子级延迟渲染
class AsideTree extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, expandItems: [] }
  }

  render() {
    return <div className={`aside-2tree ${this.props.hideCollapse ? 'hide-collapse' : ''}`}>{this.renderTree(this.props.data || [])}</div>
  }

  renderTree(items, item) {
    return (
      <ul className={`list-unstyled m-0 ${item && !this.state.expandItems.contains(item.id) ? 'hide' : ''}`}>
        {items.map((item) => {
          let $children = null
          if (item.children && item.children.length > 0) {
            $children = this.renderTree(item.children, item)
          }
          const $item = this.renderItem(item, $children !== null)
          return (
            <RF key={item.id}>
              {$item}
              {$children}
            </RF>
          )
        })}
      </ul>
    )
  }

  renderItem(item, hasChild) {
    return (
      <li className={this.state.activeItem === item.id ? 'active' : ''}>
        <span
          className={`collapse-icon ${hasChild ? '' : 'no-child'}`}
          onClick={() => {
            if (hasChild) {
              const expandItemsNew = this.state.expandItems
              expandItemsNew.toggle(item.id)
              this.setState({ expandItems: expandItemsNew })
            }
          }}>
          <i className={`zmdi zmdi-chevron-right ${this.state.expandItems.contains(item.id) ? 'open' : ''} `} />
        </span>
        <a
          data-id={item.id}
          className={`text-ellipsis ${item.disabled ? 'text-disabled' : ''}`}
          title={item.disabled ? $L('已禁用') : null}
          onClick={() => {
            this.setState({ activeItem: item.id }, () => {
              typeof this.props.onItemClick === 'function' && this.props.onItemClick(item)
            })
          }}>
          {this.props.icon && <i className={`icon ${this.props.icon}`} />}
          {item.text || item.name}

          {item.private === true && <i className="icon flag zmdi zmdi-lock" title={$L('私有')} />}
          {!!item.specUsers && <i className="icon flag zmdi zmdi-account" title={$L('指定用户')} />}
        </a>
        {typeof this.props.extrasAction === 'function' && this.props.extrasAction(item)}
      </li>
    )
  }

  refresh(data) {
    this.setState({ data: data })
  }

  // 获取所有子级 ID
  static findAllChildIds(item) {
    function _find(x, into) {
      into.push(x.id)
      if (x.children && x.children.length > 0) {
        x.children.forEach((xx) => _find(xx, into))
      }
    }

    const s = []
    _find(item, s)
    return s
  }
}

/**
 * JSX 组件渲染
 *
 * @param {*} JSX
 * @param {*} container id or object of element (or function of callback)
 * @param {*} callback callback on mounted
 * @param {*} v18
 */
const renderRbcomp = function (JSX, container, callback, v18) {
  if (typeof container === 'function') {
    callback = container
    container = null
  }

  container = container || $random('react-container-', true, 32)
  if (typeof container === 'string') {
    const c = document.getElementById(container)
    if (c) {
      container = c
    } else {
      if (container.startsWith('react-container-')) container = $(`<div id="${container}"></div>`).appendTo(document.body)[0]
      else throw 'No element found : ' + container
    }
  } else if (container instanceof $) {
    container = container[0]
  }

  if (v18 && !!ReactDOM.createRoot) {
    const root = ReactDOM.createRoot(container)
    const JSX18 = React.cloneElement(JSX, { __root18: root })
    root.render(JSX18)
    return root
  }

  ReactDOM.render(JSX, container, callback)
  return container
}

// 渲染可重用组件
const __DLGCOMPS = {}
const renderDlgcomp = function (JSX, id) {
  if (__DLGCOMPS[id]) {
    __DLGCOMPS[id].show()
  } else {
    renderRbcomp(JSX, function () {
      __DLGCOMPS[id] = this
    })
  }
}

// for: React v18
const renderRbcomp18 = function (JSX, container) {
  return renderRbcomp(JSX, container, null, true)
}
