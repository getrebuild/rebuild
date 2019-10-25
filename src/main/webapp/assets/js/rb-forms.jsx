/* eslint-disable react/jsx-no-target-blank */
/* eslint-disable no-unused-vars */
/* eslint-disable react/prop-types */
/* eslint-disable react/no-string-refs */
// ~~ 表单 Dialog
class RbFormModal extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, inLoad: true }
    if (!props.id) this.state.id = null
  }
  render() {
    return (this.state.isDestroy === true ? null :
      <div className="modal-wrapper">
        <div className="modal rbmodal colored-header colored-header-primary" ref={(c) => this._rbmodal = c}>
          <div className="modal-dialog">
            <div className="modal-content">
              <div className="modal-header modal-header-colored">
                {this.state.icon && (<span className={'icon zmdi zmdi-' + this.state.icon} />)}
                <h3 className="modal-title">{this.state.title || '新建'}</h3>
                {rb.isAdminUser ? <a className="close s" href={rb.baseUrl + '/admin/entity/' + this.state.entity + '/form-design'} title="配置布局" target="_blank"><span className="zmdi zmdi-settings"></span></a> : null}
                <button className="close md-close" type="button" onClick={() => this.hide()}><span className="zmdi zmdi-close"></span></button>
              </div>
              <div className={'modal-body rb-loading' + (this.state.inLoad ? ' rb-loading-active' : '')}>
                {this.state.alertMessage && (<div className="alert alert-warning rbform-alert">{this.state.alertMessage}</div>)}
                {this.state.formComponent}
                {this.state.inLoad && <RbSpinner />}
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }
  componentDidMount() {
    $(this._rbmodal).modal({ show: false, backdrop: 'static', keyboard: false }).on('hidden.bs.modal', () => $keepModalOpen())
    this.showAfter({}, true)
  }

  // 渲染表单
  getFormModel() {
    let that = this
    let entity = this.state.entity
    let id = this.state.id || ''
    let initialValue = this.state.initialValue || {}  // 默认值填充（仅新建有效）
    $.post(`${rb.baseUrl}/app/${entity}/form-model?id=${id}`, JSON.stringify(initialValue), function (res) {
      // 包含错误
      if (res.error_code > 0 || !!res.data.error) {
        let error = res.data.error || res.error_msg
        that.renderFromError(error)
        return
      }

      let FORM = <RbForm entity={entity} id={id} $$$parent={that}>
        {res.data.elements.map((item) => {
          return detectElement(item)
        })}
      </RbForm>
      that.setState({ formComponent: FORM, __formModel: res.data }, function () {
        that.setState({ inLoad: false })
      })
      that.__lastModified = res.data.lastModified || 0
    })
  }
  renderFromError(message) {
    let error = <div className="alert alert-danger alert-icon mt-5 w-75 mlr-auto">
      <div className="icon"><i className="zmdi zmdi-alert-triangle"></i></div>
      <div className="message" dangerouslySetInnerHTML={{ __html: '<strong>抱歉!</strong> ' + message }}></div>
    </div>

    this.setState({ formComponent: error }, () => {
      this.setState({ inLoad: false })
    })
  }

  show(state) {
    state = state || {}
    if (!state.id) state.id = null
    if ((state.id !== this.state.id || state.entity !== this.state.entity) || this.state.isDestroy === true) {
      state = { formComponent: null, initialValue: null, inLoad: true, ...state }
      this.setState(state, () => {
        this.showAfter({ isDestroy: false }, true)
      })
    } else {
      this.showAfter({ ...state, isDestroy: false })
      this.checkDrityData()
    }
  }
  showAfter(state, modelChanged) {
    this.setState(state, () => {
      $(this._rbmodal).modal('show')
      if (modelChanged === true) this.getFormModel()
    })
  }
  checkDrityData() {
    if (!this.__lastModified || !this.state.id) return
    $.get(`${rb.baseUrl}/app/entity/record-lastModified?id=${this.state.id}`, (res) => {
      if (res.error_code === 0) {
        if (res.data.lastModified !== this.__lastModified) {
          // this.setState({ alertMessage: <p>记录已由其他用户编辑过，<a onClick={() => this.__refresh()}>点击此处</a>查看最新数据</p> })
          this.__refresh()
        }
      } else if (res.error_msg === 'NO_EXISTS') {
        this.setState({ alertMessage: '记录已经不存在，可能已被其他用户删除' })
      }
    })
  }
  __refresh() {
    let hold = { id: this.state.id, entity: this.state.entity }
    this.setState({ id: null, alertMessage: null }, () => { this.show(hold) })
  }

  hide(destroy) {
    $(this._rbmodal).modal('hide')
    let state = { isDestroy: destroy === true }
    if (destroy === true) state.id = null
    this.setState(state)
  }

  // -- Usage
  /**
   * @param {*} props 
   */
  static create(props) {
    let that = this
    if (that.__HOLDER) that.__HOLDER.show(props)
    else renderRbcomp(<RbFormModal {...props} />, null, function () { that.__HOLDER = this })
  }
}

// ~~ 表单
class RbForm extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }

    this.__FormData = {}
    let iv = props.$$$parent.state.__formModel.initialValue
    if (iv) {
      for (let k in iv) this.__FormData[k] = { value: iv[k], error: null }
    }

    this.isNew = !props.$$$parent.state.id
    this.setFieldValue = this.setFieldValue.bind(this)
  }
  render() {
    return (
      <div className="rbform">
        <div className="form" ref={(c) => this._form = c}>
          {this.props.children.map((child) => {
            return React.cloneElement(child, { $$$parent: this, ref: 'ref-' + child.props.field })
            // Has error in strict-mode
            //child.$$$parent = that; return child
          })}
          {this.renderFormAction()}
        </div>
      </div>
    )
  }
  renderFormAction() {
    let pmodel = this.props.$$$parent.state.__formModel
    let moreActions = []
    if (pmodel.hadApproval) {
      moreActions.push(<a key="Action103" className="dropdown-item" onClick={() => this.post(103)}>保存并提交</a>)
    }
    if (pmodel.isMaster === true) {
      moreActions.push(<a key="Action102" className="dropdown-item" onClick={() => this.post(102)}>保存并添加明细</a>)
    } else if (pmodel.isSlave === true) {
      moreActions.push(<a key="Action101" className="dropdown-item" onClick={() => this.post(101)}>保存并继续添加</a>)
    }

    let actionBtn = <button className="btn btn-primary btn-space" type="button" onClick={() => this.post()}>保存</button>
    if (moreActions.length > 0) {
      actionBtn = (
        <div className="btn-group dropup btn-space">
          <button className="btn btn-primary" type="button" onClick={() => this.post()}>保存</button>
          <button className="btn btn-primary dropdown-toggle auto" type="button" data-toggle="dropdown"><span className="icon zmdi zmdi-chevron-up"></span></button>
          <div className="dropdown-menu dropdown-menu-primary dropdown-menu-right">
            {moreActions.map((item) => { return item })}
          </div>
        </div>)
    }

    return (
      <div className="form-group row footer">
        <div className="col-12 col-sm-8 offset-sm-3" ref="rbform-action">
          {actionBtn}
          <button className="btn btn-secondary btn-space" type="button" onClick={() => this.props.$$$parent.hide()}>取消</button>
        </div>
      </div>)
  }

  componentDidMount() {
    if (this.isNew === true) {
      let that = this
      this.props.children.map((child) => {
        let val = child.props.value
        if (val && child.props.readonly !== true) {
          if ($.type(val) === 'array') val = val[0]  // 若为数组，第一个就是真实值
          that.setFieldValue(child.props.field, val)
        }
      })
    }
  }

  // 设置字段值
  setFieldValue(field, value, error) {
    this.__FormData[field] = { value: value, error: error }
    // eslint-disable-next-line no-console
    if (rb.env === 'dev') console.log('FV ... ' + JSON.stringify(this.__FormData))
  }
  // 避免无意义更新
  setFieldUnchanged(field) {
    delete this.__FormData[field]
    // eslint-disable-next-line no-console
    if (rb.env === 'dev') console.log('FV ... ' + JSON.stringify(this.__FormData))
  }

  // 设置表单回填
  setAutoFillin(data) {
    data.forEach((item, idx) => {
      let ref = this.refs['ref-' + item.target]
      if (ref) {
        if (item.fillinForce !== true && !!ref.getValue()) return
        if ((this.isNew === true && item.whenCreate === true) || (this.isNew !== true && item.whenUpdate === true)) {
          ref.setValue(item.value)
        }
      }
    })
  }

  /**
   * @next {Number}
   */
  post(next) {
    next = next || 100
    let _data = {}
    for (let k in this.__FormData) {
      let err = this.__FormData[k].error
      if (err) { RbHighbar.create(err); return }
      else _data[k] = this.__FormData[k].value
    }

    _data.metadata = { entity: this.state.entity, id: this.state.id }
    if (RbForm.postBefore(_data) === false) return

    let btns = $(this.refs['rbform-action']).find('.btn').button('loading')
    let that = this
    $.post(`${rb.baseUrl}/app/entity/record-save`, JSON.stringify(_data), function (res) {
      btns.button('reset')
      if (res.error_code === 0) {
        RbHighbar.create('保存成功', 'success')
        setTimeout(() => {
          that.props.$$$parent.hide(true)
          RbForm.postAfter(res.data, next)

          if (next === 101) {
            let pstate = that.props.$$$parent.state
            RbFormModal.create({ title: pstate.title, entity: pstate.entity, icon: pstate.icon, initialValue: pstate.initialValue })
          } else if (next === 102) {
            let iv = { '$MASTER$': res.data.id }
            let sm = that.props.$$$parent.state.__formModel.slaveMeta
            RbFormModal.create({ title: `添加${sm[1]}`, entity: sm[0], icon: sm[2], initialValue: iv })
          } else if (next === 103) {
            renderRbcomp(<ApprovalSubmitForm id={res.data.id} disposeOnHide={true} />)
          }
        }, 100)

      } else if (res.error_code === 499) {
        renderRbcomp(<RepeatedViewer entity={that.state.entity} data={res.data} />)
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }

  // 保存前调用
  // @return false 则不继续保存
  static postBefore(data) {
    return true
  }
  // 保存后调用
  static postAfter(data, next) {
    let rlp = window.RbListPage || parent.RbListPage
    if (rlp) rlp.reload()
    if (window.RbViewPage && (next || 0) < 101) window.RbViewPage.reload()
  }
}

// 表单元素
class RbFormElement extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }

    this.handleChange = this.handleChange.bind(this)
    this.checkValue = this.checkValue.bind(this)
    this.handleClear = this.handleClear.bind(this)
  }

  render() {
    let props = this.props
    let colWidths = [3, 8]
    if (props.onView) {
      colWidths[0] = 4
      if (props.isFull === true) colWidths = [2, 10]
    }
    return (
      <div className={`form-group row type-${props.type}`} data-field={props.field}>
        <label ref={(c) => this._label = c} className={`col-12 col-form-label text-sm-right col-sm-${colWidths[0]} ${!props.onView && !props.nullable && 'required'}`}>{props.label}</label>
        <div className={'col-12 col-sm-' + colWidths[1]}>
          {this.state.viewMode ? this.renderViewElement() : this.renderElement()}
          {!props.onView && props.tip && <p className="form-text">{props.tip}</p>}
        </div>
      </div>
    )
  }
  renderViewElement() {
    return <div className="form-control-plaintext">{this.state.value || (<span className="text-muted">无</span>)}</div>
  }
  renderElement() {
    return '子类复写此方法'
  }
  componentDidMount() {
    let props = this.props
    // 必填字段
    if (props.nullable === false && props.readonly === false && props.onView !== true) {
      if (!props.value) {
        props.$$$parent.setFieldValue(props.field, null, props.label + '不能为空')
      }
    }
    if (!props.onView && props.tip) $(this._label).find('i.zmdi').tooltip({ placement: 'right' })
  }

  // 表单组件（字段）值变化应调用此方法
  handleChange(event, checkValue) {
    let val = event.target.value
    this.setState({ value: val }, () => { checkValue === true && this.checkValue() })
  }
  // 清空值
  handleClear() {
    this.setState({ value: '' }, () => { this.checkValue() })
  }
  checkValue() {
    if (this.isValueUnchanged()) {
      this.props.$$$parent.setFieldUnchanged(this.props.field)
      return false
    }

    let err = this.isValueError()
    this.setState({ hasError: err })
    let errMsg = err ? (this.props.label + err) : null
    this.props.$$$parent.setFieldValue(this.props.field, this.state.value, errMsg)
  }
  isValueError() {
    if (this.props.nullable === false) {
      let v = this.state.value
      if (v && $.type(v) === 'array') return v.length === 0 ? '不能为空' : null
      else return !v ? '不能为空' : null
    }
  }
  isValueUnchanged() {
    return $same(this.props.value || '', this.state.value || '')
  }

  // Getter / Setter

  setValue(val) {
    this.handleChange({ target: { value: val } }, true)
  }
  getValue() {
    return this.state.value
  }
}

// 只读字段
class RbFormReadonly extends RbFormElement {
  constructor(props) {
    super(props)
  }
  renderElement() {
    let text = this.props.value
    if (this.props.type === 'REFERENCE' && text) text = text[1]
    return <input className="form-control form-control-sm" type="text" readOnly value={text} />
  }
  setValue() {
    // DO NOTING
  }
  getValue() {
    return this.props.value
  }
}

// 文本
class RbFormText extends RbFormElement {
  constructor(props) {
    super(props)
  }
  renderElement() {
    return (
      <input ref="field-value" className={'form-control form-control-sm ' + (this.state.hasError ? 'is-invalid' : '')} title={this.state.hasError} type="text" value={this.state.value || ''} onChange={this.handleChange} onBlur={this.checkValue} />
    )
  }
}

// 链接
class RbFormUrl extends RbFormText {
  constructor(props) {
    super(props)
  }
  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()
    let clickUrl = rb.baseUrl + '/commons/url-safe?url=' + encodeURIComponent(this.state.value)
    return (<div className="form-control-plaintext"><a href={clickUrl} className="link" target="_blank" rel="noopener noreferrer">{this.state.value}</a></div>)
  }
  isValueError() {
    let err = super.isValueError()
    if (err) return err
    return (!!this.state.value && $regex.isUrl(this.state.value) === false) ? '链接格式不正确' : null
  }
}

// 邮箱
class RbFormEMail extends RbFormText {
  constructor(props) {
    super(props)
  }
  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()
    return (<div className="form-control-plaintext"><a href={'mailto:' + this.state.value} className="link">{this.state.value}</a></div>)
  }
  isValueError() {
    let err = super.isValueError()
    if (err) return err
    return (!!this.state.value && $regex.isMail(this.state.value) === false) ? '邮箱格式不正确' : null
  }
}

// 电话/手机
class RbFormPhone extends RbFormText {
  constructor(props) {
    super(props)
  }
  isValueError() {
    let err = super.isValueError()
    if (err) return err
    return (!!this.state.value && $regex.isTel(this.state.value) === false) ? '电话/手机格式不正确' : null
  }
}

// 整数
class RbFormNumber extends RbFormText {
  constructor(props) {
    super(props)
  }
  isValueError() {
    let err = super.isValueError()
    if (err) return err
    if (!!this.state.value && $regex.isNumber(this.state.value) === false) return '整数格式不正确'
    if (!!this.state.value && this.props.notNegative === 'true' && parseFloat(this.state.value) < 0) return '不允许为负数'
    return null
  }
}

// 货币
class RbFormDecimal extends RbFormText {
  constructor(props) {
    super(props)
  }
  isValueError() {
    let err = super.isValueError()
    if (err) return err
    if (!!this.state.value && $regex.isDecimal(this.state.value) === false) return '货币格式不正确'
    if (!!this.state.value && this.props.notNegative === 'true' && parseFloat(this.state.value) < 0) return '不允许为负数'
    return null
  }
}

// 多行文本
class RbFormTextarea extends RbFormElement {
  constructor(props) {
    super(props)
  }
  renderElement() {
    return (<textarea ref="field-value" className={'form-control form-control-sm row3x ' + (this.state.hasError ? 'is-invalid' : '')} title={this.state.hasError} value={this.state.value || ''} onChange={this.handleChange} onBlur={this.checkValue} />)
  }
  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()
    return <div className="form-control-plaintext">
      {this.state.value.split('\n').map((line, idx) => {
        return <p key={'kl-' + idx}>{line}</p>
      })}
    </div>
  }
}

// 日期-时间
class RbFormDateTime extends RbFormElement {
  constructor(props) {
    super(props)
  }
  renderElement() {
    return (
      <div className="input-group datetime-field">
        <input ref="field-value" className={'form-control form-control-sm ' + (this.state.hasError ? 'is-invalid' : '')} title={this.state.hasError} type="text" value={this.state.value || ''} onChange={this.handleChange} onBlur={this.checkValue} />
        <span className={'zmdi zmdi-close clean ' + (this.state.value ? '' : 'hide')} onClick={this.handleClear}></span>
        <div className="input-group-append">
          <button className="btn btn-secondary" type="button" ref="field-value-icon"><i className="icon zmdi zmdi-calendar"></i></button>
        </div>
      </div>
    )
  }
  componentDidMount() {
    super.componentDidMount()
    if (this.state.viewMode === true) return

    let format = (this.props.datetimeFormat || this.props.dateFormat).replace('mm', 'ii').toLowerCase()
    let minView = 0
    if (format.length === 7) minView = 'year'
    else if (format.length === 10) minView = 'month'

    let that = this
    let dtp = $(this.refs['field-value']).datetimepicker({
      componentIcon: 'zmdi zmdi-calendar',
      navIcons: { rightIcon: 'zmdi zmdi-chevron-right', leftIcon: 'zmdi zmdi-chevron-left' },
      format: format || 'yyyy-mm-dd hh:ii:ss',
      minView: minView,
      startView: minView === 'year' ? 'year' : 'month',
      weekStart: 1,
      autoclose: true,
      language: 'zh',
      todayHighlight: true,
      showMeridian: false,
      keyboardNavigation: false,
      minuteStep: 5,
    }).on('changeDate', function () {
      let val = $(this).val()
      that.handleChange({ target: { value: val } }, true)
    })
    $(this.refs['field-value-icon']).click(() => {
      dtp.datetimepicker('show')
    })
    this.__dtp = dtp
  }
  componentWillUnmount() {
    if (this.__dtp) {
      this.__dtp.datetimepicker('remove')
      this.__dtp = null
    }
  }
}

// 图片
class RbFormImage extends RbFormElement {
  constructor(props) {
    super(props)
    this.state.value = JSON.parse(props.value || '[]')
    this.__minUpload = 0
    this.__maxUpload = 9
    if (this.props.uploadNumber) {
      this.__minUpload = ~~(this.props.uploadNumber.split(',')[0] || 0)
      this.__maxUpload = ~~(this.props.uploadNumber.split(',')[1] || 9)
    }
    this.__typeName = '张图片'
  }
  renderElement() {
    return (
      <div className="img-field">
        {this.state.value.map((item) => {
          let itemUrl = rb.baseUrl + '/filex/img/' + item
          let fileName = $fileCutName(item)
          return (<span key={'file-' + item}><a title={fileName} className="img-thumbnail img-upload"><img src={itemUrl + '?imageView2/2/w/100/interlace/1/q/100'} /><b title="移除" onClick={() => this.removeItem(item)}><span className="zmdi zmdi-close"></span></b></a></span>)
        })}
        {this.state.showUploader === false ? null :
          <span title={'上传图片。需要 ' + this.__minUpload + '~' + this.__maxUpload + ' 张'}>
            <input type="file" className="inputfile" ref="upload-input" id={this.props.field + '-input'} accept="image/*" />
            <label htmlFor={this.props.field + '-input'} className="img-thumbnail img-upload"><span className="zmdi zmdi-image-alt"></span></label>
          </span>
        }
        <input ref="field-value" type="hidden" value={this.state.value} />
      </div>
    )
  }
  renderViewElement() {
    const value = this.state.value
    if (value.length === 0) {
      return <div className="form-control-plaintext"><span className="text-muted">无</span></div>
    }
    return (<div className="img-field">
      {value.map((item, idx) => {
        let itemUrl = rb.baseUrl + '/filex/img/' + item
        let fileName = $fileCutName(item)
        return <span key={'img-' + item}><a title={fileName} onClick={() => (parent || window).RbPreview.create(value, idx)} className="img-thumbnail img-upload zoom-in"><img src={itemUrl + '?imageView2/2/w/100/interlace/1/q/100'} /></a></span>
      })}
    </div>)
  }
  componentDidMount() {
    super.componentDidMount()
    if (this.state.viewMode === true) return

    let mp
    $createUploader(this.refs['upload-input'], (res) => {
      if (!mp) mp = new Mprogress({ template: 1, start: true })
      mp.set(res.percent / 100)  // 0.x
    }, (res) => {
      if (mp) mp.end()
      let paths = this.state.value
      paths.push(res.key)
      this.handleChange({ target: { value: paths } }, true)
    })
  }
  removeItem(item) {
    let paths = this.state.value
    paths.remove(item)
    this.handleChange({ target: { value: paths } }, true)
  }
  clickPreview() {
  }
  isValueError() {
    let err = super.isValueError()
    if (err) return err
    let ups = (this.state.value || []).length
    this.setState({ showUploader: this.__maxUpload > ups })
    if (this.__minUpload > 0 && ups < this.__minUpload) return `至少需要上传 ${this.__minUpload} ${this.__typeName}`
    if (this.__maxUpload < ups) return `最多允许上传 ${this.__maxUpload} ${this.__typeName}`
  }
  setValue(val) {
    val = JSON.parse(val || '[]')
    // TODO 检查数量限制
    this.handleChange({ target: { value: val } }, true)
  }
}

// 文件
class RbFormFile extends RbFormImage {
  constructor(props) {
    super(props)
    this.__typeName = '个文件'
  }
  renderElement() {
    return (
      <div className="file-field">
        {this.state.value.map((item) => {
          let fileName = $fileCutName(item)
          let fileExt = $fileExtName(fileName)
          return (<div key={'file-' + item} className="img-thumbnail" title={fileName}>
            <i className="file-icon" data-type={fileExt} /><span>{fileName}</span>
            <b title="移除" onClick={() => this.removeItem(item)}><span className="zmdi zmdi-close"></span></b>
          </div>)
        })}
        {this.state.showUploader === false ? null :
          <div className="file-select">
            <input type="file" className="inputfile" ref="upload-input" id={this.props.field + '-input'} />
            <label title={'上传文件。需要 ' + this.__minUpload + '~' + this.__maxUpload + ' 个'} htmlFor={this.props.field + '-input'} className="btn-secondary">
              <i className="zmdi zmdi-upload"></i><span>上传文件</span>
            </label>
          </div>
        }
        <input ref="field-value" type="hidden" value={this.state.value} />
      </div>
    )
  }
  renderViewElement() {
    if (this.state.value.length === 0) {
      return <div className="form-control-plaintext"><span className="text-muted">无</span></div>
    }
    return (<div className="file-field">
      {this.state.value.map((item) => {
        let fileName = $fileCutName(item)
        return <a key={'file-' + item} title={fileName} onClick={() => (parent || window).RbPreview.create(item)} className="img-thumbnail">
          <i className="file-icon" data-type={$fileExtName(fileName)} /><span>{fileName}</span>
        </a>
      })}
    </div>)
  }
}

// 列表
class RbFormPickList extends RbFormElement {
  constructor(props) {
    super(props)

    if (props.options && props.value) {  // Value has been deleted?
      let deleted = true
      $(props.options).each(function () {
        // eslint-disable-next-line eqeqeq
        if (this.id == props.value) {
          deleted = false
          return false
        }
      })
      if (deleted) props.options.push({ id: props.value, text: '[DELETED]' })
    }
  }
  renderElement() {
    let name = this.state.field + '-opt'
    return (
      <select ref="field-value" className="form-control form-control-sm" value={this.state.value || ''} onChange={this.handleChange}>
        {this.props.options.map((item) => {
          return (<option key={`${name}:${item.id}`} value={item.id}>{item.text}</option>)
        })}
      </select>
    )
  }
  componentDidMount() {
    super.componentDidMount()
    if (this.state.viewMode === true) return

    this.__select2 = $(this.refs['field-value']).select2({
      placeholder: '选择' + this.props.label
    })

    let that = this
    let s2 = this.__select2
    $setTimeout(function () {
      // No value
      if (!that.props.$$$parent.isNew && !that.props.value) {
        s2.val(null)
      }
      s2.on('change', function (e) {
        let val = e.target.value
        that.handleChange({ target: { value: val } }, true)
      }).trigger('change')
    }, 100)
  }
  componentWillUnmount() {
    if (this.__select2) {
      this.__select2.select2('destroy')
      this.__select2 = null
    }
  }
  isValueUnchanged() {
    if (this.props.$$$parent.isNew === true) return false
    return super.isValueUnchanged()
  }
  setValue(val) {
    this.__select2.val(val).trigger('change')
  }
}

// 引用
class RbFormReference extends RbFormElement {
  constructor(props) {
    super(props)
  }
  renderElement() {
    return (
      <select ref={(c) => this._fvalue = c} className="form-control form-control-sm" />
    )
  }
  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()
    let val = this.state.value
    if (typeof val === 'string') {
      return <div className="form-control-plaintext">{val}</div>
    }
    return <div className="form-control-plaintext"><a ref="field-text" href={`#!/View/${val[2]}/${val[0]}`} onClick={() => this.clickView()}>{val[1]}</a></div>
  }
  componentDidMount() {
    super.componentDidMount()
    if (this.state.viewMode === true) return

    const entity = this.props.$$$parent.props.entity
    const field = this.props.field
    let search_input = null
    this.__select2 = $(this._fvalue).select2({
      placeholder: '选择' + this.props.label,
      minimumInputLength: 0,
      maximumSelectionLength: 1,
      ajax: {
        url: rb.baseUrl + '/commons/search/reference',
        delay: 300,
        data: function (params) {
          search_input = params.term
          return { entity: entity, field: field, q: params.term }
        },
        processResults: function (data) {
          return { results: data.data }
        }
      },
      language: {
        noResults: () => { return (search_input || '').length > 0 ? '未找到结果' : '输入关键词搜索' },
        inputTooShort: () => { return '输入关键词搜索' },
        searching: () => { return '搜索中...' },
        maximumSelected: () => { return '只能选择 1 项' }
      }
    })

    let that = this
    let s2 = this.__select2
    $setTimeout(function () {
      let val = that.props.value
      if (val) {
        let o = new Option(val[1], val[0], true, true)
        s2.append(o).trigger('change')
      }

      s2.on('change', function (e) {
        let v = e.target.value
        if (v) {
          $.post(`${rb.baseUrl}/commons/search/recently-add?id=${v}`)
          // 字段回填
          $.post(`${rb.baseUrl}/app/entity/extras/fillin-value?entity=${entity}&field=${that.props.field}&source=${v}`, (res) => {
            if (res.error_code === 0 && res.data.length > 0) that.props.$$$parent.setAutoFillin(res.data)
          })
        }
        that.handleChange({ target: { value: v } }, true)
      })
    }, 100)
  }
  componentWillUnmount() {
    if (this.__select2) {
      this.__select2.select2('destroy')
      this.__select2 = null
    }
  }
  isValueUnchanged() {
    let oldv = this.props.value ? this.props.value[0] : ''
    return $same(oldv, this.state.value || '')
  }
  clickView() {
    if (window.RbViewPage) window.RbViewPage.clickView($(this.refs['field-text']))
  }
  setValue(val) {
    if (val) {
      let o = new Option(val[1], val[0], true, true)
      this.__select2.append(o)
      this.handleChange({ target: { value: val[0] } }, true)
    } else this.__select2.val(null).trigger('change')
  }
}

// 分类数据
class RbFormClassification extends RbFormElement {
  constructor(props) {
    super(props)
  }
  renderElement() {
    return (
      <div className="input-group datetime-field">
        <select ref={(c) => this._fvalue = c} className="form-control form-control-sm" />
        <div className="input-group-append">
          <button className="btn btn-secondary" type="button" onClick={() => this.showSelector()}><i className="icon zmdi zmdi-search" /></button>
        </div>
      </div>
    )
  }
  componentDidMount() {
    super.componentDidMount()
    if (this.state.viewMode === true) return

    const entity = this.props.$$$parent.props.entity
    const field = this.props.field
    let search_input = null
    this.__select2 = $(this._fvalue).select2({
      placeholder: '选择' + this.props.label,
      minimumInputLength: 0,
      maximumSelectionLength: 1,
      ajax: {
        url: rb.baseUrl + '/commons/search/classification',
        delay: 300,
        data: function (params) {
          search_input = params.term
          return { entity: entity, field: field, q: params.term }
        },
        processResults: function (data) {
          return { results: data.data }
        }
      },
      language: {
        noResults: () => { return (search_input || '').length > 0 ? '未找到结果' : '输入关键词搜索' },
        inputTooShort: () => { return '输入关键词搜索' },
        searching: () => { return '搜索中...' },
        maximumSelected: () => { return '只能选择 1 项' }
      }
    })

    // In edits
    let iv = this.state.value
    if (iv) this.giveValue({ id: iv[0], text: iv[1] })

    this.__select2.on('change', () => {
      let v = this.__select2.val()
      if (v) $.post(`${rb.baseUrl}/commons/search/recently-add?id=${v}&type=d${this.props.classification}`)
      this.handleChange({ target: { value: v } }, true)
    })
  }
  componentWillUnmount() {
    if (this.__select2) {
      this.__select2.select2('destroy')
      this.__select2 = null
    }
    if (this.__selector) {
      this.__selector.hide(true)
      this.__selector = null
    }
  }
  isValueUnchanged() {
    let oldv = this.props.value ? this.props.value[0] : ''
    return $same(oldv, this.state.value || '')
  }
  setValue(val) {
    if (val) this.giveValue({ id: val[0], text: val[1] })
    else this.__select2.val(null).trigger('change')
  }

  showSelector() {
    if (this.__selector) this.__selector.show()
    else {
      let p = this.props
      let that = this
      renderRbcomp(<ClassificationSelector entity={p.$$$parent.state.entity} field={p.field} label={p.label} openLevel={p.openLevel} $$$parent={this} />, null, function () { this.__selector = this })
    }
  }
  giveValue(s) {
    let data = this.__data || {}
    if (data[s.id]) {
      this.__select2.val(s.id).trigger('change')
    } else {
      let o = new Option(s.text, s.id, true, true)
      $(this._fvalue).append(o).trigger('change')
      data[s.id] = s.text
      this.__data = data
    }
  }
}

// 多选
class RbFormMultiSelect extends RbFormElement {
  constructor(props) {
    super(props)
    this.changeValue = this.changeValue.bind(this)
  }
  renderElement() {
    let name = 'checkbox-' + this.props.field
    return (
      <div className="mt-1" ref={(c) => this._checkboxes = c}>
        {(this.props.options || []).length === 0 && <div className="text-danger">未配置选项</div>}
        {(this.props.options || []).map((item) => {
          return <label key={name + ':' + item.mask} className="custom-control custom-control-sm custom-checkbox  custom-control-inline">
            <input className="custom-control-input" name={name} type="checkbox" checked={(this.state.value & item.mask) !== 0} value={item.mask} onChange={this.changeValue} />
            <span className="custom-control-label">{item.text}</span>
          </label>
        })}
      </div>
    )
  }
  changeValue(e) {
    let maskValue = 0
    $(this._checkboxes).find('input:checked').each(function () {
      maskValue += ~~$(this).val()
    })
    this.handleChange({ target: { value: maskValue === 0 ? null : maskValue } }, true)
  }
  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()
    return <div className="form-control-plaintext">
      {this.state.value.split(' / ').map((item) => {
        return <span key={'opt-' + item} className="badge">{item}</span>
      })}
    </div>
  }
}

// 分割线
class RbFormDivider extends React.Component {
  constructor(props) {
    super(props)
  }
  render() {
    let label = this.props.label || ''
    if (label === '分栏线') label = null
    if (this.props.onView === true) return <div className="form-line" ref={(c) => this._element = c}><fieldset>{label && <legend onClick={this.toggleNexts}>{label}</legend>}</fieldset></div>
    else return <div />  // TODO 编辑页暂无分割线
  }
  toggleNexts = () => {
    // let $next = $(this._element).parent()
    // while (($next = $next.next()).length > 0) {
    //   if ($next.find('>.form-line').length > 0) break
    //   $next.toggleClass('hide')
    // }
  }
}

// 不支持/未开放的字段
class RbFormUnsupportted extends RbFormElement {
  constructor(props) {
    super(props)
  }
  renderElement() {
    return <div className="form-control-plaintext text-warning">UNSUPPORTTED</div>
  }
  renderViewElement() {
    return this.renderElement()
  }
}

// 确定元素类型
const detectElement = function (item) {
  if (!item.key) item.key = 'field-' + (item.field === '$DIVIDER$' ? $random() : item.field)

  let isExtElement = detectElementExt(item)
  if (isExtElement !== null) {
    return isExtElement
  }

  if (item.onView === true) {
    // 根据各组件渲染
  } else if (item.readonly === true) {
    return <RbFormReadonly {...item} />
  }

  if (item.type === 'TEXT' || item.type === 'SERIES') {
    return <RbFormText {...item} />
  } else if (item.type === 'NTEXT') {
    return <RbFormTextarea {...item} />
  } else if (item.type === 'URL') {
    return <RbFormUrl {...item} />
  } else if (item.type === 'EMAIL') {
    return <RbFormEMail {...item} />
  } else if (item.type === 'PHONE') {
    return <RbFormPhone {...item} />
  } else if (item.type === 'NUMBER') {
    return <RbFormNumber {...item} />
  } else if (item.type === 'DECIMAL') {
    return <RbFormDecimal {...item} />
  } else if (item.type === 'IMAGE') {
    return <RbFormImage {...item} />
  } else if (item.type === 'FILE') {
    return <RbFormFile {...item} />
  } else if (item.type === 'DATETIME' || item.type === 'DATE') {
    return <RbFormDateTime {...item} />
  } else if (item.type === 'PICKLIST') {
    return <RbFormPickList {...item} />
  } else if (item.type === 'REFERENCE') {
    return <RbFormReference {...item} />
  } else if (item.type === 'CLASSIFICATION') {
    return <RbFormClassification {...item} />
  } else if (item.type === 'MULTISELECT') {
    return <RbFormMultiSelect {...item} />
  } else if (item.field === '$LINE$' || item.field === '$DIVIDER$') {
    return <RbFormDivider {...item} />
  } else {
    // throw new Error('Unknow element : ' + JSON.stringify(item))
    return <RbFormUnsupportted {...item} />
  }
}
var detectElementExt = function (item) {
  return null
}

// 分类数据选择
class ClassificationSelector extends React.Component {
  constructor(props) {
    super(props)
    this._select = []
    this._select2 = []
    this.state = { openLevel: props.openLevel || 0, datas: [] }
  }
  render() {
    return (
      <div className="modal selector" ref={(c) => this._dlg = c} tabIndex="-1">
        <div className="modal-dialog">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()}><span className="zmdi zmdi-close" /></button>
            </div>
            <div className="modal-body">
              <h5 className="mt-0 text-bold">选择{this.props.label}</h5>
              <div>
                <select ref={(c) => this._select.push(c)} className="form-control form-control-sm">
                  {(this.state.datas[0] || []).map((item) => {
                    return <option key={'item-' + item[0]} value={item[0]}>{item[1]}</option>
                  })}
                </select>
              </div>
              {this.state.openLevel >= 1 &&
                <div>
                  <select ref={(c) => this._select.push(c)} className="form-control form-control-sm">
                    {(this.state.datas[1] || []).map((item) => {
                      return <option key={'item-' + item[0]} value={item[0]}>{item[1]}</option>
                    })}
                  </select>
                </div>}
              {this.state.openLevel >= 2 &&
                <div>
                  <select ref={(c) => this._select.push(c)} className="form-control form-control-sm">
                    {(this.state.datas[2] || []).map((item) => {
                      return <option key={'item-' + item[0]} value={item[0]}>{item[1]}</option>
                    })}
                  </select>
                </div>}
              {this.state.openLevel >= 3 &&
                <div>
                  <select ref={(c) => this._select.push(c)} className="form-control form-control-sm">
                    {(this.state.datas[3] || []).map((item) => {
                      return <option key={'item-' + item[0]} value={item[0]}>{item[1]}</option>
                    })}
                  </select>
                </div>}
              <div>
                <button className="btn btn-primary w-100" onClick={() => this.confirm()}>确定</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }
  componentDidMount() {
    let m = this.show()
    m.on('hidden.bs.modal', () => {
      $(document.body).addClass('modal-open')  // keep scroll
    })

    let LN = ['一', '二', '三', '四']
    let that = this
    $(this._select).each(function (idx) {
      let s = $(this).select2({
        placeholder: '选择' + LN[idx] + '分类',
        allowClear: false
      }).on('change', () => {
        let p = $(s).val()
        if (p) {
          if (s.__level < that.state.openLevel) {
            that.loadData(s.__level + 1, p)  // Load next-level
          }
        }
      })
      s.__level = idx
      that._select2.push(s)
    })
    this.loadData(0)
  }
  loadData(level, p) {
    $.get(`${rb.baseUrl}/commons/metadata/classification?entity=${this.props.entity}&field=${this.props.field}&parent=${p || ''}`, (res) => {
      let s = this.state.datas
      s[level] = res.data
      this.setState({ datas: s }, () => {
        this._select2[level].trigger('change')
      })
    })
  }
  confirm() {
    let last = this._select2[this.state.openLevel]
    let v = last.val()
    if (!v) {
      RbHighbar.create('选择有误')
    } else {
      let text = []
      $(this._select2).each(function () {
        text.push(this.select2('data')[0].text)
      })
      this.props.$$$parent.giveValue({ id: v, text: text.join('.') })
      this.hide()
    }
  }
  show() {
    return $(this._dlg).modal({ show: true, keyboard: true })
  }
  hide(dispose) {
    $(this._dlg).modal('hide')
    if (dispose === true) $unmount($(this._dlg).parent())
  }
}

// 删除确认
// eslint-disable-next-line no-undef
class DeleteConfirm extends RbAlert {
  constructor(props) {
    super(props)
    this.state = { enableCascades: false }
  }
  render() {
    let message = this.props.message
    if (!message) message = this.props.ids ? `确认删除选中的 ${this.props.ids.length} 条记录？` : '确认删除当前记录？'
    return (
      <div className="modal rbalert" ref={(c) => this._dlg = c} tabIndex="-1">
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()}><span className="zmdi zmdi-close" /></button>
            </div>
            <div className="modal-body">
              <div className="text-center ml-6 mr-6">
                <div className="text-danger"><span className="modal-main-icon zmdi zmdi-alert-triangle" /></div>
                <div className="mt-3 text-bold">{message}</div>
                {!this.props.entity ? null :
                  <div className="mt-2">
                    <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-2">
                      <input className="custom-control-input" type="checkbox" checked={this.state.enableCascade === true} onChange={() => this.enableCascade()} />
                      <span className="custom-control-label"> 同时删除关联记录</span>
                    </label>
                    <div className={' ' + (this.state.enableCascade ? '' : 'hide')}>
                      <select className="form-control form-control-sm" ref={(c) => this._cascades = c} multiple>
                        {(this.state.cascadesEntity || []).map((item) => {
                          return <option key={'option-' + item[0]} value={item[0]}>{item[1]}</option>
                        })}
                      </select>
                    </div>
                  </div>
                }
                <div className="mt-4 mb-3" ref={(c) => this._btns = c}>
                  <button className="btn btn-space btn-secondary" type="button" onClick={() => this.hide()}>取消</button>
                  <button className="btn btn-space btn-danger" type="button" onClick={() => this.deleteAction()}>删除</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }
  enableCascade() {
    this.setState({ enableCascade: !this.state.enableCascade })
    if (!this.state.cascadesEntity) {
      $.get(rb.baseUrl + '/commons/metadata/references?entity=' + this.props.entity, (res) => {
        this.setState({ cascadesEntity: res.data }, () => {
          this.__select2 = $(this._cascades).select2({
            placeholder: '选择关联实体 (可选)',
            width: '88%'
          }).val(null).trigger('change')
        })
      })
    }
  }
  deleteAction() {
    let ids = this.props.ids || this.props.id
    if (!ids || ids.length === 0) return
    if (typeof ids === 'object') ids = ids.join(',')
    let cascades = this.__select2 ? this.__select2.val().join(',') : ''

    let btns = $(this._btns).find('.btn').button('loading')
    $.post(rb.baseUrl + '/app/entity/record-delete?id=' + ids + '&cascades=' + cascades, (res) => {
      if (res.error_code === 0) {
        if (res.data.deleted === res.data.requests) RbHighbar.success('删除成功')
        else if (res.data.deleted === 0) RbHighbar.error('无法删除选中记录')
        else RbHighbar.success('成功删除 ' + res.data.deleted + ' 条记录')

        this.hide()
        typeof this.props.deleteAfter === 'function' && this.props.deleteAfter()
      } else {
        RbHighbar.error(res.error_msg)
        btns.button('reset')
      }
    })
  }
}

// ~ 重复记录查看
class RepeatedViewer extends RbModalHandler {
  constructor(props) {
    super(props)
  }

  render() {
    const data = this.props.data
    return <RbModal ref={(c) => this._dlg = c} title={`存在${this.props.data.length - 1}条重复记录`} disposeOnHide={true} colored="warning">
      <table className="table table-hover repeated-table">
        <thead>
          <tr>
            {data[0].map((item, idx) => {
              if (idx === 0) return null
              return <th key={`field-${idx}`}>{item}</th>
            })}
            <th width="50"></th>
          </tr>
        </thead>
        <tbody>
          {data.map((item, idx) => {
            if (idx === 0) return null
            return this.renderOne(item, idx)
          })}
        </tbody>
      </table>
    </RbModal>
  }

  renderOne(item, idx) {
    return <tr key={`row-${idx}`}>
      {item.map((o, i) => {
        if (i === 0) return null
        return <td key={`col-${idx}-${i}`}>{o || <span className="text-muted">无</span>}</td>
      })}
      <td className="actions"><a className="icon" title="查看详情" onClick={() => this.openView(item[0])}><i className="zmdi zmdi-open-in-new" /></a></td>
    </tr>
  }

  openView(id) {
    if (window.RbViewModal) window.RbViewModal.create({ id: id, entity: this.props.entity })
    else window.open(`${rb.baseUrl}/app/${this.props.entity}/list#!/View/${this.props.entity}/${id}`)
  }
}