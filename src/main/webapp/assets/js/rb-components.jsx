/* eslint-disable no-unused-vars */
/* eslint-disable react/prop-types */
// ~~ Modal 兼容子元素和 iFrame
class RbModal extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }
  render() {
    let inFrame = !this.props.children
    return (<div className={`modal rbmodal colored-header colored-header-${this.props.colored || 'primary'}`} ref={(c) => this._rbmodal = c}>
      <div className="modal-dialog" style={{ maxWidth: (this.props.width || 680) + 'px' }}>
        <div className="modal-content">
          <div className="modal-header modal-header-colored">
            <h3 className="modal-title">{this.props.title || '无标题'}</h3>
            <button className="close" type="button" onClick={() => this.hide()}><span className="zmdi zmdi-close" /></button>
          </div>
          <div className={'modal-body' + (inFrame ? ' iframe rb-loading' : '') + (inFrame && this.state.frameLoad !== false ? ' rb-loading-active' : '')}>
            {this.props.children || <iframe src={this.props.url} frameBorder="0" scrolling="no" onLoad={() => this.resize()} />}
            {inFrame && <RbSpinner />}
          </div>
        </div>
      </div>
    </div>)
  }

  componentDidMount() {
    let root = $(this._rbmodal).modal({ show: true, backdrop: this.props.backdrop === false ? false : 'static', keyboard: false }).on('hidden.bs.modal', () => {
      $keepModalOpen()
      if (this.props.disposeOnHide === true) {
        root.modal('dispose')
        $unmount(root.parent())
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
    let root = $(this._rbmodal)
    $setTimeout(() => {
      let iframe = root.find('iframe')
      let height = iframe.contents().find('.main-content').outerHeight()
      if (height === 0) height = iframe.contents().find('body').height()
      // else height += 45 // .main-content's padding
      root.find('.modal-body').height(height)
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
    let that = this
    if (ext.disposeOnHide === false && !!that.__HOLDERs[url]) {
      that.__HOLDER = that.__HOLDERs[url]
      that.__HOLDER.show()
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
    let callback = () => {
      // eslint-disable-next-line react/no-string-refs
      let dlg = this._dlg || this.refs['dlg']
      if (dlg) dlg.show()
      typeof call === 'function' && call(this)
    }
    if (state && $.type(state) === 'object') this.setState(state, callback)
    else callback()
  }
  hide = () => {
    // eslint-disable-next-line react/no-string-refs
    let dlg = this._dlg || this.refs['dlg']
    if (dlg) dlg.hide()
  }
}

// ~~ Form 处理器
class RbFormHandler extends RbModalHandler {
  constructor(props) {
    super(props)
  }
  handleChange = (e, call) => {
    let target = e.target
    let id = target.dataset.id || target.name
    if (!id) return
    let val = target.type === 'checkbox' ? target.checked : target.value
    let s = {}
    s[id] = val
    this.setState(s, call)
    this.handleChangeAfter(id, val)
  }
  handleChangeAfter(name, value) {
  }
  componentWillUnmount() {
    // Auto destroy select2
    let ss = this.__select2
    if (ss) {
      if ($.type(ss) === 'array') $(ss).each(function () { this.select2('destroy') })
      else ss.select2('destroy')
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
    this.state = { disable: false }
  }
  render() {
    let style = {}
    if (this.props.width) style.maxWidth = ~~this.props.width
    return (
      <div className="modal rbalert" ref={(c) => this._dlg = c} tabIndex={this.state.tabIndex || -1}>
        <div className="modal-dialog modal-dialog-centered" style={style}>
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
    let icon = this.props.type === 'danger' ? 'alert-triangle' : 'help-outline'
    if (this.props.type === 'warning') icon = 'alert-circle-o'
    if (this.props.type === 'primary') icon = 'info-outline'
    let type = this.props.type || 'primary'
    let content = this.props.htmlMessage ?
      <div className="mt-3" style={{ lineHeight: 1.8 }} dangerouslySetInnerHTML={{ __html: this.props.htmlMessage }} />
      : <p>{this.props.message || '提示内容'}</p>

    let cancel = (this.props.cancel || this.hide).bind(this)
    let confirm = (this.props.confirm || this.hide).bind(this)

    return <div className="text-center ml-6 mr-6">
      {this.props.showIcon === false ? null :
        <div className={'text-' + type}><span className={'modal-main-icon zmdi zmdi-' + icon} /></div>
      }
      {this.props.title && <h4 className="mb-2 mt-3">{this.props.title}</h4>}
      <div className={this.props.title ? '' : 'mt-3'}>{content}</div>
      <div className="mt-4 mb-3">
        <button disabled={this.state.disable} className="btn btn-space btn-secondary" type="button" onClick={cancel}>{this.props.cancelText || '取消'}</button>
        <button disabled={this.state.disable} className={'btn btn-space btn-' + type} type="button" onClick={confirm}>{this.props.confirmText || '确定'}</button>
      </div>
    </div>
  }

  componentDidMount() {
    let root = $(this._dlg).modal({ show: true, keyboard: true }).on('hidden.bs.modal', function () {
      root.modal('dispose')
      $unmount(root.parent())
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
   * @param {*} titleExt 
   * @param {*} ext 
   */
  static create(message, titleExt, ext) {
    let title = titleExt
    if ($.type(titleExt) === 'object') {
      title = null
      ext = titleExt
    }
    ext = ext || {}
    let props = { ...ext, title: title }
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
    let content = this.props.htmlMessage ? <div className="message" dangerouslySetInnerHTML={{ __html: this.props.htmlMessage }} /> : <div className="message">{this.props.message}</div>
    return (<div ref={(c) => this._rbhighbar = c} className={'rbhighbar animated faster ' + this.state.animatedClass}>
      <div className={'alert alert-dismissible alert-' + (this.props.type || 'warning')}>
        <button className="close" type="button" onClick={() => this.close()}><span className="zmdi zmdi-close" /></button>
        <div className="icon"><span className={'zmdi zmdi-' + icon} /></div>
        {content}
      </div>
    </div>)
  }

  componentDidMount() {
    setTimeout(() => { this.close() }, this.props.timeout || 3000)
  }

  close() {
    this.setState({ animatedClass: 'fadeOut' }, () => {
      $unmount($(this._rbhighbar).parent())
    })
  }

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
    RbHighbar.create(message || '操作成功', 'success', { timeout: 2000 })
  }
  /**
   * @param {*} message 
   */
  static error(message) {
    RbHighbar.create(message || '系统繁忙，请稍后重试', 'danger', { timeout: 5000 })
  }
}

// ~~ 加载界面
function RbSpinner(props) {
  let spinner = <div className="rb-spinner">
    <svg width="40px" height="40px" viewBox="0 0 66 66" xmlns="http://www.w3.org/2000/svg">
      <circle fill="none" strokeWidth="4" strokeLinecap="round" cx="33" cy="33" r="30" className="circle" />
    </svg>
  </div>
  if (props && props.fully === true) return <div className="rb-loading rb-loading-active">{spinner}</div>
  return spinner
}

// ~~ 提示条幅
function RbAlertBox(props) {
  let icon = props.type === 'success' ? 'check' : 'info-outline'
  if (props.type === 'danger') icon = 'close-circle-o'
  return (<div className={'alert alert-icon alert-dismissible min alert-' + (props.type || 'warning')}>
    <div className="icon"><span className={'zmdi zmdi-' + icon} /></div>
    <div className="message">
      <a className="close" data-dismiss="alert"><span className="zmdi zmdi-close" /></a>
      <p>{props.message}</p>
    </div>
  </div>)
}

// ~~ 用户选择器
class UserSelector extends React.Component {
  constructor(props) {
    super(props)
    this.state = { dropdownOpen: false, selected: props.selected || [] }

    this.cached = {}
    this.tabTypes = []
    if (props.hideUser !== true) this.tabTypes.push(['User', '用户'])
    if (props.hideDepartment !== true) this.tabTypes.push(['Department', '部门'])
    if (props.hideRole !== true) this.tabTypes.push(['Role', '角色'])
  }

  render() {
    let noResult = null
    if (!this.state.items) noResult = noResult = <li className="select2-results__option un-hover text-muted">搜索中...</li>
    else if (this.state.items.length === 0) noResult = <li className="select2-results__option un-hover">未找到结果</li>

    return <div className="user-selector">
      <span className="select2 select2-container select2-container--default select2-container--below">
        <span className="selection">
          <span className="select2-selection select2-selection--multiple">
            <ul className="select2-selection__rendered">
              {this.state.selected.length > 0 && <span className="select2-selection__clear" onClick={this.clearSelection}>×</span>}
              {(this.state.selected).map((item) => {
                return (<li key={'s-' + item.id} className="select2-selection__choice"><span className="select2-selection__choice__remove" data-id={item.id} onClick={(e) => this.removeItem(e)}>×</span>{item.text}</li>)
              })}
              <li className="select2-selection__choice abtn" onClick={this.openDropdown}><a><i className="zmdi zmdi-plus"></i> 添加</a></li>
            </ul>
          </span>
        </span>
        <span className={'dropdown-wrapper ' + (this.state.dropdownOpen === false && 'hide')}>
          <div className="selector-search">
            <div>
              <input type="search" className="form-control" placeholder="输入关键词搜索" value={this.state.query || ''} onChange={(e) => this.searchItems(e)} />
            </div>
          </div>
          <div className="tab-container">
            <ul className="nav nav-tabs nav-tabs-classic">
              {this.tabTypes.map((item) => {
                return <li className="nav-item" key={'t-' + item[0]}><a onClick={() => this.switchTab(item[0])} className={'nav-link' + (this.state.tabType === item[0] ? ' active' : '')}>{item[1]}</a></li>
              })}
            </ul>
            <div className="tab-content">
              <div className="tab-pane active">
                <div className="rb-scroller" ref={(c) => this._scroller = c}>
                  <ul className="select2-results__options">
                    {noResult ? noResult : this.state.items.map((item) => {
                      return (<li key={'o-' + item.id} className="select2-results__option" data-id={item.id} onClick={(e) => this.clickItem(e)}><span className={'zmdi' + (this.containsItem(item.id) ? ' zmdi-check' : '')}></span>{item.text}</li>)
                    })}
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </span>
      </span>
    </div >
  }

  componentDidMount() {
    $(document.body).click((e) => {
      if (e.target && (e.target.matches('div.user-selector') || $(e.target).parents('div.user-selector').length > 0)) return
      if (this.__isUnmounted) return
      this.setState({ dropdownOpen: false })
    })
    $(this._scroller).perfectScrollbar()
  }

  componentWillUnmount() {
    this.__isUnmounted = true
    $(this._scroller).perfectScrollbar('destroy')
  }

  UNSAFE_componentWillReceiveProps(props) {
    this.setState({ selected: props.selected || this.state.selected })
  }

  clearSelection = () => {
    this.setState({ selected: [] })
  }

  openDropdown = (e) => {
    this.setState({ dropdownOpen: true }, () => {
      $(this._searchInput).focus()
      if (!this.state.tabType) this.switchTab('User')
    })
  }

  switchTab(type) {
    type = type || this.state.tabType
    const cacheKey = type + '-' + this.state.query
    this.setState({ tabType: type, items: this.cached[cacheKey] }, () => {
      if (!this.cached[cacheKey]) {
        $.get(`${rb.baseUrl}/commons/search/users?type=${type}&q=${$encode(this.state.query)}`, (res) => {
          this.cached[cacheKey] = res.data
          this.switchTab(type)
        })
      }
      $(this._scroller).perfectScrollbar('update')
    })
  }

  searchItems(e) {
    this.setState({ query: e.target.value }, () => {
      $setTimeout(() => {
        this.switchTab()
      }, 300, 'us-searchItems')
    })
  }

  clickItem(e) {
    let id = e.target.dataset.id
    let exists = false
    let ns = this.state.selected.filter((item) => {
      if (item.id === id) {
        exists = true
        return false
      }
      return true
    })

    if (exists === false) {
      ns.push({ id: id, text: $(e.target).text() })
    }
    this.setState({ selected: ns, dropdownOpen: this.props.closeOnSelect !== true })
  }

  removeItem(e) {
    this.clickItem(e)
  }

  containsItem(id) {
    let s = this.state.selected
    for (let i = 0; i < s.length; i++) {
      if (s[i].id === id) return true
    }
    return false
  }

  getSelected() {
    let ids = []
    this.state.selected.forEach((item) => {
      ids.push(item.id)
    })
    return ids
  }
}

// ~~ 用户显示
const UserShow = function (props) {
  let viewUrl = props.id ? ('#!/View/User/' + props.id) : null
  let avatarUrl = rb.baseUrl + '/account/user-avatar/' + props.id
  return (
    <a href={viewUrl} className="user-show" title={props.name} onClick={props.onClick}>
      <div className={'avatar' + (props.showName === true ? ' float-left' : '')}>{props.icon ? <i className={props.icon} /> : <img src={avatarUrl} />}</div>
      {props.showName === true ? <div className="name text-truncate">{props.name}{props.deptName ? <em>{props.deptName}</em> : null}</div> : null}
    </a>)
}

/**
 * JSX 渲染
 * @param {*} jsx 
 * @param {*} target id or Element
 * @param {*} call callback
 */
const renderRbcomp = function (jsx, target, call) {
  target = target || $random('react-comps-')
  if ($.type(target) === 'string') { // element id
    let container = document.getElementById(target)
    if (!container) {
      if (!target.startsWith('react-comps-')) throw 'No element found : ' + target
      else target = $('<div id="' + target + '"></div>').appendTo(document.body)[0]
    }
    else target = container
  } else {
    // Element object
    if (target instanceof $) target = target[0]
  }
  ReactDOM.render(jsx, target, call)
}