/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global UserSelectorWithField */

// ~~ 自动共享
// eslint-disable-next-line
class ContentAutoShare extends ActionContentSpec {
  state = { ...this.props }

  render() {
    return <div className="auto-share">
      <form className="simple">
        <div className="form-group row pt-1">
          <label className="col-12 col-lg-3 col-form-label text-lg-right">共享给谁</label>
          <div className="col-12 col-lg-8">
            <UserSelectorWithField ref={(c) => this._shareTo = c} />
          </div>
        </div>
        <div className="form-group row pb-1">
          <label className="col-12 col-lg-3 col-form-label text-lg-right">同时共享关联记录</label>
          <div className="col-12 col-lg-8">
            <div>
              <select className="form-control form-control-sm" ref={(c) => this._cascades = c}>
                {(this.state.cascadesEntity || []).map((item) => {
                  return <option key={'option-' + item[0]} value={item[0]}>{item[1]}</option>
                })}
              </select>
            </div>
          </div>
        </div>
      </form>
    </div>
  }

  componentDidMount() {
    $('.J_when').find('.custom-control-input').each(function () {
      const v = ~~$(this).val()
      if (!(v === 1 || v === 4 || v >= 128)) $(this).attr('disabled', true)
    })

    const content = this.props.content || {}

    if (content.shareTo) {
      $.post(`/commons/search/user-selector?entity=${this.props.sourceEntity}`, JSON.stringify(content.shareTo), (res) => {
        if (res.error_code === 0 && res.data.length > 0) this._shareTo.setState({ selected: res.data })
      })
    }

    const cascades = content.cascades ? content.cascades.split(',') : []
    $.get(`/commons/metadata/references?entity=${this.props.sourceEntity}`, (res) => {
      this.setState({ cascadesEntity: res.data }, () => {
        this.__select2 = $(this._cascades).select2({
          multiple: true,
          placeholder: '选择关联实体 (可选)'
        }).val(cascades.length === 0 ? null : cascades).trigger('change')
      })
    })
  }

  buildContent() {
    const _data = {
      shareTo: this._shareTo.getSelected(),
      cascades: this.__select2.val().join(',')
    }
    if (!_data.shareTo || _data.shareTo.length === 0) { RbHighbar.create('请选择共享给谁'); return false }
    return _data
  }
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  // eslint-disable-next-line no-undef
  renderRbcomp(<ContentAutoShare {...props} />, 'react-content', function () { contentComp = this })
}