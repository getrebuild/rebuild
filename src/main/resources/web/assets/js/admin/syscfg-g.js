/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-undef, react/display-name */

const wpc = window.__PageConfig

let val_MobileAppPath

$(document).ready(() => {
  const $dl = $('#_DefaultLanguage')
  $dl.text(wpc._LANGS[$dl.text()] || '中文')

  // 禁用
  ;['PasswordExpiredDays', 'DBBackupsKeepingDays', 'RevisionHistoryKeepingDays', 'RecycleBinKeepingDays'].forEach((item) => {
    const $d = $(`td[data-id=${item}]`)
    if (~~$d.attr('data-value') <= 0) $d.text($L('不启用'))
  })

  // v35
  val_MobileAppPath = $('#_MobileAppPath').data('value')
  if (val_MobileAppPath) $(`<a href="${rb.baseUrl}/h5app-download" target="_blank">${$fileCutName(val_MobileAppPath)}</a>`).appendTo($('#_MobileAppPath').empty())

  // UC
  UCenter.query((res) => {
    const bindAccount = res.bindAccount
    $('.J_cloudAccount').removeClass('hide')
    if (bindAccount) {
      $('.J_not-bind').addClass('hide')
      $('.J_has-bind a').text(bindAccount)
    } else if (rb.commercial === 10 || rb.commercial === 20) {
      $('.J_has-bind').addClass('hide')
      $('.J_not-bind .btn').on('click', () => {
        if (res.canBind) UCenter.bind()
        else RbHighbar.create($L('仅超级管理员可操作'))
      })
    }
  })

  // v34
  const $mm = $('.J_maintenanceMode')
  $.get('/admin/systems/maintenance-mode', (res) => {
    const _data = res.data
    if (_data) {
      $mm
        .find('.btn')
        .text($L('取消维护计划'))
        .on('click', () => {
          RbAlert.create($L('确认取消维护计划？'), {
            onConfirm: function () {
              const that = this
              $.post('/admin/systems/maintenance-mode?cancel=yes', () => {
                that.hide()
                setTimeout(() => location.reload(), 200)
              })
            },
          })
        })

      $mm.find('.note dd:eq(0)').text(_data.startTime.substr(0, 16) + ` ${$L('至')} ` + _data.endTime.substr(0, 16))
      $mm.find('.note dd:eq(1)').text(_data.note || $L('无'))
      $mm.find('.note').show()
    } else {
      $mm.find('.btn').on('click', () => {
        if (rb.commercial < 1) return RbHighbar.error(WrapHtml($L('免费版不支持开启维护计划功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
        renderRbcomp(<DlgMM />)
      })
    }
  })
})

useEditComp = function (name) {
  if (['OpenSignUp', 'LiveWallpaper', 'FileSharable', 'MarkWatermark', 'DBBackupsEnable', 'MultipleSessions', 'ShowViewHistory', 'PageMourningMode'].includes(name)) {
    return (
      <select className="form-control form-control-sm">
        <option value="true">{$L('是')}</option>
        <option value="false">{$L('否')}</option>
      </select>
    )
  } else if ('PasswordPolicy' === name) {
    // 借用贵宝地
    _toggleImage('.applogo', true)
    _toggleImage('.bgimg')

    return (
      <select className="form-control form-control-sm">
        <option value="1">{$L('低 (最低6位，无字符类型限制)')}</option>
        <option value="2">{$L('中 (最低6位，必须同时包含数字、字母)')}</option>
        <option value="3">{$L('高 (最低10位，必须同时包含数字、字母、特殊字符)')}</option>
      </select>
    )
  } else if ('DefaultLanguage' === name) {
    const options = []
    for (let k in wpc._LANGS) {
      options.push(
        <option value={k} key={k}>
          {wpc._LANGS[k]}
        </option>
      )
    }
    return <select className="form-control form-control-sm">{options}</select>
  } else if ('LoginCaptchaPolicy' === name) {
    return (
      <select className="form-control form-control-sm">
        <option value="1">{$L('自动')}</option>
        <option value="2">{$L('总是显示')}</option>
      </select>
    )
  } else if ('PageFooter' === name || 'AllowUsesTime' === name || 'AllowUsesIp' === name) {
    return <textarea name={name} className="form-control form-control-sm row3x" maxLength="600" />
  } else if ('Login2FAMode' === name) {
    return (
      <select className="form-control form-control-sm">
        <option value="0">{$L('不启用')}</option>
        <option value="1">{$L('手机或邮箱')}</option>
        <option value="2">{$L('仅手机')}</option>
        <option value="3">{$L('仅邮箱')}</option>
      </select>
    )
  } else if ('MobileAppPath' === name) {
    setTimeout(() => {
      $initUploader($('.file_MobileAppPath'), null, (res) => {
        $('#_MobileAppPath a').text($fileCutName(res.key))
        changeValue({ target: { name: 'MobileAppPath', value: res.key } })
      })
    }, 1000)

    return (
      <RF>
        <button className="btn btn-light btn-sm btn_MobileAppPath" type="button" onClick={() => $('.file_MobileAppPath')[0].click()}>
          <i className="icon zmdi zmdi-upload"></i> {$L('上传')}
        </button>
        <a className="ml-1" href={`${rb.baseUrl}/h5app-download`} target="_blank">
          {val_MobileAppPath ? $fileCutName(val_MobileAppPath) : null}
        </a>
        {val_MobileAppPath && (
          <a
            title={$L('移除')}
            className="ml-1"
            onClick={() => {
              $('#_MobileAppPath a').text('')
              changeValue({ target: { name: 'MobileAppPath', value: null } })
            }}>
            <i className="mdi mdi-close" />
          </a>
        )}
      </RF>
    )
  }
}

let _$imgCurrent
const _toggleImage = function (el, init) {
  const $file = $('.file_4image')
  if (init) {
    $initUploader($file, null, (res) => {
      _$imgCurrent.find('>i').css('background-image', `url(${rb.baseUrl}/filex/img/${res.key}?local=true)`)
      changeValue({ target: { name: _$imgCurrent.data('id'), value: res.key } })
    })
  }

  const $img = $(el).addClass('edit')
  $img.find('p').removeClass('hide')

  $img
    .find('a')
    .attr('title', $L('点击上传'))
    .on('click', function () {
      _$imgCurrent = $(this)
      $file[0].click()
    })
  $img.find('a>b').on('click', function (e) {
    $stopEvent(e, true)
    _$imgCurrent = $(this).parent()

    _$imgCurrent.find('>i').css('background-image', `url(${rb.baseUrl}/assets/img/s.gif)`)
    changeValue({ target: { name: _$imgCurrent.data('id'), value: '' } })
  })
}

class DlgMM extends RbAlert {
  renderContent() {
    return (
      <form className="rbalert-form-sm">
        <div className="form-group">
          <label>{$L('计划维护时间')}</label>
          <div className="input-group">
            <input type="text" className="form-control form-control-sm bg-white J_start" ref={(c) => (this._$startTime = c)} placeholder={$L('开始时间')} readOnly />
            <div className="input-group-prepend input-group-append">
              <span className="input-group-text pt-0 pb-0">{$L('至')}</span>
            </div>
            <input type="text" className="form-control form-control-sm bg-white" ref={(c) => (this._$endTime = c)} placeholder={$L('结束时间')} readOnly />
          </div>
          {this.state.takeTime1 ? <div className="form-text text-warning">{$L('将在 %s 分钟后开始，维护时间 %s 分钟', this.state.takeTime1, this.state.takeTime2)}</div> : null}
        </div>
        <div className="form-group">
          <label>{$L('维护原因')}</label>
          <textarea className="form-control form-control-sm row2x" ref={(c) => (this._$note = c)} placeholder={$L('例行维护')} />
        </div>
        <div className="form-group mb-2">
          <button type="button" className="btn btn-danger" onClick={this._onConfirm}>
            {$L('开启')}
          </button>
          <div className="mt-3">
            <RbAlertBox message={$L('开启后，系统将以弹窗形式通知所有登录用户')} />
          </div>
        </div>
      </form>
    )
  }

  componentDidMount() {
    super.componentDidMount()

    const that = this
    function calcTakeTime() {
      const post = that._buildPost()
      if (post.startTime && post.endTime) {
        that.setState({
          takeTime1: moment(post.startTime).diff(moment(), 'minutes'),
          takeTime2: Math.max(moment(post.endTime).diff(moment(post.startTime), 'minutes'), 0),
        })
      } else {
        that.setState({ takeTime: null })
      }
    }

    $([this._$startTime, this._$endTime])
      .datetimepicker({
        startDate: new Date(),
      })
      .on('changeDate', (e) => {
        if ($(e.target).hasClass('J_start')) {
          if ($val(this._$startTime) && !$val(this._$endTime)) {
            const autoEnd = moment($val(this._$startTime)).add('minute', 10).format('YYYY-MM-DD HH:mm')
            $(this._$endTime).val(autoEnd)
          }
        }
        calcTakeTime()
      })
  }

  _buildPost() {
    return {
      startTime: $val(this._$startTime),
      endTime: $val(this._$endTime),
      note: $val(this._$note),
    }
  }

  _onConfirm = () => {
    const post = this._buildPost()
    if (!post.startTime || !post.endTime) return RbHighbar.create($L('请选择计划维护时间'))

    post.startTime += ':00'
    post.endTime += ':00'
    $.post('/admin/systems/maintenance-mode', JSON.stringify(post), (res) => {
      if (res.error_code === 0) {
        this.hide()
        setTimeout(() => location.reload(), 200)
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}
