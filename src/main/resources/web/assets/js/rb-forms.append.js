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
              <h5 className="mt-0 text-bold">{$L('SelectSome').replace('{0}', this.props.label)}</h5>
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
                <button className="btn btn-primary w-100" onClick={() => this.confirm()}>
                  {$L('Confirm')}
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
          placeholder: $L('SelectSome').replace('{0}', $L('XLevelClass').replace('%d', idx + 1)),
          allowClear: false,
        })
        .on('change', () => {
          const p = $(s).val()
          if (p) {
            if (s.__level < that.state.openLevel) {
              that.loadData(s.__level + 1, p) // Load next-level
            }
          }
        })
      s.__level = idx
      that._select2.push(s)
    })
    this.loadData(0)
  }

  loadData(level, p) {
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
      RbHighbar.create($L('PlsSelectSome').replace('{0}', this.props.label))
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
        <div className="modal-dialog modal-lg">
          <div className="modal-content">
            <div className="modal-header modal-header-colored">
              <h3 className="modal-title">{this.props.title || $L('Query')}</h3>
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
