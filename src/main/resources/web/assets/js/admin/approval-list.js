/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global dlgActionAfter ShowEnable */

$(document).ready(() => {
  renderRbcomp(<ApprovalList />, 'dataList')

  $('.J_add').click(() => renderRbcomp(<ApprovalEdit />))
  $('.J_referral').click(() => renderRbcomp(<ApprovalReferral />))
})

class ApprovalList extends ConfigList {
  constructor(props) {
    super(props)
    this.requestUrl = '/admin/robot/approval/list'
  }

  render() {
    return (
      <RF>
        {(this.state.data || []).map((item) => {
          return (
            <tr key={item[0]}>
              <td>
                <a href={`approval/${item[0]}`}>{item[3]}</a>
              </td>
              <td>{item[2] || item[1]}</td>
              <td className={`J_state-${item[0]}`}>..</td>
              <td>{ShowEnable(item[4])}</td>
              <td>
                <DateShow date={item[5]} />
              </td>
              <td className="actions">
                <a className="icon" title={$L('修改')} onClick={() => this.handleEdit(item)}>
                  <i className="zmdi zmdi-edit" />
                </a>
                <a className="icon danger-hover" title={$L('删除')} onClick={() => this.handleDelete(item[0])}>
                  <i className="zmdi zmdi-delete" />
                </a>
              </td>
            </tr>
          )
        })}
      </RF>
    )
  }

  loadDataAfter() {
    if (!this.__stateCache) this.__stateCache = {}

    function fn(aid, s) {
      const states = []
      if (s[0] + s[1] === 0) {
        states.push(`<span class="text-warning">(${$L('未使用')})</span>`)
      } else {
        if (s[0] > 0) states.push(`<span class="badge badge-light text-warning border-warning" title="${$L('审批中')}">${s[0]}</span>`)
        if (s[1] > 0) states.push(`<span class="badge badge-light text-success border-success" title="${$L('审批通过')}">${s[1]}</span>`)
      }
      $(`.J_state-${aid}`).html(states.join(''))
    }

    this.state.data.forEach((item) => {
      const aid = item[0]
      if (this.__stateCache[aid]) {
        fn(aid, this.__stateCache[aid])
      } else {
        $.get(`/admin/robot/approval/use-stats?ids=${aid}`, (res) => {
          const data = res.data || {}
          this.__stateCache[aid] = data[aid] || [0, 0]
          fn(aid, this.__stateCache[aid])
        })
      }
    })
  }

  handleEdit(item) {
    renderRbcomp(<ApprovalEdit id={item[0]} name={item[3]} isDisabled={item[4]} />)
  }

  handleDelete(id) {
    const handle = super.handleDelete
    RbAlert.create(WrapHtml($L('若流程正在使用则不能删除，建议你将其禁用。[] 确认删除此审批流程吗？')), {
      type: 'danger',
      confirmText: $L('删除'),
      confirm: function () {
        this.disabled(true)
        handle(id, () => dlgActionAfter(this))
      },
    })
  }
}

class ApprovalEdit extends ConfigFormDlg {
  constructor(props) {
    super(props)
    this.subtitle = $L('审批流程')
  }

  renderFrom() {
    return (
      <RF>
        {!this.props.id && (
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('选择应用实体')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" ref={(c) => (this._entity = c)}>
                {(this.state.entities || []).map((item) => {
                  return (
                    <option key={`e-${item.name}`} value={item.name}>
                      {item.label}
                    </option>
                  )
                })}
              </select>
            </div>
          </div>
        )}
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('名称')}</label>
          <div className="col-sm-7">
            <input type="text" className="form-control form-control-sm" data-id="name" onChange={this.handleChange} value={this.state.name || ''} />
          </div>
        </div>
        {this.props.id && (
          <div className="form-group row">
            <div className="col-sm-7 offset-sm-3">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" checked={this.state.isDisabled === true} data-id="isDisabled" onChange={this.handleChange} />
                <span className="custom-control-label">
                  {$L('是否禁用')}
                  <i ref={(c) => (this._tooltip = c)} className="zmdi zmdi-help zicon" title={$L('禁用后正在使用此流程的审批记录不受影响')}></i>
                </span>
              </label>
            </div>
          </div>
        )}
      </RF>
    )
  }

  componentDidMount() {
    super.componentDidMount()

    if (this.props.id) $(this._tooltip).tooltip()

    let e = $('.aside-tree li.active>a').attr('href')
    e = e ? e.split('=')[1] : null
    if (e) {
      setTimeout(() => $(this._entity).val(e).trigger('change'), 300)
    }
  }

  confirm = () => {
    const post = { name: this.state['name'] }
    if (!post.name) return RbHighbar.create($L('请输入名称'))

    if (!this.props.id) {
      post.belongEntity = this.__select2.val()
      if (!post.belongEntity) {
        RbHighbar.create($L('请选择应用实体'))
        return
      }
    } else {
      post.isDisabled = this.state.isDisabled === true
    }
    post.metadata = {
      entity: 'RobotApprovalConfig',
      id: this.props.id || null,
    }

    this.disabled(true)
    $.post('/app/entity/common-save', JSON.stringify(post), (res) => {
      if (res.error_code === 0) {
        if (this.props.id) dlgActionAfter(this)
        else location.href = `approval/${res.data.id}`
      } else {
        RbHighbar.error(res.error_msg)
      }
      this.disabled()
    })
  }
}

// 批量转审
class ApprovalReferral extends RbModalHandler {
  render() {
    const title = (
      <RF>
        {$L('批量转审')}
        <sup className="rbv" title={$L('增值功能')} />
      </RF>
    )

    return (
      <RbModal title={title} ref={(c) => (this._dlg = c)} disposeOnHide>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('转审哪些实体')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" ref={(c) => (this._$entity = c)} multiple></select>
              <p className="form-text">{$L('可转审所有实体或指定实体的审批')}</p>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('转审谁的审批')}</label>
            <div className="col-sm-7">
              <UserSelector hideDepartment hideRole hideTeam multiple={false} ref={(c) => (this._UserSelector1 = c)} />
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('转审给谁')}</label>
            <div className="col-sm-7">
              <UserSelector hideDepartment hideRole hideTeam multiple={false} ref={(c) => (this._UserSelector2 = c)} />
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3">
              <button className="btn btn-primary" type="button" onClick={() => this.start()} ref={(c) => (this._$btn = c)}>
                {$L('开始转审')}
              </button>
              <a className="btn btn-link" onClick={this.hide}>
                {$L('取消')}
              </a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    const that = this
    $('.aside-tree li[data-entity]').each(function () {
      const $this = $(this)
      $(`<option value="${$this.data('entity')}">${$this.text()}</option>`).appendTo(that._$entity)
    })

    this.__select2 = $(this._$entity).select2({
      placeholder: $L('全部实体'),
    })
  }

  start() {
    if (rb.commercial < 10) {
      RbHighbar.error(WrapHtml($L('免费版不支持批量转审功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }

    const post = {
      oldUser: this._UserSelector1.val()[0],
      newUser: this._UserSelector2.val()[0],
      specEntities: this.__select2.val(),
    }
    if (!post.oldUser) return RbHighbar.create($L('请选择转审谁的审批'))
    if (!post.newUser) return RbHighbar.create($L('请选择转审给谁'))
    if (post.oldUser === post.newUser) return RbHighbar.create($L('不能是同一个用户'))
    if (post.specEntities.length === 0) post.specEntities = null

    const that = this
    RbAlert.create($L('如数据较多耗时会较长，请耐心等待。确定转审吗？'), {
      onConfirm: function () {
        this.hide()

        const $btn = $(that._$btn).button('loading')
        $mp.start()

        $.post('/admin/robot/approval/referral', JSON.stringify(post), (res) => {
          $mp.end()

          if (res.error_code === 0) {
            RbHighbar.success(res.data > 0 ? $L('已转审 %d 条审批记录') : $L('批量转审完成'))
            setTimeout(() => that.hide(), 1500)
          } else {
            RbHighbar.error(res.error_msg)
            $btn.button('reset')
          }
        })
      },
    })
  }
}
