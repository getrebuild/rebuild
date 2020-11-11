/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig
const bProps = { sourceEntity: wpc.referenceEntity, targetEntity: wpc.entityName, field: wpc.fieldName }

$(document).ready(() => {
  $('.J_add-rule').click(() => {
    renderRbcomp(<DlgRuleEdit {...bProps} />)
  })
  loadRules()
})

const loadRules = () => {
  $.get('../auto-fillin-list?field=' + wpc.fieldName, (res) => {
    const tbody = $('#dataList tbody').empty()
    $(res.data).each(function () {
      let tr = $('<tr></tr>').appendTo(tbody)
      $('<td><div>' + this.targetFieldLabel + '</div></td>').appendTo(tr)
      $('<td>' + this.sourceFieldLabel + '</div></td>').appendTo(tr)
      const extc = this.extConfig
      let extcLabel = []
      if (extc.whenCreate) extcLabel.push($L('WhenCreate'))
      if (extc.whenUpdate) extcLabel.push($L('WhenUpdate'))
      if (extc.fillinForce) extcLabel.push($L('ForceFillback'))
      $('<td>' + extcLabel.join(', ') + '</div></td>').appendTo(tr)
      const $act = $('<td class="actions"><a class="icon"><i class="zmdi zmdi-settings"></i></a><a class="icon danger-hover"><i class="zmdi zmdi-delete"></i></a></td>').appendTo(tr)
      $act.find('a:eq(0)').click(() => {
        renderRbcomp(<DlgRuleEdit {...bProps} {...extc} id={this.id} sourceField={this.sourceField} targetField={this.targetField} />)
      })
      const configId = this.id
      $act.find('a:eq(1)').click(() => {
        RbAlert.create($L('DeleteSomeConfirm,FillbackRule'), {
          type: 'danger',
          confirm: function () {
            this.disabled(true)
            $.post(`/app/entity/common-delete?id=${configId}`, (res) => {
              if (res.error_code === 0) {
                this.hide()
                loadRules()
              } else RbHighbar.error(res.error_msg)
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
      <RbModal title="回填规则" ref={(c) => (this._dlg = c)} disposeOnHide={true}>
        <div className="form">
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
                <span className="custom-control-label">{$L('WhenUpdate')}</span>
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
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary" type="button" onClick={this.save}>
                {$L('Confirm')}
              </button>
              <a className="btn btn-link" onClick={this.hide}>
                {$L('Cancel')}
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
        placeholder: $L('SelectSome,Field'),
        allowClear: false,
      })
      this.__select2.push(s2target)

      // #2
      $.get(`/commons/metadata/fields?entity=${this.props.sourceEntity}`, (res) => {
        this.__sourceFieldsCache = res.data
        this.setState({ sourceFields: res.data }, () => {
          const s2source = $(this._sourceField)
            .select2({
              placeholder: $L('SelectSome,Field'),
              allowClear: false,
            })
            .on('change', (e) => this.__renderTargetFields(e.target.value))
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
  }
  __renderTargetFields(s) {
    const source = this.__sourceFieldsCache.find((x) => {
      return s === x.name
    })
    let canFillinByType = CAN_FILLIN_MAPPINGS[source.type] || []
    canFillinByType.push('TEXT')
    canFillinByType.push('NTEXT')

    // 显示兼容的目标字段
    let tFields = []
    $(this.__targetFieldsCache).each(function () {
      if (
        !this.creatable ||
        this.name === wpc.fieldName ||
        this.type === 'SERIES' ||
        this.type === 'MULTISELECT' ||
        this.type === 'PICKLIST' ||
        (source.type === 'FILE' && this.type !== 'FILE') ||
        (source.type === 'IMAGE' && this.type !== 'IMAGE') ||
        (source.type === 'AVATAR' && this.type !== 'AVATAR')
      )
        return

      if (source.type === this.type || canFillinByType.includes(this.type)) {
        if (source.type === 'REFERENCE') {
          if (source.ref && this.ref && source.ref[0] === this.ref[0]) tFields.push(this)
        } else if (source.type === 'STATE') {
          if (source.stateClass && source.stateClass === this.stateClass) tFields.push(this)
        } else {
          tFields.push(this)
        }
      }
    })
    this.setState({ targetFields: tFields })
  }

  handleChange(e) {
    super.handleChange(e, () => {
      if (this.state.whenCreate === false && this.state.whenUpdate === false) {
        this.setState({ whenCreate: true })
      }
    })
  }

  save = () => {
    const _data = {
      field: this.props.field,
      sourceField: $(this._sourceField).val(),
      targetField: $(this._targetField).val(),
    }
    if (!_data.targetField) return RbHighbar.create($L('PlsSelectSome,TargetField'))

    _data.extConfig = {
      whenCreate: this.state.whenCreate,
      whenUpdate: this.state.whenUpdate,
      fillinForce: this.state.fillinForce,
    }
    if (this.props.id) _data.id = this.props.id

    this.disabled(true)
    $.post('../auto-fillin-save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        this.hide()
        loadRules()
      } else RbHighbar.create(res.error_msg)
      this.disabled()
    })
  }
}

const CAN_FILLIN_MAPPINGS = {
  NUMBER: ['DECIMAL'],
  DECIMAL: ['NUMBER'],
  DATE: ['DATETIME'],
  DATETIME: ['DATE'],
}
