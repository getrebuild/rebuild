/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(function () {
  $('.J_add').click(() => renderRbcomp(<DlgEdit />))
  renderRbcomp(<GridList />, 'list')
})

class GridList extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="card-list row">
        {(this.state.list || []).map((item) => {
          return (
            <div key={`item-${item[0]}`} className="col-xl-2 col-lg-3 col-md-4 col-sm-6">
              <div className="card">
                <div className="card-body">
                  <a className="text-truncate" href={`project/${item[0]}`}>
                    {item[1]}
                  </a>
                  <p>
                    <span className="badge badge-light">{item[2]}</span>
                  </p>
                  {item[2] && <i className={`icon zmdi zmdi-${item[3]}`}></i>}
                </div>
                <div className="card-footer card-footer-contrast">
                  <div className="float-left">
                    <a onClick={() => this._handleEdit(item)}>
                      <i className="zmdi zmdi-edit"></i>
                    </a>
                    <a onClick={() => this._handleDelete(item[0])} className="danger-hover">
                      <i className="zmdi zmdi-delete"></i>
                    </a>
                  </div>
                  <div className="clearfix"></div>
                </div>
              </div>
            </div>
          )
        })}
        {this.state.list && this.state.list.length === 0 && <div className="text-muted">{$L('NoSome,Project')}</div>}
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
    RbAlert.create($L('DeleteProjectConfirm'), {
      type: 'danger',
      confirmText: $L('Delete'),
      confirm: function () {
        this.disabled(true)
        $.post(`/app/entity/record-delete?id=${projectId}`, (res) => {
          this.hide()
          if (res.error_code === 0) {
            RbHighbar.success($L('SomeDeleted,Project'))
            setTimeout(() => location.reload(), 500)
          } else RbHighbar.error(res.error_msg)
        })
      },
    })
  }
}

class DlgEdit extends RbFormHandler {
  state = { ...this.props }

  render() {
    return (
      <RbModal title={`${$L(this.props.id ? 'Modify' : 'Add')}${$L('Project')}`} ref={(c) => (this._dlg = c)} disposeOnHide={true}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('Icon')}</label>
            <div className="col-sm-7">
              <a className="project-icon" title="选择图标" onClick={() => this._selectIcon()}>
                <i className={`icon zmdi zmdi-${this.state.iconName || 'texture'}`}></i>
              </a>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('ProjectName')}</label>
            <div className="col-sm-7">
              <input className="form-control form-control-sm" value={this.state.projectName || ''} data-id="projectName" onChange={this.handleChange} maxLength="60" />
            </div>
          </div>
          {!this.props.id && (
            <React.Fragment>
              <div className="form-group row">
                <label className="col-sm-3 col-form-label text-sm-right">{$L('ProjectCode')}</label>
                <div className="col-sm-7">
                  <input className="form-control form-control-sm " value={this.state.projectCode || ''} data-id="projectCode" onChange={this.handleChange} maxLength="6" />
                  <div className="form-text">{$L('ProjectCodeTips')}</div>
                </div>
              </div>
              <div className="form-group row">
                <label className="col-sm-3 col-form-label text-sm-right"></label>
                <div className="col-sm-7">
                  <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                    <input className="custom-control-input" type="checkbox" value="1" defaultChecked ref={(c) => (this._useTemplate = c)} />
                    <span className="custom-control-label">{$L('UseProjectTemplate')}</span>
                  </label>
                </div>
              </div>
            </React.Fragment>
          )}
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

  _selectIcon() {
    const that = this
    window.clickIcon = function (s) {
      that.setState({ iconName: s })
      RbModal.hide()
    }
    RbModal.create('/p/commons/search-icon', $L('SelectSome,Icon'))
  }

  save = () => {
    if (!this.state.projectName) return RbHighbar.create($L('PlsInputSome,ProjectName'))
    const _data = {
      projectName: this.state.projectName,
      iconName: this.state.iconName,
    }

    if (!this.props.id) {
      if (!this.state.projectCode || !/^[a-zA-Z]{2,6}$/.test(this.state.projectCode)) {
        return RbHighbar.create($L('ProjectCodeInvalid'))
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
