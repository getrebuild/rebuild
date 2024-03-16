/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(function () {
  $('.J_add').on('click', () => renderRbcomp(<DlgEdit />))
  renderRbcomp(<GridList />, 'list')
})

class GridList extends React.Component {
  state = { ...this.props }

  render() {
    const ps = this.state.list || []

    return (
      <div className="card-list row">
        {ps.map((item) => {
          return (
            <div key={`item-${item[0]}`} className="col-xl-2 col-lg-3 col-md-4 col-sm-6">
              <div className="card">
                <div className="card-body">
                  <a className="text-truncate" href={`project/${item[0]}`}>
                    {item[1]}
                  </a>
                  <p>
                    <span className="badge badge-light">{item[2]}</span>
                    {item[4] === 2 && <span className="badge badge-danger font-weight-normal">{$L('已归档')}</span>}
                  </p>
                  {item[2] && <i className={`icon zmdi zmdi-${item[3]}`} />}
                </div>
                <div className="card-footer card-footer-contrast">
                  <div className="float-left">
                    <a onClick={() => this._handleEdit(item)}>
                      <i className="zmdi zmdi-edit" />
                    </a>
                    <a onClick={() => this._handleDelete(item[0])} className="danger-hover">
                      <i className="zmdi zmdi-delete" />
                    </a>
                  </div>
                  <div className="clearfix" />
                </div>
              </div>
            </div>
          )
        })}

        {ps.length === 0 && <div className="text-muted">{$L('暂无项目')}</div>}
      </div>
    )
  }

  componentDidMount() {
    $.get('/admin/projects/list', (res) => this.setState({ list: res.data }))
  }

  _handleEdit(item) {
    renderRbcomp(<DlgEdit id={item[0]} projectName={item[1]} iconName={item[3]} />)
  }

  _handleDelete(projectId) {
    RbAlert.create($L('只有空项目 (项目下无任务) 才能被删除。确认吗？'), {
      type: 'danger',
      confirmText: $L('删除'),
      confirm: function () {
        this.disabled(true)
        $.post(`/app/entity/common-delete?id=${projectId}`, (res) => {
          if (res.error_code === 0) {
            this.hide()
            RbHighbar.success($L('项目已删除'))
            setTimeout(() => location.reload(), 500)
          } else {
            RbHighbar.error(res.error_msg)
            this.disabled()
          }
        })
      },
    })
  }
}

class DlgEdit extends RbFormHandler {
  state = { ...this.props }

  render() {
    return (
      <RbModal title={this.props.id ? $L('修改项目') : $L('添加项目')} ref={(c) => (this._dlg = c)} disposeOnHide={true}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('图标')}</label>
            <div className="col-sm-7">
              <a className="project-icon" title="选择图标" onClick={() => this._selectIcon()}>
                <i className={`icon zmdi zmdi-${this.state.iconName || 'texture'}`} />
              </a>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('项目名称')}</label>
            <div className="col-sm-7">
              <input className="form-control form-control-sm" value={this.state.projectName || ''} data-id="projectName" onChange={this.handleChange} maxLength="60" />
            </div>
          </div>
          {!this.props.id && (
            <RF>
              <div className="form-group row">
                <label className="col-sm-3 col-form-label text-sm-right">{$L('项目 ID')}</label>
                <div className="col-sm-7">
                  <input className="form-control form-control-sm " value={this.state.projectCode || ''} data-id="projectCode" onChange={this.handleChange} maxLength="6" />
                  <div className="form-text">{$L('任务编号将以项目 ID 作为前缀，用以区别不同项目。支持 2-6 位字母')}</div>
                </div>
              </div>
              <div className="form-group row">
                <label className="col-sm-3 col-form-label text-sm-right" />
                <div className="col-sm-7">
                  <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                    <input className="custom-control-input" type="checkbox" value="1" defaultChecked ref={(c) => (this._useTemplate = c)} />
                    <span className="custom-control-label">{$L('使用默认项目模板')}</span>
                  </label>
                </div>
              </div>
            </RF>
          )}
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

  _selectIcon() {
    const that = this
    window.clickIcon = function (s) {
      that.setState({ iconName: s })
      RbModal.hide()
    }
    RbModal.create('/p/common/search-icon', $L('选择图标'), { zIndex: 1051 })
  }

  save = () => {
    if (!this.state.projectName) return RbHighbar.create($L('请输入项目名称'))
    const _data = {
      projectName: this.state.projectName,
      iconName: this.state.iconName,
    }

    if (!this.props.id) {
      if (!this.state.projectCode || !/^[a-zA-Z]{2,6}$/.test(this.state.projectCode)) {
        return RbHighbar.create($L('项目 ID 无效，请输入 2-6 位字母'))
      }
      _data.projectCode = this.state.projectCode.toUpperCase()
      _data._useTemplate = this._useTemplate.checked ? 1 : 0
    }
    _data.metadata = {
      entity: 'ProjectConfig',
      id: this.props.id || null,
    }

    this.disabled(true)
    $.post('/admin/projects/post', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        if (this.props.id) location.reload()
        else location.href = 'project/' + res.data.id
      } else {
        RbHighbar.create(res.error_msg)
        this.disabled()
      }
    })
  }
}
