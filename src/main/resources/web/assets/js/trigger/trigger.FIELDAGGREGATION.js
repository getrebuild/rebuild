/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global FormulaAggregation, ActionContentSpec */

// ~~ 字段聚合
class ContentFieldAggregation extends ActionContentSpec {
  render() {
    return (
      <div className="field-aggregation">
        <form className="simple">
          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$L('目标实体')}</label>
            <div className="col-md-12 col-lg-9">
              <div className="row">
                <div className="col-5">
                  <select className="form-control form-control-sm" ref={(c) => (this._$targetEntity = c)}>
                    {(this.state.targetEntities || []).map((item) => {
                      const val = `${item[2]}.${item[0]}`
                      return (
                        <option key={val} value={val}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                </div>
              </div>
              {this.state.hasWarning && (
                <div className="form-text text-danger">
                  <i className="zmdi zmdi-alert-triangle fs-16 down-1 mr-1" />
                  {this.state.hasWarning}
                </div>
              )}
            </div>
          </div>

          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$L('聚合规则')}</label>
            <div className="col-md-12 col-lg-9">
              <div className="items">
                {(this.state.items || []).length > 0 &&
                  this.state.items.map((item) => {
                    return (
                      <div key={item.targetField}>
                        <div className="row">
                          <div className="col-5">
                            <span className="badge badge-warning">{_getFieldLabel(item.targetField, this.__targetFieldsCache)}</span>
                          </div>
                          <div className="col-2">
                            <i className="zmdi zmdi-forward zmdi-hc-rotate-180" />
                            <span className="badge badge-warning">{FormulaAggregation.CALC_MODES[item.calcMode]}</span>
                          </div>
                          <div className="col-5 del-wrap">
                            <span className="badge badge-warning">
                              {item.calcMode === 'FORMULA' ? FormulaAggregation.textFormula(item.sourceFormula, this.__sourceFieldsCache) : _getFieldLabel(item.sourceField, this.__sourceFieldsCache)}
                            </span>
                            <a className="del" title={$L('移除')} onClick={() => this.delItem(item.targetField)}>
                              <i className="zmdi zmdi-close" />
                            </a>
                          </div>
                        </div>
                      </div>
                    )
                  })}
              </div>
              <div className="row">
                <div className="col-5">
                  <select className="form-control form-control-sm" ref={(c) => (this._$targetField = c)}>
                    {(this.state.targetFields || []).map((item) => {
                      return (
                        <option key={item[0]} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                  <p>{$L('目标字段')}</p>
                </div>
                <div className="col-2 pr-0">
                  <i className="zmdi zmdi-forward zmdi-hc-rotate-180" />
                  <select className="form-control form-control-sm" ref={(c) => (this._$calcMode = c)}>
                    {(this.state.calcModes || []).map((item) => {
                      return (
                        <option key={item} value={item}>
                          {FormulaAggregation.CALC_MODES[item]}
                        </option>
                      )
                    })}
                  </select>
                  <p>{$L('聚合方式')}</p>
                </div>
                <div className="col-5">
                  <div className={this.state.calcMode === 'FORMULA' ? '' : 'hide'}>
                    <div className="form-control-plaintext formula" _title={$L('计算公式')} ref={(c) => (this._$sourceFormula = c)} onClick={() => this.showFormula()} />
                    <p>{$L('计算公式')}</p>
                  </div>
                  <div className={this.state.calcMode === 'FORMULA' ? 'hide' : ''}>
                    <select className="form-control form-control-sm" ref={(c) => (this._$sourceField = c)}>
                      {(this.state.sourceFields || []).map((item) => {
                        return (
                          <option key={item[0]} value={item[0]}>
                            {item[1]}
                          </option>
                        )
                      })}
                    </select>
                    <p>{$L('聚合字段')}</p>
                  </div>
                </div>
              </div>
              <div className="mt-1">
                <button type="button" className="btn btn-primary btn-sm btn-outline" onClick={() => this.addItem()}>
                  + {$L('添加')}
                </button>
              </div>
            </div>
          </div>

          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$L('聚合数据条件')}</label>
            <div className="col-md-12 col-lg-9">
              <a className="btn btn-sm btn-link pl-0 text-left down-2" onClick={() => this.dataAdvFilter()}>
                {this.state.dataFilterItems ? `${$L('已设置条件')} (${this.state.dataFilterItems})` : $L('点击设置')}
              </a>
              <div className="form-text mt-0">{$L('仅会聚合符合过滤条件的数据')}</div>
            </div>
          </div>

          <div className="form-group row pb-0">
            <label className="col-md-12 col-lg-3 col-form-label" />
            <div className="col-md-12 col-lg-9">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" ref={(c) => (this._$readonlyFields = c)} />
                <span className="custom-control-label">
                  {$L('自动设置目标字段为只读')}
                  <i className="zmdi zmdi-help zicon down-1" data-toggle="tooltip" title={$L('本选项仅针对表单有效')} />
                </span>
              </label>
              <div className="mt-2">
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                  <input className="custom-control-input" type="checkbox" ref={(c) => (this._$forceUpdate = c)} />
                  <span className="custom-control-label">
                    {$L('允许强制更新')}
                    <i className="zmdi zmdi-help zicon down-1" data-toggle="tooltip" title={$L('强制更新只读记录')} />
                  </span>
                </label>
              </div>
            </div>
          </div>
        </form>
      </div>
    )
  }

  componentDidMount() {
    const content = this.props.content
    this.__select2 = []

    $.get(`/admin/robot/trigger/field-aggregation-entities?source=${this.props.sourceEntity}`, (res) => {
      this.setState({ targetEntities: res.data }, () => {
        const $s2te = $(this._$targetEntity)
          .select2({ placeholder: $L('选择目标实体') })
          .on('change', () => this._changeTargetEntity())

        if (content && content.targetEntity) {
          $s2te.val(content.targetEntity)
          if (!$s2te.val()) this.setState({ hasWarning: `${$L('目标实体已经不可用')} [${content.targetEntity.toUpperCase()}]` })
          if (rb.env !== 'dev') $s2te.attr('disabled', true)
        }

        $s2te.trigger('change')
        this.__select2.push($s2te)
      })
    })

    if (content) {
      $(this._$readonlyFields).attr('checked', content.readonlyFields === true)
      $(this._$forceUpdate).attr('checked', content.forceUpdate === true)
      this.saveAdvFilter(content.dataFilter)
    }
  }

  _changeTargetEntity() {
    const te = ($(this._$targetEntity).val() || '').split('.')[1]
    if (!te) return
    // 清空现有规则
    this.setState({ items: [] })

    $.get(`/admin/robot/trigger/field-aggregation-fields?source=${this.props.sourceEntity}&target=${te}`, (res) => {
      this.__sourceFieldsCache = res.data.source
      this.__targetFieldsCache = res.data.target

      this.setState({ hasWarning: res.data.hadApproval ? $L('目标实体已启用审批流程，可能影响源实体操作 (触发动作)，建议启用“允许强制更新”') : null })

      if (this.state.sourceFields) {
        this.setState({ sourceFields: res.data.source }, () => $(this._$sourceField).trigger('change'))
      } else {
        // init
        this.setState({ sourceFields: res.data.source }, () => {
          let $s2sf, $s2cm

          $s2sf = $(this._$sourceField)
            .select2({ placeholder: $L('选择聚合字段') })
            .on('change', (e) => {
              let sf = e.target.value
              sf = this.__sourceFieldsCache.find((x) => x[0] === sf)
              if (!sf) return

              let cm = Object.keys(FormulaAggregation.CALC_MODES)
              if (['DATE', 'DATETIME'].includes(sf[2])) {
                cm = ['MAX', 'MIN', 'COUNT', 'COUNT2'] // in FormulaAggregation.CALC_MODES
              } else if (!['DATE', 'DATETIME', 'NUMBER', 'DECIMAL'].includes(sf[2])) {
                cm = ['COUNT', 'COUNT2'] // in FormulaAggregation.CALC_MODES
              }

              this.setState({ calcModes: cm }, () => $s2cm.trigger('change'))
            })

          $s2cm = $(this._$calcMode)
            .select2({ placeholder: $L('选择聚合方式') })
            .on('change', (e) => {
              const cm = e.target.value
              this.setState({ calcMode: cm })

              let sf = $s2sf.val()
              sf = this.__sourceFieldsCache.find((x) => x[0] === sf)
              if (!sf) return

              let fs = this.__targetFieldsCache.filter((x) => ['NUMBER', 'DECIMAL'].includes(x[2]))
              if (['DATE', 'DATETIME'].includes(sf[2]) && !['COUNT', 'COUNT2'].includes(cm)) {
                fs = this.__targetFieldsCache.filter((x) => ['DATE', 'DATETIME'].includes(x[2]))
              }

              this.setState({ targetFields: fs })
            })

          const $s2tf = $(this._$targetField).select2({ placeholder: $L('选择目标字段') })

          // 优先显示
          const useNum = this.__sourceFieldsCache.find((x) => ['NUMBER', 'DECIMAL'].includes(x[2]))
          if (useNum) {
            $s2sf.val(useNum[0])
          } else {
            const useDate = this.__sourceFieldsCache.find((x) => ['DATE', 'DATETIME'].includes(x[2]))
            if (useDate) $s2sf.val(useDate[0])
          }

          $s2sf.trigger('change')

          this.__select2.push($s2sf)
          this.__select2.push($s2cm)
          this.__select2.push($s2tf)
        })

        if (this.props.content) {
          this.setState({ items: this.props.content.items || [] })
        }
      }
    })
  }

  showFormula() {
    const fs = this.__sourceFieldsCache.filter((x) => x[2] === 'NUMBER' || x[2] === 'DECIMAL')
    renderRbcomp(
      <FormulaAggregation
        fields={fs}
        onConfirm={(v) => {
          $(this._$sourceFormula).attr('data-v', v).text(FormulaAggregation.textFormula(v, this.__sourceFieldsCache))
        }}
        verifyFormula
        entity={this.props.sourceEntity}
      />
    )
  }

  dataAdvFilter() {
    if (this._advFilter) {
      this._advFilter.show()
    } else {
      const that = this
      renderRbcomp(
        <AdvFilter title={$L('数据过滤条件')} inModal={true} canNoFilters={true} entity={this.props.sourceEntity} filter={that._advFilter__data} confirm={(f) => that.saveAdvFilter(f)} />,
        null,
        function () {
          that._advFilter = this
        }
      )
    }
  }

  saveAdvFilter(filter) {
    this._advFilter__data = filter
    this.setState({ dataFilterItems: filter && filter.items ? filter.items.length : 0 })
  }

  addItem() {
    const tf = $(this._$targetField).val()
    const calc = $(this._$calcMode).val()
    const sf = calc === 'FORMULA' ? null : $(this._$sourceField).val()
    const formula = calc === 'FORMULA' ? $(this._$sourceFormula).attr('data-v') : null

    if (!tf) return RbHighbar.create($L('请选择目标字段'))
    if (calc === 'FORMULA') {
      if (!formula) return RbHighbar.create($L('请输入计算公式'))
    } else if (!sf) {
      return RbHighbar.create($L('请选择聚合字段'))
    }

    // 目标字段=源字段
    const tfFull = `${$(this._$targetEntity).val().split('.')[0]}.${tf}`.replace('$PRIMARY$.', '')
    if (sf === tfFull) return RbHighbar.create($L('目标字段与聚合字段不能为同一字段'))

    const items = this.state.items || []
    const exists = items.find((x) => x.targetField === tf)
    if (exists) return RbHighbar.create($L('目标字段已添加'))

    items.push({ targetField: tf, calcMode: calc, sourceField: sf, sourceFormula: formula })
    this.setState({ items: items }, () => $(this._$sourceFormula).empty())
  }

  delItem(targetField) {
    const itemsNew = (this.state.items || []).filter((x) => x.targetField !== targetField)
    this.setState({ items: itemsNew })
  }

  buildContent() {
    const content = {
      targetEntity: $(this._$targetEntity).val(),
      items: this.state.items || [],
      readonlyFields: $(this._$readonlyFields).prop('checked'),
      forceUpdate: $(this._$forceUpdate).prop('checked'),
      dataFilter: this._advFilter__data,
    }

    if (!content.targetEntity) {
      RbHighbar.create($L('请选择目标实体'))
      return false
    }
    if (content.items.length === 0) {
      RbHighbar.create($L('请至少添加 1 个聚合规则'))
      return false
    }

    return content
  }
}

const _getFieldLabel = function (field, fields) {
  const x = fields.find((x) => x[0] === field)
  return x ? x[1] : `[${field.toUpperCase()}]`
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  renderRbcomp(<ContentFieldAggregation {...props} />, 'react-content', function () {
    // eslint-disable-next-line no-undef
    contentComp = this
    $('#react-content [data-toggle="tooltip"]').tooltip()
  })

  // eslint-disable-next-line no-undef
  useExecManual()
}
