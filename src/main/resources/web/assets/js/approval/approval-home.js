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

// ~ 审批列表
class ApprovalList extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }

    this._pageNo = 1
  }

  render() {
    const currentActive = this.state.currentActive || []

    return (
      <div className="file-list file-list-striped">
        {(this.state.datas || []).map((item) => {
          const checked = currentActive.includes(item.id)
          const state = _STATES[item.state] || [item.state, undefined]

          return (
            <div
              key={item.id}
              className={`file-list-item ${checked ? 'active' : ''}`}
              onClick={(e) => {
                $stopEvent(e, true)
                let currentActiveNew = this.state.currentActive || []
                currentActiveNew.toggle(item.id)
                this.setState({ currentActive: currentActiveNew })
              }}>
              <div className="check">
                <div className="custom-control custom-checkbox m-0">
                  <input className="custom-control-input" type="checkbox" checked={checked === true} readOnly />
                  <label className="custom-control-label" />
                </div>
              </div>
              <div className="detail user-info">
                <div className="user-avatar float-left">
                  <img src={`${rb.baseUrl}/account/user-avatar/${item.createdBy[0]}`} alt="Avatar" />
                </div>
                <div className="float-left">
                  <div>{item.createdBy[1]}</div>
                  <div className="extras">
                    <DateShow date={item.createdOn} />
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
                <span className={state[1] ? `badge badge-${state[1]}` : ''}>{state[0]}</span>
                <button className="btn btn-link btn-sm ml-2 pr-0" onClick={(e) => this._handleDetail(item, e)}>
                  {$L('详情')}
                </button>
                {item.state === 1 && item.imApprover && (
                  <button className="btn btn-secondary btn-sm ml-2" onClick={(e) => this._handleApprve(item, e)}>
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
                this.loadData(null, this._pageNo + 1)
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
    const url = `/approval/data-list?type=${type}&sort=${this._sort}&q=&pageNo=${this._pageNo}&pageSize=${PAGE_SIZE}`
    $.get(url, (res) => {
      const current = res.data || []
      let datas = this._pageNo === 1 ? [] : this.state.datas
      datas = [].concat(datas, current)
      this.setState({ datas: datas, type: type, currentSize: current.length, currentActive: [] })
    })
  }

  reload(type, sort) {
    this.loadData(type, sort, 1)
  }

  getSelected() {
    const s = this.state.currentActive
    if ((s || []).length === 0) RbHighbar.create($L('未选中任何审批'))
    else return s
  }

  _handleApprve(item, e) {
    e && $stopEvent(e)
    renderRbcomp(
      // eslint-disable-next-line react/jsx-no-undef
      <ApprovalApproveForm
        id={item.recordMeta[0]}
        approval={item.approvalId}
        entity={item.recordMeta[2]}
        onConfirm={() => {
          this.reload()
        }}
      />,
    )
  }

  _handleDetail(item, e) {
    e && $stopEvent(e)
    // eslint-disable-next-line react/jsx-no-undef
    renderRbcomp(<ApprovalStepViewer id={item.recordMeta[0]} approval={item.approvalId} $$$parent={this} />)
  }
}

let _ApprovalList

$(document).ready(() => {
  const type = $urlp('type', location.hash) || 1
  $('.aside-nav li').removeClass('active')
  $(`.aside-nav a[data-type="${type}"]`).parent().addClass('active')

  $('.aside-nav a').on('click', function () {
    $('.aside-nav li').removeClass('active')
    $(this).parent().addClass('active')

    _ApprovalList.reload($(this).attr('data-type'))
  })

  $('.J_approve').on('click', () => {
    alert(_ApprovalList.getSelected())
  })

  $('.J_sort a').on('click', function () {
    _ApprovalList.reload(null, $(this).data('sort'))
    $('.J_sort>button').text($(this).text())
  })

  renderRbcomp(<ApprovalList type={type} />, $('.approval-viewport'), function () {
    _ApprovalList = this
  })
})
