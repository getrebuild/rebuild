/* eslint-disable react/prop-types */
const wpc = window.__PageConfig
$(document).ready(() => {
  if (wpc.when > 0) {
    $([1, 2, 4, 16, 32, 64]).each(function () {
      let mask = this
      // eslint-disable-next-line eqeqeq
      if ((wpc.when & mask) != 0) $('.J_when input[value=' + mask + ']').prop('checked', true)
    })
  }

  // let advFilter
  $('.J_whenFilter .btn').click(() => {
    // if (advFilter) advFilter.show()
    // else advFilter = renderRbcomp(<AdvFilter title="设置过滤条件" entity={wpc.sourceEntity} filter={wpc.whenFilter} inModal={true} confirm={saveFilter} canNoFilters={true} />)
    renderRbcomp(<AdvFilter title="设置过滤条件" entity={wpc.sourceEntity} filter={wpc.whenFilter} inModal={true} confirm={saveFilter} canNoFilters={true} />)
  })
  saveFilter(wpc.whenFilter)

  let contentComp
  const compProps = { sourceEntity: wpc.sourceEntity, content: wpc.actionContent }
  if (wpc.actionType === 'COUNTSSLAVE') {
    contentComp = renderRbcomp(<ContentCountsSlave {...compProps} />, 'react-content')
  } else if (wpc.actionType === 'SENDNOTIFICATION') {
    contentComp = renderRbcomp(<ContentSendNotification {...compProps} />, 'react-content')
  } else {
    renderRbcomp(<div className="text-danger">未实现的操作类型: {wpc.actionType}</div>, 'react-content')
    $('.J_save').attr('disabled', true)
    return
  }

  let _btn = $('.J_save').click(() => {
    let when = 0
    $('.J_when input:checked').each(function () {
      when += ~~$(this).val()
    })

    let content = contentComp.buildContent()
    if (content === false) return

    let _data = { when: when, whenFilter: wpc.whenFilter || null, actionContent: content }
    let p = $val('#priority')
    if (p) _data.priority = ~~p || 1
    _data.metadata = { entity: 'RobotTriggerConfig', id: wpc.configId }

    _btn.button('loading')
    $.post(`${rb.baseUrl}/admin/robot/trigger/save`, JSON.stringify(_data), (res) => {
      if (res.error_code === 0) location.reload()
      else rb.hberror(res.error_msg)
      _btn.button('reset')
    })
  })
})

const saveFilter = function (res) {
  wpc.whenFilter = res
  if (wpc.whenFilter && wpc.whenFilter.items && wpc.whenFilter.items.length > 0) $('.J_whenFilter a span').text('(已配置过滤)')
  else $('.J_whenFilter a span').text('(无)')
}

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

// ~~ 明细汇总
class ContentCountsSlave extends ActionContentSpec {
  constructor(props) {
    super(props)
  }
  render() {
    return <div className="counts-slave">
      <div className="row pt-1 pb-1">
        <div className="col-5">
          <p>源字段 (明细实体)</p>
          <select className="form-control form-control-sm" ref={(c) => this._sourceField = c}>
            {(this.state.sourceFields || []).map((item) => {
              return <option key={'so-' + item[0]} value={item[0]}>{item[1]}</option>
            })}
          </select>
        </div>
        <div className="col-2 pl-0 pr-0">
          <p>计算方式</p>
          <select className="form-control form-control-sm" ref={(c) => this._calcMode = c}>
            <option value="SUM">求和</option>
            <option value="COUNT">计数</option>
            <option value="AVG">平均值</option>
            <option value="MAX">最大值</option>
            <option value="MIN">最小值</option>
          </select>
        </div>
        <div className="col-5">
          <p>目标字段 (主实体)</p>
          <select className="form-control form-control-sm" ref={(c) => this._targetField = c}>
            {(this.state.targetFields || []).map((item) => {
              return <option key={'to-' + item[0]} value={item[0]}>{item[1]}</option>
            })}
          </select>
        </div>
      </div>
    </div>
  }
  componentDidMount() {
    this.__select2 = []
    $.get(`${rb.baseUrl}/admin/robot/trigger/counts-slave-fields?sourceEntity=${this.props.sourceEntity}`, (res) => {
      this.setState({ sourceFields: res.data.slave, targetFields: res.data.master }, () => {
        let s2sf = $(this._sourceField).select2({ placeholder: '选择源字段', allowClear: false })
        let s2cm = $(this._calcMode).select2({ placeholder: '选择计算方式', allowClear: false })
        let s2tf = $(this._targetField).select2({ placeholder: '选择目标字段', allowClear: false })
        this.__select2.push(s2sf)
        this.__select2.push(s2cm)
        this.__select2.push(s2tf)
        if (this.props.content && this.props.calcMode) {
          s2sf.val(this.props.content.sourceField).trigger('change')
          s2cm.val(this.props.content.calcMode).trigger('change')
          s2tf.val(this.props.content.targetField).trigger('change')
        }
      })
    })
  }
  buildContent() {
    let _data = { sourceField: $(this._sourceField).val(), calcMode: $(this._calcMode).val(), targetField: $(this._targetField).val() }
    if (!_data.sourceField) { rb.highbar('源字段不能为空'); return false }
    if (!_data.targetField) { rb.highbar('目标字段不能为空'); return false }
    return _data
  }
}

// ~~ 发送通知
class ContentSendNotification extends ActionContentSpec {
  constructor(props) {
    super(props)
  }
  render() {
    return <div className="send-notification">
      <form className="simple">
        <div className="form-group row pt-1">
          <label className="col-12 col-lg-2 col-form-label text-lg-right">发送给谁</label>
          <div className="col-12 col-lg-8">
            <UserSelectorExt ref={(c) => this._sendTo = c} />
          </div>
        </div>
        <div className="form-group row pb-1">
          <label className="col-12 col-lg-2 col-form-label text-lg-right">发送内容</label>
          <div className="col-12 col-lg-8">
            <textarea className="form-control form-control-sm row3x" ref={(c) => this._content = c} maxLength="600"></textarea>
          </div>
        </div>
      </form>
    </div>
  }
  componentDidMount() {
    if (this.props.content && this.props.content.sendTo) {
      $.post(`${rb.baseUrl}/admin/robot/trigger/send-notification-sendtos?entity=${this.props.sourceEntity}`, JSON.stringify(this.props.content.sendTo), (res) => {
        if (res.error_code === 0 && res.data.length > 0) this._sendTo.setState({ selected: res.data })
      })
    }
    $(this._content).val(this.props.content.content || '')
  }
  buildContent() {
    let _data = { sendTo: this._sendTo.getSelected(), content: $(this._content).val() }
    if (!_data.sendTo || _data.sendTo.length === 0) { rb.highbar('请选择发送给谁'); return false }
    if (!_data.content) { rb.highbar('发送内容不能为空'); return false }
    return _data
  }
}
class UserSelectorExt extends UserSelector {
  constructor(props) {
    super(props)
    this.tabTypes.push(['FIELDS', '使用字段'])
  }
  componentDidMount() {
    super.componentDidMount()

    this.__fields = []
    $.get(`${rb.baseUrl}/commons/metadata/fields?deep=2&entity=${wpc.sourceEntity}`, (res) => {
      $(res.data).each((idx, item) => {
        if (item.type === 'REFERENCE' && item.ref && (item.ref[0] === 'User' || item.ref[0] === 'Department' || item.ref[0] === 'Role')) {
          this.__fields.push({ id: item.name, text: item.label })
        }
      })
    })
  }
  switchTab(type) {
    type = type || this.state.tabType
    if (type === 'FIELDS') {
      const q = this.state.query
      const cacheKey = type + '-' + q
      this.setState({ tabType: type, items: this.cached[cacheKey] }, () => {
        if (!this.cached[cacheKey]) {
          if (!q) this.cached[cacheKey] = this.__fields
          else {
            let fs = []
            $(this.__fields).each(function () {
              if (this.text.contains(q)) fs.push(this)
            })
            this.cached[cacheKey] = fs
          }
          this.switchTab(type)
        }
      })
    } else {
      super.switchTab(type)
    }
  }
}