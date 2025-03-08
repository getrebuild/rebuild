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
const _PT_COLUMN_WIDTH_PLUS = ['REFERENCE', 'N2NREFERENCE', 'CLASSIFICATION']

const _EXTCONFIG = window.__LAB40_PROTABLE_EXTCONFIG || {}

class ProTable extends React.Component {
  constructor(props) {
    super(props)
    this.state = { _counts: {}, _treeState: {} }
    this._extConf40 = _EXTCONFIG[this.props.entity.entity] || {}
  }

  render() {
    if (this.state.hasError) {
      // $('.detail-form-table .btn-group .btn').attr('disabled', true)
      return <RbAlertBox message={this.state.hasError} />
    }

    // 等待初始化
    if (!this.state.formFields) return null

    const formFields = this.state.formFields
    const readonly = this.props.$$$main.props.readonly
    const fixedWidth = formFields.length <= 5
    const inlineForms = this.state.inlineForms || []

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
              {this._extConf40.showTreeConfig && <th className="col-tree" />}
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
              <td className={`col-action ${this._initModel.detailsCopiable && 'has-copy-btn'} ${!fixedWidth && 'column-fixed'}`} />
            </tr>
          </thead>
          <tbody ref={(c) => (this._$tbody = c)}>
            <InlineFormTree readonly={readonly} fixedWidth={fixedWidth} inlineForms={inlineForms} extConf40={this._extConf40} _this={this} />
          </tbody>
          {this._extConf40.showCounts && (
            <tfoot className={inlineForms.length === 0 ? 'hide' : ''}>
              <tr>
                <th className="col-idx" />
                {this._extConf40.showCheckbox && <th className="col-checkbox" />}
                {this._extConf40.showTreeConfig && <th className="col-tree" />}
                {formFields.map((item) => {
                  if (item.field === TYPE_DIVIDER || item.field === TYPE_REFFORM) return null

                  let v = this.state._counts[item.field]
                  if (item.type === 'DECIMAL') v = (v || 0).toFixed(2)
                  else if (item.type === 'NUMBER') v = (v || 0).toFixed(0)
                  return (
                    <th key={item.field} className="text-bold">
                      {v}
                    </th>
                  )
                })}
                <th className="col-action" />
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

    $.post(`/app/${entity.entity}/form-model?id=`, JSON.stringify(initialValue), (res) => {
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
      // 正常编辑
      else if (this.props.mainid) {
        $.get(`/app/${entity.entity}/detail-models?mainid=${this.props.mainid}`, (res) => {
          if (res.error_code === 0) {
            let data = res.data || []
            if (this._extConf40.showTreeConfig && data.length > 0) {
              let stc = this._extConf40.showTreeConfig
              data = this._buildData2Tree(stc.parentField, stc.childField, data)
            }
            this.setLines(data)
          } else {
            RbHighbar.error($L('明细加载失败，请稍后重试'))
          }
        })
      }

      this._initDividing37()
    })

    setTimeout(() => {
      this.getSelectedInlineForms()
    }, 5000)
  }

  _buildData2Tree(parentField, childField, data) {
    // 获取字段值
    function _FIND(item, fieldName) {
      let field = item.elements.find((x) => x.field === fieldName)
      let value = field ? field.value : null
      return (typeof value === 'object' ? value.id : value) || '0'
    }

    let data2 = []
    // #1
    data.forEach((item) => {
      item._treeNodeKey = _FIND(item, parentField)
      item._treeNodeParentKey = _FIND(item, childField)
      item._id = $random('_id')
      data2.push(item)
    })
    // #2
    data2.forEach((item) => {
      let parent = data2.find((x) => item._treeNodeParentKey === x._treeNodeKey)
      if (parent) {
        if (!parent._treeChildren) parent._treeChildren = []
        parent._treeChildren.push(item._id)
      }
    })

    // #3
    let data3 = []
    data2.forEach((item) => {
      if (item._treeNodeParentKey === '0') {
        data3.push(item)
        // L2
        if (item._treeChildren) {
          item._treeChildren.forEach((child) => {
            let childItem = data2.find((x) => x._id === child)
            if (childItem) {
              childItem._treeNodeLevel = 1
              data3.push(childItem)
              // L3
              if (childItem._treeChildren) {
                childItem._treeChildren.forEach((child) => {
                  childItem = data2.find((x) => x._id === child)
                  if (childItem) {
                    childItem._treeNodeLevel = 2
                    data3.push(childItem)
                    // L4
                    if (childItem._treeChildren) {
                      childItem._treeChildren.forEach((child) => {
                        childItem = data2.find((x) => x._id === child)
                        if (childItem) {
                          childItem._treeNodeLevel = 3
                          data3.push(childItem)
                          // L5
                          if (childItem._treeChildren) {
                            childItem._treeChildren.forEach((child) => {
                              childItem = data2.find((x) => x._id === child)
                              if (childItem) {
                                childItem._treeNodeLevel = 4
                                data3.push(childItem)
                                // L6
                                if (childItem._treeChildren) {
                                  childItem._treeChildren.forEach((child) => {
                                    childItem = data2.find((x) => x._id === child)
                                    if (childItem) {
                                      childItem._treeNodeLevel = 5
                                      data3.push(childItem)
                                      // L7
                                    }
                                  })
                                }
                              }
                            })
                          }
                        }
                      })
                    }
                  }
                })
              }
            }
          })
        }
      }
    })

    data2.forEach((item) => {
      let found = data3.find((x) => x._id === item._id)
      if (!found) data3.push(item)
    })

    console.log(data2, data3)
    return data3
  }

  // prevProps, prevState, snapshot
  componentDidUpdate = () => this._componentDidUpdate()
  _componentDidUpdate() {
    const extConf40 = _EXTCONFIG[this.props.entity.entity] || {}
    if (!extConf40.showCounts || this._countsStateUpdate) return

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
                if (v === null || v === undefined || v === '');
                else {
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
    $.post(`/app/entity/extras/formdata-rebuild?mainid=${mainid}`, JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        typeof cb === 'function' && cb(res)
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }

  addNew(specFieldValues) {
    const model = $clone(this._initModel)
    if (specFieldValues) {
      model.elements.forEach((item) => {
        if (specFieldValues[item.field]) item.value = specFieldValues[item.field]
      })
    }
    this._addLine(model)
  }

  _addLine(model) {
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
          return detectElement({ ...item, colspan: 4 })
        })}
      </InlineForm>
    )

    const forms = this.state.inlineForms || []
    forms.push(FORM)
    this.setState({ inlineForms: forms }, () => {
      const refs = this._inlineFormsRefs || []
      refs.push(ref)
      this._inlineFormsRefs = refs
      this._onLineUpdated(lineKey)
    })
  }

  copyLine(lineKey) {
    const F = this.getLineForm(lineKey)
    const data = F ? F.getFormData() : null
    if (!data) return

    // force New
    delete data.metadata.id

    this._formdataRebuild(data, (res) => {
      this._addLine(res.data)
    })
  }

  removeLine(lineKey) {
    if (!this.state.inlineForms) return
    const forms = this.state.inlineForms.filter((x) => {
      if (x.key === lineKey && x.props.id) {
        const d = this._deletes || []
        d.push(x.props.id)
        this._deletes = d
      }
      return x.key !== lineKey
    })
    this.setState({ inlineForms: forms }, () => this._onLineUpdated(lineKey))
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

  // --

  /**
   * 导入明细
   * @param {*} transid
   * @param {*} form
   * @param {*} cb
   * @returns
   */
  static detailImports(transid, form, cb) {
    const formdata = form.getFormData()
    const mainid = form.props.id || null

    $.post(`/app/entity/extras/detail-imports?transid=${transid}&mainid=${mainid}`, JSON.stringify(formdata), (res) => {
      if (res.error_code === 0) {
        if ((res.data || []).length === 0) RbHighbar.create($L('没有可导入的明细记录'))
        else typeof cb === 'function' && cb(res.data)
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

class InlineFormTree extends React.Component {
  render() {
    return this.props.inlineForms.map((FORM, idx) => {
      return this.renderRow(FORM, idx)
    })
  }

  renderRow(FORM, idx) {
    const _this = this.props._this
    const readonly = this.props.readonly
    const fixedWidth = this.props.fixedWidth
    const extConf40 = this.props.extConf40

    const key = FORM.key
    const rawModel = FORM.props.rawModel
    return (
      <tr key={`if-${key}`} data-key={key}>
        <th className={`col-index ${!readonly && 'action'}`}>
          <span>{idx + 1}</span>
          {!readonly && (
            <a title={$L('展开编辑')} onClick={() => _this._expandLineForm(key)}>
              <i className="mdi mdi-arrow-expand" />
            </a>
          )}
        </th>
        {extConf40.showCheckbox && (
          <td className="col-checkbox">
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline">
              <input className="custom-control-input" type="checkbox" />
              <i className="custom-control-label" />
            </label>
          </td>
        )}
        {extConf40.showTreeConfig && (
          <td className="col-tree">
            <a className={`col-tree-level-${rawModel._treeNodeLevel || 0}`}>
              <span>{(rawModel._treeNodeLevel || 0) + 1}</span>
              <i className="zmdi zmdi-chevron-right" />
            </a>
          </td>
        )}
        {FORM}
        <td className={`col-action ${!fixedWidth && 'column-fixed'}`}>
          {_this._initModel.detailsCopiable && (
            <button className="btn btn-light" title={$L('复制')} onClick={() => _this.copyLine(key)} disabled={readonly}>
              <i className="icon zmdi zmdi-copy fs-14" />
            </button>
          )}
          <button className="btn btn-light" title={$L('移除')} onClick={() => _this.removeLine(key)} disabled={readonly}>
            <i className="icon zmdi zmdi-close fs-16 text-bold" />
          </button>
        </td>
      </tr>
    )
  }
}

class InlineForm extends RbForm {
  constructor(props) {
    super(props)
    this._InlineForm = true
  }

  render() {
    return (
      <RF>
        {this.props.children.map((fieldComp) => {
          if (fieldComp.props.field === TYPE_DIVIDER || fieldComp.props.field === TYPE_REFFORM) return null
          const refid = `fieldcomp-${fieldComp.props.field}`
          return (
            <td key={`td-${refid}`} ref={(c) => (this._$ref = c)}>
              {React.cloneElement(fieldComp, { $$$parent: this, ref: refid })}
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

  getTreeNodeKey() {
    return this.props.rawModel._treeNodeKey
  }
  getTreeNodeParentKey() {
    return this.props.rawModel._treeNodeParentKey
  }
}

// Excel 粘贴数据
class ExcelClipboardData extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    if (this.state.hasError) {
      return <div className="must-center text-danger">{this.state.hasError}</div>
    }
    if (!this.state.data) {
      let tips = $L('复制 Excel 单元格 Ctrl + V 粘贴')
      if ($.browser.mac) tips = tips.replace('Ctrl', 'Command')
      return (
        <div className="must-center text-muted">
          <div className="mb-2">
            <i className="mdi mdi-microsoft-excel" style={{ fontSize: 48 }} />
          </div>
          {tips}
        </div>
      )
    }

    return (
      <div className="rsheetb-table" ref={(c) => (this._$table = c)}>
        <div className="head-action">
          <span className="float-left">
            <h5 className="text-bold fs-14 m-0" style={{ paddingTop: 11 }}>
              {$L('请选择列字段')}
            </h5>
          </span>
          <span className="float-right">
            <button className="btn btn-primary" onClick={() => this._handleConfirm()} ref={(c) => (this._$btn = c)}>
              {$L('确定')}
            </button>
          </span>
          <div className="clearfix" />
        </div>
        {WrapHtml(this.state.data)}
      </div>
    )
  }

  componentDidMount() {
    const that = this
    function _init() {
      $(document).on('paste.csv-data', (e) => {
        let data
        try {
          // https://docs.sheetjs.com/docs/demos/local/clipboard/
          // https://docs.sheetjs.com/docs/api/utilities/html
          const c = e.originalEvent.clipboardData.getData('text/html')
          const wb = window.XLSX.read(c, { type: 'string' })
          const ws = wb.Sheets[wb.SheetNames[0]]
          data = window.XLSX.utils.sheet_to_html(ws, { id: 'rsheetb', header: '', footer: '', editable: true })

          // No content
          if (data && !$(data).text()) data = null
        } catch (err) {
          console.log('Cannot read csv-data from clipboardData', err)
        }

        if (data) {
          if (rb.env === 'dev') console.log(data)
          that.setState({ data: data, hasError: null }, () => that._tableAfter())
        } else {
          RbHighbar.createl('未识别到有效数据')
        }
      })
    }

    if (window.XLSX) _init()
    else $getScript('/assets/lib/charts/xlsx.full.min.js', setTimeout(_init, 200))
  }

  _tableAfter() {
    if (!this._$table) return

    const $table = $(this._$table).find('table').addClass('table table-sm table-bordered table-fixed')

    const fields = this.props.fields
    if (fields) {
      const len = $table.find('tbody>tr:eq(0)').find('td').length
      const $tr = $('<thead><tr></tr></thead>').appendTo($table).find('tr')
      for (let i = 0; i < len; i++) {
        const $th = $('<th><select></select></th>').appendTo($tr)
        fields.forEach((item) => {
          $(`<option value="${item.field}">${item.label}</option>`).appendTo($th.find('select'))
        })
        $th
          .find('select')
          .select2({
            placeholder: $L('无'),
          })
          .val(fields[i] ? fields[i].field : null)
          .trigger('change')
      }
    }
  }

  _handleConfirm() {
    const fields = []
    $(this._$table)
      .find('thead select')
      .each(function () {
        let fm = $(this).val() || null
        if (fm && fields.includes(fm)) fm = null
        fields.push(fm)
      })

    let noAnyFields = true
    for (let i = 0; i < fields.length; i++) {
      if (fields[i]) {
        noAnyFields = false
        break
      }
    }
    if (noAnyFields) return RbHighbar.createl('请至少选择一个列字段')

    const dataWithFields = []
    $(this._$table)
      .find('tbody tr')
      .each(function () {
        const L = {}
        $(this)
          .find('td')
          .each(function (idx) {
            const name = fields[idx]
            if (name) L[name] = $(this).text() || null
          })
        dataWithFields.push(L)
      })

    const $btn = $(this._$btn).button('loading')
    $.post(`/app/entity/extras/csvdata-rebuild?entity=${this.props.entity}&mainid=${this.props.mainid}`, JSON.stringify(dataWithFields), (res) => {
      if (res.error_code === 0) {
        this.props.onConfirm && this.props.onConfirm(res.data)
      } else {
        RbHighbar.error(res.error_msg)
      }
      $btn.button('reset')
    })
  }

  componentWillUnmount() {
    $(document).off('paste.csv-data')
  }
}

class ExcelClipboardDataModal extends RbModalHandler {
  render() {
    return (
      <RbModal title={$L('从 Excel 添加')} width="1000" className="modal-rsheetb" disposeOnHide maximize ref={(c) => (this._dlg = c)}>
        <ExcelClipboardData
          {...this.props}
          onConfirm={(data) => {
            this.props.onConfirm && this.props.onConfirm(data)
            this.hide()
          }}
        />
      </RbModal>
    )
  }
}
