/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// ~~ 表单窗口
class RbFormModal extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, inLoad: true }
    if (!props.id) this.state.id = null
  }

  render() {
    return (
      this.state.isDestroy !== true && (
        <div className="modal-wrapper">
          <div className="modal rbmodal colored-header colored-header-primary" ref={(c) => (this._rbmodal = c)}>
            <div className="modal-dialog">
              <div className="modal-content">
                <div className="modal-header modal-header-colored">
                  {this.state.icon && <span className={'icon zmdi zmdi-' + this.state.icon} />}
                  <h3 className="modal-title">{this.state.title || $L('New')}</h3>
                  {rb.isAdminUser && (
                    <a className="close s" href={rb.baseUrl + '/admin/entity/' + this.state.entity + '/form-design'} title={$L('ConfSome,FormLayout')} target="_blank">
                      <span className="zmdi zmdi-settings"></span>
                    </a>
                  )}
                  <button className="close md-close" type="button" onClick={() => this.hide()}>
                    <span className="zmdi zmdi-close"></span>
                  </button>
                </div>
                <div className={'modal-body rb-loading' + (this.state.inLoad ? ' rb-loading-active' : '')}>
                  {this.state.alertMessage && <div className="alert alert-warning rbform-alert">{this.state.alertMessage}</div>}
                  {this.state.formComponent}
                  {this.state.inLoad && <RbSpinner />}
                </div>
              </div>
            </div>
          </div>
        </div>
      )
    )
  }

  componentDidMount() {
    const $root = $(this._rbmodal)
      .modal({
        show: false,
        backdrop: 'static',
        keyboard: false,
      })
      .on('hidden.bs.modal', () => {
        $keepModalOpen()
        if (this.props.disposeOnHide === true) {
          $root.modal('dispose')
          $unmount($root.parent().parent())
        }
      })
    this.showAfter({}, true)
  }

  // 渲染表单
  getFormModel() {
    const entity = this.state.entity
    const id = this.state.id || ''
    const initialValue = this.state.initialValue || {} // 默认值填充（仅新建有效）

    const that = this
    $.post(`/app/${entity}/form-model?id=${id}`, JSON.stringify(initialValue), function (res) {
      // 包含错误
      if (res.error_code > 0 || !!res.data.error) {
        const error = (res.data || {}).error || res.error_msg
        that.renderFromError(error)
        return
      }

      const FORM = (
        <RbForm entity={entity} id={id} $$$parent={that}>
          {res.data.elements.map((item) => {
            return detectElement(item)
          })}
        </RbForm>
      )
      that.setState({ formComponent: FORM, __formModel: res.data }, () => that.setState({ inLoad: false }))
      that.__lastModified = res.data.lastModified || 0
    })
  }

  renderFromError(message) {
    const error = (
      <div className="alert alert-danger alert-icon mt-5 w-75 mlr-auto">
        <div className="icon">
          <i className="zmdi zmdi-alert-triangle"></i>
        </div>
        <div className="message" dangerouslySetInnerHTML={{ __html: `<strong>${$L('Opps')}</strong> ` + message }}></div>
      </div>
    )
    this.setState({ formComponent: error }, () => this.setState({ inLoad: false }))
  }

  show(state) {
    state = state || {}
    if (!state.id) state.id = null

    // 比较初始参数决定是否可复用
    const stateNew = [state.id, state.entity, state.initialValue]
    const stateOld = [this.state.id, this.state.entity, this.state.initialValue]

    if (this.state.isDestroy === true || JSON.stringify(stateNew) !== JSON.stringify(stateOld)) {
      state = { formComponent: null, initialValue: null, inLoad: true, ...state }
      this.setState(state, () => this.showAfter({ isDestroy: false }, true))
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
    $.get(`/app/entity/record-lastModified?id=${this.state.id}`, (res) => {
      if (res.error_code === 0) {
        if (res.data.lastModified !== this.__lastModified) {
          // this.setState({ alertMessage: <p>记录已由其他用户编辑过，<a onClick={() => this.__refresh()}>点击此处</a>查看最新数据</p> })
          this._refresh()
        }
      } else if (res.error_msg === 'NO_EXISTS') {
        this.setState({ alertMessage: $L('RecordNotExistsTips') })
      }
    })
  }

  _refresh() {
    const hold = { id: this.state.id, entity: this.state.entity }
    this.setState({ id: null, alertMessage: null }, () => {
      this.show(hold)
    })
  }

  hide(destroy) {
    $(this._rbmodal).modal('hide')
    const state = { isDestroy: destroy === true }
    if (destroy === true) state.id = null
    this.setState(state)
  }

  // -- Usage
  /**
   * @param {*} props
   */
  static create(props, newDlg) {
    if (newDlg === true) {
      renderRbcomp(<RbFormModal {...props} />)
      return
    }

    if (this.__HOLDER) {
      this.__HOLDER.show(props)
    } else {
      const that = this
      renderRbcomp(<RbFormModal {...props} />, null, function () {
        that.__HOLDER = this
      })
    }
  }
}

// ~~ 表单
class RbForm extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }

    this.__FormData = {}
    const iv = props.$$$parent.state.__formModel.initialValue
    if (iv) {
      for (let k in iv) {
        const val = iv[k]
        this.__FormData[k] = { value: typeof val === 'object' ? val.id : val, error: null }
      }
    }

    this.isNew = !props.$$$parent.state.id
    this.setFieldValue = this.setFieldValue.bind(this)
  }

  render() {
    return (
      <div className="rbform">
        <div className="form" ref={(c) => (this._form = c)}>
          {this.props.children.map((fieldComp) => {
            const refid = `fieldcomp-${fieldComp.props.field}`
            return React.cloneElement(fieldComp, { $$$parent: this, ref: refid })
          })}
          {this.renderFormAction()}
        </div>
      </div>
    )
  }

  renderFormAction() {
    const pmodel = this.props.$$$parent.state.__formModel
    const moreActions = []
    if (pmodel.hadApproval)
      moreActions.push(
        <a key="Action103" className="dropdown-item" onClick={() => this.post(103)}>
          {$L('SaveAndSubmit')}
        </a>
      )
    if (pmodel.isMain === true)
      moreActions.push(
        <a key="Action102" className="dropdown-item" onClick={() => this.post(102)}>
          {$L('SaveAndAddDetail')}
        </a>
      )
    else if (pmodel.isDetail === true)
      moreActions.push(
        <a key="Action101" className="dropdown-item" onClick={() => this.post(101)}>
          {$L('SaveAndAdd')}
        </a>
      )

    let actionBtn = (
      <button className="btn btn-primary btn-space" type="button" onClick={() => this.post()}>
        {$L('Save')}
      </button>
    )
    if (moreActions.length > 0) {
      actionBtn = (
        <div className="btn-group dropup btn-space">
          <button className="btn btn-primary" type="button" onClick={() => this.post()}>
            {$L('Save')}
          </button>
          <button className="btn btn-primary dropdown-toggle auto" type="button" data-toggle="dropdown">
            <span className="icon zmdi zmdi-chevron-up"></span>
          </button>
          <div className="dropdown-menu dropdown-menu-primary dropdown-menu-right">
            {moreActions.map((item) => {
              return item
            })}
          </div>
        </div>
      )
    }

    return (
      <div className="form-group row footer">
        <div className="col-12 col-sm-8 offset-sm-3" ref={(c) => (this._formAction = c)}>
          {actionBtn}
          <button className="btn btn-secondary btn-space" type="button" onClick={() => this.props.$$$parent.hide()}>
            {$L('Cancel')}
          </button>
        </div>
      </div>
    )
  }

  componentDidMount() {
    if (this.isNew) {
      this.props.children.map((child) => {
        const val = child.props.value
        if (val && child.props.readonly !== true) {
          // 复合型值 {id:xxx, text:xxx}
          this.setFieldValue(child.props.field, typeof val === 'object' ? val.id : val)
        }
      })
    }
  }

  // 表单回填
  setAutoFillin(data) {
    if (!data || data.length === 0) return
    const that = this
    data.forEach((item) => {
      // eslint-disable-next-line react/no-string-refs
      const fieldComp = that.refs['fieldcomp-' + item.target]
      if (fieldComp) {
        if (!item.fillinForce && fieldComp.getValue()) return
        if ((that.isNew && item.whenCreate) || (!that.isNew && item.whenUpdate)) fieldComp.setValue(item.value)
      }
    })
  }

  // 设置字段值
  setFieldValue(field, value, error) {
    this.__FormData[field] = { value: value, error: error }
    // eslint-disable-next-line no-console
    if (rb.env === 'dev') console.log('FV1 ... ' + JSON.stringify(this.__FormData))
  }

  // 避免无意义更新
  setFieldUnchanged(field) {
    delete this.__FormData[field]
    // eslint-disable-next-line no-console
    if (rb.env === 'dev') console.log('FV2 ... ' + JSON.stringify(this.__FormData))
  }

  // 保存并继续添加
  static __NEXT_ADD = 101
  // 保存并添加明细
  static __NEXT_ADDDETAIL = 102
  // 保存并提交审批
  static __NEXT_APPROVAL = 103
  /**
   * @next {Number}
   */
  post = (next) => setTimeout(() => this._post(next), 30)

  _post(next) {
    const data = {}
    for (let k in this.__FormData) {
      const err = this.__FormData[k].error
      if (err) return RbHighbar.create(err)
      else data[k] = this.__FormData[k].value
    }
    data.metadata = {
      entity: this.state.entity,
      id: this.state.id,
    }
    if (RbForm.postBefore(data) === false) return

    const $btns = $(this._formAction).find('.btn').button('loading')
    $.post('/app/entity/record-save', JSON.stringify(data), (res) => {
      $btns.button('reset')
      if (res.error_code === 0) {
        RbHighbar.success($L('SomeSuccess,Save'))
        setTimeout(() => {
          this.props.$$$parent.hide(true)
          RbForm.postAfter(res.data, next)

          if (next === RbForm.__NEXT_ADD) {
            const pstate = this.props.$$$parent.state
            RbFormModal.create({
              title: pstate.title,
              entity: pstate.entity,
              icon: pstate.icon,
              initialValue: pstate.initialValue,
            })
          } else if (next === RbForm.__NEXT_ADDDETAIL) {
            const iv = { $MAINID$: res.data.id }
            const sm = this.props.$$$parent.state.__formModel.detailMeta
            RbFormModal.create({
              title: $L('AddSome').replace('{0}', sm.entityLabel),
              entity: sm.entity,
              icon: sm.icon,
              initialValue: iv,
            })
          } else if (next === RbForm.__NEXT_APPROVAL) {
            renderRbcomp(<ApprovalSubmitForm id={res.data.id} disposeOnHide={true} />)
          }
        }, 100)
      } else if (res.error_code === 499) {
        renderRbcomp(<RepeatedViewer entity={this.state.entity} data={res.data} />)
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
    return true
  }

  // 保存前调用，返回 false 则不继续保存
  // eslint-disable-next-line no-unused-vars
  static postBefore(data) {
    return true
  }

  // 保存后调用
  static postAfter(data, next) {
    const rlp = window.RbListPage || parent.RbListPage
    if (rlp) rlp.reload()
    if (window.RbViewPage && (next || 0) < 101) window.RbViewPage.reload()
  }
}

// 表单元素基础类
class RbFormElement extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }

    this.handleChange = this.handleChange.bind(this)
    this.handleClear = this.handleClear.bind(this)
    this.checkValue = this.checkValue.bind(this)
  }

  render() {
    const props = this.props

    let colWidths = [3, 8]
    if (props.onView) {
      colWidths[0] = 4
      if (props.isFull === true) colWidths = [2, 10]
    }

    const editable = props.$$$parent.onViewEditable && props.onView && !props.readonly

    return (
      <div className={`form-group row type-${props.type} ${editable ? 'editable' : ''}`} data-field={props.field}>
        <label ref={(c) => (this._fieldLabel = c)} className={`col-12 col-sm-${colWidths[0]} col-form-label text-sm-right ${!props.onView && !props.nullable ? 'required' : ''}`}>
          {props.label}
        </label>
        <div ref={(c) => (this._fieldText = c)} className={'col-12 col-sm-' + colWidths[1]}>
          {!props.onView || (editable && this.state.editMode) ? this.renderElement() : this.renderViewElement()}
          {!props.onView && props.tip && <p className="form-text">{props.tip}</p>}
          {editable && !this.state.editMode && <a className="edit" title={$L('Edit')} onClick={() => this.toggleEditMode(true)} />}
          {editable && this.state.editMode && (
            <div className="edit-oper">
              <div className="btn-group shadow-sm">
                <button type="button" className="btn btn-secondary" onClick={this.handleEditConfirm}>
                  <i className="icon zmdi zmdi-check"></i>
                </button>
                <button type="button" className="btn btn-secondary" onClick={() => this.toggleEditMode(false)}>
                  <i className="icon zmdi zmdi-close"></i>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    )
  }

  // 渲染表单
  renderElement() {
    const value = arguments.length > 0 ? arguments[0] : this.state.value
    return (
      <input
        ref={(c) => (this._fieldValue = c)}
        className={`form-control form-control-sm ${this.state.hasError ? 'is-invalid' : ''}`}
        title={this.state.hasError}
        type="text"
        value={value || ''}
        onChange={this.handleChange}
        onBlur={this.props.readonly ? null : this.checkValue}
        readOnly={this.props.readonly}
        maxLength="255"
      />
    )
  }

  // 渲染视图
  renderViewElement() {
    let text = arguments.length > 0 ? arguments[0] : this.state.value
    if (text && $empty(text)) text = null
    return (
      <React.Fragment>
        <div className="form-control-plaintext">{text || <span className="text-muted">无</span>}</div>
      </React.Fragment>
    )
  }

  componentDidMount() {
    const props = this.props
    if (!props.onView) {
      // 必填字段
      if (!props.nullable && !props.readonly && $empty(props.value)) props.$$$parent.setFieldValue(props.field, null, $L('SomeNotEmpty').replace('{0}', props.label))
      props.tip && $(this._fieldLabel).find('i.zmdi').tooltip({ placement: 'right' })
    }
    if (!props.onView && !this.props.readonly) this.onEditModeChanged()
  }

  componentWillUnmount() {
    this.onEditModeChanged(true)
  }

  onEditModeChanged(destroy) {
    if (destroy) {
      if (this.__select2) {
        if ($.type(this.__select2) === 'array')
          $(this.__select2).each(function () {
            this.select2('destroy')
          })
        else this.__select2.select2('destroy')
        this.__select2 = null
      }
    }
  }

  // 修改值（表单组件（字段）值变化应调用此方法）
  handleChange(e, checkValue) {
    const val = e.target.value
    this.setState({ value: val }, () => checkValue === true && this.checkValue())
  }

  // 清空值
  handleClear() {
    this.setState({ value: '' }, () => this.checkValue())
  }

  // 编辑单个字段值
  toggleEditMode(mode) {
    this.setState({ editMode: mode }, () => {
      if (this.state.editMode) {
        this.onEditModeChanged()
        this._fieldValue && this._fieldValue.focus()
      } else {
        const newValue = arguments.length > 1 ? arguments[1] : this.state.newValue === undefined ? this.props.value : this.state.newValue
        this.setState({ value: newValue, newValue: newValue || null }, () => this.onEditModeChanged(true))
      }
    })
  }

  handleEditConfirm = () => {
    this.props.$$$parent.saveSingleFieldValue && this.props.$$$parent.saveSingleFieldValue(this)
  }

  // 检查值
  checkValue() {
    const err = this.isValueError()
    const errMsg = err ? this.props.label + err : null

    if (this.isValueUnchanged() && !this.props.$$$parent.isNew) {
      if (err) this.props.$$$parent.setFieldValue(this.props.field, this.state.value, errMsg)
      else this.props.$$$parent.setFieldUnchanged(this.props.field)
    } else {
      this.setState({ hasError: err })
      this.props.$$$parent.setFieldValue(this.props.field, this.state.value, errMsg)
    }
  }

  // 无效值检查
  isValueError() {
    if (this.props.nullable === false) {
      const v = this.state.value
      if (v && $.type(v) === 'array') return v.length === 0 ? $L('SomeNotEmpty').replace('{0}', '') : null
      else return !v ? $L('SomeNotEmpty').replace('{0}', '') : null
    }
  }

  // 未修改
  isValueUnchanged() {
    const oldv = this.state.newValue === undefined ? this.props.value : this.state.newValue
    return $same(oldv, this.state.value)
  }

  // Getter / Setter
  setValue(val) {
    this.handleChange({ target: { value: val } }, true)
  }

  getValue() {
    return this.state.value
  }
}

class RbFormText extends RbFormElement {
  constructor(props) {
    super(props)
  }
}

class RbFormUrl extends RbFormText {
  constructor(props) {
    super(props)
  }

  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()
    const clickUrl = rb.baseUrl + '/commons/url-safe?url=' + encodeURIComponent(this.state.value)
    return (
      <div className="form-control-plaintext">
        <a href={clickUrl} className="link" target="_blank" rel="noopener noreferrer">
          {this.state.value}
        </a>
      </div>
    )
  }

  isValueError() {
    const err = super.isValueError()
    if (err) return err
    return !!this.state.value && $regex.isUrl(this.state.value) === false ? $L('SomeNotFormatWell').replace('{0}', '') : null
  }
}

class RbFormEMail extends RbFormText {
  constructor(props) {
    super(props)
  }

  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()
    return (
      <div className="form-control-plaintext">
        <a title={$L('SendEmail')} href={'mailto:' + this.state.value} className="link">
          {this.state.value}
        </a>
      </div>
    )
  }

  isValueError() {
    const err = super.isValueError()
    if (err) return err
    return !!this.state.value && $regex.isMail(this.state.value) === false ? $L('SomeNotFormatWell').replace('{0}', '') : null
  }
}

class RbFormPhone extends RbFormText {
  constructor(props) {
    super(props)
  }

  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()
    return (
      <div className="form-control-plaintext">
        <a title="拨打电话" href={'tel:' + this.state.value} className="link">
          {this.state.value}
        </a>
      </div>
    )
  }

  isValueError() {
    const err = super.isValueError()
    if (err) return err
    return !!this.state.value && $regex.isTel(this.state.value) === false ? $L('SomeNotFormatWell').replace('{0}', '') : null
  }
}

class RbFormNumber extends RbFormText {
  constructor(props) {
    super(props)
    if (this.state.value) this.state.value = (this.state.value + '').replace(/,/g, '')
  }

  isValueError() {
    const err = super.isValueError()
    if (err) return err
    if (!!this.state.value && $regex.isNumber(this.state.value) === false) return $L('SomeNotFormatWell').replace('{0}', '')
    if (!!this.state.value && this.props.notNegative === 'true' && parseFloat(this.state.value) < 0) return $L('SomeNotNegative').replace('{0}', '')
    return null
  }

  _isValueError() {
    return super.isValueError()
  }

  renderElement() {
    let value = arguments.length > 0 ? arguments[0] : this.state.value
    if (value) value = (value + '').replace(/,/g, '') // 移除千分为位
    return (
      <input
        ref={(c) => (this._fieldValue = c)}
        className={`form-control form-control-sm ${this.state.hasError ? 'is-invalid' : ''}`}
        title={this.state.hasError}
        type="text"
        value={value || ''}
        onChange={this.handleChange}
        onBlur={this.props.readonly ? null : this.checkValue}
        readOnly={this.props.readonly}
        maxLength="30"
      />
    )
  }
}

class RbFormDecimal extends RbFormNumber {
  constructor(props) {
    super(props)
    if (this.state.value) this.state.value = (this.state.value + '').replace(/,/g, '')
  }

  isValueError() {
    const err = super._isValueError()
    if (err) return err
    if (!!this.state.value && $regex.isDecimal(this.state.value) === false) return $L('SomeNotFormatWell').replace('{0}', '')
    if (!!this.state.value && this.props.notNegative === 'true' && parseFloat(this.state.value) < 0) return $L('SomeNotNegative').replace('{0}', '')
    return null
  }
}

class RbFormTextarea extends RbFormElement {
  constructor(props) {
    super(props)
  }

  renderElement() {
    return (
      <textarea
        ref={(c) => (this._fieldValue = c)}
        className={`form-control form-control-sm row3x ${this.state.hasError ? 'is-invalid' : ''}`}
        title={this.state.hasError}
        value={this.state.value || ''}
        onChange={this.handleChange}
        onBlur={this.props.readonly ? null : this.checkValue}
        readOnly={this.props.readonly}
        maxLength="3000"
      />
    )
  }

  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()
    return (
      <div className="form-control-plaintext" ref={(c) => (this._textarea = c)}>
        {this.state.value.split('\n').map((line, idx) => {
          return <p key={'kl-' + idx}>{line}</p>
        })}
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount()
    this.unmountFieldComp()
  }

  unmountFieldComp() {
    if (this._textarea) $(this._textarea).perfectScrollbar()
  }

  mountFieldComp() {
    if (this._textarea) $(this._textarea).perfectScrollbar('destroy')
  }
}

class RbFormDateTime extends RbFormElement {
  constructor(props) {
    super(props)
  }

  renderElement() {
    if (this.props.readonly) return super.renderElement()
    return (
      <div className="input-group datetime-field">
        <input
          ref={(c) => (this._fieldValue = c)}
          className={'form-control form-control-sm ' + (this.state.hasError ? 'is-invalid' : '')}
          title={this.state.hasError}
          type="text"
          value={this.state.value || ''}
          onChange={this.handleChange}
          onBlur={this.checkValue}
        />
        <span className={'zmdi zmdi-close clean ' + (this.state.value ? '' : 'hide')} onClick={this.handleClear}></span>
        <div className="input-group-append">
          <button className="btn btn-secondary" type="button" ref={(c) => (this._fieldValue__icon = c)}>
            <i className="icon zmdi zmdi-calendar"></i>
          </button>
        </div>
      </div>
    )
  }

  onEditModeChanged(destroy) {
    if (destroy) {
      if (this.__datetimepicker) {
        this.__datetimepicker.datetimepicker('remove')
        this.__datetimepicker = null
      }
    } else {
      const format = (this.props.datetimeFormat || this.props.dateFormat).replace('mm', 'ii').toLowerCase()
      let minView = 0
      let startView = 'month'
      if (format.length === 4) minView = startView = 'decade'
      else if (format.length === 7) minView = startView = 'year'
      else if (format.length === 10) minView = 'month'

      const that = this
      this.__datetimepicker = $(this._fieldValue)
        .datetimepicker({
          format: format || 'yyyy-mm-dd hh:ii:ss',
          minView: minView,
          startView: startView,
        })
        .on('changeDate', function () {
          const val = $(this).val()
          that.handleChange({ target: { value: val } }, true)
        })
      $(this._fieldValue__icon).click(() => this.__datetimepicker.datetimepicker('show'))
    }
  }
}

class RbFormImage extends RbFormElement {
  constructor(props) {
    super(props)

    if (props.value) this.state.value = [...props.value] // clone
    if (this.props.uploadNumber) {
      this.__minUpload = ~~(this.props.uploadNumber.split(',')[0] || 0)
      this.__maxUpload = ~~(this.props.uploadNumber.split(',')[1] || 9)
    } else {
      this.__minUpload = 0
      this.__maxUpload = 9
    }
  }

  renderElement() {
    const value = this.state.value || []
    const showUpload = value.length < this.__maxUpload && !this.props.readonly

    return (
      <div className="img-field">
        {value.map((item) => {
          return (
            <span key={'img-' + item}>
              <a title={$fileCutName(item)} className="img-thumbnail img-upload">
                <img src={`${rb.baseUrl}/filex/img/${item}?imageView2/2/w/100/interlace/1/q/100`} />
                {!this.props.readonly && (
                  <b title={$L('Remove')} onClick={() => this.removeItem(item)}>
                    <span className="zmdi zmdi-close"></span>
                  </b>
                )}
              </a>
            </span>
          )
        })}
        {showUpload && (
          <span title={$L('UploadImgNeedX').replace('%d', `${this.__minUpload}~${this.__maxUpload}`)}>
            <input ref={(c) => (this._fieldValue__input = c)} type="file" className="inputfile" id={`${this.props.field}-input`} accept="image/*" data-maxsize="10240000" />
            <label htmlFor={`${this.props.field}-input`} className="img-thumbnail img-upload">
              <span className="zmdi zmdi-image-alt"></span>
            </label>
          </span>
        )}
        <input ref={(c) => (this._fieldValue = c)} type="hidden" value={value} />
      </div>
    )
  }

  renderViewElement() {
    const value = this.state.value
    if ($empty(value)) return super.renderViewElement()

    return (
      <div className="img-field">
        {value.map((item, idx) => {
          return (
            <span key={'img-' + item}>
              <a title={$fileCutName(item)} onClick={() => (parent || window).RbPreview.create(value, idx)} className="img-thumbnail img-upload zoom-in">
                <img src={`${rb.baseUrl}/filex/img/${item}?imageView2/2/w/100/interlace/1/q/100`} />
              </a>
            </span>
          )
        })}
      </div>
    )
  }

  onEditModeChanged(destroy) {
    if (destroy) {
      // NOOP
    } else {
      let mp
      const mp_end = function () {
        if (mp) mp.end()
        mp = null
      }
      $createUploader(
        this._fieldValue__input,
        (res) => {
          if (!mp) mp = new Mprogress({ template: 1, start: true })
          mp.set(res.percent / 100) // 0.x
        },
        (res) => {
          mp_end()
          const paths = this.state.value || []
          paths.push(res.key)
          this.handleChange({ target: { value: paths } }, true)
        },
        () => mp_end()
      )
    }
  }

  removeItem(item) {
    const paths = this.state.value || []
    paths.remove(item)
    this.handleChange({ target: { value: paths } }, true)
  }

  isValueError() {
    const err = super.isValueError()
    if (err) return err
    const ups = (this.state.value || []).length
    if (this.__minUpload > 0 && ups < this.__minUpload) return $L('UploadMinXTips').replace('%d', this.__minUpload)
    if (this.__maxUpload < ups) return $L('UploadMaxXTips').replace('%d', this.__maxUpload)
  }
}

class RbFormFile extends RbFormImage {
  constructor(props) {
    super(props)
  }

  renderElement() {
    const value = this.state.value || []
    const showUpload = value.length < this.__maxUpload && !this.props.readonly

    return (
      <div className="file-field">
        {value.map((item) => {
          let fileName = $fileCutName(item)
          return (
            <div key={'file-' + item} className="img-thumbnail" title={fileName}>
              <i className="file-icon" data-type={$fileExtName(fileName)} />
              <span>{fileName}</span>
              {!this.props.readonly && (
                <b title={$L('Remove')} onClick={() => this.removeItem(item)}>
                  <span className="zmdi zmdi-close"></span>
                </b>
              )}
            </div>
          )
        })}
        {showUpload && (
          <div className="file-select">
            <input type="file" className="inputfile" ref={(c) => (this._fieldValue__input = c)} id={`${this.props.field}-input`} data-maxsize="102400000" />
            <label htmlFor={`${this.props.field}-input`} title={$L('UploadFileNeedX').replace('%d', `${this.__minUpload}~${this.__maxUpload}`)} className="btn-secondary">
              <i className="zmdi zmdi-upload"></i>
              <span>{$L('UploadFile')}</span>
            </label>
          </div>
        )}
        <input ref={(c) => (this._fieldValue = c)} type="hidden" value={value} />
      </div>
    )
  }

  renderViewElement() {
    const value = this.state.value
    if ($empty(value)) return super.renderViewElement()

    return (
      <div className="file-field">
        {value.map((item) => {
          let fileName = $fileCutName(item)
          return (
            <a key={'file-' + item} title={fileName} onClick={() => (parent || window).RbPreview.create(item)} className="img-thumbnail">
              <i className="file-icon" data-type={$fileExtName(fileName)} />
              <span>{fileName}</span>
            </a>
          )
        })}
      </div>
    )
  }
}

class RbFormPickList extends RbFormElement {
  constructor(props) {
    super(props)

    const options = props.options
    if (options && props.value) {
      // Check value has been deleted
      let deleted = true
      $(options).each(function () {
        // eslint-disable-next-line eqeqeq
        if (this.id == props.value) {
          deleted = false
          return false
        }
      })

      if (deleted) {
        options.push({ id: props.value, text: '[DELETED]' })
        this.state.options = options
      }
    }
  }

  renderElement() {
    if (this.props.readonly) return super.renderElement(__findOptionText(this.state.options, this.props.value))

    const name = `${this.state.field}-opt-`
    return (
      <select ref={(c) => (this._fieldValue = c)} className="form-control form-control-sm" value={this.state.value || ''} onChange={this.handleChange}>
        <option value=""></option>
        {this.state.options.map((item) => {
          return (
            <option key={`${name}${item.id}`} value={item.id}>
              {item.text}
            </option>
          )
        })}
      </select>
    )
  }

  renderViewElement() {
    return super.renderViewElement(__findOptionText(this.state.options, this.state.value))
  }

  onEditModeChanged(destroy) {
    if (destroy) {
      super.onEditModeChanged(destroy)
    } else {
      this.__select2 = $(this._fieldValue).select2({
        placeholder: $L('SelectSome').replace('{0}', '') + this.props.label,
      })

      const that = this
      this.__select2
        .on('change', function (e) {
          const val = e.target.value
          that.handleChange({ target: { value: val } }, true)
        })
        .trigger('change')
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

class RbFormReference extends RbFormElement {
  constructor(props) {
    super(props)
  }

  renderElement() {
    if (this.props.readonly) return super.renderElement(this.props.value ? this.props.value.text : null)

    const hasDataFilter = this.props.referenceDataFilter && (this.props.referenceDataFilter.items || []).length > 0
    return (
      <div className="input-group datetime-field">
        <select ref={(c) => (this._fieldValue = c)} className="form-control form-control-sm" title={hasDataFilter ? $L('FieldUseDataFilter') : null} />
        <div className="input-group-append">
          <button className="btn btn-secondary" type="button" onClick={this.showSearcher}>
            <i className="icon zmdi zmdi-search" />
          </button>
        </div>
      </div>
    )
  }

  renderViewElement() {
    const value = this.state.value
    if ($empty(value)) return super.renderViewElement()

    if (typeof value === 'string') return <div className="form-control-plaintext">{value}</div>
    if (!value.id) return <div className="form-control-plaintext">{value.text}</div>
    return (
      <div className="form-control-plaintext">
        <a href={`#!/View/${value.entity}/${value.id}`} onClick={this._clickView}>
          {value.text}
        </a>
      </div>
    )
  }

  _clickView = (e) => window.RbViewPage && window.RbViewPage.clickView(e.target)

  componentDidMount() {
    super.componentDidMount()

    // 新建记录时触发回填
    const props = this.props
    if (props.$$$parent.isNew && !props.onView && props.value && props.value.id) {
      setTimeout(() => this.triggerAutoFillin(props.value.id), 500)
    }
  }

  onEditModeChanged(destroy) {
    if (destroy) {
      super.onEditModeChanged(destroy)
    } else {
      const entity = this.props.$$$parent.props.entity
      const field = this.props.field
      this.__select2 = $initReferenceSelect2(this._fieldValue, {
        name: field,
        label: this.props.label,
        entity: entity,
      })

      const val = this.state.value
      if (val) {
        const o = new Option(val.text, val.id, true, true)
        this.__select2.append(o).trigger('change')
      }

      const that = this
      this.__select2.on('change', function (e) {
        const v = e.target.value
        if (v) {
          $.post(`/commons/search/recently-add?id=${v}`)
          that.triggerAutoFillin(v)
        }
        that.handleChange({ target: { value: v } }, true)
      })
    }
  }

  // 字段回填
  triggerAutoFillin(value) {
    if (this.props.onView) return

    const $$$parent = this.props.$$$parent
    $.post(`/app/entity/extras/fillin-value?entity=${$$$parent.props.entity}&field=${this.props.field}&source=${value}`, (res) => {
      res.error_code === 0 && res.data.length > 0 && $$$parent.setAutoFillin(res.data)
    })
  }

  isValueUnchanged() {
    const oldv = this.state.newValue === undefined ? (this.props.value || {}).id : (this.state.newValue || {}).id
    return $same(oldv, this.state.value)
  }

  setValue(val) {
    if (val) {
      const o = new Option(val.text, val.id, true, true)
      this.__select2.append(o)
      this.handleChange({ target: { value: val.id } }, true)
    } else this.__select2.val(null).trigger('change')
  }

  showSearcher = () => {
    const that = this
    referenceSearch__call = function (selected) {
      selected = selected[0]
      if ($(that._fieldValue).find(`option[value="${selected}"]`).length > 0) {
        that.__select2.val(selected).trigger('change')
      } else {
        $.get(`/commons/search/read-labels?ids=${selected}`, (res) => {
          const o = new Option(res.data[selected], selected, true, true)
          that.__select2.append(o).trigger('change')
        })
      }
      that.__searcher.hide()
    }

    if (this.__searcher) this.__searcher.show()
    else {
      const searchUrl = `${rb.baseUrl}/commons/search/reference-search?field=${this.props.field}.${this.props.$$$parent.props.entity}`
      renderRbcomp(<ReferenceSearcher url={searchUrl} title={$L('SelectSome').replace('{0}', this.props.label)} />, function () {
        that.__searcher = this
      })
    }
  }
}

class RbFormClassification extends RbFormElement {
  constructor(props) {
    super(props)
  }

  renderElement() {
    if (this.props.readonly) return super.renderElement(this.props.value ? this.props.value.text : null)
    return (
      <div className="input-group datetime-field">
        <select ref={(c) => (this._fieldValue = c)} className="form-control form-control-sm" />
        <div className="input-group-append">
          <button className="btn btn-secondary" type="button" onClick={this.showSelector}>
            <i className="icon zmdi zmdi-search" />
          </button>
        </div>
      </div>
    )
  }

  renderViewElement() {
    return super.renderViewElement(this.state.value ? this.state.value.text : null)
  }

  onEditModeChanged(destroy) {
    if (destroy) {
      super.onEditModeChanged(destroy)
      this.__cached = null
      if (this.__selector) {
        this.__selector.hide(true)
        this.__selector = null
      }
    } else {
      this.__select2 = $initReferenceSelect2(this._fieldValue, {
        name: this.props.field,
        label: this.props.label,
        entity: this.props.$$$parent.props.entity,
        searchType: 'classification',
      })

      const value = this.state.value
      value && this.setClassificationValue(value)

      this.__select2.on('change', () => {
        const v = this.__select2.val()
        if (v) $.post(`/commons/search/recently-add?id=${v}&type=d${this.props.classification}`)
        this.handleChange({ target: { value: v } }, true)
      })
    }
  }

  isValueUnchanged() {
    const oldv = this.state.newValue === undefined ? (this.props.value || {}).id : (this.state.newValue || {}).id
    return $same(oldv, this.state.value)
  }

  setValue(val) {
    if (val && val.id) this.setClassificationValue(val)
    else this.__select2.val(null).trigger('change')
  }

  showSelector = () => {
    if (this.__selector) this.__selector.show()
    else {
      const p = this.props
      const that = this
      renderRbcomp(<ClassificationSelector entity={p.$$$parent.state.entity} field={p.field} label={p.label} openLevel={p.openLevel} $$$parent={this} />, null, function () {
        that.__selector = this
      })
    }
  }

  setClassificationValue(s) {
    if (!s.id) return

    const data = this.__cached || {}
    if (data[s.id]) {
      this.__select2.val(s.id).trigger('change')
    } else if (this._fieldValue) {
      const o = new Option(s.text, s.id, true, true)
      $(this._fieldValue).append(o).trigger('change')
      data[s.id] = s.text
      this.__cached = data
    }
  }
}

class RbFormMultiSelect extends RbFormElement {
  constructor(props) {
    super(props)
  }

  renderElement() {
    const name = `checkbox-${this.props.field}`
    return (
      <div className="mt-1" ref={(c) => (this._fieldValue__wrap = c)}>
        {(this.props.options || []).length === 0 && <div className="text-danger">{$L('NoData')}</div>}
        {(this.props.options || []).map((item) => {
          return (
            <label key={name + item.mask} className="custom-control custom-checkbox custom-control-inline">
              <input
                className="custom-control-input"
                name={name}
                type="checkbox"
                checked={(this.state.value & item.mask) !== 0}
                value={item.mask}
                onChange={this.changeValue}
                disabled={this.props.readonly}
              />
              <span className="custom-control-label">{item.text}</span>
            </label>
          )
        })}
      </div>
    )
  }

  renderViewElement() {
    const value = this.state.value
    if (!value) return super.renderViewElement()
    return (
      <div className="form-control-plaintext">
        {__findMultiTexts(this.props.options, value).map((item) => {
          return (
            <span key={'m-' + item} className="badge">
              {item}
            </span>
          )
        })}
      </div>
    )
  }

  changeValue = () => {
    let maskValue = 0
    $(this._fieldValue__wrap)
      .find('input:checked')
      .each(function () {
        maskValue += ~~$(this).val()
      })
    this.handleChange({ target: { value: maskValue === 0 ? null : maskValue } }, true)
  }
}

const BoolOptions = {
  T: $L('True'),
  F: $L('False'),
}
class RbFormBool extends RbFormElement {
  constructor(props) {
    super(props)
  }

  renderElement() {
    return (
      <div className="mt-1">
        <label className="custom-control custom-radio custom-control-inline">
          <input
            className="custom-control-input"
            name={'radio-' + this.props.field}
            type="radio"
            checked={this.state.value === 'T'}
            data-value="T"
            onChange={this.changeValue}
            disabled={this.props.readonly}
          />
          <span className="custom-control-label">{$L('True')}</span>
        </label>
        <label className="custom-control custom-radio custom-control-inline">
          <input
            className="custom-control-input"
            name={'radio-' + this.props.field}
            type="radio"
            checked={this.state.value === 'F'}
            data-value="F"
            onChange={this.changeValue}
            disabled={this.props.readonly}
          />
          <span className="custom-control-label">{$L('False')}</span>
        </label>
      </div>
    )
  }

  renderViewElement() {
    return super.renderViewElement(this.state.value ? BoolOptions[this.state.value] : null)
  }

  changeValue = (e) => {
    const val = e.target.dataset.value
    this.handleChange({ target: { value: val } }, true)
  }
}

class RbFormState extends RbFormPickList {
  constructor(props) {
    super(props)
  }
}

class RbFormBarcode extends RbFormElement {
  constructor(props) {
    super(props)
  }

  renderElement() {
    if (this.state.value) return this.renderViewElement()
    else return <div className="form-control-plaintext barcode text-muted">{`${$L('AutoValue')} (${$L(this.props.barcodeType === 'QRCODE' ? 'QrCode' : 'BarCode')})`}</div>
  }

  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()
    const codeUrl = `${rb.baseUrl}/commons/barcode/render${this.props.barcodeType === 'QRCODE' ? '-qr' : ''}?t=${$encode(this.state.value)}`
    return (
      <div className="img-field barcode">
        <a className="img-thumbnail" title={this.state.value}>
          <img src={codeUrl} alt={this.state.value} />
        </a>
      </div>
    )
  }
}

// 不支持/未开放的字段
class RbFormUnsupportted extends RbFormElement {
  constructor(props) {
    super(props)
  }

  renderElement() {
    return <div className="form-control-plaintext text-danger">UNSUPPORTTED</div>
  }

  renderViewElement() {
    return this.renderElement()
  }
}

// 分割线
class RbFormDivider extends React.Component {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <div className="form-line hover" ref={(c) => (this._element = c)}>
        <fieldset>{this.props.label && <legend onClick={this.toggle}>{this.props.label}</legend>}</fieldset>
      </div>
    )
  }

  toggle = () => {
    if (this.props.onView) {
      let $next = $(this._element).parent()
      while (($next = $next.next()).length > 0) {
        if ($next.find('>.form-line').length > 0) break
        $next.toggleClass('hide')
      }
    } else {
      let $next = $(this._element)
      while (($next = $next.next()).length > 0) {
        if ($next.hasClass('form-line') || $next.hasClass('footer')) break
        $next.toggleClass('hide')
      }
    }
  }
}

const TYPE_DIVIDER = '$DIVIDER$'

// 确定元素类型
const detectElement = function (item) {
  if (!item.key) item.key = 'field-' + (item.field === TYPE_DIVIDER ? $random() : item.field)

  // 扩展组件
  const extElement = detectElementExt(item)
  if (extElement) return extElement

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
  } else if (item.type === 'BOOL') {
    return <RbFormBool {...item} />
  } else if (item.type === 'STATE') {
    return <RbFormState {...item} />
  } else if (item.type === 'BARCODE') {
    return <RbFormBarcode {...item} readonly={true} />
  } else if (item.field === TYPE_DIVIDER || item.field === '$LINE$') {
    return <RbFormDivider {...item} />
  } else {
    return <RbFormUnsupportted {...item} />
  }
}
// eslint-disable-next-line no-unused-vars
var detectElementExt = function (item) {
  return null
}

// 获取选项型字段显示值
const __findOptionText = function (options, value) {
  if ((options || []).length === 0 || !value) return null
  const o = options.find((x) => {
    // eslint-disable-next-line eqeqeq
    return x.id == value
  })
  return o ? o.text || `[${value.toUpperCase()}]` : `[${value.toUpperCase()}]`
}

// 多选文本
const __findMultiTexts = function (options, maskValue) {
  const texts = []
  options.map((item) => {
    if ((maskValue & item.mask) !== 0) texts.push(item.text)
  })
  return texts
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
      <div className="modal selector" ref={(c) => (this._dlg = c)} tabIndex="-1">
        <div className="modal-dialog">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <h5 className="mt-0 text-bold">{$L('SelectSome').replace('{0}', this.props.label)}</h5>
              <div>
                <select ref={(c) => this._select.push(c)} className="form-control form-control-sm">
                  {(this.state.datas[0] || []).map((item) => {
                    return (
                      <option key={'item-' + item[0]} value={item[0]}>
                        {item[1]}
                      </option>
                    )
                  })}
                </select>
              </div>
              {this.state.openLevel >= 1 && (
                <div>
                  <select ref={(c) => this._select.push(c)} className="form-control form-control-sm">
                    {(this.state.datas[1] || []).map((item) => {
                      return (
                        <option key={'item-' + item[0]} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                </div>
              )}
              {this.state.openLevel >= 2 && (
                <div>
                  <select ref={(c) => this._select.push(c)} className="form-control form-control-sm">
                    {(this.state.datas[2] || []).map((item) => {
                      return (
                        <option key={'item-' + item[0]} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                </div>
              )}
              {this.state.openLevel >= 3 && (
                <div>
                  <select ref={(c) => this._select.push(c)} className="form-control form-control-sm">
                    {(this.state.datas[3] || []).map((item) => {
                      return (
                        <option key={'item-' + item[0]} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                </div>
              )}
              <div>
                <button className="btn btn-primary w-100" onClick={() => this.confirm()}>
                  {$L('Confirm')}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    const m = this.show()
    m.on('hidden.bs.modal', () => {
      $(document.body).addClass('modal-open') // keep scroll
    })

    const that = this
    $(this._select).each(function (idx) {
      const s = $(this)
        .select2({
          placeholder: $L('SelectSome').replace('{0}', $L('XLevelClass').replace('%d', idx + 1)),
          allowClear: false,
        })
        .on('change', () => {
          const p = $(s).val()
          if (p) {
            if (s.__level < that.state.openLevel) {
              that.loadData(s.__level + 1, p) // Load next-level
            }
          }
        })
      s.__level = idx
      that._select2.push(s)
    })
    this.loadData(0)
  }

  loadData(level, p) {
    $.get(`/commons/metadata/classification?entity=${this.props.entity}&field=${this.props.field}&parent=${p || ''}`, (res) => {
      const s = this.state.datas
      s[level] = res.data
      this.setState({ datas: s }, () => this._select2[level].trigger('change'))
    })
  }

  confirm() {
    const last = this._select2[this.state.openLevel]
    const v = last.val()
    if (!v) {
      RbHighbar.create($L('PlsSelectSome').replace('{0}', this.props.label))
    } else {
      const text = []
      $(this._select2).each(function () {
        text.push(this.select2('data')[0].text)
      })
      this.props.$$$parent.setClassificationValue({ id: v, text: text.join('.') })
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

// see `reference-search.html`
// eslint-disable-next-line no-unused-vars
var referenceSearch__call = function (selected) {
  /* NOOP */
}
// eslint-disable-next-line no-unused-vars
var referenceSearch__dialog

class ReferenceSearcher extends RbModal {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <div className="modal rbmodal colored-header colored-header-primary" ref={(c) => (this._rbmodal = c)}>
        <div className="modal-dialog modal-lg">
          <div className="modal-content">
            <div className="modal-header modal-header-colored">
              <h3 className="modal-title">{this.props.title || $L('Query')}</h3>
              <button className="close" type="button" onClick={() => this.hide()}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body iframe">
              <iframe src={this.props.url} frameBorder="0" style={{ minHeight: 368 }} />
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount()
    // eslint-disable-next-line no-unused-vars
    referenceSearch__dialog = this
  }
}

// 删除确认
// eslint-disable-next-line no-unused-vars
class DeleteConfirm extends RbAlert {
  constructor(props) {
    super(props)
    this.state = { enableCascades: false }
  }

  render() {
    let message = this.props.message
    if (!message) message = this.props.ids ? $L('DeleteSelectedSomeConfirm').replace('%d', this.props.ids.length) : $L('DeleteRecordConfirm')

    return (
      <div className="modal rbalert" ref={(c) => (this._dlg = c)} tabIndex="-1">
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <div className="text-center ml-6 mr-6">
                <div className="text-danger">
                  <span className="modal-main-icon zmdi zmdi-alert-triangle" />
                </div>
                <div className="mt-3 text-bold">{message}</div>
                {!this.props.entity ? null : (
                  <div className="mt-2">
                    <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-2">
                      <input className="custom-control-input" type="checkbox" checked={this.state.enableCascade === true} onChange={() => this.enableCascade()} />
                      <span className="custom-control-label"> {$L('DeleteCasTips')}</span>
                    </label>
                    <div className={' ' + (this.state.enableCascade ? '' : 'hide')}>
                      <select className="form-control form-control-sm" ref={(c) => (this._cascades = c)} multiple>
                        {(this.state.cascadesEntity || []).map((item) => {
                          return (
                            <option key={'option-' + item[0]} value={item[0]}>
                              {item[1]}
                            </option>
                          )
                        })}
                      </select>
                    </div>
                  </div>
                )}
                <div className="mt-4 mb-3" ref={(c) => (this._btns = c)}>
                  <button className="btn btn-space btn-secondary" type="button" onClick={() => this.hide()}>
                    {$L('Cancel')}
                  </button>
                  <button className="btn btn-space btn-danger" type="button" onClick={() => this.handleDelete()}>
                    {$L('Delete')}
                  </button>
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
      $.get(`/commons/metadata/references?entity=${this.props.entity}&permission=D`, (res) => {
        this.setState({ cascadesEntity: res.data }, () => {
          this.__select2 = $(this._cascades)
            .select2({
              placeholder: $L('SelectCasEntity'),
              width: '88%',
            })
            .val(null)
            .trigger('change')
        })
      })
    }
  }

  handleDelete() {
    let ids = this.props.ids || this.props.id
    if (!ids || ids.length === 0) return
    if (typeof ids === 'object') ids = ids.join(',')
    const cascades = this.__select2 ? this.__select2.val().join(',') : ''

    const btns = $(this._btns).find('.btn').button('loading')
    $.post(`/app/entity/record-delete?id=${ids}&cascades=${cascades}`, (res) => {
      if (res.error_code === 0) {
        if (res.data.deleted === res.data.requests) RbHighbar.success($L('SomeSuccess', 'Delete'))
        else if (res.data.deleted === 0) RbHighbar.error($L('NotDeleteTips'))
        else RbHighbar.success($L('SuccessDeletedXItems').replace('%d', res.data.deleted))

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
    return (
      <RbModal ref={(c) => (this._dlg = c)} title={$L('ExistsXDuplicateItems').replace('%d', this.props.data.length - 1)} disposeOnHide={true} colored="warning">
        <table className="table table-striped table-hover table-sm dialog-table">
          <thead>
            <tr>
              {data[0].map((item, idx) => {
                if (idx === 0) return null
                return <th key={`field-${idx}`}>{item}</th>
              })}
              <th width="30"></th>
            </tr>
          </thead>
          <tbody>
            {data.map((item, idx) => {
              if (idx === 0) return null
              return this.renderLine(item, idx)
            })}
          </tbody>
        </table>
      </RbModal>
    )
  }

  renderLine(item, idx) {
    return (
      <tr key={`row-${idx}`}>
        {item.map((o, i) => {
          if (i === 0) return null
          return <td key={`col-${idx}-${i}`}>{o || <span className="text-muted">{$L('Null')}</span>}</td>
        })}
        <td className="actions">
          <a className="icon" onClick={() => this.openView(item[0])} title={$L('ViewDetails')}>
            <i className="zmdi zmdi-open-in-new" />
          </a>
        </td>
      </tr>
    )
  }

  openView(id) {
    if (window.RbViewModal) window.RbViewModal.create({ id: id, entity: this.props.entity })
    else window.open(`${rb.baseUrl}/app/list-and-view?id=${id}`)
  }
}
