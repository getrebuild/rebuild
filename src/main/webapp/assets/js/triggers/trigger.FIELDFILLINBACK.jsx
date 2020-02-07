// @see trigger.FIELDAGGREGATION.jsx auto-fillin.jsx

// ~~ 数据回填
// eslint-disable-next-line no-undef
class ContentFieldFillinback extends ActionContentSpec {

  constructor(props) {
    super(props)
  }

  render() {
    return <div className="field-aggregation field-fillinback">
      <form className="simple">
        <div className="form-group row">
          <label className="col-md-12 col-lg-3 col-form-label text-lg-right">回填目标实体</label>
          <div className="col-md-12 col-lg-9">
            <div className="row">
              <div className="col-5">
                <select className="form-control form-control-sm" ref={(c) => this._targetEntity = c}>
                  {(this.state.targetEntities || []).map((item) => {
                    return <option key={'te-' + item[2] + item[0]} value={item[2] + '.' + item[0]}>{item[1]}</option>
                  })}
                </select>
              </div>
            </div>
            {this.state.hadApproval
              && <div className="form-text text-danger"><i className="zmdi zmdi-alert-triangle fs-16 down-1"></i> 目标实体已启用审批流程，可能影响源实体操作（触发动作）</div>}
          </div>
        </div>
        <div className="form-group row">
          <label className="col-md-12 col-lg-3 col-form-label text-lg-right">回填规则</label>
          <div className="col-md-12 col-lg-9">
            <div className="items">
              {(!this.state.items || this.state.items.length === 0) ? null : this.state.items.map((item) => {
                return <div key={'item-' + item.targetField}>
                  <div className="row">
                    <div className="col-5">
                      <span className="badge badge-warning">{this.__fieldLabel(this.__targetFieldsCache, item.targetField)}</span>
                    </div>
                    <div className="col-5 del-wrap">
                      <span className="zmdi zmdi-forward zmdi-hc-rotate-180"></span>
                      <span className="badge badge-warning">
                        {item.calcMode === 'FORMULA' ? this.textFormula(item.sourceFormula) : this.__fieldLabel(this.__sourceFieldsCache, item.sourceField)}
                      </span>
                      <a className="del" title="移除" onClick={() => this.delItem(item.targetField)}><span className="zmdi zmdi-close"></span></a>
                    </div>
                  </div>
                </div>
              })}
            </div>
            <div className="row">
              <div className="col-5">
                <select className="form-control form-control-sm" ref={(c) => this._targetField = c}>
                  {(this.state.targetFields || []).map((item) => {
                    return <option key={'tf-' + item[0]} value={item[0]}>{item[1]}</option>
                  })}
                </select>
                <p>目标字段</p>
              </div>
              <div className="col-5">
                <span className="zmdi zmdi-forward zmdi-hc-rotate-180"></span>
                <select className="form-control form-control-sm" ref={(c) => this._sourceField = c}>
                  {(this.state.sourceFields || []).map((item) => {
                    return <option key={'sf-' + item[0]} value={item[0]}>{item[1]}</option>
                  })}
                </select>
                <p>源字段</p>
              </div>
            </div>
            <div className="mt-1">
              <button type="button" className="btn btn-primary btn-sm bordered" onClick={() => this.addItem()}>添加</button>
            </div>
          </div>
        </div>
        <div className="form-group row pb-0">
          <label className="col-md-12 col-lg-3 col-form-label text-lg-right"></label>
          <div className="col-md-12 col-lg-9">
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
              <input className="custom-control-input" type="checkbox" ref={(c) => this._readonlyFields = c} />
              <span className="custom-control-label">自动设置目标字段为只读</span>
            </label>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-md-12 col-lg-3 col-form-label text-lg-right">回填数据条件</label>
          <div className="col-md-12 col-lg-9">
            <a className="btn btn-sm btn-link pl-0 text-left down-2" onClick={this._dataAdvFilter}>
              {this.state.dataFilterItems ? `已设置条件 (${this.state.dataFilterItems})` : '点击设置'}
            </a>
            <div className="form-text mt-0">仅会回填符合过滤条件的数据</div>
          </div>
        </div>
      </form>
    </div>
  }

  componentDidMount() {
    const content = this.props.content
    this.__select2 = []
    $.get(`${rb.baseUrl}/admin/robot/trigger/field-aggregation-entities?source=${this.props.sourceEntity}`, (res) => {
      this.setState({ targetEntities: res.data }, () => {
        const s2te = $(this._targetEntity).select2({ placeholder: '选择回填目标实体' })
          .on('change', () => this.changeTargetEntity())

        if (content && content.targetEntity) {
          s2te.val(content.targetEntity)
          if (rb.env !== 'dev') s2te.attr('disabled', true)
        }
        s2te.trigger('change')
        this.__select2.push(s2te)
      })
    })

    if (content) {
      $(this._readonlyFields).attr('checked', content.readonlyFields === true)
      this._saveAdvFilter(this.props.content.dataFilter)
    }
  }

  changeTargetEntity() {
    // 清空现有规则
    this.setState({ items: [] })

    let te = $(this._targetEntity).val()
    if (!te) return
    te = te.split('.')[1]
    $.get(`${rb.baseUrl}/admin/robot/trigger/field-fillinback-fields?source=${this.props.sourceEntity}&target=${te}`, (res) => {
      this.setState({ hadApproval: res.data.hadApproval })
      this.__targetFieldsCache = res.data.target
      this.__sourceFieldsCache = res.data.source

      if (this.state.targetFields) {
        this.setState({ targetFields: this.selectTargetFields() })
      } else {
        this.setState({ sourceFields: res.data.source, targetFields: [] }, () => {
          const s2sf = $(this._sourceField).select2({ placeholder: '选择源字段' })
            .on('change', () => this.setState({ targetFields: this.selectTargetFields() }))
          const s2tf = $(this._targetField).select2({ placeholder: '选择目标字段' })
          s2sf.trigger('change')
          this.__select2.push(s2sf)
          this.__select2.push(s2tf)
        })

        if (this.props.content) this.setState({ items: this.props.content.items || [] })
      }
    })
  }

  __fieldLabel(fields, field) {
    let found = fields.find((x) => { return x[0] === field })
    if (found) found = found[1]
    return found || ('[' + field.toUpperCase() + ']')
  }

  // 获取可回填字段（兼容的）
  selectTargetFields() {
    const sf = $(this._sourceField).val()
    const source = this.__sourceFieldsCache.find((x) => { return x[0] === sf })

    let canFillinByType = CAN_FILLINBACK_MAPPINGS[source[2]] || []
    canFillinByType.push('TEXT')
    canFillinByType.push('NTEXT')

    let tFields = []
    $(this.__targetFieldsCache).each(function () {
      if ((source[2] === 'FILE' && this[2] !== 'FILE') || (source[2] === 'IMAGE' && this[2] !== 'IMAGE')) return
      if (source[2] === this[2] || canFillinByType.includes(this[2])) {
        if (source[2] === 'REFERENCE' || source[2] === 'STATE') {
          if (source[3] === this[3]) tFields.push(this)
        } else {
          tFields.push(this)
        }
      }
    })
    console.log(sf + ' >> ' + JSON.stringify(tFields))
    return tFields
  }

  addItem() {
    const tf = $(this._targetField).val()
    const sf = $(this._sourceField).val()
    if (!tf) { RbHighbar.create('请选择目标字段'); return false }
    if (!sf) { RbHighbar.create('请选择源字段'); return false }

    const items = this.state.items || []
    const found = items.find((x) => { return x.targetField === tf })
    if (found) { RbHighbar.create('目标字段重复'); return false }

    items.push({ targetField: tf, sourceField: sf })
    this.setState({ items: items })
  }

  delItem(targetField) {
    const items = (this.state.items || []).filter((item) => {
      return item.targetField !== targetField
    })
    this.setState({ items: items })
  }

  buildContent() {
    const content = {
      targetEntity: $(this._targetEntity).val(),
      items: this.state.items,
      readonlyFields: $(this._readonlyFields).prop('checked'),
      dataFilter: this._advFilter__data
    }
    if (!content.targetEntity) { RbHighbar.create('请选择聚合目标实体'); return false }
    if (content.items.length === 0) { RbHighbar.create('请至少添加 1 个聚合规则'); return false }
    return content
  }

  _dataAdvFilter = () => {
    const that = this
    if (that._advFilter) that._advFilter.show()
    else renderRbcomp(<AdvFilter title="数据过滤条件" inModal={true} canNoFilters={true}
      entity={this.props.sourceEntity}
      filter={that._advFilter__data}
      confirm={that._saveAdvFilter} />, null, function () { that._advFilter = this })
  }

  _saveAdvFilter = (filter) => {
    this._advFilter__data = filter
    this.setState({ dataFilterItems: filter && filter.items ? filter.items.length : 0 })
  }
}

const CAN_FILLINBACK_MAPPINGS = {
  'NUMBER': ['DECIMAL'],
  'DECIMAL': ['NUMBER'],
  'DATE': ['DATETIME'],
  'DATETIME': ['DATE'],
}

renderContentComp = function (props) {
  renderRbcomp(<ContentFieldFillinback {...props} />, 'react-content', function () { contentComp = this })
}