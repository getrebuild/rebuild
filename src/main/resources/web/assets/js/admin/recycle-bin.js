/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const _ENTITIES = {
  'Feeds': $L('动态'),
  'ProjectTask': $L('任务'),
}

$(document).ready(() => {
  $.get('/commons/metadata/entities?detail=true', (res) => {
    res.data && res.data.forEach((item) => (_ENTITIES[item.name] = item.label))
    for (let name in _ENTITIES) {
      $(`<option value="${name}">${_ENTITIES[name]}</option>`).appendTo('#belongEntity')
    }

    renderRbcomp(<DataList />, 'react-list', function () {
      RbListPage._RbList = this._List
    })
  })
})

// 列表配置
const ListConfig = {
  entity: 'RecycleBin',
  fields: [
    { field: 'belongEntity', label: $L('所属实体'), unsort: true },
    { field: 'recordName', label: $L('记录名称'), width: 300 },
    { field: 'deletedOn', label: $L('删除时间'), type: 'DATETIME' },
    { field: 'deletedBy.fullName', label: $L('删除用户') },
    { field: 'channelWith', label: $L('删除渠道'), unsort: true },
    { field: 'recordId', label: $L('记录 ID'), unsort: true },
  ],
  sort: 'deletedOn:desc',
}

class DataList extends React.Component {
  render() {
    return <RbList ref={(c) => (this._List = c)} config={ListConfig} />
  }

  componentDidMount() {
    const $s2 = $('#belongEntity')
      .select2({
        placeholder: $L('选择实体'),
        allowClear: false,
      })
      .val('$ALL$')
      .trigger('change')
    $s2.on('change', () => this.queryList())

    const $btn = $('.input-search .btn'),
      $input = $('.input-search input')
    $btn.off('click').on('click', () => this.queryList())
    $input.off('keydown').on('keydown', (e) => (e.which === 13 ? $btn.trigger('click') : true))

    this._$belongEntity = $s2
    this._$recordName = $input

    $('.J_restore').on('click', () => this.restore())
  }

  queryList() {
    let e = this._$belongEntity.val(),
      n = this._$recordName.val()
    if (e === '$ALL$') e = null

    const qs = []
    if (e) {
      qs.push({ field: 'belongEntity', op: 'EQ', value: e })
    }
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

    const alertMsg = (
      <React.Fragment>
        <div className="text-bold mb-2">{$L('是否恢复选中的 %d 条记录？', ids.length)}</div>
        <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-2">
          <input className="custom-control-input" type="checkbox" />
          <span className="custom-control-label">{$L('同时恢复关联删除的记录 (如有)')}</span>
        </label>
      </React.Fragment>
    )

    const that = this
    RbAlert.create(alertMsg, {
      confirm: function () {
        this.disabled(true)

        const c = $(this._dlg).find('input').prop('checked')
        $.post(`/admin/audit/recycle-bin/restore?cascade=${c}&ids=${ids.join(',')}`, (res) => {
          if (res.error_code === 0 && res.data.restored > 0) {
            this.hide()
            RbHighbar.success($L('成功恢复 %d 条记录', res.data.restored))
            that.queryList()
          } else {
            RbHighbar.error(res.error_code > 0 ? res.error_msg : $L('无法恢复选中记录'))
            this.disabled()
          }
        })
      },
    })
  }
}

const CellRenders_renderSimple = CellRenders.renderSimple
// eslint-disable-next-line react/display-name
CellRenders.renderSimple = function (v, s, k) {
  if (k.endsWith('.channelWith')) {
    v = v ? (
      <React.Fragment>
        {$L('关联删除')}
        <span className="badge text-id ml-1" title={$L('关联主记录 ID')}>
          {v.id}
        </span>
      </React.Fragment>
    ) : (
      $L('直接删除')
    )
  } else if (k.endsWith('.recordId')) {
    v = <span className="badge text-id">{v.id}</span>
  } else if (k.endsWith('.belongEntity')) {
    v = _ENTITIES[v] || `[${v.toUpperCase()}]`
  }

  return CellRenders_renderSimple(v, s, k)
}
