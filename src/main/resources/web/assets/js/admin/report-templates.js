/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global dlgActionAfter ShowEnable */

$(document).ready(() => {
  $('.J_add').on('click', () => renderRbcomp(<ReportEditor />))
  $('.J_add-html5').on('click', () => renderRbcomp(<ReportEditor isHtml5 />))

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
          let outputType = (item[7] || {}).outputType || 'excel'
          const isHtml5 = item[6] === 3
          if (isHtml5) outputType = ''

          return (
            <tr key={item[0]}>
              <td>
                {isHtml5 ? (
                  <a title={$L('在线模板编辑')} href={`report-template/design?id=${item[0]}`}>
                    {item[3]}
                  </a>
                ) : (
                  item[3]
                )}
                {item[6] === 1 && <span className="badge badge-info badge-arrow3 badge-pill ml-1 excel">EXCEL</span>}
                {item[6] === 2 && <span className="badge badge-info badge-arrow3 badge-pill ml-1 excel">{$L('EXCEL 列表')}</span>}
                {isHtml5 && <span className="badge badge-info badge-arrow3 badge-pill ml-1 html5">{$L('在线模板')}</span>}
                {item[6] === 4 && <span className="badge badge-info badge-arrow3 badge-pill ml-1 word">WORD</span>}

                {outputType.includes('pdf') && <span className="badge badge-secondary badge-pill ml-1">PDF</span>}
                {outputType.includes('html') && <span className="badge badge-secondary badge-pill ml-1">HTML</span>}
              </td>
              <td>
                <a href={`${rb.baseUrl}/admin/entity/${item[1]}/base`} className="light-link" target={`_${item[1]}`}>
                  {item[2] || item[1]}
                </a>
              </td>
              <td>
                <div className="text-break" style={{ maxWidth: 300 }}>
                  {item[7] && item[7].useFilter && item[7].useFilter.items.length > 0 ? $L('已设置条件') : <span className="text-muted">{$L('无')}</span>}
                </div>
              </td>
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
    renderRbcomp(<ReportEditor id={item[0]} name={item[3]} isDisabled={item[4]} extraDefinition={item[7]} isHtml5={item[6] === 3} entity={item[1]} type={item[6]} />)
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
    this.confirmText = this.props.isHtml5 && !this.props.id ? $L('下一步') : null
    this.hasDetail = true
  }

  renderFrom() {
    const isHtml5Style = this.props.isHtml5 ? { display: 'none' } : {}

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
            <div className="form-group row" style={isHtml5Style}>
              <label className="col-sm-3 col-form-label text-sm-right">{$L('模板类型')}</label>
              <div className="col-sm-7 pt-1" ref={(c) => (this._$listType = c)}>
                <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-0">
                  <input className="custom-control-input" type="radio" value="1" name="reportType" defaultChecked onChange={() => this.checkTemplate()} />
                  <span className="custom-control-label">EXCEL</span>
                </label>
                <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-0">
                  <input className="custom-control-input" type="radio" value="2" name="reportType" onChange={() => this.checkTemplate()} />
                  <span className="custom-control-label">{$L('EXCEL 列表')}</span>
                </label>
                <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-0">
                  <input className="custom-control-input J_word4" type="radio" value="4" name="reportType" onChange={() => this.checkTemplate()} />
                  <span className="custom-control-label">
                    WORD <sup className="rbv" />
                  </span>
                </label>
              </div>
            </div>
          </RF>
        )}
        <div className={`form-group row ${this.props.id ? 'bosskey-show' : ''}`} style={isHtml5Style}>
          <label className="col-sm-3 col-form-label text-sm-right">{$L('模板文件')}</label>
          <div className="col-sm-9">
            <div className="float-left">
              <div className="file-select">
                <input type="file" className="inputfile" id="upload-input" accept=".xlsx,.xls,.docx" data-local="true" ref={(c) => (this._$upload = c)} />
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
            {this.state.invalidMsg && (
              <div className="invalid-vars mt-2 mr-3">
                <RbAlertBox message={this.state.invalidMsg} />
              </div>
            )}
          </div>
        </div>

        <div className="form-group row" style={isHtml5Style}>
          <label className="col-sm-3 col-form-label text-sm-right">{$L('导出格式')}</label>
          <div className="col-sm-7 pt-1" ref={(c) => (this._$outputType = c)}>
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
              <input className="custom-control-input" type="checkbox" value="excel" />
              <span className="custom-control-label">{$L('默认')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
              <input className="custom-control-input" type="checkbox" value="pdf" />
              <span className="custom-control-label">PDF</span>
              <a title={$L('查看如何使用')} target="_blank" href="https://getrebuild.com/docs/admin/excel-admin#%E6%8A%A5%E8%A1%A8%E5%AF%BC%E5%87%BA%E6%A0%BC%E5%BC%8F">
                <i className="zmdi zmdi-help zicon down-1" />
              </a>
            </label>
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0 bosskey-show">
              <input className="custom-control-input" type="checkbox" value="html" />
              <span className="custom-control-label">HTML</span>
            </label>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('名称')}</label>
          <div className="col-sm-7">
            <input type="text" className="form-control form-control-sm" data-id="name" onChange={this.handleChange} value={this.state.name || ''} />
          </div>
        </div>
        <div className="form-group row pt-1 pb-1">
          <label className="col-sm-3 col-form-label text-sm-right">
            {$L('使用条件')} <sup className="rbv" />
          </label>
          <div className="col-sm-7">
            <div className="form-control-plaintext">
              <a
                href="javascript:;"
                onClick={(e) => {
                  if ($(e.target).attr('disabled')) return
                  this._useFilter()
                }}
                ref={(c) => (this._$useFilter = c)}>
                {this.state.useFilter && this.state.useFilter.items.length > 0 ? $L('已设置条件') + ` (${this.state.useFilter.items.length})` : $L('点击设置')}
              </a>
            </div>
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
      if (this.__select2)
        this.__select2.on('change', () => {
          this.checkTemplate()

          this._UseFilter = null
          this.setState({ useFilter: null })
        })
    }, 500)

    const that = this
    if (this._$upload) {
      $createUploader(
        this._$upload,
        () => {
          that.setState({ uploadFileName: null })
          $mp.start()
        },
        (res) => {
          $mp.end()
          that.__lastFile = res.key
          if (this.__select2) {
            // 自动选择 WORD
            if (that.__lastFile.toLowerCase().endsWith('.docx')) $('.J_word4').attr('checked', true)
            that.checkTemplate()
          } else {
            const fileName = $fileCutName(this.__lastFile)
            this.setState(
              {
                templateFile: this.__lastFile,
                uploadFileName: fileName,
              },
              () => {
                // ...
              }
            )
          }
        }
      )
    }

    let e = $('.aside-tree li.active>a').attr('href')
    e = e ? e.split('=')[1] : null
    if (e) {
      setTimeout(() => $(this._entity).val(e).trigger('change'), 300)
    }

    $(this._$outputType).find('[data-toggle="tooltip"]').tooltip()

    if (this.props.id) {
      const outputType = (this.props.extraDefinition || {}).outputType || 'excel'
      if (outputType.includes('excel')) $(this._$outputType).find('input:eq(0)').attr('checked', true)
      if (outputType.includes('pdf')) $(this._$outputType).find('input:eq(1)').attr('checked', true)
      if (outputType.includes('html')) $(this._$outputType).find('input:eq(2)').attr('checked', true)

      const useFilter = (this.props.extraDefinition || {}).useFilter
      this.setState({ useFilter })
      if (this.props.type === 2) $(this._$useFilter).attr('disabled', true)
    } else {
      if (this.props.isHtml5) return

      $(this._$outputType).find('input:eq(0)').attr('checked', true)

      const $pw = $(`<a class="btn btn-secondary ml-2"><i class="icon zmdi zmdi-eye mr-1"></i>${$L('预览')}</a>`)
      $(this._btns).find('.btn-primary').after($pw)
      $pw.on('click', () => {
        if (this.props.id) {
          window.open(`./report-templates/preview?id=${this.props.id}`)
        } else {
          const ps = this._buildParams()
          if (ps === false) return RbHighbar.create($L('请选择应用实体并上传模板文件'))

          let output // excel
          if ($val($(this._$outputType).find('input:eq(1)'))) output = 'pdf'
          else if ($val($(this._$outputType).find('input:eq(2)'))) output = 'html'

          window.open(`./report-templates/preview?${ps}&output=${output || ''}`)
        }
      })
    }

    this.props.id && console.log(`RBAPI ASSISTANT *Report* :\n %c${this.props.id}`, 'color:#e83e8c;font-size:16px;font-weight:bold;font-style:italic;')
  }

  // 检查模板
  checkTemplate() {
    const ps = this._buildParams()
    if (ps === false) return

    $.get(`/admin/data/report-templates/check-template?${ps}`, (res) => {
      if (res.error_code === 0) {
        const fileName = $fileCutName(this.__lastFile)
        this.setState(
          {
            templateFile: this.__lastFile,
            uploadFileName: fileName,
            name: this.state.name || fileName,
            invalidVars: res.data.invalidVars,
            invalidMsg: res.data.invalidMsg,
          },
          () => {
            // ...
          }
        )
      } else {
        this._clearParams()
        RbHighbar.error(res.error_msg)
      }
    })
  }

  _useFilter() {
    if (this._UseFilter) {
      this._UseFilter.show()
    } else {
      const entity = this.__select2 ? this.__select2.val() : this.props.entity
      const that = this
      renderRbcomp(
        <AdvFilter
          title={$L('使用条件')}
          inModal
          canNoFilters
          entity={entity}
          filter={this.state.useFilter}
          confirm={(s) => {
            this.setState({ useFilter: s })
          }}
        />,
        function () {
          that._UseFilter = this
        }
      )
    }
  }

  _buildParams() {
    const type = ~~$(this._$listType).find('input:checked').val() || 1
    $(this._$useFilter).attr('disabled', type === 2)

    const entity = this.__select2.val()
    const file = this.__lastFile
    if (!file || !entity) return false

    if (type === 4 && rb.commercial < 10) {
      RbHighbar.error(WrapHtml($L('免费版不支持 WORD 模板功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      this._clearParams()
      return false
    }

    return `file=${$encode(file)}&entity=${entity}&type=${type}`
  }

  _clearParams() {
    this.setState({
      templateFile: null,
      uploadFileName: null,
      invalidVars: null,
      invalidMsg: null,
    })
    this.__lastFile = null
  }

  confirm = () => {
    const post = { name: this.state['name'] }
    if (!post.name) return RbHighbar.create($L('请输入名称'))

    const output = []
    if ($val($(this._$outputType).find('input:eq(0)'))) output.push('excel')
    if ($val($(this._$outputType).find('input:eq(1)'))) output.push('pdf')
    if ($val($(this._$outputType).find('input:eq(2)'))) output.push('html')

    // v3.3
    post.extraDefinition = {
      outputType: output.length === 0 ? 'excel' : output.join(','),
      templateVersion: (this.props.extraDefinition || {}).templateVersion || 2,
      // v3.7
      useFilter: rb.commercial < 1 ? null : this.state.useFilter,
    }

    if (this.props.id) {
      post.isDisabled = this.state.isDisabled === true
      if (this.state.templateFile) post.templateFile = this.state.templateFile
    } else {
      post.belongEntity = this.__select2.val()
      post.templateFile = this.state.templateFile
      post.templateType = $(this._$listType).find('input:checked').val() || 1
      if (!post.belongEntity) return RbHighbar.create($L('请选择应用实体'))
      if (this.props.isHtml5) {
        post.templateType = 3
      } else {
        if (!post.templateFile) return RbHighbar.create($L('请上传模板文件'))
      }
      post.extraDefinition.templateVersion = 3
    }

    post.metadata = {
      entity: 'DataReportConfig',
      id: this.props.id,
    }

    this.disabled(true)
    $.post('/app/entity/common-save', JSON.stringify(post), (res) => {
      if (res.error_code === 0) {
        if (this.props.isHtml5 && !this.props.id) location.href = `report-template/design?id=${res.data.id}`
        else dlgActionAfter(this)
      } else {
        RbHighbar.error(res.error_msg)
      }
      this.disabled()
    })
  }
}
