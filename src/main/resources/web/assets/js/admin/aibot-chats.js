/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// ~~ 所有会话

// token 用量单位为 K
const formatTokenUsage = function (v) {
  v = v || 0
  return v >= 1000 ? (v / 1000).toFixed(1) + 'K' : v.toLocaleString()
}

// eslint-disable-next-line no-unused-vars
class AiBotChatsModal extends RbModal {
  constructor(props) {
    super(props)
    this.state = { ...props, total: -1, data: null, user: '', showMore: false }
  }

  componentDidMount() {
    super.componentDidMount()
    this._loadNext(true)
  }

  renderContent() {
    let body
    if (this.state.data === null) {
      body = (
        <div className="rb-loading rb-loading-active">
          <RbSpinner />
        </div>
      )
    } else if (this.state.data.length === 0) {
      body = (
        <div className="list-nodata">
          <span className="zmdi zmdi-info-outline" />
          <p>{$L('暂无数据')}</p>
        </div>
      )
    } else {
      body = (
        <RF>
          <table className="table table-hover table-striped mb-0">
            <thead>
              <tr>
                <th>{$L('标题')}</th>
                <th width="120">{$L('使用时间')}</th>
                <th width="120">{$L('使用用户')}</th>
                <th width="100" className="text-right">
                  {$L('消耗 TOKEN')}
                </th>
              </tr>
            </thead>
            <tbody>
              {this.state.data.map((item) => (
                <tr key={item.id}>
                  <td>
                    <a href={`${rb.baseUrl}/aibot/chat?frame=1#chatid=${item.id}`} title={item.subject} target="_blank" className="link">
                      {item.subject}
                    </a>
                  </td>
                  <td>
                    <DateShow date={item.modifiedOn} />
                  </td>
                  <td>{item.userName}</td>
                  <td className="text-right">{formatTokenUsage(item.token)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {this.state.showMore && (
            <div className="text-center mt-4 mb-4">
              <a className="show-more-pill" onClick={() => this._loadNext()}>
                {$L('显示更多')}
              </a>
            </div>
          )}
        </RF>
      )
    }

    return (
      <div className="modal-body m-0 p-0">
        <div className="card card-table shadow-none mb-0">
          <div className="card-body">
            <div className="dataTables_wrapper container-fluid">
              <div className="row rb-datatable-header bg-eee">
                <div className="col-8">
                  <div className="dataTables_filter d-flex align-items-center">
                    <div className="input-group input-search">
                      <input
                        className="form-control"
                        type="text"
                        placeholder={$L('查询')}
                        maxLength="40"
                        onKeyDown={(e) => {
                          if (e.keyCode === 13) this._loadNext(true, $(e.target).closest('.input-search').find('input').val().trim())
                        }}
                      />
                      <span className="input-group-btn">
                        <button className="btn btn-secondary" type="button" onClick={(e) => this._loadNext(true, $(e.target).closest('.input-search').find('input').val().trim())}>
                          <i className="icon zmdi zmdi-search" />
                        </button>
                      </span>
                    </div>
                    <div className="ml-2" style={{ width: 227 }}>
                      <UserSelector
                        hideDepartment
                        hideRole
                        hideTeam
                        multiple={false}
                        onSelectItem={(s, isRemove) => this._selectUser(isRemove || !s ? '' : s.id)}
                        onClearSelection={() => this._selectUser('')}
                        title={$L('选择所属用户')}
                      />
                    </div>
                  </div>
                </div>
                <div className="col-4">
                  <div className="dataTables_oper pt-2 fs-13">{this.state.total > -1 && <span className="text-muted">{$L('共 %s 条', this.state.total)}</span>}</div>
                </div>
              </div>
              <div className="data-list">{body}</div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  _selectUser(id) {
    this.setState({ user: id || '' }, () => this._loadNext(true))
  }

  _loadNext(reset, q) {
    if (this.state._search) return
    this.setState({ _search: true })

    this.__pageNo = reset ? 1 : (this.__pageNo || 0) + 1
    this.__q = q !== undefined ? q : this.__q

    $.get('/admin/integration/aibot/chat-list', { pageNo: this.__pageNo, user: this.state.user || '', q: this.__q || '', agent: this.props.agent || '' }, (res) => {
      const d = res.data || {}
      const _data = d.data || []
      this.setState({
        _search: false,
        data: reset ? _data : (this.state.data || []).concat(_data),
        showMore: _data.length >= 20,
        total: d.total > -1 ? d.total : this.state.total,
      })
    })
  }
}
