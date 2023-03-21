/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global FIELD_TYPES */

const wpc = window.__PageConfig
const DIVIDER_LINE = '$DIVIDER$'
const COLSPANS = {
  1: 'w-25',
  2: 'w-50',
  3: 'w-75',
  4: 'w-100',
  9: 'w-33',
  8: 'w-66',
}

$(document).ready(() => {
  $.get(`../list-field?entity=${wpc.entityName}`, function (res) {
    const validFields = {},
      configFields = []
    $(wpc.formConfig.elements).each(function () {
      configFields.push(this.field)
    })

    $(res.data).each(function () {
      validFields[this.fieldName] = this
      if (!configFields.includes(this.fieldName)) render_unset(this)
    })

    AdvControl.init()

    $(wpc.formConfig.elements).each(function () {
      const field = validFields[this.field]
      if (this.field === DIVIDER_LINE) {
        render_item({ fieldName: this.field, fieldLabel: this.label || '', colspan: 4, collapsed: this.collapsed, breaked: this.breaked })
      } else if (!field) {
        const $item = $(`<div class="dd-item"><div class="dd-handle J_field J_missed"><span class="text-danger">[${this.field.toUpperCase()}] ${$L('字段已删除')}</span></div></div>`).appendTo(
          '.form-preview'
        )
        const $action = $('<div class="dd-action"><a><i class="zmdi zmdi-close"></i></a></div>').appendTo($item.find('.dd-handle'))
        $action.find('a').on('click', function () {
          $item.remove()
        })
      } else {
        render_item({ ...field, ...this })
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

  $('.J_add-divider').on('click', () => {
    $('.nav-tabs-classic a[href="#form-design"]').tab('show')
    render_item({ fieldName: DIVIDER_LINE, fieldLabel: '', colspan: 4 })
  })

  // @see field-new.html
  // @see field-type.js
  for (let k in FIELD_TYPES) {
    const ft = FIELD_TYPES[k]
    if (!ft[2]) render_type({ name: k, label: ft[0], icon: ft[1] })
  }

  // SAVE

  const _handleSave = function (elements) {
    const data = {
      config: JSON.stringify(elements),
      metadata: {
        entity: 'LayoutConfig',
        id: wpc.formConfig.id || null,
      },
    }
    // New
    if (!wpc.formConfig.id) {
      data.belongEntity = wpc.entityName
      data.applyType = 'FORM'
    }

    $('.J_save').button('loading')
    $.post('form-update', JSON.stringify(data), function (res) {
      if (res.error_code === 0) location.reload()
      else RbHighbar.error(res.error_msg)
    })
  }

  $('.J_save').on('click', function () {
    const formElements = []
    $('.form-preview .J_field').each(function () {
      const $this = $(this)
      if (!$this.data('field')) return

      const item = { field: $this.data('field') }
      if (item.field === DIVIDER_LINE) {
        item.colspan = 4
        item.label = $this.find('span').text() || ''
        item.collapsed = $isTrue($this.attr('data-collapsed'))
        item.breaked = $isTrue($this.attr('data-breaked'))
      } else {
        item.colspan = 2 // default
        if ($this.parent().hasClass('w-100')) item.colspan = 4
        if ($this.parent().hasClass('w-75')) item.colspan = 3
        if ($this.parent().hasClass('w-25')) item.colspan = 1
        if ($this.parent().hasClass('w-33')) item.colspan = 9
        if ($this.parent().hasClass('w-66')) item.colspan = 8

        const tip = $this.find('.J_tip').attr('title')
        if (tip) item.tip = tip
        const height = $this.attr('data-height')
        if (height) item.height = height
        item.__newLabel = $this.find('span').text() || null
        item.__newNullable = !$this.hasClass('not-nullable')

        AdvControl.cfgAppend(item)
      }
      formElements.push(item)
    })

    if (formElements.length === 0) return RbHighbar.create($L('请至少布局 1 个字段'))

    if ($('.field-list .not-nullable').length > 0) {
      RbAlert.create($L('有必填字段未被布局，这可能导致新建记录失败。是否仍要保存？'), {
        type: 'warning',
        confirmText: $L('保存'),
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
      RbHighbar.error(WrapHtml($L('免费版不支持高级控制功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
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

  const colspan = data.isFull === true ? 4 : data.colspan || 2
  $item.addClass(COLSPANS[colspan])

  const isDivider = data.fieldName === DIVIDER_LINE

  const $handle = $(
    `<div class="dd-handle J_field" data-field="${data.fieldName}" data-label="${data.fieldLabel}"><span _title="${isDivider ? $L('分栏') : 'FIELD'}">${data.fieldLabel}</span></div>`
  ).appendTo($item)

  const $action = $('<div class="dd-action"></div>').appendTo($handle)
  // 字段
  if (data.displayType) {
    if (data.creatable === false) $handle.addClass('readonly')
    else if (data.nullable === false) $handle.addClass('not-nullable')

    // 填写提示
    if (data.tip) $('<i class="J_tip zmdi zmdi-info-outline"></i>').appendTo($handle.find('span')).attr('title', data.tip)
    // 长文本高度
    if (data.height) $handle.attr('data-height', data.height)

    $(`<span class="ft">${data.displayType}</span>`).appendTo($item)
    $(`<a class="mr-1 colspan" title="${$L('宽度')}" data-toggle="dropdown"><i class="zmdi zmdi-view-column"></i></a>`).appendTo($action)

    const $colspan = $('<div class="dropdown-menu dropdown-menu-right"></div>').appendTo($action)
    $('<a data-colspan="1" title="4"></a>').appendTo($colspan)
    $('<a data-colspan="9" title="3"></a>').appendTo($colspan)
    $('<a data-colspan="2" title="2"></a>').appendTo($colspan)
    $('<a data-colspan="4" title="1"></a>').appendTo($colspan)
    $('<a data-colspan="3" title="3/4" class="text-right"></a>').appendTo($colspan)
    $('<a data-colspan="8" title="2/3" class="text-right"></a>').appendTo($colspan)

    $colspan.find('a').on('click', function () {
      const colspan = ~~$(this).data('colspan')
      $item.removeClass('w-25 w-50 w-75 w-100 w-33 w-66').addClass(COLSPANS[colspan])
    })

    $(`<a title="${$L('修改')}"><i class="zmdi zmdi-edit"></i></a>`)
      .appendTo($action)
      .on('click', function () {
        const _onConfirm = function (nv) {
          // 填写提示
          let $tip = $item.find('.dd-handle>span>i')
          if (!nv.fieldTips) {
            $tip.remove()
          } else {
            if ($tip.length === 0) $tip = $('<i class="J_tip zmdi zmdi-info-outline"></i>').appendTo($item.find('.dd-handle span'))
            $tip.attr('title', nv.fieldTips)
          }

          // NTEXT 高度
          if (data.displayTypeName === 'NTEXT') $item.find('.dd-handle').attr('data-height', nv.fieldHeight || '')

          // 字段名称
          if (nv.fieldLabel) $item.find('.dd-handle>span').text(nv.fieldLabel)
          else $item.find('.dd-handle>span').text($item.find('.dd-handle').data('label'))

          // 允许为空
          if (nv.fieldNullable) $item.find('.dd-handle').removeClass('not-nullable')
          else $item.find('.dd-handle').addClass('not-nullable')
        }

        const ov = {
          fieldTips: $item.find('.dd-handle>span>i').attr('title'),
          fieldLabel: $item.find('.dd-handle>span').text(),
          fieldLabelOld: $item.find('.dd-handle').data('label'),
          fieldHeight: $item.find('.dd-handle').attr('data-height') || null,
          fieldNullable: !$item.find('.dd-handle').hasClass('not-nullable'),
        }
        // if (ov.fieldLabelOld === ov.fieldLabel) ov.fieldLabel = null

        renderRbcomp(<DlgEditField onConfirm={_onConfirm} {...ov} displayType={data.displayTypeName} />)
      })

    $(`<a title="${$L('移除')}"><i class="zmdi zmdi-close"></i></a>`)
      .appendTo($action)
      .on('click', function () {
        render_unset(data)
        $item.remove()
      })

    AdvControl.set({ ...data })
  }

  if (isDivider) {
    $item.addClass('divider')
    const $handle = $item.find('.dd-handle').attr({
      'data-collapsed': data.collapsed,
      'data-breaked': data.breaked,
    })

    $(`<a title="${$L('修改')}"><i class="zmdi zmdi-edit"></i></a>`)
      .appendTo($action)
      .on('click', () => {
        const _onConfirm = function (nv) {
          $item.find('.dd-handle span').text(nv.dividerName || '')
          $handle.attr('data-collapsed', $isTrue(nv.collapsed))
          $handle.attr('data-breaked', $isTrue(nv.breaked))
        }

        const ov = {
          dividerName: $item.find('.dd-handle span').text() || '',
          collapsed: $handle.attr('data-collapsed'),
          breaked: $handle.attr('data-breaked'),
        }
        renderRbcomp(<DlgEditDivider onConfirm={_onConfirm} {...ov} />)
      })

    $(`<a title="${$L('移除')}"><i class="zmdi zmdi-close"></i></a>`)
      .appendTo($action)
      .on('click', () => $item.remove())
  }
}

const render_unset = function (data) {
  const $item = $(`<li class="dd-item"><div class="dd-handle">${data.fieldLabel}</div></li>`).appendTo('.field-list')
  $(`<span class="ft">${data.displayType}</span>`).appendTo($item)
  if (data.creatable === false) $item.find('.dd-handle').addClass('readonly')
  else if (data.nullable === false) $item.find('.dd-handle').addClass('not-nullable')

  $item.on('click', function () {
    $('.nav-tabs-classic a[href="#form-design"]').tab('show')
    render_item(data)
    $item.remove()
  })
  return $item
}

const render_type = function (fieldType) {
  const $item = $(`<li class="dd-item"><div class="dd-handle"><i class="icon mdi ${fieldType.icon || 'mdi-form-textbox'}"></i> ${$L(fieldType.label)}</div></li>`).appendTo('.type-list')
  $item.on('click', function () {
    if (wpc.isSuperAdmin) RbModal.create(`/p/admin/metadata/field-new?entity=${wpc.entityName}&type=${fieldType.name}`, $L('添加字段'), { disposeOnHide: true })
    else RbHighbar.error($L('仅超级管理员可添加字段'))
  })
  return $item
}

// 字段属性
class DlgEditField extends RbAlert {
  constructor(props) {
    super(props)
    this.state = { ...props }

    console.log(props)
  }

  renderContent() {
    return (
      <form className="field-attr">
        <div className="form-group">
          <label>{$L('填写提示')}</label>
          <input
            type="text"
            className="form-control form-control-sm"
            name="fieldTips"
            value={this.state.fieldTips || ''}
            onChange={this.handleChange}
            placeholder={$L('输入填写提示')}
            maxLength="200"
          />
        </div>
        {this.props.displayType === 'NTEXT' && (
          <div className="form-group">
            <label>{$L('高度 (行数)')}</label>
            <input type="number" className="form-control form-control-sm" name="fieldHeight" value={this.state.fieldHeight || ''} onChange={this.handleChange} placeholder={$L('默认')} />
          </div>
        )}
        <div className="form-group">
          <label>{$L('字段名称')}</label>
          <input
            type="text"
            className="form-control form-control-sm"
            name="fieldLabel"
            value={this.state.fieldLabel || ''}
            onChange={this.handleChange}
            placeholder={this.props.fieldLabelOld || $L('修改字段名称')}
            maxLength="100"
          />
        </div>
        <div className="form-group">
          <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mt-0 mb-0">
            <input className="custom-control-input" type="checkbox" defaultChecked={this.props.fieldNullable} name="fieldNullable" onChange={this.handleChange} />
            <span className="custom-control-label">{$L('允许为空')}</span>
          </label>
        </div>
        <div className="form-group mb-2">
          <button type="button" className="btn btn-primary" onClick={this._onConfirm}>
            {$L('确定')}
          </button>
        </div>
      </form>
    )
  }

  handleChange = (e) => {
    const target = e.target
    const s = {}
    s[target.name] = target.type === 'checkbox' ? target.checked : target.value
    this.setState(s)
  }

  _onConfirm = () => {
    typeof this.props.onConfirm === 'function' && this.props.onConfirm(this.state || {})
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
          <label>{$L('分栏名称')}</label>
          <input type="text" className="form-control form-control-sm" name="dividerName" value={this.state.dividerName || ''} onChange={this.handleChange} placeholder={$L('输入分栏名称')} />
        </div>
        <div className="form-group">
          <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mt-0 mb-0">
            <input className="custom-control-input" type="checkbox" defaultChecked={$isTrue(this.props.collapsed)} name="collapsed" onChange={this.handleChange} />
            <span className="custom-control-label">{$L('默认收起')}</span>
          </label>
          <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mt-0 mb-0 bosskey-show">
            <input className="custom-control-input" type="checkbox" defaultChecked={$isTrue(this.props.breaked)} name="breaked" onChange={this.handleChange} />
            <span className="custom-control-label">{$L('仅用于断行')}</span>
          </label>
        </div>
        <div className="form-group mb-2">
          <button type="button" className="btn btn-primary" onClick={this._onConfirm}>
            {$L('确定')}
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
        render_item({ ...this, tip: this.tip || null })
        return false
      }
    })
  })

  RbModal.hide()
}

// 高级控制
const AdvControl = {
  $tbody: $('#adv-control tbody'),

  init() {
    this._template = this.$tbody.find('tr').html()
    this.$tbody.find('tr').remove()
  },

  set: function (field) {
    const $c = $(`<tr data-field="${field.fieldName}">${this._template}</tr>`).appendTo(this.$tbody)
    $c.find('td:eq(0)').text(field.fieldLabel)
    const $req = $c.find('td:eq(2)')
    if (field.builtin) $req.empty()
    else if (!field.nullable) $req.find('input').attr({ disabled: true, checked: true })

    this.$tbody.find(`tr[data-field="${field.fieldName}"] input`).each(function () {
      const $this = $(this)
      if ($this.prop('disabled')) return
      const v = field[$this.attr('name')]
      if (v === true || v === false) $this.attr('checked', v)
    })
  },

  cfgAppend: function (item) {
    this.$tbody.find(`tr[data-field="${item.field}"] input`).each(function () {
      const $this = $(this)
      if ($this.prop('disabled')) return
      item[$this.attr('name')] = $this.prop('checked')
    })
  },
}
