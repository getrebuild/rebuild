/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global EasyMDE, RepeatedViewer, ProTable, Md2Html, ClassificationSelector, autosize */

/**
 * Callback API:
 * - RbForm: onFieldValueChange( callback({name:xx,value:xx}) )
 * - RbFormElement: onValueChange(this)
 * - RbFormReference/RbFormN2NReference: getCascadingFieldValue(this)
 */

const TYPE_DIVIDER = '$DIVIDER$'
const TYPE_REFFORM = '$REFFORM$'
const MODAL_MAXWIDTH = 1064

// ~~ 表单窗口
class RbFormModal extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, inLoad: true, _maximize: false }

    this.__maximizeKey = 'FormMaximize-ANY'
    this.state._maximize = $isTrue($storage.get(this.__maximizeKey)) || window.__LAB_FORM_MAXIMIZE40

    if (!props.id) this.state.id = null
  }

  render() {
    const style2 = { maxWidth: this.props.width || MODAL_MAXWIDTH }
    if (this.state._maximize) style2.maxWidth = '100%'

    return (
      <div className="modal-wrapper">
        <div className="modal rbmodal colored-header colored-header-primary" aria-modal="true" tabIndex="-1" ref={(c) => (this._rbmodal = c)}>
          <div className={`modal-dialog ${window.__LAB_FORM_SCROLLABLE42 && 'modal-dialog-scrollable'} ${this.state._maximize && 'modal-dialog-maximize'}`} style={style2}>
            <div className="modal-content" style={style2}>
              <div
                className="modal-header modal-header-colored"
                onDoubleClick={(e) => {
                  $stopEvent(e, true)
                  this._handleMaximize()
                }}>
                {this.state.icon && <span className={`icon zmdi zmdi-${this.state.icon}`} />}
                <h3 className="modal-title">{this.state.title || $L('新建')}</h3>
                {rb.isAdminUser && (
                  <a className="close s" href={`${rb.baseUrl}/admin/entity/${this.state.entity}/form-design`} title={$L('表单设计')} target="_blank">
                    <span className="zmdi zmdi-settings up-1" />
                  </a>
                )}
                <button className="close md-close J_maximize" type="button" title={this.state._maximize ? $L('向下还原') : $L('最大化')} onClick={() => this._handleMaximize()}>
                  <span className="mdi mdi-window-maximize" />
                </button>
                <button className="close md-close" type="button" title={`${$L('关闭')} (Esc)`} onClick={() => this.hide()}>
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
                {this.state.fjsAlertMessage}

                {this.state.formComponent}
                {this.state.inLoad && <RbSpinner />}
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  _handleMaximize() {
    this.setState({ _maximize: !this.state._maximize }, () => {
      $storage.set(this.__maximizeKey, this.state._maximize)

      // be:3.9.4
      setTimeout(() => {
        const $form = this.getFormComp()._$form
        $form && $($form).parent().find('.protable').perfectScrollbar('update')
      }, 200)
    })
  }

  componentDidMount() {
    const $root = $(this._rbmodal)
      .modal({
        show: false,
        backdrop: 'static',
        keyboard: true,
      })
      .on('hidden.bs.modal', () => {
        $keepModalOpen()
        if (this.props.disposeOnHide === true) {
          $root.modal('dispose')
          $unmount($root.parent().parent())
        }
      })
    this._showAfter({}, true)
  }

  // fix:v4.2 保持当前实例
  componentWillUnmount() {
    const that = RbFormModal
    if (that.__CURRENT42 && that.__CURRENT42.length) {
      that.__CURRENT42.pop() // remove
      that.__CURRENT35 = that.__CURRENT42.at(-1) || null // last
    }
  }

  // 渲染表单
  getFormModel() {
    const entity = this.state.entity
    const id = this.state.id || ''
    const initialValue = this.state.initialValue || {} // 默认值填充（仅新建有效）

    let url = `/app/${entity}/form-model?id=${id}`
    if (this.state.specLayout) url += `&layout=${this.state.specLayout}`
    if (this.props.mainLayoutId) url += `&mainLayoutId=${this.props.mainLayoutId}`

    const that = this
    function _FN2(formModel, forceInitFieldValue) {
      const FORM = (
        <RbForm
          entity={entity}
          id={id}
          rawModel={formModel}
          forceInitFieldValue={forceInitFieldValue}
          $$$parent={that}
          readonly={!!formModel.readonlyMessage}
          ref={(c) => (that._formComponentRef = c)}
          _disableAutoFillin={that.props._disableAutoFillin}>
          {formModel.elements.map((item) => detectElement(item))}
        </RbForm>
      )

      that.setState({ formComponent: FORM, alertMessage: formModel.readonlywMessage || formModel.readonlyMessage || null }, () => {
        that.setState({ inLoad: false })
        if (window.FrontJS) {
          window.FrontJS.Form._trigger('open', [formModel])
        }
      })

      that.__lastModified = formModel.lastModified || 0

      if (formModel.alertMessage) {
        setTimeout(() => RbHighbar.create(formModel.alertMessage), 1000)
      }
    }

    // v3.8
    if (this.props.initialFormModel) {
      _FN2(this.props.initialFormModel, true)
      return
    }

    $.post(url, JSON.stringify(initialValue), (res) => {
      // 包含错误
      if (res.error_code > 0 || !!res.data.error) {
        const error = (res.data || {}).error || res.error_msg
        this.renderFromError(error)
      } else {
        _FN2(res.data)
      }
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
      this.setState(state, () => this._showAfter({ reset: false }, true))
    } else {
      this._showAfter({ ...state, reset: false })
      this._checkDrityData()
    }
  }

  _showAfter(state, modelChanged) {
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
    // v3.4
    if (location.href.includes('/app/entity/form')) {
      window.close()
      return
    }

    $(this._rbmodal).modal('hide')

    const state = { reset: reset === true }
    if (state.reset) {
      state.id = null
      state.previewid = null
    }
    this.setState(state)
  }

  // 获取当前表单对象
  getFormComp() {
    return this._formComponentRef
  }

  // -- Usage
  /**
   * @param {*} props
   * @param {*} forceNew
   */
  static create(props, forceNew) {
    // 自定义编辑
    if ((window.__LAB40_EDIT_PROVIDERS || {})[props.entity]) {
      window.__LAB40_EDIT_PROVIDERS[props.entity](props, forceNew)
      return
    }

    // `__CURRENT35`, `__HOLDER` 可能已 unmount
    const that = RbFormModal
    that.__CURRENT42 = that.__CURRENT42 || []

    if (forceNew === true) {
      renderRbcomp(<RbFormModal {...props} disposeOnHide />, function () {
        that.__CURRENT35 = this
        that.__CURRENT42.push(this)
      })
      return
    }

    if (this.__HOLDER) {
      this.__HOLDER.show(props)
    } else {
      renderRbcomp(<RbFormModal {...props} />, function () {
        that.__HOLDER = this
        that.__CURRENT35 = this
        that.__CURRENT42.push(this)
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
        let val = iv[k]
        // array, object, simple
        val = val && typeof val === 'object' ? val.id || val : val
        this.__FormData[k] = { value: val, error: null }
      }
    }

    this.isNew = !props.id

    const $$$props = props.$$$parent && props.$$$parent.props ? props.$$$parent.props : {}
    this._postBefore = props.postBefore || $$$props.postBefore
    this._postAfter = props.postAfter || $$$props.postAfter
    this._onProTableLineUpdated = props.onProTableLineUpdated || $$$props.onProTableLineUpdated
    this._dividerRefs = []
    this._verticalLayout42 = window.__LAB_VERTICALLAYOUT || props.rawModel.verticalLayout === 1 || props.rawModel.verticalLayout === 3
  }

  render() {
    return (
      <div className={`rbform form-layout ${this._verticalLayout42 && 'vertical38'}`}>
        <div className="form row" ref={(c) => (this._$form = c)}>
          {this.props.children.map((fieldComp) => {
            let refKey = `fieldcomp-${fieldComp.props.field}`
            if (fieldComp.props.field === TYPE_DIVIDER) refKey = $random('divider-')
            else if (fieldComp.props.field === TYPE_REFFORM) refKey = $random('refform-')
            // for init
            if (fieldComp.props.field === TYPE_DIVIDER && fieldComp.props.collapsed) {
              this._dividerRefs.push(refKey)
            }
            return React.cloneElement(fieldComp, { $$$parent: this, ref: refKey })
          })}
        </div>

        {this.renderDetailForms()}
        {this.renderFormAction()}
      </div>
    )
  }

  renderDetailForms() {
    if (!window.ProTable || !this.props.rawModel.detailMeta) return null

    // v3.7 ND
    const detailImports = this.props.rawModel.detailImports
    // v3.9 记录转换
    const transDetails39 = this.props.rawModel['$DETAILS$']

    this._ProTables = {}

    return (
      <RF>
        {this.props.rawModel.detailMetas.map((item, idx) => {
          return <RF key={idx}>{this._renderDetailForms(item, detailImports, transDetails39)}</RF>
        })}
      </RF>
    )
  }

  _renderDetailForms(detailMeta, detailImports, transDetails39) {
    let _ProTable
    const dProps = {
      entity: detailMeta,
      mainid: this.state.id,
      ref: (c) => {
        _ProTable = c // ref
        this._ProTables[detailMeta.entity] = c
        this._ProTable = c // comp:v3.8
      },
      $$$main: this,
      transDetails: transDetails39 ? transDetails39[detailMeta.entity] : null,
      transDetailsDelete: transDetails39 ? transDetails39[detailMeta.entity + '$DELETED'] : null,
      mainLayoutId: this.props.rawModel.layoutId,
      _disableAutoFillin: this.props._disableAutoFillin,
    }

    if (window._CustomizedForms) {
      _ProTable = window._CustomizedForms.useProTable(dProps)
      if (_ProTable === false) return null // 不显示
    }

    function _addNew(n = 1) {
      for (let i = 0; i < n; i++) {
        setTimeout(() => _ProTable.addNew(), i * 20)
      }
    }

    function _setLines(details, force) {
      // v3.7 ifAuto
      if (force) _ProTable.clear()

      if (_ProTable.isEmpty()) {
        _ProTable.setLines(details)
      } else {
        RbAlert.create($L('是否保留已有明细记录？'), {
          confirmText: $L('保留'),
          cancelText: $L('不保留'),
          onConfirm: function () {
            this.hide()
            _ProTable.setLines(details)
          },
          onCancel: function () {
            this.hide()
            _ProTable.clear()
            setTimeout(() => _ProTable.setLines(details), 200)
          },
        })
      }
    }

    // 记录转换:明细导入
    let _detailImports = []
    if (detailImports) {
      let ifAutoReady = false
      detailImports.forEach((item) => {
        if (item.detailName !== detailMeta.entity) return

        const diConf = {
          icon: item.icon,
          label: item.transName || item.entityLabel,
          fetch: (form, cb, autoFields) => {
            const formdata = form.getFormData()
            if (autoFields) {
              // NOTE 全部字段有值才自动
              let lackValue = false
              autoFields.forEach((item) => {
                if (lackValue) return
                lackValue = !formdata[item]
              })
              if (lackValue) return
            }

            const mainid = form.props.id || null
            $.post(`/app/entity/extras/detail-imports?transid=${item.transid}&mainid=${mainid}&layoutId=${_ProTable.getLayoutId()}`, JSON.stringify(formdata), (res) => {
              if (res.error_code === 0) {
                if (autoFields) {
                  typeof cb === 'function' && cb(res.data)
                } else {
                  if ((res.data || []).length === 0) RbHighbar.create($L('没有可导入的明细记录'))
                  else typeof cb === 'function' && cb(res.data)
                }
              } else {
                RbHighbar.error(res.error_msg)
              }
            })
          },
        }

        _detailImports.push(diConf)

        // v3.7 ifAuto
        // 如果一个明细实体有多个配置，仅第一个生效
        if (item.auto === 3 || (this.isNew && item.auto === 1) || (!this.isNew && item.auto === 2)) {
          if (!ifAutoReady) {
            ifAutoReady = true
            let ifAutoReady_timer
            this.onFieldValueChange((fv) => {
              if (item.autoFields.includes(fv.name)) {
                if (ifAutoReady_timer) {
                  clearTimeout(ifAutoReady_timer)
                  ifAutoReady_timer = null
                }

                if (fv.value) {
                  ifAutoReady_timer = setTimeout(() => {
                    diConf.fetch(this, (details) => _setLines(details, true), item.autoFields)
                  }, 400)
                }
              }
            })
          }
        }
      })
    }

    if (!_ProTable) {
      _ProTable = ProTable.create(dProps)
    } else {
      this._ProTable = _ProTable // comp:v3.8
    }

    return (
      <div className="detail-form-table" data-entity={detailMeta.entity}>
        <div className="detail-form-table-header">
          <div className="row">
            <div className="col-4 detail-form-header">
              <h5 className="text-bold">
                <i className={`icon zmdi zmdi-${detailMeta.icon} mr-1`} />
                {detailMeta.entityLabel}
                {rb.isAdminUser && (
                  <a href={`${rb.baseUrl}/admin/entity/${detailMeta.entity}/form-design`} target="_blank" title={$L('表单设计')}>
                    <span className="zmdi zmdi-settings up-1" />
                  </a>
                )}
              </h5>
            </div>

            <div className="col-8 text-right detail-form-action">
              <div className="fjs-dock"></div>
              {_detailImports.length > 0 && (
                <div className={`btn-group J_import-detail ${this.props.readonly && 'hide'}`}>
                  <button className="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown" disabled={this.props.readonly}>
                    <i className="icon mdi mdi-transfer-down"></i> {$L('导入明细')}
                  </button>
                  <div className="dropdown-menu dropdown-menu-right">
                    {_detailImports.map((def, idx) => {
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

              <div className={`btn-group J_add-detail ml-2 ${this.props.readonly && 'hide'}`}>
                <button className="btn btn-secondary" type="button" onClick={() => _addNew()}>
                  <i className="icon x14 mdi mdi-playlist-plus mr-1" />
                  {$L('添加明细')}
                </button>
                <button className="btn btn-secondary dropdown-toggle w-auto" type="button" data-toggle="dropdown" disabled={this.props.readonly}>
                  <i className="icon zmdi zmdi-chevron-down" />
                </button>
                <div className="dropdown-menu dropdown-menu-right">
                  {[10, 20].map((n) => {
                    return (
                      <a className="dropdown-item" onClick={() => _addNew(n)} key={`n-${n}`}>
                        {$L('添加 %d 条', n)}
                      </a>
                    )
                  })}
                  <a
                    className="dropdown-item"
                    onClick={() => {
                      if (rb.commercial < 10) {
                        return RbHighbar.error(WrapHtml($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
                      }

                      const fields = []
                      _ProTable.state.formFields.forEach((item) => {
                        if (item.readonly === false && !['IMAGE', 'FILE', 'AVATAR', 'SIGN'].includes(item.type)) {
                          fields.push({ field: item.field, label: item.label, type: item.type })
                        }
                      })

                      const mainid = this.state.id || '000-0000000000000000'
                      renderRbcomp(
                        // eslint-disable-next-line react/jsx-no-undef
                        <ExcelClipboardDataModal entity={detailMeta.entity} fields={fields} mainid={mainid} layoutId={_ProTable.getLayoutId()} onConfirm={(data) => _setLines(data)} />
                      )
                    }}>
                    {$L('从 Excel 添加')} <sup className="rbv" />
                  </a>
                  <div className="dropdown-divider" />
                  <a className="dropdown-item" onClick={() => _ProTable.clear()}>
                    {$L('清空')}
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="detail-form-table-body">{_ProTable}</div>
      </div>
    )
  }

  renderFormAction() {
    const props = this.props
    const parentProps = props.$$$parent ? props.$$$parent.props || {} : {}
    let moreActions = []
    // 添加明细
    if (props.rawModel.mainMeta) {
      if (parentProps.nextAddDetail) {
        moreActions.push(
          <a key="Action101" className="dropdown-item" onClick={() => this.post(RbForm.NEXT_NEWDETAIL)}>
            {$L('保存并添加')}
          </a>
        )
      }
    } else {
      // 保存并...
      if (parentProps.showExtraButton) {
        if (props.rawModel.hadApproval && window.ApprovalSubmitForm) {
          moreActions.push(
            <a key="Action103" className="dropdown-item" onClick={() => this.post(RbForm.NEXT_SUBMIT37)}>
              {$L('保存并提交')}
            </a>
          )
        }
        moreActions.push(
          <a key="Action105" className="dropdown-item" onClick={() => this.post(RbForm.NEXT_ADD36)}>
            {$L('保存并新建')}
          </a>
        )
        moreActions.push(
          <a key="Action104" className="dropdown-item" onClick={() => this.post(RbForm.NEXT_VIEW)}>
            {$L('保存并打开')}
          </a>
        )
      }
    }

    // @see #_postAfterExec
    if (typeof this._postAfter === 'function') moreActions = []

    // v3.8
    const $$$props = props.$$$parent && props.$$$parent.props ? props.$$$parent.props : {}
    const confirmText = props.confirmText || $$$props.confirmText
    const cancelText = props.cancelText || $$$props.cancelText

    return (
      <div className="dialog-footer" ref={(c) => (this._$formAction = c)}>
        <div className="fjs-dock"></div>
        <button className="btn btn-secondary btn-space" type="button" onClick={() => props.$$$parent.hide()}>
          {cancelText || $L('取消')}
        </button>
        {!props.readonly && (
          <div className="btn-group dropup btn-space ml-1 J_save">
            <button className="btn btn-primary" type="button" onClick={() => this.post()}>
              {confirmText || $L('保存')}
            </button>
            {moreActions.length > 0 && (
              <RF>
                <button className="btn btn-primary dropdown-toggle w-auto" type="button" data-toggle="dropdown">
                  <i className="icon zmdi zmdi-chevron-up" />
                </button>
                <div className="dropdown-menu dropdown-menu-primary dropdown-menu-right">{moreActions}</div>
              </RF>
            )}
          </div>
        )}
      </div>
    )
  }

  componentDidMount() {
    // 新记录初始值
    if (this.isNew || this.props.forceInitFieldValue) {
      this.props.children.map((child) => {
        let iv = child.props.value
        if (!$empty(iv) && (!this.props.readonly || (this.props.readonly && this.props.readonlyw === 3))) {
          if (typeof iv === 'object') {
            if (child.props.type === 'N2NREFERENCE') {
              // fix: 4.0.6
              let iv2 = []
              iv.forEach((item) => iv2.push(item.id))
              iv = iv2.join(',')
            } else if (child.props.type === 'TAG') {
              // eg. 标签
              iv = iv.join('$$$$')
            } else if (child.props.type === 'LOCATION') {
              // eg. 位置
            } else if (Array.isArray(iv)) {
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
      this.refs[d]._toggle()
    })

    setTimeout(() => {
      RbForm.renderAfter(this)

      // v4.0 编辑时触发
      if (window.FrontJS && window.EasyFilterEval && this.props.id && !this.props.readonly) {
        if (window.EasyFilterEval) window.EasyFilterEval.evalAndEffect(this)
      }
    }, 20)
  }

  // 表单回填
  setAutoFillin(data) {
    if (!data || data.length === 0) return

    data.forEach((item) => {
      const fieldComp = this.getFieldComp(item.target)
      if (fieldComp) {
        // 非强制
        if (!item.fillinForce && fieldComp.getValue()) return
        if ((this.isNew && item.whenCreate) || (!this.isNew && item.whenUpdate)) fieldComp.setValue(item.value)
      }
    })
  }

  // 设置字段值
  setFieldValue(field, value, error) {
    this.__FormData[field] = { value: value, error: error }
    this._onFieldValueChangeCall(field, value)

    // eslint-disable-next-line no-console
    if (rb.env === 'dev') console.log('FV1 ... ' + field + ' : ' + JSON.stringify(this.__FormData))
  }

  // 避免无意义更新
  setFieldUnchanged(field, originValue) {
    delete this.__FormData[field]
    this._onFieldValueChangeCall(field, originValue)

    // eslint-disable-next-line no-console
    if (rb.env === 'dev') console.log('FV2 ... ' + field + ' : ' + JSON.stringify(this.__FormData))
  }

  // 添加字段值变化回调
  onFieldValueChange(cb) {
    const c = this._onFieldValueChange_calls || []
    c.push(cb)
    this._onFieldValueChange_calls = c
  }
  // 执行
  _onFieldValueChangeCall(field, value) {
    if (this._onFieldValueChange_calls) {
      this._onFieldValueChange_calls.forEach((c) => c({ name: field, value: value }))
    }

    if (window.FrontJS) {
      const fieldKey = `${this.props.entity}.${field}`
      // fix:v4.2-b3 统一使用ID
      if (value && value.id) value = value.id
      window.FrontJS.Form._trigger('fieldValueChange', [fieldKey, value, this.props.id || null])
      // v4.0
      if (window.EasyFilterEval) window.EasyFilterEval.evalAndEffect(this)
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
    const fieldRefs = this.refs
    for (let key in fieldRefs) {
      if (!key.startsWith('fieldcomp-')) continue

      const fieldComp = fieldRefs[key]
      let v = fieldComp.getValue()
      if (v && typeof v === 'object') v = v.id || v // array
      if (v) data[fieldComp.props.field] = v
    }
    return data
  }

  // 获取明细表
  getProTables() {
    return this._ProTables ? Object.values(this._ProTables) : null
  }
  getProTable(detailName) {
    if (detailName) return this._ProTables ? this._ProTables[detailName] || null : null
    return (this.getProTables() || [])[0] || null
  }

  // 保存并添加明细
  static NEXT_NEWDETAIL = 102
  // 保存并打开
  static NEXT_VIEW = 104
  // 保存并新建
  static NEXT_ADD36 = 105
  // 保存并提交
  static NEXT_SUBMIT37 = 103
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

    if (this._ProTables) {
      const detailsNotEmpty = this.props.rawModel.detailsNotEmpty
      let detailsMix = []

      const keys = Object.keys(this._ProTables)
      for (let i = 0; i < keys.length; i++) {
        const _ProTable = this._ProTables[keys[i]]
        // 明细未配置或出错
        if (!_ProTable._initModel) continue

        const details = _ProTable.buildFormData()
        if (!details) return

        const detailsNotEmpty34 = _ProTable._initModel.detailsNotEmpty || detailsNotEmpty
        if (detailsNotEmpty34 && _ProTable.isEmpty()) {
          RbHighbar.create($L('请添加明细'))
          return // break
        }

        detailsMix = [...detailsMix, ...details]
      }
      data['$DETAILS$'] = detailsMix
    }

    data.metadata = {
      entity: this.state.entity,
      id: this.state.id,
    }

    // 提交前
    if (this._postBeforeExec(data) === false) return

    const $$$parent = this.props.$$$parent
    const previewid = $$$parent.state.previewid

    const $btn = $(this._$formAction).find('.btn').button('loading')
    let url = '/app/entity/record-save'
    if (previewid) url += `?previewid=${previewid}`
    if (weakMode) url += `${url.includes('?') ? '&' : '?'}weakMode=${weakMode}`
    if (this._postAction || this.props._postAction) {
      url += `${url.includes('?') ? '&' : '?'}_postAction=${$encode(this._postAction || this.props._postAction)}`
    }
    $.post(url, JSON.stringify(data), (res) => {
      $btn.button('reset')
      if (res.error_code === 0) {
        RbHighbar.success($L('保存成功'))

        // for `reference-search.html`
        if (location.href.includes('/app/entity/form')) {
          localStorage.setItem('referenceSearch__reload', $random())
        }

        setTimeout(() => {
          $$$parent.hide(true)

          const recordId = res.data.id

          // 提交后:如有提交后回调则仅执行此
          if (typeof this._postAfter === 'function') {
            this._postAfter(recordId, next, this)
            return
          }

          if (next === RbForm.NEXT_NEWDETAIL) {
            const iv = $$$parent.props.initialValue
            const dm = this.props.rawModel.entityMeta
            RbFormModal.create({ title: $L('添加%s', dm.entityLabel), entity: dm.entity, icon: dm.icon, initialValue: iv })
            // ~
          } else if (next === RbForm.NEXT_VIEW) {
            if (window.RbViewModal) {
              window.RbViewModal.create({ id: recordId, entity: this.state.entity })
              if (window.RbListPage) location.hash = `!/View/${this.state.entity}/${recordId}`
            } else if (parent && parent.RbViewModal) {
              parent.RbViewModal.create({ id: recordId, entity: this.state.entity }, true)
            } else {
              window.open(`${rb.baseUrl}/app/redirect?id=${recordId}&type=dock`)
            }
            // ~
          } else if (next === RbForm.NEXT_ADD36) {
            let titleNew = $$$parent.state.title
            if ($$$parent.props.id) titleNew = titleNew.replace($L('编辑%s', ''), $L('新建%s', ''))
            const copyProps = { entity: $$$parent.state.entity, icon: $$$parent.state.icon, title: titleNew }
            RbFormModal.create(copyProps, false)
            // ~
          } else if (next === RbForm.NEXT_SUBMIT37) {
            renderRbcomp(<ApprovalSubmitForm id={recordId} />)
            // ~
          }

          this._postAfterExec({ ...res.data, isNew: !this.state.id }, next)

          // ~
        }, 200)
      } else if (res.error_code === 499) {
        // 重复记录
        renderRbcomp(<RepeatedViewer entity={this.state.entity} data={res.data} />)
      } else if (res.error_code === 497) {
        // 弱校验
        const that = this
        const msg_id = res.error_msg.split('$$$$')
        RbAlert.create(msg_id[0], {
          onConfirm: function () {
            this.hide()
            that._post(next, msg_id[1])
          },
        })
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
    return true
  }

  // 提交前调用
  _postBeforeExec(data) {
    if (typeof this._postBefore === 'function') {
      const ret = this._postBefore(data, this)
      if (ret === false) return false
    }

    if (window.FrontJS) {
      const ret = window.FrontJS.Form._trigger('saveBefore', [data, this])
      if (ret === false) return false
    }

    let ret = RbForm.postBefore(data, this)
    if (ret === false) return false
  }

  // 提交后调用
  _postAfterExec(data, next) {
    if (window.FrontJS) {
      window.FrontJS.Form._trigger('saveAfter', [data, next, this])
    }

    // 刷新列表
    const rlp = window.RbListPage || parent.RbListPage
    if (rlp) rlp.reload(data.id)
    // 刷新视图
    if (window.RbViewPage && next !== RbForm.NEXT_NEWDETAIL) window.RbViewPage.reload()
  }

  // -- HOOK 复写

  // 保存前调用（返回 false 则不继续保存）
  // eslint-disable-next-line no-unused-vars
  static postBefore(data, formObject) {}
  // 保存后调用
  // eslint-disable-next-line no-unused-vars
  static postAfter(data, next, formObject) {}
  // 组件渲染后调用
  // eslint-disable-next-line no-unused-vars
  static renderAfter(formObject) {}
}

// 表单元素基础类
class RbFormElement extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }

    // v35
    this._isNew = props.$$$parent.isNew
    if (props.readonly) {
      if (this._isNew) {
        this._placeholderw = props.readonlyw >= 2 ? $L('自动值') : null
      } else if (!this.state.value) {
        this._placeholderw = $L('无')
      }
    }
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
      <div className={`col-12 col-sm-${colspan} form-group type-${props.type} ${editable ? 'editable' : ''} ${state.hidden ? 'hide' : ''}`} data-field={props.field}>
        <label className={`col-form-label ${!props.onView && !state.nullable ? 'required' : ''}`}>{props.label}</label>
        <div ref={(c) => (this._fieldText = c)} className="col-form-control">
          {!props.onView || (editable && state.editMode) ? this.renderElement() : this.renderViewElement()}
          {!props.onView && state.tip && <p className="form-text">{state.tip}</p>}

          {editable && !state.editMode && <a className="edit" onClick={() => this.toggleEditMode(true)} title={$L('编辑')} />}
          {editable && state.editMode && (
            <div className="edit-oper">
              <div className="btn-group shadow-sm">
                <button type="button" className="btn btn-secondary" onClick={() => this.handleEditConfirm()} title={$L('确定')}>
                  <i className="icon zmdi zmdi-check" />
                </button>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => {
                    this.toggleEditMode(false)
                    // fix: v3.8
                    props.$$$parent && props.$$$parent.setFieldUnchanged && props.$$$parent.setFieldUnchanged(props.field)
                  }}
                  title={$L('取消')}>
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
    if (props.onView) {
      if (props.dataCopy && this._fieldValue) {
        const $text = $(this._fieldValue)
          .attr({
            'data-copy': true,
            'title': $L('点击复制'),
          })
          .on('click', (e) => {
            if (e.target.tagName === 'A' || $(e.target).closest('a').length) return // fix:4.2.3 链接不复制
            $stopEvent(e, true)
            $clipboard2($text.text(), true)
          })
      }
    } else {
      // 必填字段
      if (!this.state.nullable && $empty(props.value) && props.readonlyw !== 2 && props.unreadable !== true) {
        props.$$$parent.setFieldValue(props.field, null, $L('%s不能为空', props.label))
      }

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
    const _readonly37 = this.state.readonly
    const value = arguments.length > 0 ? arguments[0] : this.state.value

    return (
      <input
        ref={(c) => (this._fieldValue = c)}
        className={`form-control form-control-sm ${this.state.hasError ? 'is-invalid' : ''}`}
        title={this.state.hasError}
        type="text"
        value={value || ''}
        onChange={(e) => this.handleChange(e, !_readonly37)}
        readOnly={_readonly37}
        placeholder={this._placeholderw}
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
      <div
        className="form-control-plaintext"
        ref={(c) => {
          if (value) this._fieldValue = c // fix:4.2.2
        }}>
        {value || <span className="text-muted">{$L('无')}</span>}
      </div>
    )
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
      // v3.7 lazy
      if (this.__handleChangeTimer) {
        clearTimeout(this.__handleChangeTimer)
        this.__handleChangeTimer = null
      }

      this.__handleChangeTimer = setTimeout(() => {
        checkValue === true && this.checkValue()
        typeof this.props.onValueChange === 'function' && this.props.onValueChange(this)
      }, 222)
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
    const errMsg = err ? this.props.label + ' ' + err : null

    if (this.isValueUnchanged() && !this._isNew) {
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
    if (this.state.nullable === false) {
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
        if (Array.isArray(this.__select2)) {
          $(this.__select2).each(function () {
            this.select2('destroy')
          })
        } else {
          this.__select2.select2('destroy')
        }
        this.__select2 = null
        if (rb.env === 'dev') console.log('RbFormElement destroy select2 :', this.props.field)
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
    const $$$parent = this.props.$$$parent
    $$$parent && $$$parent.saveSingleFieldValue && $$$parent.saveSingleFieldValue(this)
  }

  // Setter
  setValue(val) {
    this.handleChange({ target: { value: val } }, true)
  }
  // Getter
  getValue() {
    return this.state.value
  }

  // 隐藏/显示
  setHidden(hidden) {
    this.setState({ hidden: hidden === true })
  }
  // 可空/非空
  setNullable(nullable) {
    this.setState({ nullable: nullable === true }, () => {
      // fix:v3.8 通过此方法强制检查非空属性
      this.setValue(this.state.value || null)
    })
  }
  // 只读/非只读
  // 部分字段有效，且如字段属性为只读，即使填写值也无效
  setReadonly(readonly) {
    this.setState({ readonly: readonly === true }, () => {
      // fix 4.0.6 只读变为非只读，富组件仍需初始化
      try {
        this.onEditModeChanged(readonly === true, true)
      } catch (err) {
        console.error(err)
      }
    })
  }
  // TIP 仅表单有效
  setTip(tip) {
    this.setState({ tip: tip || null })
  }
}

class RbFormText extends RbFormElement {
  constructor(props) {
    super(props)
    this._textCommonMenuId = props.readonly || !props.textCommon ? null : $random('tcddm-')
  }

  renderElement() {
    const comp = super.renderElement()
    return this._textCommonMenuId ? React.cloneElement(comp, { 'data-toggle': 'dropdown', 'data-target': `#${this._textCommonMenuId}` }) : comp
  }

  componentWillUnmount() {
    super.componentWillUnmount()

    if (this._textCommonMenuId) {
      if (rb.dev === 'env') console.log('[dev] unmount dropdown-menu with text-common:', this._textCommonMenuId)
      $unmount($(`#${this._textCommonMenuId}`).parent())
    }
  }

  onEditModeChanged(destroy) {
    if (destroy) {
      super.onEditModeChanged(destroy)
      return
    }

    if (this._textCommonMenuId && !$(`#${this._textCommonMenuId}`)[0]) {
      if (rb.dev === 'env') console.log('[dev] init dropdown-menu with text-common', this._textCommonMenuId)
      renderRbcomp(
        <div id={this._textCommonMenuId}>
          <div className="dropdown-menu common-texts">
            <h5>{$L('常用')}</h5>
            {this.props.textCommon.split(',').map((c) => {
              return (
                <a
                  key={c}
                  title={c}
                  className="badge text-ellipsis"
                  onClick={() => {
                    this.handleChange({ target: { value: c } }, true)
                    $focus2End(this._fieldValue)
                  }}>
                  {c}
                </a>
              )
            })}
          </div>
        </div>
      )

      // fix:4.1-b5 禁用时不触发
      $(this._fieldValue).on('click', function (e) {
        const $t = e.target || {}
        if ($t.disabled || $t.readOnly) {
          $stopEvent(e, true)
          return false
        }
      })
    }
  }
}

class RbFormUrl extends RbFormText {
  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()

    const clickUrl = `${rb.baseUrl}/commons/url-safe?url=${encodeURIComponent(this.state.value)}`
    return (
      <div className="form-control-plaintext" ref={(c) => (this._fieldValue = c)}>
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
      <div className="form-control-plaintext" ref={(c) => (this._fieldValue = c)}>
        {$env.isDingTalk() ? (
          <a>{this.state.value}</a>
        ) : (
          <a title={$L('发送邮件')} href={`mailto:${this.state.value}`} className="link">
            {this.state.value}
          </a>
        )}
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
      <div className="form-control-plaintext" ref={(c) => (this._fieldValue = c)}>
        {$env.isDingTalk() || $env.isWxWork() ? (
          <a>{this.state.value}</a>
        ) : (
          <a title={$L('拨打电话')} href={`tel:${this.state.value}`} className="link">
            {this.state.value}
          </a>
        )}
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

    const value = this.state.value
    if (!!value && $regex.isDecimal(value) === false) return $L('格式不正确')
    if (!!value && $isTrue(this.props.notNegative) && parseFloat(value) < 0) return $L('不能为负数')
    return null
  }
  _isValueError() {
    return super.isValueError()
  }

  renderElement() {
    const _readonly37 = this.state.readonly
    let value = arguments.length > 0 ? arguments[0] : this.state.value
    // `0`
    if (value === undefined || value === null) value = ''

    return (
      <RF>
        <input
          ref={(c) => (this._fieldValue = c)}
          className={`form-control form-control-sm ${this.state.hasError ? 'is-invalid' : ''}`}
          title={this.state.hasError}
          type="text"
          value={value}
          onChange={(e) => this.handleChange(e, !_readonly37)}
          readOnly={_readonly37}
          placeholder={this._placeholderw}
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
    // 表单计算
    if (this.props.calcFormula && !this.props.onView) __calcFormula(this)
  }

  onEditModeChanged(destroy) {
    if (destroy);
    else {
      this.setState({ value: this._removeComma(this.state.value) })
    }
  }

  // 移除千分为位
  _removeComma(n) {
    if (n === null || n === undefined || n === '') return ''
    if (n === '-') return n // 输入负数
    if ((n + '').substring(0, 1) === '*') return n // 脱敏
    if (n) n = $cleanNumber(n)
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
}

class RbFormNText extends RbFormElement {
  constructor(props) {
    super(props)
    this._textCommonMenuId = props.readonly || !props.textCommon ? null : $random('tcddm-')

    this._height = 0
    if (!this.props.useMdedit) {
      this._height = ~~this.props.height
      // v4.2 填0自动高度
      if (this.props.height === '0') {
        this._heightAuto = true
      } else if (this._height > 0) {
        if (this._height === 1) this._height = 37
        else this._height = this._height * 20 + 12
      }
    }
  }

  renderElement() {
    const _readonly37 = this.state.readonly
    const props = this.props

    let clazz2 = `form-control ${props.useCode && 'formula-code'} ${props.useMdedit && _readonly37 ? 'cm-readonly' : ''} ${this.state.hasError && 'is-invalid'}`
    if (!(this._heightAuto || this._height > 0)) clazz2 += ' row3x'
    let style2 = this._height > 0 ? { height: this._height } : this._heightAuto ? { height: 37 } : null
    return (
      <RF>
        <textarea
          ref={(c) => {
            this._fieldValue = c
            this._heightAuto && c && autosize(c)
          }}
          className={clazz2}
          style={style2}
          title={this.state.hasError}
          value={this.state.value || ''}
          onChange={(e) => this.handleChange(e, !_readonly37)}
          readOnly={_readonly37}
          placeholder={this._placeholderw}
          maxLength="6000"
          data-fix-autosize-height="37px"
        />
        {props.useMdedit && !_readonly37 && <input type="file" className="hide" accept="image/*" data-noname="true" ref={(c) => (this._fieldValue__upload = c)} />}
        {this._textCommonMenuId && (
          <a className={`badge text-common ${_readonly37 && 'hide'}`} data-toggle="dropdown" data-target={`#${this._textCommonMenuId}`}>
            {$L('常用值')}
          </a>
        )}
      </RF>
    )
  }

  renderViewElement() {
    if (!this.state.value) return super.renderViewElement()

    let style2 = {}
    if (this._height > 0) style2.maxHeight = this._height
    else if (this._heightAuto) style2.maxHeight = 481

    if (this.props.useMdedit) {
      return (
        <div className="form-control-plaintext md-content" ref={(c) => (this._fieldValue = c)} style={style2}>
          <Md2Html markdown={this.state.value} />
        </div>
      )
    } else {
      let text2 = this.state.value.replace(/</g, '&lt;').replace(/\n/g, '<br/>')
      if (this.props.useCode) {
        text2 = $formattedCode(text2, 'json')
        text2 = text2.replace(/\n/g, '<br/>') //.replace(/\s/g, '&nbsp;')
      }

      return (
        <RF>
          <div className={`form-control-plaintext ${this.props.useCode && 'formula-code'}`} ref={(c) => (this._fieldValue = c)} style={style2}>
            {WrapHtml(text2)}
          </div>

          <div className={`ntext-action ${window.__LAB_SHOWNTEXTACTION || this.props.useCode ? '' : 'hide'}`}>
            <a title={$L('展开/收起')} onClick={() => $(this._fieldValue).toggleClass('ntext-expand')}>
              <i className="mdi mdi-arrow-expand" />
            </a>
            <a ref={(c) => (this._$actionCopy = c)}>
              <i className="mdi mdi-content-copy" />
            </a>
          </div>
        </RF>
      )
    }
  }

  UNSAFE_componentWillUpdate(nextProps, nextState) {
    // destroy
    if (this.state.editMode && !nextState.editMode) {
      if (this._EasyMDE) {
        this._EasyMDE.toTextArea()
        this._EasyMDE = null
      }
    }
  }

  componentWillUnmount() {
    super.componentWillUnmount()
    this._fieldValue && this._heightAuto && autosize.destroy(this._fieldValue)
  }

  componentDidMount() {
    super.componentDidMount()
    // fix:4.1
    if (this.props.onView) {
      $(this._fieldValue).perfectScrollbar()
      this._initActionCopy()
    }
  }

  onEditModeChanged(destroy) {
    if (this.props.onView && this._fieldValue) {
      if (destroy) {
        $(this._fieldValue).perfectScrollbar()
      } else {
        $(this._fieldValue).perfectScrollbar('destroy')
      }
    }
    if (this._fieldValue && this._heightAuto && destroy) {
      autosize.destroy(this._fieldValue)
    }

    if (!destroy) {
      // MDE
      if (this.props.useMdedit) this._initMde()
      // v4.1 常用值
      if (this._textCommonMenuId && !$(`#${this._textCommonMenuId}`)[0]) {
        if (rb.dev === 'env') console.log('[dev] init dropdown-menu with text-common', this._textCommonMenuId)
        renderRbcomp(
          <div id={this._textCommonMenuId}>
            <div className="dropdown-menu  dropdown-menu-right common-texts">
              {this.props.textCommon.split(',').map((c) => {
                let cLN = c.replace(/\\n/g, '\n') // 换行符
                return (
                  <a
                    key={c}
                    title={cLN}
                    className="badge text-ellipsis"
                    onClick={() => {
                      if (this._EasyMDE) {
                        this._mdeInsert(cLN)
                      } else {
                        const ps = this._fieldValue.selectionStart,
                          pe = this._fieldValue.selectionEnd
                        let val = this.state.value
                        if ($empty(val)) val = cLN
                        else val = val.substring(0, ps) + cLN + val.substring(pe)
                        this.handleChange({ target: { value: val } }, true)
                        // $focus2End(this._fieldValue)
                      }
                    }}>
                    {c}
                  </a>
                )
              })}
            </div>
          </div>
        )
      }
    }

    this._initActionCopy()
  }

  setValue(val) {
    super.setValue(val)
    if (this.props.useMdedit) this._EasyMDE.value(val || '')
  }

  handleChange(e, checkValue) {
    super.handleChange(e, checkValue)

    // fix:4.2.2
    if (this._heightAuto) {
      setTimeout(() => autosize.update(this._fieldValue), 20)
    }
  }

  _initActionCopy() {
    if (!this._$actionCopy) return

    const that = this
    const initCopy = function () {
      $clipboard($(that._$actionCopy), that.state.value)
    }
    if (window.ClipboardJS) {
      initCopy()
    } else {
      $getScript('/assets/lib/clipboard.min.js', initCopy)
    }
  }

  _initMde() {
    const _readonly37 = this.state.readonly

    // fix:4.1-b5
    this._EasyMDE && this._EasyMDE.toTextArea()

    const mde = new EasyMDE({
      element: this._fieldValue,
      status: false,
      autoDownloadFontAwesome: false,
      spellChecker: false,
      // eslint-disable-next-line no-undef
      toolbar: _readonly37 ? false : DEFAULT_MDE_TOOLBAR(this),
      previewClass: 'md-content',
      onToggleFullScreen: (is) => {
        console.log('TODO:', is)
      },
    })
    this._EasyMDE = mde

    if (_readonly37) {
      mde.codemirror.setOption('readOnly', true)
    } else {
      $createUploader(this._fieldValue__upload, null, (res) => this._mdeInsert(`![](${rb.baseUrl}/filex/img/${res.key})`))
      if (this.props.onView) this._mdeFocus()

      mde.codemirror.on('changes', () => {
        $setTimeout(
          () => {
            this.setState({ value: mde.value() }, () => this.checkValue())
          },
          200,
          'mde-update-event'
        )
      })
      mde.codemirror.on('paste', (_mde, e) => {
        const data = e.clipboardData || window.clipboardData
        if (data && data.items && data.files && data.files.length > 0) {
          $stopEvent(e, true)
          this._fieldValue__upload.files = data.files
          $(this._fieldValue__upload).trigger('change')
        }
      })
    }
  }

  _mdeInsert(text) {
    if (!this._EasyMDE) return
    const pos = this._EasyMDE.codemirror.getCursor()
    this._EasyMDE.codemirror.setSelection(pos, pos)
    this._EasyMDE.codemirror.replaceSelection(text)
    this._mdeFocus()
  }
  _mdeFocus() {
    if (!this._EasyMDE) return
    setTimeout(() => {
      this._EasyMDE.codemirror.focus()
      this._EasyMDE.codemirror.setCursor(this._EasyMDE.codemirror.lineCount(), 0) // cursor at end
    }, 100)
  }
}

class RbFormDateTime extends RbFormElement {
  renderElement() {
    const _readonly37 = this.state.readonly
    if (_readonly37) return super.renderElement()

    return (
      <div className="input-group has-append">
        <input
          ref={(c) => (this._fieldValue = c)}
          className={'form-control form-control-sm ' + (this.state.hasError ? 'is-invalid' : '')}
          title={this.state.hasError}
          type="text"
          value={this.state.value || ''}
          onChange={(e) => this.handleChange(e, !_readonly37)}
          placeholder={this._placeholderw}
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
    const _readonly37 = this.state.readonly

    if (destroy) {
      if (this.__datetimepicker) {
        this.__datetimepicker.datetimepicker('remove')
        this.__datetimepicker = null
      }
    } else if (!_readonly37) {
      const format = (this.props.datetimeFormat || this.props.dateFormat).replace(' (E)', '').replace('mm', 'ii').toLowerCase()
      let minView = 0
      let startView = 'month'
      if (format.length === 4 || format.length === 5) minView = startView = 'decade' // 年
      else if (format.length === 7 || format.length === 8) minView = startView = 'year' // 年-月
      else if (format.length === 10 || format.length === 11) minView = 'month' // 年-月-日
      else if (format.length === 13 || format.length === 14) minView = 'day' // 年-月-日 时

      const that = this
      this.__datetimepicker = $(this._fieldValue)
        .datetimepicker({
          format: (format || 'yyyy-mm-dd hh:ii:ss').replace(':ss', ':00'),
          minView: minView,
          startView: startView,
          pickerPosition: this._getAutoPosition(),
          minuteStep: window.__LAB_MINUTESTEP || 5,
          todayBtn: true,
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

  componentDidMount() {
    super.componentDidMount()
    // 表单计算
    if (this.props.calcFormula && !this.props.onView) __calcFormula(this)
  }
}

class RbFormTime extends RbFormDateTime {
  constructor(props) {
    super(props)
    this._icon = 'time'
  }

  onEditModeChanged(destroy) {
    const _readonly37 = this.state.readonly

    if (destroy) {
      super.onEditModeChanged(destroy)
    } else if (!_readonly37) {
      const format = (this.props.timeFormat || 'hh:ii:ss').replace('mm', 'ii').toLowerCase()
      const minView = format.length === 2 ? 1 : 0

      const that = this
      this.__datetimepicker = $(this._fieldValue)
        .datetimepicker({
          format: format.replace(':ss', ':00'),
          startView: 1,
          minView: minView,
          maxView: 1,
          pickerPosition: this._getAutoPosition(),
          minuteStep: window.__LAB_MINUTESTEP || 5,
          title: $L('选择时间'),
          todayBtn: false,
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
    if (window.__LAB_FILE_MAXUPLOAD > 9) this.__maxUpload = window.__LAB_FILE_MAXUPLOAD

    // 1=默认, 2=拍摄, 3=1+2
    this._captureType = 0
    if (props.imageCapture) this._captureType += 2
    if (props.imageCaptureDef) this._captureType += 1
    if (this._captureType === 0) this._captureType = 1
  }

  renderElement() {
    const _readonly37 = this.state.readonly
    const value = this.state.value || []
    const showUpload = value.length < this.__maxUpload && !_readonly37

    if (value.length === 0) {
      if (_readonly37) {
        return (
          <div className="form-control-plaintext text-muted">
            <i className="mdi mdi-information-outline" /> {$L('只读')}
          </div>
        )
      }
    }

    return (
      <div className="img-field" ref={(c) => (this._$dropArea = c)}>
        <span className="img-field-show">
          {value.map((item, idx) => {
            return (
              <span key={item} data-key={item}>
                <a title={$fileCutName(item)} className="img-thumbnail img-upload" onClick={() => this._filePreview(value, idx)}>
                  <img src={this._formatUrl(item)} alt="IMG" />
                  {!_readonly37 && (
                    <b title={$L('移除')} onClick={(e) => this.removeItem(item, e)}>
                      <span className="zmdi zmdi-close" />
                    </b>
                  )}
                </a>
              </span>
            )
          })}
        </span>
        <span title={$L('拖动或点击选择图片。需要 %s 个', `${this.__minUpload}~${this.__maxUpload}`)} className={`img-field-btn ${!showUpload && 'hide'}`}>
          <input ref={(c) => (this._fieldValue__input = c)} type="file" className="inputfile" id={this._htmlid} accept="image/*" multiple data-updir={this.props.fileUpdir || null} />
          <label htmlFor={this._htmlid} className="img-thumbnail img-upload" onClick={(e) => this._fileClick(e)}>
            {this._captureType === 2 ? <span className="mdi mdi-camera down-2" /> : <span className="zmdi zmdi-image-alt down-2" />}
          </label>
          {this._captureType === 3 && (
            <RF>
              <label className="dropdown-toggle" data-toggle="dropdown">
                <i className="icon zmdi zmdi-chevron-down" />
              </label>
              <div className="dropdown-menu dropdown-menu-sm">
                <a className="dropdown-item" onClick={() => this._fileClick(null, 2)}>
                  <i className="icon mdi mdi-camera" /> {$L('拍摄')}
                </a>
              </div>
            </RF>
          )}
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
        <span className="img-field-show">
          {value.map((item, idx) => {
            return (
              <span key={item}>
                <a title={$fileCutName(item)} onClick={() => this._filePreview(value, idx)} className="img-thumbnail img-upload zoom-in">
                  <img src={this._formatUrl(item)} alt="IMG" />
                </a>
              </span>
            )
          })}
        </span>
      </div>
    )
  }

  _formatUrl(urlKey) {
    if (urlKey.startsWith('http://') || urlKey.startsWith('https://')) return urlKey
    else return `${rb.baseUrl}/filex/img/${urlKey}?imageView2/2/w/100/interlace/1/q/100`
  }

  _filePreview(urlKey, idx) {
    const p = parent || window
    p.RbPreview.create(urlKey, idx)
  }

  _fileClick(e, forceType) {
    if (this._captureType === 2 || forceType === 2) {
      e && $stopEvent(e, true)
      if (rb.commercial < 1) {
        RbHighbar.error(WrapHtml($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
        return
      }

      const w = $(window).width() <= 1280 ? 768 : 1024
      renderRbcomp(
        <MediaCapturer
          title={$L('拍摄')}
          width={w}
          useWhite
          disposeOnHide
          type={this._captureTypeMedia || 'image'}
          forceFile
          watermark={window.__LAB_CAPTUREWATERMARK}
          callback={(fileKey) => {
            const paths = this.state.value || []
            if (paths.length < this.__maxUpload) {
              paths.push(fileKey)
              this.handleChange({ target: { value: paths } }, true)
            }
          }}
        />
      )
    }
    // else: this._captureType=1
  }

  onEditModeChanged(destroy) {
    if (destroy) {
      // NOOP
    } else {
      if (!this._fieldValue__input) {
        console.log('No element `_fieldValue__input` defined :', this.props.field)
        return
      }

      $multipleUploader(this._fieldValue__input, (res) => {
        const paths = this.state.value || []
        // 最多上传，多余忽略
        if (paths.length < this.__maxUpload) {
          let hasByName = $fileCutName(res.key)
          hasByName = paths.find((x) => $fileCutName(x) === hasByName)
          if (!hasByName) {
            paths.push(res.key)
            this.handleChange({ target: { value: paths } }, true)
          }
        }
      })

      // 拖拽上传
      if (this._$dropArea && (this._captureType === 1 || this._captureType === 3)) {
        const that = this
        $dropUpload(this._$dropArea, function (files) {
          if (!files || files.length === 0) return false
          that._fieldValue__input.files = files
          $(that._fieldValue__input).trigger('change')
        })
      }

      // v4.1 拖动位置
      if (this._$dropArea) {
        const that = this
        const $sortable = $(this._$dropArea)
          .find('>span:eq(0)')
          .sortable({
            axis: 'x',
            containment: 'parent',
            cursor: 'move',
            forcePlaceholderSize: true,
            forceHelperSize: true,
            stop: function () {
              let s = []
              $sortable.find('>[data-key]').each(function () {
                s.push($(this).data('key'))
              })
              that.handleChange({ target: { value: s } }, true)
            },
          })
          .disableSelection()
      }
    }
  }

  removeItem(item, e) {
    e && $stopEvent(e, true)
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

    // 照片, 视频
    if (this._captureType >= 2) {
      let _fileSuffix = props.fileSuffix || 'image/*; video/*'
      const img = _fileSuffix.includes('image/*')
      const vid = _fileSuffix.includes('video/*')
      if (img && vid) this._captureTypeMedia = '*'
      else if (img) this._captureTypeMedia = 'image'
      else if (vid) this._captureTypeMedia = 'video'
    }
  }

  renderElement() {
    const _readonly37 = this.state.readonly
    const value = this.state.value || []
    const showUpload = value.length < this.__maxUpload && !_readonly37

    if (value.length === 0 && _readonly37) {
      return (
        <div className="form-control-plaintext text-muted">
          <i className="mdi mdi-information-outline" /> {$L('只读')}
        </div>
      )
    }

    return (
      <div className="file-field" ref={(c) => (this._$dropArea = c)}>
        <span className="file-field-show">
          {value.map((item) => {
            const fileName = $fileCutName(item)
            return (
              <div key={item} data-key={item} className="img-thumbnail" title={fileName} onClick={() => this._filePreview(item)}>
                {this._renderFileIcon(fileName, item)}
                {!_readonly37 && (
                  <b title={$L('移除')} onClick={(e) => this.removeItem(item, e)}>
                    <span className="zmdi zmdi-close" />
                  </b>
                )}
              </div>
            )
          })}
        </span>
        <div className={`file-select ${showUpload ? '' : 'hide'}`}>
          <input
            type="file"
            className="inputfile"
            ref={(c) => (this._fieldValue__input = c)}
            id={this._htmlid}
            accept={this.props.fileSuffix || null}
            multiple
            data-updir={this.props.fileUpdir || null}
          />
          <label htmlFor={this._htmlid} title={$L('拖动或点击选择文件。需要 %s 个', `${this.__minUpload}~${this.__maxUpload}`)} className="btn-secondary" onClick={(e) => this._fileClick(e)}>
            {this._captureType === 2 ? <span className="mdi mdi-camera" /> : <span className="zmdi zmdi-upload" />}
            <span className="ml-1">{$L('上传文件')}</span>
          </label>
          {this._captureType === 3 && (
            <RF>
              <label className="dropdown-toggle btn-secondary" data-toggle="dropdown">
                <i className="icon zmdi zmdi-chevron-down" />
              </label>
              <div className="dropdown-menu dropdown-menu-sm">
                <a className="dropdown-item" onClick={() => this._fileClick(null, 2)}>
                  <i className="icon mdi mdi-camera" /> {$L('拍摄')}
                </a>
              </div>
            </RF>
          )}
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
          const fileName = $fileCutName(item)
          return (
            <a key={item} title={fileName} onClick={() => this._filePreview(item)} className="img-thumbnail">
              {this._renderFileIcon(fileName, item)}
            </a>
          )
        })}
      </div>
    )
  }

  _renderFileIcon(fileName, file) {
    const isImage = $isImage(fileName)
    return (
      <RF>
        <i className={`file-icon ${isImage && 'image'}`} data-type={$fileExtName(fileName)}>
          {isImage && <img src={this._formatUrl(file)} />}
        </i>
        <span>{fileName}</span>
      </RF>
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
    this._options = options.filter((item) => {
      return item.hide !== true || (props.value && item.id === props.value)
    })

    this._isShowRadio39 = props.showStyle === '10'
    this._htmlid = `${props.field}-${$random()}-`
  }

  renderElement() {
    if (this._options.length === 0) {
      return <div className="form-control-plaintext text-danger">{$L('未配置')}</div>
    }

    const _readonly37 = this.state.readonly

    if (this._isShowRadio39) {
      return (
        <div ref={(c) => (this._fieldValue = c)} className="mt-1">
          {this._options.map((item) => {
            return (
              <label key={item.id} className="custom-control custom-radio custom-control-inline mb-1">
                <input
                  className="custom-control-input"
                  name={this._htmlid}
                  type="radio"
                  checked={this.state.value === item.id}
                  onChange={() => this.setValue(item.id)}
                  disabled={_readonly37 || item.hide}
                />
                <span className="custom-control-label">{item.text}</span>
              </label>
            )
          })}
        </div>
      )
    }

    return (
      <select ref={(c) => (this._fieldValue = c)} className="form-control form-control-sm" defaultValue={this.state.value || ''} disabled={_readonly37}>
        <option value="" />
        {this._options.map((item) => {
          return (
            <option key={`${this._htmlid}${item.id}`} value={item.id} disabled={$isSysMask(item.text) || item.hide}>
              {item.text}
            </option>
          )
        })}
      </select>
    )
  }

  renderViewElement() {
    return super.renderViewElement(__findOptionText(this.props.options, this.state.value, true))
  }

  onEditModeChanged(destroy, fromReadonly41) {
    if (destroy) {
      if (fromReadonly41 && this.__select2) {
        $(this._fieldValue).attr('disabled', true)
      } else {
        super.onEditModeChanged(destroy)
      }
    } else if (fromReadonly41 && this.__select2) {
      $(this._fieldValue).attr('disabled', false)
    } else {
      if (this._isShowRadio39) {
        // Nothings
      } else {
        this.__select2 = $(this._fieldValue).select2({
          placeholder: $L('选择%s', this.props.label),
        })

        const that = this
        this.__select2.on('change', function (e) {
          const val = e.target.value
          that.handleChange({ target: { value: val } }, true)
        })

        const _readonly37 = this.state.readonly
        if (_readonly37) $(this._fieldValue).attr('disabled', true)
      }
    }
  }

  isValueUnchanged() {
    if (this._isNew === true) return false
    return super.isValueUnchanged()
  }

  setValue(val) {
    if (val && typeof val === 'object') val = val.id
    if (this._isShowRadio39) {
      this.handleChange({ target: { value: val } }, true)
    } else {
      this.__select2 && this.__select2.val(val).trigger('change')
    }
  }
}

class RbFormReference extends RbFormElement {
  constructor(props) {
    super(props)
    this._hasDataFilter = props.referenceDataFilter && (props.referenceDataFilter.items || []).length > 0
  }

  renderElement() {
    const _readonly37 = this.state.readonly
    const quickNew = this.props.referenceQuickNew && !this.props.onView

    return (
      <div className="input-group has-append">
        <select
          ref={(c) => (this._fieldValue = c)}
          className="form-control form-control-sm"
          title={this._hasDataFilter ? $L('当前字段已启用数据过滤') : null}
          multiple={this._multiple === true}
          disabled={_readonly37}
        />
        {!_readonly37 && (
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

    if (typeof value === 'string' || !value.id) {
      return (
        <div className="form-control-plaintext" ref={(c) => (this._fieldValue = c)}>
          {typeof value === 'string' ? value : value.text}
        </div>
      )
    }

    return (
      <div className="form-control-plaintext" ref={(c) => (this._fieldValue = c)}>
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
    if (this._isNew && props.value && props.value.id) {
      if (!this._disableAutoFillin()) {
        setTimeout(() => this.triggerAutoFillin(props.value.id), 200)
      }
    }
  }

  // fix: 4.0.2 #IC0GPI 明细复制时无需回填
  // fix: 4.0.4 #IC63LN 记录转换时无需回填
  _disableAutoFillin() {
    if (this.props._disableAutoFillin) return true

    try {
      // Form
      let $parent = this.props.$$$parent
      if ($parent.props._disableAutoFillin) return true
      // ProTable or Modal
      $parent = $parent.props.$$$parent
      if ($parent && $parent.props._disableAutoFillin) return true
    } catch (err) {
      console.log('_disableAutoFillin', err)
    }
    return false
  }

  onEditModeChanged(destroy, fromReadonly41) {
    if (destroy) {
      if (fromReadonly41 && this.__select2) {
        $(this._fieldValue).attr('disabled', true)
      } else {
        super.onEditModeChanged(destroy)
      }
    } else if (fromReadonly41 && this.__select2) {
      $(this._fieldValue).attr('disabled', false)
    } else {
      this.__select2 = $initReferenceSelect2(this._fieldValue, {
        name: this.props.field,
        label: this.props.label,
        entity: this.props.entity,
        wrapQuery: (query) => {
          // v4.1 附加过滤条件支持从表单动态取值
          const $$$parent = this.props.$$$parent
          if (this.props.referenceDataFilter && $$$parent) {
            let varRecord = $$$parent.getFormData ? $$$parent.getFormData() : $$$parent.__ViewData
            if (varRecord) {
              // FIXME 太长的值过滤，以免 URL 超长
              for (let k in varRecord) {
                if (varRecord[k] && (varRecord[k] + '').length > 100) {
                  delete varRecord[k]
                  console.log('Ignore large value of field :', k, varRecord[k])
                }
              }
              varRecord['metadata.entity'] = $$$parent.props.entity
              query.varRecord = JSON.stringify(varRecord)
            }
          }

          const cascadingValue = this._getCascadingFieldValue()
          if (cascadingValue) query.cascadingValue = cascadingValue
          // 4.1.3
          let val = this.state.value
          if (val && typeof val === 'object') val = val.id
          if (val) query._top = val

          console.log('Reference query:', query)
          return query
        },
        placeholder: this._placeholderw,
        templateResult: $select2OpenTemplateResult,
      })

      // 先 setValue
      const val = this.state.value
      if (val) this.setValue(val)

      // 再监听
      const that = this
      this.__select2.on('change', function (e) {
        const v = $(e.target).val()
        if (v && typeof v === 'string') {
          __addRecentlyUse(v)
          that.triggerAutoFillin(v)
        }

        that.handleChange({ target: { value: v } }, true)
      })

      const _readonly37 = this.state.readonly
      if (_readonly37) $(this._fieldValue).attr('disabled', true)
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
    const props = this.props
    if (typeof props.getCascadingFieldValue === 'function') {
      return props.getCascadingFieldValue(this)
    }

    let $$$parent = props.$$$parent

    // v3.3.2 在多级级联中会同时存在父子级
    let cascadingField
    if (props._cascadingFieldParent) {
      cascadingField = props._cascadingFieldParent.split('$$$$')[0]
    } else if (props._cascadingFieldChild) {
      cascadingField = props._cascadingFieldChild.split('$$$$')[0]
      // v3.3.3 明细作为子级时不控制，因为选择后明细关联字段会清空
      // v3.9 开始控制，同时主记录中父级字段修改时不再清空明细（视图中还不能控制）
      if (cascadingField && cascadingField.includes('.') && $$$parent._ProTables) {
        const ef = cascadingField.split('.')
        const pt = $$$parent._ProTables[ef[0]]

        let vvv = []
        const ptForms = pt.getInlineForms()
        ptForms &&
          ptForms.forEach((F) => {
            const fieldComp = F.getFieldComp(ef[1])
            let v = fieldComp ? fieldComp.getValue() : null
            // N2N
            if (v && Array.isArray(v)) {
              let temp = []
              v.forEach((item) => {
                if (item.id) temp.push(item.id)
                else temp.push(item)
              })
              v = temp.join(',')
            }
            v = v ? v.id || v : null
            if (v) vvv.push(v)
          })
        return vvv.length > 0 ? vvv.join(',') : null
      }
    }
    if (!cascadingField) return null

    // v2.10 明细中使用主表单
    if ($$$parent._InlineForm && (props._cascadingFieldParent || '').includes('.')) {
      $$$parent = $$$parent.props.$$$main
      cascadingField = cascadingField.split('.')[1]
    }

    let v
    if (this.props.onView) {
      v = ($$$parent.__ViewData || {})[cascadingField]

      // v2.10 无值时使用后台值
      if (!v && props._cascadingFieldParentValue) {
        v = { id: props._cascadingFieldParentValue }
      }
    } else {
      const fieldComp = $$$parent.refs[`fieldcomp-${cascadingField}`]
      v = fieldComp ? fieldComp.getValue() : null

      // v2.10 无布局时使用后台值
      if (!fieldComp && props._cascadingFieldParentValue) {
        v = { id: props._cascadingFieldParentValue }
      }
    }

    // N2N
    if (v && Array.isArray(v)) {
      let temp = []
      v.forEach((item) => {
        if (item.id) temp.push(item.id)
        else temp.push(item)
      })
      v = temp.join(',')
    }
    return v ? v.id || v : null
  }

  // 字段回填
  triggerAutoFillin(value) {
    setTimeout(() => this._triggerAutoFillin(value), 400)
  }
  _triggerAutoFillin(value) {
    if (this.props.onView) return

    const id = value && typeof value === 'object' ? value.id : value
    // fix:4.0.4 死循环
    this.__infiniteLoop = this.__infiniteLoop || {}
    this.__infiniteLoop[id] = (this.__infiniteLoop[id] || 0) + 1
    if (this.__infiniteLoop[id] > 2) {
      console.log('Infinite loop [triggerAutoFillin] ...', id)
      return
    }
    setTimeout(() => {
      this.__infiniteLoop = {}
    }, 2000)

    const $$$form = this.props.$$$parent
    let formData = null
    if (this.props.fillinWithFormData) {
      formData = $$$form.getFormData()
      if ($$$form._InlineForm && $$$form.props.$$$main) {
        formData.$$$main = $$$form.props.$$$main.getFormData()
      }
    }

    const url = `/app/entity/extras/fillin-value?entity=${this.props.entity}&field=${this.props.field}&source=${id}`
    $.post(url, JSON.stringify(formData), (res) => {
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
      // fix:4.1.7
      if (typeof val === 'object') {
        const o = new Option(val.text, val.id, true, true)
        this.__select2.append(o).trigger('change')
      }
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

    // fix:4.1.2 哪个表单打开的
    window.referenceSearch__form = this.props.$$$parent
    if (window.referenceSearch__form && window.referenceSearch__form.__ViewData) window.referenceSearch__form = null

    let url = this._buildSearcherUrl()
    // v4.1 附加过滤条件字段变量
    if (this.props.referenceDataFilter) url += `&referenceDataFilter=${$random()}`

    if (this._ReferenceSearcher && this._ReferenceSearcher_Url === url) {
      this._ReferenceSearcher.show()
    } else {
      if (this._ReferenceSearcher) {
        this._ReferenceSearcher.destroy()
        this._ReferenceSearcher = null
      }
      this._ReferenceSearcher_Url = url

      // eslint-disable-next-line react/jsx-no-undef
      renderRbcomp(<ReferenceSearcher url={url} title={$L('选择%s', this.props.label)} useWhite maximize />, function () {
        that._ReferenceSearcher = this
      })
    }
  }

  _buildSearcherUrl() {
    return `${rb.baseUrl}/app/entity/reference-search?field=${this.props.field}.${this.props.entity}&cascadingValue=${this._getCascadingFieldValue() || ''}`
  }

  showSearcher_call(selected, that) {
    const id = selected[0]
    if ($(that._fieldValue).find(`option[value="${id}"]`).length > 0) {
      that.__select2.val(id).trigger('change')
    } else {
      $.get(`/commons/search/read-labels?ids=${id}`, (res) => {
        const _data = res.data || {}
        const o = new Option(_data[id], id, true, true)
        that.__select2.append(o).trigger('change')
      })
    }

    // v3.3 多个则添加明细行
    const $$$parent = this.props.$$$parent
    if (selected.length > 1 && $$$parent._InlineForm) {
      const _ProTable = $$$parent.props.$$$parent
      $.get(`/commons/search/read-labels?ids=${selected.join(',')}`, (res) => {
        const _data = res.data || {}
        for (let i = 1; i < selected.length; i++) {
          const id = selected[i]
          const v = {
            [this.props.field]: {
              id: id,
              text: _data[id],
            },
          }
          setTimeout(() => _ProTable.addNew(v), 20 * i)
        }
      })
    }
  }

  quickNew() {
    const e = this.props.referenceEntity
    RbFormModal.create({ title: $L('新建%s', e.entityLabel), entity: e.entity, icon: e.icon, postAfter: (id) => this.showSearcher_call([id], this) }, true)
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

    if (typeof value === 'string') {
      return (
        <div className="form-control-plaintext" ref={(c) => (this._fieldValue = c)}>
          {value}
        </div>
      )
    }

    return (
      <div className="form-control-plaintext multi-values" ref={(c) => (this._fieldValue = c)}>
        {value.map((item) => {
          return (
            <a key={item.id} href={`#!/View/${item.entity}/${item.id}`} onClick={this._clickView}>
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
    if (val && typeof val === 'object') val = val.join(',')
    super.handleChange({ target: { value: val } }, checkValue)
  }

  onEditModeChanged(destroy, fromReadonly41) {
    super.onEditModeChanged(destroy, fromReadonly41)

    if (!destroy && this.__select2) {
      this.__select2.on('select2:select', (e) => __addRecentlyUse(e.params.data.id))
    }
  }

  // @append = 追加模式
  setValue(val, append) {
    if (val && val.length > 0) {
      // fix:4.1.7
      if (Array.isArray(val)) {
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

        if (ids.length > 0) this.__select2.trigger('change')
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
}

// v4.1 任意引用
class RbFormAnyReference extends RbFormReference {
  constructor(props) {
    super(props)
    this._disableAutoFillin = true
  }

  renderElement() {
    const _readonly41 = this.state.readonly

    return (
      <div className="row">
        <div className="col-4 pr-0">
          <select className="form-control form-control-sm" ref={(c) => (this._$entity = c)} disabled={_readonly41}>
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
          <div className="input-group has-append">
            <select className="form-control form-control-sm" ref={(c) => (this._fieldValue = c)} disabled={_readonly41} />
            {!this.state.readonly && (
              <div className="input-group-append">
                <button className="btn btn-secondary" type="button" onClick={() => this.showSearcher()}>
                  <i className="icon zmdi zmdi-search" />
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    )
  }

  onEditModeChanged(destroy, fromReadonly41) {
    if (destroy) {
      if (fromReadonly41 && this.__select2) {
        $([this._$entity, this._fieldValue]).attr('disabled', true)
      } else {
        super.onEditModeChanged(destroy)
      }
    } else if (fromReadonly41 && this.__select2) {
      $([this._$entity, this._fieldValue]).attr('disabled', false)
    } else {
      const iv = this.state.value
      $.get('/commons/metadata/entities?detail=true', (res) => {
        let entities = res.data || []
        if (this.props.anyreferenceEntities) {
          const ae = this.props.anyreferenceEntities.split(',')
          if (ae.length > 0) {
            entities = entities.filter((item) => ae.includes(item.name))
          }
        }

        // #1 E
        this.setState({ entities: entities }, () => {
          this.__select2Entity = $(this._$entity).select2({
            placeholder: $L('无可用'),
            allowClear: false,
            templateResult: function (res) {
              const $span = $('<span class="icon-append"></span>').attr('title', res.text).text(res.text)
              const icon = entities.find((x) => x.entity === res.id)
              $(`<i class="icon zmdi zmdi-${icon ? icon.icon : 'texture'}"></i>`).appendTo($span)
              return $span
            },
          })
          if (iv) {
            let code = ~~(iv.id || iv).split('-')[0]
            let name = entities.find((item) => item.entityCode === code)
            if (name) {
              this.__select2Entity.val(name.entity).trigger('change')
              this._anyrefEntity = name.entity
            } else {
              this.__select2Entity.val(null).trigger('change')
            }
          }
          this.__select2Entity.on('change', (e) => {
            this._anyrefEntity = e.target.value
            if (!this._setValueStop) this.setValue(null)
          })

          // #2 R
          this.__select2 = $initReferenceSelect2(this._fieldValue, {
            name: this.props.field,
            label: this.props.label,
            entity: this.props.entity,
            placeholder: this._placeholderw,
            templateResult: $select2OpenTemplateResult,
            wrapQuery: (query) => {
              // 真实查询实体
              query.anyrefEntity = this._anyrefEntity
              return query
            },
          }).on('change', (e) => {
            if (this._setValueStop) {
              this._setValueStop = false
            } else {
              const v = $(e.target).val()
              this.handleChange({ target: { value: v } }, true)
            }
          })

          // #3 init
          if (iv) {
            this.setValue(iv, true)
          } else if (entities[0]) {
            this.__select2Entity.val(entities[0].name).trigger('change')
            this._anyrefEntity = entities[0].name
          }
        })
      })

      this.state.readonly && $([this._$entity, this._fieldValue]).attr('disabled', true)
    }
  }

  componentWillUnmount() {
    super.componentWillUnmount()

    if (this.__select2Entity) {
      this.__select2Entity.select2('destroy')
      this.__select2Entity = null
    }
  }

  setValue(val, init) {
    if (init) this._setValueStop = true

    // fix:4.1.7
    if (val && typeof val === 'object') {
      if (val.entity && val.entity !== $(this.__select2Entity).val()) {
        $(this.__select2Entity).val(val.entity).trigger('change')
      }
    }
    super.setValue(val)
  }

  // @Override
  _buildSearcherUrl() {
    return `${rb.baseUrl}/app/entity/reference-search?field=${this._anyrefEntity}Id.${this._anyrefEntity}`
  }
}

class RbFormClassification extends RbFormElement {
  renderElement() {
    const _readonly37 = this.state.readonly

    return (
      <div className="input-group has-append">
        <select ref={(c) => (this._fieldValue = c)} className="form-control form-control-sm" />
        {!_readonly37 && (
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
    let text = this.state.value
    if (text && text.color) {
      text = (
        <span className="badge" style={$tagStyle2(text.color)}>
          {text.text}
        </span>
      )
    } else if (text) {
      text = <span className="badge text-dark">{text.text}</span>
    }
    return super.renderViewElement(text)
  }

  onEditModeChanged(destroy, fromReadonly41) {
    if (destroy) {
      if (fromReadonly41 && this.__select2) {
        $(this._fieldValue).attr('disabled', true)
      } else {
        super.onEditModeChanged(destroy)
        this.__cached = null
        if (this.__selector) {
          this.__selector.hide(true)
          this.__selector = null
        }
      }
    } else if (fromReadonly41 && this.__select2) {
      $(this._fieldValue).attr('disabled', false)
    } else {
      this.__select2 = $initReferenceSelect2(this._fieldValue, {
        name: this.props.field,
        label: this.props.label,
        entity: this.props.entity,
        searchType: 'classification',
        templateResult: function (res) {
          const $span = $('<span class="code-append"></span>').attr('title', res.text).text(res.text)
          res.code && $(`<em>${res.code}</em>`).appendTo($span)
          return $span
        },
      })

      const value = this.state.value
      value && this._setClassificationValue(value)

      this.__select2.on('change', () => {
        const v = this.__select2.val()
        if (v) __addRecentlyUse(`${v}&type=d${this.props.classification}:${this.props.openLevel}`)
        this.handleChange({ target: { value: v } }, true)
      })

      const _readonly37 = this.state.readonly
      if (_readonly37) $(this._fieldValue).attr('disabled', true)
    }
  }

  isValueUnchanged() {
    const oldv = this.state.newValue === undefined ? (this.props.value || {}).id : (this.state.newValue || {}).id
    return $same(oldv, this.state.value)
  }

  setValue(val) {
    if (val) {
      // fix:4.1.7
      val.id && this._setClassificationValue(val)
    } else this.__select2.val(null).trigger('change')
  }

  showSelector = () => {
    if (this.__selector) this.__selector.show()
    else {
      const p = this.props
      const that = this
      renderRbcomp(
        <ClassificationSelector entity={p.$$$parent.state.entity} field={p.field} label={p.label} openLevel={p.openLevel} onSelect={(s) => this._setClassificationValue(s)} keepModalOpen />,
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
    this._htmlid = `${props.field}-${$random()}_`
    this._isShowSelect41 = props.showStyle === '10'
    this._options = (props.options || []).filter((item) => {
      if (props.value && props.value.id) {
        if ((props.value.id & item.mask) !== 0) return true
      }
      return item.hide !== true
    })
  }

  renderElement() {
    if (this._options.length === 0) {
      return <div className="form-control-plaintext text-danger">{$L('未配置')}</div>
    }

    const _readonly37 = this.state.readonly
    const maskValue = this._getMaskValue()

    if (this._isShowSelect41) {
      return (
        <select className="form-control form-control-sm" multiple ref={(c) => (this._fieldValue = c)} disabled={_readonly37}>
          {this._options.map((item) => {
            return (
              <option key={item.mask} value={item.mask} disabled={$isSysMask(item.text) || item.hide}>
                {item.text}
              </option>
            )
          })}
        </select>
      )
    }

    return (
      <div className="mt-1" ref={(c) => (this._fieldValue__wrap = c)}>
        {this._options.map((item) => {
          return (
            <label key={item.mask} className="custom-control custom-checkbox custom-control-inline">
              <input
                className="custom-control-input"
                name={this._htmlid}
                type="checkbox"
                checked={(maskValue & item.mask) !== 0}
                value={item.mask}
                onChange={this._changeValue}
                disabled={_readonly37 || $isSysMask(item.text) || item.hide}
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
    return (
      <div className="form-control-plaintext multi-values" ref={(c) => (this._fieldValue = c)}>
        {__findMultiTexts(this.props.options, maskValue, true)}
      </div>
    )
  }

  onEditModeChanged(destroy, fromReadonly41) {
    if (this._isShowSelect41) {
      if (destroy) {
        if (fromReadonly41 && this.__select2) {
          $(this._fieldValue).attr('disabled', true)
        } else {
          super.onEditModeChanged(destroy)
        }
      } else if (fromReadonly41 && this.__select2) {
        $(this._fieldValue).attr('disabled', false)
      } else {
        this.__select2 = $(this._fieldValue).select2({
          placeholder: $L('选择%s', this.props.label),
        })

        // init
        const maskValue = this._getMaskValue()
        if (maskValue) {
          let s = []
          this.props.options.forEach((o) => {
            if ((maskValue & o.mask) !== 0) s.push(o.mask + '')
          })
          this.__select2.val(s).trigger('change')
        }

        this.__select2.on('change', () => this._changeValue())
      }
    }
  }

  _changeValue = () => {
    let maskValue = 0
    if (this._isShowSelect41) {
      this.__select2.val().forEach((v) => (maskValue += ~~v))
    } else {
      $(this._fieldValue__wrap)
        .find('input:checked')
        .each(function () {
          maskValue += ~~$(this).val()
        })
    }

    this.handleChange({ target: { value: maskValue === 0 ? null : maskValue } }, true)
  }

  _getMaskValue() {
    const val = this.state.value
    if (!val) return 0
    return typeof val === 'object' ? val.id : val
  }

  setValue(val) {
    // eg. {id:3, text:["A", "B"]}
    if (val && typeof val === 'object') val = val.id || val
    if (this._isShowSelect41) {
      let s = []
      this.props.options &&
        this.props.options.forEach((o) => {
          if ((val & o.mask) !== 0) s.push(o.mask)
        })
      this.__select2 && this.__select2.val(s).trigger('change')
    } else {
      super.setValue(val)
    }
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
    const _readonly37 = this.state.readonly

    return (
      <div className="mt-1">
        <label className="custom-control custom-radio custom-control-inline mb-1">
          <input className="custom-control-input" name={`${this._htmlid}T`} type="radio" checked={this.state.value === 'T'} data-value="T" onChange={this._changeValue} disabled={_readonly37} />
          <span className="custom-control-label">{this._Options['T']}</span>
        </label>
        <label className="custom-control custom-radio custom-control-inline mb-1">
          <input className="custom-control-input" name={`${this._htmlid}F`} type="radio" checked={this.state.value === 'F'} data-value="F" onChange={this._changeValue} disabled={_readonly37} />
          <span className="custom-control-label">{this._Options['F']}</span>
        </label>
      </div>
    )
  }

  renderViewElement() {
    return super.renderViewElement(this.state.value ? this._Options[this.state.value] : null)
  }

  _changeValue = (e) => {
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
        <a
          className={`img-thumbnail zoom-in ${isbar && 'w-auto'}`}
          title={this.state.value}
          onClick={() => {
            RbAlert.create(
              <div className="mb-3 text-center">
                <img src={`${codeUrl}&w=${isbar ? 64 * 2 : 80 * 3}`} alt={this.state.value} style={{ maxWidth: '100%' }} />
                {!isbar && <div className="text-muted mt-2 mb-1 text-break text-bold">{this.state.value}</div>}
              </div>,
              {
                type: 'clear',
              }
            )
          }}>
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
    const _readonly37 = this.state.readonly

    return (
      <div className="img-field avatar">
        <span title={_readonly37 ? null : $L('选择头像')}>
          {!_readonly37 && <input ref={(c) => (this._fieldValue__input = c)} type="file" className="inputfile" id={this._htmlid} accept="image/*" />}
          <label htmlFor={this._htmlid} className="img-thumbnail img-upload" disabled={_readonly37}>
            <img src={this._formatUrl(this.state.value)} alt="Avatar" />
            {!_readonly37 && this.state.value && (
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
        <a className="img-thumbnail img-upload" style={{ cursor: 'default' }}>
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
  constructor(props) {
    super(props)
    this._autoLocation = props.locationAutoLocation && this._isNew && !props.value
  }

  renderElement() {
    const _readonly37 = this.state.readonly
    const lnglat = this._parseLnglat(this.state.value)

    if (_readonly37) {
      return (
        <RF>
          {super.renderElement(lnglat ? lnglat.text : null)}
          {this._autoLocation && (
            <em className="vflag">
              <i className="zmdi zmdi-pin-drop flash infinite slow fs-14" ref={(c) => (this._$icon = c)} />
            </em>
          )}
        </RF>
      )
    }

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
          placeholder={this._placeholderw}
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
        <div className="form-control-plaintext" ref={(c) => (this._fieldValue = c)}>
          {lnglat.text}
        </div>
        <div className="map-show">
          <BaiduMap lnglat={lnglat} ref={(c) => (this._BaiduMap = c)} disableScrollWheelZoom />
        </div>
      </div>
    ) : (
      <div className="form-control-plaintext" ref={(c) => (this._fieldValue = c)}>
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

  _parseLnglat(val) {
    if (!val) return null
    if (typeof val === 'object') return val

    const vals = val.split('$$$$')
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
            const val = lnglat && lnglat.text ? `${lnglat.text}$$$$${lnglat.lng || 0},${lnglat.lat || 0}` : null
            that.handleChange({ target: { value: val } }, true)
          }}
          useWhite
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

    // 自动定位
    this._autoLocation && this.reLocation()
  }

  componentWillUnmount() {
    super.componentWillUnmount()
    if (this._BaiduMapModal) {
      this._BaiduMapModal.destroy()
      this._BaiduMapModal = null
    }
  }

  // v4.2 重新定位
  reLocation() {
    $(this._$icon).addClass('animated')
    // eslint-disable-next-line no-undef
    $autoLocation((v) => {
      v = v && v.text ? `${v.text}$$$$${v.lng},${v.lat}` : null
      v && this.handleChange({ target: { value: v } }, true)

      if (this.props.readonly) $(this._$icon).remove()
      else $(this._$icon).removeClass('animated')
    })
  }
}

class RbFormSign extends RbFormElement {
  renderElement() {
    const _readonly37 = this.state.readonly
    const value = this.state.value

    return (
      <div className="img-field sign sign-edit">
        <span title={_readonly37 ? null : $L('签名')}>
          <label
            className="img-thumbnail img-upload"
            onClick={() => {
              if (!_readonly37) {
                this._openSignPad((v) => {
                  this.handleChange({ target: { value: v || null } }, true)
                })
              }
            }}
            disabled={_readonly37}>
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
      renderRbcomp(<SignPad onConfirm={onConfirm} />, function () {
        that._SignPad = this
      })
    }
  }
}

class RbFormTag extends RbFormElement {
  constructor(props) {
    super(props)

    this._initOptions()
    this.__maxSelect = props.tagMaxSelect || 9
  }

  renderElement() {
    const _readonly40 = this.state.readonly

    const keyName = `${this.state.field}-tag-`
    return (
      <select ref={(c) => (this._fieldValue = c)} className="form-control form-control-sm" multiple defaultValue={this._selected} disabled={_readonly40}>
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

    return (
      <div className="form-control-plaintext multi-values" ref={(c) => (this._fieldValue = c)}>
        {__findTagTexts(this.props.options, this.state.value)}
      </div>
    )
  }

  _initOptions() {
    const props = this.props

    let options = [...props.options]
    let selected = []
    if (this.state.value && this.state.value.length > 0) {
      let value = this.state.value
      if (typeof value === 'string') value = value.split('$$$$') // Save after

      value.forEach((name) => {
        selected.push(name)
        const found = props.options.find((x) => x.name === name)
        if (!found) options.push({ name: name })
      })
    } else if (this._isNew) {
      props.options.forEach((item) => {
        if (item.default) selected.push(item.name)
      })
    }
    this._options = options
    this._selected = selected
  }

  onEditModeChanged(destroy, fromReadonly41) {
    if (destroy) {
      if (fromReadonly41 && this.__select2) {
        $(this._fieldValue).attr('disabled', true)
      } else {
        super.onEditModeChanged(destroy)
        this._initOptions()
      }
    } else if (fromReadonly41 && this.__select2) {
      $(this._fieldValue).attr('disabled', false)
    } else {
      this.__select2 = $(this._fieldValue).select2({
        placeholder: this.props.readonlyw > 0 ? this._placeholderw : $L('输入%s', this.props.label),
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
    }
  }

  setValue(val) {
    if (val && typeof val === 'object') val = val.join('$$$$')
    super.setValue(val)

    // fix: v3.4.4
    if ($empty(val)) {
      this.__select2.val(null).trigger('change')
    } else {
      const names = this.__select2.val() || [] // fix:4.2
      val.split('$$$$').forEach((name) => {
        if (!names.includes(name)) {
          const o = new Option(name, name, true, true)
          this.__select2.append(o)
          names.push(name)
        }
      })
    }
  }
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

// 无权限读取的字段
class RbFormUnreadable extends RbFormElement {
  renderElement() {
    return <div className="form-control-plaintext text-muted">{$L('[无权限]')}</div>
  }
  renderViewElement() {
    return this.renderElement()
  }
}

// 分割线
class RbFormDivider extends React.Component {
  constructor(props) {
    super(props)
    this.state = { collapsed: false }
  }

  render() {
    if (this.props.breaked === true) {
      return <div className="form-line-breaked"></div>
    }

    return (
      <div className={`form-line hover ${this.state.collapsed && 'collapsed'}`} ref={(c) => (this._$formLine = c)}>
        <fieldset>
          <legend onClick={() => this._toggle()} title={$L('展开/收起')}>
            {this.props.label && <span>{this.props.label}</span>}
          </legend>
        </fieldset>
      </div>
    )
  }

  _toggle() {
    let collapsed = null
    let $next = $(this._$formLine)
    while (($next = $next.next()).length > 0) {
      if ($next.hasClass('form-line') || $next.hasClass('footer')) break
      $next.toggleClass('collapsed-hide')
      if (collapsed === null) collapsed = $next.hasClass('collapsed-hide')
    }
    this.setState({ collapsed })
  }
}

// 表单引用（仅视图）
class RbFormRefform extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    if (!this.props.refvalue) return null

    const verticalLayout42 = window.__LAB_VERTICALLAYOUT || this.state.verticalLayout === 1 || this.state.verticalLayout === 3
    return (
      <div className={`rbview-form form-layout refform ${verticalLayout42 && 'vertical38'}`} ref={(c) => (this._viewForm = c)}>
        {this.state.formComponent || 'LOADING'}
      </div>
    )
  }

  componentDidMount() {
    const v = this.props.refvalue
    const $$$parent = this.props.$$$parent
    // 避免循环嵌套死循环
    if (v && $$$parent && ($$$parent.__nestDepth || 0) < 3) {
      this._renderViewFrom({ id: v[0], entity: v[1] })
    }
  }

  _renderViewFrom(props) {
    $.get(`/app/${props.entity}/view-model?id=${props.id}&layout=${this.props.speclayout || ''}`, (res) => {
      // 有错误
      if (res.error_code > 0 || !!res.data.error) {
        const err = (res.data || {}).error || res.error_msg
        this.setState({ formComponent: <div className="text-danger">{err}</div> })
        return
      }

      this.__nestDepth = (this.props.$$$parent.__nestDepth || 0) + 1
      this.__ViewData = {}

      const VFORM = (
        <RF>
          <a title={$L('在新页面打开')} className="close open-in-new" href={`${rb.baseUrl}/app/redirect?id=${props.id}&type=newtab`} target="_blank">
            <i className="icon zmdi zmdi-open-in-new" />
          </a>
          <div className="row">
            {res.data.elements.map((item) => {
              if (![TYPE_DIVIDER, TYPE_REFFORM].includes(item.field)) this.__ViewData[item.field] = item.value
              item.$$$parent = this
              // eslint-disable-next-line no-undef
              return detectViewElement(item, props.entity)
            })}
          </div>
        </RF>
      )
      this.setState({ formComponent: VFORM })
    })
  }

  // for comp
  getValue() {}
  setValue() {}
}

// 确定元素类型
var detectElement = function (item, entity) {
  if (!item.key) {
    item.key = `field-${item.field === TYPE_DIVIDER || item.field === TYPE_REFFORM ? $random() : item.field}`
  }
  // v4.1-b5
  if (entity) {
    item.entity = entity
  }
  // 复写的字段组件
  if (entity && window._CustomizedForms) {
    const c = window._CustomizedForms.useFormElement(entity, item)
    if (c) return c
  }

  if (item.unreadable === true) {
    return <RbFormUnreadable {...item} />
  } else if (item.type === 'TEXT' || item.type === 'SERIES') {
    return <RbFormText {...item} />
  } else if (item.type === 'NTEXT') {
    return <RbFormNText {...item} />
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
    return <RbFormAnyReference {...item} />
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
  } else if (item.field === TYPE_REFFORM) {
    return <RbFormRefform {...item} />
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
  if (useColor) {
    if (o && o.color) {
      text = (
        <span className="badge" style={$tagStyle2(o.color)}>
          {text}
        </span>
      )
    } else {
      text = <span className="badge text-dark">{text}</span>
    }
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

    const text = (
      <span key={`tag-${item.name}`} style={$tagStyle2(item.color)}>
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

// 表单计算（视图下无效）
const __calcFormula = function (fieldComp) {
  const watchFields = fieldComp.props.calcFormula.match(/\{([a-z0-9]+)}/gi) || []
  const $$$parent = fieldComp.props.$$$parent

  const evalUrl = `/app/entity/extras/eval-calc-formula?entity=${fieldComp.props.entity}&field=${fieldComp.props.field}`
  setTimeout(() => {
    const calcFormulaValues = {}
    let _timer

    // init
    watchFields.forEach((item) => {
      const name = item.substr(1, item.length - 2)
      const c = $$$parent.refs[`fieldcomp-${name}`]
      if (c && !$empty(c.state.value)) {
        calcFormulaValues[name] = c.state.value
      } else if (item === '{NOW}') {
        // v3.7
        calcFormulaValues[name] = '{NOW}'
      }
    })

    // onchange
    $$$parent.onFieldValueChange((s) => {
      if (!watchFields.includes(`{${s.name}}`)) return false
      if (rb.env === 'dev') console.log('onFieldValueChange for calcFormula :', s, fieldComp.props.field)

      if ($empty(s.value)) delete calcFormulaValues[s.name]
      else calcFormulaValues[s.name] = s.value

      if (_timer) {
        clearTimeout(_timer)
        _timer = null
      }

      // v36
      _timer = setTimeout(() => {
        $.post(evalUrl, JSON.stringify(calcFormulaValues), (res) => {
          if (__isSameValue38(fieldComp.getValue(), res.data)) return false
          if (res.data) fieldComp.setValue(res.data)
          else fieldComp.setValue(null)
        })
      }, 300)
      return true
    })

    // 新建时
    if (fieldComp._isNew) {
      $.post(evalUrl, JSON.stringify(calcFormulaValues), (res) => {
        if (__isSameValue38(fieldComp.getValue(), res.data)) return false
        if (res.data) fieldComp.setValue(res.data)
        else fieldComp.setValue(null)
      })
    }
  }, 600) // delay for init
}
function __isSameValue38(a, b) {
  if ($same(a, b)) return true
  // fix: 3.9.4
  if ($regex.isDecimal(a) && $regex.isDecimal(b)) {
    try {
      // eslint-disable-next-line eqeqeq
      return parseFloat(a) == parseFloat(b)
    } catch (err) {
      // ignored
    }
  }
  return false
}
