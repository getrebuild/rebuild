/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// ~~ 自动审批
// eslint-disable-next-line
class ContentAutoApproval extends ActionContentSpec {
  static = { ...this.props }

  render() {
    return <div className="auto-approval">
      <form className="simple">
        <div className="form-group row pt-1">
          <label className="col-12 col-lg-3 col-form-label text-lg-right">使用审批流程 (可选)</label>
          <div className="col-12 col-lg-8">
            <select className="form-control form-control-sm" ref={(c) => this._useApproval = c}></select>
          </div>
        </div>
      </form>
    </div>
  }

  componentDidMount() {
    $('.J_when').find('.custom-control-input').each(function () {
      const v = ~~$(this).val()
      if (v !== 1 && v !== 4) $(this).attr('disabled', true)
    })

    const content = this.props.content || {}
    if (content.useApproval) $(this._useApproval).val(content.useApproval)

  }

  buildContent() {
    return {
      useApproval: this._useApproval.val(),
    }
  }
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  // eslint-disable-next-line no-undef
  renderRbcomp(<ContentAutoApproval {...props} />, 'react-content', function () { contentComp = this })
}