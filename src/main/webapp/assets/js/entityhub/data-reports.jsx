/* eslint-disable react/jsx-no-target-blank */
$(document).ready(function () {
  $('.J_add').click(() => { renderRbcomp(<ReporEdit />) })
  renderRbcomp(<ReportList />, 'dataList')
})

class ReportList extends ConfigList {
  constructor(props) {
    super(props)
    this.requestUrl = `${rb.baseUrl}/admin/datas/data-reports/list`
  }
  render() {
    return <React.Fragment>
      {(this.state.data || []).map((item) => {
        return <tr key={'k-' + item[0]}>
          <td>{item[3]}</td>
          <td>{item[2] || item[1]}</td>
          <td>{item[4] ? <span className="badge badge-warning font-weight-normal">否</span> : <span className="badge badge-success font-weight-light">是</span>}</td>
          <td>{item[5]}</td>
          <td className="actions">
            <a className="icon" title="预览" href={`${rb.baseUrl}/admin/datas/data-reports/preview?id=${item[0]}`} target="_blank"><i className="zmdi zmdi-eye" /></a>
            <a className="icon" title="修改" onClick={() => this.handleEdit(item)}><i className="zmdi zmdi-edit" /></a>
            <a className="icon" title="删除" onClick={() => this.handleDelete(item[0])}><i className="zmdi zmdi-delete" /></a>
          </td>
        </tr>
      })}
    </React.Fragment>
  }

  handleEdit(item) {
    renderRbcomp(<ReporEdit id={item[0]} name={item[3]} isDisabled={item[4]} />)
  }
  handleDelete(id) {
    let handle = super.handleDelete
    RbAlert.create('确认删除此报表模板？', {
      type: 'danger',
      confirmText: '删除',
      confirm: function () {
        this.disabled(true)
        handle(id)
      }
    })
  }
}

class ReporEdit extends ConfigFormDlg {
  constructor(props) {
    super(props)
    this.subtitle = '报表模板'
  }
  renderFrom() {
    return <React.Fragment>
      <div className="form-group row">
        <label className="col-sm-3 col-form-label text-sm-right">报表名称</label>
        <div className="col-sm-7">
          <input type="text" className="form-control form-control-sm" data-id="name" onChange={this.handleChange} value={this.state.name || ''} />
        </div>
      </div>
      {!this.props.id &&
        <React.Fragment>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">选择应用实体</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" ref={(c) => this._entity = c}>
                {(this.state.entities || []).map((item) => {
                  return <option key={'e-' + item.name} value={item.name}>{item.label}</option>
                })}
              </select>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">模板文件</label>
            <div className="col-sm-9">
              <div className="float-left">
                <div className="file-select">
                  <input type="file" className="inputfile" id="upload-input" accept=".xlsx,.xls" data-maxsize="5000000" ref={(c) => this.__upload = c} />
                  <label htmlFor="upload-input" className="btn-secondary"><i className="zmdi zmdi-upload"></i><span>选择文件</span></label>
                </div>
              </div>
              <div className="float-left ml-2" style={{ paddingTop: 8 }}>
                {this.state.uploadFileName && <div className="text-bold">{this.state.uploadFileName}</div>}
              </div>
              <div className="clearfix"></div>
              {(this.state.invalidVars || []).length > 0 && <div className="text-danger">
                存在无效字段 {'${'}{this.state.invalidVars.join('} ${')}{'}'}，建议修改
              </div>}
            </div>
          </div>
        </React.Fragment>
      }
      {this.props.id &&
        <div className="form-group row">
          <div className="col-sm-7 offset-sm-3">
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
              <input className="custom-control-input" type="checkbox" checked={this.state.isDisabled === true} data-id="isDisabled" onChange={this.handleChange} />
              <span className="custom-control-label">是否禁用</span>
            </label>
          </div>
        </div>
      }
    </React.Fragment>
  }
  componentDidMount() {
    super.componentDidMount()
    setTimeout(() => {
      if (this.__select2) this.__select2.on('change', () => this.checkTemplate())
    }, 500)

    const that = this
    if (this.__upload) {
      $(this.__upload).html5Uploader({
        postUrl: rb.baseUrl + '/filex/upload',
        onSelectError: function (field, error) {
          if (error === 'ErrorType') RbHighbar.create('请上传 Excel 文件')
          else if (error === 'ErrorMaxSize') RbHighbar.create('文件不能大于 5M')
        },
        onSuccess: function (d) {
          d = JSON.parse(d.currentTarget.response)
          if (d.error_code === 0) {
            that.__lastFile = d.data
            that.checkTemplate()
          } else RbHighbar.error('上传失败，请稍后重试')
        }
      })
    }
  }

  // 检查模板
  checkTemplate() {
    let file = this.__lastFile
    let entity = this.__select2.val()
    if (!file || !entity) return

    $.get(`${rb.baseUrl}/admin/datas/data-reports/check-template?file=${file}&entity=${entity}`, (res) => {
      if (res.error_code === 0) {
        let fileName = $fileCutName(file)
        this.setState({
          templateFile: file,
          uploadFileName: fileName,
          name: this.state.name || fileName,
          invalidVars: res.data.invalidVars
        })
      } else {
        this.setState({
          templateFile: null,
          uploadFileName: null,
          invalidVars: null
        })
        RbHighbar.error(res.error_msg)
      }
    })
  }

  confirm = () => {
    let post = { name: this.state['name'] }
    if (!post.name) { RbHighbar.create('请输入报表名称'); return }
    if (this.props.id) {
      post.isDisabled = this.state.isDisabled === true
    } else {
      post.belongEntity = this.__select2.val()
      if (!post.belongEntity) { RbHighbar.create('请选择应用实体'); return }
      post.templateFile = this.state.templateFile
      if (!post.templateFile) { RbHighbar.create('请上传模板文件'); return }
    }
    post.metadata = { entity: 'DataReportConfig', id: this.props.id }

    this.disabled(true)
    $.post(rb.baseUrl + '/app/entity/record-save', JSON.stringify(post), (res) => {
      if (res.error_code === 0) location.reload()
      else RbHighbar.error(res.error_msg)
      this.disabled()
    })
  }
}
