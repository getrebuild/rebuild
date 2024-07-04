/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// ~~ 高级过滤器

const BIZZ_ENTITIES = ['User', 'Department', 'Role', 'Team']
const NT_SPLIT = '----'
const NAME_FLAG = '&'
const VF_ACU = '$APPROVALCURRENTUSER$'

// eslint-disable-next-line no-unused-vars
class AdvFilter extends React.Component {
  constructor(props) {
    super(props)

    const extras = { useEquation: 'OR' }
    if (props.filter) {
      const clone = $clone(props.filter) // bugfix
      if (clone.equation) {
        extras.equation = clone.equation
        if (clone.equation === 'OR') extras.useEquation = 'OR'
        else if (clone.equation === 'AND') extras.useEquation = 'AND'
        else extras.useEquation = '9999'
      }
      this.__initItems = clone.items
    }

    this.state = { items: [], ...props, ...extras }
    this._itemsRef = []
    this._htmlid = `useEquation-${$random()}`
  }

  render() {
    const filterComp = (
      <div className={`adv-filter-wrap ${this.props.inModal ? 'in-modal' : 'shadow rounded'}`}>
        {this.state.hasErrorTip && (
          <div className="alert alert-warning alert-icon alert-icon-border alert-sm">
            <div className="icon">
              <i className="zmdi zmdi-alert-triangle" />
            </div>
            <div className="message">{this.state.hasErrorTip}</div>
          </div>
        )}

        <div className="adv-filter">
          <div
            className="filter-items"
            onKeyPress={(e) => {
              if (e.which === 13 && typeof this.searchNow === 'function') {
                const $input = $(e.target)
                if ($input.prop('tagName') === 'INPUT') $input[0].blur()
                setTimeout(() => this.searchNow(), 20)
              }
            }}>
            {this.state.items}

            <div className="item plus">
              <a onClick={() => this.addItem()} tabIndex="-1">
                <i className="zmdi zmdi-plus-circle icon" /> {$L('添加条件')}
              </a>
            </div>
          </div>
        </div>

        <div className="adv-filter adv-filter-option">
          <div className="mb-1">
            <div className="item mt-1">
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-2">
                <input className="custom-control-input" type="radio" name={this._htmlid} data-id="useEquation" value="OR" checked={this.state.useEquation === 'OR'} onChange={this.handleChange} />
                <span className="custom-control-label pl-1">{$L('符合任一')}</span>
              </label>
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-2">
                <input className="custom-control-input" type="radio" name={this._htmlid} data-id="useEquation" value="AND" checked={this.state.useEquation === 'AND'} onChange={this.handleChange} />
                <span className="custom-control-label pl-1">{$L('符合全部')}</span>
              </label>
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-2">
                <input className="custom-control-input" type="radio" name={this._htmlid} data-id="useEquation" value="9999" checked={this.state.useEquation === '9999'} onChange={this.handleChange} />
                <span className="custom-control-label pl-1">
                  {$L('高级表达式')}
                  <a href="https://getrebuild.com/docs/manual/basic#%E9%AB%98%E7%BA%A7%E8%A1%A8%E8%BE%BE%E5%BC%8F" title={$L('查看帮助')} target="_blank">
                    <i className="zmdi zmdi-help zicon down-1 cursor-pointer" />
                  </a>
                </span>
              </label>
            </div>
            {this.state.useEquation === '9999' && (
              <div className="mb-3 equation-state">
                <input
                  className={'form-control form-control-sm text-uppercase' + (this.state.equationError ? ' is-invalid' : '')}
                  title={this.state.equationError ? $L('无效高级表达式') : ''}
                  value={this.state.equation || ''}
                  placeholder={this.state.equationDef || ''}
                  data-id="equation"
                  onChange={this.handleChange}
                  onBlur={(e) => this.checkEquation(e)}
                />
                <i className={`zmdi ${this.state.equationError ? 'zmdi-alert-triangle text-danger' : 'zmdi-check text-success'}`} />
              </div>
            )}
          </div>

          <div className="btn-footer">{this.renderAction()}</div>
        </div>
      </div>
    )

    return this.props.inModal ? (
      <RbModal title={this.props.title || $L('高级查询')} ref={(c) => (this._dlg = c)}>
        {filterComp}
      </RbModal>
    ) : (
      filterComp
    )
  }

  renderAction() {
    return (
      <div className="item">
        <button className="btn btn-primary" type="button" onClick={() => this.confirm()}>
          {this.props.confirmText || $L('确定')}
        </button>
        <button className="btn btn-secondary" type="button" onClick={() => this.hide()}>
          {$L('取消')}
        </button>
      </div>
    )
  }

  componentDidMount() {
    const deep = this.props.deep3 || location.href.includes('/admin/') ? 3 : 2
    const referer = this.props.referer || ''
    $.get(`/commons/metadata/fields?deep=${deep}&entity=${this.props.entity}&referer=${referer}`, (res) => {
      const validFs = []
      const fields = []

      res.data.forEach((item) => {
        validFs.push(item.name)

        // 引用字段在引用实体修改了名称字段后可能存在问题
        // 例如原名称字段为日期，其设置的过滤条件也是日期相关的，修改成文本后可能出错

        if (['REFERENCE', 'N2NREFERENCE'].includes(item.type)) {
          REFENTITY_CACHE[item.name] = item.ref
          if ('N2NREFERENCE' === item.type) REFENTITY_CACHE[item.name][2] = true // _isN2N

          // NOTE: Use `NameField` field-type
          if (!BIZZ_ENTITIES.includes(item.ref[0])) {
            item.type = item.ref[1]
          }
        }

        // No BARCODE field
        if (!(item.type === 'BARCODE' || $isSysMask(item.label))) {
          fields.push(item)

          if (item.type === 'REFERENCE' && item.name === 'approvalLastUser') {
            const item2 = { ...item, name: VF_ACU, label: $L('当前审批人') + ' (废弃)' }
            validFs.push(item2.name)
            REFENTITY_CACHE[item2.name] = item2.ref
            fields.push(item2)
          }
        }
      })
      this._fields = fields

      // init
      if (this.__initItems) {
        this.__initItems.forEach((item) => {
          if (item.field.substr(0, 1) === NAME_FLAG) item.field = item.field.substr(1)

          if (validFs.includes(item.field)) {
            this.addItem(item)
          } else {
            this.setState({ hasErrorTip: $L('存在无效字段，可能已被管理员删除，建议你调整后重新保存') })
            if (rb.env === 'dev') console.warn('Unkonw field : ' + JSON.stringify(item))
          }
        })
      }
    })
  }

  handleChange = (e) => {
    const name = e.target.dataset.id || e.target.name
    const value = e.target.value
    this.setState({ [name]: value }, () => {
      if (name === 'useEquation' && value === '9999') {
        if (this.state.equation === 'AND') this.setState({ equation: null })
      }
    })
  }

  onRef = (c) => this._itemsRef.push(c)

  addItem(props) {
    if (!this._fields) return

    const items = [...this.state.items]
    if (items.length >= 9) {
      RbHighbar.create($L('最多可添加 9 个条件'))
      return
    }

    const id = `item-${$random()}`
    let itemProps = {
      fields: this._fields,
      $$$parent: this,
      key: id,
      id: id,
      onRef: this.onRef,
      index: items.length + 1,
    }
    if (props) itemProps = { ...itemProps, ...props }
    items.push(<FilterItem {...itemProps} />)

    this.setState({ items }, () => this.renderEquation())
  }

  removeItem(id) {
    this._itemsRef = this._itemsRef.filter((c) => c.props.id !== id)
    const items = this.state.items.filter((c) => c.props.id !== id)

    this.setState({ items }, () => {
      this._itemsRef.forEach((c, i) => c.setIndex(i + 1))
      this.renderEquation()
    })
  }

  checkEquation(e) {
    const v = e.target.value
    if (v) {
      $.post('/app/entity/advfilter/test-equation', v, (res) => {
        this.setState({ equationError: res.error_code !== 0 })
      })
    } else {
      this.setState({ equationError: false })
    }
  }

  renderEquation() {
    const expr = []
    for (let i = 1; i <= (this.state.items || []).length; i++) expr.push(i)
    this.setState({ equationDef: expr.join(' OR ') })
  }

  toFilterData(canNoFilters) {
    const filters = []
    let hasError = false
    for (let i = 0; i < this._itemsRef.length; i++) {
      const item = this._itemsRef[i].getFilterData()
      if (!item) hasError = true
      else filters.push(item)
    }

    if (hasError) return RbHighbar.create($L('部分条件设置有误，请检查'))
    if (filters.length === 0 && canNoFilters !== true) return RbHighbar.create($L('请至少添加 1 个条件'))

    const adv = {
      entity: this.props.entity,
      items: filters,
    }
    if (this.state.useEquation === 'AND') {
      adv.equation = 'AND'
    } else if (this.state.useEquation === '9999') {
      if (this.state.equationError === true) return RbHighbar.create($L('无效高级表达式'))
      adv.equation = this.state.equation
    }

    if (rb.env === 'dev') console.log(JSON.stringify(adv))
    return adv
  }

  confirm() {
    const adv = this.toFilterData(this.props.canNoFilters)
    if (!adv) return

    const _onConfirm = this.props.confirm || this.props.onConfirm
    typeof _onConfirm === 'function' && _onConfirm(adv)

    this.props.inModal && this._dlg.hide()
  }

  show(state) {
    this.props.inModal && this._dlg.show(state)
  }

  hide() {
    const _onCancel = this.props.cancel || this.props.onCancel
    typeof _onCancel === 'function' && _onCancel()

    this.props.inModal && this._dlg.hide()
  }
}

const OP_TYPE = {
  LK: $L('包含'),
  NLK: $L('不包含'),
  IN: $L('包含'),
  NIN: $L('不包含'),
  EQ: $L('等于'),
  NEQ: $L('不等于'),
  GT: $L('大于'),
  LT: $L('小于'),
  GE: $L('大于等于'),
  LE: $L('小于等于'),
  BW: $L('区间'),
  NL: $L('为空'),
  NT: $L('不为空'),
  BFD: $L('..天前'),
  BFM: $L('..月前'),
  BFY: $L('..年前'),
  AFD: $L('..天后'),
  AFM: $L('..月后'),
  AFY: $L('..年后'),
  RED: $L('最近..天'),
  REM: $L('最近..月'),
  REY: $L('最近..年'),
  FUD: $L('未来..天'),
  FUM: $L('未来..月'),
  FUY: $L('未来..年'),
  SFU: $L('本人'),
  SFB: $L('本部门'),
  SFD: $L('本部门及子部门'),
  SFT: $L('所在团队'),
  YTA: $L('昨天'),
  TDA: $L('今天'),
  TTA: $L('明天'),
  CUW: $L('本周'),
  CUM: $L('本月'),
  CUQ: $L('本季度'),
  CUY: $L('本年'),
  PUW: $L('上周'),
  NUW: $L('下周'),
  PUM: $L('上月'),
  NUM: $L('下月'),
  PUQ: $L('上季度'),
  NUQ: $L('下季度'),
  PUY: $L('去年'),
  NUY: $L('明年'),
  EVW: $L('本周..'),
  EVM: $L('本月..'),
  DDD: $L('指定..天'),
  HHH: $L('指定..时'),
  REP: $L('重复'),
}
// prettier-ignore
const OP_NOVALUE = ['NL', 'NT', 'SFU', 'SFB', 'SFD', 'YTA', 'TDA', 'TTA', 'PUW', 'CUW', 'NUW', 'PUM', 'CUM', 'NUM', 'PUQ', 'CUQ', 'NUQ', 'PUY', 'CUY', 'NUY']
// prettier-ignore
const OP_DATE_NOPICKER = ['TDA', 'YTA', 'TTA', 'RED', 'REM', 'REY', 'FUD', 'FUM', 'FUY', 'BFD', 'BFM', 'BFY', 'AFD', 'AFM', 'AFY', 'PUW', 'CUW', 'NUW', 'PUM', 'CUM', 'NUM', 'PUQ', 'CUQ', 'NUQ', 'PUY', 'CUY', 'NUY', 'EVW', 'EVM', 'DDD', 'HHH', 'REP']
const REFENTITY_CACHE = {}
const PICKLIST_CACHE = {}

// 过滤项
class FilterItem extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }

    this._searchEntity = props.$$$parent.props.entity
    this._loadedPickList = false
    this._loadedBizzSearch = false
  }

  render() {
    return (
      <div className="row item">
        <div className="col-sm-5 field">
          <em>{this.state.index}</em>
          <i className="zmdi zmdi-minus-circle" title={$L('移除')} onClick={() => this.props.$$$parent.removeItem(this.props.id)} />
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
        <div className={`col-sm-5 val ${OP_NOVALUE.includes(this.state.op) && 'hide'}`}>{this.renderValue()}</div>
      </div>
    )
  }

  selectOp() {
    const fieldType = this.state.type

    // default
    let op = ['LK', 'NLK', 'EQ', 'NEQ']

    if (fieldType === 'NUMBER' || fieldType === 'DECIMAL') {
      op = ['GT', 'LT', 'EQ', 'BW', 'GE', 'LE']
    } else if (fieldType === 'DATE' || fieldType === 'DATETIME') {
      // prettier-ignore
      op = ['TDA', 'YTA', 'TTA', 'GT', 'LT', 'EQ', 'BW', 'RED', 'REM', 'REY', 'FUD', 'FUM', 'FUY', 'BFD', 'BFM', 'BFY', 'AFD', 'AFM', 'AFY', 'PUW', 'CUW', 'NUW', 'PUM', 'CUM', 'NUM', 'PUQ', 'CUQ', 'NUQ', 'PUY', 'CUY', 'NUY', 'EVW', 'EVM', 'DDD']
      if (fieldType === 'DATETIME') op.push('HHH')
    } else if (fieldType === 'TIME') {
      op = ['GT', 'LT', 'EQ', 'BW']
    } else if (fieldType === 'FILE' || fieldType === 'IMAGE' || fieldType === 'AVATAR' || fieldType === 'SIGN' || fieldType === 'ANYREFERENCE') {
      op = []
    } else if (fieldType === 'PICKLIST' || fieldType === 'STATE' || fieldType === 'MULTISELECT') {
      op = ['IN', 'NIN']
      if (fieldType === 'MULTISELECT') {
        op.push('EQ', 'NEQ')
      }
    } else if (fieldType === 'REFERENCE') {
      if (this.isBizzField('User')) {
        op = ['IN', 'NIN', 'SFU', 'SFB', 'SFT']
      } else if (this.isBizzField('Department')) {
        op = ['IN', 'NIN', 'SFB', 'SFD']
      } else if (this.isBizzField('Role') || this.isBizzField('Team')) {
        op = ['IN', 'NIN']
      }
      // else default
    } else if (fieldType === 'BOOL') {
      op = ['EQ']
    } else if (fieldType === 'LOCATION') {
      op = ['LK', 'NLK']
    } else if (fieldType === 'TAG') {
      op = ['IN', 'NIN']
    }

    // v3.6-b4,v3.7
    if (['TEXT', 'PHONE', 'EMAIL', 'URL', 'DATE', 'DATETIME', 'TIME'].includes(fieldType)) op.push('REP')

    if (this.isApprovalState()) op = ['IN', 'NIN']
    else if (this.state.field === VF_ACU) op = ['IN', 'SFU', 'SFB', 'SFT'] // v3.7 准备废弃
    else if (this.isN2NUsers()) op = ['IN', 'NIN', 'SFU', 'SFB', 'NL', 'NT']
    else op.push('NL', 'NT')

    this.__op = op
    return op
  }

  renderValue() {
    let valComp
    if (this.state.op === 'BW') {
      valComp = (
        <div className="val-range">
          <input className="form-control form-control-sm" ref={(c) => (this._filterVal = c)} onChange={(e) => this.valueHandle(e)} onBlur={(e) => this.valueCheck(e)} value={this.state.value || ''} />
          <input
            className="form-control form-control-sm"
            ref={(c) => (this._filterVal2 = c)}
            onChange={(e) => this.valueHandle(e)}
            onBlur={(e) => this.valueCheck(e)}
            value={this.state.value2 || ''}
            data-at="2"
          />
          <span>{$L('起')}</span>
          <span className="end">{$L('止')}</span>
        </div>
      )
    } else if (this.state.type === 'PICKLIST' || this.state.type === 'STATE' || this.state.type === 'MULTISELECT') {
      valComp = (
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
      valComp = <select className="form-control form-control-sm" multiple ref={(c) => (this._filterVal = c)} />
    } else if (this.state.type === 'BOOL') {
      valComp = (
        <select className="form-control form-control-sm" ref={(c) => (this._filterVal = c)}>
          <option value="T">{$L('是')}</option>
          <option value="F">{$L('否')}</option>
        </select>
      )
    } else {
      valComp = (
        <input className="form-control form-control-sm" ref={(c) => (this._filterVal = c)} onChange={(e) => this.valueHandle(e)} onBlur={(e) => this.valueCheck(e)} value={this.state.value || ''} />
      )
    }

    return valComp
  }

  // 引用 User/Department/Role/Team
  isBizzField(entity) {
    if (this.state.type === 'REFERENCE') {
      const ifRefField = REFENTITY_CACHE[this.state.field]
      if (entity) return ifRefField[0] === entity
      else return BIZZ_ENTITIES.includes(ifRefField[0])
    }
    // 多引用用户
    return this.isN2NUsers()
  }

  // v3.7: 多引用用户|当前审批人
  isN2NUsers() {
    const fieldName = this.state.field || ''
    if (fieldName === 'approvalStepUsers' || fieldName.endsWith('.approvalStepUsers')) return true
    if (this.state.type === 'N2NREFERENCE') {
      let ifRefField = REFENTITY_CACHE[fieldName]
      return ifRefField[0] === 'User'
    }
    return false
  }

  // 数字值
  isNumberValue() {
    if (this.state.type === 'NUMBER' || this.state.type === 'DECIMAL') {
      return true
    } else if ((this.state.type === 'DATE' || this.state.type === 'DATETIME') && OP_DATE_NOPICKER.includes(this.state.op)) {
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

    let $s2field, $s2op
    let op4init = this.props.op
    const that = this
    $s2field = $(this._filterField)
      .select2({ allowClear: false })
      .on('change', function (e) {
        const fieldAndType = e.target.value.split(NT_SPLIT)
        that.setState({ field: fieldAndType[0], type: fieldAndType[1] }, () => {
          if (op4init) {
            $s2op.val(op4init).trigger('change')
            op4init = null
          } else {
            $s2op.val(that.__op[0]).trigger('change')
          }
        })
      })
    $s2op = $(this._filterOp)
      .select2({ allowClear: false })
      .on('change', function (e) {
        that.setState({ op: e.target.value }, () => that._componentDidUpdate())
      })

    this.__select2 = [$s2field, $s2op]

    // init
    if (this.props.field) {
      let field = this.props.field
      $(this.props.fields).each(function () {
        if (this.name === field) {
          field = [field, this.type].join(NT_SPLIT)
          return false
        }
      })
      $s2field.val(field).trigger('change')
    } else {
      $s2field.trigger('change')
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

    if (state.type === 'DATE' || state.type === 'DATETIME' || state.type === 'TIME') {
      this.removeDatepicker()
      if (OP_DATE_NOPICKER.includes(state.op)) {
        // 无需日期组件
      } else {
        this.renderDatepicker(state.type === 'TIME')
      }
    } else if (lastType === 'DATE' || lastType === 'DATETIME' || lastType === 'TIME') {
      this.removeDatepicker()
    }

    if (this.isBizzField()) {
      let ifRefField = REFENTITY_CACHE[state.field]
      ifRefField = state.op === 'SFT' ? 'Team' : ifRefField[0]
      this.renderBizzSearch(ifRefField)
    } else if (lastType === 'REFERENCE' || lastType === 'N2NREFERENCE') {
      this.removeBizzSearch()
    }

    if (state.type === 'BOOL') {
      this.removeBool()
      if (OP_NOVALUE.includes(state.op)) {
        // 无需组件
      } else {
        this.renderBool()
      }
    } else if (lastType === 'BOOL') {
      this.removeBool()
    }

    if (state.value) this.valueCheck($(this._filterVal))
    if (state.value2 && this._filterVal2) this.valueCheck($(this._filterVal2))
  }

  componentWillUnmount() {
    this.__select2.forEach((item) => item.select2('destroy'))
    this.__select2 = null
    this.removePickList()
    this.removeDatepicker()
    this.removeBizzSearch()
    this.removeBool()
  }

  valueHandle(e) {
    const v = e.target.value
    if (~~e.target.dataset.at === 2) this.setState({ value2: v })
    else this.setState({ value: v })
  }

  // @e = el or event
  valueCheck(e) {
    const $el = e.target ? $(e.target) : e
    $el.removeClass('is-invalid')
    const v = e.target ? e.target.value : e.val()
    if ($empty(v)) {
      // 忽略数组
      if (typeof v !== 'object') $el.addClass('is-invalid')
    } else {
      if (/^\{@[a-z0-9._]{4,}}$/i.test(v)) {
        // pass: Field var {@FIELD}
      } else if (this.isNumberValue()) {
        if ($regex.isDecimal(v) === false) $el.addClass('is-invalid')
      } else if (this.state.type === 'DATE' || this.state.type === 'DATETIME') {
        if ($regex.isDate(v) === false) $el.addClass('is-invalid')
      } else if (this.state.type === 'TIME') {
        if ($regex.isTime(v) === false) $el.addClass('is-invalid')
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
    const $s2val = $(this._filterVal)
      .select2({
        width: this.props.select2Width,
        placeholder: this.props.select2Placeholder,
      })
      .on('change.select2', function () {
        that.setState({ value: $s2val.val().join('|') })
      })
    this.__select2_PickList = $s2val

    // Load
    if (this.props.value && this._loadedPickList === false) {
      const v = this.props.value.split('|')
      $s2val.val(v).trigger('change')
      this._loadedPickList = true
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
    // BIZZ 实体变了
    const clearSelected = this.__lastBizzEntity && this.__lastBizzEntity !== entity
    this.__lastBizzEntity = entity

    const that = this
    const $s2val = $(this._filterVal)
      .select2({
        width: this.props.select2Width,
        placeholder: this.props.select2Placeholder,
        minimumInputLength: 1,
        ajax: {
          url: '/commons/search/search',
          delay: 300,
          data: function (params) {
            return {
              entity: entity,
              q: params.term,
            }
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
        const val = $s2val.val()
        that.setState({ value: val.join('|') })
      })

    if (clearSelected) $s2val.val(null).trigger('change')
    this.__select2_BizzSearch = $s2val

    // Load
    if (this.props.value && this._loadedBizzSearch === false) {
      $.get(`/commons/search/read-labels?ids=${$encode(this.props.value)}`, (res) => {
        for (let kid in res.data) {
          const o = new Option(res.data[kid], kid, true, true)
          $s2val.append(o)
        }
      })
      this._loadedBizzSearch = true
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

  renderDatepicker(onlyTime) {
    let dpcfg = {
      format: 'yyyy-mm-dd',
      minView: 2,
      startView: 'month',
      todayBtn: true,
      clearBtn: this.props.allowClear || false,
      forceParse: false,
    }

    // 仅时间
    if (onlyTime) {
      dpcfg = {
        format: 'hh:ii',
        minView: 0,
        maxView: 1,
        startView: 1,
        title: $L('选择时间'),
        todayBtn: true,
        clearBtn: this.props.allowClear || false,
        forceParse: false,
      }
    }

    const that = this
    const $dp1 = $(this._filterVal).datetimepicker(dpcfg)
    $dp1.on('change.select2', function (e) {
      that.setState({ value: e.target.value }, () => {
        that.valueCheck($(that._filterVal))
      })
    })
    this.__datepicker = [$dp1]

    if (this._filterVal2) {
      const $dp2 = $(this._filterVal2).datetimepicker(dpcfg)
      $dp2.on('change.select2', function (e) {
        that.setState({ value2: e.target.value }, () => {
          that.valueCheck($(that._filterVal2))
        })
      })
      this.__datepicker.push($dp2)
    }
  }

  removeDatepicker() {
    if (this.__datepicker) {
      this.__datepicker.forEach((item) => item.datetimepicker('remove'))
      this.__datepicker = null
    }
  }

  // 布尔

  renderBool() {
    const that = this
    const $s2val = $(this._filterVal)
      .select2({
        width: this.props.select2Width,
        placeholder: this.props.select2Placeholder,
        allowClear: this.props.allowClear === true,
      })
      .on('change.select2', function () {
        that.setState({ value: $s2val.val() })
      })
    this.__select2_Bool = $s2val
    $s2val.val(this.props.value || 'T').trigger('change')
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

  getFilterData() {
    const s = this.state // DON'T CHANGES `state`!!!
    let noValue = false
    if (!s.value) {
      if (OP_NOVALUE.includes(s.op)) {
        noValue = true
      } else {
        return
      }
    } else if (OP_NOVALUE.includes(s.op)) {
      noValue = true
    }

    if (s.op === 'BW' && !s.value2) {
      return
    }

    // 允许无值
    if (noValue === true);
    else {
      if (!!s.value && ($(this._filterVal).hasClass('is-invalid') || $(this._filterVal2).hasClass('is-invalid'))) {
        return
      }
    }

    const item = {
      index: s.index,
      field: s.field,
      op: s.op,
    }
    if (noValue === false) {
      if (s.value) item.value = s.value
      if (s.value2) item.value2 = s.value2
    }

    // 引用字段查询名称字段
    const ifRefField = REFENTITY_CACHE[s.field]
    if (ifRefField && !(s.op === 'NL' || s.op === 'NT')) {
      if (BIZZ_ENTITIES.includes(ifRefField[0])) {
        if (ifRefField[2]) {
          if (!this.isN2NUsers()) item.field = NAME_FLAG + item.field
        }
      } else {
        item.field = NAME_FLAG + item.field
      }
    }

    this.setState({ hasError: false })
    return item
  }
}

// ~~ 高级查询:列表专用

// eslint-disable-next-line no-unused-vars
class ListAdvFilter extends AdvFilter {
  render() {
    const filterComp = super.render()
    return this.props.inModal ? filterComp : <div className="dropdown-menu-advfilter">{filterComp}</div>
  }

  renderAction() {
    return (
      <div className="item dialog-footer">
        {this.props.inModal ? (
          <RF>
            <div className="float-left" style={{ marginTop: -1 }}>
              {rb.isAdminUser ? (
                <Share2 ref={(c) => (this._Share2 = c)} shareTo={this.props.shareTo} noSwitch hasName configName={this.props.filterName} />
              ) : (
                <div className="float-left input">
                  <input
                    className="form-control form-control-sm text"
                    maxLength="20"
                    value={this.state.filterName || ''}
                    data-id="filterName"
                    onChange={this.handleChange}
                    placeholder={$L('输入名称')}
                  />
                </div>
              )}
            </div>
            <div className="float-right">
              <button
                className="btn btn-primary"
                type="button"
                onClick={() => {
                  const d = this._Share2 ? this._Share2.getData() : null
                  const name = d ? d.configName : this.state.filterName
                  const shareTo = d ? d.shareTo : null
                  this.post(name, shareTo)
                }}>
                {$L('保存')}
              </button>
              <button className="btn btn-primary btn-outline" type="button" onClick={() => this.searchNow()}>
                <i className="icon zmdi zmdi-search" /> {$L('查询')}
              </button>
            </div>
          </RF>
        ) : (
          <div className="float-right">
            <div className="btn-group">
              <button className="btn btn-primary btn-outline" type="button" onClick={() => this.searchNow()}>
                <i className="icon zmdi zmdi-search" /> {$L('查询')}
              </button>
              <button className="btn btn-primary btn-outline dropdown-toggle w-auto" type="button" data-toggle="dropdown" style={{ marginLeft: -1 }}>
                <i className="icon zmdi zmdi-chevron-down" />
              </button>
              <div className="dropdown-menu dropdown-menu-right">
                <a className="dropdown-item" onClick={() => this.handleNew()}>
                  {$L('保存到常用查询')}
                </a>
              </div>
            </div>
            <button className="btn btn-secondary" type="button" onClick={() => this.searchNow(true)}>
              <i className="icon mdi mdi-restore" /> {$L('重置')}
            </button>
          </div>
        )}
        <div className="clearfix" />
      </div>
    )
  }

  searchNow(reset) {
    if (reset) {
      RbListPage._RbList.search({ items: [] }, true)
    } else {
      const adv = this.toFilterData(true)
      if (adv) {
        const storageKey = `CustomAdv-${this.props.entity}`
        localStorage.setItem(storageKey, JSON.stringify(adv))
        RbListPage._RbList.search(adv, true)
      }
    }
  }

  post(name, shareTo) {
    const filter = this.toFilterData(this.props.canNoFilters)
    if (!filter) return

    let url = `/app/${this.props.entity}/advfilter/post?id=${this.props.id || ''}`
    if (name) url += `&name=${$encode(name)}`
    if (shareTo) url += `&shareTo=${$encode(shareTo)}`

    $.post(url, JSON.stringify(filter), (res) => {
      if (res.error_code === 0) {
        this.props.inModal && this._dlg.hide()
        typeof this.props.onConfirm === 'function' && this.props.onConfirm(res.data.id)
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }

  handleNew() {
    const filter = this.toFilterData(this.props.canNoFilters)
    if (!filter) return

    if (this._ListAdvFilterSave) {
      this._ListAdvFilterSave.show()
    } else {
      renderRbcomp(
        // eslint-disable-next-line react/jsx-no-undef
        <ListAdvFilterSave
          onConfirm={(d) => {
            this.post(d.name, d.shareTo)
          }}
          ref={(c) => (this._ListAdvFilterSave = c)}
        />
      )
    }
  }
}

class ListAdvFilterSave extends RbFormHandler {
  render() {
    return (
      <RbModal title={$L('保存高级查询')} ref={(c) => (this._dlg = c)}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('名称')}</label>
            <div className="col-sm-7">
              <input className="form-control form-control-sm" value={this.state.name || ''} placeholder={$L('我的查询')} data-id="name" onChange={this.handleChange} maxLength="20" />
            </div>
          </div>
          {rb.isAdminUser && (
            <div className="form-group row pt-0">
              <label className="col-sm-3 col-form-label text-sm-right" />
              <div className="col-sm-7">
                <div className="shareTo--wrap">
                  <Share2 ref={(c) => (this._Share2 = c)} noSwitch />
                </div>
              </div>
            </div>
          )}
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3">
              <button className="btn btn-primary" type="button" onClick={() => this.handleConfirm()}>
                {$L('确定')}
              </button>
              <button className="btn btn-link" type="button" onClick={() => this.hide()}>
                {$L('取消')}
              </button>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  handleConfirm() {
    const data = {
      name: this.state.name || '',
      shareTo: this._Share2 ? this._Share2.getData().shareTo : 'SELF',
    }
    typeof this.props.onConfirm === 'function' && this.props.onConfirm(data)

    this.setState({ name: '' })
    this.hide()
  }
}
