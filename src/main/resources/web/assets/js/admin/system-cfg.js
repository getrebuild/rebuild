/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-undef, react/display-name */

const wpc = window.__PageConfig

$(document).ready(() => {
  const $dl = $('#_DefaultLanguage')
  $dl.text(wpc._LANGS[$dl.text()] || '中文')

  const $ns = $('#_MobileNavStyle')
  // eslint-disable-next-line eqeqeq
  $ns.text($ns.text() == '35' ? $L('卡片式') : $L('默认'))

  // 禁用
  ;['PasswordExpiredDays', 'DBBackupsKeepingDays', 'RevisionHistoryKeepingDays', 'RecycleBinKeepingDays'].forEach((item) => {
    const $d = $(`td[data-id=${item}]`)
    if (~~$d.attr('data-value') <= 0) $d.text($L('不启用'))
  })

  // HomeURL
  const checkHomeURL = $('td[data-id="HomeURL"]').attr('data-value')
  if (checkHomeURL === 'https://getrebuild.com/') {
    let m = $L('主页地址/域名设置有误，将导致相关功能不可用。建议立即 [修改](###)')
    m = `<div class="alert alert-danger alert-icon alert-sm m-0 mt-1"><div class="icon"><span class="mdi mdi-message-alert-outline"></span></div><div class="message">${m}</div></div>`
    m = $(m).appendTo('td[data-id="HomeURL"]')
    m.find('.message>a').on('click', (e) => {
      $stopEvent(e, true)
      $('.J_edit').trigger('click')
    })
  }

  // UC
  UCenter.query((res) => {
    const bindAccount = res.bindAccount
    if (bindAccount) {
      $('.J_cloudAccount').removeClass('hide')
      $('.J_has-bind').removeClass('hide')
      $('.J_has-bind a').text(bindAccount)
    } else if (rb.commercial === 10 || rb.commercial === 20) {
      $('.J_cloudAccount').removeClass('hide')
      $('.J_not-bind').removeClass('hide')
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
      $mm.find('.btn').on('click', () => renderRbcomp(<DlgMM />))
    }
  })

  $('.J_backDb').on('click', () => renderRbcomp(<DlgBackup />))
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
  } else if (['PageFooter', 'AllowUsesTime', 'AllowUsesIp'].includes(name)) {
    return <textarea name={name} className="form-control form-control-sm row2x" maxLength="2000" />
  } else if ('Login2FAMode' === name) {
    return (
      <select className="form-control form-control-sm">
        <option value="0">{$L('不启用')}</option>
        <option value="1">{$L('手机或邮箱')}</option>
        <option value="2">{$L('仅手机')}</option>
        <option value="3">{$L('仅邮箱')}</option>
      </select>
    )
  } else if ('MobileNavStyle' === name) {
    return (
      <select className="form-control form-control-sm">
        <option value="34">{$L('默认')}</option>
        <option value="35">{$L('卡片式')}</option>
      </select>
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
  $img
    .find('.J_logo-gen')
    .removeAttr('title')
    .off('click')
    .on('click', () => {})
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
          <label>{$L('弹窗附加内容')}</label>
          <textarea className="form-control form-control-sm row2x" ref={(c) => (this._$note = c)} placeholder={$L('维护期间系统将无法使用，请及时保存数据！')} />
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

    // https://flatpickr.js.org/options/
    $([this._$startTime, this._$endTime]).flatpickr({
      enableTime: true,
      enableSeconds: false,
      time_24hr: true,
      minuteIncrement: 1,
      // defaultDate: dd,
      minDate: moment().add(5, 'm').toDate(),
      dateFormat: 'Y-m-d H:i', // :S
      prevArrow: '<i class="mdi mdi-chevron-left"></i>',
      nextArrow: '<i class="mdi mdi-chevron-right"></i>',
      locale: rb.locale.split('_')[0], // zh, en
      onClose: function (s, d, inst) {
        setTimeout(() => {
          const st = $val(that._$startTime)
          if ($(inst.element).hasClass('J_start') && st) {
            const endd = moment(st).add(10, 'm').format('YYYY-MM-DD HH:mm')
            $(that._$endTime).val(endd)
          }
          calcTakeTime()
        }, 200)
      },
      plugins: [
        new ShortcutButtonsPlugin({
          button: [{ label: $L('%d 分钟后', 30) }],
          onClick(i, fp) {
            fp.setDate(moment().add(30, 'm').toDate())
            fp.close()
          },
        }),
        new ShortcutButtonsPlugin({
          button: [{ label: $L('%d 分钟后', 10) }],
          onClick(i, fp) {
            fp.setDate(moment().add(10, 'm').toDate())
            fp.close()
          },
        }),
        new ShortcutButtonsPlugin({
          button: [{ label: $L('%d 分钟后', 5) }],
          onClick(i, fp) {
            fp.setDate(moment().add(5, 'm').toDate())
            fp.close()
          },
        }),
      ],
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

// ~~ App

$(document).ready(() => {
  if (rb.commercial < 10) {
    $('.td-MobileAppPath button').remove()
    return
  }

  const renderMobileAppPath = function (key) {
    $('.td-MobileAppPath>a').text($fileCutName(key)).attr({
      href: '../h5app-download',
      target: '_blank',
    })
    $('button.J_MobileAppPath-del').removeClass('hide')
  }

  const $input = $('input.J_MobileAppPath')
  $initUploader(
    $input,
    (res) => {
      $('button.J_MobileAppPath span').text(` (${res.percent.toFixed(1)}%)`)
    },
    (res) => {
      const fileKey = res.key
      $.post(location.href, JSON.stringify({ MobileAppPath: fileKey }), (res) => {
        if (res.error_code === 0) {
          renderMobileAppPath(fileKey)
          RbHighbar.success($L('上传成功'))
        } else {
          RbHighbar.error(res.error_msg)
        }
        $('button.J_MobileAppPath span').text('')
      })
    }
  )
  $('button.J_MobileAppPath').on('click', () => $input[0].click())

  $('button.J_MobileAppPath-del').on('click', () => {
    RbAlert.create($L('确认删除 APP 安装包？'), {
      onConfirm: function () {
        this.hide()
        $.post(location.href, JSON.stringify({ MobileAppPath: '' }), () => {
          location.reload()
        })
      },
    })
  })

  const apk = $('.td-MobileAppPath>a').text()
  if (apk && apk.length > 20) renderMobileAppPath(apk)
})

class DlgBackup extends RbAlert {
  state = { ...this.props }

  renderContent() {
    const _backup = this.state.db || this.state.file
    return (
      <form className="rbalert-form-sm">
        <div className="form-group mb-0">
          <label className="text-bold">{$L('选择要备份哪些数据')}</label>
          <div ref={(c) => (this._$bkType = c)}>
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline">
              <input className="custom-control-input" type="checkbox" defaultChecked />
              <span className="custom-control-label">{$L('数据库')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline">
              <input className="custom-control-input" type="checkbox" defaultChecked />
              <span className="custom-control-label">{$L('数据目录文件')}</span>
            </label>
          </div>
        </div>
        <div className="form-group mb-1">
          {_backup ? (
            <div className="backup-box">
              <table>
                <tbody>
                  <tr>
                    <td className="text-ellipsis pr-1">{$L('数据库')}</td>
                    <td>{this.state.db ? <code>{this.state.db}</code> : <span className="text-muted">{$L('未备份')}</span>}</td>
                  </tr>
                  <tr>
                    <td className="text-ellipsis pr-1">{$L('数据目录文件')}</td>
                    <td>{this.state.file ? <code>{this.state.file}</code> : <span className="text-muted">{$L('未备份')}</span>}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          ) : (
            <div className="text-warning mb-1" ref={(c) => (this._$tips = c)}>
              <i className="mdi-alert-outline mdi" /> {$L('请勿在业务高峰时段执行备份')}
            </div>
          )}
          <button type="button" className="btn btn-space btn-primary" onClick={this.confirm} ref={(c) => (this._$btn = c)} data-spinner>
            {$L('开始备份')}
          </button>
        </div>
      </form>
    )
  }

  confirm = () => {
    const type = ($(this._$bkType).find('input:eq(0)').prop('checked') ? 1 : 0) + ($(this._$bkType).find('input:eq(1)').prop('checked') ? 2 : 0)
    if (type === 0) return

    this.disabled(true, true)
    const $btn = $(this._$btn).button('loading')
    $.post(`systems/backup?type=${type}`, (res) => {
      if (res.error_code === 0) this.setState({ ...res.data })
      else RbHighbar.error(res.error_msg)

      this.disabled(false, false)
      $btn.button('reset')
    })
  }
}
