/*
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
    $(res.data).each(function () {
      _ENTITIES[this.name] = this.label
    })

    for (let name in _ENTITIES) {
      $(`<option value="${name}">${_ENTITIES[name]}</option>`).appendTo('#belongEntity')
    }

    renderRbcomp(<DataList />, 'react-list')
  })
})

// 列表配置
const ListConfig = {
  entity: 'RevisionHistory',
  fields: [
    { field: 'belongEntity', label: $L('所属实体'), unsort: true },
    { field: 'revisionType', label: $L('变更类型') },
    { field: 'revisionOn', label: $L('变更时间'), type: 'DATETIME' },
    { field: 'revisionBy.fullName', label: $L('变更用户') },
    { field: 'channelWith', label: $L('变更渠道'), unsort: true },
    { field: 'recordId', label: $L('记录 ID'), unsort: true },
  ],
  sort: 'revisionOn:desc',
}

// 操作类型
const RevTypes = {
  1: $L('新建'),
  2: $L('删除'),
  4: $L('更新'),
  16: $L('分派'),
  32: $L('共享'),
  64: $L('取消共享'),
}

class DataList extends React.Component {
  state = { ...this.props }

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
    $btn.click(() => this.queryList())
    $input.keydown((e) => (e.which === 13 ? $btn.trigger('click') : true))

    this._$belongEntity = $s2
    this._$recordName = $input

    $('.J_details').click(() => this.showDetails())
  }

  queryList() {
    let e = this._$belongEntity.val(),
      n = this._$recordName.val()
    if (e === '$ALL$') e = null

    const qs = []
    if (e) {
      qs.push({ field: 'belongEntity', op: 'EQ', value: e })
    }
    if (n && $regex.isId(n)) {
      qs.push({ field: 'recordId', op: 'EQ', value: n })
    }
    const q = {
      entity: 'RevisionHistory',
      equation: 'AND',
      items: qs,
    }

    this._List.search(JSON.stringify(q))
  }

  showDetails() {
    const ids = this._List.getSelectedIds()
    if (!ids || ids.length === 0) return
    renderRbcomp(<DlgDetails id={ids[0]} width="681" />)
  }
}

const CellRenders_renderSimple = CellRenders.renderSimple
// eslint-disable-next-line react/display-name
CellRenders.renderSimple = function (v, s, k) {
  if (k.endsWith('.channelWith')) {
    v = v ? (
      <React.Fragment>
        {$L('关联操作')}
        <span className="badge text-id ml-1" title={$L('关联主记录 ID')}>
          {v.id}
        </span>
      </React.Fragment>
    ) : (
      $L('直接操作')
    )
  } else if (k.endsWith('.recordId')) {
    v = <span className="badge text-id">{v.id}</span>
  } else if (k.endsWith('.belongEntity')) {
    v = _ENTITIES[v] || `[${v.toUpperCase()}]`
  } else if (k.endsWith('.revisionType')) {
    v = RevTypes[v] || 'N'
  }

  return CellRenders_renderSimple(v, s, k)
}

// ~~ 变更详情
class DlgDetails extends RbAlert {
  renderContent() {
    const _data = (this.state.data || []).filter((item) => !$same(item.after, item.before))
    if (_data.length === 0) return <div className="m-3 text-center text-muted">{$L('无变更详情')}</div>

    return (
      <table className="table table-fixed">
        <thead>
          <tr>
            <th width="22%">{$L('字段')}</th>
            <th>{$L('变更前')}</th>
            <th>{$L('变更后')}</th>
          </tr>
        </thead>
        <tbody>
          {_data.map((item) => {
            return (
              <tr key={item.field}>
                <td>{item.field}</td>
                <td>
                  <div>{this._formatValue(item.before)}</div>
                </td>
                <td>
                  <div>{this._formatValue(item.after)}</div>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    )
  }

  componentDidMount() {
    $.get(`/admin/audit/revision-history/details?id=${this.props.id}`, (res) => {
      if (res.data.length === 0) {
        RbHighbar.create($L('无变更详情'))
        this.hide()
      } else {
        super.componentDidMount()
        this.setState({ data: res.data })
      }
    })
  }

  _formatValue(v) {
    if (v) {
      return typeof v === 'object' ? v.join(', ') : v
    } else {
      return <span className="text-muted">{$L('空')}</span>
    }
  }
}
