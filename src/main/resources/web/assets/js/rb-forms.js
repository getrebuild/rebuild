/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global SimpleMDE, RepeatedViewer, ProTable, Md2Html */

/**
 * Callback API:
 * - RbForm: onFieldValueChange( callback({name:xx,value:xx}) )
 * - RbFormElement: onValueChange(this)
 * - RbFormReference/RbFormN2NReference: getCascadingFieldValue(this)
 */

const TYPE_DIVIDER = '$DIVIDER$'
const MODAL_MAXWIDTH = 1064

// ~~ 表单窗口
class RbFormModal extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, inLoad: true, _maximize: false }

    this.__maximizeKey = `FormMaximize-${props.entity}`
    this.state._maximize = $isTrue($storage.get(this.__maximizeKey))

    if (!props.id) this.state.id = null
  }

  render() {
    const style2 = { maxWidth: this.props.width || MODAL_MAXWIDTH }
    if (this.state._maximize) {
      style2.maxWidth = $(window).width() - 60
      if (style2.maxWidth < MODAL_MAXWIDTH) style2.maxWidth = MODAL_MAXWIDTH
    }

    return (
      <div className="modal-wrapper">
        <div className="modal rbmodal colored-header colored-header-primary" ref={(c) => (this._rbmodal = c)}>
          <div className="modal-dialog" style={style2}>
            <div className="modal-content" style={style2}>
              <div className="modal-header modal-header-colored">
                {this.state.icon && <span className={`icon zmdi zmdi-${this.state.icon}`} />}
                <h3 className="modal-title">{this.state.title || $L('新建')}</h3>
                {rb.isAdminUser && (
                  <a className="close s" href={`${rb.baseUrl}/admin/entity/${this.state.entity}/form-design`} title={$L('表单设计')} target="_blank">
                    <span className="zmdi zmdi-settings up-1" />
                  </a>
                )}
                <button
                  className="close md-close"
                  type="button"
                  title={this.state._maximize ? $L('向下还原') : $L('最大化')}
                  onClick={() => {
                    this.setState({ _maximize: !this.state._maximize }, () => {
                      $storage.set(this.__maximizeKey, this.state._maximize)
                    })
                  }}>
                  <span className={`mdi ${this.state._maximize ? 'mdi mdi-window-restore' : 'mdi mdi-window-maximize'}`} />
                </button>
                <button className="close md-close" type="button" title={$L('关闭')} onClick={() => this.hide()}>
                  <span className="zmdi zmdi-close" />
                </button>
              </div>
              <div className={`modal-body rb-loading ${this.state.inLoad ? 'rb-loading-active' : ''}`}>
                {this.state.alertMessage && (
                  <div className="alert alert-warning rbform-alert">
                    <i className="zmdi zmdi-alert-triangle mr-1" />
                    {this.state.alertMessage}
                  </div>
                )}
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

    let url = `/app/${entity}/form-model?id=${id}`
    if (this.state.previewid) url += `&previewid=${this.state.previewid}`
    $.post(url, JSON.stringify(initialValue), (res) => {
      // 包含错误
      if (res.error_code > 0 || !!res.data.error) {
        const error = (res.data || {}).error || res.error_msg
        this.renderFromError(error)
        return
      }

      const formModel = res.data
      const FORM = (
        <RbForm entity={entity} id={id} rawModel={formModel} $$$parent={this} readonly={!!formModel.readonlyMessage}>
          {formModel.elements.map((item) => {
            return detectElement(item, entity)
          })}
        </RbForm>
      )

      this.setState({ formComponent: FORM, alertMessage: formModel.readonlyMessage || null }, () => {
        this.setState({ inLoad: false })
        if (window.FrontJS) {
          window.FrontJS.Form._trigger('open', [res.data])
        }
      })

      this.__lastModified = res.data.lastModified || 0
    })
  }

  renderFromError(message) {
    const error = (
      <div className="alert alert-danger alert-icon mt-5 w-75 mlr-auto">
        <div className="icon">
          <i className="zmdi zmdi-alert-triangle" />
        </div>
        <div className="message" dangerouslySetInnerHTML={{ __html: `<strong>${$L('抱歉!')}</strong> ${message}` }} />
      </div>
    )
    this.setState({ formComponent: error }, () => this.setState({ inLoad: false }))
  }

  show(state) {
    state = state || {}
    if (!state.id) state.id = null

    let reset = this.state.reset === true
    if (!reset) {
      // 比较初始参数决定是否可复用
      const stateNew = [state.id, state.entity, state.initialValue, state.previewid]
      const stateOld = [this.state.id, this.state.entity, this.state.initialValue, this.state.previewid]
      reset = !$same(stateNew, stateOld)
    }

    if (reset) {
      state = { formComponent: null, initialValue: null, previewid: null, alertMessage: null, inLoad: true, ...state }
      this.setState(state, () => this.showAfter({ reset: false }, true))
    } else {
      this.showAfter({ ...state, reset: false })
      this._checkDrityData()
    }
  }

  showAfter(state, modelChanged) {
    this.setState(state, () => {
      $(this._rbmodal).modal('show')
      if (modelChanged === true) this.getFormModel()
    })
  }

  // 脏数据检查
  _checkDrityData() {
    if (!this.__lastModified || !this.state.id) return

    $.get(`/app/entity/extras/record-last-modified?id=${this.state.id}`, (res) => {
      if (res.error_code === 0) {
        if (res.data.lastModified !== this.__lastModified) {
          // this.setState({ alertMessage: <p>记录已由其他用户编辑过，<a onClick={() => this._refresh()}>点击此处</a>查看最新数据</p> })
          this._refresh()
        }
      } else if (res.error_msg === 'NO_EXISTS') {
        this.setState({ alertMessage: $L('记录已经不存在，可能已被其他用户删除') })
      }
    })
  }

  _refresh() {
    const hs = { id: this.state.id, entity: this.state.entity }
    this.setState({ id: null, alertMessage: null }, () => this.show(hs))
  }

  hide(reset) {
    $(this._rbmodal).modal('hide')

    const state = { reset: reset === true }
    if (state.reset) {
      state.id = null
      state.previewid = null
    }
    this.setState(state)
  }

  // -- Usage
  /**
   * @param {*} props
   * @param {*} forceNew
   */
  static create(props, forceNew) {
    if (forceNew === true) {
      renderRbcomp(<RbFormModal {...props} disposeOnHide />)
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
    const iv = props.rawModel.initialValue
    if (iv) {
      for (let k in iv) {
        const val = iv[k]
        this.__FormData[k] = { value: typeof val === 'object' ? val.id : val, error: null }
      }
    }

    this.isNew = !props.id

    const $$$props = props.$$$parent && props.$$$parent.props ? props.$$$parent.props : {}
    this._postBefore = props.postBefore || $$$props.postBefore
    this._postAfter = props.postAfter || $$$props.postAfter

    this._dividerRefs = []
  }

  render() {
    return (
      <div className="rbform form-layout">
        <div className="form row" ref={(c) => (this._form = c)}>
          {this.props.children.map((fieldComp) => {
            const ref = fieldComp.props.field === TYPE_DIVIDER ? $random('divider-') : `fieldcomp-${fieldComp.props.field}`
            if (fieldComp.props.field === TYPE_DIVIDER && fieldComp.props.collapsed) {
              this._dividerRefs.push(ref)
            }
            return React.cloneElement(fieldComp, { $$$parent: this, ref: ref })
          })}

          {this.renderCustomizedFormArea()}
        </div>

        {this.renderDetailForm()}
        {this.renderFormAction()}
      </div>
    )
  }

  renderCustomizedFormArea() {
    let _FormArea
    if (window._CustomizedForms) _FormArea = window._CustomizedForms.useFormArea(this.props.entity, this)
    return _FormArea || null
  }

  renderDetailForm() {
    const detailMeta = this.props.rawModel.detailMeta
    if (!detailMeta || !window.ProTable) return null

    let _ProTable
    if (window._CustomizedForms) {
      _ProTable = window._CustomizedForms.useProTable(this.props.entity, this)
      if (_ProTable === false) return null // 不显示
    }

    const that = this

    function _addNew(n = 1) {
      for (let i = 0; i < n; i++) {
        setTimeout(() => that._ProTable.addNew(), i * 20)
      }
    }

    function _setLines(details) {
      if (that._ProTable.isEmpty()) {
        that._ProTable.setLines(details)
      } else {
        RbAlert.create($L('是否保留已有明细记录？'), {
          confirmText: $L('保留'),
          cancelText: $L('不保留'),
          onConfirm: function () {
            this.hide()
            that._ProTable.setLines(details)
          },
          onCancel: function () {
            this.hide()
            that._ProTable.clear()
            setTimeout(() => that._ProTable.setLines(details), 200)
          },
        })
      }
    }

    // 记录转换:明细导入
    let detailImports = []
    if (this.props.rawModel.detailImports) {
      this.props.rawModel.detailImports.forEach((item) => {
        detailImports.push({
          icon: item.icon,
          label: item.transName || item.entityLabel,
          fetch: (form, callback) => {
            const formdata = form.getFormData()
            const mainid = form.props.id || null

            $.post(`/app/entity/extras/detail-imports?transid=${item.transid}&mainid=${mainid}`, JSON.stringify(formdata), (res) => {
              if (res.error_code === 0) {
                if ((res.data || []).length === 0) RbHighbar.create($L('没有可导入的明细记录'))
                else typeof callback === 'function' && callback(res.data)
              } else {
                RbHighbar.error(res.error_msg)
              }
            })
          },
        })
      })
    }

    // 记录转换:预览模式
    const previewid = this.props.$$$parent ? this.props.$$$parent.state.previewid : null

    if (!_ProTable) {
      _ProTable = <ProTable entity={detailMeta} mainid={this.state.id} previewid={previewid} ref={(c) => (this._ProTable = c)} $$$main={this} />
    }

    return (
      <div className="detail-form-table">
        <div className="row">
          <div className="col">
            <h5 className="mt-3 mb-0 text-bold fs-14">
              <i className={`icon zmdi zmdi-${detailMeta.icon} fs-15 mr-2`} />
              {detailMeta.entityLabel}
            </h5>
          </div>

          <div className="col text-right">
            {detailImports && detailImports.length > 0 && (
              <div className="btn-group mr-2">
                <button className="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown">
                  <i className="icon mdi mdi-transfer-down"></i> {$L('导入明细')}
                </button>
                <div className="dropdown-menu dropdown-menu-right">
                  {detailImports.map((def, idx) => {
                    return (
                      <a
                        key={`imports-${idx}`}
                        className="dropdown-item"
                        onClick={() => {
                          def.fetch(this, (details) => _setLines(details))
                        }}>
                        {def.icon && <i className={`icon zmdi zmdi-${def.icon}`} />}
                        {def.label}
                      </a>
                    )
                  })}
                </div>
              </div>
            )}

            <div className="btn-group">
              <button className="btn btn-secondary" type="button" onClick={() => _addNew()} disabled={this.props.readonly}>
                <i className="icon x14 zmdi zmdi-playlist-plus mr-1" />
                {$L('添加明细')}
              </button>
              <button className="btn btn-secondary dropdown-toggle w-auto" type="button" data-toggle="dropdown" disabled={this.props.readonly}>
                <i className="icon zmdi zmdi-chevron-down" />
              </button>
              <div className="dropdown-menu dropdown-menu-right">
                {[5, 10, 20].map((n) => {
                  return (
                    <a className="dropdown-item" onClick={() => _addNew(n)} key={`n-${n}`}>
                      {$L('添加 %d 条', n)}
                    </a>
                  )
                })}
              </div>
            </div>
          </div>
        </div>

        <div className="mt-2">{_ProTable}</div>
      </div>
    )
  }

  renderFormAction() {
    let moreActions = []
    // 添加明细
    if (this.props.rawModel.mainMeta) {
      const previewid = this.props.$$$parent ? this.props.$$$parent.state.previewid : null
      if (!previewid) {
        moreActions.push(
          <a key="Action101" className="dropdown-item" onClick={() => this.post(RbForm.NEXT_ADDDETAIL)}>
            {$L('保存并继续添加')}
          </a>
        )
      }
    }
    // 列表页添加
    else if (window.RbViewModal && window.__PageConfig.type === 'RecordList') {
      moreActions.push(
        <a key="Action104" className="dropdown-item" onClick={() => this.post(RbForm.NEXT_VIEW)}>
          {$L('保存并打开')}
        </a>
      )
    }

    // Clean others action
    if (this._postAfter) moreActions = []

    return (
      <div className="dialog-footer" ref={(c) => (this._$formAction = c)}>
        <button className="btn btn-secondary btn-space" type="button" onClick={() => this.props.$$$parent.hide()}>
          {$L('取消')}
        </button>
        {!this.props.readonly && (
          <div className="btn-group dropup btn-space ml-1">
            <button className="btn btn-primary" type="button" onClick={() => this.post()}>
              {$L('保存')}
            </button>
            {moreActions.length > 0 && (
              <React.Fragment>
                <button className="btn btn-primary dropdown-toggle w-auto" type="button" data-toggle="dropdown">
                  <i className="icon zmdi zmdi-chevron-up" />
                </button>
                <div className="dropdown-menu dropdown-menu-primary dropdown-menu-right">{moreActions}</div>
              </React.Fragment>
            )}
          </div>
        )}
      </div>
    )
  }

  componentDidMount() {
    // 新纪录初始值
    if (this.isNew) {
      this.props.children.map((child) => {
        let iv = child.props.value
        if (iv && (!this.props.readonly || (this.props.readonly && this.props.readonlyw === 3))) {
          if (typeof iv === 'object') {
            if (child.props.type === 'TAG') {
              // eg. 标签
              iv = iv.join('$$$$')
            } else if ($.isArray(iv)) {
              // eg. 文件/图片
            } else {
              // eg. {id:xxx, text:xxx}
              iv = iv.id
            }
          }

          this.setFieldValue(child.props.field, iv)
        }
      })
    }

    // v3.2 默认收起
    this._dividerRefs.forEach((d) => {
      // eslint-disable-next-line react/no-string-refs
      this.refs[d].toggle()
    })

    setTimeout(() => RbForm.renderAfter(this), 0)
  }

  // 表单回填
  setAutoFillin(data) {
    if (!data || data.length === 0) return

    this._inAutoFillin = true
    data.forEach((item) => {
      const fieldComp = this.getFieldComp(item.target)
      if (fieldComp) {
        if (!item.fillinForce && fieldComp.getValue()) return
        if ((this.isNew && item.whenCreate) || (!this.isNew && item.whenUpdate)) fieldComp.setValue(item.value)
      }
    })
    this._inAutoFillin = false
  }

  // 设置字段值
  setFieldValue(field, value, error) {
    this.__FormData[field] = { value: value, error: error }
    if (!error) this._onFieldValueChangeCall(field, value)

    // eslint-disable-next-line no-console
    if (rb.env === 'dev') console.log('FV1 ... ' + JSON.stringify(this.__FormData))
  }

  // 避免无意义更新
  setFieldUnchanged(field, originValue) {
    delete this.__FormData[field]
    this._onFieldValueChangeCall(field, originValue)

    // eslint-disable-next-line no-console
    if (rb.env === 'dev') console.log('FV2 ... ' + JSON.stringify(this.__FormData))
  }

  // 添加字段值变化回调
  onFieldValueChange(call) {
    const c = this._onFieldValueChange_calls || []
    c.push(call)
    this._onFieldValueChange_calls = c
  }
  // 执行
  _onFieldValueChangeCall(field, value) {
    if (this._onFieldValueChange_calls) {
      this._onFieldValueChange_calls.forEach((c) => c({ name: field, value: value }))
    }

    if (window.FrontJS) {
      const ret = window.FrontJS.Form._trigger('fieldValueChange', [`${this.props.entity}.${field}`, value, this.props.id || null])
      if (ret === false) return false
    }
  }

  // 获取字段组件
  getFieldComp(field) {
    // eslint-disable-next-line react/no-string-refs
    return this.refs[`fieldcomp-${field}`] || null
  }

  // 获取当前表单数据
  getFormData() {
    const data = {}
    // eslint-disable-next-line react/no-string-refs
    const _refs = this.refs
    for (let key in _refs) {
      if (!key.startsWith('fieldcomp-')) continue

      const fieldComp = _refs[key]
      let v = fieldComp.getValue()
      if (v && typeof v === 'object') v = v.id
      if (v) data[fieldComp.props.field] = v
    }
    return data
  }

  // 保存并添加明细
  static NEXT_ADDDETAIL = 102
  // 保存并打开
  static NEXT_VIEW = 104
  /**
   * @next {Number}
   */
  post(next) {
    // fix dblclick
    if (this.__post === 1) return
    this.__post = 1
    setTimeout(() => (this.__post = 0), 800)
    setTimeout(() => this._post(next), 40)
  }

  _post(next, weakMode) {
    let data = {}
    for (let k in this.__FormData) {
      const err = this.__FormData[k].error
      if (err) return RbHighbar.create(err)
      else data[k] = this.__FormData[k].value
    }

    if (this._FormArea) {
      const data2 = this._FormArea.buildFormData(data)
      if (data2 === false) return
      if (typeof data2 === 'object') {
        data = { ...data, ...data2 }
      }
    }

    if (this._ProTable) {
      const details = this._ProTable.buildFormData()
      if (!details) return

      if (this._ProTable.isEmpty() && this.props.rawModel.detailsNotEmpty === true) {
        RbHighbar.create($L('请添加明细'))
        return
      }
      data['$DETAILS$'] = details
    }

    data.metadata = {
      entity: this.state.entity,
      id: this.state.id,
    }

    if (RbForm.postBefore(data) === false) {
      console.log('FrontJS prevented save')
      return
    }

    const $$$parent = this.props.$$$parent
    const previewid = $$$parent.state.previewid

    const $btn = $(this._$formAction).find('.btn').button('loading')
    let url = '/app/entity/record-save'
    if (previewid) url += `?previewid=${previewid}`
    if (weakMode === true) {
      if (url.includes('?')) url += '&weakMode=true'
      else url += '?weakMode=true'
    }

    $.post(url, JSON.stringify(data), (res) => {
      $btn.button('reset')
      if (res.error_code === 0) {
        RbHighbar.success($L('保存成功'))

        if (location.hash === '#!/New') {
          // location.hash = '!/'
          try {
            localStorage.setItem('referenceSearch__reload', $random())
          } catch (err) {
            // Nothings
          }
        }

        setTimeout(() => {
          $$$parent.hide(true)

          const recordId = res.data.id

          if (typeof this._postAfter === 'function') {
            this._postAfter(recordId)
            return
          } else if (next === RbForm.NEXT_ADDDETAIL) {
            const iv = { '$MAINID$': recordId }
            const dm = this.props.rawModel.detailMeta
            RbFormModal.create({
              title: $L('添加%s', dm.entityLabel),
              entity: dm.entity,
              icon: dm.icon,
              initialValue: iv,
            })
          } else if (next === RbForm.NEXT_VIEW && window.RbViewModal) {
            window.RbViewModal.create({ id: recordId, entity: this.state.entity })
          } else if (previewid && window.RbViewPage) {
            window.RbViewPage.clickView(`!#/View/${this.state.entity}/${recordId}`)
          }

          RbForm.postAfter({ ...res.data, isNew: !this.state.id }, next)

          // ...
        }, 200)
      } else if (res.error_code === 499) {
        renderRbcomp(<RepeatedViewer entity={this.state.entity} data={res.data} />)
      } else if (res.error_code === 497) {
        const that = this
        RbAlert.create(res.error_msg, {
          onConfirm: function () {
            this.hide()
            that._post(next, true)
          },
        })
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
    return true
  }

  // -- HOOK

  // 保存前调用（返回 false 则不继续保存）
  static postBefore(data) {
    if (window.FrontJS) {
      const ret = window.FrontJS.Form._trigger('saveBefore', [data])
      if (ret === false) return false
    }
    return true
  }

  // 保存后调用
  static postAfter(data, next) {
    if (window.FrontJS) {
      window.FrontJS.Form._trigger('saveAfter', [data, next])
    }

    // TODO 本实体才刷新?

    // 刷新列表
    const rlp = window.RbListPage || parent.RbListPage
    if (rlp) rlp.reload(data.id)
    // 刷新视图
    if (window.RbViewPage && next !== RbForm.NEXT_ADDDETAIL) window.RbViewPage.reload()
  }

  // 渲染后调用
  static renderAfter(form) {
    console.log('renderAfter ...', form)
  }
}

// 表单元素基础类
class RbFormElement extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    const props = this.props
    const state = this.state

    let colspan = 6 // default
    if (props.colspan === 4 || props.isFull === true) colspan = 12
    else if (props.colspan === 1) colspan = 3
    else if (props.colspan === 3) colspan = 9
    else if (props.colspan === 9) colspan = 4
    else if (props.colspan === 8) colspan = 8

    const editable = props.$$$parent.onViewEditable && props.onView && !props.readonly

    return (
      <div className={`col-12 col-sm-${colspan} form-group type-${props.type} ${editable ? 'editable' : ''}`} data-field={props.field}>
        <label ref={(c) => (this._fieldLabel = c)} className={`col-form-label ${!props.onView && !props.nullable ? 'required' : ''}`}>
          {props.label}
        </label>
        <div ref={(c) => (this._fieldText = c)} className="col-form-control">
          {!props.onView || (editable && state.editMode) ? this.renderElement() : this.renderViewElement()}
          {!props.onView && state.tip && <p className={`form-text ${state.tipForce && 'form-text-force'}`}>{state.tip}</p>}

          {editable && !state.editMode && <a className="edit" title={$L('编辑')} onClick={() => this.toggleEditMode(true)} />}
          {editable && state.editMode && (
            <div className="edit-oper">
              <div className="btn-group shadow-sm">
                <button type="button" className="btn btn-secondary" onClick={() => this.handleEditConfirm()}>
                  <i className="icon zmdi zmdi-check" />
                </button>
                <button type="button" className="btn btn-secondary" onClick={() => this.toggleEditMode(false)}>
                  <i className="icon zmdi zmdi-close" />
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
      if (!props.nullable && $empty(props.value) && props.readonlyw !== 2) {
        props.$$$parent.setFieldValue(props.field, null, $L('%s 不能为空', props.label))
      }
      // props.tip && $(this._fieldLabel).find('i.zmdi').tooltip({ placement: 'right' })

      this.onEditModeChanged()
    }
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
        onChange={(e) => this.handleChange(e, this.props.readonly ? false : true)}
        // onBlur={this.props.readonly ? null : () => this.checkValue()}
        readOnly={this.props.readonly}
        placeholder={this.props.readonlyw > 0 ? $L('自动值') : null}
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

    return <div className="form-control-plaintext">{value || <span className="text-muted">{$L('无')}</span>}</div>
  }

  /**
   * 修改值（表单组件（字段）值变化应调用此方法）
   *
   * @param {Event} e
   * @param {Boolean} checkValue
   */
  handleChange(e, checkValue) {
    const val = e.target.value
    this.setState({ value: val }, () => {
      checkValue === true && this.checkValue()
      typeof this.props.onValueChange === 'function' && typeof this.props.onValueChange(this)
    })
  }

  /**
   * 清空值
   */
  handleClear() {
    this.setState({ value: '' }, () => {
      this.checkValue()
      typeof this.props.onValueChange === 'function' && typeof this.props.onValueChange(this)
    })
  }

  /**
   * 检查值
   */
  checkValue() {
    const err = this.isValueError()
    this.setState({ hasError: err || null })
    const errMsg = err ? this.props.label + err : null

    if (this.isValueUnchanged() && !this.props.$$$parent.isNew) {
      if (err) this.props.$$$parent.setFieldValue(this.props.field, this.state.value, errMsg)
      else this.props.$$$parent.setFieldUnchanged(this.props.field, this.state.value)
    } else {
      this.props.$$$parent.setFieldValue(this.props.field, this.state.value, errMsg)
    }
  }

  /**
   * 无效值检查
   */
  isValueError() {
    if (this.props.nullable === false) {
      return $empty(this.state.value) ? $L('不能为空') : null
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
   * 编辑模式
   *
   * @param {Boolean} destroy
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
   * @param {Boolean} editMode
   */
  toggleEditMode(editMode) {
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
  // @return string or object
  getValue() {
    return this.state.value
  }
}

class RbFormText extends RbFormElement {
  renderElement() {
    const comp = super.renderElement()
    if (this.props.readonly || !this.props.textCommon) return comp

    // FIXME `常用`有明细遮挡问题，dropdown-menu 需要脱离到 body 中
    return (
      <RF>
        {React.cloneElement(comp, { 'data-toggle': 'dropdown' })}
        <div className="dropdown-menu common-texts">
          <h5>{$L('常用')}</h5>
          {this.props.textCommon.split(',').map((item) => {
            return (
              <a
                key={item}
                title={item}
                className="badge text-ellipsis"
                onClick={() => {
                  // $(this._fieldValue).val(item)
                  this.handleChange({ target: { value: item } }, true)
                }}>
                {item}
              </a>
            )
          })}
        </div>
      </RF>
    )
  }
}

class RbFormUrl extends RbFormText {
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
  isValueError() {
    const err = super.isValueError()
    if (err) return err

    const value = this._removeComma(this.state.value)
    if (!!value && $regex.isNumber(value) === false) return $L('格式不正确')
    if (!!value && $isTrue(this.props.notNegative) && parseFloat(value) < 0) return $L('不能为负数')
    return null
  }
  _isValueError() {
    return super.isValueError()
  }

  renderElement() {
    const value = arguments.length > 0 ? arguments[0] : this.state.value
    return (
      <RF>
        <input
          ref={(c) => (this._fieldValue = c)}
          className={`form-control form-control-sm ${this.state.hasError ? 'is-invalid' : ''}`}
          title={this.state.hasError}
          type="text"
          value={this._removeComma(value)}
          onChange={(e) => this.handleChange(e, !this.props.readonly)}
          // onBlur={this.props.readonly ? null : () => this.checkValue()}
          readOnly={this.props.readonly}
          placeholder={this.props.readonlyw > 0 ? $L('自动值') : null}
          maxLength="29"
        />
        {this.__valueFlag && <em className="vflag">{this.__valueFlag}</em>}
      </RF>
    )
  }

  renderViewElement() {
    const c = super.renderViewElement()
    // 负数
    if (this.state.value && (this.state.value + '').includes('-')) {
      return React.cloneElement(c, { className: 'form-control-plaintext text-danger' })
    } else {
      return c
    }
  }

  componentDidMount() {
    super.componentDidMount()

    // 表单计算（视图下无效）
    if (this.props.calcFormula && !this.props.onView) {
      const calcFormula = this.props.calcFormula.replace(new RegExp('×', 'ig'), '*').replace(new RegExp('÷', 'ig'), '/')
      const fixed = this.props.decimalFormat ? (this.props.decimalFormat.split('.')[1] || '').length : 0

      // 等待字段初始化完毕
      setTimeout(() => {
        const calcFormulaValues = {}
        const watchFields = calcFormula.match(/\{([a-z0-9]+)\}/gi) || []

        watchFields.forEach((item) => {
          const name = item.substr(1, item.length - 2)
          const fieldComp = this.props.$$$parent.refs[`fieldcomp-${name}`]
          if (fieldComp && !$emptyNum(fieldComp.state.value)) {
            calcFormulaValues[name] = this._removeComma(fieldComp.state.value)
          }
        })

        // 表单计算
        // TODO 考虑异步延迟执行
        this.props.$$$parent.onFieldValueChange((s) => {
          if (!watchFields.includes(`{${s.name}}`)) {
            if (rb.env === 'dev') console.log('onFieldValueChange :', s)
            return false
          } else if (rb.env === 'dev') {
            console.log('onFieldValueChange for calcFormula :', s)
          }

          // fix: 3.2 字段相互使用导致死循环
          this.__fixUpdateDepth = (this.__fixUpdateDepth || 0) + 1
          if (this.__fixUpdateDepth > 9) {
            console.log(`Maximum update depth exceeded : ${this.props.field}=${this.props.calcFormula}`)
            setTimeout(() => (this.__fixUpdateDepth = 0), 100)
            return false
          }

          if ($emptyNum(s.value)) {
            delete calcFormulaValues[s.name]
          } else {
            calcFormulaValues[s.name] = this._removeComma(s.value)
          }

          let formula = calcFormula
          for (let key in calcFormulaValues) {
            formula = formula.replace(new RegExp(`{${key}}`, 'ig'), calcFormulaValues[key] || 0)
          }

          // 还有变量无值
          if (formula.includes('{')) {
            this.setValue(null)
            return false
          }

          try {
            let calcv = null
            eval(`calcv = ${formula}`)
            if (!isNaN(calcv)) this.setValue(calcv.toFixed(fixed))
          } catch (err) {
            if (rb.env === 'dev') console.log(err)
          }
          return true
        })
      }, 200)
    }
  }

  // 移除千分为位
  _removeComma(n) {
    if (n === null || n === undefined) return ''
    // if (n) return (n + '').replace(/,/g, '')
    // debugger
    if (n) n = $regex.clearNumber(n)
    if (n === '-') return n
    if (isNaN(n)) return ''
    return n // `0`
  }
}

class RbFormDecimal extends RbFormNumber {
  constructor(props) {
    super(props)
    // 0, %, etc.
    if (props.decimalType && props.decimalType !== '0') {
      this.__valueFlag = props.decimalType
    }
  }

  isValueError() {
    const err = super._isValueError()
    if (err) return err

    const value = this._removeComma(this.state.value)
    if (!!value && $regex.isDecimal(value) === false) return $L('格式不正确')
    if (!!value && $isTrue(this.props.notNegative) && parseFloat(value) < 0) return $L('不能为负数')
    return null
  }
}

class RbFormTextarea extends RbFormElement {
  constructor(props) {
    super(props)

    this._height = this.props.useMdedit ? 0 : ~~this.props.height
    if (this._height && this._height > 0) {
      if (this._height === 1) this._height = 37
      else this._height = this._height * 20 + 12
    }
  }

  renderElement() {
    return (
      <React.Fragment>
        <textarea
          ref={(c) => {
            this._fieldValue = c
            this._height > 0 && c && $(c).attr('style', `height:${this._height}px !important`)
          }}
          className={`form-control form-control-sm row3x ${this.state.hasError ? 'is-invalid' : ''} ${this.props.useMdedit && this.props.readonly ? 'cm-readonly' : ''}`}
          title={this.state.hasError}
          value={this.state.value || ''}
          onChange={(e) => this.handleChange(e, !this.props.readonly)}
          // onBlur={this.props.readonly ? null : () => this.checkValue()}
          readOnly={this.props.readonly}
          placeholder={this.props.readonlyw > 0 ? $L('自动值') : null}
          maxLength="6000"
        />
        {this.props.useMdedit && !this.props.readonly && <input type="file" className="hide" accept="image/*" ref={(c) => (this._fieldValue__upload = c)} />}
      </React.Fragment>
    )
  }

  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()

    const style = {}
    if (this._height > 0) style.maxHeight = this._height

    if (this.props.useMdedit) {
      return (
        <div className="form-control-plaintext mdedit-content" ref={(c) => (this._textarea = c)} style={style}>
          <Md2Html markdown={this.state.value} />
        </div>
      )
    } else {
      return (
        <div className="form-control-plaintext" ref={(c) => (this._textarea = c)} style={style}>
          {this.state.value.split('\n').map((line, idx) => {
            return <p key={`line-${idx}`}>{line}</p>
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
      toolbar: this.props.readonly ? false : DEFAULT_MDE_TOOLBAR(this),
    })
    this._simplemde = mde

    if (this.props.readonly) {
      mde.codemirror.setOption('readOnly', true)
    } else {
      $createUploader(this._fieldValue__upload, null, (res) => {
        const pos = mde.codemirror.getCursor()
        mde.codemirror.setSelection(pos, pos)
        mde.codemirror.replaceSelection(`![${$L('图片')}](${rb.baseUrl}/filex/img/${res.key})`)
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
            this.setState({ value: mde.value() }, () => this.checkValue())
          },
          200,
          'mde-update-event'
        )
      })
    }
  }
}

class RbFormDateTime extends RbFormElement {
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
          onChange={(e) => this.handleChange(e, !this.props.readonly)}
          // onBlur={this.props.readonly ? null : () => this.checkValue()}
          placeholder={this.props.readonlyw > 0 ? $L('自动值') : null}
          maxLength="20"
        />
        <span className={'zmdi zmdi-close clean ' + (this.state.value ? '' : 'hide')} onClick={() => this.handleClear()} />
        <div className="input-group-append">
          <button className="btn btn-secondary" type="button" ref={(c) => (this._fieldValue__icon = c)}>
            <i className={`icon zmdi zmdi-${this._icon || 'calendar'}`} />
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
          pickerPosition: this._getAutoPosition(),
        })
        .on('changeDate', function () {
          const val = $(this).val()
          that.handleChange({ target: { value: val } }, true)
        })

      $(this._fieldValue__icon).on('click', () => this.__datetimepicker.datetimepicker('show'))
    }
  }

  // https://github.com/smalot/bootstrap-datetimepicker/pull/645
  _getAutoPosition() {
    const wh = $(document.body).height() || 9999,
      wt = $(this._fieldValue).offset().top
    return wt + 280 < wh ? 'bottom-right' : 'top-right'
  }
}

class RbFormTime extends RbFormDateTime {
  constructor(props) {
    super(props)
    this._icon = 'time'
  }

  onEditModeChanged(destroy) {
    if (destroy) {
      super.onEditModeChanged(destroy)
    } else if (!this.props.readonly) {
      const format = (this.props.timeFormat || 'hh:ii:ss').replace('mm', 'ii').toLowerCase()
      const minView = format.length === 2 ? 1 : 0

      const that = this
      this.__datetimepicker = $(this._fieldValue)
        .datetimepicker({
          format: format,
          startView: 1,
          minView: minView,
          maxView: 1,
          pickerPosition: this._getAutoPosition(),
          title: $L('选择时间'),
        })
        .on('changeDate', function () {
          const val = $(this).val()
          that.handleChange({ target: { value: val } }, true)
        })

      $(this._fieldValue__icon).on('click', () => this.__datetimepicker.datetimepicker('show'))
    }
  }
}

class RbFormImage extends RbFormElement {
  constructor(props) {
    super(props)
    this._htmlid = `${props.field}-${$random()}-input`

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
    const showUpload = value.length < this.__maxUpload && !this.props.readonly && !this.props.imageCapture

    if (value.length === 0) {
      if (this.props.readonly) {
        return (
          <div className="form-control-plaintext text-muted">
            <i className="mdi mdi-information-outline" /> {$L('只读')}
          </div>
        )
      } else if (this.props.imageCapture) {
        return (
          <div className="form-control-plaintext text-muted">
            <i className="mdi mdi-information-outline" /> {$L('仅允许拍照上传')}
          </div>
        )
      }
    }

    return (
      <div className="img-field">
        {value.map((item) => {
          return (
            <span key={item}>
              <a title={$fileCutName(item)} className="img-thumbnail img-upload">
                <img src={this._formatUrl(item)} alt="IMG" />
                {!this.props.readonly && (
                  <b title={$L('移除')} onClick={() => this.removeItem(item)}>
                    <span className="zmdi zmdi-close" />
                  </b>
                )}
              </a>
            </span>
          )
        })}
        <span title={$L('上传图片。需要 %s 个', `${this.__minUpload}~${this.__maxUpload}`)} className={showUpload ? '' : 'hide'}>
          <input ref={(c) => (this._fieldValue__input = c)} type="file" className="inputfile" id={this._htmlid} accept="image/*" />
          <label htmlFor={this._htmlid} className="img-thumbnail img-upload">
            <span className="zmdi zmdi-image-alt mt-1" />
          </label>
        </span>
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
            <span key={item}>
              <a title={$fileCutName(item)} onClick={() => (parent || window).RbPreview.create(value, idx)} className="img-thumbnail img-upload zoom-in">
                <img src={this._formatUrl(item)} alt="IMG" />
              </a>
            </span>
          )
        })}
      </div>
    )
  }

  _formatUrl(urlKey) {
    if (urlKey.startsWith('http://') || urlKey.startsWith('https://')) return urlKey
    else return `${rb.baseUrl}/filex/img/${urlKey}?imageView2/2/w/100/interlace/1/q/100`
  }

  onEditModeChanged(destroy) {
    if (destroy) {
      // NOOP
    } else {
      // Mobile camera only
      if (this.props.imageCapture === true) {
        return
      } else if (!this._fieldValue__input) {
        console.warn('No element `_fieldValue__input` defined')
        return
      }

      let mp
      const mp_end = function () {
        setTimeout(() => {
          if (mp) mp.end()
          mp = null
        }, 510)
      }

      $createUploader(
        this._fieldValue__input,
        (res) => {
          if (!mp) mp = new Mprogress({ template: 2, start: true })
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
  renderElement() {
    const value = this.state.value || []
    const showUpload = value.length < this.__maxUpload && !this.props.readonly

    if (value.length === 0 && this.props.readonly) {
      return (
        <div className="form-control-plaintext text-muted">
          <i className="mdi mdi-information-outline" /> {$L('只读')}
        </div>
      )
    }

    return (
      <div className="file-field">
        {value.map((item) => {
          let fileName = $fileCutName(item)
          return (
            <div key={item} className="img-thumbnail" title={fileName}>
              <i className="file-icon" data-type={$fileExtName(fileName)} />
              <span>{fileName}</span>
              {!this.props.readonly && (
                <b title={$L('移除')} onClick={() => this.removeItem(item)}>
                  <span className="zmdi zmdi-close" />
                </b>
              )}
            </div>
          )
        })}
        <div className={`file-select ${showUpload ? '' : 'hide'}`}>
          <input type="file" className="inputfile" ref={(c) => (this._fieldValue__input = c)} id={this._htmlid} accept={this.props.fileSuffix || null} />
          <label htmlFor={this._htmlid} title={$L('上传文件。需要 %d 个', `${this.__minUpload}~${this.__maxUpload}`)} className="btn-secondary">
            <i className="zmdi zmdi-upload" />
            <span>{$L('上传文件')}</span>
          </label>
        </div>
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
            <a key={item} title={fileName} onClick={() => (parent || window).RbPreview.create(item)} className="img-thumbnail">
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

    const options = [...props.options]
    if (props.value) {
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
      }
    }
    this._options = options
  }

  renderElement() {
    // if ((this.state.options || []).length === 0) {
    //   return <div className="form-control-plaintext text-danger">{$L('未配置')}</div>
    // }

    const keyName = `${this.state.field}-option-`
    return (
      <select ref={(c) => (this._fieldValue = c)} className="form-control form-control-sm" defaultValue={this.state.value || ''}>
        <option value="" />
        {this._options.map((item) => {
          return (
            <option key={`${keyName}${item.id}`} value={item.id} disabled={$isSysMask(item.text)}>
              {item.text}
            </option>
          )
        })}
      </select>
    )
  }

  renderViewElement() {
    return super.renderViewElement(__findOptionText(this.state.options, this.state.value, true))
  }

  onEditModeChanged(destroy) {
    if (destroy) {
      super.onEditModeChanged(destroy)
    } else {
      this.__select2 = $(this._fieldValue).select2({
        placeholder: $L('选择%s', this.props.label),
      })

      const that = this
      this.__select2.on('change', function (e) {
        const val = e.target.value
        that.handleChange({ target: { value: val } }, true)
      })

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
    this._hasDataFilter = props.referenceDataFilter && (props.referenceDataFilter.items || []).length > 0
  }

  renderElement() {
    const quickNew = this.props.referenceQuickNew && !this.props.onView

    return (
      <div className="input-group has-append">
        <select ref={(c) => (this._fieldValue = c)} className="form-control form-control-sm" title={this._hasDataFilter ? $L('当前字段已启用数据过滤') : null} multiple={this._multiple === true} />
        {!this.props.readonly && (
          <div className="input-group-append">
            <button className="btn btn-secondary" type="button" onClick={() => this.showSearcher()}>
              <i className="icon zmdi zmdi-search" />
            </button>
            {quickNew && (
              <button className="btn btn-secondary" type="button" onClick={() => this.quickNew()} title={$L('新建')}>
                <i className="icon zmdi zmdi-plus" />
              </button>
            )}
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
    if (props.$$$parent.isNew && props.value && props.value.id) {
      setTimeout(() => this.triggerAutoFillin(props.value.id), 500)
    }
  }

  onEditModeChanged(destroy) {
    if (destroy) {
      super.onEditModeChanged(destroy)
    } else {
      this.__select2 = $initReferenceSelect2(this._fieldValue, {
        name: this.props.field,
        label: this.props.label,
        entity: this.props.$$$parent.props.entity,
        wrapQuery: (query) => {
          const cascadingValue = this._getCascadingFieldValue()
          return cascadingValue ? { cascadingValue, ...query } : query
        },
      })

      const val = this.state.value
      if (val) this.setValue(val)

      const that = this
      this.__select2.on('change', function (e) {
        const v = $(e.target).val()
        if (v && typeof v === 'string') {
          __addRecentlyUse(v)
          that.triggerAutoFillin(v)

          // v2.10 FIXME 父级改变后清除明细
          // v3.1 因为父级无法获取到明细的级联值，且级联值有多个（逻辑上存在多个父级值）
          const $$$form = that.props.$$$parent
          if ($$$form._ProTable && !$$$form._inAutoFillin && (that.props._cascadingFieldChild || '').includes('.')) {
            const field = that.props._cascadingFieldChild.split('$$$$')[0].split('.')[1]
            $$$form._ProTable.setFieldNull(field)
            console.log('Clean details ...', field)
          }
        }

        that.handleChange({ target: { value: v } }, true)
      })

      if (this.props.readonly) $(this._fieldValue).attr('disabled', true)
    }
  }

  componentWillUnmount() {
    super.componentWillUnmount()

    if (this._ReferenceSearcher) {
      this._ReferenceSearcher.destroy()
      this._ReferenceSearcher = null
    }
  }

  _getCascadingFieldValue() {
    if (typeof this.props.getCascadingFieldValue === 'function') {
      return this.props.getCascadingFieldValue(this)
    }

    let cascadingField
    if (this.props._cascadingFieldParent) {
      cascadingField = this.props._cascadingFieldParent.split('$$$$')[0]
    } else if (this.props._cascadingFieldChild) {
      cascadingField = this.props._cascadingFieldChild.split('$$$$')[0]
    }
    if (!cascadingField) return null

    let $$$parent = this.props.$$$parent

    // v2.10 明细中使用主表单
    if ($$$parent._InlineForm && (this.props._cascadingFieldParent || '').includes('.')) {
      $$$parent = $$$parent.props.$$$main
      cascadingField = cascadingField.split('.')[1]
    }

    let v
    if (this.props.onView) {
      v = ($$$parent.__ViewData || {})[cascadingField]

      // v2.10 无值时使用后台值
      if (!v && this.props._cascadingFieldParentValue) {
        v = { id: this.props._cascadingFieldParentValue }
      }
    } else {
      const fieldComp = $$$parent.refs[`fieldcomp-${cascadingField}`]
      v = fieldComp ? fieldComp.getValue() : null

      // v2.10 无布局时使用后台值
      if (!fieldComp && this.props._cascadingFieldParentValue) {
        v = { id: this.props._cascadingFieldParentValue }
      }
    }

    if (v && $.isArray(v)) v = v[0] // N2N
    return v ? v.id || v : null
  }

  // 字段回填
  triggerAutoFillin(value) {
    if (this.props.onView) return

    const $$$form = this.props.$$$parent
    const url = `/app/entity/extras/fillin-value?entity=${$$$form.props.entity}&field=${this.props.field}&source=${value}`
    $.get(url, (res) => {
      if (res.error_code === 0 && res.data.length > 0) {
        const fillin2main = []
        const fillin2this = []
        res.data.forEach((item) => {
          if (item.target.includes('.')) {
            fillin2main.push({ ...item, target: item.target.split('.')[1] })
          } else {
            fillin2this.push(item)
          }
        })

        if (fillin2this.length > 0) {
          $$$form.setAutoFillin(fillin2this)
        }
        // 明细 > 主记录
        if (fillin2main.length > 0 && $$$form._InlineForm) {
          const $$$formMain = $$$form.props.$$$main
          $$$formMain && $$$formMain.setAutoFillin(fillin2main)
        }
      }
    })
  }

  isValueUnchanged() {
    const oldv = this.state.newValue === undefined ? (this.props.value || {}).id : (this.state.newValue || {}).id
    return $same(oldv, this.state.value)
  }

  setValue(val) {
    if (val) {
      const o = new Option(val.text, val.id, true, true)
      this.__select2.append(o).trigger('change')
    } else {
      this.__select2.val(null).trigger('change')
    }
  }

  showSearcher() {
    const that = this
    window.referenceSearch__call = function (selected) {
      that.showSearcher_call(selected, that)
      that._ReferenceSearcher.hide()
    }

    const url = `${rb.baseUrl}/commons/search/reference-search?field=${this.props.field}.${this.props.$$$parent.props.entity}&cascadingValue=${this._getCascadingFieldValue() || ''}`
    if (!this._ReferenceSearcher_Url) this._ReferenceSearcher_Url = url

    if (this._ReferenceSearcher && this._ReferenceSearcher_Url === url) {
      this._ReferenceSearcher.show()
    } else {
      if (this._ReferenceSearcher) {
        this._ReferenceSearcher.destroy()
        this._ReferenceSearcher = null
      }
      this._ReferenceSearcher_Url = url

      // eslint-disable-next-line react/jsx-no-undef
      renderRbcomp(<ReferenceSearcher url={url} title={$L('选择%s', this.props.label)} />, function () {
        that._ReferenceSearcher = this
      })
    }
  }

  showSearcher_call(selected, that) {
    const id = selected[0]
    if ($(that._fieldValue).find(`option[value="${id}"]`).length > 0) {
      that.__select2.val(id).trigger('change')
    } else {
      $.get(`/commons/search/read-labels?ids=${id}`, (res) => {
        const o = new Option(res.data[id], id, true, true)
        that.__select2.append(o).trigger('change')
      })
    }
  }

  quickNew() {
    const e = this.props.referenceEntity
    RbFormModal.create(
      {
        title: $L('新建%s', e.entityLabel),
        entity: e.entity,
        icon: e.icon,
        postAfter: (id) => this.showSearcher_call([id], this),
      },
      true
    )
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
      <div className="form-control-plaintext multi-values">
        {value.map((item) => {
          return (
            <a key={item.id} className="hover-color" href={`#!/View/${item.entity}/${item.id}`} onClick={this._clickView}>
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

  // @append = 追加模式
  setValue(val, append) {
    if (val && val.length > 0) {
      let currentIds = this.state.value || '' // init is Object

      if (!append) {
        this.__select2.val(null).trigger('change')
        currentIds = ''
      }

      const ids = []
      val.forEach((item) => {
        if (!currentIds.includes(item.id)) {
          const o = new Option(item.text, item.id, true, true)
          this.__select2.append(o)
          ids.push(item.id)
        }
      })

      if (ids.length > 0) {
        let ss = ids.join(',')
        if (append && currentIds && currentIds !== '') ss = currentIds + ',' + ss
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
    __addRecentlyUse(ids)

    // v3.1 回填父级
    if (selected[0] && this.props._cascadingFieldParent) {
      this.triggerAutoFillin(selected[0])
    }
  }

  onEditModeChanged(destroy) {
    super.onEditModeChanged(destroy)
    if (!destroy && this.__select2) {
      this.__select2.on('select2:select', (e) => __addRecentlyUse(e.params.data.id))
    }
  }
}

// TODO 任意引用（不支持手动编辑）
class RbFormAnyReference extends RbFormReference {}

class RbFormClassification extends RbFormElement {
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
        if (v) __addRecentlyUse(`${v}&type=d${this.props.classification}:${this.props.openLevel}`)
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
        <ClassificationSelector entity={p.$$$parent.state.entity} field={p.field} label={p.label} openLevel={p.openLevel} onSelect={(s) => this._setClassificationValue(s)} keepModalOpen={true} />,
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
  renderElement() {
    if ((this.props.options || []).length === 0) {
      return <div className="form-control-plaintext text-danger">{$L('未配置')}</div>
    }

    const maskValue = this._getMaskValue()
    return (
      <div className="mt-1" ref={(c) => (this._fieldValue__wrap = c)}>
        {(this.props.options || []).map((item) => {
          return (
            <label key={`mask-${item.mask}`} className="custom-control custom-checkbox custom-control-inline">
              <input
                className="custom-control-input"
                name={`checkbox-${this.props.field}`}
                type="checkbox"
                checked={(maskValue & item.mask) !== 0}
                value={item.mask}
                onChange={this.changeValue}
                disabled={this.props.readonly || $isSysMask(item.text)}
              />
              <span className="custom-control-label">{item.text}</span>
            </label>
          )
        })}
      </div>
    )
  }

  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()

    const maskValue = this._getMaskValue()
    return <div className="form-control-plaintext multi-values">{__findMultiTexts(this.props.options, maskValue, true)}</div>
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

  _getMaskValue() {
    const value = this.state.value
    if (!value) return 0
    return typeof value === 'object' ? value.id : value
  }
}

class RbFormBool extends RbFormElement {
  _Options = {
    T: $L('是'),
    F: $L('否'),
  }

  constructor(props) {
    super(props)
    this._htmlid = `${props.field}-${$random()}_`
  }

  renderElement() {
    return (
      <div className="mt-1">
        <label className="custom-control custom-radio custom-control-inline mb-1">
          <input className="custom-control-input" name={`${this._htmlid}T`} type="radio" checked={this.state.value === 'T'} data-value="T" onChange={this.changeValue} disabled={this.props.readonly} />
          <span className="custom-control-label">{this._Options['T']}</span>
        </label>
        <label className="custom-control custom-radio custom-control-inline mb-1">
          <input className="custom-control-input" name={`${this._htmlid}F`} type="radio" checked={this.state.value === 'F'} data-value="F" onChange={this.changeValue} disabled={this.props.readonly} />
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

class RbFormState extends RbFormPickList {}

class RbFormBarcode extends RbFormElement {
  renderElement() {
    if (this.state.value) return this.renderViewElement()

    return (
      <div className="form-control-plaintext barcode text-muted">
        {$L('自动值')} ({this.props.barcodeType === 'BARCODE' ? $L('条形码') : $L('二维码')})
      </div>
    )
  }

  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()

    const isbar = this.props.barcodeType === 'BARCODE'
    const codeUrl = `${rb.baseUrl}/commons/barcode/render${isbar ? '' : '-qr'}?t=${$encode(this.state.value)}`
    return (
      <div className="img-field barcode">
        <a className={`img-thumbnail ${isbar && 'w-auto'}`} title={this.state.value}>
          <img src={codeUrl} alt={this.state.value} />
        </a>
      </div>
    )
  }
}

class RbFormAvatar extends RbFormElement {
  constructor(props) {
    super(props)
    this._htmlid = `${props.field}-${$random()}-input`
  }

  renderElement() {
    const readonly = this.props.readonly
    return (
      <div className="img-field avatar">
        <span title={this.props.readonly ? null : $L('选择头像')}>
          {!readonly && <input ref={(c) => (this._fieldValue__input = c)} type="file" className="inputfile" id={this._htmlid} accept="image/*" />}
          <label htmlFor={this._htmlid} className="img-thumbnail img-upload" disabled={readonly}>
            <img src={this._formatUrl(this.state.value)} alt="Avatar" />
            {!readonly && this.state.value && (
              <b
                title={$L('移除')}
                onClick={(e) => {
                  $stopEvent(e, true)
                  this.handleClear()
                }}>
                <span className="zmdi zmdi-close" />
              </b>
            )}
          </label>
        </span>
      </div>
    )
  }

  renderViewElement() {
    return (
      <div className="img-field avatar">
        <a className="img-thumbnail img-upload">
          <img src={this._formatUrl(this.state.value)} alt="Avatar" />
        </a>
      </div>
    )
  }

  _formatUrl(urlKey) {
    if (urlKey && (urlKey.startsWith('http://') || urlKey.startsWith('https://'))) return urlKey
    else return rb.baseUrl + (urlKey ? `/filex/img/${urlKey}?imageView2/2/w/100/interlace/1/q/100` : '/assets/img/avatar.png')
  }

  onEditModeChanged(destroy) {
    if (destroy) {
      // NOOP
    } else {
      let mp
      const mp_end = function () {
        setTimeout(() => {
          if (mp) mp.end()
          mp = null
        }, 510)
      }

      $createUploader(
        this._fieldValue__input,
        (res) => {
          if (!mp) mp = new Mprogress({ template: 2, start: true })
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

class RbFormLocation extends RbFormElement {
  renderElement() {
    const lnglat = this._parseLnglat(this.state.value)
    if (this.props.readonly) return super.renderElement(lnglat ? lnglat.text : null)

    return (
      <div className="input-group has-append">
        <input
          type="text"
          ref={(c) => (this._fieldValue = c)}
          className={`form-control form-control-sm bg-white ${this.state.hasError ? 'is-invalid' : ''}`}
          title={this.state.hasError}
          value={lnglat ? lnglat.text || '' : ''}
          onChange={(e) => this.handleChange(e)}
          readOnly
          placeholder={this.props.readonlyw > 0 ? $L('自动值') : null}
          onClick={() => this._showMap(lnglat)}
        />
        <span className={`zmdi zmdi-close clean ${this.state.value ? '' : 'hide'}`} onClick={() => this.handleClear()} title={$L('清除')} />
        <div className="input-group-append">
          <button className="btn btn-secondary" type="button" onClick={() => this._showMap(lnglat)}>
            <i className="icon zmdi zmdi-pin-drop flash infinite slow" ref={(c) => (this._$icon = c)} />
          </button>
        </div>
      </div>
    )
  }

  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()

    const lnglat = this._parseLnglat(this.state.value)
    return this.props.locationMapOnView ? (
      <div>
        <div className="form-control-plaintext">{lnglat.text}</div>
        <div className="map-show">
          <BaiduMap lnglat={lnglat} ref={(c) => (this._BaiduMap = c)} disableScrollWheelZoom />
        </div>
      </div>
    ) : (
      <div className="form-control-plaintext">
        <a
          href={`#!/Map:${lnglat.lng || ''}:${lnglat.lat || ''}`}
          onClick={(e) => {
            $stopEvent(e, true)
            BaiduMapModal.view(lnglat)
          }}>
          {lnglat.text}
        </a>
      </div>
    )
  }

  _parseLnglat(value) {
    if (!value) return null
    if (typeof value === 'object') return value

    const vals = value.split('$$$$')
    const lnglat = vals[1] ? vals[1].split(',') : null // 无坐标
    return {
      text: vals[0],
      lng: lnglat ? lnglat[0] : null,
      lat: lnglat ? lnglat[1] : null,
    }
  }

  _showMap(lnglat) {
    if (this._BaiduMapModal) {
      this._BaiduMapModal.show()
    } else {
      const that = this
      renderRbcomp(
        <BaiduMapModal
          canPin
          lnglat={lnglat}
          title={$L('选取位置')}
          onConfirm={(lnglat) => {
            const val = lnglat && lnglat.text ? `${lnglat.text}$$$$${lnglat.lng},${lnglat.lat}` : null
            that.handleChange({ target: { value: val } }, true)
          }}
        />,
        null,
        function () {
          that._BaiduMapModal = this
        }
      )
    }
  }

  onEditModeChanged(destroy) {
    if (destroy) {
      // Auto destroy by BaiduMap#componentWillUnmount
    }
  }

  componentDidMount() {
    super.componentDidMount()

    const props = this.props
    if (props.locationAutoLocation && props.$$$parent.isNew && !props.value) {
      $(this._$icon).addClass('animated')
      // eslint-disable-next-line no-undef
      $autoLocation((v) => {
        $(this._$icon).removeClass('animated')
        v = v && v.text ? `${v.text}$$$$${v.lng},${v.lat}` : null
        v && this.handleChange({ target: { value: v } }, true)
      })
    }
  }

  componentWillUnmount() {
    super.componentWillUnmount()
    if (this._BaiduMapModal) {
      this._BaiduMapModal.destroy()
      this._BaiduMapModal = null
    }
  }
}

class RbFormSign extends RbFormElement {
  renderElement() {
    const value = this.state.value

    return (
      <div className="img-field sign sign-edit">
        <span title={this.props.readonly ? null : $L('签名')}>
          <label
            className="img-thumbnail img-upload"
            onClick={() => {
              if (!this.props.readonly) {
                this._openSignPad((v) => {
                  this.handleChange({ target: { value: v || null } }, true)
                })
              }
            }}
            disabled={this.props.readonly}>
            {value ? <img src={value} alt="SIGN" /> : <span className="mdi mdi-file-sign" />}
          </label>
        </span>
      </div>
    )
  }

  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()

    return (
      <div className="img-field sign">
        <a className="img-thumbnail img-upload">
          <img src={this.state.value} alt="SIGN" />
        </a>
      </div>
    )
  }

  _openSignPad(onConfirm) {
    if (this._SignPad) {
      this._SignPad.show(true)
    } else {
      const that = this
      renderRbcomp(<SignPad onConfirm={onConfirm} />, null, function () {
        that._SignPad = this
      })
    }
  }
}

class RbFormTag extends RbFormElement {
  constructor(props) {
    super(props)

    this._initOptions()
    this.__maxSelect = props.tagMaxSelect || 20
  }

  renderElement() {
    const keyName = `${this.state.field}-tag-`
    return (
      <select ref={(c) => (this._fieldValue = c)} className="form-control form-control-sm" multiple defaultValue={this._selected}>
        {this._options.map((item) => {
          return (
            <option key={`${keyName}${item.name}`} value={item.name}>
              {item.name}
            </option>
          )
        })}
      </select>
    )
  }

  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()

    return <div className="form-control-plaintext multi-values">{__findTagTexts(this.props.options, this.state.value)}</div>
  }

  _initOptions() {
    const props = this.props

    let options = [...props.options]
    let selected = []
    if (props.$$$parent.isNew) {
      props.options.forEach((item) => {
        if (item.default) selected.push(item.name)
      })
    } else if (this.state.value) {
      let value = this.state.value
      if (typeof value === 'string') value = value.split('$$$$') // Save after

      value.forEach((name) => {
        selected.push(name)
        const found = props.options.find((x) => x.name === name)
        if (!found) options.push({ name: name })
      })
    }
    this._options = options
    this._selected = selected
  }

  onEditModeChanged(destroy) {
    if (destroy) {
      super.onEditModeChanged(destroy)
      this._initOptions()
    } else {
      this.__select2 = $(this._fieldValue).select2({
        placeholder: $L('输入%s', this.props.label),
        maximumSelectionLength: this.__maxSelect,
        tags: true,
        language: {
          noResults: function () {
            return $L('输入后回车')
          },
        },
        theme: 'default select2-tag',
      })

      const that = this
      this.__select2.on('change', function (e) {
        const mVal = $(e.currentTarget).val()
        that.handleChange({ target: { value: mVal.join('$$$$') } }, true)
      })

      if (this.props.readonly) $(this._fieldValue).attr('disabled', true)
    }
  }

  // isValueUnchanged() {}
}

// 不支持/未开放的字段
class RbFormUnsupportted extends RbFormElement {
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
    if (this.props.breaked === true) {
      return <div className="form-line-breaked"></div>
    }
    return (
      <div className="form-line hover" ref={(c) => (this._$formLine = c)}>
        <fieldset>
          {this.props.label && (
            <legend onClick={() => this.toggle()} className="text-bold" title={$L('展开/收起')}>
              {this.props.label}
            </legend>
          )}
        </fieldset>
      </div>
    )
  }

  toggle() {
    let $next = $(this._$formLine)
    while (($next = $next.next()).length > 0) {
      if ($next.hasClass('form-line') || $next.hasClass('footer')) break
      $next.toggleClass('hide')
    }
  }
}

// 确定元素类型
var detectElement = function (item, entity) {
  if (!item.key) item.key = `field-${item.field === TYPE_DIVIDER ? $random() : item.field}`

  if (entity && window._CustomizedForms) {
    const c = window._CustomizedForms.useFormElement(entity, item)
    if (c) return c
  }

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
  } else if (item.type === 'TIME') {
    return <RbFormTime {...item} />
  } else if (item.type === 'PICKLIST') {
    return <RbFormPickList {...item} />
  } else if (item.type === 'REFERENCE') {
    return <RbFormReference {...item} />
  } else if (item.type === 'N2NREFERENCE') {
    return <RbFormN2NReference {...item} />
  } else if (item.type === 'ANYREFERENCE') {
    return <RbFormAnyReference {...item} readonly />
  } else if (item.type === 'CLASSIFICATION') {
    return <RbFormClassification {...item} />
  } else if (item.type === 'MULTISELECT') {
    return <RbFormMultiSelect {...item} />
  } else if (item.type === 'BOOL') {
    return <RbFormBool {...item} />
  } else if (item.type === 'STATE') {
    return <RbFormState {...item} />
  } else if (item.type === 'BARCODE') {
    return <RbFormBarcode {...item} readonly />
  } else if (item.type === 'AVATAR') {
    return <RbFormAvatar {...item} />
  } else if (item.type === 'LOCATION') {
    return <RbFormLocation {...item} />
  } else if (item.type === 'SIGN') {
    return <RbFormSign {...item} />
  } else if (item.type === 'TAG') {
    return <RbFormTag {...item} />
  } else if (item.field === TYPE_DIVIDER || item.field === '$LINE$') {
    return <RbFormDivider {...item} />
  } else {
    return <RbFormUnsupportted {...item} />
  }
}

// 获取选项型字段显示值
const __findOptionText = function (options, value, useColor) {
  if ((options || []).length === 0 || !value) return null
  // eslint-disable-next-line eqeqeq
  const o = options.find((x) => x.id == value)

  let text = (o || {}).text || `[${value.toUpperCase()}]`
  if (useColor && o && o.color) {
    const style2 = { borderColor: o.color, backgroundColor: o.color, color: '#fff' }
    text = (
      <span className="badge" style={style2}>
        {text}
      </span>
    )
  }
  return text
}

// 多选文本
const __findMultiTexts = function (options, maskValue, useColor) {
  const texts = []
  options.map((o) => {
    if ((maskValue & o.mask) !== 0) {
      const style2 = o.color && useColor ? { borderColor: o.color, backgroundColor: o.color, color: '#fff' } : null
      const text = (
        <span key={`mask-${o.mask}`} style={style2}>
          {o.text}
        </span>
      )
      texts.push(text)
    }
  })
  return texts
}

// 标签文本
const __findTagTexts = function (options, value) {
  if (typeof value === 'string') value = value.split('$$$$')

  const texts = []
  value.map((name) => {
    let item = options.find((x) => x.name === name)
    if (!item) item = { name: name }

    const style2 = item.color ? { borderColor: item.color, color: item.color } : null
    const text = (
      <span key={`tag-${item.name}`} style={style2}>
        {item.name}
      </span>
    )
    texts.push(text)
  })
  return texts
}

// 最近使用
const __addRecentlyUse = function (id) {
  if (id && typeof id === 'string') {
    $.post(`/commons/search/recently-add?id=${id}`)
  }
}

// -- Lite

// eslint-disable-next-line no-unused-vars
class LiteForm extends RbForm {
  renderCustomizedFormArea() {
    return null
  }

  renderDetailForm() {
    return null
  }

  renderFormAction() {
    return null
  }

  componentDidMount() {
    super.componentDidMount()
    // TODO init...
  }

  buildFormData() {
    const s = {}
    const data = this.__FormData || {}
    for (let k in data) {
      const error = data[k].error
      if (error) {
        RbHighbar.create(error)
        return false
      }
      s[k] = data[k].value
    }
    s.metadata = { id: this.props.id || '' }
    return s
  }
}
