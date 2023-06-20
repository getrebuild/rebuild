/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-undef */

const wpc = window.__PageConfig

$(document).ready(function () {
  renderRbcomp(<LevelBoxes id={wpc.id} />, 'boxes')

  let _dlgImports
  $('.J_imports').on('click', () => {
    if (_dlgImports) {
      _dlgImports.show()
    } else {
      renderRbcomp(<DlgImports id={wpc.id} />, null, function () {
        _dlgImports = this
      })
    }
  })

  $addResizeHandler(() => $('#boxes .rb-scroller').css('max-height', $(window).height() - 290))()
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
    const forId = `turnOn-${this.props.level}`
    return (
      <div className={`col-md-3 ${this.state.turnOn ? '' : 'off'}`}>
        <div className="float-left">
          <h5 className="text-bold">{$L('%d 级分类', ~~this.props.level + 1)}</h5>
        </div>
        {this.props.level < 1 ? null : (
          <div className="float-right">
            <div className="switch-button switch-button-xs">
              <input type="checkbox" id={forId} onChange={this.turnToggle} checked={this.state.turnOn} />
              <span>
                <label htmlFor={forId} title={$L('启用/禁用')} />
              </span>
            </div>
          </div>
        )}
        <div className="clearfix" />
        <form className="mt-1" onSubmit={this.saveItem}>
          <div className="input-group input-group-sm">
            <input className="form-control" type="text" maxLength="50" placeholder={$L('添加分类项')} value={this.state.itemName || ''} data-id="itemName" onChange={this.changeVal} />
            <div className="input-group-append">
              <button className="btn btn-primary" type="submit" disabled={this.state.inSave === true}>
                {$L('添加')}
              </button>
            </div>
          </div>
        </form>
        <div className="rb-scroller mt-3">
          <ol className="dd-list unset-list" _title={$L('暂无分类项')}>
            {(this.state.items || []).map((item) => {
              const active = this.state.activeId === item[0]
              return (
                <li className={`dd-item ${active && 'active'}`} key={item[0]} onClick={() => this.clickItem(item[0])} data-key={item[0]}>
                  <div className={`dd-handle ${item[3] && 'text-disabled'}`} title={item[3] ? $L('已禁用') : null}>
                    {item[1]}
                    {item[3] && <small />}
                  </div>
                  <div className="dd-action">
                    <a>
                      <i className="zmdi zmdi-edit" title={$L('修改')} onClick={(e) => this.editItem(item, e)} />
                    </a>
                    <a className="danger-hover">
                      <i className="zmdi zmdi-delete" title={$L('删除')} onClick={(e) => this.delItem(item, e)} />
                    </a>
                  </div>
                  {active && <span className="zmdi zmdi-caret-right arrow hide" />}
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

  saveItem = (e, _data) => {
    $stopEvent(e, true)
    _data = _data || this.state

    const name = $.trim(_data.itemName)
    if (!name) return
    if (this.props.level >= 1 && !this.parentId) return RbHighbar.create($L('请先选择上级分类项'))

    const repeated = this.state.items.find((x) => {
      return x[1] === name && x[0] !== _data.itemId
    })
    if (repeated) return RbHighbar.create($L('分类项重复'))

    let url = `/admin/metadata/classification/save-data-item?data_id=${wpc.id}&name=${$encode(name)}&hide=${!!_data.itemHide}`
    if (_data.itemId) url += `&item_id=${_data.itemId}`
    else url += `&parent=${this.parentId || ''}&level=${this.props.level}`

    // 修改时
    if (typeof _data.itemCode !== 'undefined') url += `&code=${$encode(_data.itemCode || '')}`

    this.setState({ inSave: true })
    $.post(url, (res) => {
      if (res.error_code === 0) {
        const items = this.state.items || []
        if (_data.itemId) {
          items.forEach((x) => {
            if (x[0] === _data.itemId) {
              x[1] = name
              if (typeof _data.itemCode !== 'undefined') x[2] = _data.itemCode
              x[3] = _data.itemHide || false
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
    $stopEvent(e, true)
    renderRbcomp(
      <DlgEditItem
        id={item[0]}
        name={item[1]}
        code={item[2]}
        hide={item[3]}
        onConfirm={(s) => {
          this.saveItem(null, s)
        }}
      />
    )
  }

  delItem(item, e) {
    e.stopPropagation()
    const that = this
    let alertMsg = WrapHtml($L('删除后子分类也将被一并删除。[] 如果此分类项已被使用，使用了这些分类项的字段也将无法显示。确认删除吗？'))
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
      alertMsg = WrapHtml($L('删除后子分类也将被一并删除。[] 如果此分类项已被使用，建议你禁用，否则已使用这些分类项的字段将无法显示。'))
      alertExt.confirmText = $L('删除')
      alertExt.cancelText = $L('禁用')
      alertExt.cancel = function () {
        this.disabled()
        $.post(`/admin/metadata/classification/save-data-item?item_id=${item[0]}&hide2=true`, (res) => {
          this.hide()
          if (res.error_code !== 0) {
            RbHighbar.error(res.error_msg)
            return
          }

          RbHighbar.success($L('分类项已禁用'))
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
          RbHighbar.success($L('已启用 %d 级分类', level + 1))
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
      <RbModal title={$L('导入分类数据')} ref={(c) => (this._dlg = c)}>
        <div className="tab-container">
          <ul className="nav nav-tabs">
            <li className="nav-item">
              <a className="nav-link active" href="#FILE" data-toggle="tab">
                {$L('文件导入')}
              </a>
            </li>
            <li className="nav-item">
              <a className="nav-link" href="#RBSTORE" data-toggle="tab">
                <i className="icon zmdi zmdi-cloud-outline-alt" />
                {$L('从 RB 仓库导入')}
              </a>
            </li>
          </ul>
          <div className="tab-content m-0 pb-0">
            <div className="tab-pane active" id="FILE">
              <div className="form">
                <div className="form-group row">
                  <label className="col-sm-3 col-form-label text-sm-right">{$L('上传文件')}</label>
                  <div className="col-sm-7">
                    <div className="float-left">
                      <div className="file-select">
                        <input type="file" className="inputfile" id="upload-input" accept=".xlsx,.xls,.csv" data-local="temp" ref={(c) => (this._uploadInput = c)} />
                        <label htmlFor="upload-input" className="btn-secondary">
                          <i className="zmdi zmdi-upload" />
                          <span>{$L('选择文件')}</span>
                        </label>
                      </div>
                    </div>
                    <div className="float-left ml-2 pt-1">
                      <u className="text-bold">{$fileCutName(this.state.uploadFile || '')}</u>
                    </div>
                    <div className="clearfix" />
                    <div
                      className="form-text link"
                      dangerouslySetInnerHTML={{
                        __html: $L('支持 Excel 或 CSV 文件，文件格式请 [参考文档](https://getrebuild.com/docs/admin/entity/field-classification#%E5%88%86%E7%B1%BB%E6%95%B0%E6%8D%AE)'),
                      }}
                    />
                  </div>
                </div>
                <div className="form-group row footer">
                  <div className="col-sm-7 offset-sm-3">
                    <button className="btn btn-primary" type="button" onClick={() => this.import4File()} disabled={this.state.inProgress}>
                      {$L('开始导入')}
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
                          <a target="_blank" className="link" rel="noopener noreferrer" href={item.source}>
                            {item.author || item.source}
                          </a>
                          {item.updated && ' · ' + item.updated}
                        </div>
                      </div>
                      <div className="float-right">
                        <button disabled={this.state.inProgress === true} className="btn btn-sm btn-primary" data-file={item.file} data-name={item.name} onClick={this.import4Rbstore}>
                          {$L('导入')}
                        </button>
                      </div>
                      <div className="clearfix" />
                    </div>
                  )
                })}
              </div>
              <div className="mt-2 text-right">
                <a href="https://getrebuild.com/market/go/1220-rb-store" className="link" target="_blank" rel="noopener noreferrer">
                  {$L('提交数据到 RB 仓库')}
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
    $createUploader(
      this._uploadInput,
      () => $mp.start(),
      (res) => {
        $mp.end()
        this.setState({ uploadFile: res.key })
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
      RbHighbar.create($L('请上传文件'))
      return
    }

    this.setState({ inProgress: true })
    const url = `/admin/metadata/classification/imports/file?dest=${this.props.id}&file=${$encode(this.state.uploadFile)}`
    $.post(url, (res) => {
      if (res.error_code === 0) this._checkState(res.data)
      else RbHighbar.error(res.error_msg)
    })
  }

  import4Rbstore = (e) => {
    const file = e.currentTarget.dataset.file
    const name = e.currentTarget.dataset.name
    const url = `/admin/metadata/classification/imports/start?dest=${this.props.id}&file=${$encode(file)}`
    const that = this
    RbAlert.create(WrapHtml(`<strong>${name}</strong><br>${$L('此导入为增量导入，不会对现有数据造成影响。开始导入吗？')}`), {
      confirm: function () {
        this.hide()
        that.setState({ inProgress: true })
        $.post(url, (res) => {
          if (res.error_code === 0) that._checkState(res.data)
          else RbHighbar.error(res.error_msg)
        })
      },
    })
  }

  _checkState(taskid) {
    if (!this.__mp) {
      const mp_parent = $(this._dlg._element).find('.modal-body').attr('id')
      this.__mp = new Mprogress({ template: 2, start: true, parent: `#${mp_parent}` })
    }

    $.get(`/commons/task/state?taskid=${taskid}`, (res) => {
      if (res.error_code === 0) {
        if (res.data.hasError) {
          setTimeout(() => {
            if (this.__mp) this.__mp.end()
            this.__mp = null
          }, 510)

          RbHighbar.error(res.data.hasError)
          return
        }

        const cp = res.data.progress
        if (cp >= 1) {
          RbHighbar.success($L('导入成功'))
          this.__mp.end()
          setTimeout(() => location.reload(), 1500)
        } else {
          this.__mp.set(cp)
          setTimeout(() => this._checkState(taskid), 1000)
        }
      }
    })
  }
}

// 编辑分类项
class DlgEditItem extends RbAlert {
  renderContent() {
    return (
      <form className="rbalert-form-sm">
        <div className="form-group">
          <label className="text-bold">{$L('分类项名称')}</label>
          <input type="text" className="form-control form-control-sm" name="name" value={this.state.name || ''} onChange={this.handleChange} maxLength="50" />
        </div>
        <div className="form-group">
          <label className="text-bold">{$L('编码')}</label>
          <input type="text" className="form-control form-control-sm" name="code" value={this.state.code || ''} onChange={this.handleChange} maxLength="50" placeholder={$L('无')} />
        </div>
        <div className="form-group">
          <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mt-0 mb-0">
            <input className="custom-control-input" type="checkbox" name="hide" defaultChecked={this.props.hide} onChange={this.handleChange} />
            <span className="custom-control-label">{$L('是否禁用')}</span>
          </label>
        </div>
        <div className="form-group mb-2">
          <button type="button" className="btn btn-primary" onClick={this._onConfirm}>
            {$L('确定')}
          </button>
        </div>
      </form>
    )
  }

  handleChange = (e) => {
    const target = e.target
    const s = {}
    s[target.name] = target.type === 'checkbox' ? target.checked : target.value
    this.setState(s)
  }

  _onConfirm = () => {
    const _data = {
      itemId: this.props.id,
      itemName: this.state.name,
      itemCode: this.state.code,
      itemHide: this.state.hide,
    }
    typeof this.props.onConfirm === 'function' && this.props.onConfirm(_data)
    this.hide()
  }
}
