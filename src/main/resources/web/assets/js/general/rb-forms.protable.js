/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global detectElement, TYPE_DIVIDER, TYPE_REFFORM */
/* eslint-disable no-unused-vars */

// ~~ 表格型表单

const _PT_COLUMN_MIN_WIDTH = 30
const _PT_COLUMN_MAX_WIDTH = 500
const _PT_COLUMN_DEF_WIDTH = 200
const _PT_COLUMN_WIDTH_PLUS = ['REFERENCE', 'N2NREFERENCE', 'ANYREFERENCE', 'CLASSIFICATION']

const _EXTCONFIG = window.__LAB40_PROTABLE_EXTCONFIG || {}

class ProTable extends React.Component {
  constructor(props) {
    super(props)
    this.state = { _counts: {}, _treeState: {} }
    this._extConf40 = _EXTCONFIG[this.props.entity.entity] || {}
  }

  render() {
    if (this.state.hasError) {
      return <RbAlertBox message={this.state.hasError} />
    }
    // 等待初始化
    if (!this.state.formFields) return null

    const formFields = this.state.formFields
    const readonly = this.props.$$$main.props.readonly
    // const fixedWidth = formFields.length <= 5
    const fixedWidth = false // v42
    const inlineForms = this.state.inlineForms || []
    const colActionClazz = `col-action ${this._initModel.detailsCopiable && 'has-copy-btn'} ${!fixedWidth && 'column-fixed'}`

    return (
      <div className={`protable rb-scroller ${!fixedWidth && 'column-fixed-pin'}`} ref={(c) => (this._$scroller = c)}>
        <table className={`table table-sm ${fixedWidth && 'table-fixed'}`}>
          <thead>
            <tr>
              <th className="col-index" />
              {formFields.map((item) => {
                if (item.field === TYPE_DIVIDER || item.field === TYPE_REFFORM) return null

                let colStyle2 = { minWidth: _PT_COLUMN_DEF_WIDTH }
                if (!fixedWidth) {
                  // v35,v38,v42
                  let _colspan = ~~(item.colspan || 2)
                  if (_colspan === 9) _colspan = 1.5
                  if (_colspan === 8) _colspan = 2.5
                  colStyle2.minWidth = (_PT_COLUMN_DEF_WIDTH / 2) * _colspan
                  if (_PT_COLUMN_WIDTH_PLUS.includes(item.type)) colStyle2.minWidth += 38 // btn
                }
                // v37 LAB
                if (item.width) {
                  colStyle2.width = item.width
                  colStyle2.minWidth = 'auto'
                }

                return (
                  <th key={item.field} data-field={item.field} style={colStyle2} className={item.nullable ? '' : 'required'}>
                    {item.label}
                    {item.tip && <i className="tipping zmdi zmdi-info-outline" title={item.tip} />}
                    <i className="dividing hide" />
                  </th>
                )
              })}
              {!readonly && <td className={colActionClazz} />}
            </tr>
          </thead>
          <tbody ref={(c) => (this._$tbody = c)}>
            {inlineForms.map((FORM, idx) => {
              const key = FORM.key
              return (
                <tr
                  key={`if-${key}`}
                  data-key={key}
                  onMouseDown={(e) => {
                    $(this._$tbody).find('tr.active').removeClass('active')
                    $(e.currentTarget).addClass('active')
                  }}>
                  <th className={`col-index ${!readonly && 'action'}`}>
                    <span>{idx + 1}</span>
                    {!readonly && (
                      <a title={$L('展开编辑')} onClick={() => this._expandLineForm(key)}>
                        <i className="mdi mdi-arrow-expand" />
                      </a>
                    )}
                  </th>
                  {FORM}
                  {!readonly && (
                    <td className={`col-action ${!fixedWidth && 'column-fixed'}`}>
                      {this._initModel.detailsCopiable && (
                        <button className="btn btn-light J_copy-detail" title={$L('复制')} onClick={() => this.copyLine(key)} disabled={readonly}>
                          <i className="icon zmdi zmdi-copy fs-13" />
                        </button>
                      )}
                      <button className="btn btn-light J_remove-detail" title={$L('移除')} onClick={() => this.removeLine(key)} disabled={readonly}>
                        <i className="icon zmdi zmdi-close fs-16" />
                      </button>
                    </td>
                  )}
                </tr>
              )
            })}
          </tbody>
          {this._extConf40.showCounts && (
            <tfoot className={inlineForms.length === 0 ? 'hide' : ''}>
              <tr>
                <th className="col-idx" />
                {formFields.map((item) => {
                  if (item.field === TYPE_DIVIDER || item.field === TYPE_REFFORM) return null

                  let v = this.state._counts[item.field]
                  if (item.type === 'DECIMAL') v = $formatNumber(v || 0, 2)
                  else if (item.type === 'NUMBER') v = $formatNumber(v || 0, 0)
                  return (
                    <th key={item.field} className="text-bold">
                      {v}
                    </th>
                  )
                })}
                {!readonly && <td className={colActionClazz} />}
              </tr>
            </tfoot>
          )}
        </table>

        {inlineForms.length === 0 && <div className="text-center text-muted mt-6">{$L('请添加明细')}</div>}
      </div>
    )
  }

  componentDidMount() {
    const entity = this.props.entity
    const initialValue = {
      '$MAINID$': this.props.mainid || '$MAINID$',
    }

    $.post(`/app/${entity.entity}/form-model?mainLayoutId=${this.props.mainLayoutId}&id=`, JSON.stringify(initialValue), (res) => {
      // 包含错误
      if (res.error_code > 0 || !!res.data.error) {
        const error = (res.data || {}).error || res.error_msg
        this.setState({ hasError: error })
        return
      }

      this._initModel = res.data // 新建用
      this.setState({ formFields: res.data.elements }, () => {
        $(this._$scroller).perfectScrollbar({
          suppressScrollY: true,
        })
        $(this._$scroller).find('thead .tipping').tooltip()
      })

      // v3.9 记录转换
      if (this.props.transDetails) {
        this.setLines(this.props.transDetails)
        this._deletesQuietly = this.props.transDetailsDelete
      }
      // 常规编辑
      else if (this.props.mainid) {
        $.get(`/app/${entity.entity}/detail-models?mainid=${this.props.mainid}`, (res) => {
          if (res.error_code === 0) {
            this.setLines(res.data || [])
          } else {
            RbHighbar.error($L('明细加载失败，请稍后重试'))
          }
        })
      }

      this._initDividing37()
    })
  }

  // prevProps, prevState, snapshot
  componentDidUpdate = () => this._componentDidUpdate()
  _componentDidUpdate() {
    if (!this._extConf40.showCounts || this._countsStateUpdate) return

    // 计算合计
    if (this._countsTimer) clearTimeout(this._countsTimer)
    this._countsTimer = setTimeout(() => {
      const _counts = {}
      const inlineForms = this.getInlineForms()
      inlineForms &&
        inlineForms.forEach((FORM) => {
          this.state.formFields.forEach((item) => {
            if (item.type === 'NUMBER' || item.type === 'DECIMAL') {
              const c = FORM.getFieldComp(item.field)
              if (c) {
                let v = c.getValue()
                if (!$empty(v)) {
                  v = $cleanNumber(v, true)
                  _counts[item.field] = (_counts[item.field] || 0) + v
                }
              }
            }
          })
        })

      this._countsStateUpdate = true
      this.setState({ _counts }, () => {
        this._countsStateUpdate = false
      })
    }, 400)
  }

  _initDividing37() {
    const $scroller = $(this._$scroller)
    const that = this
    $scroller.find('th .dividing').draggable({
      containment: $scroller,
      axis: 'x',
      helper: 'clone',
      stop: function (e, ui) {
        const field = $(e.target).parents('th').data('field')
        let left = ui.position.left - -10
        if (left < _PT_COLUMN_MIN_WIDTH) left = _PT_COLUMN_MIN_WIDTH
        else if (left > _PT_COLUMN_MAX_WIDTH) left = _PT_COLUMN_MAX_WIDTH

        const fields = that.state.formFields
        for (let i = 0; i < fields.length; i++) {
          if (fields[i].field === field) {
            fields[i].width = left
            break
          }
        }

        that.setState({ formFields: fields }, () => $scroller.perfectScrollbar('update'))
      },
    })
  }

  _expandLineForm(lineKey) {
    const F = this.getLineForm(lineKey)
    if (!F) return

    const that = this
    const props = {
      title: $L('编辑'),
      confirmText: $L('确定'),
      icon: this.props.entity.icon,
      entity: F.props.entity,
      id: F.props.id || null,
      initialFormModel: null,
      postBefore: function (data) {
        that._formdataRebuild(data, (res) => {
          const dataBuild = res.data.elements
          const dataUpdated = {}
          for (let name in data) {
            const c = dataBuild.find((x) => x.field === name)
            if (c && c.readonly !== true) dataUpdated[name] = c.value
          }
          F.updatetFormData(dataUpdated)

          // hide
          if (RbFormModal.__CURRENT35) RbFormModal.__CURRENT35.hide(true)
        })
        return false
      },
    }

    this._formdataRebuild(F.getFormData(), (res) => {
      props.initialFormModel = res.data
      RbFormModal.create(props, true)
    })
  }

  _formdataRebuild(data, cb) {
    const mainid = this.props.$$$main.props.id || '000-0000000000000000'
    $.post(`/app/entity/extras/formdata-rebuild?mainid=${mainid}&layoutId=${this.getLayoutId()}`, JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        typeof cb === 'function' && cb(res)
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }

  addNew(specFieldValues, index) {
    const model = $clone(this._initModel)
    if (specFieldValues) {
      model.elements.forEach((item) => {
        if (specFieldValues[item.field]) item.value = specFieldValues[item.field]
      })
    }
    this._addLine(model, index)
  }

  _addLine(model, index, _disableAutoFillin) {
    // 明细未配置或出错
    if (!model) {
      if (this.state.hasError) RbHighbar.create(this.state.hasError)
      return
    }

    const entityName = this.props.entity.entity
    const lineKey = `${entityName}-${model.id ? model.id : $random()}`
    const ref = React.createRef()
    const FORM = (
      <InlineForm entity={entityName} id={model.id} rawModel={model} $$$parent={this} $$$main={this.props.$$$main} key={lineKey} ref={ref} _componentDidUpdate={() => this._componentDidUpdate()}>
        {model.elements.map((item) => {
          return detectElement({ ...item, colspan: 4, _disableAutoFillin: _disableAutoFillin === true })
        })}
      </InlineForm>
    )

    const inlineForms = this.state.inlineForms || []
    if (index >= 0) inlineForms.splice(index, 0, FORM)
    else inlineForms.push(FORM)
    this.setState({ inlineForms: inlineForms }, () => {
      const refs = this._inlineFormsRefs || []
      refs.push(ref)
      this._inlineFormsRefs = refs
      this._onLineUpdated(lineKey)
    })
    return ref
  }

  copyLine(lineKey, index) {
    const F = this.getLineForm(lineKey)
    const data = F ? F.getFormData() : null
    if (!data) return

    delete data.metadata.id // force New
    this._formdataRebuild(data, (res) => this._addLine(res.data, index, true))
  }

  removeLine(lineKey) {
    if (!this.state.inlineForms) return
    const inlineForms = this.state.inlineForms.filter((x) => {
      if (x.key === lineKey && x.props.id) {
        const d = this._deletes || []
        d.push(x.props.id)
        this._deletes = d
      }
      return x.key !== lineKey
    })
    this.setState({ inlineForms }, () => this._onLineUpdated(lineKey))
  }

  setLines(models = []) {
    models.forEach((item, idx) => {
      setTimeout(() => this._addLine(item), idx * 20)
    })
  }

  isEmpty() {
    return !this.state.inlineForms || this.state.inlineForms.length === 0
  }

  clear() {
    this.state.inlineForms &&
      this.state.inlineForms.forEach((c) => {
        if (c.props.id) {
          const d = this._deletes || []
          d.push(c.props.id)
          this._deletes = d
        }
      })
    this.setState({ inlineForms: [] }, () => this._onLineUpdated())
  }

  // 新增/删除行时触发
  _onLineUpdated(lineKey) {
    if (this.props.$$$main && this.props.$$$main._onProTableLineUpdated) {
      this.props.$$$main._onProTableLineUpdated(lineKey, this)
    }
  }

  /**
   * 清空某字段值
   * @param {string} field
   */
  setFieldNull(field) {
    this.state.inlineForms &&
      this.state.inlineForms.forEach((c) => {
        const fieldComp = c.ref.current.refs[`fieldcomp-${field}`]
        fieldComp && fieldComp.setValue(null)
      })
  }

  // @Deprecated
  getLineForm(lineKey) {
    console.warn('@Deprecated : getLineForm')
    return this.getInlineForm(lineKey)
  }

  /**
   * 获取指定 InlineForm
   * @param {string} lineKey
   * @returns
   */
  getInlineForm(lineKey) {
    if (!this.state.inlineForms) return null
    const F = this.state.inlineForms.find((c) => c.key === lineKey)
    return F ? F.ref.current || null : null
  }

  /**
   * @returns
   */
  getInlineForms() {
    if (!this.state.inlineForms) return null
    let ff = []
    this.state.inlineForms.forEach((F) => {
      if (F && F.ref.current) ff.push(F.ref.current)
    })
    return ff
  }

  /**
   * 构建数据
   * @param {boolean} retAll 是否返回所有数据
   * @returns
   */
  buildFormData(retAll) {
    const datas = []
    let error = null

    this._inlineFormsRefs &&
      this._inlineFormsRefs.forEach((item) => {
        if (!item.current) return
        const d = item.current.buildFormData(retAll)

        if (!d || typeof d === 'string') {
          if (!error) error = d
        } else if (Object.keys(d).length > 0) {
          datas.push(d)
        }
      })

    if (error) {
      RbHighbar.create(error)
      return null
    }

    // 删除的
    this._deletes &&
      this._deletes.forEach((item) => {
        datas.push({
          metadata: {
            entity: this.props.entity.entity,
            id: item,
            delete: true,
          },
        })
      })
    // 记录转换删除的
    this._deletesQuietly &&
      this._deletesQuietly.forEach((item) => {
        datas.push({
          metadata: {
            entity: this.props.entity.entity,
            id: item,
            deleteQuietly: true,
          },
        })
      })

    return datas
  }

  getLayoutId() {
    return this._initModel ? this._initModel.layoutId : null
  }

  // --

  /**
   * 创建
   * @param {*} rest
   * @returns
   */
  static create(rest) {
    const _extConf40 = _EXTCONFIG[rest.entity.entity] || {}
    if (_extConf40.showTreeConfig || _extConf40.showCheckbox) {
      return <ProTableTree {...rest} showTreeConfig={_extConf40.showTreeConfig} showCheckbox={_extConf40.showCheckbox} />
    }
    return <ProTable {...rest} />
  }

  // Remove from v4.2
  // /**
  //  * 记录转换-明细导入
  //  * @param {*} transid
  //  * @param {*} formObject
  //  * @param {*} cb
  //  * @returns
  //  */
  // static detailImports(transid, formObject, cb) {
  //   const formData = formObject.getFormData()
  //   const mainid = formObject.props.id || null
  //   $.post(`/app/entity/extras/detail-imports?transid=${transid}&mainid=${mainid}`, JSON.stringify(formData), (res) => {
  //     if (res.error_code === 0) {
  //       if ((res.data || []).length === 0) RbHighbar.create($L('没有可导入的明细记录'))
  //       else typeof cb === 'function' && cb(res.data)
  //     } else {
  //       RbHighbar.error(res.error_msg)
  //     }
  //   })
  // }
}

class InlineForm extends RbForm {
  constructor(props) {
    super(props)
    this._InlineForm = true
    this._extConf40 = props.$$$parent._extConf40 || {}
  }

  render() {
    const rawModel = this.props.rawModel
    return (
      <RF>
        {this._extConf40.showCheckbox && (
          <td className="col-checkbox">
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline">
              <input className="custom-control-input" type="checkbox" />
              <i className="custom-control-label" />
            </label>
          </td>
        )}
        {this._extConf40.showTreeConfig && this._extConf40.showTreeConfig.parentField && (
          <td className="col-tree">
            {rawModel._treeNodeLevel >= 0 && (
              <a style={{ marginLeft: rawModel._treeNodeLevel * 9 }}>
                <span>{rawModel._treeNodeLevel + 1}</span>
                <i className="zmdi zmdi-chevron-right" />
              </a>
            )}
          </td>
        )}

        {this.props.children.map((fieldComp) => {
          if (fieldComp.props.field === TYPE_DIVIDER || fieldComp.props.field === TYPE_REFFORM) return null
          const key = `fieldcomp-${fieldComp.props.field}`
          return (
            <td key={key} ref={(c) => (this._$ref = c)}>
              {React.cloneElement(fieldComp, { $$$parent: this, ref: key })}
            </td>
          )
        })}
      </RF>
    )
  }

  _baseFormData() {
    const data = {}
    this.props.rawModel.elements.forEach((item) => {
      let val = item.value
      if (val) {
        if (item.type === 'N2NREFERENCE') {
          let ids = item.value.map((n) => {
            return n.id ? n.id : n
          })
          val = ids.join(',')
        } else {
          val = typeof val === 'object' ? val.id || val : val
        }

        data[item.field] = val || null
      }
    })
    return data
  }

  buildFormData(retAll) {
    const data = retAll ? this._baseFormData() : {}

    const $idx = $(this._$ref).parent().find('th.col-index').removeAttr('title')
    let error = null
    for (let k in this.__FormData) {
      const err = this.__FormData[k].error
      if (err) {
        error = err
        $idx.attr('title', err)
        break
      } else {
        data[k] = this.__FormData[k].value
      }
    }

    if (error) return error

    // 是否修改
    if (Object.keys(data).length > 0) {
      data.metadata = {
        entity: this.state.entity,
        id: this.state.id || null,
      }
    }
    return data
  }

  getFormData() {
    const data = this._baseFormData()
    // updated
    for (let k in this.__FormData) {
      const err = this.__FormData[k].error
      if (err) data[k] = null
      else data[k] = this.__FormData[k].value
    }

    data.metadata = {
      entity: this.state.entity,
      id: this.state.id || null,
    }
    return data
  }

  updatetFormData(data) {
    if (rb.env === 'dev') console.log('InlineForm update :', data)
    for (let name in data) {
      const c = this.getFieldComp(name)
      if (c) c.setValue(data[name])
    }
  }

  _onFieldValueChangeCall(field, value) {
    super._onFieldValueChangeCall(field, value)
    // v4.0
    typeof this.props._componentDidUpdate === 'function' && this.props._componentDidUpdate()
  }
}

// LAB 树状
class ProTableTree extends ProTable {
  render() {
    if (this.state.hasError) {
      return <RbAlertBox message={this.state.hasError} />
    }
    // 等待初始化
    if (!this.state.formFields) return null

    const formFields = this.state.formFields
    const readonly = this.props.$$$main.props.readonly
    const fixedWidth = false
    const inlineForms = this.state.inlineForms || []
    // v4.0-b3
    const stcParentField = (this._extConf40.showTreeConfig || {}).parentField
    const stcMaxNodeLevel = (this._extConf40.showTreeConfig || {}).maxNodeLevel || 99
    const colActionClazz = `col-action ${stcParentField && 'has-copy-btn'} ${!fixedWidth && 'column-fixed'}`

    return (
      <div className={`protable rb-scroller ${!fixedWidth && 'column-fixed-pin'}`} ref={(c) => (this._$scroller = c)}>
        <table className={`table table-sm ${fixedWidth && 'table-fixed'}`}>
          <thead>
            <tr>
              <th className="col-index" />
              {this._extConf40.showCheckbox && (
                <th className="col-checkbox">
                  <label className="custom-control custom-control-sm custom-checkbox custom-control-inline">
                    <input
                      className="custom-control-input"
                      type="checkbox"
                      onChange={(e) => {
                        $(this._$tbody).find('.col-checkbox input').prop('checked', e.target.checked)
                      }}
                    />
                    <i className="custom-control-label" />
                  </label>
                </th>
              )}
              {stcParentField && <th className="col-tree" />}

              {formFields.map((item) => {
                if (item.field === TYPE_DIVIDER || item.field === TYPE_REFFORM) return null

                let colStyle2 = { minWidth: _PT_COLUMN_DEF_WIDTH }
                if (!fixedWidth) {
                  // v35, v38
                  let _colspan = ~~(item.colspan || 2)
                  if (_colspan === 9) _colspan = 1.5
                  if (_colspan === 8) _colspan = 2.5
                  colStyle2.minWidth = (_PT_COLUMN_DEF_WIDTH / 2) * _colspan
                  if (_PT_COLUMN_WIDTH_PLUS.includes(item.type)) colStyle2.minWidth += 38 // btn
                }
                // v37 LAB
                if (item.width) {
                  colStyle2.width = item.width
                  colStyle2.minWidth = 'auto'
                }

                return (
                  <th key={item.field} data-field={item.field} style={colStyle2} className={item.nullable ? '' : 'required'}>
                    {item.label}
                    {item.tip && <i className="tipping zmdi zmdi-info-outline" title={item.tip} />}
                    <i className="dividing hide" />
                  </th>
                )
              })}
              <td className={colActionClazz} />
            </tr>
          </thead>
          <tbody ref={(c) => (this._$tbody = c)}>
            {inlineForms.map((FORM, idx) => {
              const key = FORM.key
              return (
                <tr key={`if-${key}`} data-key={key}>
                  <th className={`col-index ${!readonly && 'action'}`}>
                    <span>{idx + 1}</span>
                    {!readonly && (
                      <a title={$L('展开编辑')} onClick={() => this._expandLineForm(key)}>
                        <i className="mdi mdi-arrow-expand" />
                      </a>
                    )}
                  </th>
                  {FORM}
                  <td className={`col-action ${!fixedWidth && 'column-fixed'}`}>
                    {stcParentField && FORM.props.rawModel._treeNodeLevel + 1 < stcMaxNodeLevel && (
                      <button className="btn btn-light" title={$L('添加子级')} onClick={() => this.insertLine(key, idx + 1)} disabled={readonly}>
                        <i className="icon zmdi zmdi-plus fs-16" />
                      </button>
                    )}
                    <button className="btn btn-light" title={$L('移除')} onClick={() => this.removeLine(key)} disabled={readonly}>
                      <i className="icon zmdi zmdi-close fs-16" />
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
          {this._extConf40.showCounts && (
            <tfoot className={inlineForms.length === 0 ? 'hide' : ''}>
              <tr>
                <th className="col-idx" />
                {this._extConf40.showCheckbox && <td className="col-checkbox" />}
                {stcParentField && <td className="col-tree" />}
                {formFields.map((item) => {
                  if (item.field === TYPE_DIVIDER || item.field === TYPE_REFFORM) return null

                  let v = this.state._counts[item.field]
                  if (item.type === 'DECIMAL') v = $formatNumber(v || 0, 2)
                  else if (item.type === 'NUMBER') v = $formatNumber(v || 0, 0)
                  return (
                    <th key={item.field} className="text-bold">
                      {v}
                    </th>
                  )
                })}
                <td className={colActionClazz} />
              </tr>
            </tfoot>
          )}
        </table>

        {inlineForms.length === 0 && <div className="text-center text-muted mt-6">{$L('请添加明细')}</div>}
      </div>
    )
  }

  // 插入下级
  insertLine(parentLineKey, index) {
    const model = $clone(this._initModel)
    const PF = this.getInlineForm(parentLineKey)
    model.id = $random('000-', true, 20) // newVID
    model._treeNodeLevel = PF.props.rawModel._treeNodeLevel + 1
    // 父级ID
    const stc = this.props.showTreeConfig
    if (stc && stc.parentField) {
      // v4.2 默认本实体
      if (!stc.parentFieldRefEntityCode) stc.parentFieldRefEntityCode = this._initModel.entityMeta.entityCode
      const parentVId = PF.props.rawModel.id.replace('000-', stc.parentFieldRefEntityCode + '-')
      this._setValueInModel(model, stc.parentField, { id: parentVId, text: $L('父级') }, true)
    }
    this._addLine(model, index)
  }

  _addLine(model, index) {
    const stc = this.props.showTreeConfig
    if (stc) {
      if (!model.id) model.id = $random('000-', true, 20) // newVID
      model._treeNodeLevel = model._treeNodeLevel || 0
    }
    super._addLine(model, index)
  }

  setLines(models = []) {
    const stc = this.props.showTreeConfig
    if (stc && stc.parentField) {
      let root = []
      models.forEach((model) => {
        const p = this._getValueInModel(model, stc.parentField, true)
        if (!p) {
          if (!model.id) model.id = $random('000-', true, 20) // newVID
          model._treeNodeLevel = 0
          this._findNodes(model, models)
          root.push(model)
        }
      })

      let orders = []
      root.forEach((model) => {
        orders.push(model)
        this._orderNodes(model, orders)
      })
      models = orders
    }

    super.setLines(models)
  }

  _findNodes(parent, models) {
    const stc = this.props.showTreeConfig
    models.forEach((model) => {
      let p = this._getValueInModel(model, stc.parentField, true)
      if (p && (p === parent.id || p.substr(3) === (parent.id || '').substr(3))) {
        model._treeNodeLevel = parent._treeNodeLevel + 1
        // recursion
        parent._treeNodes = parent._treeNodes || []
        parent._treeNodes.push(model)
        this._findNodes(model, models)
      }
    })
  }
  _orderNodes(model, into) {
    if (model._treeNodes) {
      model._treeNodes.forEach((node) => {
        into.push(node)
        this._orderNodes(node, into)
      })
    }
  }
  _getValueInModel(model, fieldName, checkDeleted) {
    let found = model.elements.find((x) => x.field === fieldName)
    if (!found || !found.value) return null
    if (checkDeleted && found.value.text === '[DELETED]') return null
    return found.value.id || null
  }
  _setValueInModel(model, fieldName, value, readonly) {
    let found = model.elements.find((x) => x.field === fieldName)
    if (found) {
      if (readonly === true) found.readonly = true
      if (typeof value === 'object') found.value = value
      else found.value = { id: value, text: value }
    }
  }

  /**
   * 获取选中
   */
  getSelectedInlineForms() {
    const ff = []
    $(this._$tbody)
      .find('.col-checkbox input[type="checkbox"]:checked')
      .each((idx, c) => {
        let key = $(c).parents('tr').attr('data-key')
        let F = this.getInlineForm(key)
        if (F) ff.push(F)
      })
    return ff
  }

  buildFormData() {
    let datas = super.buildFormData(true) // 强制所有字段
    if (!datas) return datas

    let datas2 = []
    datas.forEach((d) => {
      if (d.metadata.delete && (d.metadata.id || '').startsWith('000-'));
      else datas2.push(d)
    })
    return datas2
  }
}
