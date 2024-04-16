/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global FormulaAggregation, ActionContentSpec */

const CALC_MODES2 = {
  ...FormulaAggregation.CALC_MODES,
  RBJOIN: $L('连接'),
  RBJOIN2: $L('去重连接'),
  RBJOIN3: $L('去重连接*N'),
}

// ~~ 分组聚合
class ContentGroupAggregation extends ActionContentSpec {
  render() {
    return (
      <div className="field-aggregation group-aggregation">
        <form className="simple">
          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$L('目标实体')}</label>
            <div className="col-md-12 col-lg-9">
              <div className="row">
                <div className="col-5">
                  <select className="form-control form-control-sm" ref={(c) => (this._$targetEntity = c)}>
                    {(this.state.targetEntities || []).map((item) => {
                      return (
                        <option key={item[0]} value={item[0]}>
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
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$L('分组字段关联')}</label>
            <div className="col-md-12 col-lg-9">
              {this.state.groupFields && this.state.groupFields.length > 0 && (
                <div style={{ marginBottom: 12 }}>
                  {this.state.groupFields.map((item) => {
                    return (
                      <span className="mt-1 d-inline-block" key={item.targetField}>
                        <span className="badge badge-primary badge-close m-0 mr-1">
                          <span>{FormulaAggregation.getLabel(item.targetField, this.__targetGroupFieldsCache)}</span>
                          <i className="mdi mdi-arrow-left-right ml-2 mr-2" />
                          <span>{FormulaAggregation.getLabel(item.sourceField, this.__sourceGroupFieldsCache)}</span>
                          <a className="close down-1" title={$L('移除')} onClick={(e) => this.delGroupField(item.targetField, e)}>
                            <i className="mdi mdi-close" />
                          </a>
                        </span>
                      </span>
                    )
                  })}
                </div>
              )}
              <div className="row">
                <div className="col-5">
                  <select className="form-control form-control-sm" ref={(c) => (this._$targetGroupField = c)}>
                    {(this.state.targetGroupFields || []).map((item) => {
                      if (['createdBy', 'createdOn', 'modifiedBy', 'modifiedOn', 'owningUser', 'owningDept'].includes(item.name) || item.type === 'DATETIME') return null

                      return (
                        <option key={item.name} value={item.name}>
                          {item.label}
                        </option>
                      )
                    })}
                  </select>
                  <p>{$L('目标字段')}</p>
                </div>
                <div className="col-5">
                  <i className="zmdi mdi mdi-arrow-left-right" />
                  <select className="form-control form-control-sm" ref={(c) => (this._$sourceGroupField = c)}>
                    {(this.state.sourceGroupFields || []).map((item) => {
                      return (
                        <option key={item.name} value={item.name}>
                          {item.label}
                        </option>
                      )
                    })}
                  </select>
                  <p>{$L('源字段')}</p>
                </div>
              </div>
              <div className="mt-1">
                <button type="button" className="btn btn-primary btn-sm btn-outline" onClick={() => this.addGroupField()}>
                  + {$L('添加')}
                </button>
              </div>
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
                            <span className="badge badge-warning">{FormulaAggregation.getLabel(item.targetField, this.__targetFieldsCache)}</span>
                          </div>
                          <div className="col-2">
                            <i className="zmdi zmdi-forward zmdi-hc-rotate-180" />
                            <span className="badge badge-warning">{CALC_MODES2[item.calcMode]}</span>
                          </div>
                          <div className="col-5 del-wrap">
                            <span className="badge badge-warning">
                              {item.calcMode === 'FORMULA'
                                ? FormulaAggregation.textFormula(item.sourceFormula, this.__sourceFieldsCache)
                                : FormulaAggregation.getLabel(item.sourceField, this.__sourceFieldsCache)}
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
                        <option key={item.name} value={item.name}>
                          {item.label}
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
                          {CALC_MODES2[item]}
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
                          <option key={item.name} value={item.name}>
                            {item.label}
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
              <div className="form-text mt-0">{$L('符合聚合数据条件的记录才会被聚合')}</div>
            </div>
          </div>

          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right" />
            <div className="col-md-12 col-lg-9">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" ref={(c) => (this._$autoCreate = c)} />
                <span className="custom-control-label">{$L('目标记录不存在时自动新建')}</span>
              </label>
              <div className="mt-2">
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                  <input className="custom-control-input" type="checkbox" ref={(c) => (this._$readonlyFields = c)} />
                  <span className="custom-control-label">
                    {$L('自动设置目标字段为只读')}
                    <i className="zmdi zmdi-help zicon down-1" data-toggle="tooltip" title={$L('本选项仅针对表单有效')} />
                  </span>
                </label>
              </div>
              <div className="mt-2">
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                  <input className="custom-control-input" type="checkbox" ref={(c) => (this._$forceUpdate = c)} />
                  <span className="custom-control-label">
                    {$L('允许强制更新')}
                    <i className="zmdi zmdi-help zicon down-1" data-toggle="tooltip" title={$L('强制更新只读记录')} />
                  </span>
                </label>
              </div>
              <div className="mt-2 bosskey-show">
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                  <input className="custom-control-input" type="checkbox" ref={(c) => (this._$stopPropagation = c)} />
                  <span className="custom-control-label">{$L('禁用传播')} (LAB)</span>
                </label>
              </div>
            </div>
          </div>

          <div className="form-group row">
            <label className="col-md-12 col-lg-3 col-form-label text-lg-right">{$L('聚合后回填')}</label>
            <div className="col-md-12 col-lg-9">
              <div className="col-6 pl-0 pr-0">
                <select className="form-control form-control-sm" style={{ maxWidth: 300 }} ref={(c) => (this._$fillbackField = c)}>
                  {(this.state.fillbackFields || []).map((item) => {
                    return (
                      <option key={item.name} value={item.name}>
                        {item.label}
                      </option>
                    )
                  })}
                </select>
              </div>
              <div className="form-text">{$L('可将聚合后的记录 ID 回填至源记录中')}</div>
            </div>
          </div>
        </form>
      </div>
    )
  }

  componentDidMount() {
    const content = this.props.content
    this.__select2 = []

    $.get(`/admin/robot/trigger/group-aggregation-entities?source=${this.props.sourceEntity}`, (res) => {
      this.__sourceFieldsCache = res.data.sourceFields
      this.__sourceGroupFieldsCache = res.data.sourceGroupFields

      this.setState({ ...res.data }, () => {
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
      $(this._$autoCreate).attr('checked', content.autoCreate === true)
      $(this._$readonlyFields).attr('checked', content.readonlyFields === true)
      $(this._$forceUpdate).attr('checked', content.forceUpdate === true)
      if (content.stopPropagation === true) {
        $(this._$stopPropagation).attr('checked', true).parents('.bosskey-show').removeClass('bosskey-show')
      }
      this.saveAdvFilter(content.dataFilter)
    } else {
      $(this._$autoCreate).attr('checked', true)
    }
  }

  _changeTargetEntity() {
    const te = $(this._$targetEntity).val()
    if (!te) return
    // 清空现有规则
    this.setState({ items: [], groupFields: [], fillbackFields: [] })

    $.get(`/admin/robot/trigger/group-aggregation-fields?source=${this.props.sourceEntity}&target=${te}`, (res) => {
      this.setState({ hasWarning: res.data.hadApproval ? $L('目标实体已启用审批流程，可能影响源实体操作 (触发动作)，建议启用“允许强制更新”') : null })

      this.__targetFieldsCache = res.data.targetFields
      this.__targetGroupFieldsCache = res.data.targetGroupFields

      const fb = this.__sourceGroupFieldsCache.filter((x) => x.type === `REFERENCE:${te}`)
      this.setState({ fillbackFields: fb }, () => {
        $(this._$fillbackField).val(null).trigger('change')
      })

      if (this.state.targetFields) {
        this.setState({ ...res.data }, () => {
          $(this._$targetGroupField).trigger('change')
          $(this._$calcMode).trigger('change')
        })
      } else {
        // init
        this.setState({ ...res.data }, () => {
          // 分组字段关联
          let $s2tgf, $s2sgf

          $s2tgf = $(this._$targetGroupField)
            .select2({ placeholder: $L('选择目标字段') })
            .on('change', () => {
              let tgf = $s2tgf.val()
              if (!tgf) return
              tgf = this.state.targetGroupFields.find((x) => x.name === tgf)

              // 仅同类型的字段（DATE DATETIME 兼容）
              const sgfAllow = this.__sourceGroupFieldsCache.filter((x) => {
                if (tgf.type === 'DATE' && x.type === 'DATETIME') return true
                if (tgf.type === 'DATETIME' && x.type === 'DATE') return true
                return x.type === tgf.type
              })
              this.setState({ sourceGroupFields: sgfAllow })
            })

          $s2sgf = $(this._$sourceGroupField)
            .select2({ placeholder: $L('选择源字段') })
            .on('change', () => {})

          $s2tgf.trigger('change')

          this.__select2.push($s2tgf)
          this.__select2.push($s2sgf)

          // 聚合规则

          let $s2sf, $s2cm, $s2tf

          $s2sf = $(this._$sourceField)
            .select2({ placeholder: $L('选择聚合字段') })
            .on('change', (e) => {
              let sf = e.target.value
              sf = this.__sourceFieldsCache.find((x) => x.name === sf)
              if (!sf) return

              let cmAllow = Object.keys(FormulaAggregation.CALC_MODES)
              if (['DATE', 'DATETIME'].includes(sf.type)) {
                cmAllow = ['MAX', 'MIN', 'COUNT', 'COUNT2', 'RBJOIN', 'RBJOIN2', 'RBJOIN3', 'FORMULA']
              } else if (!['DATE', 'DATETIME', 'NUMBER', 'DECIMAL'].includes(sf.type)) {
                cmAllow = ['COUNT', 'COUNT2', 'RBJOIN', 'RBJOIN2', 'RBJOIN3', 'FORMULA']
              }

              this.setState({ calcModes: cmAllow }, () => $s2cm.trigger('change'))
            })

          $s2cm = $(this._$calcMode)
            .select2({ placeholder: $L('选择聚合方式') })
            .on('change', (e) => {
              const cm = e.target.value
              this.setState({ calcMode: cm })

              let sf = $s2sf.val()
              sf = this.__sourceFieldsCache.find((x) => x.name === sf)
              if (!sf) return

              let tfAllow = this.__targetFieldsCache.filter((x) => ['NUMBER', 'DECIMAL'].includes(x.type))
              if ('RBJOIN' === cm || 'RBJOIN2' === cm || 'RBJOIN3' === cm) {
                tfAllow = this.__targetFieldsCache.filter((x) => {
                  if ('NTEXT' === x.type) return true
                  if ('N2NREFERENCE' === x.type) return sf.ref && sf.ref[0] === x.ref[0]
                  if ('FILE' === x.type) return true
                  return false
                })
              } else if (['DATE', 'DATETIME'].includes(sf.type) && !['COUNT', 'COUNT2', 'FORMULA'].includes(cm)) {
                tfAllow = this.__targetFieldsCache.filter((x) => ['DATE', 'DATETIME'].includes(x.type))
              }

              this.setState({ targetFields: tfAllow })
            })

          $s2tf = $(this._$targetField)
            .select2({ placeholder: $L('选择目标字段') })
            .on('change', () => {})

          // 优先显示
          const useNum = this.__sourceFieldsCache.find((x) => ['NUMBER', 'DECIMAL'].includes(x.type))
          if (useNum) {
            $s2sf.val(useNum.name)
          } else {
            const useDate = this.__sourceFieldsCache.find((x) => ['DATE', 'DATETIME'].includes(x.type))
            if (useDate) $s2sf.val(useDate.name)
          }

          $s2sf.trigger('change')

          this.__select2.push($s2sf)
          this.__select2.push($s2cm)
          this.__select2.push($s2tf)

          // 回填
          const $fbf = $(this._$fillbackField).select2({ placeholder: $L('(可选)'), allowClear: true })
          this.__select2.push($fbf)

          if (this.props.content) {
            this.setState({
              groupFields: this.props.content.groupFields || [],
              items: this.props.content.items || [],
            })

            setTimeout(() => {
              if (this.props.content.fillbackField) {
                $(this._$fillbackField).val(this.props.content.fillbackField).trigger('change')
              }
            }, 200)
          }
        })
      } // End `if`
    })
  }

  showFormula() {
    const sfAllow = this.__sourceFieldsCache.filter((x) => x.type === 'NUMBER' || x.type === 'DECIMAL')
    renderRbcomp(
      <FormulaAggregation
        fields={sfAllow}
        onConfirm={(v) => {
          $(this._$sourceFormula).attr('data-v', v).text(FormulaAggregation.textFormula(v, this.__sourceFieldsCache))
        }}
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
        <AdvFilter title={$L('聚合数据条件')} inModal canNoFilters entity={this.props.sourceEntity} filter={that._advFilter__data} confirm={(f) => that.saveAdvFilter(f)} />,
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

    const items = this.state.items || []
    let exists = items.find((x) => x.targetField === tf)
    if (exists) return RbHighbar.create($L('目标字段已添加'))

    items.push({ targetField: tf, calcMode: calc, sourceField: sf, sourceFormula: formula })
    this.setState({ items: items }, () => $(this._$sourceFormula).empty())
  }

  delItem(targetField) {
    const itemsNew = (this.state.items || []).filter((x) => x.targetField !== targetField)
    this.setState({ items: itemsNew })
  }

  addGroupField() {
    const groupFields = [...(this.state.groupFields || [])]
    const tgf = $(this._$targetGroupField).val()
    const sgf = $(this._$sourceGroupField).val()
    if (!tgf) return RbHighbar.create($L('请选择目标字段'))
    if (!sgf) return RbHighbar.create($L('请选择源字段'))

    let exists = groupFields.find((x) => x.targetField === tgf)
    if (exists) return RbHighbar.create($L('目标字段已添加'))
    exists = groupFields.find((x) => x.sourceField === sgf)
    if (exists) return RbHighbar.create($L('源字段已添加'))

    groupFields.push({ targetField: tgf, sourceField: sgf })
    this.setState({ groupFields })
  }

  delGroupField(targetField) {
    const groupFields = this.state.groupFields.filter((x) => x.targetField !== targetField)
    this.setState({ groupFields })
  }

  buildContent() {
    const content = {
      targetEntity: $(this._$targetEntity).val(),
      items: this.state.items || [],
      dataFilter: this._advFilter__data,
      autoCreate: $(this._$autoCreate).prop('checked'),
      readonlyFields: $(this._$readonlyFields).prop('checked'),
      forceUpdate: $(this._$forceUpdate).prop('checked'),
      stopPropagation: $(this._$stopPropagation).prop('checked'),
      groupFields: this.state.groupFields || [],
      fillbackField: $(this._$fillbackField).val() || null,
    }

    if (!content.targetEntity) {
      RbHighbar.create($L('请选择目标实体'))
      return false
    }
    if (content.groupFields.length === 0) {
      RbHighbar.create($L('请至少添加 1 个分组字段'))
      return false
    }
    if (content.items.length === 0) {
      RbHighbar.create($L('请至少添加 1 个聚合规则'))
      return false
    }

    return content
  }
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  renderRbcomp(<ContentGroupAggregation {...props} />, 'react-content', function () {
    // eslint-disable-next-line no-undef
    contentComp = this
    $('#react-content [data-toggle="tooltip"]').tooltip()
  })
}
