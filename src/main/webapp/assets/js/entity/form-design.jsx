const wpc = window.__PageConfig
$(document).ready(function () {
  $.get('../list-field?entity=' + wpc.entityName, function (res) {
    let validFields = {}, configFields = []
    $(wpc.formConfig.elements).each(function () { configFields.push(this.field) })
    $(res.data).each(function () {
      validFields[this.fieldName] = this
      if (configFields.contains(this.fieldName) === false) render_unset(this, '.field-list')
    })

    $(wpc.formConfig.elements).each(function () {
      let field = validFields[this.field]
      if (this.field === '$DIVIDER$') {
        render_item({ fieldName: this.field, fieldLabel: this.label || '分栏', isFull: true }, '.form-preview')
      } else if (!field) {
        let item = $('<div class="dd-item"><div class="dd-handle J_field J_missed"><span class="text-danger">[' + this.field + '] 字段已被删除</span></div></div>').appendTo('.form-preview')
        let action = $('<div class="dd-action"><a>[移除]</a></div>').appendTo(item.find('.dd-handle'))
        action.find('a').click(function () {
          item.remove()
          check_empty()
        })
      } else {
        render_item({ ...field, isFull: this.isFull || false, tip: this.tip || null }, '.form-preview')
      }
    })

    check_empty()
    $('.form-preview').sortable({
      cursor: 'move',
      placeholder: 'dd-placeholder',
      cancel: '.nodata',
      stop: check_empty
    }).disableSelection()
  })

  $('.J_add-divider').click(function () {
    render_item({ fieldName: '$DIVIDER$', fieldLabel: '分栏', isFull: true }, '.form-preview')
  })

  $('.J_save').click(function () {
    let elements = []
    $('.form-preview .J_field').each(function () {
      let $this = $(this)
      if (!$this.data('field')) return
      let item = { field: $this.data('field') }
      if (item.field === '$DIVIDER$') {
        item.isFull = true
        item.label = $this.find('span').text()
      } else {
        item.isFull = $this.parent().hasClass('w-100')
        let tip = $this.find('.J_tip').attr('title')
        if (tip) item.tip = tip
      }
      elements.push(item)
    })
    if (elements.length === 0) { rb.highbar('请至少布局1个字段'); return }

    let _data = { belongEntity: wpc.entityName, applyType: 'FORM', config: JSON.stringify(elements) }
    _data.metadata = { entity: 'LayoutConfig', id: wpc.formConfig.id || null }

    $(this).button('loading')
    $.post('form-update', JSON.stringify(_data), function (res) {
      if (res.error_code === 0) location.reload()
      else rb.hberror(res.error_msg)
    })
  })

  $(window).resize(() => {
    $setTimeout(() => {
      $('.field-aside .rb-scroller').height($(window).height() - 123)
    }, 200, 'FeildAslide-resize')
  }).trigger('resize')
})
const render_item = function (data) {
  const item = $('<div class="dd-item"></div>').appendTo('.form-preview')
  if (data.isFull === true) item.addClass('w-100')

  let handle = $('<div class="dd-handle J_field" data-field="' + data.fieldName + '"><span>' + data.fieldLabel + '</span></div>').appendTo(item)
  if (data.creatable === false) handle.addClass('readonly')
  else if (data.nullable === false) handle.addClass('not-nullable')

  if (data.tip) {
    let tip = $('<i class="J_tip zmdi zmdi-info-outline"></i>').appendTo(handle.find('span'))
    tip.attr('title', data.tip)
  }

  let action = $('<div class="dd-action"></div>').appendTo(handle)
  if (data.displayType) {
    $('<span class="ft">' + data.displayType.split('(')[0].trim() + '</span>').appendTo(item)
    $('<a class="rowspan" title="双列">[双]</a>').appendTo(action).click(function () { item.removeClass('w-100') })
    $('<a class="rowspan" title="单列">[单]</a>').appendTo(action).click(function () { item.addClass('w-100') })
    $('<a>[修改]</a>').appendTo(action).click(function () {
      let call = function (nv) {
        let tip = item.find('.dd-handle span>i')
        if (!nv) {
          tip.remove()
        } else {
          if (tip.length === 0) tip = $('<i class="J_tip zmdi zmdi-info-outline"></i>').appendTo(item.find('.dd-handle span'))
          tip.attr('title', nv)
        }
      }
      renderRbcomp(<DlgEditField call={call} value={item.find('.dd-handle span>i').attr('title')} />)
    })
    $('<a>[移除]</a>').appendTo(action).click(function () {
      render_unset(data)
      item.remove()
      check_empty()
    })
  }

  if (data.fieldName === '$DIVIDER$') {
    item.addClass('divider')
    $('<a>[修改]</a>').appendTo(action).click(function () {
      let call = function (nv) {
        item.find('.dd-handle span').text(nv || '分栏')
      }
      renderRbcomp(<DlgEditDivider call={call} value={item.find('.dd-handle span').text()} />)
    })
    $('<a>[移除]</a>').appendTo(action).click(function () {
      item.remove()
      check_empty()
    })
  }
}
const render_unset = function (data) {
  let item = $('<li class="dd-item"><div class="dd-handle">' + data.fieldLabel + '</div></li>').appendTo('.field-list')
  $('<span class="ft">' + data.displayType.split('(')[0].trim() + '</span>').appendTo(item)
  if (data.creatable === false) item.find('.dd-handle').addClass('readonly')
  else if (data.nullable === false) item.find('.dd-handle').addClass('not-nullable')
  item.click(function () {
    render_item(data)
    item.remove()
    check_empty()
  })
  return item
}
const check_empty = function () {
  if ($('.field-list .dd-item').length === 0) $('.field-list .nodata').show()
  else $('.field-list .nodata').hide()
  if ($('.form-preview .dd-item').length === 0) $('.form-preview .nodata').show()
  else $('.form-preview .nodata').hide()
}

class DlgEditField extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }
  render() {
    return (
      <div className="modal" ref={(c) => this._dlg = c} tabIndex="-1">
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content field-edit">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()}><span className="zmdi zmdi-close" /></button>
            </div>
            <div className="modal-body">
              {this.renderComp()}
            </div>
          </div>
        </div>
      </div>
    )
  }
  renderComp() {
    return (
      <form>
        <div className="form-group">
          <label>修改填写提示</label>
          <input type="text" className="form-control form-control-sm" ref={(c) => this._value = c} placeholder="输入填写提示" />
        </div>
        <div className="form-group mb-1">
          <button type="button" className="btn btn-space btn-primary" onClick={() => this.confirm()}>确定</button>
        </div>
      </form>
    )
  }
  confirm() {
    let val = $val(this._value)
    // eslint-disable-next-line react/prop-types
    typeof this.props.call === 'function' && this.props.call(val)
    this.hide()
  }
  componentDidMount() {
    this.show()
    // eslint-disable-next-line react/prop-types
    if (this.props.value) $(this._value).val(this.props.value)
  }
  hide() {
    $(this._dlg).modal('hide')
  }
  show() {
    $(this._dlg).modal({ show: true, keyboard: true })
  }
}

class DlgEditDivider extends DlgEditField {
  constructor(props) {
    super(props)
  }
  renderComp() {
    return (
      <form>
        <div className="form-group">
          <label>修改分栏名称</label>
          <input type="text" className="form-control form-control-sm" ref={(c) => this._value = c} placeholder="输入分栏线名称" />
        </div>
        <div className="form-group mb-1">
          <button type="button" className="btn btn-space btn-primary" onClick={() => this.confirm()}>确定</button>
        </div>
      </form>
    )
  }
}