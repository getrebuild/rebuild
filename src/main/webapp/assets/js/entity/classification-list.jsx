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
        return (<div key={'item-' + item[0]} className="col-xl-2 col-lg-3 col-md-4 col-sm-6">
          <div className="card">
            <div className="card-body">
              <a className="text-truncate" href={'classification/' + item[0]}>{item[1]}</a>
              <p className="text-muted text-truncate">{item[3]}级分类</p>
            </div>
            <div className="card-footer card-footer-contrast">
              <div className="float-left">
                <a onClick={() => this.editItem(item)}><i className="zmdi zmdi-edit"></i></a>
                <a onClick={() => this.deleteItem(item[0])}><i className="zmdi zmdi-delete"></i></a>
              </div>
              {item[2] && <div className="badge badge-warning">已禁用</div>}
              <div className="clearfix"></div>
            </div>
          </div>
        </div>)
      })}
      {(!this.state.list || this.state.list.length === 0) && <div className="text-muted">尚未配置分类数据</div>}
    </div>
  }
  componentDidMount() {
    $.get(`${rb.baseUrl}/admin/entityhub/classification/list`, (res) => {
      this.setState({ list: res.data })
    })
  }
  editItem(item) {
    renderRbcomp(<DlgEdit id={item[0]} name={item[1]} isDisabled={item[2]} />)
  }
  deleteItem(dataId) {
    RbAlert.create('删除前请确认此分类数据未被使用。确认删除吗？', {
      type: 'danger',
      confirmText: '删除',
      confirm: function () {
        this.disabled(true)
        $.post(`${rb.baseUrl}/app/entity/record-delete?id=${dataId}`, (res) => {
          if (res.error_code === 0) {
            RbHighbar.success('分类数据已删除')
            setTimeout(() => { location.reload() }, 500)
          } else RbHighbar.error(res.error_msg)
        })
      }
    })
  }
}

class DlgEdit extends RbFormHandler {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }
  render() {
    return (<RbModal title={(this.props.id ? '编辑' : '添加') + '分类'} ref={(c) => this._dlg = c}>
      <div className="form">
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">分类名称</label>
          <div className="col-sm-7">
            <input className="form-control form-control-sm" value={this.state.name || ''} data-id="name" onChange={this.handleChange} maxLength="40" />
          </div>
        </div>
        {this.props.id &&
          <div className="form-group row">
            <div className="col-sm-7 offset-sm-3">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" checked={this.state.isDisabled === true} data-id="isDisabled" onChange={this.handleChange} />
                <span className="custom-control-label">是否禁用 (禁用不影响已有数据)</span>
              </label>
            </div>
          </div>
        }
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3">
            <button className="btn btn-primary" type="button" onClick={this.save}>确定</button>
          </div>
        </div>
      </div>
    </RbModal>)
  }
  save = (e) => {
    e.preventDefault()
    if (!this.state.name) { RbHighbar.create('请输入名称'); return }
    let _data = { name: this.state.name, isDisabled: this.state.isDisabled === true }
    _data.metadata = { entity: 'Classification', id: this.props.id || null }
    $.post(rb.baseUrl + '/app/entity/record-save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        if (this.props.id) location.reload()
        else location.href = 'classification/' + res.data.id
      } else RbHighbar.error(res.error_msg)
    })
  }
}