/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig

let contentComp = null
let whenUpdateFields
let whenApproveNodes

$(document).ready(() => {
  $.fn.select2.defaults.set('allowClear', false)

  for (let i = 0; i < 24; i++) {
    const H1 = i < 10 ? `0${i}` : i
    const H2 = i + 1 < 10 ? `0${i + 1}` : i + 1
    $(`<option value="${i}">${H1}:00</option>`).appendTo('.J_startHour1')
    $(`<option value="${i}">${H2}:00</option>`).appendTo('.J_startHour2')
  }
  $('.J_startHour1')
    .val('0')
    .on('change', function () {
      const start = ~~this.value
      const end = ~~$('.J_startHour2').val()
      $('.J_startHour2 option').each(function () {
        $(this).attr('disabled', ~~$(this).val() < start)
      })
      if (end < start) $('.J_startHour2').val(start)
    })
  $('.J_startHour2').val('23')

  if (wpc.when > 0) {
    $([1, 2, 4, 16, 32, 64, 128, 256, 512, 1024, 2048]).each(function () {
      let mask = this
      if ((wpc.when & mask) !== 0) {
        $(`.J_when input[value=${mask}]`).prop('checked', true)

        if (mask === 512) {
          $('.on-timers').removeClass('hide')
          const wt = (wpc.whenTimer || 'D:1').split(':')
          $('.J_whenTimer1').val(wt[0])
          $('.J_whenTimer2').val(wt[1])
          // v2.9
          if (wt[2]) $('.J_startHour1').val(wt[2])
          if (wt[3]) $('.J_startHour2').val(wt[3])
          // v3.8
          if (wt[4]) $('.J_whenTimer4').val(wt[4]).parents('.bosskey-show').removeClass('bosskey-show')

          $('.J_whenTimer1').trigger('change')
        }
      }
    })
  }

  // 评估具体执行时间
  function evalTriggerTimes() {
    const whenTimer = `${$('.J_whenTimer1').val() || 'D'}:${$('.J_whenTimer2').val() || 1}:${$('.J_startHour1').val() || 0}:${$('.J_startHour2').val() || 23}:${$('.J_whenTimer4').val() || ''}`
    $.get(`/admin/robot/trigger/eval-trigger-times?whenTimer=${whenTimer}`, (res) => {
      renderRbcomp(
        <RbAlertBox
          icon="time"
          message={
            <div>
              <span className="mr-1">{$L('预计执行时间 (最多显示近 9 次)')} : </span>
              <code>{res.data.slice(0, 9).join(', ')}</code>
            </div>
          }
        />,
        $('.eval-exec-times')[0]
      )
    })
  }
  if (rb.commercial >= 10) {
    $('.on-timers select').on('change', () => $setTimeout(evalTriggerTimes, 500, 'eval-trigger-times'))
    $('.on-timers input').on('input', () => $setTimeout(evalTriggerTimes, 500, 'eval-trigger-times'))
  }

  let advFilter
  $('.J_whenFilter .btn').on('click', () => {
    if (advFilter) {
      advFilter.show()
    } else {
      renderRbcomp(<AdvFilter entity={wpc.sourceEntity} filter={wpc.whenFilter} confirm={saveFilter} title={$L('附加过滤条件')} inModal canNoFilters />, function () {
        advFilter = this
      })
    }
  })
  saveFilter(wpc.whenFilter)

  // 指定字段
  $('.when-update a').on('click', (e) => {
    $stopEvent(e, true)
    renderDlgcomp(
      <DlgSpecFields
        selected={whenUpdateFields}
        onConfirm={(s) => {
          whenUpdateFields = s
          const $s = $('.when-update .custom-control-label')
          if (s.length > 0) $s.text(`${$s.text().split(' (')[0]} (${s.length})`)
          else $s.text($s.text().split(' (')[0])
        }}
      />,
      'DlgSpecFields'
    )
  })
  DlgSpecFields.render(wpc.actionContent)

  // 指定步骤
  $('.when-approve a').on('click', (e) => {
    $stopEvent(e, true)
    renderDlgcomp(
      <DlgSpecApproveNodes
        selected={whenApproveNodes}
        onConfirm={(s) => {
          whenApproveNodes = s
          const $s = $('.when-approve .custom-control-label')
          if (s.length > 0) $s.text(`${$s.text().split(' (')[0]} (${s.length})`)
          else $s.text($s.text().split(' (')[0])
        }}
      />,
      'DlgSpecApproveNodes'
    )
  })
  DlgSpecApproveNodes.render(wpc.actionContent)

  // 立即执行
  useExecManual()

  renderContentComp({ sourceEntity: wpc.sourceEntity, content: wpc.actionContent })

  const $btn = $('.J_save').on('click', () => {
    if (!contentComp) return

    let when = 0
    $('.J_when input:checked').each(function () {
      when += ~~$(this).val()
    })
    if (rb.commercial < 10 && ((when & 512) !== 0 || (when & 1024) !== 0 || (when & 2048) !== 0)) {
      RbHighbar.error(WrapHtml($L('免费版不支持审批提交时/审批驳回时/定时执行功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }

    const whenTimer = `${$('.J_whenTimer1').val() || 'D'}:${$('.J_whenTimer2').val() || 1}:${$('.J_startHour1').val() || 0}:${$('.J_startHour2').val() || 23}:${$('.J_whenTimer4').val() || ''}`

    const content = contentComp.buildContent()
    if (content === false) return

    if (window.whenUpdateFields) content.whenUpdateFields = window.whenUpdateFields
    if (window.whenApproveNodes) content.whenApproveNodes = window.whenApproveNodes
    const data = {
      when: when,
      whenTimer: whenTimer,
      whenFilter: wpc.whenFilter || null,
      actionContent: content,
      metadata: {
        entity: 'RobotTriggerConfig',
        id: wpc.configId,
      },
    }
    const priority = $val('#priority')
    if (priority && !isNaN(priority)) data.priority = ~~priority

    $btn.button('loading')
    $.post('/app/entity/common-save', JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        const msg = (
          <RF>
            <strong>{$L('保存成功')}</strong>
            {when <= 0 && <p className="text-warning m-0 mt-1">{$L('由于未启用任何触发动作，此触发器不会被自动执行')}</p>}
          </RF>
        )
        RbAlert.create(msg, {
          icon: 'info-outline',
          cancelText: $L('返回列表'),
          cancel: () => location.replace('../triggers'),
          confirmText: $L('继续编辑'),
          confirm: () => location.reload(),
        })
      } else {
        RbHighbar.error(res.error_msg)
      }
      $btn.button('reset')
    })
  })

  if (LastLogsViewer.renderLog && rb.commercial > 1) {
    $.get(`/admin/robot/trigger/last-logs?id=${wpc.configId}`, (res) => {
      const _data = res.data || []
      if (_data.length > 0) {
        const last = _data[0]
        const $a = $(`<a href="#last-logs">${$fromNow(last[0])}</a>`).appendTo($('.J_last-logs .form-control-plaintext').empty())
        $a.on('click', (e) => {
          $stopEvent(e, true)
          renderRbcomp(<LastLogsViewer width="681" data={_data} />)
        })
      }
      $('.J_last-logs').removeClass('hide')
    })
  }

  // v3.7
  $('.page-help>a').attr('href', $('.page-help>a').attr('href') + wpc.actionType.toLowerCase())
})

const saveFilter = function (res) {
  wpc.whenFilter = res
  if (wpc.whenFilter && wpc.whenFilter.items && wpc.whenFilter.items.length > 0) $('.J_whenFilter a').text(`${$L('已设置条件')} (${wpc.whenFilter.items.length})`)
  else $('.J_whenFilter a').text($L('点击设置'))
}

// 组件复写
var renderContentComp = function (props) {
  // eslint-disable-next-line no-console
  if (rb.env === 'dev') console.log(props)
}

// 执行日志查看
class LastLogsViewer extends RbAlert {
  renderContent() {
    return (
      <RF>
        <table className="table table-hover table-logs">
          <thead>
            <tr>
              <th>{$L('执行内容/结果')}</th>
              <th width="150">{$L('执行时间')}</th>
            </tr>
          </thead>
          <tbody>
            {this.props.data.map((item, idx) => {
              return (
                <tr key={idx}>
                  <td>{this._renderLog(item[1])}</td>
                  <td>{item[0]}</td>
                </tr>
              )
            })}
          </tbody>
        </table>
        {this.props.data.length >= 100 && <p className="text-muted text-center">{$L('最多显示最近 100 次执行')}</p>}
      </RF>
    )
  }

  _renderLog(log) {
    if (!log) return <p className="text-warning">UNKNOW</p>

    try {
      const L = JSON.parse(log)
      const LR = LastLogsViewer.renderLog(L)
      if (LR === false) {
        return <p className={`text-uppercase ${L.level === 3 ? 'text-warning' : 'text-muted'}`}>{L.message || 'N'}</p>
      }
      return LR
    } catch (err) {
      console.debug(err)
      return <p className="text-danger text-overflow">{log}</p>
    }
  }

  // 日志解析复写
  static renderLog(L) {
    L.level = L.level || 2
    return L.level === 1 ? (
      <div className="v36-logdesc">
        {LastLogsViewer._Title || $L('影响记录')}
        {L.affected.map((a, idx) => {
          return (
            <a key={idx} className="badge text-id ml-1" href={`${rb.baseUrl}/app/redirect?id=${a}&type=newtab`} target="_blank">
              {a}
            </a>
          )
        })}
        {LastLogsViewer._Chain && L.chain && (
          <RF>
            <dt className="mt-1 font-weight-normal">
              <a
                onClick={(e) => {
                  $(e.currentTarget).find('i.mdi').toggleClass('mdi-chevron-double-up')
                  $(e.currentTarget).parent().next().toggleClass('hide')
                }}>
                {$L('技术细节')}
                <i className="mdi mdi-chevron-double-down" />
              </a>
            </dt>
            <dd className="mb-0 hide">
              <blockquote className="tech-details code">{L.chain}</blockquote>
            </dd>
          </RF>
        )}
      </div>
    ) : (
      false
    )
  }
}

const BIZZ_ENTITIES = ['User', 'Department', 'Role', 'Team']

// 用户选择器
// eslint-disable-next-line no-unused-vars
class UserSelectorWithField extends UserSelector {
  constructor(props) {
    super(props)
    this._useTabs.push(['FIELDS', $L('使用字段')])
  }

  componentDidMount() {
    super.componentDidMount()

    this._fields = []
    $.get(`/commons/metadata/fields?deep=3&entity=${this.props.entity || wpc.sourceEntity}`, (res) => {
      $(res.data).each((idx, item) => {
        if ((item.type === 'REFERENCE' || item.type === 'N2NREFERENCE') && item.ref && BIZZ_ENTITIES.includes(item.ref[0])) {
          this._fields.push({ id: item.name, text: item.label })
        }
      })
    })
  }

  switchTab(type) {
    type = type || this.state.tabType
    if (type === 'FIELDS') {
      const q = this.state.query
      const ckey = type + '-' + q
      this.setState({ tabType: type, items: this._cached[ckey] }, () => {
        if (!this._cached[ckey]) {
          if (!q) {
            this._cached[ckey] = this._fields
          } else {
            const fs = []
            $(this._fields).each(function () {
              if (this.text.contains(q)) fs.push(this)
            })
            this._cached[ckey] = fs
          }
          this.switchTab(type)
        }
      })
    } else {
      super.switchTab(type)
    }
  }
}

// 动作类定义
// eslint-disable-next-line no-unused-vars
class ActionContentSpec extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  // 子类复写返回操作内容
  buildContent() {
    return false
  }
}

// eslint-disable-next-line no-unused-vars
function _handle512Change() {
  if ($(event.target).prop('checked')) $('.on-timers').removeClass('hide')
  else $('.on-timers').addClass('hide')
}

// 立即执行
function useExecManual() {
  function _FN(taskid, that) {
    const mp_parent = $(that._dlg).find('.modal-header').attr('id', $random('node-'))
    const mp = new Mprogress({ template: 1, start: true, parent: '#' + $(mp_parent).attr('id') })
    useExecManual_checkState(taskid, mp, that)
  }

  $('.J_exec-manual').on('click', () => {
    if (rb.commercial < 10) {
      RbHighbar.error(WrapHtml($L('免费版不支持立即执行功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }

    RbAlert.create($L('将直接执行此触发器，数据过多耗时会较长，请耐心等待。是否继续？'), {
      onConfirm: function () {
        const that = this
        that.disabled(true, true)
        $.post(`/admin/robot/trigger/exec-manual?id=${wpc.configId}`, (res) => {
          let taskid = res.data || ''
          // 执行中了
          if (taskid.startsWith('_EXECUTE:')) {
            taskid = taskid.substr(9)
            RbAlert.create($L('此触发器已在执行中，不能同时执行。是否显示执行状态？'), {
              onConfirm: function () {
                this.hide()
                _FN(taskid, that)
              },
              onCancel: function () {
                this.hide()
                that.hide(true)
              },
            })
            return
          }

          _FN(taskid, that)
        })
      },
      countdown: 5,
    })
  })
}
// 检查状态
function useExecManual_checkState(taskid, mp, _alert) {
  $.get(`/commons/task/state?taskid=${taskid}`, (res) => {
    const cp = res.data.progress
    if (res.data.isCompleted) {
      mp && mp.end()
      _alert && _alert.hide(true)
      RbHighbar.success($L('执行成功'))
    } else {
      mp && mp.set(cp)
      setTimeout(() => useExecManual_checkState(taskid, mp, _alert), 1000)
    }
  })
}

// ~ 指定字段
class DlgSpecFields extends RbModalHandler {
  render() {
    const _selected = this.props.selected || []
    return (
      <RbModal
        title={
          <RF>
            {$L('指定字段')}
            <sup className="rbv" />
          </RF>
        }
        ref={(c) => (this._dlg = c)}
        width="780">
        <div className="p-2">
          <RbAlertBox message={$L('指定字段被更新时触发，默认为全部字段')} />
          <div className="row" ref={(c) => (this._$fields = c)}>
            {(this.state.fields || []).map((item) => {
              if (item.type === 'BARCODE' || item.updatable === false) return null
              return (
                <div className="col-3" key={`field-${item.name}`}>
                  <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-1">
                    <input className="custom-control-input" type="checkbox" value={item.name} defaultChecked={_selected.includes(item.name)} />
                    <span className="custom-control-label">{item.label}</span>
                  </label>
                </div>
              )
            })}
          </div>
        </div>

        <div className="dialog-footer">
          <button className="btn btn-secondary btn-space mr-2" type="button" onClick={this.hide}>
            {$L('取消')}
          </button>
          <button className="btn btn-primary btn-space" type="button" onClick={() => this.handleConfirm()}>
            {$L('确定')}
          </button>
        </div>
      </RbModal>
    )
  }

  handleConfirm() {
    if (rb.commercial < 1) {
      RbHighbar.error(WrapHtml($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }

    const selected = []
    $(this._$fields)
      .find('input:checked')
      .each(function () {
        selected.push(this.value)
      })

    typeof this.props.onConfirm === 'function' && this.props.onConfirm(selected)
    this.hide()
  }

  componentDidMount() {
    $.get(`/commons/metadata/fields?entity=${wpc.sourceEntity}`, (res) => this.setState({ fields: res.data }))
  }

  static render(content) {
    if (content && content.whenUpdateFields && content.whenUpdateFields.length > 0) {
      window.whenUpdateFields = content.whenUpdateFields
      const $s = $('.when-update .custom-control-label')
      $s.text(`${$s.text()} (${content.whenUpdateFields.length})`)
    }
  }
}

// ~ 指定审批步骤
class DlgSpecApproveNodes extends RbModalHandler {
  render() {
    return (
      <RbModal
        title={
          <RF>
            {$L('指定步骤')}
            <sup className="rbv" />
          </RF>
        }
        ref={(c) => (this._dlg = c)}
        width="780">
        <div className="p-2">
          <RbAlertBox message={$L('指定审批步骤 (名称) 通过时触发，默认仅审批完成时触发')} />
          <div className="row">
            <div className="col-12">
              <label>
                {$L('填写步骤名称')} ({$L('* 表示所有')})
              </label>
              <div>
                <select className="form-control form-control-sm" ref={(c) => (this._$set = c)} multiple>
                  {(this.state.stepNames || []).map((item) => {
                    return (
                      <option key={item} value={item}>
                        {item}
                      </option>
                    )
                  })}
                </select>
              </div>
            </div>
          </div>
        </div>

        <div className="dialog-footer">
          <button className="btn btn-secondary btn-space mr-2" type="button" onClick={this.hide}>
            {$L('取消')}
          </button>
          <button className="btn btn-primary btn-space" type="button" onClick={() => this.handleConfirm()}>
            {$L('确定')}
          </button>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    this.__select2 = $(this._$set).select2({
      placeholder: $L('无'),
      language: {
        noResults: function () {
          return $L('请输入')
        },
      },
      tags: true,
      theme: 'default select2-tag',
      allowClear: true,
    })

    $.get(`/admin/robot/trigger/approval-steps?entity=${wpc.sourceEntity}`, (res) => {
      let names = res.data || []
      if (this.props.selected) {
        this.props.selected.forEach((item) => {
          if (!names.includes(item)) names.push(item)
        })
      }
      this.setState({ stepNames: names }, () => {
        this.props.selected && this.__select2.val(this.props.selected).trigger('change')
      })
    })
  }

  handleConfirm() {
    if (rb.commercial < 1) {
      RbHighbar.error(WrapHtml($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }

    const selected = $(this._$set).val()
    typeof this.props.onConfirm === 'function' && this.props.onConfirm(selected)
    this.hide()
  }

  static render(content) {
    if (content && content.whenApproveNodes && content.whenApproveNodes.length > 0) {
      window.whenApproveNodes = content.whenApproveNodes
      const $s = $('.when-approve .custom-control-label')
      $s.text(`${$s.text()} (${content.whenApproveNodes.length})`)
    }
  }
}

// eslint-disable-next-line no-unused-vars
function disableWhen() {
  const args = arguments
  $('.J_when')
    .find('.custom-control-input')
    .each(function () {
      const when = ~~$(this).val()
      for (let i = 0; i < args.length; i++) {
        if (args[i] === when) {
          $(this).attr('disabled', true)
          // 指定步骤/指定字段
          if (when === 128 || when === 4) {
            $(this).parent().find('>a').remove()
          }
          break
        }
      }
    })
}
