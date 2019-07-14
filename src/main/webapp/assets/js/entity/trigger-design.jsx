const wpc = window.__PageConfig
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

  let contentComp
  const compProps = { sourceEntity: wpc.sourceEntity, content: wpc.actionContent }
  if (wpc.actionType === 'FIELDAGGREGATION') {
    // eslint-disable-next-line react/jsx-no-undef
    renderRbcomp(<ContentFieldAggregation {...compProps} />, 'react-content', function () { contentComp = this })
  } else if (wpc.actionType === 'SENDNOTIFICATION') {
    // eslint-disable-next-line react/jsx-no-undef
    renderRbcomp(<ContentSendNotification {...compProps} />, 'react-content', function () { contentComp = this })
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
    $.post(`${rb.baseUrl}/app/entity/record-save`, JSON.stringify(_data), (res) => {
      if (res.error_code === 0) location.href = '../triggers'
      else RbHighbar.error(res.error_msg)
      _btn.button('reset')
    })
  })
})

const saveFilter = function (res) {
  wpc.whenFilter = res
  if (wpc.whenFilter && wpc.whenFilter.items && wpc.whenFilter.items.length > 0) $('.J_whenFilter a span').text('(已配置过滤)')
  else $('.J_whenFilter a span').text('(无)')
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