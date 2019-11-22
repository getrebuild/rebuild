const CALC_MODES = { 'SUM': '求和', 'COuNT': '计数', 'AVG': '平均值', 'MAX': '最大', 'MIN': '最小' }

// ~~ 数据聚合
// eslint-disable-next-line no-undef
class ContentFieldAggregation extends ActionContentSpec {
  constructor(props) {
    super(props)
  }
  render() {
    return <div className="field-aggregation">
      <form className="simple">
        <div className="form-group row">
          <label className="col-md-12 col-lg-3 col-form-label text-lg-right">聚合目标实体</label>
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
            {this.state.hadApproval && <div className="form-text text-danger"><i className="zmdi zmdi-alert-triangle fs-16 down-1"></i> 目标实体已启用审批流程，可能影响源实体操作（触发动作）</div>}
          </div>
        </div>
        <div className="form-group row">
          <label className="col-md-12 col-lg-3 col-form-label text-lg-right">聚合规则</label>
          <div className="col-md-12 col-lg-9">
            <div className="items">
              {(!this.state.items || this.state.items.length === 0) ? null : this.state.items.map((item) => {
                return (<div key={'item-' + item.targetField}><div className="row">
                  <div className="col-5"><span className="badge badge-warning">{this.__getFieldLabel(this.state.targetFields, item.targetField)}</span></div>
                  <div className="col-2">
                    <span className="zmdi zmdi-forward zmdi-hc-rotate-180"></span>
                    <span className="badge badge-warning">{CALC_MODES[item.calcMode]}</span>
                  </div>
                  <div className="col-5">
                    <span className="badge badge-warning">{this.__getFieldLabel(this.state.sourceFields, item.sourceField)}</span>
                    <a className="del" title="移除" onClick={() => this.delItem(item.targetField)}><span className="zmdi zmdi-close"></span></a>
                  </div>
                </div></div>)
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
              <div className="col-2 pr-0">
                <span className="zmdi zmdi-forward zmdi-hc-rotate-180"></span>
                <select className="form-control form-control-sm" ref={(c) => this._calcMode = c}>
                  {Object.keys(CALC_MODES).map((item) => {
                    return <option key={'opt-' + item} value={item}>{CALC_MODES[item]}</option>
                  })}
                </select>
                <p>聚合方式</p>
              </div>
              <div className="col-5">
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
          <label className="col-md-12 col-lg-3 col-form-label text-lg-right">聚合数据条件</label>
          <div className="col-md-12 col-lg-9">
            <a className="btn btn-sm btn-link pl-0 text-left down-2" onClick={this._dataAdvFilter}>
              {this.state.dataFilterItems ? `已设置条件 (${this.state.dataFilterItems})` : '点击设置'}
            </a>
            <p className="form-text mb-0 mt-0">仅会聚合符合过滤条件的数据</p>
          </div>
        </div>
      </form>
    </div>
  }

  componentDidMount() {
    this.__select2 = []
    $.get(`${rb.baseUrl}/admin/robot/trigger/field-aggregation-entities?source=${this.props.sourceEntity}`, (res) => {
      this.setState({ targetEntities: res.data }, () => {
        let s2te = $(this._targetEntity).select2({ placeholder: '选择聚合目标实体' })
          .on('change', () => {
            this.__changeTargetEntity()
          })
        s2te.trigger('change')

        if (this.props.content && this.props.content.targetEntity) {
          s2te.val(this.props.content.targetEntity)
          if (rb.env !== 'dev') {
            s2te.attr('disabled', true)
          }
        }
        s2te.trigger('change')
        this.__select2.push(s2te)
      })
    })

    if (this.props.content) {
      $(this._readonlyFields).attr('checked', this.props.content.readonlyFields === true)
      this._saveAdvFilter(this.props.content.dataFilter)
    }
  }

  __changeTargetEntity() {
    // 清空现有规则
    this.setState({ items: [] })

    let te = $(this._targetEntity).val()
    if (!te) return
    te = te.split('.')[1]
    $.get(`${rb.baseUrl}/admin/robot/trigger/field-aggregation-fields?source=${this.props.sourceEntity}&target=${te}`, (res) => {
      this.setState({ hadApproval: res.data.hadApproval })
      if (this.state.targetFields) {
        this.setState({ targetFields: res.data.target })
      } else {
        this.setState({ sourceFields: res.data.source, targetFields: res.data.target }, () => {
          let s2sf = $(this._sourceField).select2({ placeholder: '选择源字段' })
          let s2cm = $(this._calcMode).select2({ placeholder: '选择聚合方式' })
          let s2tf = $(this._targetField).select2({ placeholder: '选择目标字段' })
          this.__select2.push(s2sf)
          this.__select2.push(s2cm)
          this.__select2.push(s2tf)
        })

        if (this.props.content && this.props.content.items) {
          this.setState({ items: this.props.content.items })
        }
      }
    })
  }
  __getFieldLabel(list, field) {
    for (let i = 0; i < list.length; i++) {
      if (list[i][0] === field) {
        return list[i][1]
      }
    }
    return '[' + field.toUpperCase() + ']'
  }

  addItem() {
    let tf = $(this._targetField).val()
    let sf = $(this._sourceField).val()
    if (!tf) { RbHighbar.create('请选择目标字段'); return false }
    if (!sf) { RbHighbar.create('请选择源字段'); return false }

    let items = this.state.items || []
    $(items).each(function () {
      if (this.targetField === tf) {
        RbHighbar.create('目标字段重复')
        items = null
        return false
      }
    })

    if (items) {
      items.push({ sourceField: sf, calcMode: $(this._calcMode).val(), targetField: tf })
      this.setState({ items: items })
    }
  }
  delItem(targetField) {
    let items = (this.state.items || []).filter((item) => {
      return item.targetField !== targetField
    })
    this.setState({ items: items })
  }

  buildContent() {
    let _data = {
      targetEntity: $(this._targetEntity).val(),
      items: this.state.items,
      readonlyFields: $(this._readonlyFields).prop('checked'),
      dataFilter: this._advFilter__data
    }
    if (!_data.targetEntity) { RbHighbar.create('请选择聚合目标实体'); return false }
    if (_data.items.length === 0) { RbHighbar.create('请至少添加 1 个聚合规则'); return false }
    return _data
  }

  _dataAdvFilter = () => {
    let that = this
    if (that._advFilter) that._advFilter.show()
    else renderRbcomp(<AdvFilter title="数据过滤条件" inModal={true} canNoFilters={true}
      entity={this.props.sourceEntity}
      filter={that._advFilter__data}
      confirm={that._saveAdvFilter} />, null, function () { that._advFilter = this })
  }
  _saveAdvFilter = (filter) => {
    this._advFilter__data = filter
    let num = filter && filter.items ? filter.items.length : 0
    this.setState({ dataFilterItems: num })
  }
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  // eslint-disable-next-line no-undef
  renderRbcomp(<ContentFieldAggregation {...props} />, 'react-content', function () { contentComp = this })
}