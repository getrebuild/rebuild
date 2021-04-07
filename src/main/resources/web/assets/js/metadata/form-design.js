/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig
const DIVIDER_LINE = '$DIVIDER$'

$(document).ready(function () {
  $.get(`../list-field?entity=${wpc.entityName}`, function (res) {
    const validFields = {},
      configFields = []
    $(wpc.formConfig.elements).each(function () {
      configFields.push(this.field)
    })

    const $advControls = $('#adv-control tbody')
    const template = $advControls.find('tr').html()
    $advControls.find('tr').remove()

    $(res.data).each(function () {
      validFields[this.fieldName] = this
      if (configFields.includes(this.fieldName) === false) render_unset(this, '.field-list')

      // Adv control
      const $control = $(`<tr data-field="${this.fieldName}">${template}</tr>`).appendTo($advControls)
      $control.find('td:eq(0)').text(this.fieldLabel)
      const $req = $control.find('td:eq(2)')
      if (this.builtin) $req.empty()
      else if (!this.nullable) $req.find('input').attr({ disabled: true, checked: true })
    })

    $(wpc.formConfig.elements).each(function () {
      const field = validFields[this.field]
      if (this.field === DIVIDER_LINE) {
        render_item({ fieldName: this.field, fieldLabel: this.label || '', isFull: true }, '.form-preview')
      } else if (!field) {
        const $item = $(`<div class="dd-item"><div class="dd-handle J_field J_missed"><span class="text-danger">[${this.field.toUpperCase()}] ${$L('SomeDeleted,Field')}</span></div></div>`).appendTo(
          '.form-preview'
        )
        const $action = $('<div class="dd-action"><a><i class="zmdi zmdi-close"></i></a></div>').appendTo($item.find('.dd-handle'))
        $action.find('a').click(function () {
          $item.remove()
        })
      } else {
        render_item({ ...field, isFull: this.isFull || false, tip: this.tip || null }, '.form-preview')
        AdvControl.set(this)
      }
    })

    $('.form-preview')
      .sortable({
        cursor: 'move',
        placeholder: 'dd-placeholder',
        cancel: '.nodata',
      })
      .disableSelection()
  })

  $('.J_add-divider').click(function () {
    $('.nav-tabs-classic a[href="#form-design"]').tab('show')
    render_item({ fieldName: DIVIDER_LINE, fieldLabel: '', isFull: true }, '.form-preview')
  })

  // @see field-new.html
  const FIELD_TYPES = [
    'TEXT',
    'NTEXT',
    'PHONE',
    'EMAIL',
    'URL',
    'NUMBER',
    'DECIMAL',
    'DATE',
    'DATETIME',
    'PICKLIST',
    'CLASSIFICATION',
    'MULTISELECT',
    'REFERENCE',
    'N2NREFERENCE',
    'FILE',
    'IMAGE',
    'AVATAR',
    'SERIES',
    'BARCODE',
    'BOOL',
  ]
  FIELD_TYPES.forEach((type) => render_type(type))

  // SAVE

  const _handleSave = function (elements) {
    const data = {
      belongEntity: wpc.entityName,
      applyType: 'FORM',
      config: JSON.stringify(elements),
      metadata: {
        entity: 'LayoutConfig',
        id: wpc.formConfig.id || null,
      },
    }

    $('.J_save').button('loading')
    $.post('form-update', JSON.stringify(data), function (res) {
      if (res.error_code === 0) location.reload()
      else RbHighbar.error(res.error_msg)
    })
  }

  $('.J_save').click(function () {
    const formElements = []
    $('.form-preview .J_field').each(function () {
      const $this = $(this)
      if (!$this.data('field')) return

      const item = { field: $this.data('field') }
      if (item.field === DIVIDER_LINE) {
        item.isFull = true
        item.label = $this.find('span').text()
      } else {
        item.isFull = $this.parent().hasClass('w-100')
        const tip = $this.find('.J_tip').attr('title')
        if (tip) item.tip = tip
        item.__newLabel = $this.find('span').text()
        if (item.__newLabel === $this.data('label')) delete item.__newLabel

        AdvControl.append(item)
      }
      formElements.push(item)
    })

    if (formElements.length === 0) return RbHighbar.create($L('PlsLayout1FieldsLeast'))

    if ($('.field-list .not-nullable').length > 0) {
      RbAlert.create($L('HasRequiredFieldUnLayoutConfirm'), {
        type: 'warning',
        confirmText: $L('Save'),
        confirm: function () {
          this.hide()
          _handleSave(formElements)
        },
      })
    } else {
      _handleSave(formElements)
    }
  })

  $addResizeHandler(() => {
    $('.field-aside .rb-scroller').height($(window).height() - 130)
  })()

  $('.nav-tabs-classic a[href="#adv-control"]').on('click', (e) => {
    if (rb.commercial < 1) {
      e.preventDefault()
      RbHighbar.create($L('FreeVerNotSupportted,FormAdvControl'), { type: 'danger', html: true, timeout: 6000 })
      return false
    }

    // 只显示布局的
    const shows = []
    $('.form-preview .J_field').each(function () {
      shows.push($(this).data('field') || '')
    })

    $('#adv-control tbody>tr').each(function () {
      const $tr = $(this)
      if (shows.indexOf($tr.data('field')) > -1) $tr.removeClass('hide')
      else $tr.addClass('hide')
    })
  })
})

const render_item = function (data) {
  const $item = $('<div class="dd-item"></div>').appendTo('.form-preview')
  if (data.isFull === true) $item.addClass('w-100')

  const $handle = $(`<div class="dd-handle J_field" data-field="${data.fieldName}" data-label="${data.fieldLabel}"><span _title="${$L('Divider')}">${data.fieldLabel}</span></div>`).appendTo($item)
  if (data.creatable === false) $handle.addClass('readonly')
  else if (data.nullable === false) $handle.addClass('not-nullable')
  // 填写提示
  if (data.tip) $('<i class="J_tip zmdi zmdi-info-outline"></i>').appendTo($handle.find('span')).attr('title', data.tip)

  const $action = $('<div class="dd-action"></div>').appendTo($handle)
  if (data.displayType) {
    $(`<span class="ft">${data.displayType}</span>`).appendTo($item)
    $(`<a class="rowspan mr-1" title="${$L('Column1Or2')}"><i class="zmdi zmdi-unfold-more"></i></a>`)
      .appendTo($action)
      .click(function () {
        $item.toggleClass('w-100')
      })
    $(`<a title="${$L('Modify')}"><i class="zmdi zmdi-edit"></i></a>`)
      .appendTo($action)
      .click(function () {
        const call = function (nv) {
          // 字段名
          if (nv.fieldLabel) $item.find('.dd-handle>span').text(nv.fieldLabel)
          else $item.find('.dd-handle>span').text($item.find('.dd-handle').data('label'))

          // 填写提示
          let $tip = $item.find('.dd-handle>span>i')
          if (!nv.fieldTips) {
            $tip.remove()
          } else {
            if ($tip.length === 0) $tip = $('<i class="J_tip zmdi zmdi-info-outline"></i>').appendTo($item.find('.dd-handle span'))
            $tip.attr('title', nv.fieldTips)
          }
        }
        const ov = {
          fieldTips: $item.find('.dd-handle>span>i').attr('title'),
          fieldLabel: $item.find('.dd-handle>span').text(),
          fieldLabelOld: $item.find('.dd-handle').data('label'),
        }
        if (ov.fieldLabelOld === ov.fieldLabel) ov.fieldLabel = null

        renderRbcomp(<DlgEditField call={call} {...ov} />)
      })

    $(`<a title="${$L('Remove')}"><i class="zmdi zmdi-close"></i></a>`)
      .appendTo($action)
      .click(function () {
        render_unset(data)
        $item.remove()
      })
  }

  if (data.fieldName === DIVIDER_LINE) {
    $item.addClass('divider')
    $(`<a title="${$L('Modify')}"><i class="zmdi zmdi-edit"></i></a>`)
      .appendTo($action)
      .click(function () {
        const call = function (nv) {
          $item.find('.dd-handle span').text(nv.dividerName || '')
        }
        const ov = $item.find('.dd-handle span').text()
        renderRbcomp(<DlgEditDivider call={call} dividerName={ov || ''} />)
      })

    $(`<a title="${$L('Remove')}"><i class="zmdi zmdi-close"></i></a>`)
      .appendTo($action)
      .click(function () {
        $item.remove()
      })
  }
}

const render_unset = function (data) {
  const $item = $(`<li class="dd-item"><div class="dd-handle">${data.fieldLabel}</div></li>`).appendTo('.field-list')
  $(`<span class="ft">${data.displayType}</span>`).appendTo($item)
  if (data.creatable === false) $item.find('.dd-handle').addClass('readonly')
  else if (data.nullable === false) $item.find('.dd-handle').addClass('not-nullable')

  $item.click(function () {
    $('.nav-tabs-classic a[href="#form-design"]').tab('show')
    render_item(data)
    $item.remove()
  })
  return $item
}

const render_type = function (fieldType) {
  const $item = $(`<li class="dd-item"><div class="dd-handle">${$L(`t.${fieldType}`)}</div></li>`).appendTo('.type-list')
  $item.click(function () {
    if (wpc.isSuperAdmin) RbModal.create(`/p/admin/metadata/field-new?entity=${wpc.entityName}&type=${fieldType}`, $L('AddField'))
    else RbHighbar.error($L('OnlyAdminCanSome,AddField'))
  })
  return $item
}

// 字段属性
class DlgEditField extends RbAlert {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  renderContent() {
    return (
      <form className="field-attr">
        <div className="form-group">
          <label>{$L('InputTips')}</label>
          <input
            type="text"
            className="form-control form-control-sm"
            name="fieldTips"
            value={this.state.fieldTips || ''}
            onChange={this.handleChange}
            placeholder={$L('InputSome,InputTips')}
            maxLength="200"
          />
        </div>
        <div className="form-group">
          <label>
            {$L('FieldName')} <span>({$L('SomeFieldsNotModify')})</span>
          </label>
          <input
            type="text"
            className="form-control form-control-sm"
            name="fieldLabel"
            value={this.state.fieldLabel || ''}
            onChange={this.handleChange}
            placeholder={this.props.fieldLabelOld || $L('ModifySome,FieldName')}
            maxLength="100"
          />
        </div>
        <div className="form-group mb-1">
          <button type="button" className="btn btn-space btn-primary" onClick={this.confirm}>
            {$L('Confirm')}
          </button>
        </div>
      </form>
    )
  }

  handleChange = (e) => {
    let target = e.target
    let s = {}
    s[target.name] = target.type === 'checkbox' ? target.checked : target.value
    this.setState(s)
  }

  confirm = () => {
    typeof this.props.call === 'function' && this.props.call(this.state || {})
    this.hide()
  }
}

// 分栏属性
class DlgEditDivider extends DlgEditField {
  constructor(props) {
    super(props)
  }

  renderContent() {
    return (
      <form className="field-attr">
        <div className="form-group">
          <label>{$L('DividerName')}</label>
          <input type="text" className="form-control form-control-sm" name="dividerName" value={this.state.dividerName || ''} onChange={this.handleChange} placeholder={$L('InputSome,DividerName')} />
        </div>
        <div className="form-group mb-1">
          <button type="button" className="btn btn-space btn-primary" onClick={this.confirm}>
            {$L('Confirm')}
          </button>
        </div>
      </form>
    )
  }
}

// 追加到布局
// eslint-disable-next-line no-unused-vars
const add2Layout = function (fieldName) {
  $.get(`../list-field?entity=${wpc.entityName}`, function (res) {
    $(res.data).each(function () {
      if (this.fieldName === fieldName) {
        render_item({ ...this, isFull: this.isFull || false, tip: this.tip || null }, '.form-preview')
        return false
      }
    })
  })

  RbModal.hide()
}

// 高级控制
const AdvControl = {
  $controls: $('#adv-control tbody'),

  append: function (item) {
    this.$controls.find(`tr[data-field="${item.field}"] input`).each(function () {
      const $this = $(this)
      item[$this.attr('name')] = $this.prop('checked')
    })
  },

  set: function (item) {
    this.$controls.find(`tr[data-field="${item.field}"] input`).each(function () {
      const $this = $(this)
      const v = item[$this.attr('name')]
      if (v === true || v === false) $this.attr('checked', v)
    })
  },
}
