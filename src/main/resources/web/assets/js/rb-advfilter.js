/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// ~~ 高级过滤器

const BIZZ_ENTITIES = ['User', 'Department', 'Role', 'Team']
const NT_SPLIT = '----'
const NAME_FLAG = '&'

// eslint-disable-next-line no-unused-vars
class AdvFilter extends React.Component {
  constructor(props) {
    super(props)

    const ext = { useEquation: 'OR' }
    if (props.filter) {
      if (props.filter.equation) {
        ext.equation = props.filter.equation
        if (props.filter.equation === 'OR') ext.useEquation = 'OR'
        else if (props.filter.equation === 'AND') ext.useEquation = 'AND'
        else ext.useEquation = '9999'
      }
      this.__items = props.filter.items
    }

    this.state = { ...props, ...ext }
    this.childrenRef = []
  }

  render() {
    const opButtons = this.props.fromList ? (
      <div className="float-right">
        <button className="btn btn-primary" type="button" onClick={() => this.confirm()}>
          {$lang('Save')}
        </button>
        <button className="btn btn-primary bordered" type="button" onClick={() => this.searchNow()}>
          <i className="icon zmdi zmdi-search" /> {$lang('QueryNow')}
        </button>
      </div>
    ) : (
      <div className="item">
        <button className="btn btn-primary" type="button" onClick={() => this.confirm()}>
          {$lang('Confirm')}
        </button>
        <button className="btn btn-secondary" type="button" onClick={() => this.hide(true)}>
          {$lang('Cancel')}
        </button>
      </div>
    )

    const advFilter = (
      <div className={'adv-filter-wrap ' + (this.props.inModal ? 'in-modal' : 'shadow rounded')}>
        <div className="adv-filter">
          <div className="filter-items" onKeyPress={this.searchByKey}>
            {(this.state.items || []).map((item) => {
              return item
            })}
            <div className="item plus">
              <a onClick={() => this.addItem()} tabIndex="-1">
                <i className="zmdi zmdi-plus-circle icon"></i> {$lang('AddFilterItem')}
              </a>
            </div>
          </div>
        </div>
        <div className="adv-filter adv-filter-option">
          <div className="mb-1">
            <div className="item mt-1">
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-2">
                <input className="custom-control-input" type="radio" name="useEquation" value="OR" checked={this.state.useEquation === 'OR'} onChange={this.handleChange} />
                <span className="custom-control-label"> {$lang('OrEquation')}</span>
              </label>
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-2">
                <input className="custom-control-input" type="radio" name="useEquation" value="AND" checked={this.state.useEquation === 'AND'} onChange={this.handleChange} />
                <span className="custom-control-label"> {$lang('AndEquation')}</span>
              </label>
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-2">
                <input className="custom-control-input" type="radio" name="useEquation" value="9999" checked={this.state.useEquation === '9999'} onChange={this.handleChange} />
                <span className="custom-control-label"> {$lang('AdvEquation')}</span>
              </label>
            </div>
            {this.state.useEquation === '9999' && (
              <div className="mb-3 equation-state">
                <input
                  className={'form-control form-control-sm text-uppercase' + (this.state.equationError ? ' is-invalid' : '')}
                  title={this.state.equationError ? $lang('SomeInvalid,AdvEquation') : ''}
                  value={this.state.equation || ''}
                  placeholder={this.state.equationDef || ''}
                  data-id="equation"
                  onChange={this.handleChange}
                  onBlur={(e) => this.checkEquation(e)}
                />
                {this.state.equationError ? <i className="zmdi zmdi-alert-triangle text-danger"></i> : <i className="zmdi zmdi-check text-success"></i>}
              </div>
            )}
          </div>
          {this.props.fromList ? (
            <div className="item dialog-footer">
              <div className="float-left">
                <div className="float-left input">
                  <input
                    className="form-control form-control-sm text"
                    maxLength="20"
                    value={this.state.filterName || ''}
                    data-id="filterName"
                    onChange={this.handleChange}
                    placeholder={$lang('InputNameSave')}
                  />
                </div>
                {rb.isAdminUser && <Share2 ref={(c) => (this._shareTo = c)} noSwitch={true} shareTo={this.props.shareTo} />}
              </div>
              {opButtons}
              <div className="clearfix" />
            </div>
          ) : (
            <div className="btn-footer">{opButtons}</div>
          )}
        </div>
      </div>
    )

    if (this.props.inModal)
      return (
        <RbModal ref={(c) => (this._dlg = c)} title={this.props.title || $lang('AdvFilter')} disposeOnHide={!!this.props.filterName}>
          {advFilter}
        </RbModal>
      )
    else return advFilter
  }

  componentDidMount() {
    $.get(`/commons/metadata/fields?deep=2&ft=QUERY&entity=${this.props.entity}`, (res) => {
      const valideFs = []
      this.fields = res.data.map((item) => {
        valideFs.push(item.name)
        if (item.type === 'REFERENCE') {
          REFMETA_CACHE[this.props.entity + '.' + item.name] = item.ref
          // Use `NameField` type
          if (!BIZZ_ENTITIES.includes(item.ref[0])) item.type = item.ref[1]
        }
        if (item.type === 'DATETIME') item.type = 'DATE'
        return item
      })

      if (this.__items) {
        this.__items.forEach((item) => {
          if (item.field.substr(0, 1) === NAME_FLAG) item.field = item.field.substr(1)
          if (valideFs.includes(item.field)) this.addItem(item)
          // eslint-disable-next-line no-console
          else if (rb.env === 'dev') console.warn('Unkonw field : ' + JSON.stringify(item))
        })
      }
    })
  }

  onRef = (child) => {
    this.childrenRef.push(child)
  }

  handleChange = (e) => {
    const val = e.target.value
    const id = e.target.dataset.id || e.target.name
    const state = {}
    state[id] = val
    this.setState({ ...state })
  }

  addItem(props) {
    if (!this.fields) return
    const _items = this.state.items || []
    if (_items.length >= 9) {
      RbHighbar.create($lang('MaxFilterItems'))
      return
    }

    const id = 'item-' + $random()
    let itemProps = {
      fields: this.fields,
      $$$parent: this,
      key: 'key-' + id,
      id: id,
      onRef: this.onRef,
      index: _items.length + 1,
    }
    if (props) itemProps = { ...itemProps, ...props }
    _items.push(<FilterItem {...itemProps} />)

    this.setState({ items: _items }, () => this.renderEquation())
  }

  removeItem(id) {
    const _items = []
    this.state.items.forEach((item) => {
      if (item.props.id !== id) _items.push(item)
    })
    const _children = []
    this.childrenRef.forEach((item) => {
      if (item.props.id !== id) _children.push(item)
    })
    this.childrenRef = _children

    this.setState({ items: _items }, () => {
      this.childrenRef.forEach((child, idx) => {
        child.setIndex(idx + 1)
      })
      this.renderEquation()
    })
  }

  checkEquation(e) {
    const val = e.target.value
    if (!val) return
    $.post('/app/entity/advfilter/test-equation', val, (res) => {
      this.setState({ equationError: res.error_code !== 0 })
    })
  }

  renderEquation() {
    const exp = []
    for (let i = 1; i <= (this.state.items || []).length; i++) exp.push(i)
    this.setState({ equationDef: exp.join(' OR ') })
  }

  toFilterJson(canNoFilters) {
    const filters = []
    let hasError = false
    for (let i = 0; i < this.childrenRef.length; i++) {
      const item = this.childrenRef[i].getFilterJson()
      if (!item) hasError = true
      else filters.push(item)
    }
    if (hasError) {
      RbHighbar.create($lang('HasInvalidFilterItems'))
      return
    }
    if (filters.length === 0 && canNoFilters !== true) {
      RbHighbar.create($lang('Pls1FilterItemLeast'))
      return
    }

    const adv = { entity: this.props.entity, items: filters }
    if (this.state.useEquation === 'AND') {
      adv.equation = 'AND'
    } else if (this.state.useEquation === '9999') {
      if (this.state.equationError === true) {
        RbHighbar.create($lang('SomeInvalid,AdvOperator'))
        return
      }
      adv.equation = this.state.equation
    }

    // eslint-disable-next-line no-console
    if (rb.env === 'dev') console.log(JSON.stringify(adv))
    return adv
  }

  searchByKey = (e) => {
    if (this.props.fromList !== true || e.which !== 13) return // Not [enter]
    this.searchNow()
  }

  searchNow = () => {
    const adv = this.toFilterJson(true)
    if (!!adv && window.RbListPage) RbListPage._RbList.search(adv, true)
  }

  confirm() {
    const adv = this.toFilterJson(this.props.canNoFilters)
    if (!adv) return

    typeof this.props.confirm === 'function' && this.props.confirm(adv, this.state.filterName, this._shareTo ? this._shareTo.getData().shareTo : null)
    this.props.inModal && this._dlg.hide()
    this.setState({ filterName: null })
  }

  show(state) {
    this.props.inModal && this._dlg.show(state)
  }

  hide() {
    this.props.inModal && this._dlg.hide()
    typeof this.props.cancel === 'function' && this.props.cancel()
  }
}

const OP_TYPE = {
  LK: $lang('FilterLK'),
  NLK: $lang('FilterNLK'),
  IN: $lang('FilterLK'),
  NIN: $lang('FilterNLK'),
  EQ: $lang('FilterEQ'),
  NEQ: $lang('FilterNEQ'),
  GT: $lang('FilterGT'),
  LT: $lang('FilterLT'),
  GE: $lang('FilterGE'),
  LE: $lang('FilterLE'),
  BW: $lang('FilterBW'),
  NL: $lang('FilterNL'),
  NT: $lang('FilterNT'),
  BFD: $lang('FilterBFD'),
  BFM: $lang('FilterBFM'),
  BFY: $lang('FilterBFY'),
  AFD: $lang('FilterAFD'),
  AFM: $lang('FilterAFM'),
  AFY: $lang('FilterAFY'),
  RED: $lang('FilterRED'),
  REM: $lang('FilterREM'),
  REY: $lang('FilterREY'),
  SFU: $lang('FilterSFU'),
  SFB: $lang('FilterSFB'),
  SFD: $lang('FilterSFD'),
  YTA: $lang('FilterYTA'),
  TDA: $lang('FilterTDA'),
  TTA: $lang('FilterTTA'),
  CUW: $lang('FilterCUW'),
  CUM: $lang('FilterCUM'),
  CUQ: $lang('FilterCUQ'),
  CUY: $lang('FilterCUY'),
}
const OP_NOVALUE = ['NL', 'NT', 'SFU', 'SFB', 'SFD', 'YTA', 'TDA', 'TTA', 'CUW', 'CUM', 'CUQ', 'CUY']
const OP_DATE_NOPICKER = ['TDA', 'YTA', 'TTA', 'RED', 'REM', 'REY', 'BFD', 'BFM', 'BFY', 'AFD', 'AFM', 'AFY']
const PICKLIST_CACHE = {}
const REFMETA_CACHE = {}
const INPUTVALS_HOLD = {} // 输入值保持

// 过滤项
class FilterItem extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
    this.$$$entity = this.props.$$$parent.props.entity

    this.loadedPickList = false
    this.loadedBizzSearch = false

    if (props.field && props.value) INPUTVALS_HOLD[props.field] = props.value
  }

  render() {
    return (
      <div className="row item">
        <div className="col-sm-5 field">
          <em>{this.state.index}</em>
          <i className="zmdi zmdi-minus-circle" title={$lang('Remove')} onClick={() => this.props.$$$parent.removeItem(this.props.id)}></i>
          <select className="form-control form-control-sm" ref={(c) => (this._filterField = c)}>
            {this.state.fields.map((item) => {
              return (
                <option value={item.name + NT_SPLIT + item.type} key={`field-${item.name}`} title={item.label}>
                  {item.label}
                </option>
              )
            })}
          </select>
        </div>
        <div className="col-sm-2 op">
          <select className="form-control form-control-sm" ref={(c) => (this._filterOp = c)}>
            {this.selectOp().map((item) => {
              return (
                <option value={item} key={`op-${item}`} title={OP_TYPE[item]}>
                  {OP_TYPE[item]}
                </option>
              )
            })}
          </select>
        </div>
        <div className={'col-sm-5 val' + (OP_NOVALUE.includes(this.state.op) ? ' hide' : '')}>{this.renderValue()}</div>
      </div>
    )
  }

  selectOp() {
    const fieldType = this.state.type
    let op = ['LK', 'NLK', 'EQ', 'NEQ']
    if (fieldType === 'NUMBER' || fieldType === 'DECIMAL') {
      op = ['GT', 'LT', 'EQ', 'BW', 'GE', 'LE']
    } else if (fieldType === 'DATE' || fieldType === 'DATETIME') {
      op = ['TDA', 'YTA', 'TTA', 'GT', 'LT', 'EQ', 'BW', 'RED', 'REM', 'REY', 'BFD', 'BFM', 'BFY', 'AFD', 'AFM', 'AFY', 'CUW', 'CUM', 'CUQ', 'CUY']
    } else if (fieldType === 'FILE' || fieldType === 'IMAGE' || fieldType === 'AVATAR') {
      op = []
    } else if (fieldType === 'PICKLIST' || fieldType === 'STATE' || fieldType === 'MULTISELECT') {
      op = ['IN', 'NIN']
    } else if (fieldType === 'CLASSIFICATION') {
      op = ['LK', 'NLK']
    } else if (fieldType === 'REFERENCE') {
      if (this.isBizzField('User')) {
        op = ['IN', 'NIN', 'SFU', 'SFB']
      } else if (this.isBizzField('Department')) {
        op = ['IN', 'NIN', 'SFB', 'SFD']
      } else if (this.isBizzField('Role')) {
        op = ['IN', 'NIN']
      } else {
        // 引用字段在引用实体修改了名称字段后可能存在问题
        // 例如原名称字段为日期，其设置的过滤条件也是日期相关的，修改成文本后可能出错
        // op = []
      }
    } else if (fieldType === 'BOOL') {
      op = ['EQ']
    }
    op.push('NL', 'NT')
    if (this.isApprovalState()) op = ['IN', 'NIN']

    this.__op = op
    return op
  }

  renderValue() {
    let val = <input className="form-control form-control-sm" ref={(c) => (this._filterVal = c)} onChange={this.valueHandle} onBlur={this.valueCheck} value={this.state.value || ''} />
    if (this.state.op === 'BW') {
      val = (
        <div className="val-range">
          <input className="form-control form-control-sm" ref={(c) => (this._filterVal = c)} onChange={this.valueHandle} onBlur={this.valueCheck} value={this.state.value || ''} />
          <input className="form-control form-control-sm" ref={(c) => (this._filterVal2 = c)} onChange={this.valueHandle} onBlur={this.valueCheck} value={this.state.value2 || ''} data-at="2" />
          <span>{$lang('Start0')}</span>
          <span className="end">{$lang('End0')}</span>
        </div>
      )
    } else if (this.state.type === 'PICKLIST' || this.state.type === 'STATE' || this.state.type === 'MULTISELECT') {
      val = (
        <select className="form-control form-control-sm" multiple ref={(c) => (this._filterVal = c)}>
          {(this.state.options || []).map((item) => {
            let id = item.id || item.mask
            return (
              <option key={id} value={id}>
                {item.text}
              </option>
            )
          })}
        </select>
      )
    } else if (this.isBizzField()) {
      val = <select className="form-control form-control-sm" multiple ref={(c) => (this._filterVal = c)} />
    } else if (this.state.type === 'BOOL') {
      val = (
        <select className="form-control form-control-sm" ref={(c) => (this._filterVal = c)}>
          <option value="T">{$lang('True')}</option>
          <option value="F">{$lang('False')}</option>
        </select>
      )
    }

    INPUTVALS_HOLD[this.state.field] = this.state.value
    return val
  }

  // 引用 User/Department/Role
  isBizzField(entity) {
    if (this.state.type === 'REFERENCE') {
      const ref = REFMETA_CACHE[this.$$$entity + '.' + this.state.field]
      if (!entity) return BIZZ_ENTITIES.includes(ref[0])
      else return ref[0] === entity
    }
    return false
  }

  // 数字值
  isNumberValue() {
    if (this.state.type === 'NUMBER' || this.state.type === 'DECIMAL') {
      return true
    } else if (this.state.type === 'DATE' && OP_DATE_NOPICKER.includes(this.state.op)) {
      return true
    }
    return false
  }

  // 审批状态
  isApprovalState() {
    const fieldName = this.state.field || ''
    return fieldName === 'approvalState' || fieldName.endsWith('.approvalState')
  }

  componentDidMount() {
    this.props.onRef(this)

    const that = this
    const s2field = $(this._filterField)
      .select2({
        allowClear: false,
      })
      .on('change', function (e) {
        const ft = e.target.value.split(NT_SPLIT)
        that.setState({ field: ft[0], type: ft[1] }, () => s2op.val(that.__op[0]).trigger('change'))
      })
    const s2op = $(this._filterOp)
      .select2({
        allowClear: false,
      })
      .on('change', function (e) {
        that.setState({ op: e.target.value }, () => that._componentDidUpdate())
      })
    this.__select2 = [s2field, s2op]

    // Load
    if (this.props.field) {
      let field = this.props.field
      $(this.props.fields).each(function () {
        if (this.name === field) {
          field = [field, this.type].join(NT_SPLIT)
          return false
        }
      })
      s2field.val(field).trigger('change')
      setTimeout(() => s2op.val(that.props.op).trigger('change'), 100)
    } else {
      s2field.trigger('change')
    }
  }

  _componentDidUpdate() {
    const state = this.state
    const lastType = this.__lastType
    this.__lastType = state.type

    if (state.type === 'PICKLIST' || state.type === 'STATE' || state.type === 'MULTISELECT') {
      this.renderPickList(state.field)
    } else if (lastType === 'PICKLIST' || lastType === 'STATE' || lastType === 'MULTISELECT') {
      this.removePickList()
    }

    if (state.type === 'DATE') {
      this.removeDatepicker()
      if (OP_DATE_NOPICKER.includes(state.op)) {
        // 无需日期组件
      } else {
        this.renderDatepicker()
      }
    } else if (lastType === 'DATE') {
      this.removeDatepicker()
    }

    if (this.isBizzField()) {
      const ref = REFMETA_CACHE[this.$$$entity + '.' + state.field]
      this.renderBizzSearch(ref[0])
    } else if (lastType === 'REFERENCE') {
      this.removeBizzSearch()
    }

    if (state.type === 'BOOL') {
      this.removeBool()
      if (!OP_NOVALUE.includes(state.op)) this.renderBool()
    } else if (lastType === 'BOOL') {
      this.removeBool()
    }

    if (state.value) this.valueCheck($(this._filterVal))
    if (state.value2 && this._filterVal2) this.valueCheck($(this._filterVal2))
  }

  componentWillUnmount() {
    this.__select2.forEach((item) => {
      item.select2('destroy')
    })
    this.__select2 = null
    this.removePickList()
    this.removeDatepicker()
    this.removeBizzSearch()
    this.removeBool()
  }

  valueHandle = (e) => {
    const val = e.target.value
    if (~~e.target.dataset.at === 2) this.setState({ value2: val })
    else this.setState({ value: val })
  }
  // @e = el or event
  valueCheck = (e) => {
    let el = e.target ? $(e.target) : e
    let val = e.target ? e.target.value : e.val()
    el.removeClass('is-invalid')
    if (!val) {
      el.addClass('is-invalid')
    } else {
      if (this.isNumberValue()) {
        if ($regex.isDecimal(val) === false) el.addClass('is-invalid')
      } else if (this.state.type === 'DATE') {
        if ($regex.isUTCDate(val) === false) el.addClass('is-invalid')
      }
    }
  }

  // 列表

  renderPickList(field) {
    const entity = this.props.$$$parent.props.entity
    const plKey = entity + '.' + field
    if (PICKLIST_CACHE[plKey]) {
      this.setState({ options: PICKLIST_CACHE[plKey] }, () => this.renderPickListAfter())
    } else {
      $.get(`/commons/metadata/field-options?entity=${entity}&field=${field}`, (res) => {
        if (res.error_code === 0) {
          PICKLIST_CACHE[plKey] = res.data
          this.setState({ options: PICKLIST_CACHE[plKey] }, () => this.renderPickListAfter())
        } else {
          RbHighbar.error(res.error_msg)
        }
      })
    }
  }

  renderPickListAfter() {
    const that = this
    const s2val = $(this._filterVal)
      .select2({})
      .on('change.select2', function () {
        that.setState({ value: s2val.val().join('|') })
      })
    this.__select2_PickList = s2val

    // Load
    if (this.props.value && this.loadedPickList === false) {
      const val = this.props.value.split('|')
      s2val.val(val).trigger('change')
      this.loadedPickList = true
    }
  }

  removePickList() {
    if (this.__select2_PickList) {
      this.__select2_PickList.select2('destroy')
      this.__select2_PickList = null
      this.setState({ value: null })
    }
  }

  // 用户/部门

  renderBizzSearch(entity) {
    const that = this
    const s2val = $(this._filterVal)
      .select2({
        minimumInputLength: 1,
        ajax: {
          url: '/commons/search/search',
          delay: 300,
          data: function (params) {
            const query = {
              entity: entity,
              quickFields: entity === 'User' ? 'loginName,fullName,email,quickCode' : 'name,quickCode',
              q: params.term,
            }
            return query
          },
          processResults: function (data) {
            const rs = data.data.map((item) => {
              return item
            })
            return { results: rs }
          },
        },
      })
      .on('change.select2', function () {
        const val = s2val.val()
        that.setState({ value: val.join('|') })
      })
    this.__select2_BizzSearch = s2val

    // Load
    if (this.props.value && this.loadedBizzSearch === false) {
      $.get(`/commons/search/read-labels?ids=${$encode(this.props.value)}`, (res) => {
        for (let kid in res.data) {
          const option = new Option(res.data[kid], kid, true, true)
          s2val.append(option)
        }
      })
      this.loadedBizzSearch = true
    }
  }

  removeBizzSearch() {
    if (this.__select2_BizzSearch) {
      this.__select2_BizzSearch.select2('destroy')
      this.__select2_BizzSearch = null
      this.setState({ value: null })
    }
  }

  // 日期时间

  renderDatepicker() {
    const dpcfg = {
      format: 'yyyy-mm-dd',
      minView: 2,
      startView: 'month',
    }

    const that = this
    const dp1 = $(this._filterVal).datetimepicker(dpcfg)
    dp1.on('change.select2', function (e) {
      that.setState({ value: e.target.value }, () => {
        that.valueCheck($(that._filterVal))
      })
    })
    this.__datepicker = [dp1]

    if (this._filterVal2) {
      const dp2 = $(this._filterVal2).datetimepicker(dpcfg)
      dp2.on('change.select2', function (e) {
        that.setState({ value2: e.target.value }, () => {
          that.valueCheck($(that._filterVal2))
        })
      })
      this.__datepicker.push(dp2)
    }
  }

  removeDatepicker() {
    if (this.__datepicker) {
      this.__datepicker.forEach((item) => {
        item.datetimepicker('remove')
      })
      this.__datepicker = null
    }
  }

  // 布尔

  renderBool() {
    const that = this
    const s2val = $(this._filterVal)
      .select2({
        allowClear: false,
      })
      .on('change.select2', function () {
        that.setState({ value: s2val.val() })
      })
    this.__select2_Bool = s2val
    s2val.val(this.props.value || 'T').trigger('change')
  }

  removeBool() {
    if (this.__select2_Bool) {
      this.__select2_Bool.select2('destroy')
      this.__select2_Bool = null
      this.setState({ value: null })
    }
  }

  setIndex(idx) {
    this.setState({ index: idx })
  }

  getFilterJson() {
    let s = this.state
    if (!s.value) {
      if (OP_NOVALUE.includes(s.op)) {
        // 允许无值
      } else {
        return
      }
    } else if (OP_NOVALUE.includes(s.op)) {
      s.value = null
    }

    if (s.op === 'BW' && !s.value2) {
      return
    }

    if (!!s.value && ($(this._filterVal).hasClass('is-invalid') || $(this._filterVal2).hasClass('is-invalid'))) {
      return
    }

    let item = { index: s.index, field: s.field, op: s.op }
    if (s.value) item.value = s.value
    if (s.value2) item.value2 = s.value2
    // 引用字段查询名称字段
    const isRef = REFMETA_CACHE[this.$$$entity + '.' + s.field]
    if (isRef && !BIZZ_ENTITIES.includes(isRef[0]) && (s.op === 'LK' || s.op === 'NLK' || s.op === 'EQ' || s.op === 'NEQ')) {
      item.field = NAME_FLAG + item.field
    }
    this.setState({ hasError: false })
    return item
  }
}
