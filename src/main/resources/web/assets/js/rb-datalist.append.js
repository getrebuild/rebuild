/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// ~~ 列表记录批量操作

class BatchOperator extends RbFormHandler {
  constructor(props) {
    super(props)
    this.state.dataRange = 2
  }

  render() {
    const _listRef = this.props.listRef
    const selectedRows = _listRef.getSelectedIds(true).length
    const pageRows = _listRef.state.rowsData.length
    const queryRows = _listRef.getLastQueryTotal()

    return (
      <RbModal title={this.state.title} disposeOnHide={true} ref={(c) => (this._dlg = c)}>
        <div className="form batch-form">
          <div className="form-group">
            <label className="text-bold">{$L('SelectDataRange')}</label>
            <div>
              {selectedRows > 0 && (
                <label className="custom-control custom-control-sm custom-radio mb-2">
                  <input className="custom-control-input" name="dataRange" type="radio" checked={~~this.state.dataRange === 1} value="1" onChange={this.handleChange} />
                  <span className="custom-control-label">
                    {$L('DatasSelected')} ({$L('XItem').replace('%d', selectedRows)})
                  </span>
                </label>
              )}
              <label className="custom-control custom-control-sm custom-radio mb-2">
                <input className="custom-control-input" name="dataRange" type="radio" checked={~~this.state.dataRange === 2} value="2" onChange={this.handleChange} />
                <span className="custom-control-label">
                  {$L('DatasPaged')} ({$L('XItem').replace('%d', pageRows)})
                </span>
              </label>
              <label className="custom-control custom-control-sm custom-radio mb-2">
                <input className="custom-control-input" name="dataRange" type="radio" checked={~~this.state.dataRange === 3} value="3" onChange={this.handleChange} />
                <span className="custom-control-label">
                  {$L('DatasQueryed')} ({$L('XItem').replace('%d', queryRows)})
                </span>
              </label>
              <label className="custom-control custom-control-sm custom-radio mb-1">
                <input className="custom-control-input" name="dataRange" type="radio" checked={~~this.state.dataRange === 10} value="10" onChange={this.handleChange} />
                <span className="custom-control-label">{$L('AllDatas')}</span>
              </label>
            </div>
          </div>
          {this.renderOperator()}
        </div>
        <div className="dialog-footer" ref={(c) => (this._btns = c)}>
          <a className="btn btn-link btn-space" onClick={this.hide}>
            {$L('Cancel')}
          </a>
          <button className="btn btn-primary btn-space" type="button" onClick={this.confirm}>
            {$L('Confirm')}
          </button>
        </div>
      </RbModal>
    )
  }

  getQueryData() {
    const qd = this.props.listRef.getLastQueryEntry()
    if (~~this.state.dataRange === 1) qd._selected = this.props.listRef.getSelectedIds(true).join('|')
    return qd
  }

  // 子类复写

  renderOperator() {}

  confirm = () => {}
}

// ~ 数据导出

// eslint-disable-next-line no-unused-vars
class DataExport extends BatchOperator {
  constructor(props) {
    super(props)
    this.state.title = $L('DataExport')
  }

  confirm = () => {
    this.disabled(true)
    $.post(`/app/${this.props.entity}/export/submit?dr=${this.state.dataRange}`, JSON.stringify(this.getQueryData()), (res) => {
      if (res.error_code === 0) {
        this.hide()
        window.open(`${rb.baseUrl}/filex/download/${res.data}?temp=yes`)
      } else {
        this.disabled(false)
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

// ~ 批量修改

// eslint-disable-next-line no-unused-vars
class BatchUpdate extends BatchOperator {
  constructor(props) {
    super(props)
    this.state.title = $L('BatchUpdate')
  }

  componentDidMount() {
    $.get(`/app/${this.props.entity}/batch-update/fields`, (res) => this.setState({ fields: res.data }))
  }

  renderOperator() {
    return (
      <div className="form-group">
        <label className="text-bold">{$L('UpdateContents')}</label>
        <div>
          <div className="batch-contents">
            {(this.state.updateContents || []).map((item) => {
              return (
                <div key={`update-${item.field}`}>
                  <div className="row">
                    <div className="col-4">
                      <a className="del" onClick={() => this.removeItem(item.field)} title={$L('Remove')}>
                        <i className="zmdi zmdi-close"></i>
                      </a>
                      <span className="badge badge-light">{this._fieldLabel(item.field)}</span>
                    </div>
                    <div className="col-2 pl-0 pr-0">
                      <span className="badge badge-light">{BUE_OPTYPES[item.op]}</span>
                    </div>
                    <div className="col-6">
                      {item.op !== 'NULL' && <span className="badge badge-light text-break text-left">{item.text || item.value}</span>}
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
          <div className="mt-2">
            {this.state.fields && <BatchUpdateEditor ref={(c) => (this._editor = c)} fields={this.state.fields} entity={this.props.entity} />}
            <div className="mt-1">
              <button className="btn btn-primary btn-sm btn-outline" onClick={this.addItem} type="button">
                + {$L('Add')}
              </button>
            </div>
          </div>
        </div>
      </div>
    )
  }

  _fieldLabel(fieldName) {
    const field = this.state.fields.find((item) => {
      return fieldName === item.name
    })
    return field ? field.label : `[${fieldName}.toUpperCase()]`
  }

  addItem = () => {
    const item = this._editor.buildItem()
    if (!item) return

    const contents = this.state.updateContents || []
    const found = contents.find((x) => {
      return item.field === x.field
    })
    if (found) {
      RbHighbar.create($L('UpdateFieldExists'))
      return
    }

    contents.push(item)
    this.setState({ updateContents: contents })
  }

  removeItem(fieldName) {
    const contents = []
    this.state.updateContents.forEach((item) => {
      if (fieldName !== item.field) contents.push(item)
    })
    this.setState({ updateContents: contents })
  }

  confirm = () => {
    if (!this.state.updateContents || this.state.updateContents.length === 0) {
      RbHighbar.create($L('PlsAddSome,UpdateContents'))
      return
    }
    const _data = { queryData: this.getQueryData(), updateContents: this.state.updateContents }
    // eslint-disable-next-line no-console
    if (rb.env === 'dev') console.log(JSON.stringify(_data))

    const that = this
    RbAlert.create($L('BatchUpdateConfirm'), {
      confirm: function () {
        this.hide()
        that.disabled(true)
        $.post(`/app/${that.props.entity}/batch-update/submit?dr=${that.state.dataRange}`, JSON.stringify(_data), (res) => {
          if (res.error_code === 0) {
            const mp = new Mprogress({ template: 1, start: true })
            that.__checkState(res.data, mp)
          } else {
            that.disabled(false)
            RbHighbar.error(res.error_msg)
          }
        })
      },
    })
  }

  __checkState(taskid, mp) {
    $.get(`/commons/task/state?taskid=${taskid}`, (res) => {
      if (res.error_code === 0) {
        if (res.data.hasError) {
          mp && mp.end()
          RbHighbar.error(res.data.hasError)
          return
        }

        const cp = res.data.progress
        if (cp >= 1) {
          mp && mp.end()
          $(this._btns).find('.btn-primary').text($L('Finished'))
          RbHighbar.success($L('BatchUpdateSuccessTips').replace('%d', res.data.succeeded))
          setTimeout(() => {
            this.hide()
            window.RbListPage && window.RbListPage.reload()
          }, 500)
        } else {
          mp && mp.set(cp)
          setTimeout(() => {
            this.__checkState(taskid, mp)
          }, 1000)
        }
      }
    })
  }
}

const BUE_OPTYPES = {
  SET: $L('BatchUpdateOpSET'),
  NULL: $L('BatchUpdateOpNULL'),
  // TODO 支持更多修改模式
  // PREFIX: $L('BatchUpdateOpPREFIX'),
  // SUFFIX: $L('BatchUpdateOpSUFFIX'),
  // PLUS: $L('CalcPlus'),
  // MINUS: $L('CalcMinus'),
}

// ~ 批量修改编辑器
class BatchUpdateEditor extends React.Component {
  state = { ...this.props, selectOp: 'SET' }

  componentDidMount() {
    const field2s = $(this._field)
      .select2({
        allowClear: false,
      })
      .on('change', () => {
        this.setState({ selectField: field2s.val() })
      })
    const op2s = $(this._op)
      .select2({
        allowClear: false,
      })
      .on('change', () => {
        this.setState({ selectOp: op2s.val() })
      })
    field2s.trigger('change')
    this.__select2 = [field2s, op2s]
  }

  componentWillUnmount() {
    this.__select2.forEach((item) => item.select2('destroy'))
    this.__select2 = null
    this.__destroyLastValueComp()
  }

  render() {
    if (this.props.fields.length === 0) {
      return <div className="text-danger">{$L('NoUpdateFields')}</div>
    }

    return (
      <div className="row">
        <div className="col-4">
          <select className="form-control form-control-sm" ref={(c) => (this._field = c)}>
            {this.props.fields.map((item) => {
              return (
                <option value={item.name} key={`field-${item.name}`}>
                  {item.label}
                </option>
              )
            })}
          </select>
        </div>
        <div className="col-2 pl-0 pr-0">{this.renderOp()}</div>
        <div className="col-6">
          <div className={`${this.state.selectOp === 'NULL' ? 'hide' : ''}`}>{this.state.selectField && this.state.selectOp && this.renderValue()}</div>
        </div>
      </div>
    )
  }

  renderOp() {
    return (
      <select className="form-control form-control-sm" ref={(c) => (this._op = c)}>
        <option value="SET">{BUE_OPTYPES['SET']}</option>
        <option value="NULL">{BUE_OPTYPES['NULL']}</option>
      </select>
    )
  }

  renderValue() {
    if (this.state.selectOp === 'NULL' || !this.state.selectField) return null // set Null

    const field = this.props.fields.find((item) => {
      return this.state.selectField === item.name
    })
    const fieldKey = `fv-${field.name}`
    if (field.type === 'PICKLIST' || field.type === 'STATE' || field.type === 'MULTISELECT' || field.type === 'BOOL' || field.type === 'REFERENCE' || field.type === 'CLASSIFICATION') {
      return (
        <select className="form-control form-control-sm" multiple={field.type === 'MULTISELECT'} ref={(c) => (this._value = c)} key={fieldKey}>
          {(field.options || []).map((item) => {
            let itemId = item.id || item.mask
            if (item.id === false) itemId = 'false' // for BOOL
            return (
              <option key={`value-${itemId}`} value={itemId}>
                {item.text}
              </option>
            )
          })}
        </select>
      )
    } else {
      return <input className="form-control form-control-sm" placeholder={$L('NewValue')} ref={(c) => (this._value = c)} key={fieldKey} maxLength="255" />
    }
  }

  __destroyLastValueComp() {
    if (this.__lastSelect2) {
      this.__lastSelect2.select2('destroy')
      this.__lastSelect2 = null
    }
    if (this.__lastDatetimepicker) {
      this.__lastDatetimepicker.datetimepicker('remove')
      this.__lastDatetimepicker = null
    }
  }

  componentDidUpdate(prevProps, prevState) {
    // Unchanged
    if (prevState.selectField === this.state.selectField && prevState.selectOp === this.state.selectOp) return
    if (this.state.selectOp === 'NULL') return
    this.__destroyLastValueComp()

    const field = this.props.fields.find((item) => {
      return this.state.selectField === item.name
    })
    if (this._value.tagName === 'SELECT') {
      if (field.type === 'REFERENCE' || field.type === 'CLASSIFICATION') {
        this.__lastSelect2 = $initReferenceSelect2(this._value, {
          name: field.name,
          label: field.label,
          entity: this.props.entity,
          searchType: field.type === 'CLASSIFICATION' ? 'classification' : null,
        })
      } else {
        this.__lastSelect2 = $(this._value).select2({
          placeholder: $L('NewValue'),
        })
      }
      this.__lastSelect2.val(null).trigger('change')
    } else if (field.type === 'DATE' || field.type === 'DATETIME') {
      this.__lastDatetimepicker = $(this._value).datetimepicker({
        format: field.type === 'DATE' ? 'yyyy-mm-dd' : 'yyyy-mm-dd hh:ii:ss',
        minView: field.type === 'DATE' ? 'month' : 0,
      })
    }
  }

  buildItem() {
    const item = { field: this.state.selectField, op: this.state.selectOp }
    const field = this.props.fields.find((item) => {
      return this.state.selectField === item.name
    })
    if (item.op === 'NULL') {
      if (!field.nullable) {
        RbHighbar.create($L('SomeNotEmpty').replace('{0}', field.label))
        return null
      } else {
        return item
      }
    }

    item.value = $(this._value).val()
    if (!item.value || item.value.length === 0) {
      RbHighbar.create($L('SomeNotEmpty,ModifyValue'))
      return null
    }

    if (field.type === 'MULTISELECT') {
      let maskTotal = 0
      item.value.forEach((mask) => (maskTotal += ~~mask))
      item.value = maskTotal
    } else if (field.type === 'NUMBER' || field.type === 'DECIMAL') {
      if (isNaN(item.value)) {
        RbHighbar.create($L('SomeNotFormatWell').replace('{0}', field.label))
        return null
      } else if (field.notNegative === 'true' && ~~item.value < 0) {
        RbHighbar.create($L('SomeNotBeNegative').replace('{0}', field.label))
        return null
      }
    } else if (field.type === 'EMAIL') {
      if (!$regex.isMail(item.value)) {
        RbHighbar.create($L('SomeNotFormatWell').replace('{0}', field.label))
        return null
      }
    } else if (field.type === 'URL') {
      if (!$regex.isUrl(item.value)) {
        RbHighbar.create($L('SomeNotFormatWell').replace('{0}', field.label))
        return null
      }
    } else if (field.type === 'PHONE') {
      if (!$regex.isTel(item.value)) {
        RbHighbar.create($L('SomeNotFormatWell').replace('{0}', field.label))
        return null
      }
    }

    if (this._value.tagName === 'SELECT') {
      const texts = $(this._value)
        .select2('data')
        .map((o) => {
          return o.text
        })
      item.text = texts.join(', ')
      $(this._value).val(null).trigger('change')
    } else {
      $(this._value).val('')
    }
    return item
  }
}
