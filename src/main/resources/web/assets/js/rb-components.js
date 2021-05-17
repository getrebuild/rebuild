/*
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
    const inFrame = !this.props.children
    return (
      <div className={`modal rbmodal colored-header colored-header-${this.props.colored || 'primary'}`} ref={(c) => (this._rbmodal = c)}>
        <div className="modal-dialog" style={{ maxWidth: `${this.props.width || 680}px` }}>
          <div className="modal-content">
            <div className="modal-header modal-header-colored">
              <h3 className="modal-title">{this.props.title || 'UNTITLED'}</h3>
              <button className="close" type="button" onClick={() => this.hide()} title={$L('关闭')}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className={`modal-body ${inFrame ? 'iframe rb-loading' : ''} ${inFrame && this.state.frameLoad !== false ? 'rb-loading-active' : ''}`}>
              {this.props.children || <iframe src={this.props.url} frameBorder="0" scrolling="no" onLoad={() => this.resize()} />}
              {inFrame && <RbSpinner />}
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
      renderRbcomp(<RbModal url={url} title={title} width={options.width} disposeOnHide={options.disposeOnHide} />, null, function () {
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
  }

  render() {
    const styles = {}
    if (this.props.width) styles.maxWidth = ~~this.props.width

    return (
      <div className="modal rbalert" ref={(c) => (this._dlg = c)} tabIndex={this.state.tabIndex || -1}>
        <div className="modal-dialog modal-dialog-centered" style={styles}>
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()} title={$L('关闭')}>
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

    const content = this.props.htmlMessage ? (
      <div className="mt-3" style={{ lineHeight: 1.8 }} dangerouslySetInnerHTML={{ __html: this.props.htmlMessage }} />
    ) : (
      <p>{this.props.message || 'INMESSAGE'}</p>
    )

    const cancel = (this.props.cancel || this.hide).bind(this)
    const confirm = (this.props.confirm || this.hide).bind(this)

    return (
      <div className="text-center ml-6 mr-6">
        <div className={`text-${type}`}>
          <i className={`modal-main-icon zmdi zmdi-${icon}`} />
        </div>
        {this.props.title && <h4 className="mb-2 mt-3">{this.props.title}</h4>}
        <div className={this.props.title ? '' : 'mt-3'}>{content}</div>
        <div className="mt-4 mb-3">
          <button disabled={this.state.disable} className="btn btn-space btn-secondary" type="button" onClick={cancel}>
            {this.props.cancelText || $L('取消')}
          </button>
          <button disabled={this.state.disable} className={`btn btn-space btn-${type}`} type="button" onClick={confirm}>
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
   * @param {*} titleOrOptions
   * @param {*} options
   */
  static create(message, titleOrOptions, options) {
    if (typeof titleOrOptions === 'object') {
      options = titleOrOptions
      titleOrOptions = null
    }

    options = options || {}
    const props = { ...options, title: titleOrOptions }
    if (options.html === true) props.htmlMessage = message
    else props.message = message
    renderRbcomp(<RbAlert {...props} />, null, options.call)
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
    const content = this.props.htmlMessage ? <div className="message pl-0" dangerouslySetInnerHTML={{ __html: this.props.htmlMessage }} /> : <div className="message pl-0">{this.props.message}</div>

    return (
      <div ref={(c) => (this._rbhighbar = c)} className={`rbhighbar animated faster ${this.state.animatedClass}`}>
        <div className={`alert alert-dismissible alert-${this.props.type || 'warning'} mb-0`}>
          <button className="close" type="button" onClick={this.close} title={$L('关闭')}>
            <i className="zmdi zmdi-close" />
          </button>
          <div className="icon">
            <i className={`zmdi zmdi-${icon}`} />
          </div>
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
   * @param {*} options
   */
  static create(message, options) {
    if (top !== self && parent.RbHighbar) {
      parent.RbHighbar.create(message, options)
    } else {
      options = options || {}
      if (options.html === true) renderRbcomp(<RbHighbar htmlMessage={message} type={options.type} timeout={options.timeout} />)
      else renderRbcomp(<RbHighbar message={message} type={options.type} timeout={options.timeout} />)
    }
  }

  /**
   * @param {*} message
   */
  static success(message) {
    RbHighbar.create(message || $L('操作成功'), { type: 'success', timeout: 2000 })
  }

  /**
   * @param {*} message
   */
  static error(message) {
    RbHighbar.create(message || $L('系统繁忙，请稍后重试'), { type: 'danger', timeout: 4000 })
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
        <span className="spinner-border spinner-border-xl text-primary"></span>
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
    this.state.tabType = this._useTabs[0][0]
  }

  render() {
    let inResult
    if (!this.state.items) {
      inResult = <li className="select2-results__option un-hover text-muted">{$L('搜索中...')}</li>
    } else if (this.state.items.length === 0) {
      inResult = <li className="select2-results__option un-hover">{$L('未找到结果')}</li>
    } else {
      inResult = this.state.items.map((item) => {
        return (
          <li key={`o-${item.id}`} className="select2-results__option" data-id={item.id} onClick={(e) => this.clickItem(e)}>
            <i className={`zmdi ${!this.props.hideSelection && this.containsItem(item.id) ? ' zmdi-check' : ''}`}></i>
            {this.state.tabType === 'User' && <img alt="Avatar" src={`${rb.baseUrl}/account/user-avatar/${item.id}`} className="avatar" />}
            <span className="text">{item.text}</span>
          </li>
        )
      })
    }

    const _DropdownMenu = (
      <div className="dropdown-menu">
        <div className="selector-search">
          <div>
            <input
              type="search"
              className="form-control search"
              placeholder={$L('InputForSearch')}
              value={this.state.query || ''}
              ref={(c) => (this._input = c)}
              onChange={(e) => this.searchItems(e)}
              onKeyDown={(e) => this._keyEvent(e)}
            />
          </div>
        </div>
        <div className="tab-container m-0">
          <ul className={`nav nav-tabs nav-tabs-classic ${this._useTabs.length < 2 ? 'hide' : ''}`}>
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
              <div className="rb-scroller" ref={(c) => (this._scroller = c)}>
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
            <span ref={(c) => (this._dropdownParent = c)}>
              {this.props.compToggle}
              {_DropdownMenu}
            </span>
          ) : (
            <div className="select2-selection select2-selection--multiple">
              <div className="select2-selection__rendered" ref={(c) => (this._dropdownParent = c)}>
                {this.state.selected.length > 0 && (
                  <span className="select2-selection__clear" onClick={() => this.clearSelection()}>
                    &times;
                  </span>
                )}
                {this.state.selected.map((item) => {
                  return (
                    <span key={`s-${item.id}`} className="select2-selection__choice">
                      <span className="select2-selection__choice__remove" data-id={item.id} onClick={(e) => this.removeItem(e)}>
                        &times;
                      </span>{' '}
                      {item.text}
                    </span>
                  )
                })}
                <span className="select2-selection__choice abtn" data-toggle="dropdown">
                  <a>
                    <i className="zmdi zmdi-plus" /> {this.props.multiple === false ? $L('Select') : $L('添加')}
                  </a>
                </span>
                {_DropdownMenu}
              </div>
            </div>
          )}
        </span>
      </span>
    )
  }

  componentDidMount() {
    $(this._scroller).perfectScrollbar()

    const that = this
    $(this._dropdownParent).on({
      'show.bs.dropdown': function () {
        // 初始化
        if (!that.state.items) that.switchTab()
      },
      'shown.bs.dropdown': function () {
        that._input && that._input.focus()
        $(that._scroller).find('li.active').removeClass('active')
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
    $(this._scroller).perfectScrollbar('destroy')
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
        $.get(`/commons/search/users?type=${type}&q=${$encode(this.state.query)}`, (res) => {
          // // 全部用户
          // if (this.props.showAllUser && type === 'User' && !this.state.query) {
          //   res.data.unshift({ id: '001-9999999999999999', text: '全部用户' })
          // }

          this._cached[ckey] = res.data
          this.switchTab(type)
        })
      }
      $(this._scroller).perfectScrollbar('update')
    })
  }

  _tryActive($active, $el) {
    if ($el && $el.length === 1) {
      $active.removeClass('active')
      $el.addClass('active')

      const st = $(this._scroller).scrollTop()
      const et = $el.position().top
      if (et >= 0) {
        const top = et + st - (222 - 36) // maxHeight - elementHeight
        if (top > 0) $(this._scroller).scrollTop(top)
      } else {
        const top = st + et
        if (top >= 0) $(this._scroller).scrollTop(top)
      }
    }
  }

  _keyEvent(e) {
    if (e.keyCode === 40) {
      const $active = $(this._scroller).find('li.active')
      const $next = $active.length === 0 ? $(this._scroller).find('li:eq(0)') : $active.next()
      this._tryActive($active, $next)
    } else if (e.keyCode === 38) {
      const $active = $(this._scroller).find('li.active')
      const $prev = $active.length === 0 ? null : $active.prev()
      this._tryActive($active, $prev)
    } else if (e.keyCode === 13) {
      e.preventDefault()
      const $active = $(this._scroller).find('li.active')
      if ($active.length === 1) {
        $active.trigger('click')
        $stopEvent(e)
      }
    }
  }

  searchItems(e) {
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
      RbHighbar.create($L('MaxSelectX').replace('%d', 20))
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
const DateShow = function (props) {
  return props.date ? <span title={props.date}>{$fromNow(props.date)}</span> : null
}

// ~~ 任意记录选择
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
            placeholder: $L('NoAnySome,Entity'),
            allowClear: false,
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
        placeholder: `${$L('选择,Record')}`,
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
            return (search_input || '').length > 0 ? $L('NoResults') : $L('InputForSearch')
          },
          inputTooShort: () => {
            return $L('InputForSearch')
          },
          searching: () => {
            return $L('Searching')
          },
          maximumSelected: () => {
            return $L('OnlyXSelected').replace('%d', 1)
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
const DEFAULT_MDE_TOOLBAR = [
  {
    name: 'bold',
    action: SimpleMDE.toggleBold,
    className: 'zmdi zmdi-format-bold',
    title: $L('MdeditBold'),
  },
  {
    name: 'italic',
    action: SimpleMDE.toggleItalic,
    className: 'zmdi zmdi-format-italic',
    title: $L('MdeditItalic'),
  },
  {
    name: 'strikethrough',
    action: SimpleMDE.toggleStrikethrough,
    className: 'zmdi zmdi-format-strikethrough',
    title: $L('MdeditStrikethrough'),
  },
  {
    name: 'heading',
    action: SimpleMDE.toggleHeadingSmaller,
    className: 'zmdi zmdi-format-size',
    title: $L('MdeditHeading'),
  },
  {
    name: 'unordered-list',
    action: SimpleMDE.toggleUnorderedList,
    className: 'zmdi zmdi-format-list-bulleted',
    title: $L('MdeditUnorderedList'),
  },
  {
    name: 'ordered-list',
    action: SimpleMDE.toggleOrderedList,
    className: 'zmdi zmdi-format-list-numbered',
    title: $L('MdeditOrderedList'),
  },
  {
    name: 'link',
    action: SimpleMDE.drawLink,
    className: 'zmdi zmdi-link',
    title: $L('MdeditLink'),
  },
  {
    name: 'image',
    action: () => this._fieldValue__upload.click(),
    className: 'zmdi zmdi-image-o',
    title: $L('MdeditImage'),
  },
  {
    name: 'table',
    action: SimpleMDE.drawTable,
    className: 'zmdi zmdi-border-all',
    title: $L('MdeditTable'),
  },
  '|',
  {
    name: 'fullscreen',
    action: SimpleMDE.toggleFullScreen,
    className: 'zmdi zmdi-fullscreen no-disable',
    title: $L('MdeditFullScreen'),
  },
  {
    name: 'preview',
    action: SimpleMDE.togglePreview,
    className: 'zmdi zmdi-eye no-disable',
    title: $L('MdeditTogglePreview'),
  },
  {
    name: 'guide',
    action: () => window.open('https://getrebuild.com/docs/markdown-guide'),
    className: 'zmdi zmdi-help-outline no-disable',
    title: $L('MdeditGuide'),
  },
]

/**
 * JSX 组件渲染
 *
 * @param {*} jsx
 * @param {*} target id or object of element (or function of callback)
 * @param {*} call callback
 */
const renderRbcomp = function (jsx, target, call) {
  if (typeof target === 'function') {
    call = target
    target = null
  }

  target = target || $random('react-container-')
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
  ReactDOM.render(jsx, target, call)
}
