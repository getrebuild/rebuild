/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(function () {
  $('.J_add').click(() => renderRbcomp(<TransformEdit />))
  renderRbcomp(<TransformList />, 'dataList')
})

class TransformList extends ConfigList {
  constructor(props) {
    super(props)
    this.requestUrl = '/admin/transform/list'
  }

  render() {
    return (
      <React.Fragment>
        {(this.state.data || []).map((item) => {
          const name = item[6] || `${item[2]} · ${item[4]}`
          return (
            <tr key={item[0]}>
              <td>
                <a href={`transform/${item[0]}`}>{name}</a>
              </td>
              <td>{item[2]}</td>
              <td>{item[4]}</td>
              <td>{item[7] ? <span className="badge badge-warning font-weight-light">{$L('False')}</span> : <span className="badge badge-success font-weight-light">{$L('True')}</span>}</td>
              <td>{item[5]}</td>
              <td className="actions">
                <a className="icon" title={$L('Modify')} onClick={() => this.handleEdit(item)}>
                  <i className="zmdi zmdi-edit" />
                </a>
                <a className="icon danger-hover" title={$L('Delete')} onClick={() => this.handleDelete(item[0])}>
                  <i className="zmdi zmdi-delete" />
                </a>
              </td>
            </tr>
          )
        })}
      </React.Fragment>
    )
  }

  handleEdit(item) {
    renderRbcomp(<TransformEdit id={item[0]} name={item[6]} isDisabled={item[7]} />)
  }

  handleDelete(id) {
    const handle = super.handleDelete
    RbAlert.create($L('DeleteApprovalConfirm'), {
      html: true,
      type: 'danger',
      confirmText: $L('Delete'),
      confirm: function () {
        this.disabled(true)
        handle(id)
      },
    })
  }
}

class TransformEdit extends ConfigFormDlg {
  constructor(props) {
    super(props)
    this.subtitle = $L('e.TransformConfig')
  }

  renderFrom() {
    return (
      <React.Fragment>
        {!this.props.id && (
          <React.Fragment>
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('SelectSome,SourceEntity')}</label>
              <div className="col-sm-7">
                <select className="form-control form-control-sm" ref={(c) => (this._source = c)}>
                  {(this.state.entities || []).map((item) => {
                    return (
                      <option key={'e-' + item.name} value={item.name}>
                        {item.label}
                      </option>
                    )
                  })}
                </select>
              </div>
            </div>
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('SelectSome,TargetEntity')}</label>
              <div className="col-sm-7">
                <select className="form-control form-control-sm" ref={(c) => (this._target = c)}>
                  {(this.state.entities || []).map((item) => {
                    return (
                      <option key={'e-' + item.name} value={item.name}>
                        {item.label}
                      </option>
                    )
                  })}
                </select>
              </div>
            </div>
          </React.Fragment>
        )}
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">
            {$L('Name')} ({$L('Optional')})
          </label>
          <div className="col-sm-7">
            <input type="text" className="form-control form-control-sm" data-id="name" onChange={this.handleChange} value={this.state.name || ''} placeholder={$L('Unname')} />
          </div>
        </div>
        {this.props.id && (
          <div className="form-group row">
            <div className="col-sm-7 offset-sm-3">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" checked={this.state.isDisabled === true} data-id="isDisabled" onChange={this.handleChange} />
                <span className="custom-control-label">{$L('IsDisable')}</span>
              </label>
            </div>
          </div>
        )}
      </React.Fragment>
    )
  }

  componentDidMount() {
    $.get('/commons/metadata/entities?detail=true', (res) => {
      this.setState({ entities: res.data }, () => {
        this._$source = $(this._source).select2({
          placeholder: $L('SelectSome,Entity'),
          allowClear: false,
        })
        this._$target = $(this._target).select2({
          placeholder: $L('SelectSome,Entity'),
          allowClear: false,
        })
      })
    })
  }

  confirm = () => {
    const post = { name: this.state['name'] }
    if (!this.props.id) {
      post.belongEntity = this._$source.val()
      if (!post.belongEntity) return RbHighbar.create($L('PlsSelectSome,SourceEntity'))

      post.targetEntity = this._$target.val()
      if (!post.targetEntity) return RbHighbar.create($L('PlsSelectSome,TargetEntity'))
    } else {
      post.isDisabled = this.state.isDisabled === true
    }

    post.metadata = {
      entity: 'TransformConfig',
      id: this.props.id || null,
    }

    this.disabled(true)
    $.post('/app/entity/record-save', JSON.stringify(post), (res) => {
      if (res.error_code === 0) {
        if (this.props.id) location.reload()
        else location.href = 'transform/' + res.data.id
      } else {
        RbHighbar.error(res.error_msg)
        this.disabled()
      }
    })
  }
}
