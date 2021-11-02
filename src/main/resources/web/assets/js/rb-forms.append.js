/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
// 表单附加操作，可在其他页面独立引入

// 分类数据选择
// eslint-disable-next-line no-unused-vars
class ClassificationSelector extends React.Component {
  constructor(props) {
    super(props)

    this._select = []
    this._select2 = []
    this.state = { openLevel: props.openLevel || 0, datas: [] }
  }

  render() {
    return (
      <div className="modal selector" ref={(c) => (this._dlg = c)} tabIndex="-1">
        <div className="modal-dialog">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <h5 className="mt-0 text-bold">{$L('选择%s', this.props.label)}</h5>
              <div>
                <select ref={(c) => this._select.push(c)} className="form-control form-control-sm">
                  {(this.state.datas[0] || []).map((item) => {
                    return (
                      <option key={'item-' + item[0]} value={item[0]}>
                        {item[1]}
                      </option>
                    )
                  })}
                </select>
              </div>
              {this.state.openLevel >= 1 && (
                <div>
                  <select ref={(c) => this._select.push(c)} className="form-control form-control-sm">
                    {(this.state.datas[1] || []).map((item) => {
                      return (
                        <option key={'item-' + item[0]} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                </div>
              )}
              {this.state.openLevel >= 2 && (
                <div>
                  <select ref={(c) => this._select.push(c)} className="form-control form-control-sm">
                    {(this.state.datas[2] || []).map((item) => {
                      return (
                        <option key={'item-' + item[0]} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                </div>
              )}
              {this.state.openLevel >= 3 && (
                <div>
                  <select ref={(c) => this._select.push(c)} className="form-control form-control-sm">
                    {(this.state.datas[3] || []).map((item) => {
                      return (
                        <option key={'item-' + item[0]} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                </div>
              )}
              <div>
                <button className="btn btn-primary btn-outline w-100" onClick={() => this.confirm()}>
                  <i className="icon zmdi zmdi-check" /> {$L('确定')}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    const $m = this.show()
    if (this.props.keepModalOpen) {
      $m.on('hidden.bs.modal', () => {
        $(document.body).addClass('modal-open') // keep scroll
      })
    }

    const that = this
    $(this._select).each(function (idx) {
      const s = $(this)
        .select2({
          placeholder: $L('选择 %d 级分类', idx + 1),
          allowClear: false,
        })
        .on('change', () => {
          const p = $(s).val()
          if (p) {
            if (s.__level < that.state.openLevel) {
              that._loadData(s.__level + 1, p) // Load next-level
            }
          }
        })
      s.__level = idx
      that._select2.push(s)
    })
    this._loadData(0)
  }

  _loadData(level, p) {
    $.get(`/commons/metadata/classification?entity=${this.props.entity}&field=${this.props.field}&parent=${p || ''}`, (res) => {
      const s = this.state.datas
      s[level] = res.data
      this.setState({ datas: s }, () => this._select2[level].trigger('change'))
    })
  }

  confirm() {
    const last = this._select2[this.state.openLevel]
    const v = last.val()
    if (!v) {
      RbHighbar.create($L('请选择 %s', this.props.label))
    } else {
      const text = []
      $(this._select2).each(function () {
        text.push(this.select2('data')[0].text)
      })

      typeof this.props.onSelect === 'function' && typeof this.props.onSelect({ id: v, text: text.join('.') })
      this.hide()
    }
  }

  show() {
    return $(this._dlg).modal({ show: true, keyboard: true })
  }

  hide(dispose) {
    $(this._dlg).modal('hide')
    if (dispose === true) $unmount($(this._dlg).parent())
  }
}

// eslint-disable-next-line no-unused-vars
window.referenceSearch__call = function (selected) {}
// eslint-disable-next-line no-unused-vars
window.referenceSearch__dlg

// ~~ 引用字段搜索
// see `reference-search.html`
// eslint-disable-next-line no-unused-vars
class ReferenceSearcher extends RbModal {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <div className="modal rbmodal colored-header colored-header-primary" ref={(c) => (this._rbmodal = c)}>
        <div className="modal-dialog modal-xl">
          <div className="modal-content">
            <div className="modal-header modal-header-colored">
              <h3 className="modal-title">{this.props.title || $L('查询')}</h3>
              <button className="close" type="button" onClick={() => this.hide()}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body iframe">
              <iframe src={this.props.url} frameBorder="0" style={{ minHeight: 368 }} />
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount()
    // eslint-disable-next-line no-unused-vars
    window.referenceSearch__dlg = this
  }
}

// 删除确认
// eslint-disable-next-line no-unused-vars
class DeleteConfirm extends RbAlert {
  constructor(props) {
    super(props)
    this.state = { enableCascades: false }
  }

  render() {
    let message = this.props.message
    if (!message) message = this.props.ids ? $L('确认删除选中的 %d 条记录？', this.props.ids.length) : $L('确认删除当前记录吗？')

    return (
      <div className="modal rbalert" ref={(c) => (this._dlg = c)} tabIndex="-1">
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <div className="text-center ml-6 mr-6">
                <div className="text-danger">
                  <span className="modal-main-icon zmdi zmdi-alert-triangle" />
                </div>
                <div className="mt-3 text-bold">{message}</div>
                {!this.props.entity ? null : (
                  <div className="mt-2">
                    <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-2">
                      <input className="custom-control-input" type="checkbox" checked={this.state.enableCascade === true} onChange={() => this.enableCascade()} />
                      <span className="custom-control-label"> {$L('同时删除关联记录')}</span>
                    </label>
                    <div className={this.state.enableCascade ? '' : 'hide'}>
                      <select className="form-control form-control-sm" ref={(c) => (this._cascades = c)} multiple>
                        {(this.state.cascadesEntity || []).map((item) => {
                          return (
                            <option key={`opt-${item[0]}`} value={item[0]}>
                              {item[1]}
                            </option>
                          )
                        })}
                      </select>
                    </div>
                  </div>
                )}
                <div className="mt-4 mb-3" ref={(c) => (this._btns = c)}>
                  <button className="btn btn-space btn-secondary" type="button" onClick={() => this.hide()}>
                    {$L('取消')}
                  </button>
                  <button className="btn btn-space btn-danger" type="button" onClick={() => this.handleDelete()}>
                    {$L('删除')}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  enableCascade() {
    this.setState({ enableCascade: !this.state.enableCascade })
    if (!this.state.cascadesEntity) {
      $.get(`/commons/metadata/references?entity=${this.props.entity}&permission=D`, (res) => {
        this.setState({ cascadesEntity: res.data }, () => {
          this.__select2 = $(this._cascades)
            .select2({
              placeholder: $L('选择关联实体 (可选)'),
              width: '88%',
            })
            .val(null)
            .trigger('change')
        })
      })
    }
  }

  handleDelete() {
    let ids = this.props.ids || this.props.id
    if (!ids || ids.length === 0) return
    if (typeof ids === 'object') ids = ids.join(',')
    const cascades = this.__select2 ? this.__select2.val().join(',') : ''

    const $btns = $(this._btns).find('.btn').button('loading')
    $.post(`/app/entity/record-delete?id=${ids}&cascades=${cascades}`, (res) => {
      if (res.error_code === 0) {
        if (res.data.deleted === res.data.requests) RbHighbar.success($L('删除成功'))
        else if (res.data.deleted === 0) RbHighbar.error($L('无法删除选中记录'))
        else RbHighbar.success($L('成功删除 %d 条记录', res.data.deleted))

        this.hide()
        typeof this.props.deleteAfter === 'function' && this.props.deleteAfter()
      } else {
        RbHighbar.error(res.error_msg)
        $btns.button('reset')
      }
    })
  }
}
