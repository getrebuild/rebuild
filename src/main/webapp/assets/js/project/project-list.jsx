/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(function () {
  $('.J_add').click(() => renderRbcomp(<DlgEdit />))
  renderRbcomp(<GridList />, 'list')
})

class GridList extends React.Component {

  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    return (
      <div className="card-list row">
        {(this.state.list || []).map((item) => {
          return (
            <div key={`item-${item[0]}`} className="col-xl-2 col-lg-3 col-md-4 col-sm-6">
              <div className="card">
                <div className="card-body">
                  <a className="text-truncate" href={`project/${item[0]}`}>{item[1]}</a>
                  <p><span className="badge badge-light">{item[2]}</span></p>
                </div>
                <div className="card-footer card-footer-contrast">
                  <div className="float-left">
                    <a onClick={() => this._handleEdit(item)}><i className="zmdi zmdi-edit"></i></a>
                    <a onClick={() => this._handleDelete(item[0])} className="danger"><i className="zmdi zmdi-delete"></i></a>
                  </div>
                  <div className="clearfix"></div>
                </div>
              </div>
            </div>
          )
        })}
        {(!this.state.list || this.state.list.length === 0) && <div className="text-muted">尚未配置项目</div>}
      </div>
    )
  }

  componentDidMount() {
    $.get('/admin/projects/list', (res) => this.setState({ list: res.data }))
  }

  _handleEdit(item) {
    renderRbcomp(<DlgEdit id={item[0]} projectName={item[1]} projectCode={item[2]} />)
  }

  _handleDelete(projectId) {
    RbAlert.create('只有空项目才能被删除。确认删除吗？', {
      type: 'danger',
      confirmText: '删除',
      confirm: function () {
        this.disabled(true)
        $.post(`/app/entity/record-delete?id=${projectId}`, (res) => {
          if (res.error_code === 0) {
            RbHighbar.success('项目已删除')
            setTimeout(() => location.reload(), 500)
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
    return (
      <RbModal title={`${this.props.id ? '修改' : '添加'}项目`} ref={(c) => this._dlg = c}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">项目名称</label>
            <div className="col-sm-7">
              <input className="form-control form-control-sm" value={this.state.projectName || ''} data-id="projectName" onChange={this.handleChange} maxLength="60" />
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">项目 ID</label>
            <div className="col-sm-7">
              <input className="form-control form-control-sm " value={this.state.projectCode || ''} data-id="projectCode" onChange={this.handleChange} maxLength="6" placeholder="(可选)" />
              <div className="form-text">任务编号将以项目 ID 作为前缀，用以区别不同项目。支持 2-6 位字母</div>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3">
              <button className="btn btn-primary" type="button" onClick={this.save}>确定</button>
              <a className="btn btn-link" onClick={this.hide}>取消</a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  save = (e) => {
    e.preventDefault()
    if (!this.state.projectName) return RbHighbar.create('请输入项目名称')
    if (this.state.projectCode && !/^[A-Z]{2,6}$/i.test(this.state.projectCode)) return RbHighbar.create('项目 ID 无效，支持 2-6 位字母')

    const _data = {
      projectName: this.state.projectName,
      projectCode: (this.state.projectCode || '').toUpperCase(),
    }
    _data.metadata = { entity: 'ProjectConfig', id: this.props.id || null }

    $.post('/app/entity/record-save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        if (this.props.id) location.reload()
        else location.href = 'project/' + res.data.id
      } else RbHighbar.error(res.error_msg)
    })
  }
}