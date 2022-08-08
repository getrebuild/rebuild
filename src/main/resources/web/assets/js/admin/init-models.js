/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// 初始导入实体

// eslint-disable-next-line no-unused-vars
class InitModels extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
    this._$refs = {}
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="mb-7">
          <RbAlertBox message={this.state.hasError} />
        </div>
      )
    } else if (!this.state.data) {
      return null
    }

    const types = this.state.data.types
    const schemas = this.state.data.schemas
    const _find = (s) => {
      return schemas.find((x) => s === x.key)
    }

    return (
      <div className="init-models">
        {Object.keys(types).map((t) => {
          return (
            <fieldset key={t}>
              <legend>
                <div className="row">
                  <div className="col">
                    <strong>{t}</strong>
                  </div>
                  <div className="col text-right">
                    <label className="custom-control custom-checkbox custom-control-inline custom-control-sm" title={$L('全选')}>
                      <input className="custom-control-input" type="checkbox" onClick={(e) => this._handleSelectAll(e)} />
                      <span className="custom-control-label" />
                    </label>
                  </div>
                </div>
              </legend>
              <form>
                {types[t].map((s) => {
                  const item = _find(s)
                  return (
                    <div key={item.key}>
                      <label className="custom-control custom-checkbox m-0" title={item.desc} ref={(c) => (this._$refs[item.key] = c)}>
                        <input className="custom-control-input" type="checkbox" value={item.key} data-refs={item.refs} onClick={(e) => this._handleSelect(e)} disabled={item.exists} />
                        <span className="custom-control-label text-bold">
                          {item.name}
                          {item.rbv && <i className="zmdi zmdi-info-outline text-warning zicon ml-1 J_rbv-tip" title={$L('部分功能可能需要商业版才能正常运行')} />}
                        </span>
                        <p>{item.desc}</p>
                      </label>
                    </div>
                  )
                })}
              </form>
              <div className="clearfix" />
            </fieldset>
          )
        })}
      </div>
    )
  }

  componentDidMount() {
    $.get(rb.isAdminVerified ? '/admin/rbstore/load-metaschemas' : '/setup/init-models', (res) => {
      const s = {}
      if (res.error_code > 0) s.hasError = $L('暂无可用业务实体')
      else s.data = res.data
      this.setState(s, () => {
        $('i.J_rbv-tip').tooltip()
        typeof this.props.onLoad === 'function' && this.props.onLoad()
      })
    })
  }

  _handleSelect(e) {
    const $el = $(e.currentTarget)
    if (!$el.prop('checked')) return

    const refs = ($el.data('refs') || '').split(',')
    refs.forEach((s) => {
      const $chk = $(this._$refs[s]).find('input')
      if ($chk.attr('disabled')) return
      $chk.prop('checked', true)
    })
  }

  _handleSelectAll(e) {
    const chk = $(e.currentTarget).prop('checked')
    $(e.currentTarget)
      .parents('legend')
      .next()
      .find('input')
      .each(function () {
        const $chk = $(this)
        if ($chk.attr('disabled')) return
        $(this).prop('checked', chk)
      })
  }

  getSelected() {
    const ss = []
    for (let k in this._$refs) {
      const $chk = $(this._$refs[k]).find('input')
      if ($chk.prop('checked')) ss.push($chk.val())
    }
    return ss
  }
}
