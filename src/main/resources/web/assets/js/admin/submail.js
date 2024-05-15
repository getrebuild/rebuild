/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// eslint-disable-next-line no-undef
postBefore = function (data) {
  const isSmtp = $('.email-set').hasClass('smtp')
  if (isSmtp) {
    data['MailSmtpServer'] = $('input[name="MailSmtpServer"]').val()
    if (!data['MailSmtpServer']) {
      RbHighbar.create($L('SMTP 服务器地址不能为空'))
      return false
    }
  } else {
    data['MailSmtpServer'] = ''
  }
  return data
}

$(document).ready(() => {
  $('.J_test-email').on('click', () => renderRbcomp(<TestSend type="email" />))
  $('.J_test-sms').on('click', () => renderRbcomp(<TestSend type="sms" />))

  // Use SMTP
  if ($('td[data-id="MailSmtpServer"]').attr('data-value')) {
    $('.email-set').addClass('smtp')
  }
  // Switch SMTP
  $('.J_switch-email-set').on('click', () => $('.email-set').toggleClass('smtp'))

  $.get('./submail/stats', (res) => {
    let $el = $('.J_stats-sms')
    $el.find('strong').text(res.data.smsCount || 0)
    _renderStats(res.data.sms, $el)

    $el = $('.J_stats-email')
    $el.find('strong').text(res.data.emailCount || 0)
    _renderStats(res.data.email, $el)
  })
})

const _renderStats = function (data, $el) {
  const xAxis = []
  const series = []
  data.forEach((item) => {
    xAxis.push(item[0])
    series.push(item[1])
  })

  const option = {
    grid: { left: 0, right: 0, top: 4, bottom: 4 },
    animation: true,
    tooltip: {
      trigger: 'axis',
      formatter: '{b} : <b>{c}</b>',
      textStyle: {
        fontSize: 12,
        lineHeight: 1.3,
        color: '#333',
      },
      axisPointer: {
        lineStyle: { color: '#ddd' },
      },
      backgroundColor: '#fff',
      extraCssText: 'border-radius:0;box-shadow:0 0 6px 0 rgba(0, 0, 0, .1), 0 8px 10px 0 rgba(170, 182, 206, .2);',
      confine: true,
      position: 'top',
    },
    textStyle: {
      fontFamily: 'Roboto, "Hiragina Sans GB", San Francisco, "Helvetica Neue", Helvetica, Arial, PingFangSC-Light, "WenQuanYi Micro Hei", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif',
    },
    xAxis: {
      show: false,
      type: 'category',
      data: xAxis,
    },
    yAxis: {
      show: false,
      type: 'value',
      splitLine: { show: false },
      cursor: 'default',
    },
    series: [
      {
        data: series,
        areaStyle: {
          color: 'rgba(66,133,244,0.2)',
        },
        itemStyle: {
          normal: {
            color: 'rgba(66,133,244,1)',
            lineStyle: {
              color: 'rgba(66,133,244,1)',
            },
          },
        },
        type: 'line',
        smooth: true,
        connectNulls: true,
      },
    ],
  }

  const c = echarts.init($el.find('span')[0])
  c.setOption(option)
}

const __LastReceiver = {}
class TestSend extends RbAlert {
  constructor(props) {
    super(props)
    this.state = { ...props }
    this._typeName = props.type === 'email' ? $L('邮箱') : $L('手机')
  }

  renderContent() {
    return (
      <form style={{ maxWidth: 400, margin: '0 auto' }}>
        <div className="form-group">
          <label>{$L('输入接收%s', this._typeName)}</label>
          <input type="text" className="form-control form-control-sm" placeholder={$L(this._typeName)} ref={(c) => (this._input = c)} defaultValue={__LastReceiver[this._typeName]} />
        </div>
        <div className="form-group mb-2">
          <button type="button" className="btn btn-primary" onClick={() => this.confirm()} ref={(c) => (this._btn = c)}>
            {$L('发送')}
          </button>
        </div>
      </form>
    )
  }

  confirm() {
    const receiver = $(this._input).val()
    if (!receiver) return RbHighbar.create($L('请输入%s', this._typeName))

    const conf = {}
    $('.syscfg table td[data-id]').each(function () {
      const $this = $(this)
      conf[$this.data('id')] = $this.find('input').val()
    })

    // None SMTP
    if ($('.email-set.smtp').length === 0) conf['MailSmtpServer'] = null

    $(this._btn).button('loading')
    $.post('./submail/test?type=' + this.props.type + '&receiver=' + $encode(receiver), JSON.stringify(conf), (res) => {
      if (res.error_code === 0) {
        RbHighbar.success($L('发送成功'))
        // this.hide()
      } else {
        RbHighbar.create(res.error_msg || $L('发送失败'))
      }
      $(this._btn).button('reset')
      __LastReceiver[this._typeName] = receiver
    })
  }
}
