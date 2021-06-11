/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig
var contentComp = null

$(document).ready(() => {
  $.fn.select2.defaults.set('allowClear', false)

  if (wpc.when > 0) {
    $([1, 2, 4, 16, 32, 64, 128, 256, 512]).each(function () {
      let mask = this
      if ((wpc.when & mask) !== 0) {
        $('.J_when input[value=' + mask + ']').prop('checked', true)
        if (mask === 512) {
          $('.on-timers').removeClass('hide')
          const wt = (wpc.whenTimer || 'D:1').split(':')
          $('.J_whenTimer1').val(wt[0])
          $('.J_whenTimer2').val(wt[1])
        }
      }
    })
  }

  let advFilter
  $('.J_whenFilter .btn').click(() => {
    if (advFilter) {
      advFilter.show()
    } else {
      renderRbcomp(<AdvFilter entity={wpc.sourceEntity} filter={wpc.whenFilter} confirm={saveFilter} title={$L('附加过滤条件')} inModal={true} canNoFilters={true} />, null, function () {
        advFilter = this
      })
    }
  })
  saveFilter(wpc.whenFilter)

  $.get(`/admin/robot/trigger/${wpc.configId}/actionContent`, (res) => {
    renderContentComp({ sourceEntity: wpc.sourceEntity, content: res.data })
  })

  const $btn = $('.J_save').click(() => {
    if (!contentComp) return

    let when = 0
    $('.J_when input:checked').each(function () {
      when += ~~$(this).val()
    })
    const whenTimer = ($('.J_whenTimer1').val() || 'D') + ':' + ($('.J_whenTimer2').val() || 1)
    if (rb.commercial < 1 && (when & 512) !== 0) {
      return RbHighbar.create($L('免费版不支持定时执行功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)'), { type: 'danger', html: true, timeout: 6000 })
    }

    const content = contentComp.buildContent()
    if (content === false) return

    const _data = { when: when, whenTimer: whenTimer, whenFilter: wpc.whenFilter || null, actionContent: content }
    const priority = $val('#priority')
    if (priority) _data.priority = ~~priority || 1
    _data.metadata = { entity: 'RobotTriggerConfig', id: wpc.configId }

    $btn.button('loading')
    $.post('/app/entity/common-save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) location.href = '../triggers'
      else RbHighbar.error(res.error_msg)
      $btn.button('reset')
    })
  })
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
    $.get(`/commons/metadata/fields?deep=2&entity=${this.props.entity || wpc.sourceEntity}`, (res) => {
      $(res.data).each((idx, item) => {
        if (item.type === 'REFERENCE' && item.ref && BIZZ_ENTITIES.includes(item.ref[0])) {
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
