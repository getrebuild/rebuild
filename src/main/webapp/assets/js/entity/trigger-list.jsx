
$(document).ready(function () {
  $('.J_add').click(() => {
    renderRbcomp(<DlgEdit />)
  })
})

class DlgEdit extends RbFormHandler {
  constructor(props) {
    super(props)
  }
  render() {
    return (<RbModal title="添加触发器" ref={(c) => this._dlg = c}>
      <div className="form">
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">触发类型</label>
          <div className="col-sm-7">
            <select className="form-control form-control-sm" ref={(c) => this._operatorType = c}>
              {(this.state.operators || []).map((item) => {
                return <option key={'o-' + item[0]} value={item[0]}>{item[1]}</option>
              })}
            </select>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">触发源实体</label>
          <div className="col-sm-7">
            <select className="form-control form-control-sm" ref={(c) => this._sourceEntity = c}>
              {(this.state.sourceEntities || []).map((item) => {
                return <option key={'e-' + item[0]} value={item[0]}>{item[1]}</option>
              })}
            </select>
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
    $.get(`${rb.baseUrl}/admin/robot/trigger/available-operators`, (res) => {
      let s2ot = null
      this.setState({ operators: res.data }, () => {
        s2ot = $(this._operatorType).select2({
          placeholder: '选择触发类型',
          allowClear: false
        }).on('change', () => {
          this.__getEntitiesByOperator(s2ot.val())
        })
        this.__select2.push(s2ot)

        // #2
        let s2se = $(this._sourceEntity).select2({
          placeholder: '选择源实体',
          allowClear: false
        })
        this.__select2.push(s2se)

        s2ot.trigger('change')
      })
    })
  }
  __getEntitiesByOperator(type) {
    $.get(`${rb.baseUrl}/admin/robot/trigger/available-entities?operator=${type}`, (res) => {
      this.setState({ sourceEntities: res.data })
    })
  }

  save = (e) => {
    e.preventDefault()
    let _data = { operatorType: this.__select2[0].val(), belongEntity: this.__select2[1].val() }
    if (!_data.operatorType || !_data.belongEntity) {
      rb.hignbar('请选择源触发实体')
      return
    }
    _data.metadata = { entity: 'RobotTriggerConfig', id: this.props.id || null }

    let _btns = $(this._btns).find('.btn').button('loading')
    $.post(rb.baseUrl + '/admin/robot/trigger/save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        if (this.props.id) location.reload()
        else location.href = 'trigger/' + res.data.id
      } else rb.hberror(res.error_msg)
      _btns.button('reset')
    })
  }
}