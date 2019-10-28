const wpc = window.__PageConfig
var contentComp = null
$(document).ready(() => {
  $.fn.select2.defaults.set('allowClear', false)

  if (wpc.when > 0) {
    $([1, 2, 4, 16, 32, 64]).each(function () {
      let mask = this
      // eslint-disable-next-line eqeqeq
      if ((wpc.when & mask) != 0) $('.J_when input[value=' + mask + ']').prop('checked', true)
    })
  }

  let advFilter
  $('.J_whenFilter .btn').click(() => {
    if (advFilter) advFilter.show()
    else renderRbcomp(<AdvFilter title="设置过滤条件" entity={wpc.sourceEntity} filter={wpc.whenFilter} inModal={true} confirm={saveFilter} canNoFilters={true} />, null, function () { advFilter = this })
  })
  saveFilter(wpc.whenFilter)

  renderContentComp({ sourceEntity: wpc.sourceEntity, content: wpc.actionContent })

  let _btn = $('.J_save').click(() => {
    if (!contentComp) return

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
    $.post(`${rb.baseUrl}/app/entity/record-save`, JSON.stringify(_data), (res) => {
      if (res.error_code === 0) location.href = '../triggers'
      else RbHighbar.error(res.error_msg)
      _btn.button('reset')
    })
  })
})

const saveFilter = function (res) {
  wpc.whenFilter = res
  if (wpc.whenFilter && wpc.whenFilter.items && wpc.whenFilter.items.length > 0) $('.J_whenFilter a').text(`已设置条件 (${wpc.whenFilter.items.length})`)
  else $('.J_whenFilter a').text('点击设置')
}

// 组件复写
var renderContentComp = function (props) {
  // eslint-disable-next-line no-console
  if (rb.env === 'dev') console.log(props)
}

// 用户选择器
// eslint-disable-next-line no-unused-vars
class UserSelectorExt extends UserSelector {
  constructor(props) {
    super(props)
    this.tabTypes.push(['FIELDS', '使用字段'])
  }

  componentDidMount() {
    super.componentDidMount()
    this.__fields = []
    $.get(`${rb.baseUrl}/commons/metadata/fields?deep=2&entity=${this.props.entity || wpc.sourceEntity}`, (res) => {
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