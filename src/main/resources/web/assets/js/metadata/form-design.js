/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global FIELD_TYPES, EasyFilter */

const wpc = window.__PageConfig
const DIVIDER_LINE = '$DIVIDER$'
const REFFORM_LINE = '$REFFORM$'

const COLSPANS = {
  1: 'w-25',
  2: 'w-50',
  3: 'w-75',
  4: 'w-100',
  9: 'w-33',
  8: 'w-66',
}

const _FieldLabelChanged = {}
const _FieldNullableChanged = {}
const _ValidFields = {}

$(document).ready(() => {
  $.get(`../list-field?refname=true&entity=${wpc.entityName}`, function (res) {
    const configFields = []
    $(wpc.formConfig.elements).each(function () {
      configFields.push(this.field)
    })

    $(res.data).each(function () {
      _ValidFields[this.fieldName] = this
      if (!configFields.includes(this.fieldName)) render_unset(this)
    })

    AdvControl.init()

    $(wpc.formConfig.elements).each(function () {
      const field = _ValidFields[this.field]
      if (this.field === DIVIDER_LINE) {
        render_item({ fieldName: this.field, fieldLabel: this.label || '', colspan: 4, collapsed: this.collapsed, breaked: this.breaked })
      } else if (this.field === REFFORM_LINE) {
        render_item({ fieldName: this.field, fieldLabel: this.label || '', colspan: 4, reffield: this.reffield, speclayout: this.speclayout })
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
        start: function (e, ui) {
          const wc = ui.item.attr('class').match(/\w-([0-9]{2,3})/gi)
          if (wc) ui.placeholder.addClass((wc[0] || '') + ' dd-item')
        },
      })
      .disableSelection()
  })

  $('.J_add-divider').on('click', () => {
    $('.nav-tabs-classic a[href="#form-design"]').tab('show')
    render_item({ fieldName: DIVIDER_LINE, fieldLabel: '', colspan: 4 })
  })

  $('.J_add-refform').on('click', () => {
    if (rb.commercial < 1) {
      RbHighbar.error(WrapHtml($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return false
    }
    $('.nav-tabs-classic a[href="#form-design"]').tab('show')
    render_item({ fieldName: REFFORM_LINE, fieldLabel: '', colspan: 4 })
  })

  // @see field-new.html
  // @see field-type.js
  for (let k in FIELD_TYPES) {
    const ft = FIELD_TYPES[k]
    const $type = render_type({ name: k, label: ft[0], icon: ft[1] })
    if (ft[2]) $type.addClass('bosskey-show')
  }

  // v3.7, v3.8
  $('.J_add-nform').on('click', () => {
    if (rb.commercial < 1) {
      RbHighbar.error(WrapHtml($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return false
    }
    renderRbcomp(<DlgNForm entity={wpc.entityName} />)
  })
  wpc.formsAttr &&
    wpc.formsAttr.forEach((item) => {
      const $item = $(`<a class="dropdown-item" href="?id=${item.id}"></a>`).appendTo('.form-action-menu')
      const $title = $(`<span>${item.name || $L('默认布局')}</span>`).appendTo($item)
      if (!item.name) $title.addClass('text-muted')

      const $action = $(`<div class="action"><span title="${$L('修改')}"><i class="zmdi zmdi-edit"></i></span></div>`).appendTo($item)
      $action.find('span').on('click', (e) => {
        $stopEvent(e, true)
        renderRbcomp(<DlgNForm entity={wpc.entityName} id={item.id} name={item.name} attrs={item.shareTo} />)
        $('.form-action-menu').prev().dropdown('toggle') // hide
      })

      if (rb.commercial < 1) $action.find('span').remove()
      if (wpc.formConfig.id === item.id) $item.addClass('check')
    })
  // 无
  if (!wpc.formConfig.id) {
    $(`<a class="dropdown-item text-disabled">${$L('无')}</a>`).appendTo('.form-action-menu')
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
      } else if (item.field === REFFORM_LINE) {
        item.colspan = 4
        item.label = $this.find('span').text() || ''
        item.reffield = $this.attr('data-reffield') || ''
        item.speclayout = $this.attr('data-speclayout') || ''
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

        if (_FieldLabelChanged[item.field]) item.__newLabel = _FieldLabelChanged[item.field]
        if (_FieldNullableChanged[item.field] !== undefined) item.__newNullable = _FieldNullableChanged[item.field]

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
    if (rb.commercial < 10) {
      e.preventDefault()
      RbHighbar.error(WrapHtml($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
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

  $('.J_resize-fields').on('click', () => {
    RbAlert.create(
      <RF>
        <strong>{$L('重置布局为')}</strong>
        <div className="mt-1">
          <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
            <input className="custom-control-input" name="resize_fields" type="radio" value="w-50" defaultChecked />
            <span className="custom-control-label">{$L('两栏')}</span>
          </label>
          <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
            <input className="custom-control-input" name="resize_fields" type="radio" value="w-33" />
            <span className="custom-control-label">{$L('三栏')}</span>
          </label>
          <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
            <input className="custom-control-input" name="resize_fields" type="radio" value="w-25" />
            <span className="custom-control-label">{$L('四栏')}</span>
          </label>
        </div>
      </RF>,
      {
        icon: 'unicon hide',
        onConfirm: function () {
          const w = $(this._element).find('input[type=radio]:checked').val()
          $('.form-preview .dd-item').each(function () {
            const $field = $(this).find('.J_field')
            if ($field.data('field') !== '$DIVIDER$' && $field.data('field') !== '$REFFORM$') {
              $field.removeAttr('data-height').parent().removeClass('w-25 w-50 w-75 w-100 w-33 w-66').addClass(w)
            }
          })
          this.hide()
        },
      }
    )
  })

  $('.J_copy-layout').on('click', () => {
    if (rb.commercial < 10) {
      RbHighbar.error(WrapHtml($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return false
    }
    renderRbcomp(<CopyLayoutTo layoutId={wpc.formConfig.id} formsAttr={wpc.formsAttr || []} />)
  })

  $('.J_del-unlayout-fields').on('click', () => {
    RbAlert.create($L('是否删除所有未布局字段？'), {
      type: 'danger',
      onConfirm: function () {
        this.disabled(true, true)
        const that = this

        let del = 0
        $('#FIELDLIST .dd-handle').each(function () {
          const $item = $(this)
          if ($item.hasClass('readonly') || $item.text().includes('SYS ')) return

          del++
          $.post(`/admin/entity/field-drop?id=${wpc.entityName}.${$item.data('field')}`, (res) => {
            if (res.error_code === 0) $item.parent().remove()

            if (--del <= 0) {
              RbHighbar.success($L('删除完成'))
              that.hide(true)
            }
          })
        })
        // No del?
        if (del === 0) that.hide(true)
      },
    })
  })
})

const render_item = function (data) {
  const $item = $('<div class="dd-item"></div>').appendTo('.form-preview')

  const colspan = data.isFull === true ? 4 : data.colspan || 2
  $item.addClass(COLSPANS[colspan])

  const isDivider = data.fieldName === DIVIDER_LINE
  const isRefform = data.fieldName === REFFORM_LINE
  const _title1 = isDivider ? $L('分栏') : isRefform ? $L('表单引用') : ''
  const _title2 = isDivider ? $L('断行') : ''

  const $handle = $(
    `<div class="dd-handle J_field" data-field="${data.fieldName}" data-label="${data.fieldLabel}"><span _title="${_title1}" _title2="${_title2}">${data.fieldLabel}</span></div>`
  ).appendTo($item)

  const $action = $('<div class="dd-action"></div>').appendTo($handle)
  // 字段
  if (data.displayType) {
    if (data.nullable === false) $handle.addClass('not-nullable')
    if (data.creatable === false) $handle.addClass('readonly')

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
          // 字段名称
          _FieldLabelChanged[nv.field] = nv.fieldLabel || null
          if (nv.fieldLabel) $item.find('.dd-handle>span').text(nv.fieldLabel)
          else $item.find('.dd-handle>span').text($item.find('.dd-handle').data('label'))

          // 允许为空
          _FieldNullableChanged[nv.field] = nv.fieldNullable ? true : false
          if (nv.fieldNullable) $item.find('.dd-handle').removeClass('not-nullable')
          else $item.find('.dd-handle').addClass('not-nullable')

          // 填写提示
          let $tip = $item.find('.dd-handle>span>i')
          if (!nv.fieldTips) {
            $tip.remove()
          } else {
            if (!$tip[0]) $tip = $('<i class="J_tip zmdi zmdi-info-outline"></i>').appendTo($item.find('.dd-handle>span'))
            $tip.attr('title', nv.fieldTips)
          }

          // NTEXT 高度
          if (data.displayTypeName === 'NTEXT') $item.find('.dd-handle').attr('data-height', nv.fieldHeight || '')
        }

        const ov = {
          fieldTips: $item.find('.dd-handle>span>i').attr('title'),
          fieldLabel: $item.find('.dd-handle>span').text(),
          fieldLabelOld: $item.find('.dd-handle').data('label'),
          fieldHeight: $item.find('.dd-handle').attr('data-height') || null,
          fieldNullable: !$item.find('.dd-handle').hasClass('not-nullable'),
          field: $item.find('.dd-handle').data('field'),
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

  // v35
  if (isRefform) {
    $item.addClass('refform')
    const $handle = $item.find('.dd-handle').attr({
      'data-reffield': data.reffield,
      'data-speclayout': data.speclayout,
    })

    $(`<a title="${$L('修改')}"><i class="zmdi zmdi-edit"></i></a>`)
      .appendTo($action)
      .on('click', () => {
        const _onConfirm = function (nv) {
          $item.find('.dd-handle span').text(nv.reffield ? _ValidFields[nv.reffield].fieldLabel : '')
          $handle.attr('data-reffield', nv.reffield || '')
          $handle.attr('data-speclayout', nv.speclayout || '')
        }

        const ov = {
          reffield: $handle.attr('data-reffield'),
          speclayout: $handle.attr('data-speclayout'),
        }
        renderRbcomp(<DlgEditRefform onConfirm={_onConfirm} {...ov} />)
      })

    $(`<a title="${$L('移除')}"><i class="zmdi zmdi-close"></i></a>`)
      .appendTo($action)
      .on('click', () => $item.remove())
  }
}

const render_unset = function (data) {
  const $item = $(`<li class="dd-item"><div class="dd-handle" data-field="${data.fieldName}">${data.fieldLabel}</div></li>`).appendTo('.field-list')
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
    // eslint-disable-next-line react/jsx-no-undef
    renderRbcomp(<FieldNew2 entity={wpc.entityName} fieldType={fieldType.name} />)
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
          <label>{$L('填写提示')}</label>
          <input type="text" className="form-control form-control-sm" name="fieldTips" value={this.state.fieldTips || ''} onChange={this.handleChange} placeholder={$L('无')} maxLength="200" />
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
          <a className="btn btn-link" href={`./field/${this.props.field}`} target="_blank">
            {$L('更多配置')}
          </a>
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
          <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mt-0 mb-0">
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

// 引用属性
class DlgEditRefform extends DlgEditField {
  constructor(props) {
    super(props)

    this.__FormsAttr = {}
    this.state.formsAttr = [{ id: null }]
  }

  renderContent() {
    return (
      <form className="field-attr">
        <div className="form-group">
          <label>{$L('引用字段')}</label>
          <select
            className="form-control form-control-sm"
            name="reffield"
            onChange={(e) => {
              this.handleChange(e)
              setTimeout(() => this._loadFormsAttr(), 200)
            }}
            defaultValue={this.props.reffield || null}
            ref={(c) => (this._$reffield = c)}>
            {Object.keys(_ValidFields).map((k) => {
              const field = _ValidFields[k]
              if (['REFERENCE', 'ANYREFERENCE'].includes(field.displayTypeName) && field.fieldName !== 'approvalId') {
                return (
                  <option key={field.fieldName} value={field.fieldName}>
                    {field.fieldLabel}
                  </option>
                )
              }
              return null
            })}
          </select>
        </div>
        <div className="form-group">
          <label>{$L('指定布局')}</label>
          <select className="form-control form-control-sm" name="speclayout" onChange={this.handleChange} ref={(c) => (this._$speclayout = c)}>
            <option value="N">{$L('自动')}</option>
            {this.state.formsAttr &&
              this.state.formsAttr.map((item) => {
                return (
                  <option key={item.id || 'N'} value={item.id || ''}>
                    {item.name || $L('默认布局')}
                  </option>
                )
              })}
          </select>
        </div>
        <div className="form-group mb-2">
          <button type="button" className="btn btn-primary" onClick={this._onConfirm}>
            {$L('确定')}
          </button>
        </div>
      </form>
    )
  }

  componentDidMount() {
    super.componentDidMount()
    // init
    if (this.props.reffield) {
      this._loadFormsAttr(true)
    } else {
      this.setState({ reffield: $(this._$reffield).val() }, () => this._loadFormsAttr(true))
    }
  }

  _loadFormsAttr(init) {
    const f = init ? this.props.reffield || this.state.reffield : this.state.reffield
    if (f) {
      const e = _ValidFields[f].displayTypeRef[0]
      if (this.__FormsAttr[e]) {
        this.setState({ formsAttr: this.__FormsAttr[e] })
      } else {
        $.get(`/admin/entity/${e}/get-forms-attr`, (res) => {
          this.__FormsAttr[e] = res.data || []
          this.setState({ formsAttr: this.__FormsAttr[e] }, () => {
            // init
            if (init && this.props.speclayout) {
              $(this._$speclayout).val(this.props.speclayout)
            }
          })
        })
      }
    }
  }
}

// 追加到布局
// eslint-disable-next-line no-unused-vars
const add2Layout = function (fieldName, dlg) {
  $.get(`../list-field?entity=${wpc.entityName}`, function (res) {
    $(res.data).each(function () {
      if (this.fieldName === fieldName) {
        render_item({ ...this, tip: this.tip || null })
        _ValidFields[fieldName] = this
        return false
      }
    })
  })
  dlg && dlg.hide()
}

// 高级控制
const AdvControl = {
  $tbody: $('#adv-control tbody'),

  init() {
    this._$template = this.$tbody.find('tr').html()
    this.$tbody.find('tr').remove()
  },

  set: function (field) {
    // comp:v3.9
    if (typeof field['displayOnCreate'] !== 'undefined') {
      if (field['displayOnCreate'] === false) field.hiddenOnCreate = true
      if (field['displayOnUpdate'] === false) field.hiddenOnUpdate = true
    }

    const $c = $(`<tr data-field="${field.fieldName}">${this._$template}</tr>`).appendTo(this.$tbody)
    $c.find('td:eq(0)').text(field.fieldLabel)

    // 隐藏
    const $show = $c.find('td:eq(1)')
    $show.find('>a').on('click', () => this._showEasyFilter(field.fieldName, 'hidden', $L('隐藏条件')))
    // 必填
    const $req = $c.find('td:eq(2)')
    if (field.builtin) $req.empty()
    else if ($L('二维码') === field.displayType || $L('自动编号') === field.displayType) {
      $req.find('input').attr({ disabled: true, checked: false })
      $req.find('>a').remove()
    } else if (!field.nullable) {
      $req.find('input').attr({ disabled: true, checked: true })
      $req.find('>a').remove()
    }
    $req.find('>a').on('click', () => this._showEasyFilter(field.fieldName, 'required', $L('必填条件')))
    // 只读
    const $ro = $c.find('td:eq(3)')
    if (field.builtin) $ro.empty()
    else {
      if (!field.creatable) $ro.find('input:eq(0)').attr({ disabled: true, checked: true })
      if (!field.updatable) $ro.find('input:eq(1)').attr({ disabled: true, checked: true })
      if (!field.creatable || !field.updatable) $ro.find('>a').remove()
    }
    $ro.find('>a').on('click', () => this._showEasyFilter(field.fieldName, 'readonly', $L('只读条件')))

    // init
    const $tr = this.$tbody.find(`tr[data-field="${field.fieldName}"]`)
    $tr.find('input').each(function () {
      const $this = $(this)
      if ($this.prop('disabled')) return
      const v = field[$this.attr('name')]
      if (v === true || v === false) $this.attr('checked', v)
    })
    // v4.0
    ;['hidden', 'required', 'readonly'].forEach((type) => {
      const s = field[type + 'OnEasyFilter']
      if (s) {
        const key = field.fieldName + '--' + type
        AdvControl._EasyFilters__data[key] = s
        $tr.find(`a.easy-control[data-type="${type}"]`).addClass('active')
      }
    })
  },

  cfgAppend: function (item) {
    const $tr = this.$tbody.find(`tr[data-field="${item.field}"]`)
    $tr.find('input').each(function () {
      const $this = $(this)
      if ($this.prop('disabled')) return
      item[$this.attr('name')] = $this.prop('checked')
    })
    // v4.0
    $tr.find('a.easy-control.active').each(function () {
      const type = $(this).data('type')
      const key = item.field + '--' + type
      if (AdvControl._EasyFilters__data[key]) item[type + 'OnEasyFilter'] = AdvControl._EasyFilters__data[key]
    })
  },

  _EasyFilters: {},
  _EasyFilters__data: {},
  _showEasyFilter: function (field, type, title) {
    const key = field + '--' + type
    if (this._EasyFilters[key]) {
      this._EasyFilters[key].show()
    } else {
      renderRbcomp(
        <EasyFilter
          inModal
          inEasyFilter
          canNoFilters
          fsDeep={1}
          entity={wpc.entityName}
          title={title}
          filter={this._EasyFilters__data[key] || null}
          onConfirm={(s) => {
            this._EasyFilters__data[key] = s
            const $t = $(`#adv-control tr[data-field="${field}"]`).find(`a.easy-control[data-type="${type}"]`)
            if (s.items.length === 0) {
              $t.removeClass('active')
              delete this._EasyFilters__data[key]
            } else {
              $t.addClass('active')
            }
          }}
          hasTip={$L('选择的条件字段需布局在表单中')}
        />,
        function () {
          AdvControl._EasyFilters[key] = this
        }
      )
    }
  },
}

// ~~ 新的表单布局
class DlgNForm extends RbModalHandler {
  constructor(props) {
    super(props)
    this.state = { ...props }

    if (props.attrs === 'ALL' && !props.name) {
      this.state.fallback = true
      this.state.fornew = true
      this._name = $L('默认布局')
    } else if (typeof props.attrs === 'object') {
      this.state.fallback = props.attrs.fallback
      this.state.fornew = props.attrs.fornew
      this.state.useFilter = props.attrs.filter || null
    }
  }

  render() {
    const title = this.props.id ? $L('修改表单布局') : $L('添加表单布局')
    return (
      <RbModal ref={(c) => (this._dlg = c)} title={title} disposeOnHide>
        <div>
          <form>
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('名称')}</label>
              <div className="col-sm-7">
                <input className="form-control form-control-sm" type="text" maxLength="40" defaultValue={this.props.name} placeholder={this._name || ''} ref={(c) => (this._$name = c)} />
              </div>
            </div>
            <div className="form-group row pt-1 pb-1">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('使用条件')}</label>
              <div className="col-sm-7">
                <div className="form-control-plaintext">
                  <a
                    href="###"
                    onClick={(e) => {
                      $stopEvent(e, true)
                      this._handleUseFilter()
                    }}
                    ref={(c) => (this._$useFilter = c)}>
                    {this.state.useFilter && this.state.useFilter.items.length > 0 ? $L('已设置条件') + ` (${this.state.useFilter.items.length})` : $L('点击设置')}
                  </a>
                  <p className="form-text m-0 mt-1">{$L('符合条件的表单将使用此布局')}</p>
                </div>
              </div>
            </div>
            <div className="form-group row pt-0">
              <label className="col-sm-3 col-form-label text-sm-right"></label>
              <div className="col-sm-7">
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                  <input className="custom-control-input" type="checkbox" defaultChecked={this.state.fallback} ref={(c) => (this._$fallback = c)} />
                  <span className="custom-control-label">{$L('默认布局')}</span>
                </label>
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                  <input className="custom-control-input" type="checkbox" defaultChecked={this.state.fornew} ref={(c) => (this._$fornew = c)} />
                  <span className="custom-control-label">{$L('可用于新建')}</span>
                </label>
              </div>
            </div>

            {this.state.detailsFromsAttr && (
              <div className="form-group row">
                <label className="col-sm-3 col-form-label text-sm-right">{$L('指定明细实体布局')}</label>
                <div className="col-sm-8" ref={(c) => (this._$detailsFromsAttr = c)}>
                  {this.state.detailsFromsAttr.map((de) => {
                    return (
                      <div className="row mb-2" key={de[0]}>
                        <div className="col-8">
                          <select className="form-control form-control-sm" name={de[0]}>
                            <option value="0">{$L('自动')}</option>
                            {de[2].map((item) => {
                              return (
                                <option value={item.id} key={item.id}>
                                  {item.name || $L('默认布局')}
                                </option>
                              )
                            })}
                          </select>
                        </div>
                        <div className="col-4 pl-0 pr-0 text-bold" style={{ paddingTop: 8 }}>
                          {de[1]}
                        </div>
                      </div>
                    )
                  })}
                </div>
              </div>
            )}

            <div className="form-group row footer">
              <div className="col-sm-7 offset-sm-3" ref={(c) => (this._$btns = c)}>
                <button className="btn btn-primary" type="button" onClick={() => this.postAttr()}>
                  {$L('确定')}
                </button>
                {this.props.id && (
                  <button className="btn btn-danger btn-outline ml-2" type="button" onClick={() => this.delete()}>
                    <i className="zmdi zmdi-delete icon" /> {$L('删除')}
                  </button>
                )}
                {!this.props.id && (
                  <button className="btn btn-link" type="button" onClick={() => this.hide()}>
                    {$L('取消')}
                  </button>
                )}
              </div>
            </div>
          </form>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    // super.componentDidMount()

    if (!wpc.mainEntityName) {
      $.get(`/admin/entity/${wpc.entityName}/get-details-forms-attr`, (res) => {
        this.setState({ detailsFromsAttr: res.data || {} }, () => {
          const detailsFromsAttr = (this.props.attrs || {}).detailsFromsAttr
          if (detailsFromsAttr) {
            $(this._$detailsFromsAttr)
              .find('select')
              .each(function () {
                let name = $(this).attr('name')
                $(this).val(detailsFromsAttr[name] || '0')
              })
          }
        })
      })
    }
  }

  _handleUseFilter() {
    if (this._UseFilter) {
      this._UseFilter.show()
    } else {
      const that = this
      renderRbcomp(
        <AdvFilter
          title={$L('使用条件')}
          inModal
          canNoFilters
          entity={this.props.entity}
          filter={this.state.useFilter}
          confirm={(s) => {
            this.setState({ useFilter: s })
          }}
        />,
        function () {
          that._UseFilter = this
        }
      )
    }
  }
  postAttr() {
    let detailsFromsAttr = {}
    if (this._$detailsFromsAttr) {
      $(this._$detailsFromsAttr)
        .find('select')
        .each(function () {
          let v = $(this).val()
          if (v !== '0') detailsFromsAttr[$(this).attr('name')] = v
        })
    }

    const ps = {
      name: $val(this._$name),
      filter: this.state.useFilter || null,
      fallback: $val(this._$fallback),
      fornew: $val(this._$fornew),
      detailsFromsAttr: Object.keys(detailsFromsAttr).length === 0 ? null : detailsFromsAttr,
    }
    if (!ps.name) {
      return RbHighbar.createl('请输入名称')
    }
    if (!ps.fallback) {
      if (!ps.filter || ps.filter.items.length === 0) return RbHighbar.createl('非默认布局请设置使用条件')
    }

    const $btn = $(this._$btns).button('loading')
    $.post(`form-attr-save?id=${this.props.id || ''}`, JSON.stringify(ps), (res) => {
      if (res.error_code === 0) {
        location.href = '?id=' + res.data
      } else {
        RbHighbar.error(res.error_msg)
        $btn.button('reset')
      }
    })
  }

  delete() {
    const _id = this.props.id
    RbAlert.create($L('确认删除此表单布局？'), {
      type: 'danger',
      onConfirm: function () {
        this.hide()
        $.post('/app/entity/common-delete?id=' + _id, (res) => {
          if (res.error_code === 0) {
            location.href = './form-design'
          } else {
            RbHighbar.error(res.error_msg)
          }
        })
      },
    })
  }
}

// 复制布局
class CopyLayoutTo extends RbModalHandler {
  render() {
    return (
      <RbModal title={$L('复制布局')} ref={(c) => (this._dlg = c)} disposeOnHide>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('复制到哪些布局')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" multiple ref={(c) => (this._$copyTo = c)}>
                {this.props.formsAttr.map((item) => {
                  if (item.id === this.props.layoutId) return null
                  return (
                    <option key={item.id} value={item.id}>
                      {item.name || $L('默认布局')}
                    </option>
                  )
                })}
              </select>
              <p className="form-text">{$L('将当前布局复制到选择的布局中')}</p>
            </div>
          </div>
        </div>
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3">
            <button className="btn btn-primary" type="button" onClick={() => this.submit()} ref={(c) => (this._$btn = c)}>
              {$L('复制')}
            </button>
            <a className="btn btn-link" onClick={this.hide}>
              {$L('取消')}
            </a>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    $(this._$copyTo).select2({
      placeholder: $L('请选择'),
      allowClear: false,
    })
  }

  submit() {
    if (!this.props.layoutId) {
      return RbHighbar.create($L('请先保存布局'))
    }

    const post = {
      from: this.props.layoutId,
      copyTo: $(this._$copyTo).val(),
    }
    if ((post.copyTo || []).length === 0) return RbHighbar.create($L('请选择复制到哪些布局'))

    const that = this
    RbAlert.create($L('选择布局的原有配置会被覆盖。确定复制吗？'), {
      onConfirm: function () {
        this.hide()

        const $btn = $(that._$btn).button('loading')
        $.post('form-copyto', JSON.stringify(post), (res) => {
          if (res.error_code === 0) {
            RbHighbar.success($L('复制完成'))
            setTimeout(() => that.hide(), 1000)
          } else {
            RbHighbar.error(res.error_msg)
            $btn.button('reset')
          }
        })
      },
    })
  }
}
