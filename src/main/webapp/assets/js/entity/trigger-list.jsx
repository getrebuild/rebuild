
$(document).ready(function () {
  $('.J_add').click(() => { renderRbcomp(<DlgEdit />) })
  renderRbcomp(<GridList />, 'list')
})

class GridList extends React.Component {
  constructor(props) {
    super(props)
    this.state = { list: [] }
  }
  render() {
    return <div className="entry-list">
      {this.state.list.map((item) => {
        return (<div key={'item-' + item[0]} className="col-xl-2 col-lg-3 col-md-4 col-sm-6">
          <div className="card">
            <div className="card-body">
              <a href={'trigger/' + item[0]}>地区地区地区</a>
              <p className="text-muted m-0 fs-12">123</p>
            </div>
            <div className="card-footer card-footer-contrast text-muted">
              <div className="float-left">
                <a className="J_del" href="javascript:;"><i className="zmdi zmdi-delete"></i></a>
              </div>
              <div className="float-right fs-12 text-warning"></div>
              <div className="clearfix"></div>
            </div>
          </div>
        </div>)
      })}
    </div>
  }
  componentDidMount() {
    $.get(`${rb.baseUrl}/admin/robot/trigger/list?entity=`, (res) => {
      this.setState({ list: res.data })
    })
  }
}


class DlgEdit extends RbFormHandler {
  constructor(props) {
    super(props)
  }
  render() {
    return (<RbModal title="添加规则触发" ref={(c) => this._dlg = c}>
      <div className="form">
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">触发类型</label>
          <div className="col-sm-7">
            <select className="form-control form-control-sm" ref={(c) => this._actionType = c}>
              {(this.state.actions || []).map((item) => {
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
    $.get(`${rb.baseUrl}/admin/robot/trigger/available-actions`, (res) => {
      let s2ot = null
      this.setState({ actions: res.data }, () => {
        s2ot = $(this._actionType).select2({
          placeholder: '选择触发类型',
          allowClear: false
        }).on('change', () => {
          this.__getEntitiesByAction(s2ot.val())
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
  __getEntitiesByAction(type) {
    $.get(`${rb.baseUrl}/admin/robot/trigger/available-entities?action=${type}`, (res) => {
      this.setState({ sourceEntities: res.data })
    })
  }

  save = (e) => {
    e.preventDefault()
    let _data = { actionType: this.__select2[0].val(), belongEntity: this.__select2[1].val() }
    if (!_data.actionType || !_data.belongEntity) {
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