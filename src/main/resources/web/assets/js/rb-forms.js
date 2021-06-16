/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global SimpleMDE */

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
            <div className="modal-dialog modal-lg">
              <div className="modal-content">
                <div className="modal-header modal-header-colored">
                  {this.state.icon && <span className={'icon zmdi zmdi-' + this.state.icon} />}
                  <h3 className="modal-title">{this.state.title || $L('新建')}</h3>
                  {rb.isAdminUser && (
                    <a className="close s" href={`${rb.baseUrl}/admin/entity/${this.state.entity}/form-design`} title={$L('表单设计')} target="_blank">
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
        <div className="message" dangerouslySetInnerHTML={{ __html: `<strong>${$L('抱歉!')}</strong> ` + message }}></div>
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
    $.get(`/app/entity/extras/record-last-modified?id=${this.state.id}`, (res) => {
      if (res.error_code === 0) {
        if (res.data.lastModified !== this.__lastModified) {
          // this.setState({ alertMessage: <p>记录已由其他用户编辑过，<a onClick={() => this.__refresh()}>点击此处</a>查看最新数据</p> })
          this._refresh()
        }
      } else if (res.error_msg === 'NO_EXISTS') {
        this.setState({ alertMessage: $L('记录已经不存在，可能已被其他用户删除') })
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
   * @param {*} newDlg
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
    if (pmodel.hadApproval) {
      moreActions.push(
        <a key="Action103" className="dropdown-item" onClick={() => this.post(103)}>
          {$L('保存并提交')}
        </a>
      )
    }

    if (pmodel.isMain === true) {
      moreActions.push(
        <a key="Action102" className="dropdown-item" onClick={() => this.post(102)}>
          {$L('保存并添加明细')}
        </a>
      )
    } else if (pmodel.isDetail === true) {
      moreActions.push(
        <a key="Action101" className="dropdown-item" onClick={() => this.post(101)}>
          {$L('保存并继续添加')}
        </a>
      )
    }

    let actionBtn = (
      <button className="btn btn-primary btn-space" type="button" onClick={() => this.post()}>
        {$L('保存')}
      </button>
    )
    if (moreActions.length > 0) {
      actionBtn = (
        <div className="btn-group dropup btn-space">
          <button className="btn btn-primary" type="button" onClick={() => this.post()}>
            {$L('保存')}
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
            {$L('取消')}
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
      const fieldComp = that.refs[`fieldcomp-${item.target}`]
      if (fieldComp) {
        if (!item.fillinForce && fieldComp.getValue()) return
        if ((that.isNew && item.whenCreate) || (!that.isNew && item.whenUpdate)) fieldComp.setValue(item.value)
      }
    })
  }

  // 设置字段值
  setFieldValue(field, value, error) {
    this.__FormData[field] = { value: value, error: error }
    if (!error) RbForm.__FIELDVALUECHANGE_CALLS.forEach((c) => c({ name: field, value: value }))
    // eslint-disable-next-line no-console
    if (rb.env === 'dev') console.log('FV1 ... ' + JSON.stringify(this.__FormData))
  }

  // 避免无意义更新
  setFieldUnchanged(field) {
    delete this.__FormData[field]
    RbForm.__FIELDVALUECHANGE_CALLS.forEach((c) => c({ name: field }))
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
        RbHighbar.success($L('保存成功'))
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
              title: $L('添加%s', sm.entityLabel),
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

  static __FIELDVALUECHANGE_CALLS = []
  // 字段值变化回调
  static onFieldValueChange(call) {
    RbForm.__FIELDVALUECHANGE_CALLS.push(call)
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
        <label
          ref={(c) => (this._fieldLabel = c)}
          className={`col-12 col-sm-${colWidths[0]} col-form-label text-sm-right ${!props.onView && !props.nullable ? 'required' : ''}`}>
          {props.label}
        </label>
        <div ref={(c) => (this._fieldText = c)} className={'col-12 col-sm-' + colWidths[1]}>
          {!props.onView || (editable && this.state.editMode) ? this.renderElement() : this.renderViewElement()}
          {!props.onView && props.tip && <p className="form-text">{props.tip}</p>}
          {editable && !this.state.editMode && <a className="edit" title={$L('编辑')} onClick={() => this.toggleEditMode(true)} />}
          {editable && this.state.editMode && (
            <div className="edit-oper">
              <div className="btn-group shadow-sm">
                <button type="button" className="btn btn-secondary" onClick={() => this.handleEditConfirm()}>
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

  componentDidMount() {
    const props = this.props
    if (!props.onView) {
      // 必填字段
      if (!props.nullable && $empty(props.value)) props.$$$parent.setFieldValue(props.field, null, $L('%s 不能为空', props.label))
      props.tip && $(this._fieldLabel).find('i.zmdi').tooltip({ placement: 'right' })
    }
    if (!props.onView) this.onEditModeChanged()
  }

  componentWillUnmount() {
    this.onEditModeChanged(true)
  }

  /**
   * 渲染表单
   */
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
        maxLength={this.props.maxLength || 200}
      />
    )
  }

  /**
   * 渲染视图
   */
  renderViewElement() {
    let value = arguments.length > 0 ? arguments[0] : this.state.value
    if (value && $empty(value)) value = null
    return (
      <React.Fragment>
        <div className="form-control-plaintext">{value || <span className="text-muted">{$L('无')}</span>}</div>
      </React.Fragment>
    )
  }

  /**
   * 修改值（表单组件（字段）值变化应调用此方法）
   *
   * @param {*} e
   * @param {*} checkValue
   */
  handleChange(e, checkValue) {
    const val = e.target.value
    this.setState({ value: val }, () => checkValue === true && this.checkValue())
  }

  /**
   * 清空值
   */
  handleClear() {
    this.setState({ value: '' }, () => this.checkValue())
  }

  /**
   * 检查值
   */
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

  /**
   * 无效值检查
   */
  isValueError() {
    if (this.props.nullable === false) {
      const v = this.state.value
      if (v && $.type(v) === 'array') return v.length === 0 ? $L('不能为空') : null
      else return !v ? $L('不能为空') : null
    }
  }

  /**
   * 是否未修改
   */
  isValueUnchanged() {
    const oldv = this.state.newValue === undefined ? this.props.value : this.state.newValue
    return $same(oldv, this.state.value)
  }

  /**
   * 视图编辑-编辑状态改变
   *
   * @param {*} destroy
   */
  onEditModeChanged(destroy) {
    if (destroy) {
      if (this.__select2) {
        if ($.type(this.__select2) === 'array') {
          $(this.__select2).each(function () {
            this.select2('destroy')
          })
        } else {
          this.__select2.select2('destroy')
        }
        this.__select2 = null
      }
    }
  }

  /**
   * 视图编辑-编辑模式
   *
   * @param {*} mode
   */
  toggleEditMode(editMode) {
    // if (editMode) {
    //   this.setState({ editMode: editMode }, () => {
    //     this.onEditModeChanged()
    //     this._fieldValue && this._fieldValue.focus()
    //   })
    // } else {
    //   const newValue = arguments.length > 1 ? arguments[1] : this.state.newValue === undefined ? this.props.value : this.state.newValue
    //   const state = { editMode: editMode, value: newValue, newValue: newValue || null }
    //
    //   this.onEditModeChanged(true)
    //   this.setState(state)
    // }

    this.setState({ editMode: editMode }, () => {
      if (this.state.editMode) {
        this.onEditModeChanged()
        this._fieldValue && this._fieldValue.focus()
      } else {
        const newValue = arguments.length > 1 ? arguments[1] : this.state.newValue === undefined ? this.props.value : this.state.newValue
        this.setState({ value: newValue, newValue: newValue || null }, () => this.onEditModeChanged(true))
      }
    })
  }

  /**
   * 视图编辑-确认
   */
  handleEditConfirm() {
    this.props.$$$parent.saveSingleFieldValue && this.props.$$$parent.saveSingleFieldValue(this)
  }

  // Setter
  setValue(val) {
    this.handleChange({ target: { value: val } }, true)
  }
  // Getter
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

    const clickUrl = `${rb.baseUrl}/commons/url-safe?url=${encodeURIComponent(this.state.value)}`
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
    return !!this.state.value && $regex.isUrl(this.state.value) === false ? $L('格式不正确') : null
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
        <a title={$L('发送邮件')} href={`mailto:${this.state.value}`} className="link">
          {this.state.value}
        </a>
      </div>
    )
  }

  isValueError() {
    const err = super.isValueError()
    if (err) return err
    return !!this.state.value && $regex.isMail(this.state.value) === false ? $L('格式不正确') : null
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
        <a title={$L('拨打电话')} href={`tel:${this.state.value}`} className="link">
          {this.state.value}
        </a>
      </div>
    )
  }

  isValueError() {
    const err = super.isValueError()
    if (err) return err
    return !!this.state.value && $regex.isTel(this.state.value) === false ? $L('格式不正确') : null
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
    if (!!this.state.value && $regex.isNumber(this.state.value) === false) return $L('格式不正确')
    if (!!this.state.value && $isTrue(this.props.notNegative) && parseFloat(this.state.value) < 0) return $L('不能为负数')
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
        maxLength="29"
      />
    )
  }

  componentDidMount() {
    super.componentDidMount()

    // 表单计算（视图下无效）
    if (this.props.calcFormula && !this.props.onView) {
      const calcFormula = this.props.calcFormula.replace(new RegExp('×', 'ig'), '*').replace(new RegExp('÷', 'ig'), '/')
      const watchFields = calcFormula.match(/\{([a-z0-9]+)\}/gi) || []

      this.calcFormula__values = {}
      // 初始值
      watchFields.forEach((item) => {
        const name = item.substr(1, item.length - 2)
        const fieldComp = this.props.$$$parent.refs[`fieldcomp-${name}`]
        if (fieldComp && fieldComp.state.value) {
          this.calcFormula__values[name] = fieldComp.state.value
        }
      })

      // 小数位
      const fixed = this.props.decimalFormat ? (this.props.decimalFormat.split('.')[1] || '').length : 0

      RbForm.onFieldValueChange((s) => {
        if (!watchFields.includes(`{${s.name}}`)) return
        this.calcFormula__values[s.name] = s.value

        let formula = calcFormula
        for (let key in this.calcFormula__values) {
          formula = formula.replace(new RegExp(`{${key}}`, 'ig'), this.calcFormula__values[key] || 0)
        }
        if (formula.includes('{')) return

        try {
          let calcv
          eval(`calcv = ${formula}`)
          if (!isNaN(calcv)) this.setValue(calcv.toFixed(fixed))
        } catch (err) {
          if (rb.env === 'dev') console.log(err)
        }
      })
    }
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
    if (!!this.state.value && $regex.isDecimal(this.state.value) === false) return $L('格式不正确')
    if (!!this.state.value && $isTrue(this.props.notNegative) && parseFloat(this.state.value) < 0) return $L('不能为负数')
    return null
  }
}

class RbFormTextarea extends RbFormElement {
  constructor(props) {
    super(props)
  }

  renderElement() {
    return (
      <React.Fragment>
        <textarea
          ref={(c) => (this._fieldValue = c)}
          className={`form-control form-control-sm row3x ${this.state.hasError ? 'is-invalid' : ''} ${
            this.props.useMdedit && this.props.readonly ? 'cm-readonly' : ''
          }`}
          title={this.state.hasError}
          value={this.state.value || ''}
          onChange={this.handleChange}
          onBlur={this.props.readonly ? null : this.checkValue}
          readOnly={this.props.readonly}
          maxLength="6000"
        />
        {this.props.useMdedit && !this.props.readonly && <input type="file" className="hide" ref={(c) => (this._fieldValue__upload = c)} />}
      </React.Fragment>
    )
  }

  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()

    if (this.props.useMdedit) {
      const md2html = SimpleMDE.prototype.markdown(this.state.value)
      return <div className="form-control-plaintext mdedit-content" ref={(c) => (this._textarea = c)} dangerouslySetInnerHTML={{ __html: md2html }} />
    } else {
      return (
        <div className="form-control-plaintext" ref={(c) => (this._textarea = c)}>
          {this.state.value.split('\n').map((line, idx) => {
            return <p key={'kl-' + idx}>{line}</p>
          })}
        </div>
      )
    }
  }

  componentDidMount() {
    super.componentDidMount()
    this.props.onView && this.onEditModeChanged(true)
  }

  UNSAFE_componentWillUpdate(nextProps, nextState) {
    // destroy
    if (this.state.editMode && !nextState.editMode) {
      if (this._simplemde) {
        this._simplemde.toTextArea()
        this._simplemde = null
      }
    }
  }

  onEditModeChanged(destroy) {
    if (this._textarea) {
      if (destroy) {
        $(this._textarea).perfectScrollbar()
      } else {
        $(this._textarea).perfectScrollbar('destroy')
      }
    }

    if (this.props.useMdedit && !destroy) this._initMde()
  }

  setValue(val) {
    super.setValue(val)
    if (this.props.useMdedit) this._simplemde.value(val)
  }

  _initMde() {
    const mde = new SimpleMDE({
      element: this._fieldValue,
      status: false,
      autoDownloadFontAwesome: false,
      spellChecker: false,
      // eslint-disable-next-line no-undef
      toolbar: this.props.readonly ? false : DEFAULT_MDE_TOOLBAR,
    })
    this._simplemde = mde

    if (this.props.readonly) {
      mde.codemirror.setOption('readOnly', true)
    } else {
      $createUploader(this._fieldValue__upload, null, (res) => {
        const pos = mde.codemirror.getCursor()
        mde.codemirror.setSelection(pos, pos)
        mde.codemirror.replaceSelection(`![](${rb.baseUrl}/filex/img/${res.key})`)
      })
      if (this.props.onView) {
        setTimeout(() => {
          mde.codemirror.focus()
          mde.codemirror.setCursor(mde.codemirror.lineCount(), 0) // cursor at end
        }, 100)
      }

      mde.codemirror.on('changes', () => {
        $setTimeout(
          () => {
            this.setState({ value: mde.value() }, this.checkValue)
          },
          200,
          'mde-update-event'
        )
      })
    }
  }
}

class RbFormDateTime extends RbFormElement {
  constructor(props) {
    super(props)
  }

  renderElement() {
    if (this.props.readonly) return super.renderElement()

    return (
      <div className="input-group has-append">
        <input
          ref={(c) => (this._fieldValue = c)}
          className={'form-control form-control-sm ' + (this.state.hasError ? 'is-invalid' : '')}
          title={this.state.hasError}
          type="text"
          value={this.state.value || ''}
          onChange={this.handleChange}
          onBlur={this.checkValue}
          maxLength="20"
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
    } else if (!this.props.readonly) {
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
                <img src={`${rb.baseUrl}/filex/img/${item}?imageView2/2/w/100/interlace/1/q/100`} alt="IMG" />
                {!this.props.readonly && (
                  <b title={$L('移除')} onClick={() => this.removeItem(item)}>
                    <span className="zmdi zmdi-close"></span>
                  </b>
                )}
              </a>
            </span>
          )
        })}
        {showUpload && (
          <span title={$L('上传图片。需要 %s 个', `${this.__minUpload}~${this.__maxUpload}`)}>
            <input ref={(c) => (this._fieldValue__input = c)} type="file" className="inputfile" id={`${this.props.field}-input`} accept="image/*" />
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
                <img src={`${rb.baseUrl}/filex/img/${item}?imageView2/2/w/100/interlace/1/q/100`} alt="IMG" />
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
    if (this.__minUpload > 0 && ups < this.__minUpload) return $L('至少需要上传 %d 个', this.__minUpload)
    if (this.__maxUpload < ups) return $L('最多允许上传 %d 个', this.__maxUpload)
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
                <b title={$L('移除')} onClick={() => this.removeItem(item)}>
                  <span className="zmdi zmdi-close"></span>
                </b>
              )}
            </div>
          )
        })}
        {showUpload && (
          <div className="file-select">
            <input type="file" className="inputfile" ref={(c) => (this._fieldValue__input = c)} id={`${this.props.field}-input`} />
            <label
              htmlFor={`${this.props.field}-input`}
              title={$L('上传文件。需要 %d 个', `${this.__minUpload}~${this.__maxUpload}`)}
              className="btn-secondary">
              <i className="zmdi zmdi-upload"></i>
              <span>{$L('上传文件')}</span>
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
    const keyName = `${this.state.field}-opt-`
    return (
      <select ref={(c) => (this._fieldValue = c)} className="form-control form-control-sm" value={this.state.value || ''} onChange={this.handleChange}>
        <option value=""></option>
        {this.state.options.map((item) => {
          return (
            <option key={`${keyName}${item.id}`} value={item.id}>
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
        placeholder: $L('选择%s', this.props.label),
      })

      const that = this
      this.__select2
        .on('change', function (e) {
          const val = e.target.value
          that.handleChange({ target: { value: val } }, true)
        })
        .trigger('change')

      if (this.props.readonly) $(this._fieldValue).attr('disabled', true)
    }
  }

  isValueUnchanged() {
    if (this.props.$$$parent.isNew === true) return false
    return super.isValueUnchanged()
  }

  setValue(val) {
    if (typeof val === 'object') val = val.id
    this.__select2.val(val).trigger('change')
  }
}

class RbFormReference extends RbFormElement {
  constructor(props) {
    super(props)
  }

  renderElement() {
    const hasDataFilter = this.props.referenceDataFilter && (this.props.referenceDataFilter.items || []).length > 0
    return (
      <div className="input-group has-append">
        <select
          ref={(c) => (this._fieldValue = c)}
          className="form-control form-control-sm"
          title={hasDataFilter ? $L('当前字段已启用数据过滤') : null}
          multiple={this._multiple === true}
        />
        {!this.props.readonly && (
          <div className="input-group-append">
            <button className="btn btn-secondary" type="button" onClick={() => this.showSearcher()}>
              <i className="icon zmdi zmdi-search" />
            </button>
          </div>
        )}
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
  _renderViewElement() {
    return super.renderViewElement()
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
        this.setValue(val)
        // const o = new Option(val.text, val.id, true, true)
        // this.__select2.append(o).trigger('change')
      }

      const that = this
      this.__select2.on('change', function (e) {
        const v = $(e.target).val()
        if (v && typeof v === 'string') {
          $.post(`/commons/search/recently-add?id=${v}`)
          that.triggerAutoFillin(v)
        }
        that.handleChange({ target: { value: v } }, true)
      })

      if (this.props.readonly) $(this._fieldValue).attr('disabled', true)
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
    } else {
      this.__select2.val(null).trigger('change')
    }
  }

  showSearcher() {
    const that = this
    window.referenceSearch__call = function (selected) {
      that.showSearcher_call(selected, that)
      that.__searcher.hide()
    }

    if (this.__searcher) {
      this.__searcher.show()
    } else {
      const searchUrl = `${rb.baseUrl}/commons/search/reference-search?field=${this.props.field}.${this.props.$$$parent.props.entity}`
      // eslint-disable-next-line react/jsx-no-undef
      renderRbcomp(<ReferenceSearcher url={searchUrl} title={$L('选择%s', this.props.label)} />, function () {
        that.__searcher = this
      })
    }
  }

  showSearcher_call(selected, that) {
    const first = selected[0]
    if ($(that._fieldValue).find(`option[value="${first}"]`).length > 0) {
      that.__select2.val(first).trigger('change')
    } else {
      $.get(`/commons/search/read-labels?ids=${first}`, (res) => {
        const o = new Option(res.data[first], first, true, true)
        that.__select2.append(o).trigger('change')
      })
    }
  }
}

class RbFormN2NReference extends RbFormReference {
  constructor(props) {
    super(props)
    this._multiple = true
  }

  renderViewElement() {
    const value = this.state.value
    if ($empty(value)) return super._renderViewElement()
    if (typeof value === 'string') return <div className="form-control-plaintext">{value}</div>

    return (
      <div className="form-control-plaintext">
        {value.map((item) => {
          return (
            <a key={`o-${item.id}`} href={`#!/View/${item.entity}/${item.id}`} onClick={this._clickView}>
              {item.text}
            </a>
          )
        })}
      </div>
    )
  }

  isValueUnchanged() {
    let oldvArray = this.state.newValue || this.props.value || []
    let oldv = []
    oldvArray.forEach((s) => oldv.push(s.id))
    return $same(oldv.join(','), this.state.value)
  }

  handleChange(e, checkValue) {
    let val = e.target.value
    if (typeof val === 'object') val = val.join(',')
    this.setState({ value: val }, () => checkValue === true && this.checkValue())
  }

  setValue(val, isAppend) {
    if (val && val.length > 0) {
      const currentValue = this.state.value || ''
      const ids = []
      val.forEach((item) => {
        if (!currentValue.includes(item.id)) {
          const o = new Option(item.text, item.id, true, true)
          this.__select2.append(o)
          ids.push(item.id)
        }
      })

      if (ids.length > 0) {
        let ss = ids.join(',')
        if (isAppend && currentValue && currentValue !== '') ss = currentValue + ',' + ss
        this.handleChange({ target: { value: ss } }, true)
      }
    } else {
      this.__select2.val(null).trigger('change')
    }
  }

  showSearcher_call(selected, that) {
    const ids = selected.join(',')
    $.get(`/commons/search/read-labels?ids=${ids}`, (res) => {
      const val = []
      for (let k in res.data) {
        val.push({ id: k, text: res.data[k] })
      }
      that.setValue(val, true)
    })
    this._recentlyAdd(ids)
  }

  onEditModeChanged(destroy) {
    super.onEditModeChanged(destroy)
    if (!destroy && this.__select2) {
      this.__select2.on('select2:select', (e) => this._recentlyAdd(e.params.data.id))
    }
  }

  _recentlyAdd(id) {
    if (!id) return
    $.post(`/commons/search/recently-add?id=${id}`)
  }
}

class RbFormClassification extends RbFormElement {
  constructor(props) {
    super(props)
  }

  renderElement() {
    return (
      <div className="input-group has-append">
        <select ref={(c) => (this._fieldValue = c)} className="form-control form-control-sm" />
        {!this.props.readonly && (
          <div className="input-group-append">
            <button className="btn btn-secondary" type="button" onClick={this.showSelector}>
              <i className="icon zmdi zmdi-search" />
            </button>
          </div>
        )}
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
      value && this._setClassificationValue(value)

      this.__select2.on('change', () => {
        const v = this.__select2.val()
        if (v) $.post(`/commons/search/recently-add?id=${v}&type=d${this.props.classification}`)
        this.handleChange({ target: { value: v } }, true)
      })

      if (this.props.readonly) $(this._fieldValue).attr('disabled', true)
    }
  }

  isValueUnchanged() {
    const oldv = this.state.newValue === undefined ? (this.props.value || {}).id : (this.state.newValue || {}).id
    return $same(oldv, this.state.value)
  }

  setValue(val) {
    if (val && val.id) this._setClassificationValue(val)
    else this.__select2.val(null).trigger('change')
  }

  showSelector = () => {
    if (this.__selector) this.__selector.show()
    else {
      const p = this.props
      const that = this
      renderRbcomp(
        // eslint-disable-next-line react/jsx-no-undef
        <ClassificationSelector
          entity={p.$$$parent.state.entity}
          field={p.field}
          label={p.label}
          openLevel={p.openLevel}
          onSelect={(s) => this._setClassificationValue(s)}
          keepModalOpen={true}
        />,
        null,
        function () {
          that.__selector = this
        }
      )
    }
  }

  _setClassificationValue(s) {
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
    if (props.value) this.state.value = props.value.id || 0
  }

  renderElement() {
    const keyName = `checkbox-${this.props.field}-`
    return (
      <div className="mt-1" ref={(c) => (this._fieldValue__wrap = c)}>
        {(this.props.options || []).length === 0 && <div className="text-danger">{$L('未配置')}</div>}
        {(this.props.options || []).map((item) => {
          return (
            <label key={keyName + item.mask} className="custom-control custom-checkbox custom-control-inline">
              <input
                className="custom-control-input"
                name={keyName}
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
    let value = this.state.value
    if (!value) return super.renderViewElement()
    if (typeof value === 'object') value = value.id

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

class RbFormBool extends RbFormElement {
  _Options = {
    T: $L('是'),
    F: $L('否'),
  }

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
            checked={$isTrue(this.state.value)}
            data-value="T"
            onChange={this.changeValue}
            disabled={this.props.readonly}
          />
          <span className="custom-control-label">{this._Options['T']}</span>
        </label>
        <label className="custom-control custom-radio custom-control-inline">
          <input
            className="custom-control-input"
            name={'radio-' + this.props.field}
            type="radio"
            checked={!$isTrue(this.state.value)}
            data-value="F"
            onChange={this.changeValue}
            disabled={this.props.readonly}
          />
          <span className="custom-control-label">{this._Options['F']}</span>
        </label>
      </div>
    )
  }

  renderViewElement() {
    return super.renderViewElement(this.state.value ? this._Options[this.state.value] : null)
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
    else
      return (
        <div className="form-control-plaintext barcode text-muted">
          {$L('自动值')} ({this.props.barcodeType === 'QRCODE' ? $L('二维码') : $L('条形码')})
        </div>
      )
  }

  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()

    const codeUrl = `${rb.baseUrl}/commons/barcode/render${this.props.barcodeType === 'BARCODE' ? '' : '-qr'}?t=${$encode(this.state.value)}`
    return (
      <div className="img-field barcode">
        <a className="img-thumbnail" title={this.state.value}>
          <img src={codeUrl} alt={this.state.value} />
        </a>
      </div>
    )
  }
}

class RbFormAvatar extends RbFormElement {
  constructor(props) {
    super(props)
  }

  renderElement() {
    const aUrl = rb.baseUrl + (this.state.value ? `/filex/img/${this.state.value}?imageView2/2/w/100/interlace/1/q/100` : '/assets/img/avatar.png')
    return (
      <div className="img-field avatar">
        <span title={this.props.readonly ? null : $L('选择头像')}>
          {!this.props.readonly && (
            <input ref={(c) => (this._fieldValue__input = c)} type="file" className="inputfile" id={`${this.props.field}-input`} accept="image/*" />
          )}
          <label htmlFor={`${this.props.field}-input`} className="img-thumbnail img-upload">
            <img src={aUrl} alt="Avatar" />
          </label>
        </span>
      </div>
    )
  }

  renderViewElement() {
    const aUrl = rb.baseUrl + (this.state.value ? `/filex/img/${this.state.value}?imageView2/2/w/100/interlace/1/q/100` : '/assets/img/avatar.png')
    return (
      <div className="img-field avatar">
        <a className="img-thumbnail img-upload">
          <img src={aUrl} alt="Avatar" />
        </a>
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
          this.handleChange({ target: { value: res.key } }, true)
        },
        () => mp_end()
      )
    }
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
        <fieldset>{this.props.label && <legend onClick={() => this.toggle()}>{this.props.label}</legend>}</fieldset>
      </div>
    )
  }

  toggle() {
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
var detectElement = function (item) {
  if (!item.key) item.key = 'field-' + (item.field === TYPE_DIVIDER ? $random() : item.field)

  if (item.type === 'TEXT' || item.type === 'SERIES') {
    return <RbFormText {...item} />
  } else if (item.type === 'NTEXT') {
    return <RbFormTextarea {...item} />
  } else if (item.type === 'URL') {
    return <RbFormUrl {...item} />
  } else if (item.type === 'EMAIL') {
    return <RbFormEMail {...item} maxLength="100" />
  } else if (item.type === 'PHONE') {
    return <RbFormPhone {...item} maxLength="40" />
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
  } else if (item.type === 'N2NREFERENCE') {
    return <RbFormN2NReference {...item} />
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
  } else if (item.type === 'AVATAR') {
    return <RbFormAvatar {...item} />
  } else if (item.field === TYPE_DIVIDER || item.field === '$LINE$') {
    return <RbFormDivider {...item} />
  } else {
    return <RbFormUnsupportted {...item} />
  }
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

// ~ 重复记录查看
class RepeatedViewer extends RbModalHandler {
  constructor(props) {
    super(props)
  }

  render() {
    const data = this.props.data
    return (
      <RbModal ref={(c) => (this._dlg = c)} title={$L('存在 %d 条重复记录', this.props.data.length - 1)} disposeOnHide={true} colored="warning">
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
              return this.renderRow(item, idx)
            })}
          </tbody>
        </table>
      </RbModal>
    )
  }

  renderRow(item, idx) {
    return (
      <tr key={`row-${idx}`}>
        {item.map((o, i) => {
          if (i === 0) return null
          return <td key={`col-${idx}-${i}`}>{o || <span className="text-muted">{$L('无')}</span>}</td>
        })}
        <td className="actions">
          <a className="icon" onClick={() => this.openView(item[0])} title={$L('查看详情')}>
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
