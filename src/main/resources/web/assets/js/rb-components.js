/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */
/* global EasyMDE */

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
      style2.maxWidth = this.state._maximize ? '100%' : null
      if (!style2.maxWidth && props.width) style2.maxWidth = ~~props.width
    }

    let modalClazz = props.useWhite ? 'modal rbmodal use-white' : `modal rbmodal colored-header colored-header-${props.colored || 'primary'}`
    let modalDialogClazz42 = `modal-dialog ${props.useWhite && 'modal-xl'} ${props.className || ''} ${this.state._maximize && 'modal-dialog-maximize'}`
    if (props.useScrollable) modalDialogClazz42 += ' modal-dialog-scrollable'

    return (
      <div
        className={modalClazz}
        style={props.zIndex ? { zIndex: props.zIndex } : null}
        aria-modal="true"
        tabIndex="-1"
        ref={(c) => {
          this._rbmodal = c
          this._element = c
        }}>
        <div className={modalDialogClazz42} style={style2}>
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
                <a className="close s fs-17" href={props.url} target="_blank" title={$L('在新页面打开')}>
                  <span className="zmdi zmdi-open-in-new down-2" />
                </a>
              )}
              {this.props.maximize && (
                <button
                  className="close md-close J_maximize"
                  type="button"
                  title={this.state._maximize ? $L('向下还原') : $L('最大化')}
                  onClick={() => this.setState({ _maximize: !this.state._maximize })}
                  style={{ marginTop: -9 }}>
                  <span className="mdi mdi-window-maximize" />
                </button>
              )}
              <button className="close" type="button" onClick={() => this.hide()} title={`${$L('关闭')} (Esc)`}>
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
        keyboard: true,
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
    if (this.__select2) {
      __destroySelect2(this.__select2)
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
    const style1 = {}
    const style2 = {}
    if (this.props.zIndex) style1.zIndex = this.props.zIndex
    if (this.props.width) style2.maxWidth = ~~this.props.width

    return (
      <div
        className={`modal rbalert ${this.props.className || ''}`}
        aria-modal="true"
        style={style1}
        tabIndex="-1"
        ref={(c) => {
          this._dlg = c
          this._element = c
        }}>
        <div className="modal-dialog modal-dialog-centered" style={style2}>
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()} title={`${$L('关闭')} (Esc)`}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">{this.props.type === 'clear' ? this.props.message : this.renderContent()}</div>
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
        <div className={`text-${type}`}>
          <i className={`modal-main-icon zmdi zmdi-${icon}`} />
        </div>

        {this.props.title && <h4 className="mb-2 mt-3">{this.props.title}</h4>}
        <div className={this.props.title ? '' : 'mt-3'}>
          <div>{this.props.message}</div>
        </div>

        <div className="mt-4 mb-3">
          <button disabled={this.state.disabled} className="btn btn-space btn-secondary" type="button" onClick={_onCancel} ref={(c) => (this._$cancel = c)}>
            {this.props.cancelText || $L('取消')}
          </button>
          <button disabled={this.state.disabled} className={`btn btn-space btn-${type}`} type="button" onClick={_onConfirm} ref={(c) => (this._$btn = c)}>
            {this.props.confirmText || $L('确定')}
          </button>
        </div>
      </div>
    )
  }

  componentDidMount() {
    const that = this
    const $root = $(this._dlg)
      .modal({ show: true, keyboard: true })
      .on('hidden.bs.modal', function () {
        $keepModalOpen()
        $root.modal('dispose')
        $unmount($root.parent())
        // v4.2
        typeof that.props.onHide === 'function' && that.props.onHide()
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

  componentWillUnmount() {
    if (this.__select2) {
      __destroySelect2(this.__select2)
      this.__select2 = null
    }
  }

  hide(forceHide) {
    if (forceHide) $(this._dlg).off('hide.bs.modal')
    $(this._dlg).modal('hide')
  }

  disabled(d, preventHide) {
    d = d === true
    // 带有 tabIndex=-1 导致 select2 组件搜索框无法搜索???
    this.setState({ disabled: d }, () => _preventHide(d, preventHide, this._dlg))
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
    setTimeout(() => {
      $('.rbhighbar').each(function () {
        const top = ~~($(this).css('top') || '0').replace('px', '')
        if (top >= 62) $(this).animate({ top: top - 62 }, 100)
      })
    }, 100)
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
      <div className={`alert alert-icon alert-icon-border alert-sm alert-${type} ${props.unclose ? '' : 'alert-dismissible'} ${props.className || ''}`} ref={(c) => (this._element = c)}>
        <div className="icon">
          <i className={`zmdi zmdi-${icon}`} />
        </div>
        <div className="message">
          {props.unclose ? null : (
            <a className="close" onClick={() => this._handleClose()} title={$L('关闭')} data-dismiss="alert">
              <i className="zmdi zmdi-close" />
            </a>
          )}
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
                {this.props.allowClear !== false && this.state.selected.length > 0 && (
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
class DateShow extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    const date = this.props.date
    return date ? (
      <span title={this.props.title || date} onClick={() => this.setState({ _showOrigin: !this.state._showOrigin })}>
        {this.props.showOrigin && this.state._showOrigin ? date : $fromNow(date)}
      </span>
    ) : null
  }
}

// ~~ 附件显示
class FileShow extends React.Component {
  render() {
    const file = this.props.file
    const fileName = $fileCutName(file)
    const isImage = $isImage(fileName)
    let imageUrl
    if (isImage) {
      if (file.startsWith('http://') || file.startsWith('https://')) imageUrl = file
      else imageUrl = `${rb.baseUrl}/filex/img/${file}?imageView2/2/w/100/interlace/1/q/100`
    }

    return (
      <div data-key={file} className="img-thumbnail" title={fileName} onClick={() => (parent || window).RbPreview.create(file)}>
        <i className={`file-icon ${isImage && 'image'}`} data-type={$fileExtName(fileName)}>
          {isImage && <img src={imageUrl} />}
        </i>
        <span>{fileName}</span>
        {this.props.removeHandle && (
          <b
            title={$L('移除')}
            onClick={(e) => {
              $stopEvent(e, true)
              this.props.removeHandle(e, file)
            }}>
            <span className="zmdi zmdi-close" />
          </b>
        )}
      </div>
    )
  }
}

// ~~ 记录选择器
// @see rb-page.js#$initReferenceSelect2
class RecordSelector extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    return (
      <div className="input-group has-append">
        <select className="form-control form-control-sm" ref={(c) => (this._$select = c)} />
        <div className="input-group-append">
          <button className="btn btn-secondary" onClick={() => this._showSearcher()} ref={(c) => (this._$btn = c)}>
            <i className="icon zmdi zmdi-search" />
          </button>
        </div>
      </div>
    )
  }

  componentDidMount() {
    this._initSelect2()
  }

  _initSelect2(reset) {
    const props = this.state // use state
    if (!props.entity) return

    if (reset) {
      this.reset()
      this.__select2 && this.__select2.select2('destroy')
      this._ReferenceSearcher && this._ReferenceSearcher.destroy()
      this._ReferenceSearcher = null
    }

    this.__select2 = $initReferenceSelect2(this._$select, {
      searchType: 'search',
      entity: props.entity,
      placeholder: props.entityLabel || null,
    }).on('change', (e) => {
      typeof props.onSelect === 'function' && props.onSelect(e.target.value)
    })
  }

  _showSearcher() {
    const that = this
    window.referenceSearch__call = function (selected) {
      const id = selected[0]
      if ($(that._$select).find(`option[value="${id}"]`).length > 0) {
        that.__select2.val(id).trigger('change')
      } else {
        that._setValue(id)
      }
      that._ReferenceSearcher.hide()
    }

    if (this._ReferenceSearcher) {
      this._ReferenceSearcher.show()
    } else {
      const props = this.state // use state
      const searchUrl = `${rb.baseUrl}/app/entity/reference-search?field=${props.entity}` // be:v4.2 只传实体
      renderRbcomp(<ReferenceSearcher url={searchUrl} title={$L('选择%s', props.entityLabel || '')} useWhite />, function () {
        that._ReferenceSearcher = this
      })
    }
  }

  _setValue(id) {
    $.get(`/commons/search/read-labels?ids=${id}`, (res) => {
      const _data = res.data || {}
      const o = new Option(_data[id], id, true, true)
      this.__select2.append(o).trigger('change')
    })
  }

  // return `id`
  val() {
    return $(this._$select).val()
  }

  // return `{ id:xx, text:xx }`
  getValue() {
    const id = this.val()
    if (id) {
      return {
        id: id,
        text: this.__select2.select2('data')[0].text,
      }
    }
    return null
  }

  setValue(id, text) {
    if (text) {
      const o = new Option(text, id, true, true)
      this.__select2.append(o).trigger('change')
    } else {
      this._setValue(id)
    }
  }

  reset() {
    $(this._$select).val(null).trigger('change')
  }

  componentWillUnmount() {
    this.__select2 && this.__select2.select2('destroy')
  }
}

// ~~ 任意记录选择器
class AnyRecordSelector extends RecordSelector {
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
        <div className="col-8 pl-2">{super.render()}</div>
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount()

    $.get(`/commons/metadata/entities?detail=true&bizz=${this.props.allowBizz || false}`, (res) => {
      let entities = res.data || []
      if (this.props.allowEntities && this.props.allowEntities.length > 0) {
        entities = entities.filter((item) => this.props.allowEntities.includes(item.name))
      }

      this.setState({ entities }, () => {
        this.__select2Entity = $(this._$entity)
          .select2({
            placeholder: $L('无可用'),
            allowClear: false,
            templateResult: function (res) {
              const $span = $('<span class="icon-append"></span>').attr('title', res.text).text(res.text)
              const icon = entities.find((x) => x.entity === res.id)
              $(`<i class="icon zmdi zmdi-${icon ? icon.icon : 'texture'}"></i>`).appendTo($span)
              return $span
            },
          })
          .on('change', (e) => {
            if (e.target.value) {
              this.setState(
                {
                  entity: e.target.value,
                  entityLabel: this.__select2Entity.select2('data')[0].text,
                },
                () => this._initSelect2(true)
              )
            }
          })
        // init
        entities[0] && $(this._$entity).val(entities[0].name).trigger('change')

        // 编辑时
        const iv = this.props.initValue
        if (iv) {
          $(this._$entity).val(iv.entity).trigger('change')
          setTimeout(() => {
            const o = new Option(iv.text, iv.id, true, true)
            $(this._$select).append(o)
          }, 200)
        }
      })
    })
  }

  getValue() {
    let v = super.getValue()
    if (v) {
      v = { ...v, entity: $(this._$entity).val() }
    }
    return v
  }

  componentWillUnmount() {
    super.componentWillUnmount()
    this.__select2Entity && this.__select2Entity.select2('destroy')
  }
}

// ~~ 选择记录
class RecordSelectorModal extends RbAlert {
  renderContent() {
    return (
      <div className="form ml-3 mr-3">
        <div className="form-group">
          <label className="text-bold">{this.props.title || $L('选择记录')}</label>
          <AnyRecordSelector ref={(c) => (this._AnyRecordSelector = c)} allowEntities={this.props.allowEntities} allowBizz={this.props.allowBizz} />
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

  // -- Usage

  /**
   * @param {*} props
   */
  static create(props) {
    renderRbcomp(<RecordSelectorModal {...props} zIndex="1050" />)
  }
}

// ~~ 默认 EasyMDE 工具栏
const DEFAULT_MDE_TOOLBAR = (c) => {
  return [
    {
      name: 'bold',
      action: EasyMDE.toggleBold,
      className: 'zmdi zmdi-format-bold',
      title: $L('粗体'),
    },
    {
      name: 'italic',
      action: EasyMDE.toggleItalic,
      className: 'zmdi zmdi-format-italic',
      title: $L('斜体'),
    },
    {
      name: 'strikethrough',
      action: EasyMDE.toggleStrikethrough,
      className: 'zmdi zmdi-format-strikethrough',
      title: $L('删除线'),
    },
    {
      name: 'heading',
      action: EasyMDE.toggleHeadingSmaller,
      className: 'zmdi zmdi-format-size',
      title: $L('标题'),
    },
    {
      name: 'unordered-list',
      action: EasyMDE.toggleUnorderedList,
      className: 'zmdi zmdi-format-list-bulleted',
      title: $L('列表'),
    },
    {
      name: 'ordered-list',
      action: EasyMDE.toggleOrderedList,
      className: 'zmdi zmdi-format-list-numbered',
      title: $L('数字列表'),
    },
    {
      name: 'link',
      action: EasyMDE.drawLink,
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
      action: EasyMDE.drawTable,
      className: 'zmdi zmdi-border-all',
      title: $L('表格'),
    },
    '|',
    {
      name: 'preview',
      action: EasyMDE.togglePreview,
      className: 'zmdi zmdi-eye no-disable',
      title: $L('预览'),
    },
    {
      name: 'fullscreen',
      action: EasyMDE.toggleFullScreen,
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

// ~~ HTML 内容
const WrapHtml = (htmlContent) => <span dangerouslySetInnerHTML={{ __html: htmlContent }} />

// ~~ MD > HTML
class Md2Html extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    return <span ref={(c) => (this._$md2html = c)} dangerouslySetInnerHTML={{ __html: this.state.md2html }} />
  }

  componentDidMount() {
    let md = this.props.markdown
    if (this.props.keepHtml !== true) {
      md = md.replace(/>/g, '&gt;').replace(/</g, '&lt;')
      md = md.replace(/&gt; /g, '> ')
    }

    // 替换换行并保持表格换行
    let cHtml = marked.parse(md.replace(/(?<!\|)\n(?!\|)/g, '\n\n'))
    cHtml = cHtml.replace(/<img src="([^"]+)"/g, function (s, src) {
      let srcNew = src + (src.includes('?') ? '&' : '?') + 'imageView2/2/w/1000/interlace/1/q/100'
      return s.replace(src, srcNew)
    })

    this.setState({ md2html: cHtml }, () => {
      $(this._$md2html)
        .find('a')
        .each(function () {
          const $a = $(this)
          $a.attr({
            href: `${rb.baseUrl}/commons/url-safe?url=${encodeURIComponent($a.attr('href'))}`,
            target: '_blank',
          }).on('click', (e) => {
            $stopEvent(e, false)
          })
        })

      // 图片预览
      let imgs = []
      $(this._$md2html)
        .find('img[src]')
        .each(function () {
          const $img = $(this)
          let isrc = $img.attr('src')
          if (isrc) {
            if (isrc.includes('/filex/img/')) {
              isrc = isrc.split('/filex/img/')[1].split(/[?&]imageView2/)[0]
            }
            imgs.push(isrc)
            $img.on('click', (e) => {
              $stopEvent(e, true)
              const p = parent || window
              p.RbPreview.create(imgs, imgs.indexOf(isrc) || 0)
            })
          }
        })
    })
  }
}

// ~~ short React.Fragment
const RF = ({ children }) => <React.Fragment>{children}</React.Fragment>

// ~~ 右下弹出框
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

  _addItem(message, option) {
    const itemid = `gritter-item-${option.id || $random()}`
    const type = option.type || 'success'
    const icon = option.icon || (type === 'success' ? 'check' : type === 'danger' ? 'close-circle-o' : 'info-outline')

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
                typeof option.onCancel === 'function' && option.onCancel()
              }}
              title={$L('关闭')}
            />
            {option.title && <span className="gritter-title">{option.title}</span>}
            <p>{message}</p>
          </div>
        </div>
      </div>
    )

    const itemsNew = this.state.items || []
    itemsNew.push(item)
    this.setState({ items: itemsNew }, () => {
      setTimeout(() => this._removeItem(itemid), option.timeout || 6000)
    })
  }

  _removeItem(itemid) {
    const itemsNew = this.state.items.filter((item) => itemid !== item.key)
    this.setState({ items: itemsNew })
  }

  // -- Usage
  /**
   * @param {*} message
   * @param {*} option
   */
  static create(message, option = {}) {
    if (top !== self && parent.RbGritter) {
      parent.RbGritter.create(message, option)
    } else {
      if (this._RbGritter) {
        this._RbGritter._addItem(message, option)
      } else {
        const that = this
        renderRbcomp(<RbGritter />, function () {
          that._RbGritter = this
          that._RbGritter._addItem(message, option)
        })
      }
    }
  }

  /**
   * @param {*} id
   */
  static remove(id) {
    if (this._RbGritter) {
      if (id) id = 'gritter-item-' + id
      this._RbGritter._removeItem(id)
    }
  }
}

// ~~ 代码查看
class CodeViewport extends React.Component {
  render() {
    return (
      <div className="code-viewport">
        <pre ref={(c) => (this._$code = c)}>LOADING</pre>
        {window.ClipboardJS && (
          <a className="copy" title={$L('复制')} ref={(c) => (this._$copy = c)}>
            <i className="icon zmdi zmdi-copy" />
          </a>
        )}
      </div>
    )
  }

  componentDidMount() {
    this._$code.innerHTML = $formattedCode(this.props.code || '', this.props.type)

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
    // eslint-disable-next-line eqeqeq
    if (newProps.code && newProps.code != this.props.code) {
      this._$code.innerHTML = $formattedCode(newProps.code, this.props.type)
    }
  }
}

// ~~ 树组件 v2.5 v3.7
// TODO 子级延迟渲染
class AsideTree extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, expandItems: [] }
    this.__items = {}
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
    this.__items[item.id] = item
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
    this.__items = {}
    this.setState({ data: data })
  }

  triggerClick(id) {
    const item = this.__items[id]
    if (item) {
      this.setState({ activeItem: item.id }, () => {
        typeof this.props.onItemClick === 'function' && this.props.onItemClick(item)

        // 树状需要展开父级
        let ps = []
        let loop = item
        while (loop.parent) {
          ps.push(loop.parent)
          loop = this.__items[loop.parent]
        }
        ps.length > 0 && this.setState({ expandItems: ps })
      })
    }
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

// ~~ 文件重命名
class FileRename extends RbAlert {
  constructor(props) {
    super(props)
    this.__fileName = $fileCutName(this.props.fileKey)
  }

  renderContent() {
    const isOffice = this.props.fileId && ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'].includes($fileExtName(this.__fileName))
    return (
      <form className="rbalert-form-sm">
        <div className="form-group">
          <label className="text-dark text-bold">{$L('重命名')}</label>
          <input type="text" className="form-control form-control-sm" defaultValue={this.__fileName} placeholder={this.__fileName} ref={(c) => (this._$fileName = c)} maxLength="100" />
        </div>
        <div className="form-group mb-1">
          <button disabled={this.state.disabled} type="button" className="btn btn-primary" onClick={(e) => this.handleConfirm(e)}>
            {$L('确定')}
          </button>
          {isOffice && (
            <a className="btn btn-link ml-1" href={`${rb.baseUrl}/filex/editor?src=${this.props.fileId}`} target="_blank">
              <i className="mdi mdi-microsoft-office icon" />
              &nbsp;
              {$L('在线编辑')} (LAB)
            </a>
          )}
        </div>
      </form>
    )
  }

  componentDidMount() {
    super.componentDidMount()
    // 选择
  }

  handleConfirm(e) {
    const newName = $val(this._$fileName)
    if (newName && this.__fileName !== newName) {
      typeof this.props.onConfirm === 'function' && this.props.onConfirm(newName, this)
    } else {
      typeof this.props.onConfirm === 'function' && this.props.onConfirm(null) // Nochangs
      this.hide()
    }
  }

  // -- Usage

  /**
   * @param {*} fileKey
   * @param {*} onConfirm
   */
  static create(fileKey, onConfirm) {
    renderRbcomp(<FileRename fileKey={fileKey} onConfirm={onConfirm} />)
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

// destroy select2
function __destroySelect2(__select2) {
  if (__select2) {
    if (Array.isArray(__select2)) {
      __select2.forEach((s) => s.select2('destroy'))
    } else {
      __select2.select2('destroy')
    }
  }
}
