
$(document).ready(function () {
  $('.J_add').click(() => { renderRbcomp(<DlgEdit />) })
  renderRbcomp(<GridList />, 'list')
})

class GridList extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }
  render() {
    return <div className="card-list row">
      {(this.state.list || []).map((item) => {
        return (<div key={'item-' + item[0]} className="col-xl-3 col-lg-4 col-md-6">
          <div className="card">
            <div className="card-body">
              <a className="text-truncate" href={'approval/' + item[0]}>{item[1]}</a>
              <p className="text-muted text-truncate">{item[3]}</p>
            </div>
            <div className="card-footer card-footer-contrast">
              <div className="float-left">
                <a onClick={() => renderRbcomp(<DlgEdit id={item[0]} name={item[1]} isDisabled={item[4]} />)}><i className="zmdi zmdi-edit"></i></a>
                <a onClick={() => this.delete(item[0])}><i className="zmdi zmdi-delete"></i></a>
              </div>
              {item[4] && <div className="badge badge-warning">已禁用</div>}
              <div className="clearfix"></div>
            </div>
          </div>
        </div>)
      })}
      {(!this.state.list || this.state.list.length === 0) && <div className="text-muted">尚未配置审批流程</div>}
    </div>
  }
  componentDidMount() {
    this.loadData()
  }

  loadData(entity) {
    $.get(`${rb.baseUrl}/admin/robot/approval/list?entity=${$encode(entity)}`, (res) => {
      this.setState({ list: res.data })
      if (!this.__treeRendered) this.renderEntityTree()
    })
  }
  renderEntityTree() {
    this.__treeRendered = true
    const dest = $('.dept-tree ul')
    const ues = []
    $(this.state.list).each(function () {
      if (!ues.contains(this[3])) $('<li data-entity="' + this[2] + '"><a class="text-truncate">' + this[3] + '</a></li>').appendTo(dest)
      ues.push(this[2])
    })
    let that = this
    dest.find('li').click(function () {
      dest.find('li').removeClass('active')
      $(this).addClass('active')
      that.loadData($(this).data('entity'))
    })
  }

  delete(configId) {
    rb.alert('确认要删除此审批流程？', {
      type: 'danger',
      confirmText: '删除',
      confirm: function () {
        this.disabled(true)
        $.post(`${rb.baseUrl}/app/entity/record-delete?id=${configId}`, (res) => {
          if (res.error_code === 0) {
            rb.hbsuccess('审批流程已删除')
            setTimeout(() => { location.reload() }, 500)
          } else rb.hberror(res.error_msg)
        })
      }
    })
  }
}

class DlgEdit extends RbFormHandler {
  constructor(props) {
    super(props)
  }
  render() {
    return (<RbModal title={(this.props.id ? '编辑' : '添加') + '审批流程'} ref={(c) => this._dlg = c}>
      <div className="form">
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">流程名称</label>
          <div className="col-sm-7">
            <input type="text" className="form-control form-control-sm" data-id="name" onChange={this.handleChange} value={this.state.name || ''} />
          </div>
        </div>
        {!this.props.id &&
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">应用实体</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" ref={(c) => this._applyEntity = c}>
                {(this.state.applyEntities || []).map((item) => {
                  return <option key={'e-' + item.name} value={item.name}>{item.label}</option>
                })}
              </select>
            </div>
          </div>}
        {this.props.id &&
          <div className="form-group row">
            <div className="col-sm-7 offset-sm-3">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" checked={this.state.isDisabled === true} data-id="isDisabled" onChange={this.handleChange} />
                <span className="custom-control-label">是否禁用 <i ref={(c) => this._tooltip = c} className="zmdi zmdi-help zicon" title="禁用后正在使用此流程的审批记录不受影响"></i></span>
              </label>
            </div>
          </div>}
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3" ref={(c) => this._btns = c}>
            <button className="btn btn-primary" type="button" onClick={this.save}>确定</button>
          </div>
        </div>
      </div>
    </RbModal>)
  }
  componentDidMount() {
    if (!this.props.id) {
      $.get(`${rb.baseUrl}/commons/metadata/entities`, (res) => {
        this.setState({ applyEntities: res.data }, () => {
          this.__select2 = $(this._applyEntity).select2({
            placeholder: '选择实体',
            allowClear: false
          })
        })
      })
    } else {
      $(this._tooltip).tooltip()
    }
  }
  save = () => {
    let _data = { name: this.state['name'] }
    if (!_data.name) { rb.highbar('请输入流程名称'); return }
    if (!this.props.id) {
      _data.belongEntity = this.__select2.val()
      if (!_data.belongEntity) { rb.highbar('请选择应用实体'); return }
    } else {
      _data.isDisabled = this.state.isDisabled === true
    }
    _data.metadata = { entity: 'RobotApprovalConfig', id: this.props.id || null }

    let _btns = $(this._btns).find('.btn').button('loading')
    $.post(rb.baseUrl + '/app/entity/record-save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        if (this.props.id) location.reload()
        else location.href = 'approval/' + res.data.id
      } else rb.hberror(res.error_msg)
      _btns.button('reset')
    })
  }
}