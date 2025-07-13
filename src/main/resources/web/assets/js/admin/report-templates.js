/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global dlgActionAfter ShowEnable taggedTitle */

$(document).ready(() => {
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
          let outputType = (item[7] || {}).outputType || 'excel'
          const isHtml5 = item[6] === 3
          if (isHtml5) outputType = ''

          return (
            <tr key={item[0]}>
              <td className="name">
                {isHtml5 ? (
                  <a title={$L('网页模版设计器')} href={`report-template/design?id=${item[0]}`}>
                    {taggedTitle(item[3])}
                  </a>
                ) : (
                  taggedTitle(item[3])
                )}
                {item[6] === 1 && <span className="badge badge-info badge-arrow3 badge-pill ml-1 excel">EXCEL</span>}
                {item[6] === 2 && <span className="badge badge-info badge-arrow3 badge-pill ml-1 excel">{$L('EXCEL 列表')}</span>}
                {isHtml5 && <span className="badge badge-info badge-arrow3 badge-pill ml-1 html5">{$L('网页模板')}</span>}
                {item[6] === 4 && <span className="badge badge-info badge-arrow3 badge-pill ml-1 word">WORD</span>}

                {outputType.includes('pdf') && <span className="badge badge-secondary badge-pill ml-1">PDF</span>}
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
                  <i className="zmdi mdi mdi-file-eye-outline" />
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
    renderRbcomp(<ReportEditor id={item[0]} name={item[3]} isDisabled={item[4]} extraDefinition={item[7]} entity={item[1]} reportType={item[6]} templateFile={item[9]} />)
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
    $logRBAPI(props.id, 'Report')
  }

  renderFrom() {
    const isHtml5 = this.props.reportType === 3 || this.state.reportType === 3
    const templateFile = this.state.templateFile || this.props.templateFile

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
              <label className="col-sm-3 col-form-label text-sm-right">{$L('模板类型')}</label>
              <div className="col-sm-8 pt-1" ref={(c) => (this._$listType = c)}>
                <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                  <input className="custom-control-input" type="radio" value="1" name="reportType" defaultChecked onChange={(e) => this.checkTemplate(e)} />
                  <span className="custom-control-label">EXCEL</span>
                </label>
                <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                  <input className="custom-control-input J_word4" type="radio" value="4" name="reportType" onChange={(e) => this.checkTemplate(e)} />
                  <span className="custom-control-label">
                    WORD <sup className="rbv" />
                  </span>
                </label>
                <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                  <input className="custom-control-input" type="radio" value="3" name="reportType" onChange={(e) => this.checkTemplate(e)} />
                  <span className="custom-control-label">
                    {$L('网页')} <sup className="rbv" />
                  </span>
                </label>
                <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                  <input className="custom-control-input" type="radio" value="2" name="reportType" onChange={(e) => this.checkTemplate(e)} />
                  <span className="custom-control-label">{$L('EXCEL 列表')}</span>
                </label>
              </div>
            </div>
          </RF>
        )}
        <div className={`form-group row ${isHtml5 && 'hide'}`}>
          <label className="col-sm-3 col-form-label text-sm-right">{$L('模板文件')}</label>
          <div className="col-sm-9">
            <div className="float-left">
              <div className="file-select">
                <input type="file" className="inputfile" id="upload-input" accept=".xlsx,.xls,.docx" data-local="true" data-updir="REPORT_TEMPLATES" ref={(c) => (this._$upload = c)} />
                <label htmlFor="upload-input" className="btn-secondary">
                  <i className="zmdi zmdi-upload" />
                  <span>{this.props.id ? $L('重新上传') : $L('选择文件')}</span>
                </label>
              </div>
            </div>
            <div className="float-left bosskey-show ml-2" style={{ paddingTop: 8 }}>
              <a className="btn-secondary" target="_blank" href={`${rb.baseUrl}/commons/file-editor?src=${$encode('/data/' + templateFile)}&id=${this.props.id}`}>
                {$L('在线编辑')} (LAB)
              </a>
            </div>
            <div className="float-left ml-2" style={{ paddingTop: 8 }}>
              {templateFile && (
                <a href={`${rb.baseUrl}/admin/data/report-templates/download?file=${$encode(templateFile)}`} target="_blank" title={$L('下载模版')} className="text-bold text-dark text-underline">
                  {$fileCutName(templateFile)}
                </a>
              )}
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
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('导出格式')}</label>
          <div className="col-sm-7 pt-1" ref={(c) => (this._$outputType = c)}>
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-1">
              <input className="custom-control-input" type="checkbox" value="excel" />
              <span className="custom-control-label">{$L('默认')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-1">
              <input className="custom-control-input" type="checkbox" value="pdf" disabled={isHtml5} />
              <span className="custom-control-label">PDF</span>
              <a href="https://getrebuild.com/docs/admin/excel-admin#%E5%AF%BC%E5%87%BA%20PDF%20%E6%A0%BC%E5%BC%8F" title={$L('查看帮助')} target="_blank">
                <i className="zmdi zmdi-help zicon down-1" />
              </a>
            </label>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('名称')}</label>
          <div className="col-sm-7">
            <input type="text" className="form-control form-control-sm" data-id="name" onChange={this.handleChange} value={this.state.name || ''} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">
            {$L('使用条件')} <sup className="rbv" />
          </label>
          <div className="col-sm-7">
            <div className="form-control-plaintext">
              <a
                href="###"
                disabled={this.state.reportType === 2}
                onClick={(e) => {
                  $stopEvent(e, true)
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
          <div className="form-group row pt-0">
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
          that.setState({ templateFile: null })
          $mp.start()
        },
        (res) => {
          $mp.end()
          that.__lastFile = res.key
          // 新建时
          if (this.__select2) {
            // 自动选择 WORD
            if (that.__lastFile.toLowerCase().endsWith('.docx') && that.state.reportType !== 4) {
              $('.J_word4')[0].click()
            } else {
              that.checkTemplate()
            }
          } else {
            // v3.8
            that.checkTemplate()
          }
        }
      )
    }

    let e = $('.aside-tree li.active>a').attr('href')
    e = e ? e.split('=')[1] : null
    if (e) {
      setTimeout(() => $(this._entity).val(e).trigger('change'), 300)
    }

    if (this.props.id) {
      const outputType = (this.props.extraDefinition || {}).outputType || 'excel'
      if (outputType.includes('excel')) $(this._$outputType).find('input:eq(0)').attr('checked', true)
      if (outputType.includes('pdf')) $(this._$outputType).find('input:eq(1)').attr('checked', true)

      const useFilter = (this.props.extraDefinition || {}).useFilter
      this.setState({ useFilter })
      if (this.props.reportType === 2) $(this._$useFilter).attr('disabled', true)
    } else {
      $(this._$outputType).find('input:eq(0)').attr('checked', true)
    }

    const $p = $(`<button type="button" class="btn btn-secondary ml-2 J_pw3"><i class="icon mdi mdi-file-eye-outline mr-1"></i>${$L('预览')}</button>`)
    $(this._btns).find('.btn-primary').after($p)
    $p.on('click', () => {
      const ps = this._buildParams()
      if (ps === false) return RbHighbar.create($L('请选择应用实体并上传模板文件'))

      let output // default
      let url = `./report-templates/preview?${ps}&output=${output || ''}`
      if (this.state.reportType === 3) url += '&id=' + this.state.id // H5
      window.open(url)
    })
  }

  // 检查模板
  checkTemplate(e) {
    if (e) {
      this.setState({ reportType: ~~e.target.value }, () => {
        if (this.state.reportType === 3) {
          $('.J_pw3').addClass('hide')
          $(this._btns).find('.btn-primary').text($L('下一步'))
        } else {
          $('.J_pw3').removeClass('hide')
          $(this._btns).find('.btn-primary').text($L('确定'))
        }
      })
    }

    setTimeout(() => {
      const ps = this._buildParams()
      if (ps === false) return
      if (this.state.reportType === 3) return

      $.get(`/admin/data/report-templates/check-template?${ps}`, (res) => {
        if (res.error_code === 0) {
          this.setState(
            {
              templateFile: this.__lastFile,
              name: this.state.name || $fileCutName(this.__lastFile, true),
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
    }, 200)
  }

  _useFilter() {
    if (rb.commercial < 10) {
      RbHighbar.error(WrapHtml($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }

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
    const type = this.state.reportType
    if (type === 2) this.setState({ useFilter: null })

    const entity = this.__select2 ? this.__select2.val() : this.props.entity
    const file = this.__lastFile || this.props.templateFile
    if (type === 3) {
      if (!entity) return false
    } else {
      if (!file || !entity) return false
    }

    if ((type === 4 || type === 3) && rb.commercial < 10) {
      RbHighbar.error(WrapHtml($L('免费版不支持 WORD 和网页模板功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      this._clearParams()
      return false
    }

    return `file=${$encode(file)}&entity=${entity}&type=${type || ''}`
  }

  _clearParams() {
    this.setState({
      templateFile: null,
      invalidVars: null,
      invalidMsg: null,
    })
    this.__lastFile = null
  }

  confirm = () => {
    const post = { name: this.state['name'] }
    if (!post.name) return RbHighbar.create($L('请输入名称'))

    let output = []
    if ($val($(this._$outputType).find('input:eq(0)'))) output.push('excel')
    if ($val($(this._$outputType).find('input:eq(1)'))) output.push('pdf')
    // default
    if (this.state.reportType === 3) output = ['excel']

    // v3.3
    post.extraDefinition = {
      outputType: output.length === 0 ? 'excel' : output.join(','),
      templateVersion: (this.props.extraDefinition || {}).templateVersion || 2,
      // v3.7
      useFilter: rb.commercial < 1 ? null : this.state.useFilter,
    }

    if (this.props.id) {
      post.isDisabled = this.state.isDisabled === true
      if (this.__lastFile) post.templateFile = this.__lastFile
    } else {
      post.belongEntity = this.__select2.val()
      post.templateFile = this.state.templateFile
      post.templateType = this.state.reportType
      if (!post.belongEntity) return RbHighbar.create($L('请选择应用实体'))
      if (post.templateType !== 3) {
        if (!post.templateFile) return RbHighbar.create($L('请上传模板文件'))
      }
      post.extraDefinition.templateVersion = 3

      if ((post.templateType === 4 || post.templateType === 3) && rb.commercial < 10) {
        RbHighbar.error(WrapHtml($L('免费版不支持 WORD 和网页模板功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
        return
      }
    }

    post.metadata = {
      entity: 'DataReportConfig',
      id: this.props.id,
    }

    this.disabled(true)
    $.post('/app/entity/common-save', JSON.stringify(post), (res) => {
      if (res.error_code === 0) {
        if (post.templateType === 3 && !this.props.id) location.href = `report-template/design?id=${res.data.id}`
        else dlgActionAfter(this)
      } else {
        RbHighbar.error(res.error_msg)
      }
      this.disabled()
    })
  }
}
