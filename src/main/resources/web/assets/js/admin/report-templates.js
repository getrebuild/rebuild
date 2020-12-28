/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(function () {
  $('.J_add').click(() => renderRbcomp(<ReporEdit />))
  renderRbcomp(<ReportList />, 'dataList')
})

class ReportList extends ConfigList {
  constructor(props) {
    super(props)
    this.requestUrl = '/admin/data/report-templates/list'
  }

  render() {
    return (
      <React.Fragment>
        {(this.state.data || []).map((item) => {
          return (
            <tr key={'k-' + item[0]}>
              <td>{item[3]}</td>
              <td>{item[2] || item[1]}</td>
              <td>{item[4] ? <span className="badge badge-warning font-weight-normal">{$L('False')}</span> : <span className="badge badge-success font-weight-light">{$L('True')}</span>}</td>
              <td>
                <DateShow date={item[5]} />
              </td>
              <td className="actions">
                <a className="icon" title={$L('Preview')} href={`${rb.baseUrl}/admin/data/report-templates/preview?id=${item[0]}`} target="_blank">
                  <i className="zmdi zmdi-open-in-new" />
                </a>
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
    renderRbcomp(<ReporEdit id={item[0]} name={item[3]} isDisabled={item[4]} />)
  }

  handleDelete(id) {
    const handle = super.handleDelete
    RbAlert.create($L('DeleteSomeConfirm,ReportTemplate'), {
      type: 'danger',
      confirmText: $L('Delete'),
      confirm: function () {
        this.disabled(true)
        handle(id)
      },
    })
  }
}

class ReporEdit extends ConfigFormDlg {
  constructor(props) {
    super(props)
    this.subtitle = $L('ReportTemplate')
  }

  renderFrom() {
    return (
      <React.Fragment>
        {!this.props.id && (
          <React.Fragment>
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('SelectSome,ApplyEntity')}</label>
              <div className="col-sm-7">
                <select className="form-control form-control-sm" ref={(c) => (this._entity = c)}>
                  {(this.state.entities || []).map((item) => {
                    return (
                      <option key={item.name} value={item.name}>
                        {item.label}
                      </option>
                    )
                  })}
                </select>
              </div>
            </div>
            <div className="form-group row pb-1">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('TemplateFile')}</label>
              <div className="col-sm-9">
                <div className="float-left">
                  <div className="file-select">
                    <input type="file" className="inputfile" id="upload-input" accept=".xlsx,.xls" data-local="true" ref={(c) => (this.__upload = c)} />
                    <label htmlFor="upload-input" className="btn-secondary">
                      <i className="zmdi zmdi-upload"></i>
                      <span>{$L('SelectFile')}</span>
                    </label>
                  </div>
                </div>
                <div className="float-left ml-2" style={{ paddingTop: 8 }}>
                  {this.state.uploadFileName && <u className="text-bold">{this.state.uploadFileName}</u>}
                </div>
                <div className="clearfix"></div>
                <p className="form-text mt-0 mb-1 link" dangerouslySetInnerHTML={{ __html: $L('HowWriteTemplateTips') }}></p>
                {(this.state.invalidVars || []).length > 0 && <p className="form-text text-danger mt-0 mb-1">{$L('ExistsInvalidFieldsX').replace('%s', `{${this.state.invalidVars.join('} {')}}`)}</p>}
              </div>
            </div>
          </React.Fragment>
        )}
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('Name')}</label>
          <div className="col-sm-7">
            <input type="text" className="form-control form-control-sm" data-id="name" onChange={this.handleChange} value={this.state.name || ''} />
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
    super.componentDidMount()
    setTimeout(() => {
      if (this.__select2) this.__select2.on('change', () => this.checkTemplate())
    }, 500)

    const that = this
    if (this.__upload) {
      let mp = false
      $createUploader(
        this.__upload,
        () => {
          if (!mp) {
            $mp.start()
            mp = true
          }
        },
        (res) => {
          $mp.end()
          mp = false
          that.__lastFile = res.key
          that.checkTemplate()
        }
      )
    }
  }

  // 检查模板
  checkTemplate() {
    const file = this.__lastFile
    const entity = this.__select2.val()
    if (!file || !entity) return

    $.get(`/admin/data/report-templates/check-template?file=${file}&entity=${entity}`, (res) => {
      $mp.end()
      if (res.error_code === 0) {
        const fileName = $fileCutName(file)
        this.setState({
          templateFile: file,
          uploadFileName: fileName,
          name: this.state.name || fileName,
          invalidVars: res.data.invalidVars,
        })
      } else {
        this.setState({
          templateFile: null,
          uploadFileName: null,
          invalidVars: null,
        })
        RbHighbar.error(res.error_msg)
      }
    })
  }

  confirm = () => {
    const post = { name: this.state['name'] }
    if (!post.name) {
      RbHighbar.create($L('PlsInputSome,Name'))
      return
    }
    if (this.props.id) {
      post.isDisabled = this.state.isDisabled === true
    } else {
      post.belongEntity = this.__select2.val()
      if (!post.belongEntity) {
        RbHighbar.create($L('PlsSelectSome,ApplyEntity'))
        return
      }
      post.templateFile = this.state.templateFile
      if (!post.templateFile) {
        RbHighbar.create($L('PlsUploadFile'))
        return
      }
    }
    post.metadata = { entity: 'DataReportConfig', id: this.props.id }

    this.disabled(true)
    $.post('/app/entity/common-save', JSON.stringify(post), (res) => {
      if (res.error_code === 0) location.reload()
      else RbHighbar.error(res.error_msg)
      this.disabled()
    })
  }
}
