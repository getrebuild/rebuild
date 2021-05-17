/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-undef */

const wpc = window.__PageConfig

$(document).ready(function () {
  renderRbcomp(<LevelBoxes id={wpc.id} />, 'boxes')

  let _dlgImports
  $('.J_imports').click(() => {
    if (_dlgImports) {
      _dlgImports.show()
    } else {
      renderRbcomp(<DlgImports id={wpc.id} />, null, function () {
        _dlgImports = this
      })
    }
  })

  $addResizeHandler(() => $('#boxes .rb-scroller').css('max-height', $(window).height() - 310))()
})

class LevelBoxes extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
    this.boxes = []
  }

  render() {
    return (
      <div className="row level-boxes">
        <LevelBox level={0} $$$parent={this} ref={(c) => (this.boxes[0] = c)} />
        <LevelBox level={1} $$$parent={this} ref={(c) => (this.boxes[1] = c)} />
        <LevelBox level={2} $$$parent={this} ref={(c) => (this.boxes[2] = c)} />
        <LevelBox level={3} $$$parent={this} ref={(c) => (this.boxes[3] = c)} />
      </div>
    )
  }

  componentDidMount() {
    this.notifyToggle(wpc.openLevel + 1, true)
    $('#boxes').removeClass('rb-loading-active')
    $addResizeHandler()()
    $('#boxes .rb-scroller').perfectScrollbar()
  }

  notifyItemActive(level, id) {
    if (level < 3) {
      this.boxes[level + 1].loadItems(id)
      for (let i = level + 2; i <= 3; i++) {
        this.boxes[i].clear(true)
      }
    }
  }

  notifyItemClean(level) {
    for (let i = level + 1; i <= 3; i++) {
      this.boxes[i].clear(true)
    }
  }

  notifyToggle(level, c) {
    const e = { target: { checked: c } }
    if (c === true) {
      for (let i = 1; i < level; i++) {
        this.boxes[i].turnToggle(e, true)
      }
    } else {
      for (let i = level + 1; i <= 3; i++) {
        this.boxes[i].turnToggle(e, true)
      }
    }
  }
}

class LevelBox extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, turnOn: props.level === 0 }
  }

  render() {
    const forId = 'turnOn-' + this.props.level
    return (
      <div className={`col-md-3 ${this.state.turnOn ? '' : 'off'}`}>
        <div className="float-left">
          <h5 className="text-bold">{$L('XLevelClass').replace('%d', ~~this.props.level + 1 + ' ')}</h5>
        </div>
        {this.props.level < 1 ? null : (
          <div className="float-right">
            <div className="switch-button switch-button-xs">
              <input type="checkbox" id={forId} onChange={this.turnToggle} checked={this.state.turnOn} />
              <span>
                <label htmlFor={forId} title={$L('EnableOrDisable')}></label>
              </span>
            </div>
          </div>
        )}
        <div className="clearfix"></div>
        <form className="mt-1" onSubmit={this.saveItem}>
          <div className="input-group input-group-sm">
            <input className="form-control" type="text" maxLength="60" placeholder={$L('AddSome,ClassItem')} value={this.state.itemName || ''} data-id="itemName" onChange={this.changeVal} />
            {this.state.itemId && this.state.itemHide && (
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline">
                <input className="custom-control-input" type="checkbox" data-id="itemUnhide" onChange={this.changeVal} />
                <span className="custom-control-label">{$L('Enable')}</span>
              </label>
            )}
            <div className="input-group-append">
              <button className="btn btn-primary" type="submit" disabled={this.state.inSave === true}>
                {this.state.itemId ? $L('保存') : $L('添加')}
              </button>
            </div>
          </div>
        </form>
        <div className="rb-scroller mt-3">
          <ol className="dd-list unset-list" _title={$L('暂无分类项')}>
            {(this.state.items || []).map((item) => {
              const active = this.state.activeId === item[0]
              return (
                <li className={`dd-item ${active ? ' active' : ''}`} key={item[0]} onClick={() => this.clickItem(item[0])}>
                  <div className={`dd-handle ${item[3] ? ' text-disabled' : ''}`} title={item[3] ? $L('已禁用') : null}>
                    {item[1]}
                    {item[3] && <small />}
                  </div>
                  <div className="dd-action">
                    <a>
                      <i className="zmdi zmdi-edit" title={$L('修改')} onClick={(e) => this.editItem(item, e)}></i>
                    </a>
                    <a>
                      <i className="zmdi zmdi-delete" title={$L('删除')} onClick={(e) => this.delItem(item, e)}></i>
                    </a>
                  </div>
                  {active && <span className="zmdi zmdi-caret-right arrow hide"></span>}
                </li>
              )
            })}
          </ol>
        </div>
      </div>
    )
  }

  componentDidMount() {
    if (this.props.level === 0) this.loadItems()
  }

  loadItems(p) {
    this.parentId = p
    $.get(`/admin/metadata/classification/load-data-items?data_id=${wpc.id}&parent=${p || ''}`, (res) => {
      this.clear()
      this.setState({ items: res.data, activeId: null })
    })
  }

  clickItem(id) {
    this.setState({ activeId: id })
    this.props.$$$parent.notifyItemActive(this.props.level, id)
  }

  turnToggle = (e, stop) => {
    const c = e.target.checked
    this.setState({ turnOn: c })
    if (stop !== true) {
      const that = this
      RbAlert.create($L('改变分类级别可能会影响现有数据。是否继续？'), {
        confirm: function () {
          that.props.$$$parent.notifyToggle(that.props.level, c)
          saveOpenLevel()
          this.hide()
        },
        cancel: function () {
          that.setState({ turnOn: !c })
          this.hide()
        },
      })
    }
  }

  changeVal = (e) => {
    const s = {}
    s[e.target.dataset.id] = e.target.value
    this.setState(s)
  }

  saveItem = (e) => {
    e.preventDefault()
    const name = $.trim(this.state.itemName)
    if (!name) return
    if (this.props.level >= 1 && !this.parentId) return RbHighbar.create($L('请先选择上级分类项'))

    const repeated = this.state.items.find((x) => {
      return x[1] === name && x[0] !== this.state.itemId
    })
    if (repeated) return RbHighbar.create($L('分类项重复'))

    let url = `/admin/metadata/classification/save-data-item?data_id=${wpc.id}&name=${name}`
    if (this.state.itemId) url += `&item_id=${this.state.itemId}`
    else url += `&parent=${this.parentId}&level=${this.props.level}`
    let isUnhide = null
    if (this.state.itemHide && this.state.itemUnhide) {
      url += '&hide=false'
      isUnhide = true
    }

    this.setState({ inSave: true })
    $.post(url, (res) => {
      if (res.error_code === 0) {
        const items = this.state.items || []
        if (this.state.itemId) {
          items.forEach((i) => {
            if (i[0] === this.state.itemId) {
              i[1] = name
              if (isUnhide === true) i[3] = false
            }
          })
        } else {
          items.insert(0, [res.data, name, null, false])
        }
        this.setState({ items: items, itemName: null, itemId: null, inSave: false })
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }

  editItem(item, e) {
    e.stopPropagation()
    this.setState({ itemName: item[1], itemId: item[0], itemHide: item[3] })
  }

  delItem(item, e) {
    e.stopPropagation()

    const that = this

    let alertMsg = $L('删除后其子分类项也将被一并删除。[] 如果此分类项已被使用，使用了这些分类项的字段也将无法显示。确认删除吗？')
    const alertExt = {
      type: 'danger',
      confirm: function () {
        this.disabled()
        $.post(`/admin/metadata/classification/delete-data-item?item_id=${item[0]}`, (res) => {
          this.hide()
          if (res.error_code !== 0) {
            RbHighbar.error(res.error_msg)
            return
          }

          RbHighbar.success($L('分类项已删除'))
          const ns = []
          that.state.items.forEach((i) => {
            if (i[0] !== item[0]) ns.push(i)
          })
          that.setState({ items: ns }, () => {
            that.props.$$$parent.notifyItemClean(that.props.level)
          })
        })
        return false
      },
    }

    if (item[3] !== true) {
      alertMsg = $L('DeleteClassOptionConfirm2')
      alertExt.confirmText = $L('删除')
      alertExt.cancelText = $L('Disable')
      alertExt.cancel = function () {
        this.disabled()
        const url = `/admin/metadata/classification/save-data-item?item_id=${item[0]}&hide=true`
        $.post(url, (res) => {
          this.hide()
          if (res.error_code !== 0) {
            RbHighbar.error(res.error_msg)
            return
          }

          RbHighbar.success($L('SomeDisabled,ClassItem'))
          const ns = []
          $(that.state.items || []).each(function () {
            if (this[0] === item[0]) this[3] = true
            ns.push(this)
          })
          that.setState({ items: ns })
        })
      }
    }

    alertExt.html = true
    RbAlert.create(alertMsg, alertExt)
  }

  clear(isAll) {
    if (isAll === true) this.parentId = null
    this.setState({ items: [], itemId: null, itemName: null, activeId: null })
  }
}

let saveOpenLevel_last = wpc.openLevel
const saveOpenLevel = function () {
  $setTimeout(
    () => {
      const level = ~~($('.switch-button input:checkbox:checked:last').attr('id') || 't-0').split('-')[1]
      if (saveOpenLevel_last === level) return

      const data = {
        openLevel: level,
        metadata: {
          entity: 'Classification',
          id: wpc.id,
        },
      }

      $.post('/app/entity/common-save', JSON.stringify(data), (res) => {
        if (res.error_code > 0) {
          RbHighbar.error(res.error_msg)
        } else {
          saveOpenLevel_last = level
          RbHighbar.success($L('EnabledXLevelClass').replace('%d', level + 1))
        }
      })
    },
    500,
    'saveOpenLevel'
  )
}

// 导入
class DlgImports extends RbModalHandler {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <RbModal title={$L('ImportSome,Classification')} ref={(c) => (this._dlg = c)}>
        <div className="tab-container">
          <ul className="nav nav-tabs">
            <li className="nav-item">
              <a className="nav-link active" href="#FILE" data-toggle="tab">
                {$L('FileImport')}
              </a>
            </li>
            <li className="nav-item">
              <a className="nav-link" href="#RBSTORE" data-toggle="tab">
                <i className="icon zmdi zmdi-cloud-outline-alt"></i>
                {$L('RbImport')}
              </a>
            </li>
          </ul>
          <div className="tab-content m-0 pb-0">
            <div className="tab-pane active" id="FILE">
              <div className="form">
                <div className="form-group row">
                  <label className="col-sm-3 col-form-label text-sm-right">{$L('UploadSome,File')}</label>
                  <div className="col-sm-7">
                    <div className="float-left">
                      <div className="file-select">
                        <input type="file" className="inputfile" id="upload-input" accept=".xlsx,.xls,.csv" data-local="temp" ref={(c) => (this._uploadInput = c)} />
                        <label htmlFor="upload-input" className="btn-secondary">
                          <i className="zmdi zmdi-upload"></i>
                          <span>{$L('选择,File')}</span>
                        </label>
                      </div>
                    </div>
                    <div className="float-left ml-2 pt-1">
                      <u className="text-bold">{$fileCutName(this.state.uploadFile || '')}</u>
                    </div>
                    <div className="clearfix"></div>
                    <div className="form-text link" dangerouslySetInnerHTML={{ __html: $L('ImportClassDataTips') }}></div>
                  </div>
                </div>
                <div className="form-group row footer">
                  <div className="col-sm-7 offset-sm-3">
                    <button className="btn btn-primary" type="button" onClick={() => this.import4File()} disabled={this.state.inProgress}>
                      {$L('StartImport')}
                    </button>
                    <button className="btn btn-link" type="button" onClick={() => this._dlg.hide()} disabled={this.state.inProgress}>
                      {$L('取消')}
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div className="tab-pane" id="RBSTORE">
              <div className="rbs-indexes">
                {(this.state.indexes || []).map((item) => {
                  return (
                    <div key={'data-' + item.file}>
                      <div className="float-left">
                        <h5>{item.name}</h5>
                        <div className="text-muted">
                          {$L('DataSource')}{' '}
                          <a target="_blank" className="link" rel="noopener noreferrer" href={item.source}>
                            {item.author || item.source}
                          </a>
                          {item.updated && ' · ' + item.updated}
                        </div>
                      </div>
                      <div className="float-right">
                        <button disabled={this.state.inProgress === true} className="btn btn-sm btn-primary" data-file={item.file} data-name={item.name} onClick={this.import4Rbstore}>
                          {$L('Import')}
                        </button>
                      </div>
                      <div className="clearfix"></div>
                    </div>
                  )
                })}
              </div>
              <div className="mt-2 mr-2 text-right">
                <a href="https://github.com/getrebuild/rebuild-datas/" className="link" target="_blank" rel="noopener noreferrer">
                  {$L('RbSubmit')}
                </a>
              </div>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    // FILE
    let uploadStart = false
    $createUploader(
      this._uploadInput,
      () => {
        if (!uploadStart) {
          uploadStart = true
          $mp.start()
        }
      },
      (res) => {
        this.setState({ uploadFile: res.key })
        $mp.end()
        uploadStart = false
      }
    )

    // RBSTORE
    $.get('/admin/rbstore/load-index?type=classifications', (res) => {
      if (res.error_code === 0) this.setState({ indexes: res.data })
      else RbHighbar.error(res.error_msg)
    })
  }

  import4File() {
    if (!this.state.uploadFile) {
      RbHighbar.create($L('PlsUploadFile'))
      return
    }

    this.setState({ inProgress: true })
    const url = `/admin/metadata/classification/imports/file?dest=${this.props.id}&file=${$encode(this.state.uploadFile)}`
    $.post(url, (res) => {
      if (res.error_code === 0) this.__checkState(res.data)
      else RbHighbar.error(res.error_msg)
    })
  }

  import4Rbstore = (e) => {
    const file = e.currentTarget.dataset.file
    const name = e.currentTarget.dataset.name
    const url = `/admin/metadata/classification/imports/start?dest=${this.props.id}&file=${$encode(file)}`
    const that = this
    RbAlert.create(`<strong>${name}</strong><br>${$L('ImportClassDataConfirm')}`, {
      html: true,
      confirm: function () {
        this.hide()
        that.setState({ inProgress: true })
        $.post(url, (res) => {
          if (res.error_code === 0) that.__checkState(res.data)
          else RbHighbar.error(res.error_msg)
        })
      },
    })
  }

  __checkState(taskid) {
    if (!this.__mp) this.__mp = new Mprogress({ template: 1, start: true })

    $.get(`/commons/task/state?taskid=${taskid}`, (res) => {
      if (res.error_code === 0) {
        if (res.data.hasError) {
          this.__mp.end()
          RbHighbar.error(res.data.hasError)
          return
        }

        const cp = res.data.progress
        if (cp >= 1) {
          RbHighbar.success($L('SomeSuccess,Import'))
          this.__mp.end()
          setTimeout(() => location.reload(), 1500)
        } else {
          this.__mp.set(cp)
          setTimeout(() => this.__checkState(taskid), 1000)
        }
      }
    })
  }
}
