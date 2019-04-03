/* eslint-disable no-unused-vars */
/* eslint-disable react/prop-types */
/* eslint-disable react/no-string-refs */
// ~~ 表单 Dialog
class RbFormModal extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, inLoad: true }
  }
  render() {
    return (this.state.isDestroy === true ? null :
      <div className="modal-warpper">
        <div className="modal rbmodal colored-header colored-header-primary" ref="rbmodal">
          <div className="modal-dialog">
            <div className="modal-content">
              <div className="modal-header modal-header-colored">
                {this.state.icon ? (<span className={'icon zmdi zmdi-' + this.state.icon}></span>) : ''}
                <h3 className="modal-title">{this.state.title || '新建'}</h3>
                {rb.isAdminUser ? <a className="close s" href={rb.baseUrl + '/admin/entity/' + this.state.entity + '/form-design'} title="配置布局" target="_blank" rel="noopener noreferrer"><span className="zmdi zmdi-settings"></span></a> : null}
                <button className="close md-close" type="button" onClick={() => this.hide()}><span className="zmdi zmdi-close"></span></button>
              </div>
              <div className={'modal-body rb-loading' + (this.state.inLoad ? ' rb-loading-active' : '')}>
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
    })
  }
  renderFromError(message) {
    let error = <div className="alert alert-danger alert-icon mt-5 w-75 mlr-auto">
      <div className="icon"><i className="zmdi zmdi-alert-triangle"></i></div>
      <div className="message" dangerouslySetInnerHTML={{ __html: '<strong>抱歉!</strong> ' + message }}></div>
    </div>

    let that = this
    that.setState({ formComponent: error }, function () {
      that.setState({ inLoad: false })
    })
  }

  show(state) {
    state = state || {}
    let that = this
    if ((state.id !== this.state.id || state.entity !== this.state.entity) || this.state.isDestroy === true) {
      state = { ...state, isDestroy: true, formComponent: null, inLoad: true, id: state.id, entity: state.entity }
      this.setState(state, function () {
        that.showAfter({ ...state, isDestroy: false }, true)
      })
    } else {
      this.showAfter({ ...state, isDestroy: false })
    }
  }
  showAfter(state, modelChanged) {
    let that = this
    this.setState(state, function () {
      $(that.refs['rbmodal']).modal({ show: true, backdrop: 'static' })
      if (modelChanged === true) {
        that.getFormModel()
      }
    })
  }

  hide(destroy) {
    $(this.refs['rbmodal']).modal('hide')
    let state = { isDestroy: destroy === true }
    if (destroy === true) state.id = null
    this.setState(state)
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
    let that = this
    return (
      <div className="rbform">
        <form>
          {this.props.children.map((child) => {
            return React.cloneElement(child, { $$$parent: that })
            // Has error in strict-mode
            //child.$$$parent = that; return child
          })}
          {this.renderFormAction()}
        </form>
      </div>
    )
  }
  renderFormAction() {
    let pmodel = this.props.$$$parent.state.__formModel
    let saveBtns = (
      <div className="btn-group dropup btn-space">
        <button className="btn btn-primary" type="button" onClick={() => this.post()}>保存</button>
        <button className="btn btn-primary dropdown-toggle auto" type="button" data-toggle="dropdown"><span className="icon zmdi zmdi-chevron-up"></span></button>
        <div className="dropdown-menu dropdown-menu-primary dropdown-menu-right">
          {pmodel.isSlave === true && <a className="dropdown-item" onClick={() => this.post(101)}>保存并继续添加</a>}
          {pmodel.isMaster === true && <a className="dropdown-item" onClick={() => this.post(102)}>保存并添加明细</a>}
          {pmodel.isSlave !== true && <a className="dropdown-item" onClick={() => this.post(101)}>保存并继续新建</a>}
        </div>
      </div>
    )

    let entity = this.state.entity
    let wpc = window.__PageConfig
    if (entity === 'User' || entity === 'Department' || entity === 'Role'
      || wpc.type === 'SlaveView' || wpc.type === 'SlaveList' || this.isNew !== true) {
      saveBtns = <button className="btn btn-primary btn-space" type="button" onClick={() => this.post()}>保存</button>
    }

    return (
      <div className="form-group row footer">
        <div className="col-12 col-sm-8 offset-sm-3" ref="rbform-action">
          {saveBtns}
          <button className="btn btn-secondary btn-space" type="button" onClick={() => this.props.$$$parent.hide()}>取消</button>
        </div>
      </div>
    )
  }

  componentDidMount() {
    if (this.isNew === true) {
      let that = this
      this.props.children.map((child) => {
        let val = child.props.value
        if (val) {
          if ($.type(val) === 'array') val = val[0]  // 若为数组，第一个就是真实值
          that.setFieldValue(child.props.field, val)
        }
      })
    }
  }

  setFieldValue(field, value, error) {
    this.__FormData[field] = { value: value, error: error }
  }
  // 避免无意义更新
  setFieldUnchanged(field) {
    if (this.isNew === true) return
    delete this.__FormData[field]
  }
  post(next) {
    let _data = {}
    for (let k in this.__FormData) {
      let err = this.__FormData[k].error
      if (err) { rb.highbar(err); return }
      else _data[k] = this.__FormData[k].value
    }

    _data.metadata = { entity: this.state.entity, id: this.state.id }
    if (RbForm.postBefore(_data) === false) {
      return
    }

    let btns = $(this.refs['rbform-action']).find('.btn').button('loading')
    let that = this
    $.post(`${rb.baseUrl}/app/entity/record-save`, JSON.stringify(_data), function (res) {
      btns.button('reset')
      if (res.error_code === 0) {
        rb.highbar('保存成功', 'success')
        setTimeout(() => {
          that.props.$$$parent.hide(true)
          RbForm.postAfter(res.data, next === 101)

          if (next === 101) {
            let pstate = that.props.$$$parent.state
            rb.RbFormModal({ title: pstate.title, entity: pstate.entity, icon: pstate.icon })
          } else if (next === 102) {
            let iv = { '$MASTER$': res.data.id }
            let sm = that.props.$$$parent.state.__formModel.slaveMeta
            rb.RbFormModal({ title: `添加${sm[1]}`, entity: sm[0], icon: sm[2], initialValue: iv })
          }
        }, 500)

      } else {
        rb.hberror(res.error_msg)
      }
    })
  }

  // 保存前调用
  // @return false 则不继续保存
  static postBefore(data) {
    return true
  }
  // 保存后调用
  static postAfter(data, notReload) {
    let rlp = window.RbListPage || parent.RbListPage
    if (rlp) rlp._RbList.reload()
    if (window.RbViewPage && notReload !== true) location.reload()
  }
}

// 表单元素
class RbFormElement extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }

    this.handleChange = this.handleChange.bind(this)
    this.checkError = this.checkError.bind(this)
    this.handleClear = this.handleClear.bind(this)
  }

  render() {
    let colWidths = [3, 8]
    if (this.props.onView) {
      colWidths[0] = 4
      if (this.props.isFull === true) colWidths = [2, 10]
    }
    return (
      <div className={'form-group row type-' + this.props.type}>
        <label className={'col-12 col-form-label text-sm-right col-sm-' + colWidths[0]} title={this.props.nullable ? '' : '必填项'}>{this.props.label}{!this.props.nullable && <em />}</label>
        <div className={'col-12 col-sm-' + colWidths[1]}>
          {this.state.viewMode === true ? this.renderViewElement() : this.renderElement()}
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
  }

  // 表单组件（字段）值变化应调用此方法
  handleChange(event, checkError) {
    let val = event.target.value
    this.setState({ value: val }, () => { checkError === true && this.checkError() })
  }
  checkError() {
    // Unchanged
    if (this.state.value && $same(this.props.value, this.state.value)) {
      // Clean change status
      if (!$same(this.__lastValue, this.props.value)) {
        this.props.$$$parent.setFieldUnchanged(this.props.field)
        this.__lastValue = this.props.value
      }
      return
    }
    if (this.__lastValue && $same(this.__lastValue, this.state.value)) {
      return
    }
    // If it's array, using clones
    this.__lastValue = $.type(this.state.value) === 'array' ? this.state.value.concat() : this.state.value

    let err = this.checkHasError()
    this.setState({ hasError: err })
    let errMsg = err ? (this.props.label + err) : null
    this.props.$$$parent.setFieldValue(this.props.field, this.state.value, errMsg)
  }
  checkHasError() {
    if (this.props.nullable === false) {
      let v = this.state.value
      if (v && $.type(v) === 'array') return v.length === 0 ? '不能为空' : null
      else return !v ? '不能为空' : null
    }
  }
  // 清空值
  handleClear() {
    this.setState({ value: '' }, () => { this.checkError() })
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
    return <input className="form-control form-control-sm" type="text" readOnly="true" value={text} />
  }
}

// 文本
class RbFormText extends RbFormElement {
  constructor(props) {
    super(props)
  }
  renderElement() {
    return (
      <input ref="field-value" className={'form-control form-control-sm ' + (this.state.hasError ? 'is-invalid' : '')} title={this.state.hasError} type="text" value={this.state.value || ''} onChange={this.handleChange} onBlur={this.checkError} />
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
  checkHasError() {
    let err = super.checkHasError()
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
  checkHasError() {
    let err = super.checkHasError()
    if (err) return err
    return (!!this.state.value && $regex.isMail(this.state.value) === false) ? '邮箱格式不正确' : null
  }
}

// 电话/手机
class RbFormPhone extends RbFormText {
  constructor(props) {
    super(props)
  }
  checkHasError() {
    let err = super.checkHasError()
    if (err) return err
    return (!!this.state.value && $regex.isTel(this.state.value) === false) ? '电话/手机格式不正确' : null
  }
}

// 整数
class RbFormNumber extends RbFormText {
  constructor(props) {
    super(props)
  }
  checkHasError() {
    let err = super.checkHasError()
    if (err) return err
    return (!!this.state.value && $regex.isNumber(this.state.value) === false) ? '整数格式不正确' : null
  }
}

// 货币
class RbFormDecimal extends RbFormText {
  constructor(props) {
    super(props)
  }
  checkHasError() {
    let err = super.checkHasError()
    if (err) return err
    return (!!this.state.value && $regex.isDecimal(this.state.value) === false) ? '货币格式不正确' : null
  }
}

// 多行文本
class RbFormTextarea extends RbFormElement {
  constructor(props) {
    super(props)
  }
  renderElement() {
    return (<textarea ref="field-value" className={'form-control form-control-sm row3x ' + (this.state.hasError ? 'is-invalid' : '')} title={this.state.hasError} value={this.state.value || ''} onChange={this.handleChange} onBlur={this.checkError} />)
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
        <input ref="field-value" className={'form-control form-control-sm ' + (this.state.hasError ? 'is-invalid' : '')} title={this.state.hasError} type="text" value={this.state.value || ''} onChange={this.handleChange} onBlur={this.checkError} />
        <span className={'zmdi zmdi-close clean ' + (this.state.value ? '' : 'hide')} onClick={this.handleClear}></span>
        <div className="input-group-append">
          <button className="btn btn-primary" type="button" ref="field-value-icon"><i className="icon zmdi zmdi-calendar"></i></button>
        </div>
      </div>
    )
  }
  componentDidMount() {
    super.componentDidMount()
    if (this.state.viewMode === true) return

    let format = (this.props.datetimeFormat || this.props.dateFormat).replace('mm', 'ii').toLowerCase()
    let minView = 0
    switch (format.length) {
    case 7:
      minView = 'year'
      break
    case 10:
      minView = 'month'
      break
    default:
      break
    }

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
  }
  renderElement() {
    return (
      <div className="img-field">
        {this.state.value.map((item) => {
          let itemUrl = rb.baseUrl + '/cloud/img/' + item
          let fileName = $fileCutName(item)
          return (<span key={'file-' + item}><a title={fileName} className="img-thumbnail img-upload"><img src={itemUrl + '?imageView2/2/w/100/interlace/1/q/100'} /><b title="移除" onClick={() => this.removeItem(item)}><span className="zmdi zmdi-close"></span></b></a></span>)
        })}
        {this.state.showUploader === false ? null :
          <span title={'上传图片。需要 ' + this.__minUpload + '～' + this.__maxUpload + ' 张'}>
            <input type="file" className="inputfile" ref="upload-input" id={this.props.field + '-input'} accept="image/*" />
            <label htmlFor={this.props.field + '-input'} className="img-thumbnail img-upload"><span className="zmdi zmdi-image-alt"></span></label>
          </span>
        }
        <input ref="field-value" type="hidden" value={this.state.value} />
      </div>
    )
  }
  renderViewElement() {
    return (<div className="img-field">
      {this.state.value.map((item) => {
        let itemUrl = rb.baseUrl + '/cloud/img/' + item
        let fileName = $fileCutName(item)
        return <span key={'img-' + item}><a title={fileName} onClick={this.clickPreview.bind(this, itemUrl)} className="img-thumbnail img-upload zoom-in" href={itemUrl} target="_blank" rel="noopener noreferrer"><img src={itemUrl + '?imageView2/2/w/100/interlace/1/q/100'} /></a></span>
      })}
    </div>)
  }
  componentDidMount() {
    super.componentDidMount()
    if (this.state.viewMode === true) return

    let that = this
    let mprogress
    $(that.refs['upload-input']).html5Uploader({
      name: that.props.field,
      postUrl: rb.baseUrl + '/filex/upload?cloud=auto&type=image',
      onSelectError: function (field, error) {
        if (error === 'ErrorType') rb.highbar('请上传图片')
      },
      onClientLoad: function (e, file) {
        mprogress = new Mprogress({ template: 3 })
        mprogress.start()
      },
      onSuccess: function (d) {
        if (mprogress === null) return false
        mprogress.end()
        d = JSON.parse(d.currentTarget.response)
        if (d.error_code === 0) {
          let paths = that.state.value
          paths.push(d.data)
          that.handleChange({ target: { value: paths } }, true)
        } else rb.hberror(d.error_msg || '上传失败，请稍后重试')
      }
    })
  }
  removeItem(item) {
    let paths = this.state.value
    paths.remove(item)
    this.handleChange({ target: { value: paths } }, true)
  }
  clickPreview() {
  }
  checkHasError() {
    let err = super.checkHasError()
    if (err) return err
    let ups = (this.state.value || []).length
    this.setState({ showUploader: this.__maxUpload > ups })
    if (this.__minUpload > 0 && ups < this.__minUpload) return `至少需要上传 ${this.__minUpload} 张图片`
    if (this.__maxUpload < ups) return `最多允许上传 ${this.__maxUpload} 张图片`
  }
}

// 文件
class RbFormFile extends RbFormElement {
  constructor(props) {
    super(props)
    this.state.value = JSON.parse(props.value || '[]')
    this.__minUpload = 0
    this.__maxUpload = 9
    if (this.props.uploadNumber) {
      this.__minUpload = ~~(this.props.uploadNumber.split(',')[0] || 0)
      this.__maxUpload = ~~(this.props.uploadNumber.split(',')[1] || 9)
    }
  }
  renderElement() {
    return (
      <div className="file-field">
        {this.state.value.map((item) => {
          let fileName = $fileCutName(item)
          let fileIcon = $fileDetectingIcon(fileName)
          return (<div key={'file-' + item} className="img-thumbnail" title={fileName}><i className={'ftype ' + fileIcon} /><span>{fileName}</span><b title="移除" onClick={() => this.removeItem(item)}><span className="zmdi zmdi-close"></span></b></div>)
        })}
        {this.state.showUploader === false ? null :
          <div className="file-select">
            <input type="file" className="inputfile" ref="upload-input" id={this.props.field + '-input'} />
            <label title={'上传文件。需要 ' + this.__minUpload + '～' + this.__maxUpload + ' 个'} htmlFor={this.props.field + '-input'} className="btn-secondary">
              <i className="zmdi zmdi-upload"></i><span>上传文件</span>
            </label>
          </div>
        }
        <input ref="field-value" type="hidden" value={this.state.value} />
      </div>
    )
  }
  renderViewElement() {
    return (<div className="file-field">
      {this.state.value.map((item) => {
        let itemUrl = rb.baseUrl + '/cloud/download/' + item
        let fileName = $fileCutName(item)
        let fileIcon = $fileDetectingIcon(fileName)
        return <a key={'file-' + item} title={fileName} onClick={this.clickPreview.bind(this, itemUrl)} className="img-thumbnail" href={itemUrl} target="_blank" rel="noopener noreferrer"><i className={'ftype ' + fileIcon} /><span>{fileName}</span></a>
      })}
    </div>)
  }
  componentDidMount() {
    super.componentDidMount()
    if (this.state.viewMode === true) return

    let that = this
    let mprogress
    $(that.refs['upload-input']).html5Uploader({
      name: that.props.field,
      postUrl: rb.baseUrl + '/filex/upload?cloud=auto',
      onClientLoad: function (e, file) {
        mprogress = new Mprogress({ template: 3 })
        mprogress.start()
      },
      onSuccess: function (d) {
        mprogress.end()
        d = JSON.parse(d.currentTarget.response)
        if (d.error_code === 0) {
          let paths = that.state.value
          paths.push(d.data)
          that.handleChange({ target: { value: paths } }, true)
        } else rb.hberror(d.error_msg || '上传失败，请稍后重试')
      }
    })
  }
  removeItem(item) {
    let paths = this.state.value
    paths.remove(item)
    this.handleChange({ target: { value: paths } }, true)
  }
  clickPreview() {
  }
  checkHasError() {
    let err = super.checkHasError()
    if (err) return err
    let ups = (this.state.value || []).length
    this.setState({ showUploader: this.__maxUpload > ups })
    if (this.__minUpload > 0 && ups < this.__minUpload) return `至少需要上传 ${this.__minUpload} 个文件`
    if (this.__maxUpload < ups) return `最多允许上传 ${this.__maxUpload} 个文件`
  }
}

// 列表
class RbFormPickList extends RbFormElement {
  constructor(props) {
    super(props)
  }
  renderElement() {
    return (
      <select ref="field-value" className="form-control form-control-sm" value={this.state.value || ''} onChange={this.handleChange}>
        {this.props.options.map((item) => {
          return (<option key={item.field + '-' + item.id} value={item.id}>{item.text}</option>)
        })}
      </select>
    )
  }
  componentDidMount() {
    super.componentDidMount()
    if (this.state.viewMode === true) return

    let select2 = $(this.refs['field-value']).select2({
      placeholder: '选择' + this.props.label
    })
    this.__select2 = select2

    let that = this
    $setTimeout(function () {
      // 没有值
      if (that.props.$$$parent.isNew === false && !that.props.value) {
        select2.val(null)
      }
      select2.trigger('change')
      select2.on('change.select2', function (e) {
        let val = e.target.value
        that.handleChange({ target: { value: val } }, true)
      })
    }, 100)
  }
  componentWillUnmount() {
    if (this.__select2) {
      this.__select2.select2('destroy')
      this.__select2 = null
    }
  }
}

// 引用
class RbFormReference extends RbFormElement {
  constructor(props) {
    super(props)
  }
  renderElement() {
    return (
      <select ref="field-value" className="form-control form-control-sm" multiple="multiple" />
    )
  }
  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()
    let val = this.state.value
    return <div className="form-control-plaintext"><a ref="field-text" href={`#!/View/${val[2]}/${val[0]}`} onClick={() => this.clickView()}>{val[1]}</a></div>
  }
  componentDidMount() {
    super.componentDidMount()
    if (this.state.viewMode === true) return

    let that = this
    let select2 = $(this.refs['field-value']).select2({
      placeholder: '选择' + this.props.label,
      minimumInputLength: 1,
      maximumSelectionLength: 1,
      ajax: {
        url: rb.baseUrl + '/app/entity/reference-search',
        delay: 300,
        data: function (params) {
          let query = {
            entity: that.props.$$$parent.props.entity,
            field: that.props.field,
            q: params.term,
          }
          return query
        },
        processResults: function (data) {
          let rs = data.data.map((item) => { return item })
          return { results: rs }
        }
      }
    })
    this.__select2 = select2

    $setTimeout(function () {
      let val = that.props.value
      if (val) {
        let option = new Option(val[1], val[0], true, true)
        select2.append(option)
      }
      select2.trigger('change')
      select2.on('change.select2', function (e) {
        // TODO Clear 触发两次 ???
        that.handleChange({ target: { value: e.target.value } }, true)
      })
    }, 100)
  }
  componentWillUnmount() {
    if (this.__select2) {
      this.__select2.select2('destroy')
      this.__select2 = null
    }
  }
  clickView() {
    if (window.RbViewPage) window.RbViewPage.clickView($(this.refs['field-text']))
  }
}

// 头像
class RbFormAvatar extends RbFormElement {
  constructor(props) {
    super(props)
  }
  renderElement() {
    let aUrl = rb.baseUrl + (this.state.value ? `/cloud/img/${this.state.value}?imageView2/2/w/100/interlace/1/q/100` : '/assets/img/avatar.png')
    return (
      <div className="img-field avatar">
        <span title="选择头像图片">
          <input type="file" className="inputfile" ref="upload-input" id={this.props.field + '-input'} accept="image/*" />
          <label htmlFor={this.props.field + '-input'} className="img-thumbnail img-upload">
            <img src={aUrl} />
          </label>
        </span>
      </div>
    )
  }
  renderViewElement() {
    let aUrl = rb.baseUrl + (this.state.value ? `/cloud/img/${this.state.value}?imageView2/2/w/100/interlace/1/q/100` : '/assets/img/avatar.png')
    return (
      <div className="img-field avatar">
        <a className="img-thumbnail img-upload"><img src={aUrl} /></a>
      </div>
    )
  }
  componentDidMount() {
    super.componentDidMount()
    let that = this
    $(that.refs['upload-input']).html5Uploader({
      name: that.props.field,
      postUrl: rb.baseUrl + '/filex/upload?cloud=auto&type=image',
      onClientLoad: function (e, file) {
        if (file.type.substr(0, 5) !== 'image') {
          rb.highbar('请上传图片')
          return false
        }
      },
      onSuccess: function (d) {
        d = JSON.parse(d.currentTarget.response)
        if (d.error_code === 0) {
          that.handleChange({ target: { value: d.data } }, true)
        } else rb.hberror(d.error_msg || '上传失败，请稍后重试')
      }
    })
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
        <select ref={(c) => this._fvalue = c} className="form-control form-control-sm">
          {this.state.data && <option value={this.state.data.id}>{this.state.data.text}</option>}
        </select>
        <span className={'zmdi zmdi-close clean ' + (this.state.value ? '' : 'hide')} onClick={this.handleClear}></span>
        <div className="input-group-append">
          <button className="btn btn-secondary" type="button" onClick={() => this.showSelector()}><i className="icon zmdi zmdi-search" /></button>
        </div>
      </div>
    )
  }
  componentDidMount() {
    super.componentDidMount()
    if (this.state.viewMode === true) return

    this._select2 = $(this._fvalue).select2({
      placeholder: '选择'
    })
  }
  componentWillUnmount() {
  }

  showSelector() {
    if (this.__selector) {
      this.__selector.show()
    } else {
      this.__selector = renderRbcomp(<ClassificationSelector entity={this.props.$$$parent.state.entity} field={this.props.field} $$$parent={this} />)
    }
  }
  giveValue(s) {
    this.setState({ data: s })
  }
}

// 分割线
class RbFormDivider extends React.Component {
  constructor(props) {
    super(props)
  }
  render() {
    let label = this.props.label || ''
    if (label === '分栏') label = null
    if (this.props.onView === true) return <div className="form-line"><fieldset>{label ? (<legend>{label}</legend>) : null}</fieldset></div>
    else return <div />  // TODO 编辑页暂无分割线
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
  } else if (item.type === 'AVATAR') {
    return <RbFormAvatar {...item} />
  } else if (item.type === 'CLASSIFICATION') {
    return <RbFormClassification {...item} />
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

// -- for View

//~~ 右侧滑出视图窗口
class RbViewModal extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, inLoad: true, isHide: true, isDestroy: false }
    this.mcWidth = this.props.subView === true ? 1170 : 1220
    if ($(window).width() < 1280) this.mcWidth -= 100
  }
  render() {
    return (this.state.isDestroy === true ? null :
      <div className="modal-warpper">
        <div className="modal rbview" ref="rbview">
          <div className="modal-dialog">
            <div className="modal-content" style={{ width: this.mcWidth + 'px' }}>
              <div className={'modal-body iframe rb-loading ' + (this.state.inLoad === true && 'rb-loading-active')}>
                <iframe className={this.state.isHide ? 'invisible' : ''} src={this.state.showAfterUrl || 'about:blank'} frameBorder="0" scrolling="no"></iframe>
                <RbSpinner />
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }
  componentDidMount() {
    let that = this
    let root = $(this.refs['rbview'])
    let mc = root.find('.modal-content')
    root.on('hidden.bs.modal', function () {
      mc.css({ 'margin-right': -1500 })
      that.setState({ inLoad: true, isHide: true })

      // 如果还有其他 rbview 处于 open 态， 则保持 modal-open
      if ($('.rbview.show').length > 0) $(document.body).addClass('modal-open').css({ 'padding-right': 17 })
      // subView always dispose
      if (that.state.disposeOnHide === true) {
        root.modal('dispose')
        let warp = root.parent().parent()
        that.setState({ isDestroy: true }, function () {
          rb.__currentRbFormModalCache[that.state.id] = null
          setTimeout(function () { warp.remove() }, 500)
        })
      }

    }).on('shown.bs.modal', function () {
      mc.css('margin-right', 0)
      let mcs = $('body>.modal-backdrop.show')
      if (mcs.length > 1) {
        mcs.addClass('o')
        mcs.eq(0).removeClass('o')
      }
    })
    this.show()
  }
  hideLoading() {
    this.setState({ inLoad: false, isHide: false })
  }
  show(url, ext) {
    let urlChanged = true
    if (url && url === this.state.url) urlChanged = false
    ext = ext || {}
    url = url || this.state.url
    let root = $(this.refs['rbview'])
    let that = this
    this.setState({ ...ext, url: url, inLoad: urlChanged, isHide: urlChanged }, function () {
      root.modal({ show: true, backdrop: true })
      setTimeout(function () {
        that.setState({ showAfterUrl: that.state.url })
      }, 400)
    })
  }
  hide() {
    let root = $(this.refs['rbview'])
    root.modal('hide')
  }
}

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
      <div className="modal rbalert" ref="dlg" tabIndex="-1">
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
                      <select className="form-control form-control-sm" ref="cascades" multiple="multiple">
                        {(this.state.cascadesEntity || []).map((item) => { return <option key={'option-' + item[0]} value={item[0]}>{item[1]}</option> })}
                      </select>
                    </div>
                  </div>
                }
                <div className="mt-4 mb-3" ref="btns">
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
          this.__select2 = $(this.refs['cascades']).select2({
            placeholder: '选择关联实体 (可选)'
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

    let btns = $(this.refs['btns']).find('.btn').button('loading')
    $.post(rb.baseUrl + '/app/entity/record-delete?id=' + ids + '&cascades=' + cascades, (res) => {
      if (res.error_code === 0) {
        if (res.data.deleted === res.data.requests) rb.hbsuccess('删除成功')
        else rb.hbsuccess('已成功删除 ' + res.data.deleted + ' 条记录')

        this.hide()
        typeof this.props.deleteAfter === 'function' && this.props.deleteAfter()
      } else {
        rb.hberror(res.error_msg)
        btns.button('reset')
      }
    })
  }
}

// 分类数据选择
class ClassificationSelector extends React.Component {
  constructor(props) {
    super(props)
    this._select = []
    this._select2 = []
    this.state = { openLevel: 1, datas: [] }
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
              <h5 className="mt-0">选择</h5>
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
                <button className="btn btn-primary" onClick={() => this.confirm()}>确定</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }
  componentDidMount() {
    this.show()

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
    $.get(`${rb.baseUrl}/app/entity/classification?entity=${this.props.entity}&field=${this.props.field}&parent=${p || ''}`, (res) => {
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
      rb.highbar('选择有误')
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
    $(this._dlg).modal({ show: true, keyboard: true })
  }
  hide() {
    $(this._dlg).modal('hide')
  }
}

// -- Usage

let rb = rb || {}

rb.__currentRbFormModal
// @props = { id, entity, title, icon }
rb.RbFormModal = function (props) {
  if (rb.__currentRbFormModal) rb.__currentRbFormModal.show(props)
  else rb.__currentRbFormModal = renderRbcomp(<RbFormModal {...props} />)
  return rb.__currentRbFormModal
}

rb.__currentRbViewModal
rb.__currentRbFormModalCache = {}
// @props = { id, entity }
rb.RbViewModal = function (props, subView) {
  let viewUrl = `${rb.baseUrl}/app/${props.entity}/view/${props.id}`
  if (subView === true) {
    rb.RbViewModalHide(props.id)
    let m = renderRbcomp(<RbViewModal url={viewUrl} disposeOnHide={true} id={props.id} subView={true} />)
    rb.__currentRbFormModalCache[props.id] = m
    return m
  }

  if (rb.__currentRbViewModal) rb.__currentRbViewModal.show(viewUrl)
  else rb.__currentRbViewModal = renderRbcomp(<RbViewModal url={viewUrl} />)
  rb.__currentRbFormModalCache[props.id] = rb.__currentRbViewModal
  return rb.__currentRbViewModal
}
rb.RbViewModalGet = function (id) {
  return rb.__currentRbFormModalCache[id]
}

rb.RbViewModalHide = function (id) {
  if (!id) {
    if (rb.__currentRbViewModal) rb.__currentRbViewModal.hide()
  } else {
    let cm = rb.__currentRbFormModalCache[id]
    if (cm) {
      cm.hide()
      rb.__currentRbFormModalCache[id] = null
    }
  }
}
rb.RbViewModalHideLoading = function (id) {
  if (!id) {
    if (rb.__currentRbViewModal) rb.__currentRbViewModal.hideLoading()
  } else {
    let m = rb.__currentRbFormModalCache[id]
    if (m) m.hideLoading()
  }
}