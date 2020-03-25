/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable react/no-string-refs */

const wpc = window.__PageConfig

$(document).ready(function () {
  const dt = wpc.fieldType
  const extConfigOld = wpc.extConfig

  const _btn = $('.J_save').click(function () {
    if (!wpc.metaId) return
    let _data = {
      fieldLabel: $val('#fieldLabel'),
      comments: $val('#comments'),
      nullable: $val('#fieldNullable'),
      updatable: $val('#fieldUpdatable'),
      repeatable: $val('#fieldRepeatable')
    }
    if (_data.fieldLabel === '') {
      RbHighbar.create('请输入字段名称')
      return
    }

    const dv = $val('#defaultValue')
    if (dv) {
      if (checkDefaultValue(dv, dt) === false) return
      else _data.defaultValue = dv
    } else if (dv === '') _data.defaultValue = dv

    const extConfig = {}
    $(`.J_for-${dt} .form-control, .J_for-${dt} .custom-control-input`).each(function () {
      const k = $(this).attr('id')
      if ('defaultValue' !== k) {
        extConfig[k] = $val(this)
      }
    })
    if (!$same(extConfig, extConfigOld)) {
      _data['extConfig'] = JSON.stringify(extConfig)
      if (Object.keys(extConfig).length === 0) _data['extConfig'] = ''
    }

    _data = $cleanMap(_data)
    if (Object.keys(_data).length === 0) {
      location.href = '../fields'
      return
    }

    _data.metadata = { entity: 'MetaField', id: wpc.metaId }
    _btn.button('loading')
    $.post('/admin/entity/field-update', JSON.stringify(_data), function (res) {
      if (res.error_code === 0) {
        if (rb.env === 'dev') location.reload(true)
        else location.href = '../fields'
      } else {
        _btn.button('reset')
        RbHighbar.error(res.error_msg)
      }
    })
  })

  $('#fieldNullable').attr('checked', $('#fieldNullable').data('o') === true)
  $('#fieldUpdatable').attr('checked', $('#fieldUpdatable').data('o') === true)
  $('#fieldRepeatable').attr('checked', $('#fieldRepeatable').data('o') === true)

  $('.J_for-' + dt).removeClass('hide')

  // 设置扩展值
  for (let k in extConfigOld) {
    const $ext = $('#' + k)
    if ($ext.length === 1) {
      if ($ext.attr('type') === 'checkbox') $ext.attr('checked', extConfigOld[k] === 'true' || extConfigOld[k] === true)
      else if ($ext.prop('tagName') === 'DIV') $ext.text(extConfigOld[k])
      else $ext.val(extConfigOld[k])
    }
  }

  if (dt === 'PICKLIST' || dt === 'MULTISELECT') {
    $.get(`/admin/field/picklist-gets?entity=${wpc.entityName}&field=${wpc.fieldName}&isAll=false`, function (res) {
      if (res.data.length === 0) { $('#picklist-items li').text('请添加选项'); return }
      $('#picklist-items').empty()
      $(res.data).each(function () { picklistItemRender(this) })
      if (res.data.length > 5) $('#picklist-items').parent().removeClass('autoh')
    })
    $('.J_picklist-edit').click(() => RbModal.create(`${rb.baseUrl}/admin/p/entityhub/picklist-editor?entity=${wpc.entityName}&field=${wpc.fieldName}&multi=${dt === 'MULTISELECT'}`, '配置选项'))
  }
  else if (dt === 'SERIES') {
    $('#defaultValue').parents('.form-group').remove()
    $('#fieldNullable, #fieldUpdatable, #fieldRepeatable').attr('disabled', true)
  }
  else if (dt === 'DATE' || dt === 'DATETIME') {
    $('#defaultValue').datetimepicker({
      componentIcon: 'zmdi zmdi-calendar',
      navIcons: {
        rightIcon: 'zmdi zmdi-chevron-right',
        leftIcon: 'zmdi zmdi-chevron-left'
      },
      format: dt === 'DATE' ? 'yyyy-mm-dd' : 'yyyy-mm-dd hh:ii:ss',
      minView: dt === 'DATE' ? 2 : 0,
      weekStart: 1,
      autoclose: true,
      language: 'zh',
      todayHighlight: false,
      showMeridian: false,
      keyboardNavigation: false,
      minuteStep: 5
    })
    $('#defaultValue').next().removeClass('hide').find('button').click(() => renderRbcomp(<AdvDateDefaultValue type={dt} />))
  }
  else if (dt === 'FILE' || dt === 'IMAGE') {
    let uploadNumber = [0, 9]
    if (extConfigOld['uploadNumber']) {
      uploadNumber = extConfigOld['uploadNumber'].split(',')
      uploadNumber[0] = ~~uploadNumber[0]
      uploadNumber[1] = ~~uploadNumber[1]
      $('.J_minmax b').eq(0).text(uploadNumber[0])
      $('.J_minmax b').eq(1).text(uploadNumber[1])
    }

    $('input.bslider').slider({ value: uploadNumber }).on('change', function (e) {
      const v = e.value.newValue
      $setTimeout(() => {
        $('.J_minmax b').eq(0).text(v[0])
        $('.J_minmax b').eq(1).text(v[1])
        $('#fieldNullable').attr('checked', v[0] <= 0)
      }, 200, 'bslider-change')
    })
    $('#fieldNullable').attr('disabled', true)
  }
  else if (dt === 'CLASSIFICATION') {
    $.get(`/admin/entityhub/classification/info?id=${extConfigOld.classification}`, function (res) {
      $('#useClassification a').attr({ href: `${rb.baseUrl}/admin/entityhub/classification/${extConfigOld.classification}` }).text(res.data.name)
    })
  }
  else if (wpc.fieldName === 'approvalState' || wpc.fieldName === 'approvalId') {
    $('.J_for-STATE, .J_for-REFERENCE').remove()
  }

  // 重复值选项
  if ((dt === 'TEXT' || dt === 'DATE' || dt === 'DATETIME' || dt === 'EMAIL' || dt === 'URL' || dt === 'PHONE' || dt === 'REFERENCE' || dt === 'SERIES')
    && wpc.fieldName !== 'approvalId') {
    $('#fieldRepeatable').parents('.custom-control').removeClass('hide')
  }

  // 内建字段
  if (wpc.fieldBuildin) {
    $('#fieldNullable, #fieldUpdatable, #fieldRepeatable').attr('disabled', true)
    $('.footer .alert').removeClass('hide')
  } else {
    $('.footer .J_action').removeClass('hide')
  }

  $('.J_del').click(function () {
    if (!wpc.isSuperAdmin) { RbHighbar.error('仅超级管理员可删除字段'); return }

    const alertExt = { type: 'danger', confirmText: '删除' }
    alertExt.confirm = function () {
      this.disabled(true)
      $.post(`/admin/entity/field-drop?id=${wpc.metaId}`, (res) => {
        if (res.error_code === 0) {
          this.hide()
          RbHighbar.success('字段已删除')
          setTimeout(function () { location.replace('../fields') }, 1500)
        } else RbHighbar.error(res.error_msg)
      })
    }
    alertExt.call = function () { $countdownButton($(this._dlg).find('.btn-danger')) }
    RbAlert.create('字段删除后将无法恢复，请务必谨慎操作。确认删除吗？', '删除字段', alertExt)
  })
})

// Render item to PickList box
const picklistItemRender = function (data) {
  let item = $('<li class="dd-item" data-key="' + data.id + '"><div class="dd-handle">' + data.text + '</div></li>').appendTo('#picklist-items')
  if (data['default'] === true) item.addClass('default')
}

// Check incorrect?
// Also see RbFormElement#checkHasError in rb-forms.jsx
const checkDefaultValue = function (v, t) {
  let valid = true
  if (t === 'NUMBER' || t === 'DECIMAL') {
    valid = !isNaN(v)
  } else if (t === 'URL') {
    valid = $regex.isUrl(v)
  } else if (t === 'EMAIL') {
    valid = $regex.isMail(v)
  } else if (t === 'PHONE') {
    valid = $regex.isTel(v)
  }
  if (valid === false) RbHighbar.create('默认值设置有误')
  return valid
}

// ~~ 日期高级表达式
class AdvDateDefaultValue extends RbAlert {

  constructor(props) {
    super(props)
    this._refs = []
    this.state.uncalc = true
  }

  renderContent() {
    return (
      <form className="ml-6 mr-6">
        <div className="form-group">
          <label className="text-bold">设置日期公式</label>
          <div className="input-group">
            <select className="form-control form-control-sm" ref={(c) => this._refs[0] = c}>
              <option value="NOW">当前日期</option>
            </select>
            <select className="form-control form-control-sm" ref={(c) => this._refs[1] = c}
              onChange={(e) => this.setState({ uncalc: !e.target.value })}>
              <option value="">不计算</option>
              <option value="+">加上</option>
              <option value="-">减去</option>
            </select>
            <input type="number" min="1" max="999999" className="form-control form-control-sm" defaultValue="1" disabled={this.state.uncalc} ref={(c) => this._refs[2] = c} />
            <select className="form-control form-control-sm" disabled={this.state.uncalc} ref={(c) => this._refs[3] = c}>
              <option value="D">天</option>
              <option value="M">月</option>
              <option value="Y">年</option>
              {this.props.type === 'DATETIME' &&
                <React.Fragment>
                  <option value="H">小时</option>
                  <option value="S">分钟</option>
                </React.Fragment>
              }
            </select>
          </div>
        </div>
        <div className="form-group mb-1">
          <button type="button" className="btn btn-space btn-primary" onClick={this.confirm}>确定</button>
        </div>
      </form>
    )
  }

  confirm = () => {
    let expr = 'NOW'
    const op = $(this._refs[1]).val()
    const num = $(this._refs[2]).val() || 1
    if (op) {
      if (isNaN(num)) {
        RbHighbar.create('请输入数字')
        return
      }
      expr += ` ${op} ${num}${$(this._refs[3]).val()}`
    }
    $('#defaultValue').val('{' + expr + '}')
    this.hide()
  }
}