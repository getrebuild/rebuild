/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

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

    return <RbModal title={this.state.title} disposeOnHide={true} ref={(c) => this._dlg = c}>
      <div className="form batch-form">
        <div className="form-group">
          <label className="text-bold">选择数据范围</label>
          <div>
            {selectedRows > 0 && <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" name="dataRange" type="radio" checked={~~this.state.dataRange === 1} value="1" onChange={this.handleChange} />
              <span className="custom-control-label">选中的数据 ({selectedRows}条)</span>
            </label>}
            <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" name="dataRange" type="radio" checked={~~this.state.dataRange === 2} value="2" onChange={this.handleChange} />
              <span className="custom-control-label">当前页的数据 ({pageRows}条)</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" name="dataRange" type="radio" checked={~~this.state.dataRange === 3} value="3" onChange={this.handleChange} />
              <span className="custom-control-label">查询后的数据 ({queryRows}条)</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio mb-1">
              <input className="custom-control-input" name="dataRange" type="radio" checked={~~this.state.dataRange === 10} value="10" onChange={this.handleChange} />
              <span className="custom-control-label">全部数据</span>
            </label>
          </div>
        </div>
        {this.renderOperator()}
      </div>
      <div className="dialog-footer" ref={(c) => this._btns = c}>
        <a className="btn btn-link btn-space" onClick={this.hide}>取消</a>
        <button className="btn btn-primary btn-space" type="button" data-loading-text="请稍后" onClick={this.confirm}>确定</button>
      </div>
    </RbModal>
  }

  getQueryData() {
    const qd = this.props.listRef.getLastQueryEntry()
    if (~~this.state.dataRange === 1) qd._selected = this.props.listRef.getSelectedIds(true).join('|')
    return qd
  }

  // 子类复写

  renderOperator() { }
  confirm = () => { }
}

// ~ 数据导出

// eslint-disable-next-line no-unused-vars
class DataExport extends BatchOperator {

  constructor(props) {
    super(props)
    this.state.title = '数据导出'
  }

  confirm = () => {
    this.disabled(true)
    $.post(`/app/${this.props.entity}/data-export/submit?dr=${this.state.dataRange}`, JSON.stringify(this.getQueryData()), (res) => {
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
    this.state.title = '批量修改'
  }

  componentDidMount() {
    $.get(`/app/${this.props.entity}/batch-update/fields`, (res) => this.setState({ fields: res.data }))
  }

  renderOperator() {
    return <div className="form-group">
      <label className="text-bold">修改内容</label>
      <div>
        <div className="batch-contents">
          {(this.state.updateContents || []).map((item) => {
            return <div key={`update-${item.field}`}>
              <div className="row">
                <div className="col-4">
                  <span className="badge badge-light">{this._fieldLabel(item.field)}</span>
                </div>
                <div className="col-2 pl-0 pr-0">
                  <span className="badge badge-warning">{BUE_OPTYPES[item.op]}</span>
                </div>
                <div className="col-6">
                  {item.op !== 'NULL' && <span className="badge badge-light">{item.text || item.value}</span>}
                  <a className="del" onClick={() => this.removeItem(item.field)}><i className="zmdi zmdi-close"></i></a>
                </div>
              </div>
            </div>
          })}
        </div>
        <div className="mt-2">
          {this.state.fields && <BatchUpdateEditor ref={(c) => this._editor = c} fields={this.state.fields} entity={this.props.entity} />}
          <div className="mt-1">
            <button className="btn btn-primary btn-sm bordered" onClick={this.addItem}>添加</button>
          </div>
        </div>
      </div>
    </div>
  }

  _fieldLabel(fieldName) {
    const field = this.state.fields.find((item) => { return fieldName === item.name })
    return field ? field.label : `[${fieldName}.toUpperCase()]`
  }

  addItem = () => {
    const item = this._editor.buildItem()
    if (!item) return

    const contents = this.state.updateContents || []
    const found = contents.find((x) => { return item.field === x.field })
    if (found) {
      RbHighbar.create('修改字段已经存在')
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
    if (!this.state.updateContents || this.state.updateContents.length === 0) { RbHighbar.create('请添加修改内容'); return }
    const _data = { queryData: this.getQueryData(), updateContents: this.state.updateContents }
    // eslint-disable-next-line no-console
    if (rb.env === 'dev') console.log(JSON.stringify(_data))

    const that = this
    RbAlert.create('请再次确认修改数据范围和修改内容。开始修改吗？', {
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
      }
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
          $(this._btns).find('.btn-primary').text('修改成功')
          RbHighbar.success(`成功修改 ${res.data.succeeded} 条记录`)
          setTimeout(() => {
            this.hide()
            window.RbListPage && window.RbListPage.reload()
          }, 500)
        } else {
          mp && mp.set(cp)
          setTimeout(() => { this.__checkState(taskid, mp) }, 1000)
        }
      }
    })
  }
}

const BUE_OPTYPES = { 'SET': '修改为', 'NULL': '置空', 'PREFIX': '前添加', 'SUFFIX': '后添加', 'PLUS': '加上', 'MINUS': '减去' }

// ~ 批量修改编辑器
class BatchUpdateEditor extends React.Component {
  state = { ...this.props, selectOp: 'SET' }

  componentDidMount() {
    const field2s = $(this._field).select2({
      allowClear: false
    }).on('change', () => {
      this.setState({ selectField: field2s.val() })
    })
    const op2s = $(this._op).select2({
      allowClear: false
    }).on('change', () => {
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
      return <div className="text-danger">没有可修改字段</div>
    }

    return <div className="row">
      <div className="col-4">
        <select className="form-control form-control-sm" ref={(c) => this._field = c}>
          {this.props.fields.map((item) => {
            return <option value={item.name} key={`field-${item.name}`}>{item.label}</option>
          })}
        </select>
      </div>
      <div className="col-2 pl-0 pr-0">{this.renderOp()}</div>
      <div className="col-6">
        <div className={`${this.state.selectOp === 'NULL' ? 'hide' : ''}`}>
          {(this.state.selectField && this.state.selectOp) && this.renderValue()}
        </div>
      </div>
    </div>
  }

  renderOp() {
    return <select className="form-control form-control-sm" ref={(c) => this._op = c}>
      <option value="SET">修改为</option>
      <option value="NULL">置空</option>
    </select>
  }

  renderValue() {
    if (this.state.selectOp === 'NULL' || !this.state.selectField) return null // set Null

    const field = this.props.fields.find((item) => { return this.state.selectField === item.name })
    const fieldKey = `fv-${field.name}`
    if (field.type === 'PICKLIST' || field.type === 'STATE' || field.type === 'MULTISELECT' || field.type === 'BOOL'
      || field.type === 'REFERENCE' || field.type === 'CLASSIFICATION') {
      return <select className="form-control form-control-sm" multiple={field.type === 'MULTISELECT'} ref={(c) => this._value = c} key={fieldKey}>
        {(field.options || []).map((item) => {
          let itemId = item.id || item.mask
          if (item.id === false) itemId = 'false'  // for BOOL
          return <option key={`value-${itemId}`} value={itemId}>{item.text}</option>
        })}
      </select>
    } else {
      return <input className="form-control form-control-sm" placeholder="新值" ref={(c) => this._value = c} key={fieldKey} maxLength="255" />
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

    const field = this.props.fields.find((item) => { return this.state.selectField === item.name })
    if (this._value.tagName === 'SELECT') {
      if (field.type === 'REFERENCE' || field.type === 'CLASSIFICATION') {
        this.__lastSelect2 = $initReferenceSelect2(this._value, {
          name: field.name,
          label: field.label,
          entity: this.props.entity,
          searchType: field.type === 'CLASSIFICATION' ? 'classification' : null
        })
      } else {
        this.__lastSelect2 = $(this._value).select2({
          placeholder: '新值'
        })
      }
      this.__lastSelect2.val(null).trigger('change')

    } else if (field.type === 'DATE' || field.type === 'DATETIME') {
      this.__lastDatetimepicker = $(this._value).datetimepicker({
        componentIcon: 'zmdi zmdi-calendar',
        navIcons: { rightIcon: 'zmdi zmdi-chevron-right', leftIcon: 'zmdi zmdi-chevron-left' },
        format: field.type === 'DATE' ? 'yyyy-mm-dd' : 'yyyy-mm-dd hh:ii:ss',
        minView: field.type === 'DATE' ? 'month' : 0,
        weekStart: 1,
        autoclose: true,
        language: 'zh',
        todayHighlight: true,
        showMeridian: false,
        keyboardNavigation: false,
        minuteStep: 5
      })
    }
  }

  buildItem() {
    const item = { field: this.state.selectField, op: this.state.selectOp }
    const field = this.props.fields.find((item) => { return this.state.selectField === item.name })
    if (item.op === 'NULL') {
      if (!field.nullable) {
        RbHighbar.create(`${field.label}不允许为空`)
        return null
      } else {
        return item
      }
    }

    item.value = $(this._value).val()
    if (!item.value || item.value.length === 0) {
      RbHighbar.create('修改值不能为空')
      return null
    }

    if (field.type === 'MULTISELECT') {
      let maskTotal = 0
      item.value.forEach((mask) => maskTotal += ~~mask)
      item.value = maskTotal
    } else if (field.type === 'NUMBER' || field.type === 'DECIMAL') {
      if (isNaN(item.value)) {
        RbHighbar.create(`${field.label}格式不正确`)
        return null
      } else if (field.notNegative === 'true' && ~~item.value < 0) {
        RbHighbar.create(`${field.label}不允许为负数`)
        return null
      }
    } else if (field.type === 'EMAIL') {
      if (!$regex.isMail(item.value)) {
        RbHighbar.create(`${field.label}格式不正确`)
        return null
      }
    } else if (field.type === 'URL') {
      if (!$regex.isUrl(item.value)) {
        RbHighbar.create(`${field.label}格式不正确`)
        return null
      }
    } else if (field.type === 'PHONE') {
      if (!$regex.isTel(item.value)) {
        RbHighbar.create(`${field.label}格式不正确`)
        return null
      }
    }

    if (this._value.tagName === 'SELECT') {
      const texts = $(this._value).select2('data').map((o) => { return o.text })
      item.text = texts.join(', ')
      $(this._value).val(null).trigger('change')
    } else {
      $(this._value).val('')
    }
    return item
  }
}