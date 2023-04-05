/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global dlgActionAfter ShowEnable */

$(document).ready(function () {
  $('.J_add').on('click', () => renderRbcomp(<ReportEditor />))
  renderRbcomp(<ReportList />, 'dataList')
})

class ReportList extends ConfigList {
  constructor(props) {
    super(props)
    this.requestUrl = '/admin/data/report-templates/list'
  }

  render() {
    return (
      <RF>
        {(this.state.data || []).map((item) => {
          const outputType = (item[7] || {}).outputType || 'excel'
          return (
            <tr key={item[0]}>
              <td>
                {item[3]}
                {item[6] === 2 && <span className="badge badge-secondary badge-arrow3 badge-sm ml-1">{$L('列表模板')}</span>}
                {outputType.includes('excel') && <span className="badge badge-secondary badge-sm ml-1">Excel</span>}
                {outputType.includes('pdf') && <span className="badge badge-secondary badge-sm ml-1">PDF</span>}
              </td>
              <td>{item[2] || item[1]}</td>
              <td>{ShowEnable(item[4])}</td>
              <td>
                <DateShow date={item[5]} />
              </td>
              <td className="actions">
                <a className="icon" title={$L('预览')} href={`${rb.baseUrl}/admin/data/report-templates/preview?id=${item[0]}`} target="_blank">
                  <i className="zmdi zmdi-eye" />
                </a>
                <a className="icon" title={$L('下载模板')} href={`${rb.baseUrl}/admin/data/report-templates/download?id=${item[0]}`} target="_blank">
                  <i className="zmdi zmdi-download" />
                </a>
                <a className="icon" title={$L('修改')} onClick={() => this.handleEdit(item)}>
                  <i className="zmdi zmdi-edit" />
                </a>
                <a className="icon danger-hover" title={$L('删除')} onClick={() => this.handleDelete(item[0])}>
                  <i className="zmdi zmdi-delete" />
                </a>
              </td>
            </tr>
          )
        })}
      </RF>
    )
  }

  handleEdit(item) {
    renderRbcomp(<ReportEditor id={item[0]} name={item[3]} isDisabled={item[4]} extraDefinition={item[7]} />)
  }

  handleDelete(id) {
    const handle = super.handleDelete
    RbAlert.create($L('确认删除此报表模板？'), {
      type: 'danger',
      confirmText: $L('删除'),
      confirm: function () {
        this.disabled(true)
        handle(id, () => dlgActionAfter(this))
      },
    })
  }
}

class ReportEditor extends ConfigFormDlg {
  constructor(props) {
    super(props)
    this.subtitle = $L('报表模板')
    this.hasDetail = true
  }

  renderFrom() {
    return (
      <RF>
        {!this.props.id && (
          <RF>
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('选择应用实体')}</label>
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
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('模板文件')}</label>
              <div className="col-sm-9">
                <div className="float-left">
                  <div className="file-select">
                    <input type="file" className="inputfile" id="upload-input" accept=".xlsx,.xls" data-local="true" ref={(c) => (this.__upload = c)} />
                    <label htmlFor="upload-input" className="btn-secondary">
                      <i className="zmdi zmdi-upload" />
                      <span>{$L('选择文件')}</span>
                    </label>
                  </div>
                </div>
                <div className="float-left ml-2" style={{ paddingTop: 8 }}>
                  {this.state.uploadFileName && <u className="text-bold">{this.state.uploadFileName}</u>}
                </div>
                <div className="clearfix" />
                <p className="form-text mt-0 mb-0 link" dangerouslySetInnerHTML={{ __html: $L('[如何编写模板文件](https://getrebuild.com/docs/admin/excel-admin)') }} />

                {(this.state.invalidVars || []).length > 0 && (
                  <div className="invalid-vars mt-2 mr-3">
                    <RbAlertBox message={$L('存在无效字段 %s 建议修改', `{${this.state.invalidVars.join('} {')}}`)} />
                  </div>
                )}
              </div>
            </div>
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right" />
              <div className="col-sm-7">
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0" ref={(c) => (this._$listType = c)}>
                  <input className="custom-control-input" type="checkbox" />
                  <span className="custom-control-label">
                    {$L('这是一个列表模板')}
                    <a title={$L('查看如何使用')} target="_blank" href="https://getrebuild.com/docs/admin/excel-admin#%E5%9C%A8%E5%88%97%E8%A1%A8%E4%B8%AD%E4%BD%BF%E7%94%A8">
                      <i className="zmdi zmdi-help zicon down-1" />
                    </a>
                  </span>
                </label>

                {this.state.invalidMsg && (
                  <div className="invalid-vars mt-2 mr-3">
                    <RbAlertBox message={this.state.invalidMsg} />
                  </div>
                )}
              </div>
            </div>
          </RF>
        )}

        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('名称')}</label>
          <div className="col-sm-7">
            <input type="text" className="form-control form-control-sm" data-id="name" onChange={this.handleChange} value={this.state.name || ''} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('报表导出格式')}</label>
          <div className="col-sm-7 pt-1" ref={(c) => (this._$outputType = c)}>
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
              <input className="custom-control-input" type="checkbox" value="excel" />
              <span className="custom-control-label">Excel</span>
            </label>
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
              <input className="custom-control-input" type="checkbox" value="pdf" />
              <span className="custom-control-label">PDF</span>
              <a title={$L('查看如何使用')} target="_blank" href="https://getrebuild.com/docs/admin/excel-admin">
                <i className="zmdi zmdi-help zicon down-1" />
              </a>
            </label>
          </div>
        </div>

        {this.props.id && (
          <div className="form-group row">
            <div className="col-sm-7 offset-sm-3">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" checked={this.state.isDisabled === true} data-id="isDisabled" onChange={this.handleChange} />
                <span className="custom-control-label">{$L('是否禁用')}</span>
              </label>
            </div>
          </div>
        )}
      </RF>
    )
  }

  componentDidMount() {
    super.componentDidMount()
    setTimeout(() => {
      if (this.__select2) this.__select2.on('change', () => this.checkTemplate())
    }, 500)

    const that = this
    if (this.__upload) {
      $createUploader(
        this.__upload,
        () => $mp.start(),
        (res) => {
          $mp.end()
          that.__lastFile = res.key
          that.checkTemplate()
        }
      )
    }

    let e = $('.aside-tree li.active>a').attr('href')
    e = e ? e.split('=')[1] : null
    if (e) {
      setTimeout(() => $(this._entity).val(e).trigger('change'), 300)
    }

    $(this._$listType)
      .on('change', () => this.checkTemplate())
      .find('[data-toggle="tooltip"]')
      .tooltip()

    $(this._$outputType).find('[data-toggle="tooltip"]').tooltip()

    if (this.props.id) {
      const outputType = (this.props.extraDefinition || {}).outputType || 'excel'
      if (outputType.includes('excel')) $(this._$outputType).find('input:eq(0)').attr('checked', true)
      if (outputType.includes('pdf')) $(this._$outputType).find('input:eq(1)').attr('checked', true)
    } else {
      const $pw = $(`<a class="btn btn-secondary ml-2">${$L('预览')}</a>`)
      $(this._btns).find('.btn-primary').after($pw)
      $pw.on('click', () => {
        if (this.props.id) {
          window.open(`./report-templates/preview?id=${this.props.id}`)
        } else {
          const ps = this._buildParams()
          if (ps === false) return RbHighbar.create($L('请选择应用实体并上传模板文件'))
          window.open(`./report-templates/preview?${ps}`)
        }
      })
    }
  }

  // 检查模板
  checkTemplate() {
    const ps = this._buildParams()
    if (ps === false) return

    $.get(`/admin/data/report-templates/check-template?${ps}`, (res) => {
      if (res.error_code === 0) {
        const fileName = $fileCutName(this.__lastFile)
        this.setState({
          templateFile: this.__lastFile,
          uploadFileName: fileName,
          name: this.state.name || fileName,
          invalidVars: res.data.invalidVars,
          invalidMsg: res.data.invalidMsg,
        })
      } else {
        this.setState({
          templateFile: null,
          uploadFileName: null,
          invalidVars: null,
          invalidMsg: null,
        })
        this.__lastFile = null
        RbHighbar.error(res.error_msg)
      }
    })
  }

  _buildParams() {
    const entity = this.__select2.val()
    const file = this.__lastFile
    if (!file || !entity) return false

    const list = $(this._$listType).find('input').prop('checked')
    return `file=${$encode(file)}&entity=${entity}&list=${list}`
  }

  confirm = () => {
    const post = { name: this.state['name'] }
    if (!post.name) return RbHighbar.create($L('请输入名称'))

    const output = []
    if ($val($(this._$outputType).find('input:eq(1)'))) output.push('pdf')
    if ($val($(this._$outputType).find('input:eq(0)'))) output.push('excel')

    // v3.3
    post.extraDefinition = {
      outputType: output.length === 0 ? 'excel' : output.join(','),
      templateVersion: (this.props.extraDefinition || {}).templateVersion || 2
    }

    if (this.props.id) {
      post.isDisabled = this.state.isDisabled === true
    } else {
      post.belongEntity = this.__select2.val()
      post.templateFile = this.state.templateFile
      post.templateType = $(this._$listType).find('input').prop('checked') ? 2 : 1
      if (!post.belongEntity) return RbHighbar.create($L('请选择应用实体'))
      if (!post.templateFile) return RbHighbar.create($L('请上传模板文件'))
      post.extraDefinition.templateVersion = 3
    }

    post.metadata = {
      entity: 'DataReportConfig',
      id: this.props.id,
    }

    this.disabled(true)
    $.post('/app/entity/common-save', JSON.stringify(post), (res) => {
      if (res.error_code === 0) dlgActionAfter(this)
      else RbHighbar.error(res.error_msg)
      this.disabled()
    })
  }
}
