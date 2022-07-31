/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// ~~ 自动审批
// eslint-disable-next-line
class ContentAutoApproval extends ActionContentSpec {
  static = { ...this.props }

  render() {
    return (
      <div className="auto-approval">
        <form className="simple">
          <div className="form-group row pt-1">
            <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('使用审批流程')}</label>
            <div className="col-12 col-lg-8">
              <select className="form-control form-control-sm" ref={(c) => (this._$useApproval = c)}>
                <option value="">{$L('不使用')}</option>
                {(this.state.approvalList || []).map((item) => {
                  return (
                    <option key={item.id} value={item.id} disabled={item.disabled === true}>
                      {item.text}
                    </option>
                  )
                })}
              </select>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-12 col-lg-3 col-form-label text-lg-right" />
            <div className="col-12 col-lg-8">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" ref={(c) => (this._$submitMode = c)} />
                <span className="custom-control-label">{$L('提交模式')}</span>
              </label>
              <p className="form-text">{$L('仅提交不做自动审批。选择的审批流程至少配置一个审批人，否则会提交失败')}</p>
            </div>
          </div>
        </form>
      </div>
    )
  }

  componentDidMount() {
    $('.J_when')
      .find('.custom-control-input')
      .each(function () {
        const v = ~~$(this).val()
        if (!(v === 1 || v === 4 || v === 512)) $(this).attr('disabled', true)
      })

    const content = this.props.content || {}
    $.get(`/admin/robot/trigger/auto-approval-alist?entity=${this.props.sourceEntity}`, (res) => {
      this.setState({ approvalList: res.data }, () => {
        if (content.useApproval) {
          $(this._$useApproval).val(content.useApproval)
          content.submitMode && $(this._$submitMode).attr('checked', true)
        }
      })
    })
  }

  buildContent() {
    const s = {
      useApproval: $val(this._$useApproval) || null,
      submitMode: $val(this._$submitMode),
    }

    if (s.submitMode && !s.useApproval) {
      RbHighbar.create($L('启用提交模式必须选择审批流程'))
      return false
    } else {
      return s
    }
  }
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  renderRbcomp(<ContentAutoApproval {...props} />, 'react-content', function () {
    // eslint-disable-next-line no-undef
    contentComp = this
  })
}
