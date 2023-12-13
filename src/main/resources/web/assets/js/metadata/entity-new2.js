/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global InitModels, FIELD_TYPES */

window.__LAB_SHOWNDETAIL = false
window.bosskeyTrigger = function () {
  window.__LAB_SHOWNDETAIL = true
}

// ~~ 新建实体
// eslint-disable-next-line no-unused-vars
class EntityNew2 extends RbModalHandler {
  constructor(props) {
    super(props)
    this.state = { ...props }
    this.state.excelfile = '141930232__IUI000008390352CNY23100100N.xls'
  }

  render() {
    return (
      <RbModal ref={(c) => (this._dlg = c)} title={$L('添加实体')} className="entity-new2">
        <div className="tab-container" ref={(c) => (this._$container = c)}>
          <ul className="nav nav-tabs">
            <li className="nav-item">
              <a className="nav-link active" href="#MANUAL" data-toggle="tab">
                {$L('手动')}
              </a>
            </li>
            <li className="nav-item">
              <a className="nav-link" href="#COPY" data-toggle="tab">
                {$L('复制')}
              </a>
            </li>
            <li className="nav-item">
              <a className="nav-link" href="#EXCEL" data-toggle="tab">
                {$L('从 EXCEL 导入')}
              </a>
            </li>
            <li className="nav-item">
              <a className="nav-link J_imports" href="#IMPORTS" data-toggle="tab">
                {$L('从 RB 仓库导入')}
              </a>
            </li>
          </ul>
          <div className="tab-content m-0 pb-0">
            <div className="tab-pane active" id="MANUAL">
              <div className="form">
                <div className="form-group row">
                  <label className="col-sm-3 col-form-label text-sm-right">{$L('实体名称')}</label>
                  <div className="col-sm-7">
                    <input className="form-control form-control-sm" type="text" ref={(c) => (this._$entityLabel = c)} maxLength="40" />
                  </div>
                </div>
                <div className="form-group row">
                  <label className="col-sm-3 col-form-label text-sm-right">{$L('备注')}</label>
                  <div className="col-sm-7">
                    <textarea className="form-control form-control-sm row2x" ref={(c) => (this._$comments = c)} maxLength="100" placeholder={$L('(选填)')}></textarea>
                  </div>
                </div>
                <div className="form-group row pt-2">
                  <label className="col-sm-3 col-form-label text-sm-right"></label>
                  <div className="col-sm-7">
                    <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                      <input className="custom-control-input" type="checkbox" ref={(c) => (this._$nameField = c)} />
                      <span className="custom-control-label">{$L('添加一个名称字段')}</span>
                    </label>
                  </div>
                </div>
                <div className="form-group row pt-0">
                  <label className="col-sm-3 col-form-label text-sm-right"></label>
                  <div className="col-sm-7">
                    <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                      <input className="custom-control-input" type="checkbox" ref={(c) => (this._$seriesField = c)} />
                      <span className="custom-control-label">{$L('添加一个自动编号字段')}</span>
                    </label>
                  </div>
                </div>
                <div className="form-group row pt-0">
                  <label className="col-sm-3 col-form-label text-sm-right"></label>
                  <div className="col-sm-7">
                    <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                      <input className="custom-control-input" type="checkbox" ref={(c) => (this._$newIsDetail = c)} onClick={(e) => this.setState({ newIsDetail: $val(e.target) })} />
                      <span className="custom-control-label">
                        {$L('这是明细实体')}
                        <i className="zmdi zmdi-help zicon" data-toggle="tooltip" title={$L('通过明细实体可以更好的组织业务关系。例如订单明细通常依附于订单，而非独立存在')}></i>
                      </span>
                    </label>
                  </div>
                </div>
                <div className={`form-group row ${this.state.newIsDetail ? '' : 'hide'}`}>
                  <label className="col-sm-3 col-form-label text-sm-right">{$L('选择主实体')}</label>
                  <div className="col-sm-7">
                    <select className="form-control form-control-sm" ref={(c) => (this._$mainEntity = c)}>
                      {this.state.entities &&
                        this.state.entities.map((item) => {
                          if (item.mainEntity) return null
                          if (item.detailEntity && !window.__LAB_SHOWNDETAIL) return null
                          return (
                            <option key={item.entityName} value={item.entityName}>
                              {item.entityLabel}
                              {item.detailEntity ? ' (LAB)' : ''}
                            </option>
                          )
                        })}
                    </select>
                  </div>
                </div>
                <div className="form-group row footer">
                  <div className="col-sm-7 offset-sm-3">
                    <button className="btn btn-primary J_new" type="button" onClick={() => this.postNew()}>
                      {$L('确定')}
                    </button>
                    <button className="btn btn-link" type="button" onClick={() => this.hide()}>
                      {$L('取消')}
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div className="tab-pane" id="COPY">
              <div className="form">
                <div className="form-group row">
                  <label className="col-sm-3 col-form-label text-sm-right">{$L('复制哪个实体')}</label>
                  <div className="col-sm-7">
                    <select className="form-control form-control-sm" ref={(c) => (this._$copySourceEntity = c)}>
                      {this.state.entities &&
                        this.state.entities.map((item) => {
                          return (
                            <option key={item.entityName} value={item.entityName}>
                              {item.entityLabel}
                            </option>
                          )
                        })}
                    </select>
                  </div>
                </div>
                <div className="form-group row">
                  <label className="col-sm-3 col-form-label text-sm-right">{$L('实体名称')}</label>
                  <div className="col-sm-7">
                    <input className="form-control form-control-sm" type="text" ref={(c) => (this._$copyEntityLabel = c)} maxLength="40" />
                  </div>
                </div>
                <div className={`form-group row ${this.state.copyHasDetail ? '' : 'hide'}`}>
                  <label className="col-sm-3 col-form-label text-sm-right">{$L('明细实体名称')}</label>
                  <div className="col-sm-7">
                    <input className="form-control form-control-sm" type="text" ref={(c) => (this._$copyDetailLabel = c)} maxLength="40" placeholder={$L('(选填)')} />
                    <p className="form-text">{$L('不填写则不复制明细实体')}</p>
                  </div>
                </div>
                <div className="form-group row footer">
                  <div className="col-sm-7 offset-sm-3">
                    <button className="btn btn-primary J_copy" type="button" onClick={() => this.postCopy()}>
                      {$L('确定')}
                    </button>
                    <button className="btn btn-link" type="button" onClick={() => this.hide()}>
                      {$L('取消')}
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div className="tab-pane" id="EXCEL">
              <div className="form">
                <div className="form-group row">
                  <label className="col-sm-3 col-form-label text-sm-right">{$L('上传数据文件')}</label>
                  <div className="col-sm-7">
                    <div className="file-select">
                      <input type="file" className="inputfile" accept=".xlsx,.xls,.csv" data-local="temp" ref={(c) => (this._$uploadfile = c)} />
                      <label htmlFor="upload-input" className="btn-secondary mb-0" ref={(c) => (this._$uploadbtn = c)}>
                        <i className="zmdi zmdi-upload"></i>
                        <span>{$L('上传文件')}</span>
                      </label>
                    </div>
                    {this.state.excelfile && (
                      <div className="mt-1">
                        <u className="text-bold">{$fileCutName(this.state.excelfile)}</u>
                      </div>
                    )}
                    <p className="form-text">{$L('[点击查看](https://getrebuild.com/docs/admin/entity/) 数据文件格式要求')}</p>
                  </div>
                </div>
                <div className="form-group row footer">
                  <div className="col-sm-7 offset-sm-3">
                    <button className="btn btn-primary J_excel" type="button" onClick={() => this.postExcel()}>
                      {$L('下一步')}
                    </button>
                    <button className="btn btn-link" type="button" onClick={() => this.hide()}>
                      {$L('取消')}
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div className="tab-pane" id="IMPORTS">
              <InitModels ref={(c) => (this._InitModels = c)} />
              <div className="dialog-footer">
                <div className="float-right">
                  <button className="btn btn-primary J_rbstore" onClick={() => this.postRbImports()}>
                    {$L('开始导入')}
                  </button>
                </div>
                <div className="float-right">
                  <p className="protips mt-2 pr-2">{$L('可在导入后根据自身需求做适当调整/修改')}</p>
                </div>
                <div className="clearfix" />
              </div>
            </div>
            <div className="mt-2 text-right hide">
              <a href="https://getrebuild.com/market/go/1220-rb-store" className="link" target="_blank">
                {$L('提交数据到 RB 仓库')}
              </a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    $(this._$container).find('[data-toggle="tooltip"]').tooltip()

    $.get('/admin/entity/entity-list?detail=true', (res) => {
      this.setState({ entities: res.data }, () => {
        const ps = {
          allowClear: false,
          matcher: $select2MatcherAll,
        }
        $(this._$mainEntity).select2(ps)
        $(this._$copySourceEntity)
          .select2(ps)
          .on('change', (e) => {
            const val = e.currentTarget.value
            const found = this.state.entities.find((x) => x.entityName === val)
            this.setState({ copyHasDetail: found && !!found.detailEntity })
          })
      })
    })

    $createUploader(
      this._$uploadfile,
      (res) => {
        $(this._$uploadbtn).text(`${$L('上传中')} ... ${res.percent.toFixed(0)}%`)
      },
      (res) => {
        this.setState({ excelfile: res.key })
        $(this._$uploadbtn).text($L('上传文件'))
      }
    )
    $(this._$uploadbtn).on('click', () => this._$uploadfile.click())
  }

  postNew() {
    const data = {
      label: $val(this._$entityLabel),
      comments: $val(this._$comments),
    }
    if (!data.label) return RbHighbar.create($L('请输入实体名称'))

    if (this.state.newIsDetail) {
      data.mainEntity = $val(this._$mainEntity)
      if (!data.mainEntity) return RbHighbar.create($L('请选择主实体'))
    }

    const $btn = $(this._$container).find('.J_new').button('loading')
    $.post(`/admin/entity/entity-new?nameField=${$val(this._$nameField)}&seriesField=${$val(this._$seriesField)}`, JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        location.href = `${rb.baseUrl}/admin/entity/${res.data}/base`
      } else {
        $btn.button('reset')
        RbHighbar.error(res.error_msg)
      }
    })
  }

  postCopy() {
    const data = {
      sourceEntity: $val(this._$copySourceEntity),
      entityName: $val(this._$copyEntityLabel),
      detailEntityName: $val(this._$copyDetailLabel),
      keepConfig: [],
    }
    if (!data.sourceEntity) return RbHighbar.create($L('请选择从哪个实体复制'))
    if (!data.entityName) return RbHighbar.create($L('请输入实体名称'))

    const $btn = $(this._$container).find('.J_copy').button('loading')
    $.post('/admin/entity/entity-copy', JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        location.href = `${rb.baseUrl}/admin/entity/${res.data}/base`
      } else {
        $btn.button('reset')
        RbHighbar.error(res.error_msg)
      }
    })
  }

  postExcel() {
    $.get(`/app/entity/data-imports/check-file?file=${$encode(this.state.excelfile)}`, (res) => {
      if (res.error_code > 0) {
        this.setState({ excelfile: null })
        RbHighbar.create(res.error_msg)
        return
      }

      const _data = res.data
      if (_data.preview.length < 1 || _data.preview[0].length === 0) {
        RbHighbar.create($L('上传的文件无有效数据'))
        return
      }

      renderRbcomp(<ExcelPreview datas={_data.preview} title={$L('确认导入字段')} maximize disposeOnHide useWhite />)
    })
  }

  postRbImports() {
    const s = this._InitModels.getSelected()
    if (s.length < 1) return RbHighbar.create($L('请选择要导入的实体'))

    const $btn = $(this._$container).find('.J_rbstore').button('loading')
    $mp.start()
    $.post(`/admin/metadata/imports?key=${s.join(',')}`, (res) => {
      $mp.end()
      $btn.button('reset')

      if (res.error_code === 0) {
        RbHighbar.success($L('导入成功'))
        setTimeout(() => location.reload(), 1500)
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

const _LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')
class ExcelPreview extends RbModal {
  renderContent() {
    let colMax = 0
    this.props.datas.forEach((d) => {
      colMax = Math.max(colMax, d.length)
    })

    const colNames = []
    for (let i = 0; i < colMax; i++) {
      let L = _LETTERS[i]
      if (i > 25) L = `A${_LETTERS[i - 26] || 'X'}` // AA
      if (i > 51) L = `B${_LETTERS[i - 52] || 'X'}` // BA
      colNames.push(L)
    }

    const ftKeys = Object.keys(FIELD_TYPES)
    const dataHead = this.props.datas[0]

    return (
      <div className="modal-body m-0 p-0">
        <div style={{ overflow: 'auto' }}>
          <table className="table table-bordered table-excel m-0">
            <thead>
              <tr>
                <th></th>
                {colNames.map((item) => {
                  return <th key={item}>{item}</th>
                })}
              </tr>
            </thead>
            <tbody>
              <tr>
                <th>1</th>
                {colNames.map((item, idx) => {
                  return (
                    <td key={item}>
                      <div>
                        <label className="mb-0">{$L('字段名称')}</label>
                        <input className="form-control form-control-sm" defaultValue={dataHead[idx]} />
                      </div>
                      <div className="mt-1">
                        <label className="mb-0">{$L('字段类型')}</label>
                        <select className="form-control form-control-sm" defaultValue={$empty(dataHead[idx]) ? '-' : 'TEXT'}>
                          <option value="-">{$L('不导入')}</option>
                          {ftKeys.map((item) => {
                            if (FIELD_TYPES[item][2]) return null
                            return (
                              <option key={item} value={item}>
                                {FIELD_TYPES[item][0]}
                              </option>
                            )
                          })}
                        </select>
                      </div>
                    </td>
                  )
                })}
              </tr>
              {this.props.datas.map((d, idx) => {
                if (idx === 0) return null
                return (
                  <tr key={idx}>
                    <th>{idx + 1}</th>
                    {colNames.map((item, idx2) => {
                      return (
                        <td key={`${idx}-${idx2}`} className="text-break">
                          {d[idx2] || ''}
                        </td>
                      )
                    })}
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </div>
    )
  }
}
