/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */

// ~~ Modal 兼容子元素和 iFrame
class RbModal extends React.Component {

  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    const inFrame = !this.props.children
    return <div className={`modal rbmodal colored-header colored-header-${this.props.colored || 'primary'}`} ref={(c) => this._rbmodal = c}>
      <div className="modal-dialog" style={{ maxWidth: `${this.props.width || 680}px` }}>
        <div className="modal-content">
          <div className="modal-header modal-header-colored">
            <h3 className="modal-title">{this.props.title || 'UNTITLED'}</h3>
            <button className="close" type="button" onClick={() => this.hide()}><span className="zmdi zmdi-close" /></button>
          </div>
          <div className={`modal-body ${inFrame ? 'iframe rb-loading' : ''} ${inFrame && this.state.frameLoad !== false ? 'rb-loading-active' : ''}`}>
            {this.props.children || <iframe src={this.props.url} frameBorder="0" scrolling="no" onLoad={() => this.resize()} />}
            {inFrame && <RbSpinner />}
          </div>
        </div>
      </div>
    </div>
  }

  componentDidMount() {
    const $root = $(this._rbmodal).modal({ show: true, backdrop: this.props.backdrop === false ? false : 'static', keyboard: false })
      .on('hidden.bs.modal', () => {
        $keepModalOpen()
        if (this.props.disposeOnHide === true) {
          $root.modal('dispose')
          $unmount($root.parent())
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
    $setTimeout(() => {
      const $iframe = $root.find('iframe')
      let height = $iframe.contents().find('.main-content').outerHeight()
      if (height === 0) height = $iframe.contents().find('body').height()
      // else height += 45 // .main-content's padding
      $root.find('.modal-body').height(height)
      this.setState({ frameLoad: false })
    }, 20, 'RbModal-resize')
  }

  // -- Usage
  /**
   * @param {*} url 
   * @param {*} title 
   * @param {*} ext 
   */
  static create(url, title, ext) {
    ext = ext || {}
    ext.disposeOnHide = ext.disposeOnHide === true // default false
    this.__HOLDERs = this.__HOLDERs || {}

    const that = this
    if (ext.disposeOnHide === false && !!that.__HOLDERs[url]) {
      that.__HOLDER = that.__HOLDERs[url]
      that.__HOLDER.show()
      that.__HOLDER.resize()
    } else {
      renderRbcomp(<RbModal url={url} title={title} width={ext.width} disposeOnHide={ext.disposeOnHide} />, null, function () {
        that.__HOLDER = this
        if (ext.disposeOnHide === false) that.__HOLDERs[url] = this
      })
    }
  }
  /**
   * @param {*} url 
   */
  static hide(url) {
    this.__HOLDERs = this.__HOLDERs || {}
    if (url) this.__HOLDERs[url] && this.__HOLDERs[url].hide()
    else if (this.__HOLDER) this.__HOLDER.hide()
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

  show = (state, call) => {
    const callback = () => {
      // eslint-disable-next-line react/no-string-refs
      const dlg = this._dlg || this.refs['dlg']
      if (dlg) dlg.show()
      typeof call === 'function' && call(this)
    }
    if (state && $.type(state) === 'object') this.setState(state, callback)
    else callback()
  }

  hide = (e) => {
    if (e && e.target && $(e.target).attr('disabled')) return
    // eslint-disable-next-line react/no-string-refs
    const dlg = this._dlg || this.refs['dlg']
    if (dlg) dlg.hide()
  }
}

// ~~ Modal of Form 处理器
class RbFormHandler extends RbModalHandler {

  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  handleChange = (e, call) => {
    const target = e.target
    const id = target.dataset.id || target.name
    if (!id) return
    const val = target.type === 'checkbox' ? target.checked : target.value
    const s = {}
    s[id] = val
    this.setState(s, call)
    this.handleChangeAfter(id, val)
  }
  handleChangeAfter(name, value) {/* NOOP */ }

  componentWillUnmount() {
    // destroy select2
    if (this.__select2) {
      if ($.type(this.__select2) === 'array') $(this.__select2).each(function () { this.select2('destroy') })
      else this.__select2.select2('destroy')
      this.__select2 = null
    }
  }

  disabled(d) {
    if (!this._btns) return
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
    const styles = {}
    if (this.props.width) styles.maxWidth = ~~this.props.width
    return (
      <div className="modal rbalert" ref={(c) => this._dlg = c} tabIndex={this.state.tabIndex || -1}>
        <div className="modal-dialog modal-dialog-centered" style={styles}>
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()}><span className="zmdi zmdi-close" /></button>
            </div>
            <div className="modal-body">
              {this.renderContent()}
            </div>
          </div>
        </div>
      </div>
    )
  }

  renderContent() {
    const type = this.props.type || 'primary'
    let icon = this.props.icon
    if (!icon) icon = type === 'danger' ? 'alert-triangle' : (type === 'primary' ? 'help-outline' : 'alert-circle-o')

    const content = this.props.htmlMessage ?
      <div className="mt-3" style={{ lineHeight: 1.8 }} dangerouslySetInnerHTML={{ __html: this.props.htmlMessage }} />
      : <p>{this.props.message || 'INMESSAGE'}</p>

    const cancel = (this.props.cancel || this.hide).bind(this)
    const confirm = (this.props.confirm || this.hide).bind(this)

    return (
      <div className="text-center ml-6 mr-6">
        <div className={`text-${type}`}><i className={`modal-main-icon zmdi zmdi-${icon}`} /></div>
        {this.props.title && <h4 className="mb-2 mt-3">{this.props.title}</h4>}
        <div className={this.props.title ? '' : 'mt-3'}>{content}</div>
        <div className="mt-4 mb-3">
          <button disabled={this.state.disable} className="btn btn-space btn-secondary" type="button" onClick={cancel}>{this.props.cancelText || '取消'}</button>
          <button disabled={this.state.disable} className={`btn btn-space btn-${type}`} type="button" onClick={confirm}>{this.props.confirmText || '确定'}</button>
        </div>
      </div>
    )
  }

  componentDidMount() {
    const $root = $(this._dlg).modal({ show: true, keyboard: true }).on('hidden.bs.modal', () => {
      $root.modal('dispose')
      $unmount($root.parent())
    })
  }

  hide() {
    $(this._dlg).modal('hide')
  }

  disabled(d) {
    d = d === true
    this.setState({ disable: d, tabIndex: d ? 99999 : -1 }, () => {
      // disabled 时不能简单关闭
      // $(this._dlg).modal({ backdrop: d ? 'static' : true, keyboard: !d })
    })
  }

  // -- Usage
  /**
   * @param {*} message 
   * @param {*} titleOrExt 
   * @param {*} ext 
   */
  static create(message, titleOrExt, ext) {
    if (typeof titleOrExt === 'object') {
      ext = titleOrExt
      titleOrExt = null
    }

    ext = ext || {}
    const props = { ...ext, title: titleOrExt }
    if (ext.html === true) props.htmlMessage = message
    else props.message = message
    renderRbcomp(<RbAlert {...props} />, null, ext.call)
  }
}

// ~~ 顶部提示条
class RbHighbar extends React.Component {

  constructor(props) {
    super(props)
    this.state = { animatedClass: 'slideInDown' }
  }

  render() {
    let icon = this.props.type === 'success' ? 'check' : 'info-outline'
    icon = this.props.type === 'danger' ? 'close-circle-o' : icon
    const content = this.props.htmlMessage
      ? <div className="message pl-0" dangerouslySetInnerHTML={{ __html: this.props.htmlMessage }} />
      : <div className="message pl-0">{this.props.message}</div>

    return (
      <div ref={(c) => this._rbhighbar = c} className={`rbhighbar animated faster ${this.state.animatedClass}`}>
        <div className={`alert alert-dismissible alert-${(this.props.type || 'warning')} mb-0`}>
          <button className="close" type="button" onClick={this.close}><i className="zmdi zmdi-close" /></button>
          <div className="icon"><i className={`zmdi zmdi-${icon}`} /></div>
          {content}
        </div>
      </div>
    )
  }

  componentDidMount() {
    setTimeout(() => this.close(), this.props.timeout || 3000)
  }

  close = () => this.setState({ animatedClass: 'fadeOut' }, () => $unmount($(this._rbhighbar).parent()))

  // -- Usage
  /**
   * @param {*} message 
   * @param {*} type 
   * @param {*} ext 
   */
  static create(message, type, ext) {
    if (top !== self && parent.RbHighbar) {
      parent.RbHighbar.create(message, type, ext)
    } else {
      ext = ext || {}
      if (ext.html === true) renderRbcomp(<RbHighbar htmlMessage={message} type={type} timeout={ext.timeout} />)
      else renderRbcomp(<RbHighbar message={message} type={type} timeout={ext.timeout} />)
    }
  }

  /**
   * @param {*} message 
   */
  static success(message) {
    if (!message) message = $lang('ActionSuccess')
    RbHighbar.create(message, 'success', { timeout: 2000 })
  }

  /**
   * @param {*} message 
   */
  static error(message) {
    if (!message) message = $lang('SystemBusy')
    RbHighbar.create(message, 'danger', { timeout: 4000 })
  }
}

// ~~ 提示条
function RbAlertBox(props) {
  const type = (props || {}).type || 'warning'
  const icon = type === 'success' ? 'check' : (type === 'danger' ? 'close-circle-o' : 'info-outline')

  return (
    <div className={`alert alert-icon alert-icon-border alert-dismissible alert-sm alert-${type}`}>
      <div className="icon"><i className={`zmdi zmdi-${icon}`} /></div>
      <div className="message">
        <a className="close" data-dismiss="alert"><i className="zmdi zmdi-close" /></a>
        <p>{props.message || 'INMESSAGE'}</p>
      </div>
    </div>
  )
}

// ~~ 加载动画
function RbSpinner(props) {
  const spinner = (
    <div className="rb-spinner">
      {$.browser.msie
        ? <span className="spinner-border spinner-border-xl text-primary"></span>
        : <svg width="40px" height="40px" viewBox="0 0 66 66" xmlns="http://www.w3.org/2000/svg">
          <circle fill="none" strokeWidth="4" strokeLinecap="round" cx="33" cy="33" r="30" className="circle" />
        </svg>
      }
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

    this.cached = {}
    this.tabTypes = []
    if (props.hideUser !== true) this.tabTypes.push(['User', '用户'])
    if (props.hideDepartment !== true) this.tabTypes.push(['Department', '部门'])
    if (props.hideRole !== true) this.tabTypes.push(['Role', '角色'])
    if (props.hideTeam !== true && rb.rbv) this.tabTypes.push(['Team', '团队'])
  }

  render() {
    let inResult
    if (!this.state.items) inResult = <li className="select2-results__option un-hover text-muted">搜索中...</li>
    else if (this.state.items.length === 0) inResult = <li className="select2-results__option un-hover">未找到结果</li>

    const _DropdownMenu = (
      <div className="dropdown-menu">
        <div className="selector-search">
          <div>
            <input type="search" className="form-control search" placeholder="输入关键词搜索" value={this.state.query || ''} ref={(c) => this._input = c}
              onChange={(e) => this.searchItems(e)}
              onKeyDown={(e) => this._keyEvent(e)} />
          </div>
        </div>
        <div className="tab-container m-0">
          <ul className={`nav nav-tabs nav-tabs-classic ${this.tabTypes.length < 2 ? 'hide' : ''}`}>
            {this.tabTypes.map((item) => {
              return (
                <li className="nav-item" key={`t-${item[0]}`}>
                  <a onClick={() => this.switchTab(item[0])} className={`nav-link ${this.state.tabType === item[0] ? ' active' : ''}`}>{item[1]}</a>
                </li>
              )
            })}
          </ul>
          <div className="tab-content">
            <div className="tab-pane active">
              <div className="rb-scroller" ref={(c) => this._scroller = c}>
                <ul className="select2-results__options">
                  {inResult ? inResult : this.state.items.map((item) => {
                    return (
                      <li key={`o-${item.id}`} className="select2-results__option" data-id={item.id} onClick={(e) => this.clickItem(e)}>
                        <i className={`zmdi ${!this.props.hideSelection && this.containsItem(item.id) ? ' zmdi-check' : ''}`}></i>
                        {this.state.tabType === 'User' && <img src={`${rb.baseUrl}/account/user-avatar/${item.id}`} className="avatar" />}
                        <span className="text">{item.text}</span>
                      </li>
                    )
                  })}
                </ul>
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
          {this.props.compToggle ?
            <span ref={(c) => this._dropdownParent = c}>
              {this.props.compToggle}
              {_DropdownMenu}
            </span>
            :
            <div className="select2-selection select2-selection--multiple">
              <div className="select2-selection__rendered" ref={(c) => this._dropdownParent = c}>
                {this.state.selected.length > 0 && <span className="select2-selection__clear" onClick={() => this.clearSelection()}>&times;</span>}
                {this.state.selected.map((item) => {
                  return (
                    <span key={`s-${item.id}`} className="select2-selection__choice">
                      <span className="select2-selection__choice__remove" data-id={item.id} onClick={(e) => this.removeItem(e)}>&times;</span> {item.text}
                    </span>
                  )
                })}
                <span className="select2-selection__choice abtn" data-toggle="dropdown">
                  <a><i className="zmdi zmdi-plus" /> {this.props.multiple === false ? '选择' : '添加'}</a>
                </span>
                {_DropdownMenu}
              </div>
            </div>
          }
        </span>
      </span>
    )
  }

  componentDidMount() {
    $(this._scroller).perfectScrollbar()

    const that = this
    $(this._dropdownParent).on({
      'show.bs.dropdown': function () {
        if (!that.state.tabType) that.switchTab('User')
      },
      'shown.bs.dropdown': function () {
        that._input && that._input.focus()
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
      }
    })
  }

  componentWillUnmount() {
    $(this._scroller).perfectScrollbar('destroy')
  }

  UNSAFE_componentWillReceiveProps(props) {
    this.setState({ selected: props.selected || this.state.selected })
  }

  clearSelection() {
    this.setState({ selected: [] })
  }

  switchTab(type) {
    type = type || this.state.tabType
    const ckey = `${type}-${this.state.query}`
    this.setState({ tabType: type, items: this.cached[ckey] }, () => {
      if (!this.cached[ckey]) {
        $.get(`/commons/search/users?type=${type}&q=${$encode(this.state.query)}`, (res) => {
          this.cached[ckey] = res.data
          this.switchTab(type)
        })
      }
      $(this._scroller).perfectScrollbar('update')
    })
  }

  _keyEvent(e) {
    // if (e.keyCode === 40) {
    //   const $next = this._$foucsedItem ? this._$foucsedItem.next() : $(this._scroller).find('li:eq(0)')
    //   if ($next.length > 0) {
    //     this._$foucsedItem && this._$foucsedItem.removeClass('active')
    //     $next.addClass('active')
    //     this._$foucsedItem = $next
    //   }

    // } else if (e.keyCode === 38 && this._$foucsedItem) {
    //   const $prev = this._$foucsedItem.prev()
    //   if ($prev && $prev.length > 0) {
    //     this._$foucsedItem.removeClass('active')
    //     $prev.addClass('active')
    //     this._$foucsedItem = $prev
    //   }

    // } else if (e.keyCode === 13 && this._$foucsedItem) {
    //   this._$foucsedItem.trigger('click')
    //   $stopEvent(e)
    // }
  }

  searchItems(e) {
    this.setState({ query: e.target.value }, () => {
      $setTimeout(() => this.switchTab(), 300, 'us-searchItems')
    })
  }

  clickItem(e) {
    const $target = $(e.currentTarget)
    const id = $target.data('id') || $target.parents('.select2-results__option').data('id')

    let exists = false
    let ns = []
    // 单选
    if (this.props.multiple !== false) {
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
      RbHighbar.create('最多选择 20 个')
      return false
    }

    this.setState({ selected: ns }, () => {
      typeof this.props.onSelectItem === 'function' && this.props.onSelectItem(selected)
    })
  }

  removeItem(e) {
    this.clickItem(e, true)
  }

  containsItem(id) {
    return !!this.state.selected.find(x => x.id === id)
  }

  getSelected() {
    const ids = []
    this.state.selected.forEach((item) => ids.push(item.id))
    return ids
  }

  val() {
    return this.getSelected()
  }
}

// ~~ 用户显示
const UserShow = function (props) {
  const viewUrl = props.id ? `#!/View/User/${props.id}` : null
  const avatarUrl = `${rb.baseUrl}/account/user-avatar/${props.id}`
  return (
    <a href={viewUrl} className="user-show" title={props.name} onClick={props.onClick}>
      <div className={`avatar ${props.showName === true ? ' float-left' : ''}`}>{props.icon ? <i className={props.icon} /> : <img src={avatarUrl} alt="Avatar" />}</div>
      {props.showName && (<div className={`text-truncate name ${props.deptName ? 'vm' : ''}`}>{props.name}{props.deptName && <em>{props.deptName}</em>}</div>)}
    </a>
  )
}

// ~~ 日期显示
const DateShow = function (props) {
  return props.date ? <span title={props.date}>{$fromNow(props.date)}</span> : null
}

/**
 * JSX 组件渲染
 * @param {*} jsx 
 * @param {*} target id or object of element (or function of callback)
 * @param {*} call callback
 */
const renderRbcomp = function (jsx, target, call) {
  if (typeof target === 'function') {
    call = target
    target = null
  }

  target = target || $random('react-comps-')
  if ($.type(target) === 'string') { // element id
    const container = document.getElementById(target)
    if (!container) {
      if (!target.startsWith('react-comps-')) throw 'No element found : ' + target
      else target = $(`<div id="${target}"></div>`).appendTo(document.body)[0]
    } else {
      target = container
    }
  } else if (target instanceof $) {
    target = target[0]
  }
  ReactDOM.render(jsx, target, call)
}