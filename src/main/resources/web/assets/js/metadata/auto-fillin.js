/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig
const bProps = { sourceEntity: wpc.referenceEntity, targetEntity: wpc.entityName, field: wpc.fieldName }

$(document).ready(() => {
  $('.J_add-rule').click(() => renderRbcomp(<DlgRuleEdit {...bProps} />))
  loadRules()
})

const loadRules = () => {
  $.get(`../auto-fillin-list?field=${wpc.fieldName}`, (res) => {
    const $tbody = $('#dataList tbody').empty()
    $(res.data).each(function () {
      const $tr = $('<tr></tr>').appendTo($tbody)
      $(`<td><div>${this.targetFieldLabel}</div></td>`).appendTo($tr)
      $(`<td>${this.sourceFieldLabel}</div></td>`).appendTo($tr)

      const ruleLabels = []
      if (this.extConfig.whenCreate) ruleLabels.push($L('WhenCreate'))
      if (this.extConfig.whenUpdate) ruleLabels.push($L('WhenEdit'))
      if (this.extConfig.fillinForce) ruleLabels.push($L('ForceFillback'))
      if (this.extConfig.readonlyTargetField) ruleLabels.push($L('TargetFieldReadonly'))
      $(`<td>${ruleLabels.join(', ')}</div></td>`).appendTo($tr)

      const $btns = $('<td class="actions"><a class="icon"><i class="zmdi zmdi-settings"></i></a><a class="icon danger-hover"><i class="zmdi zmdi-delete"></i></a></td>').appendTo($tr)
      $btns.find('a:eq(0)').click(() => {
        renderRbcomp(<DlgRuleEdit {...bProps} {...this.extConfig} id={this.id} sourceField={this.sourceField} targetField={this.targetField} />)
      })

      const cfgid = this.id
      $btns.find('a:eq(1)').click(() => {
        RbAlert.create($L('DeleteSomeConfirm,FillbackRule'), {
          type: 'danger',
          confirm: function () {
            this.disabled(true)
            $.post(`/app/entity/common-delete?id=${cfgid}`, (res) => {
              if (res.error_code === 0) {
                RbHighbar.success($L('删除成功'))
                this.hide()
                loadRules()
              } else {
                RbHighbar.error(res.error_msg)
              }
            })
          },
        })
      })
    })

    $('#dataList').parent().removeClass('rb-loading-active')
    if (res.data.length === 0) $('.list-nodata').removeClass('hide')
    else $('.list-nodata').addClass('hide')
  })
}

class DlgRuleEdit extends RbFormHandler {
  constructor(props) {
    super(props)
    if (!props.id) this.state = { ...this.state, whenCreate: true }
  }

  render() {
    return (
      <RbModal title={$L('FillbackRule')} ref={(c) => (this._dlg = c)} disposeOnHide={true}>
        <div className="form" ref={(c) => (this._form = c)}>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('SourceField')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" ref={(c) => (this._sourceField = c)}>
                {(this.state.sourceFields || []).map((item) => {
                  return (
                    <option key={'sf-' + item.name} value={item.name}>
                      {item.label}
                    </option>
                  )
                })}
              </select>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('TargetField')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" ref={(c) => (this._targetField = c)}>
                {(this.state.targetFields || []).map((item) => {
                  return (
                    <option key={'tf-' + item.name} value={item.name}>
                      {item.label}
                    </option>
                  )
                })}
              </select>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right pt-1">{$L('HowFillback')}</label>
            <div className="col-sm-7">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" checked={this.state.whenCreate === true} data-id="whenCreate" onChange={this.handleChange} />
                <span className="custom-control-label">{$L('WhenCreate')}</span>
              </label>
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" checked={this.state.whenUpdate === true} data-id="whenUpdate" onChange={this.handleChange} />
                <span className="custom-control-label">{$L('WhenEdit')}</span>
              </label>
            </div>
          </div>
          <div className="form-group row pt-1">
            <label className="col-sm-3 col-form-label text-sm-right pt-1">{$L('WhenTargetFieldNotEmpty')}</label>
            <div className="col-sm-7">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" checked={this.state.fillinForce === true} data-id="fillinForce" onChange={this.handleChange} />
                <span className="custom-control-label">{$L('ForceFillback')}</span>
              </label>
            </div>
          </div>
          <div className="form-group row pt-1">
            <label className="col-sm-3 col-form-label text-sm-right pt-1"></label>
            <div className="col-sm-7">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" checked={this.state.readonlyTargetField === true} data-id="readonlyTargetField" onChange={this.handleChange} />
                <span className="custom-control-label">
                  {$L('SetTargetFieldReadonly')}
                  <i className="zmdi zmdi-help zicon down-1" data-toggle="tooltip" title={$L('OnlyFormEffectiveTip')} />
                </span>
              </label>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary" type="button" onClick={this.save}>
                {$L('确定')}
              </button>
              <a className="btn btn-link" onClick={this.hide}>
                {$L('取消')}
              </a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    this.__select2 = []
    // #1
    $.get(`/commons/metadata/fields?entity=${this.props.targetEntity}`, (res) => {
      this.__targetFieldsCache = res.data
      const s2target = $(this._targetField).select2({
        placeholder: $L('选择,Field'),
        allowClear: false,
      })
      this.__select2.push(s2target)

      // #2
      $.get(`/commons/metadata/fields?entity=${this.props.sourceEntity}`, (res) => {
        this.__sourceFieldsCache = res.data
        this.setState({ sourceFields: res.data }, () => {
          const s2source = $(this._sourceField)
            .select2({
              placeholder: $L('选择,Field'),
              allowClear: false,
            })
            .on('change', (e) => this._renderTargetFields(e.target.value))
          this.__select2.push(s2source)

          if (this.props.sourceField) {
            s2source.val(this.props.sourceField).trigger('change')
            setTimeout(() => s2target.val(this.props.targetField).trigger('change'), 100)
          } else {
            s2source.trigger('change')
          }

          if (this.props.id && rb.env !== 'dev') {
            s2target.prop('disabled', true)
            s2source.prop('disabled', true)
          }
        })
      })
    })

    $(this._form).find('[data-toggle="tooltip"]').tooltip()
  }

  _renderTargetFields(s) {
    const source = this.__sourceFieldsCache.find((x) => s === x.name)

    // 显示兼容的目标字段
    const targetFields = []
    $(this.__targetFieldsCache).each(function () {
      if (this.creatable && this.name !== wpc.fieldName && this.type !== 'SERIES' && $fieldIsCompatible(source, this)) {
        targetFields.push(this)
      }
    })
    this.setState({ targetFields: targetFields }, () => {
      if (targetFields.length > 0) this.__select2[0].val(targetFields[0].name)
    })
  }

  save = () => {
    const _data = {
      field: this.props.field,
      sourceField: $(this._sourceField).val(),
      targetField: $(this._targetField).val(),
    }
    if (!_data.targetField) return RbHighbar.create($L('请选择,TargetField'))

    _data.extConfig = {
      whenCreate: this.state.whenCreate,
      whenUpdate: this.state.whenUpdate,
      fillinForce: this.state.fillinForce,
      readonlyTargetField: this.state.readonlyTargetField,
    }
    if (this.props.id) _data.id = this.props.id

    this.disabled(true)
    $.post('../auto-fillin-save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        this.hide()
        loadRules()
      } else {
        RbHighbar.create(res.error_msg)
      }
      this.disabled()
    })
  }
}
