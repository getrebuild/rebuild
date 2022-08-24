/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global dlgActionAfter ShowEnable */

$(document).ready(function () {
  $('.J_add').click(() => renderRbcomp(<TriggerEdit />))
  renderRbcomp(<TriggerList />, 'dataList')
})

const WHENS = {
  1: $L('新建'),
  4: $L('更新'),
  2: $L('删除'),
  16: $L('分派'),
  32: $L('共享'),
  64: $L('取消共享'),
  128: $L('审批通过'),
  256: $L('审批撤销'),
  512: `(${$L('定期执行')})`,
}

const RBV_TRIGGERS = {
  'HOOKURL': $L('回调 URL'),
  'AUTOTRANSFORM': $L('自动记录转换'),
  'DATAVALIDATE': $L('数据校验'),
  'AUTOREVOKE': $L('自动撤销'),
}

const formatWhen = function (maskVal) {
  const as = []
  for (let k in WHENS) {
    // eslint-disable-next-line eqeqeq
    if ((maskVal & k) !== 0) as.push(WHENS[k])
  }
  return as.join('/')
}

class TriggerList extends ConfigList {
  constructor(props) {
    super(props)
    this.requestUrl = '/admin/robot/trigger/list'
  }

  render() {
    return (
      <React.Fragment>
        {(this.state.data || []).map((item) => {
          const locked = item[8]
          const disabled = locked && locked[0] !== rb.currentUser

          return (
            <tr key={item[0]}>
              <td>
                <a href={`trigger/${item[0]}`}>{item[3] || item[2] + ' · ' + item[7]}</a>
              </td>
              <td>{item[2] || item[1]}</td>
              <td>{item[7]}</td>
              <td>{item[6] > 0 ? $L('当 %s 时', formatWhen(item[6])) : <span className="text-warning">({$L('无触发动作')})</span>}</td>
              <td>
                <span className="badge badge-light">{item[9]}</span>
              </td>
              <td>{ShowEnable(item[4])}</td>
              <td>
                <DateShow date={item[5]} />
              </td>
              <td className="actions">
                {locked ? (
                  <a className="icon" title={locked[0] === rb.currentUser ? $L('解锁') : $L('已被 %s 锁定', locked[1])} onClick={() => this.handleLock(item)}>
                    <i className={`zmdi zmdi-lock-outline text-${disabled ? 'danger' : 'warning'}`} />
                  </a>
                ) : (
                  <a className="icon" title={$L('锁定')} onClick={() => this.handleLock(item, true)}>
                    <i className="zmdi zmdi-lock-open" />
                  </a>
                )}
                <a className="icon" title={$L('修改')} onClick={() => !disabled && this.handleEdit(item)} disabled={disabled}>
                  <i className="zmdi zmdi-edit" />
                </a>
                <a className="icon danger-hover" title={$L('删除')} onClick={() => !disabled && this.handleDelete(item[0])} disabled={disabled}>
                  <i className="zmdi zmdi-delete" />
                </a>
              </td>
            </tr>
          )
        })}
      </React.Fragment>
    )
  }

  handleLock(item, lock) {
    if (lock !== true && item[8][0] !== rb.currentUser) {
      RbHighbar.create($L('请联系 %s 解锁', item[8][1]))
      return
    }

    const tips = lock ? $L('锁定后只有你可以修改/删除，其他人无法操作，直到你解锁') : $L('确认解锁？')

    RbAlert.create(tips, {
      type: 'warning',
      onConfirm: function () {
        this.disabled(true)
        $.post(`/admin/lock/${lock ? 'lock' : 'unlock'}?id=${item[0]}`, (res) => {
          this.hide()
          if (res.error_code === 0) dlgActionAfter()
          else RbHighbar.error(res.error_msg)
        })
      },
    })
  }

  handleEdit(item) {
    renderRbcomp(<TriggerEdit id={item[0]} name={item[3]} isDisabled={item[4]} />)
  }

  handleDelete(id) {
    const handle = super.handleDelete
    RbAlert.create($L('确认删除此触发器？'), {
      type: 'danger',
      confirmText: $L('删除'),
      confirm: function () {
        this.disabled(true)
        handle(id, () => dlgActionAfter(this))
      },
    })
  }
}

class TriggerEdit extends ConfigFormDlg {
  constructor(props) {
    super(props)
    this.subtitle = $L('触发器')
  }

  renderFrom() {
    return (
      <React.Fragment>
        {!this.props.id && (
          <React.Fragment>
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('选择触发器')}</label>
              <div className="col-sm-7">
                <select className="form-control form-control-sm" ref={(c) => (this._$actionType = c)}>
                  {(this.state.actions || []).map((item) => {
                    return (
                      <option key={item[0]} value={item[0]}>
                        {item[1]}
                      </option>
                    )
                  })}
                </select>
              </div>
            </div>
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('选择源实体')}</label>
              <div className="col-sm-7">
                <select className="form-control form-control-sm" ref={(c) => (this._$sourceEntity = c)}>
                  {(this.state.sourceEntities || []).map((item) => {
                    return (
                      <option key={item[0]} value={item[0]}>
                        {item[1]}
                      </option>
                    )
                  })}
                </select>
              </div>
            </div>
          </React.Fragment>
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
                <span className="custom-control-label">{$L('是否禁用')}</span>
              </label>
            </div>
          </div>
        )}
      </React.Fragment>
    )
  }

  componentDidMount() {
    if (this.props.id) return

    this.__select2 = []
    // #1
    $.get('/admin/robot/trigger/available-actions', (res) => {
      this.setState({ actions: res.data }, () => {
        const s2ot = $(this._$actionType)
          .select2({
            placeholder: $L('选择触发类型'),
            allowClear: false,
            templateResult: function (s) {
              if (Object.keys(RBV_TRIGGERS).includes(s.id)) {
                return $(`<span>${s.text} <sup class="rbv"></sup></span>`)
              } else {
                return s.text
              }
            },
          })
          .on('change', () => {
            this._getEntitiesByAction(s2ot.val())
          })
        this.__select2.push(s2ot)

        // #2
        const s2se = $(this._$sourceEntity).select2({
          placeholder: $L('选择源实体'),
          allowClear: false,
        })
        this.__select2.push(s2se)

        s2ot.trigger('change')
      })
    })
  }

  _getEntitiesByAction(type) {
    $.get(`/admin/robot/trigger/available-entities?action=${type}`, (res) => {
      this.setState({ sourceEntities: res.data })
    })
  }

  confirm = () => {
    let data = { name: this.state['name'] }
    if (!data.name) return RbHighbar.create($L('请输入名称'))

    if (this.props.id) {
      data.isDisabled = this.state.isDisabled === true
    } else {
      data = { ...data, actionType: this.__select2[0].val(), belongEntity: this.__select2[1].val() }
      if (!data.actionType || !data.belongEntity) {
        return RbHighbar.create($L('请选择源实体'))
      }
    }
    data.metadata = {
      entity: 'RobotTriggerConfig',
      id: this.props.id || null,
    }

    if (rb.commercial < 10 && Object.keys(RBV_TRIGGERS).includes(data.actionType)) {
      RbHighbar.error(WrapHtml($L('免费版不支持%s功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)', RBV_TRIGGERS[data.actionType])))
      return
    }

    this.disabled(true)
    $.post('/app/entity/common-save', JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        if (this.props.id) dlgActionAfter(this)
        else location.href = 'trigger/' + res.data.id
      } else {
        RbHighbar.error(res.error_msg)
      }
      this.disabled()
    })
  }
}
