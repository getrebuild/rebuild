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
    this._htmlid = $random('modal-body-', true, 20)
  }

  render() {
    const styles = {}
    if (this.props.zIndex) styles.zIndex = this.props.zIndex

    const iframe = !this.props.children // No child

    return (
      <div
        className={`modal rbmodal colored-header colored-header-${this.props.colored || 'primary'}`}
        style={styles}
        ref={(c) => {
          this._rbmodal = c
          this._element = c
        }}>
        <div className="modal-dialog" style={{ maxWidth: this.props.width || 680 }}>
          <div className="modal-content">
            <div className="modal-header modal-header-colored">
              <h3 className="modal-title">{this.props.title || 'UNTITLED'}</h3>
              <button className="close" type="button" onClick={() => this.hide()} title={$L('关闭')}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className={`modal-body ${iframe ? 'iframe rb-loading' : ''} ${iframe && this.state.frameLoad !== false ? 'rb-loading-active' : ''}`} id={this._htmlid}>
              {this.props.children || <iframe src={this.props.url} frameBorder="0" scrolling="no" onLoad={() => this.resize()} />}
              {iframe && <RbSpinner />}
            </div>
          </div>
        </div>
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
   * @param {*} options
   */
  static create(url, title, options) {
    // URL prefix
    if (url.substr(0, 1) === '/' && rb.baseUrl) url = rb.baseUrl + url

    options = options || {}
    options.disposeOnHide = options.disposeOnHide === true // default false
    this.__HOLDERs = this.__HOLDERs || {}

    const that = this
    if (options.disposeOnHide === false && !!that.__HOLDERs[url]) {
      that.__HOLDER = that.__HOLDERs[url]
      that.__HOLDER.show()
      that.__HOLDER.resize()
    } else {
      renderRbcomp(<RbModal url={url} title={title} width={options.width} disposeOnHide={options.disposeOnHide} zIndex={options.zIndex} />, null, function () {
        that.__HOLDER = this
        if (options.disposeOnHide === false) that.__HOLDERs[url] = this
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
      if ($.type(this.__select2) === 'array')
        $(this.__select2).each(function () {
          this.select2('destroy')
        })
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
    this._htmlid = $random('alert-body-', true, 20)
  }

  render() {
    const styles = {}
    if (this.props.width) styles.maxWidth = ~~this.props.width

    return (
      <div
        className="modal rbalert"
        ref={(c) => {
          this._dlg = c
          this._element = c
        }}>
        <div className="modal-dialog modal-dialog-centered" style={styles}>
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()} title={$L('关闭')}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body" id={this._htmlid}>
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
    if (!icon) icon = type === 'danger' ? 'alert-triangle' : type === 'primary' ? 'help-outline' : 'alert-circle-o'

    const _onCancel = (this.props.onCancel || this.props.cancel || this.hide).bind(this)
    const _onConfirm = (this.props.onConfirm || this.props.confirm || this.hide).bind(this)

    return (
      <div className="text-center ml-6 mr-6" ref={(c) => (this._element = c)}>
        <div className={`text-${type}`}>
          <i className={`modal-main-icon zmdi zmdi-${icon}`} />
        </div>
        {this.props.title && <h4 className="mb-2 mt-3">{this.props.title}</h4>}
        <div className={this.props.title ? '' : 'mt-3'}>
          <div>{this.props.message}</div>
        </div>
        <div className="mt-4 mb-3">
          <button disabled={this.state.disable} className="btn btn-space btn-secondary" type="button" onClick={_onCancel}>
            {this.props.cancelText || $L('取消')}
          </button>
          <button disabled={this.state.disable} className={`btn btn-space btn-${type}`} type="button" onClick={_onConfirm}>
            {this.props.confirmText || $L('确定')}
          </button>
        </div>
      </div>
    )
  }

  componentDidMount() {
    const $root = $(this._dlg)
      .modal({ show: true, keyboard: true })
      .on('hidden.bs.modal', () => {
        $keepModalOpen()
        $root.modal('dispose')
        $unmount($root.parent())
      })
  }

  hide(forceHide) {
    if (forceHide) {
      $mp.end()
      $(this._dlg).off('hide.bs.modal')
    }
    $(this._dlg).modal('hide')
  }

  disabled(d, preventHide) {
    d = d === true
    // 带有 tabIndex=-1 导致 select2 组件搜索框无法搜索???
    this.setState({ disable: d }, () => {
      if (d && preventHide) {
        $mp.start()
        $(this._dlg).find('.close').attr('disabled', true)
        $(this._dlg).on('hide.bs.modal', function () {
          return false
        })
      }
    })
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

// ~~ 顶部提示条
class RbHighbar extends React.Component {
  constructor(props) {
    super(props)
    this.state = { animatedClass: 'slideInDown' }
  }

  render() {
    let icon = this.props.type === 'success' ? 'check' : 'info-outline'
    icon = this.props.type === 'danger' ? 'close-circle-o' : icon

    return (
      <div ref={(c) => (this._rbhighbar = c)} className={`rbhighbar animated faster ${this.state.animatedClass}`}>
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

  close = () => this.setState({ animatedClass: 'fadeOut' }, () => $unmount($(this._rbhighbar).parent()))

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
function RbAlertBox(props) {
  const type = (props || {}).type || 'warning'
  const icon = type === 'success' ? 'check' : type === 'danger' ? 'close-circle-o' : 'info-outline'

  return (
    <div className={`alert alert-icon alert-icon-border alert-dismissible alert-sm alert-${type}`}>
      <div className="icon">
        <i className={`zmdi zmdi-${icon}`} />
      </div>
      <div className="message">
        <a className="close" data-dismiss="alert" onClick={() => typeof props.onClose === 'function' && props.onClose()} title={$L('关闭')}>
          <i className="zmdi zmdi-close" />
        </a>
        <p>{props.message || 'INMESSAGE'}</p>
      </div>
    </div>
  )
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
    if (props.hideRole !== true) this._useTabs.push(['Role', $L('角色')])
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

    if (this.props.defaultValue) {
      let dv = this.props.defaultValue
      if ($.type(this.props.defaultValue) === 'string') dv = dv.split(',')

      $.post('/commons/search/user-selector', JSON.stringify(dv), (res) => {
        if (res.error_code === 0 && res.data.length > 0) {
          this.setState({ selected: res.data })
        }
      })
    }
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
    return this.getSelected()
  }

  toggle() {
    // $(this._$dropdownParent).dropdown('toggle')
    $(this._$dropdownParent).find('[data-toggle="dropdown"]').dropdown('toggle')
  }
}

// ~~ 用户显示
const UserShow = function (props) {
  const viewUrl = props.id ? `#!/View/User/${props.id}` : null
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
const DateShow = function ({ date }) {
  return date ? <span title={date}>{$fromNow(date)}</span> : null
}

// ~~ 任意记录选择
// @see rb-page.js#$initReferenceSelect2
class AnyRecordSelector extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="row">
        <div className="col-4 pr-0">
          <select className="form-control form-control-sm" ref={(c) => (this._entity = c)}>
            {(this.state.entities || []).map((item) => {
              return (
                <option key={item.name} value={item.name}>
                  {item.label}
                </option>
              )
            })}
          </select>
        </div>
        <div className="col-8 pl-2">
          <select className="form-control form-control-sm float-left" ref={(c) => (this._record = c)} />
        </div>
      </div>
    )
  }

  componentDidMount() {
    $.get('/commons/metadata/entities', (res) => {
      if ((res.data || []).length === 0) $(this._record).attr('disabled', true)

      this.setState({ entities: res.data || [] }, () => {
        $(this._entity)
          .select2({
            placeholder: $L('无可用实体'),
            allowClear: false,
            matcher: $select2MatcherAll,
          })
          .on('change', () => {
            $(this._record).val(null).trigger('change')
          })

        // 编辑时
        const iv = this.props.initValue
        if (iv) {
          $(this._entity).val(iv.entity).trigger('change')
          const option = new Option(iv.text, iv.id, true, true)
          $(this._record).append(option)
        }
      })
    })

    const that = this
    let search_input = null
    $(this._record)
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
              entity: $(that._entity).val(),
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
            return (search_input || '').length > 0 ? $L('未找到结果') : $L('输入关键词搜索')
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
  }

  // return `id`
  val() {
    return $(this._record).val()
  }

  // return `{ id:xx, text:xx, entity:xx }`
  value() {
    const val = this.val()
    if (!val) return null

    return {
      entity: $(this._entity).val(),
      id: val,
      text: $(this._record).select2('data')[0].text,
    }
  }

  reset() {
    $(this._record).val(null).trigger('change')
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
        {info.dept && <p className="text-muted fs-12 up-2">{info.dept}</p>}
        {info.email && <p className="email">{info.email}</p>}
        {info.phone && <p className="phone">{info.phone}</p>}
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

  function _leave() {
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
              mouseleave: _leave,
            })
          }
        })
      }, 400)
    },
    mouseleave: _leave,
  })
}

// ~~ HTML 内容
const WrapHtml = (htmlContent) => <span dangerouslySetInnerHTML={{ __html: htmlContent }} />

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
    const icon = type === 'success' ? 'check' : type === 'danger' ? 'close-circle-o' : 'info-outline'

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
              }}
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
    console.log(itemsNew)
    this.setState({ items: itemsNew })
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
      if (this.__$wrapper) {
        this.__$wrapper._addItem(message, options)
      } else {
        const that = this
        renderRbcomp(<RbGritter />, null, function () {
          that.__$wrapper = this
          that.__$wrapper._addItem(message, options)
        })
      }
    }
  }
}

/**
 * JSX 组件渲染
 *
 * @param {*} jsx
 * @param {*} target id or object of element (or function of callback)
 * @param {*} callback callback on mounted
 */
const renderRbcomp = function (jsx, target, callback) {
  if (typeof target === 'function') {
    callback = target
    target = null
  }

  target = target || $random('react-container-', true, 32)
  if ($.type(target) === 'string') {
    // element id
    const container = document.getElementById(target)
    if (!container) {
      if (!target.startsWith('react-container-')) throw 'No element found : ' + target
      else target = $(`<div id="${target}"></div>`).appendTo(document.body)[0]
    } else {
      target = container
    }
  } else if (target instanceof $) {
    target = target[0]
  }

  // ReactDOM.render(<React.StrictMode>{jsx}</React.StrictMode>, target, call)
  ReactDOM.render(jsx, target, callback)
  return target
}
