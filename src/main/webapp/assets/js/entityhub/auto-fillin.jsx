const wpc = window.__PageConfig
const bProps = { sourceEntity: wpc.referenceEntity, targetEntity: wpc.entityName, field: wpc.fieldName }
$(document).ready(() => {
  $('.J_add-rule').click(() => { renderRbcomp(<DlgRuleEdit {...bProps} />) })
  loadRules()
})

const loadRules = () => {
  $.get('../auto-fillin-list?field=' + wpc.fieldName, (res) => {
    let tbody = $('#dataList tbody').empty()
    $(res.data).each(function () {
      let tr = $('<tr></tr>').appendTo(tbody)
      $('<td><div>' + this.targetFieldLabel + '</div></td>').appendTo(tr)
      $('<td>' + this.sourceFieldLabel + '</div></td>').appendTo(tr)
      let extc = this.extConfig
      let extcLabel = []
      if (extc.whenCreate) extcLabel.push('新建时')
      if (extc.whenUpdate) extcLabel.push('更新时')
      if (extc.fillinForce) extcLabel.push('强制回填')
      $('<td>' + extcLabel.join(', ') + '</div></td>').appendTo(tr)
      let act = $('<td class="actions"><a class="icon"><i class="zmdi zmdi-settings"></i></a><a class="icon"><i class="zmdi zmdi-delete"></i></a></td>').appendTo(tr)
      act.find('a:eq(0)').click(() => {
        renderRbcomp(<DlgRuleEdit {...bProps} {...extc} id={this.id} sourceField={this.sourceField} targetField={this.targetField} />)
      })
      let configId = this.id
      act.find('a:eq(1)').click(() => {
        RbAlert.create('确认删除此配置项？', {
          type: 'danger',
          confirm: function () {
            this.disabled(true)
            $.post(`${rb.baseUrl}/app/entity/record-delete?id=${configId}`, (res) => {
              if (res.error_code === 0) {
                this.hide()
                loadRules()
              } else RbHighbar.error(res.error_msg)
            })
          }
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
    return (<RbModal title="回填规则" ref={(c) => this._dlg = c} disposeOnHide={true}>
      <div className="form">
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">源字段</label>
          <div className="col-sm-7">
            <select className="form-control form-control-sm" ref={(c) => this._sourceField = c}>
              {(this.state.sourceFields || []).map((item) => {
                return <option key={'sf-' + item.name} value={item.name}>{item.label}</option>
              })}
            </select>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">目标字段</label>
          <div className="col-sm-7">
            <select className="form-control form-control-sm" ref={(c) => this._targetField = c}>
              {(this.state.targetFields || []).map((item) => {
                return <option key={'tf-' + item.name} value={item.name}>{item.label}</option>
              })}
            </select>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right pt-1">何时回填</label>
          <div className="col-sm-7">
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
              <input className="custom-control-input" type="checkbox" checked={this.state.whenCreate === true} data-id="whenCreate" onChange={this.handleChange} />
              <span className="custom-control-label">新建时</span>
            </label>
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
              <input className="custom-control-input" type="checkbox" checked={this.state.whenUpdate === true} data-id="whenUpdate" onChange={this.handleChange} />
              <span className="custom-control-label">更新时</span>
            </label>
          </div>
        </div>
        <div className="form-group row pt-1">
          <label className="col-sm-3 col-form-label text-sm-right pt-1">当目标字段非空时</label>
          <div className="col-sm-7">
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
              <input className="custom-control-input" type="checkbox" checked={this.state.fillinForce === true} data-id="fillinForce" onChange={this.handleChange} />
              <span className="custom-control-label">强制回填</span>
            </label>
          </div>
        </div>
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3" ref={(c) => this._btns = c}>
            <button className="btn btn-primary" type="button" onClick={this.save}>确定</button>
          </div>
        </div>
      </div>
    </RbModal>)
  }
  componentDidMount() {
    this.__select2 = []
    // #1
    $.get(`${rb.baseUrl}/commons/metadata/fields?entity=${this.props.targetEntity}`, (res) => {
      this.__targetFieldsCache = res.data
      let s2target = $(this._targetField).select2({
        placeholder: '选择字段',
        allowClear: false
      })
      this.__select2.push(s2target)

      // #2
      $.get(`${rb.baseUrl}/commons/metadata/fields?entity=${this.props.sourceEntity}`, (res) => {
        for (let i = 0; i < res.data.length; i++) {
          let item = res.data[i]
          if (item.ref) item.type = 'REFERENCE'
        }
        this.__sourceFieldsCache = res.data

        this.setState({ sourceFields: res.data }, () => {
          let s2source = $(this._sourceField).select2({
            placeholder: '选择字段',
            allowClear: false
          }).on('change', (e) => {
            this.__renderTargetFields(e.target.value)
          })
          this.__select2.push(s2source)

          if (this.props.sourceField) {
            s2source.val(this.props.sourceField).trigger('change')
            setTimeout(() => { s2target.val(this.props.targetField).trigger('change') }, 100)
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
    let source = null
    $(this.__sourceFieldsCache).each(function () {
      if (s === this.name) {
        source = this
        return false
      }
    })

    let canFillinByType = CAN_FILLIN_MAPPINGS[source.type] || []
    canFillinByType.push('TEXT')
    canFillinByType.push('NTEXT')

    // 显示兼容的目标字段
    let tFields = []
    $(this.__targetFieldsCache).each(function () {
      if (!this.creatable || this.name === wpc.fieldName) return
      if (source.type === 'FILE' && this.type !== 'FILE') return
      if (source.type === 'IMAGE' && this.type !== 'IMAGE') return
      if (source.type === this.type || canFillinByType.contains(this.type)) {
        if (this.ref) {  // reference field
          if (source.type === 'REFERENCE' && source.ref[0] === this.ref[0]) {
            tFields.push(this)
          }
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
    let _data = { field: this.props.field, sourceField: $(this._sourceField).val(), targetField: $(this._targetField).val() }
    if (!_data.targetField) { RbHighbar.create('请选择目标字段'); return }
    _data.extConfig = { whenCreate: this.state.whenCreate, whenUpdate: this.state.whenUpdate, fillinForce: this.state.fillinForce }
    if (this.props.id) _data.id = this.props.id

    let _btns = $(this._btns).find('.btn').button('loading')
    $.post('../auto-fillin-save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        this.hide()
        loadRules()
      } else RbHighbar.create(res.error_msg)
      _btns.button('reset')
    })
  }
}

const CAN_FILLIN_MAPPINGS = {
  'NUMBER': ['DECIMAL'],
  'DECIMAL': ['NUMBER'],
  'DATE': ['DATETIME'],
  'DATETIME': ['DATE'],
}