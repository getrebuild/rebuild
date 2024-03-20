/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global LastLogsViewer */

// ~~ 自动审批
// eslint-disable-next-line
class ContentAutoApproval extends ActionContentSpec {
  constructor(props) {
    super(props)
    this.state.useMode = 1
  }

  render() {
    return (
      <div className="auto-approval">
        <form className="simple">
          <div className="form-group row pt-1">
            <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('审批模式')}</label>
            <div className="col-12 col-lg-8">
              <select className="form-control form-control-sm" ref={(c) => (this._$useMode = c)}>
                <optgroup label={$L('未提交记录')}>
                  <option value="1">{$L('直接通过')}</option>
                  <option value="2">{$L('仅提交')}</option>
                </optgroup>
                <optgroup label={$L('审批中记录')}>
                  <option value="11">{$L('通过')}</option>
                  <option value="12">{$L('驳回')}</option>
                  <option value="13">{$L('退回至上一步')}</option>
                </optgroup>
                <optgroup label={$L('已通过记录')}>
                  <option value="21">{$L('撤销')}</option>
                </optgroup>
              </select>
              <p className="form-text">{WrapHtml($L('针对不同的记录审批状态，可选择不同的审批模式'))}</p>
            </div>
          </div>

          <div className={`form-group row pt-2 ${this.state.useMode <= 2 ? '' : 'hide'}`}>
            <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('使用审批流程')}</label>
            <div className="col-12 col-lg-8">
              <select className="form-control form-control-sm" ref={(c) => (this._$useApproval = c)}>
                <option value="">{$L('不使用')}</option>
                {(this.state.approvalList || []).map((item) => {
                  return (
                    <option key={item.id} value={item.id}>
                      {item.text || `@${item.id.toUpperCase()}`}
                    </option>
                  )
                })}
              </select>
              <p className="form-text">{WrapHtml($L('需要先添加 [审批流程](../approvals) 才能在此处选择'))}</p>
            </div>
          </div>
        </form>
      </div>
    )
  }

  componentDidMount() {
    // eslint-disable-next-line no-undef
    disableWhen(2, 16, 32, 64, 128, 256, 1024, 2048)

    $(this._$useMode)
      .select2({})
      .on('change', (e) => this.setState({ useMode: ~~e.target.value }))

    const content = this.props.content || {}
    $.get(`/admin/robot/trigger/auto-approval-alist?entity=${this.props.sourceEntity}`, (res) => {
      this.setState({ approvalList: res.data || [] }, () => {
        $(this._$useApproval).select2({
          placeholder: $L('无'),
          allowClear: true,
        })

        // comp: v3.7
        if (content.submitMode) content.useMode = 2
        if (content.useMode) $(this._$useMode).val(content.useMode).trigger('change')
        if (content.useApproval) $(this._$useApproval).val(content.useApproval).trigger('change')
      })
    })
  }

  buildContent() {
    const s = {
      useMode: this.state.useMode,
      useApproval: $val(this._$useApproval) || null,
    }

    if (s.useMode === 2 && !s.useApproval) {
      RbHighbar.create($L('审批模式为“仅提交”时需要使用审批流程'))
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

  LastLogsViewer._Title = $L('审批记录')
}
