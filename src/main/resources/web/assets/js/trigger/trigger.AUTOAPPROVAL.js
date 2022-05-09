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
            <label className="col-12 col-lg-3 col-form-label text-lg-right">
              {$L('使用审批流程')} {$L('(可选)')}
            </label>
            <div className="col-12 col-lg-8">
              <select className="form-control form-control-sm" ref={(c) => (this._useApproval = c)}>
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
        if (content.useApproval) $(this._useApproval).val(content.useApproval)
      })
    })
  }

  buildContent() {
    return {
      useApproval: $(this._useApproval).val() || '',
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
