/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const _ENTITIES = {}
$(document).ready(() => {
  $.get('/commons/metadata/entities?detail=true', (res) => {
    $(res.data).each(function () {
      $(`<option value="${this.name}">${this.label}</option>`).appendTo('#belongEntity')
      _ENTITIES[this.name] = this.label
    })

    renderRbcomp(<DataList />, 'react-list')
  })
})

// 列表配置
const ListConfig = {
  entity: 'RecycleBin',
  fields: [
    { field: 'belongEntity', label: $L('BelongEntity'), unsort: true },
    { field: 'recordName', label: $L('RecordName'), width: 300 },
    { field: 'deletedOn', label: $L('DeletedOn'), type: 'DATETIME' },
    { field: 'deletedBy.fullName', label: $L('DeletedBy') },
    { field: 'channelWith', label: $L('DeleteChannel'), unsort: true },
    { field: 'recordId', label: $L('RecordId'), unsort: true },
  ],
  sort: 'deletedOn:desc',
}

class DataList extends React.Component {
  constructor(props) {
    super(props)
  }

  render() {
    return <RbList ref={(c) => (this._List = c)} config={ListConfig}></RbList>
  }

  componentDidMount() {
    const select2 = $('#belongEntity')
      .select2({
        placeholder: $L('SelectSome,Entity'),
        width: 220,
        allowClear: false,
      })
      .val('$ALL$')
      .trigger('change')
    select2.on('change', () => this.queryList())

    const $btn = $('.input-search .btn'),
      $input = $('.input-search input')
    $btn.click(() => this.queryList())
    $input.keydown((e) => (e.which === 13 ? $btn.trigger('click') : true))

    this._belongEntity = select2
    this._recordName = $input

    $('.J_restore').click(() => this.restore())
  }

  queryList() {
    let e = this._belongEntity.val(),
      n = this._recordName.val()
    if (e === '$ALL$') e = null

    const qs = []
    if (e) qs.push({ field: 'belongEntity', op: 'EQ', value: e })
    if (n) {
      if ($regex.isId(n)) qs.push({ field: 'recordId', op: 'EQ', value: n })
      else qs.push({ field: 'recordName', op: 'LK', value: n })
    }
    const q = {
      entity: 'RecycleBin',
      equation: 'AND',
      items: qs,
    }
    this._List.search(JSON.stringify(q))
  }

  restore() {
    const ids = this._List.getSelectedIds()
    if (!ids || ids.length === 0) return

    const cont =
      `<div class="text-bold mb-2">${$L('RestoreConfirm').replace('%d', ids.length)}</div>` +
      '<label class="custom-control custom-control-sm custom-checkbox custom-control-inline mb-2">' +
      '<input class="custom-control-input" type="checkbox">' +
      `<span class="custom-control-label">${$L('RestoreCasTips')}</span>` +
      '</label>'

    const that = this
    RbAlert.create(cont, {
      html: true,
      confirm: function () {
        this.disabled(true)
        let c = $(this._dlg).find('input').prop('checked')
        $.post(`/admin/audit/recycle-bin/restore?cascade=${c}&ids=${ids.join(',')}`, (res) => {
          this.hide()
          this.disabled()
          if (res.error_code === 0 && res.data.restored > 0) {
            RbHighbar.success($L('RestoreSuccessTips').replace('%d', res.data.restored))
            that.queryList()
          } else {
            RbHighbar.error(res.error_code > 0 ? res.error_msg : $L('RestoreFailedTips'))
          }
        })
      },
    })
  }
}

// eslint-disable-next-line react/display-name
CellRenders.renderSimple = function (v, s, k) {
  if (k.endsWith('.channelWith')) {
    v = v ? (
      <React.Fragment>
        {$L('CasDelete')}
        <span className="badge text-id ml-1" title={$L('CasMainId')}>
          {v.id}
        </span>
      </React.Fragment>
    ) : (
      $L('DirectDelete')
    )
  } else if (k.endsWith('.recordId')) {
    v = <span className="badge text-id">{v.id}</span>
  } else if (k.endsWith('.belongEntity')) {
    v = _ENTITIES[v] || `[${v.toUpperCase()}]`
  }

  return (
    <td key={k}>
      <div style={s}>{v || ''}</div>
    </td>
  )
}
