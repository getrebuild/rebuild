/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global UserSelectorWithField */

// ~~ 自动分派
// eslint-disable-next-line
class ContentAutoAssign extends ActionContentSpec {
  static = { ...this.props, assignRule: 1 }

  render() {
    return (
      <div className="auto-assign">
        <form className="simple">
          <div className="form-group row pt-1">
            <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('分派给谁')}</label>
            <div className="col-12 col-lg-8">
              <UserSelectorWithField ref={(c) => (this._assignTo = c)} />
            </div>
          </div>

          <div className="form-group row pb-1">
            <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('(多人) 分派规则')}</label>
            <div className="col-12 col-lg-8 pt-1" ref={(c) => (this._assignRule = c)}>
              <label className="custom-control custom-control-sm custom-radio custom-control-inline">
                <input className="custom-control-input" name="assignRule" type="radio" value="1" onClick={(e) => this.changeValue(e)} defaultChecked />
                <span className="custom-control-label">{$L('依次平均分派')}</span>
              </label>
              <label className="custom-control custom-control-sm custom-radio custom-control-inline">
                <input className="custom-control-input" name="assignRule" type="radio" value="2" onClick={(e) => this.changeValue(e)} />
                <span className="custom-control-label">{$L('随机分派')}</span>
              </label>
            </div>
          </div>

          <div className="form-group row pb-1">
            <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('同时分派关联记录')}</label>
            <div className="col-12 col-lg-8">
              <div className="entity-select">
                <select className="form-control form-control-sm" ref={(c) => (this._cascades = c)}>
                  {(this.state.cascadesEntity || []).map((item) => {
                    return (
                      <option key={'option-' + item[0]} value={item[0]}>
                        {item[1]}
                      </option>
                    )
                  })}
                </select>
              </div>
            </div>
          </div>
        </form>
      </div>
    )
  }

  componentDidMount() {
    // eslint-disable-next-line no-undef
    disableWhen(2, 16)

    const content = this.props.content || {}

    if (content.assignTo) {
      $.post(`/commons/search/user-selector?entity=${this.props.sourceEntity}`, JSON.stringify(content.assignTo), (res) => {
        if (res.error_code === 0 && res.data.length > 0) this._assignTo.setState({ selected: res.data })
      })
    }

    if (content.assignRule === 2) {
      $(this._assignRule).find('input:eq(1)').prop('checked', true)
    }

    const cascades = content.cascades ? content.cascades.split(',') : []
    $.get(`/commons/metadata/references?entity=${this.props.sourceEntity}`, (res) => {
      this.setState({ cascadesEntity: res.data }, () => {
        this.__select2 = $(this._cascades)
          .select2({
            multiple: true,
            placeholder: `${$L('选择关联实体')} ${$L('(可选)')}`,
          })
          .val(cascades.length === 0 ? null : cascades)
          .trigger('change')
      })
    })
  }

  changeValue = (e) => {
    const s = {}
    s[e.target.name] = e.target.value
    this.setState(s)
  }

  buildContent() {
    const _data = {
      assignTo: this._assignTo.getSelected(),
      assignRule: ~~this.state.assignRule,
      cascades: this.__select2.val().join(','),
    }
    if (!_data.assignTo || _data.assignTo.length === 0) {
      RbHighbar.create($L('请选择分派给谁'))
      return false
    }
    return _data
  }
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  renderRbcomp(<ContentAutoAssign {...props} />, 'react-content', function () {
    // eslint-disable-next-line no-undef
    contentComp = this
  })
}
