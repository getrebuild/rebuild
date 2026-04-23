/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */

const PAGE_SIZE = 40

const _STATES = {
  '1': ['待处理', 'warning'],
  '10': ['通过', 'success'],
  '11': ['驳回', 'danger'],
  '12': ['撤回', 'danger'],
  '0': ['无效'],
}

let currentSearch

// ~ 审批列表
class ApprovalList extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }

    this._pageNo = 1
    this._formsApprove = {}
    this._formsDetail = {}
  }

  render() {
    const _active = this.state._active || []
    return (
      <div className="file-list file-list-striped">
        {(this.state.datas || []).map((item) => {
          const checked = _active.includes(item.id)
          const state = _STATES[item.state] || [item.state, undefined]

          return (
            <div
              key={item.id}
              className={`file-list-item ${checked ? 'active' : ''}`}
              onClick={(e) => {
                $stopEvent(e, true)
                let _activeNew = this.state._active || []
                _activeNew.toggle(item.id)
                this.setState({ _active: _activeNew })
              }}>
              <div className="check">
                <label className="custom-control custom-checkbox m-0">
                  <input className="custom-control-input" type="checkbox" checked={checked === true} readOnly />
                  <span className="custom-control-label" />
                </label>
              </div>
              <div className="detail user-info">
                <div className="user-avatar float-left">
                  <img src={`${rb.baseUrl}/account/user-avatar/${item.createdBy[0]}`} alt="Avatar" />
                </div>
                <div className="float-left">
                  <div>{item.createdBy[1]}</div>
                  <div className="extras">
                    <DateShow date={item.createdOn} showOrigin />
                  </div>
                </div>
              </div>
              <div className="detail record-info">
                <a href="#/View" onClick={(e) => RbViewModal.openView({ id: item.recordMeta[0] })} title={$L('查看记录')}>
                  {item.recordMeta[1]}
                </a>
                <div className="extras">
                  <span>{item.recordMeta[3]}</span>
                </div>
              </div>

              <div className="info w-auto">
                {item.approvedOn && (
                  <span className="mr-2">
                    <DateShow date={item.approvedOn} showOrigin />
                  </span>
                )}
                <span className={state[1] ? `badge badge-${state[1]}` : ''}>{state[0]}</span>

                <button className="btn btn-link btn-sm ml-2" onClick={(e) => this._handleDetail(item, e)}>
                  {$L('详情')}
                </button>
                {item.state === 1 && item.imApprover && (
                  <button className="btn btn-secondary btn-sm" onClick={(e) => this._handleApprove(item, e)}>
                    {$L('审批')}
                  </button>
                )}
              </div>
            </div>
          )
        })}

        {this.state.currentSize >= PAGE_SIZE && (
          <div className="text-center mt-4 pb-4">
            <a
              className="show-more-pill"
              onClick={(e) => {
                $stopEvent(e, true)
                this.loadData(null, null, this._pageNo + 1)
              }}>
              {$L('显示更多')}
            </a>
          </div>
        )}
        {this._pageNo > 1 && this.state.currentSize > 0 && this.state.currentSize < PAGE_SIZE && (
          <div className="mt-6 pb-1">
            <div className="loadmore-line">
              <span>{$L('已加载全部')}</span>
            </div>
          </div>
        )}
        {this._pageNo === 1 && this.state.datas && this.state.datas.length === 0 && (
          <div className="list-nodata">
            <i className="mdi mdi-progress-check" />
            <p>{$L('暂无审批')}</p>
          </div>
        )}
      </div>
    )
  }

  componentDidMount = () => this.loadData()

  loadData(type, sort, pageNo) {
    type = type || this.state.type
    this._sort = sort || this._sort || ''
    this._pageNo = pageNo || 1
    const url = `/approval/data-list?type=${type}&sort=${this._sort}&entity=${currentSearch || ''}&pageNo=${this._pageNo}&pageSize=${PAGE_SIZE}`
    $.get(url, (res) => {
      const current = res.data || []
      let datas = this._pageNo === 1 ? [] : this.state.datas
      datas = [].concat(datas, current)
      this.setState({ datas: datas, type: type, currentSize: current.length, _active: [] })
    })
  }

  // 适配
  approve(rest) {
    this._handleApprove({
      recordMeta: [rest.id, null, rest.entity],
      approvalId: rest.approval,
    })
  }

  _handleApprove(item, e) {
    e && $stopEvent(e)

    const that = this
    if (this._formsApprove[item.id || '0']) {
      this._formsApprove[item.id].show()
    } else {
      renderRbcomp(
        // eslint-disable-next-line react/jsx-no-undef
        <ApprovalApproveForm
          id={item.recordMeta[0]}
          entity={item.recordMeta[2]}
          approval={item.approvalId}
          onConfirm={() => {
            if (item.id) {
              // delete that._formsApprove[item.id]  // 审批后户自动 reload
              delete that._formsApprove[item.id] // 其实还在
            }
            this.reload()
          }}
        />,
        function () {
          if (item.id) that._formsApprove[item.id] = this
        },
      )
    }
  }

  _handleDetail(item, e) {
    e && $stopEvent(e)

    const that = this
    if (this._formsDetail[item.id || '0']) {
      this._formsDetail[item.id].show()
    } else {
      renderRbcomp(
        // eslint-disable-next-line react/jsx-no-undef
        <ApprovalStepViewer id={item.recordMeta[0]} approval={item.approvalId} $$$parent={this} />,
        function () {
          if (item.id) that._formsDetail[item.id] = this
        },
      )
    }
  }

  // -- APIs

  reload(type, sort) {
    this.loadData(type, sort, 1)
  }

  getSelected(hideWarning) {
    return this.getSelectedIds(hideWarning)
  }

  getSelectedIds(hideWarning) {
    const s = this.state._active || []
    if (s.length === 0 && hideWarning !== true) RbHighbar.create($L('未选中任何审批'))

    // 这里返回记录的ID
    let ids = []
    this.state.datas.forEach((d) => {
      if (s.includes(d.id)) ids.push(d.recordMeta[0])
    })
    return ids
  }
}

// eslint-disable-next-line no-undef
class BatchApprove2 extends BatchApprove {
  constructor(props) {
    super(props)
    this.state.dataRange = 1
    this._confirmTip = $L('请再次确认审批方式。开始审批吗？')
  }

  render() {
    return (
      <RbModal title={$L('批量审批')} ref={(c) => (this._dlg = c)} disposeOnHide>
        <div className="form batch-form">{this.renderOperator()}</div>

        <div className="dialog-footer" ref={(c) => (this._btns = c)}>
          <button className="btn btn-secondary btn-spacem mr-2" type="button" onClick={() => this.handleCancel()}>
            {$L('取消')}
          </button>
          <button className="btn btn-primary btn-space mr-1" type="button" onClick={() => this.handleConfirm()}>
            {$L('审批')}
          </button>
        </div>
      </RbModal>
    )
  }

  renderOperator() {
    return (
      <div>
        <div className="form-group">
          <label className="text-bold">{$L('审批方式')}</label>
          <div>
            <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-0">
              <input className="custom-control-input" type="radio" name="approveState" value="10" onClick={this.handleChange} />
              <span className="custom-control-label">{$L('通过')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-0">
              <input className="custom-control-input" type="radio" name="approveState" value="11" onClick={this.handleChange} />
              <span className="custom-control-label">{$L('驳回')}</span>
            </label>
          </div>
        </div>
        <div className="form-group">
          <label className="text-bold">{$L('批注')}</label>
          <textarea className="form-control form-control-sm row2x" name="approveRemark" placeholder={$L('输入批注')} maxLength="600" onChange={this.handleChange} />
        </div>
      </div>
    )
  }

  componentDidMount() {
    // super.componentDidMount()
  }

  getQueryData() {
    return {
      _selected: this.props.listRef.getSelectedIds(true).join('|'),
    }
  }
}

let _ApprovalList

$(document).ready(() => {
  // NAV
  function _FN(t) {
    $('.aside-nav li').removeClass('active')
    $(`.aside-nav a[data-type="${t}"]`).parent().addClass('active')

    _ApprovalList && _ApprovalList.reload(t)
    $('.J_approve').attr('disabled', ~~t !== 1)
  }

  const type = $urlp('type', location.hash) || 1
  _FN(type)

  $('.aside-nav a').on('click', function () {
    _FN($(this).attr('data-type'))
  })

  // 全选
  $('.J_select-all').on('change', (e) => {
    let ids = []
    if (e.target.checked) {
      _ApprovalList.state.datas.forEach((d) => ids.push(d.id))
    }
    _ApprovalList.setState({ _active: ids })
  })

  // 搜索
  // $initQuickSearch(null, (q) => {
  //   currentSearch = q
  //   _ApprovalList.reload()
  // })
  $.get('/commons/metadata/entities?approval=true', (res) => {
    res.data &&
      res.data.forEach((d) => {
        $(`<option value="${d.entity}">${d.label}</option>`).appendTo('.J_search-entity')
      })

    $('.J_search-entity')
      .select2({
        placeholder: $L('业务实体'),
        allowClear: true,
      })
      .on('change', function () {
        currentSearch = $(this).val()
        _ApprovalList.reload()
      })
  })

  // 批量
  $('.J_approve').on('click', () => {
    if (_ApprovalList.getSelectedIds().length === 0) return
    renderRbcomp(<BatchApprove2 listRef={_ApprovalList} entity="User" />)
  })

  // 排序
  $('.J_sort .dropdown-item').on('click', function () {
    const $this = $(this)
    _ApprovalList.reload(null, $this.data('sort'))
    $('.J_sort>button>span').text($this.text())
  })

  renderRbcomp(<ApprovalList type={type} />, $('.approval-viewport'), function () {
    _ApprovalList = this
  })
})
