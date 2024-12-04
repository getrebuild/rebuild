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
  16: $L('分配'),
  32: $L('共享'),
  64: $L('取消共享'),
  991: $L('审批通过'),
  992: $L('审批撤销'),
}

class DataList extends React.Component {
  state = { ...this.props }

  render() {
    return <RbList ref={(c) => (this._List = c)} config={ListConfig} />
  }

  componentDidMount() {
    const $be = $('#belongEntity')
      .select2({
        placeholder: $L('选择实体'),
        allowClear: false,
      })
      .val('$ALL$')
      .trigger('change')

    $be.on('change', () => this.queryList())

    const $btn = $('.input-search .btn'),
      $input = $('.input-search input')
    $btn.off('click').on('click', () => this.queryList())
    $input.off('keydown').on('keydown', (e) => (e.which === 13 ? $btn.trigger('click') : true))

    this._$belongEntity = $be
    this._$recordName = $input

    $('.J_details').on('click', () => this.showDetails())
    if (rb.commercial < 1) {
      $('.J_details')
        .off('click')
        .on('click', () => {
          RbHighbar.error(WrapHtml($L('免费版不支持此功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
        })
    }
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
    ids && ids[0] && renderRbcomp(<DlgDetails id={ids[0]} width="681" />)
  }
}

const CellRenders_renderSimple = CellRenders.renderSimple
// eslint-disable-next-line react/display-name
CellRenders.renderSimple = function (v, s, k) {
  if (k.endsWith('.channelWith')) {
    v = v ? (
      <RF>
        {$L('关联操作')}
        <span className="badge text-id ml-1" title={$L('关联记录 ID')}>
          {v.id}
        </span>
      </RF>
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
    if (this.state.viewAll) return <HistoryViewport id={this.props.id} />

    return (
      <table className="table table-fixed mb-0">
        <thead>
          <tr>
            <th width="25%">{$L('字段')}</th>
            <th>{$L('变更前')}</th>
            <th>{$L('变更后')}</th>
          </tr>
        </thead>
        <tbody>
          <ContentsGroup contents={this.state.data} />
          <tr>
            <td colSpan="3" className="text-center pb-0">
              <div className="mt-1">
                <a className="show-more-pill" onClick={() => this.setState({ viewAll: true })}>
                  {$L('查看全部')}
                </a>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    )
  }

  componentDidMount() {
    $.get(`/admin/audit/revision-history/details?id=${this.props.id}`, (res) => {
      super.componentDidMount()
      this.setState({ data: res.data || [] })
    })
  }
}

class HistoryViewport extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    if (!this.state.dataList) {
      return (
        <div className="rb-loading rb-loading-active">
          <RbSpinner />
          <div style={{ minHeight: 132 }} />
        </div>
      )
    }

    return (
      <table className="table table-fixed group-table">
        <thead>
          <tr>
            <th width="25%">{$L('字段')}</th>
            <th>{$L('变更前')}</th>
            <th>{$L('变更后')}</th>
          </tr>
        </thead>
        <tbody>
          {this.state.dataList.map((item, idx) => {
            return (
              <RF key={idx}>
                <tr className="group-title">
                  <td colSpan="3">
                    <h5>{WrapHtml($L('**%s** 由 %s %s', item[2].split(' UTC')[0], item[3], RevTypes[item[1]]))}</h5>
                  </td>
                </tr>
                <ContentsGroup contents={item[0]} />
              </RF>
            )
          })}
        </tbody>
      </table>
    )
  }

  componentDidMount() {
    $.get(`/admin/audit/revision-history/details-list?id=${this.props.id}`, (res) => {
      this.setState({ dataList: res.data || [] })
    })
  }
}

function ContentsGroup({ contents }) {
  const _FN = function (v) {
    if (v === true) return $L('是')
    else if (v === false) return $L('否')
    else if (v === 0) return 0
    else if (v) return typeof v === 'object' ? v.join(', ') : v
    return <span className="text-muted">{$L('空')}</span>
  }

  // 排除相同
  const notSame = (contents || []).filter((item) => !$same(item.after, item.before))
  return (
    <RF>
      {(notSame || []).length === 0 ? (
        <tr>
          <td colSpan="3">
            <div className="text-muted">{$L('无变更详情')}</div>
          </td>
        </tr>
      ) : (
        notSame.map((item) => {
          return (
            <tr key={item.field}>
              <td>{item.field}</td>
              <td>
                <div>{_FN(item.before)}</div>
              </td>
              <td>
                <div>{_FN(item.after)}</div>
              </td>
            </tr>
          )
        })
      )}
    </RF>
  )
}
