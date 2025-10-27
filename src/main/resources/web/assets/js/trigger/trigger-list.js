/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global dlgActionAfter ShowEnable taggedTitle */

$(document).ready(function () {
  $('.J_add').on('click', () => renderRbcomp(<TriggerEdit />))
  renderRbcomp(<TriggerList />, 'dataList')

  if (rb.commercial >= 10) {
    $.get('../robot/trigger/trigger-incomplete', (res) => {
      if (res && res.data) console.log('Timer incomplete :', res.data)
    })
  }
})

const RBV_TRIGGERS = {
  'HOOKURL': $L('回调 URL'),
  'AUTOTRANSFORM': $L('自动记录转换'),
  'DATAVALIDATE': $L('数据校验'),
  'AUTOREVOKE': $L('自动撤销审批'),
  'AUTODELETE': $L('自动删除'),
  'PROXYTRIGGERACTION': $L('自定义触发器'),
  'AUTOUNSHARE': $L('自动取消共享'),
  'CREATEFEED': $L('新建动态'),
  'CREATETASK': $L('新建任务'),
  'AUTOGENREPORT': $L('导出报表'),
  'AUTOAPPROVAL': $L('自动审批'),
}

const WHENS = {
  'W1': $L('新建'),
  'W4': $L('更新'),
  'W2': $L('删除'),
  'W16': $L('分配'),
  'W32': $L('共享'),
  'W64': $L('取消共享'),
  'W128': $L('审批通过'),
  'W256': $L('审批撤销'),
  'W1024': $L('审批提交时'),
  'W2048': $L('审批驳回/撤回时'),
  'W512': `(${$L('定期执行')})`,
}

const formatWhen = function (maskVal) {
  const ss = []
  let timed
  for (let k in WHENS) {
    let k2 = ~~k.substring(1)
    if ((maskVal & k2) !== 0) {
      if (k2 === 'W512') timed = true
      else ss.push(WHENS[k])
    }
  }

  if (timed) ss.join(WHENS['W512'])
  return ss.join('/')
}

class TriggerList extends ConfigList {
  constructor(props) {
    super(props)
    this.requestUrl = '/admin/robot/trigger/list'
  }

  render() {
    const q = this.getSearchKey(true)
    return (
      <RF>
        {(this.state.data || []).map((item) => {
          // v4.0 渲染时过滤
          if (q) {
            if (q.startsWith('#')) {
              let name = item[3] || ''
              if (name.includes(q + ' ') || name.endsWith(q));
              else return null
            } else {
              let names = [item[3], item[2], item[7]]
              if (item[9] && item[9][1]) names.push(item[9][1])
              names = names.join(';').toLowerCase()
              if (!names.includes(q.toLowerCase())) return null
            }
          }

          let targetRef = item[9]
          // [ID, NAME]
          if (targetRef) {
            if (!targetRef[0]) {
              targetRef = <a className="text-danger">{targetRef[1]}</a>
            } else if (targetRef[0].startsWith('028-')) {
              targetRef = (
                <a href={`${rb.baseUrl}/admin/robot/approval/${targetRef[0]}`} className="light-link" target={`_${targetRef[0]}`}>
                  {targetRef[1]}
                </a>
              )
            } else if (targetRef[0].startsWith('037-')) {
              targetRef = (
                <a href={`${rb.baseUrl}/admin/robot/transform/${targetRef[0]}`} className="light-link" target={`_${targetRef[0]}`}>
                  {targetRef[1]}
                </a>
              )
            } else if (targetRef[0].startsWith('032-')) {
              targetRef = (
                <a href={`${rb.baseUrl}/admin/data/report-templates#gs=${targetRef[0]}`} className="light-link" target={`_${targetRef[0]}`}>
                  {targetRef[1]}
                </a>
              )
            } else {
              targetRef = (
                <a href={`${rb.baseUrl}/admin/entity/${targetRef[0]}/base`} className="light-link" target={`_${targetRef[0]}`}>
                  {targetRef[1]}
                </a>
              )
            }
          }

          return (
            <tr key={item[0]}>
              <td className="name">
                <a href={`trigger/${item[0]}`}>{taggedTitle(item[3]) || item[2] + ' - ' + item[7]}</a>
              </td>
              <td>
                <a href={`${rb.baseUrl}/admin/entity/${item[1]}/base`} className="light-link" target={`_${item[1]}`}>
                  {item[2] || item[1]}
                </a>
              </td>
              <td>
                {item[7]}
                {targetRef && <span className="ml-1">({targetRef})</span>}
              </td>
              <td className="text-wrap">{item[6] > 0 ? $L('当 %s 时', formatWhen(item[6])) : <span className="text-warning">({$L('无触发动作')})</span>}</td>
              <td>
                <span className="badge badge-light">{item[8]}</span>
              </td>
              <td>{ShowEnable(item[4], item[0])}</td>
              <td>
                <DateShow date={item[5]} title={`${$L('修改时间')} : ${item[5].split('UTC')[0]}\n${$L('添加时间')} : ${item[10]}`} />
              </td>
              <td className="actions">
                <a className="icon" title={$L('触发过程')} onClick={() => this.handleShowChain(item[0])}>
                  <i className="zmdi mdi mdi-vector-polyline" />
                </a>
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

  getSearchKey(real) {
    if (real) return super.getSearchKey()
    // v4.0 渲染时过滤
    return ''
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

  handleShowChain(id) {
    if (rb.commercial < 10) {
      return RbHighbar.error(WrapHtml($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
    }
    RbModal.create(`trigger/trigger-chain?id=${id}`, $L('触发过程'), { urlOpenInNew: true })
  }
}

class TriggerEdit extends ConfigFormDlg {
  constructor(props) {
    super(props)
    this.subtitle = $L('触发器')
  }

  renderFrom() {
    return (
      <RF>
        {!this.props.id && (
          <RF>
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
          </RF>
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
      </RF>
    )
  }

  componentDidMount() {
    if (this.props.id) return

    this.__select2 = []
    // #1
    $.get('/admin/robot/trigger/available-actions', (res) => {
      let actions = res.data || []
      if (!window.__BOSSKEY) {
        actions = actions.filter((item) => !['PROXYTRIGGERACTION', 'AUTOREVOKE'].includes(item[0]))
      }

      this.setState({ actions }, () => {
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

        // #3
        let e = $('.aside-tree li.active>a').attr('href')
        e = e ? e.split('=')[1] : null
        if (e) {
          setTimeout(() => $(this._$sourceEntity).val(e).trigger('change'), 300)
        }
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
