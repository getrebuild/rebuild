/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */

const PAGE_SIZE = 40

const _STATES = {
  '1': '待处理',
  '10': '审批通过',
  '11': '审批驳回',
}
// ~ 审批列表
class ApprovalList extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }

    this._lastType = props.type || 1
    this._pageNo = 1
  }

  render() {
    const currentActive = this.state.currentActive || []

    return (
      <div className="file-list file-list-striped">
        {(this.state.datas || []).map((item) => {
          const checked = currentActive.includes(item.id)
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
              <div className="detail">
                <a onClick={(e) => RbViewModal.openView({ id: item.recordId })} title={$L('查看记录')}>
                  {item.recordName}
                </a>
                <div className="extras">
                  <span>{item.recordMeta[1]}</span>
                </div>
              </div>
              <div className="info position-relative">
                <span className="fop-action">
                  <a title={$L('通过')} onClick={(e) => this._handleApprve(item, 10, e)}>
                    <i className="icon mdi mdi-check-circle-outline" />
                  </a>
                  <a title={$L('驳回')} onClick={(e) => this._handleApprve(item, 11, e)}>
                    <i className="icon mdi mdi-close-circle-outline" />
                  </a>
                </span>
              </div>
              <div className="info">
                <DateShow date={item.createdOn} />
              </div>
              <div className="info">{item.createdBy[1]}</div>
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

  loadData(type, pageNo) {
    this._lastType = type || this._lastType
    this._pageNo = pageNo || 1
    const url = `/approval/data-list?type=${this._lastType}&sort=&q=&pageNo=${this._pageNo}&pageSize=${PAGE_SIZE}`
    $.get(url, (res) => {
      const current = res.data || []
      let datas = this._pageNo === 1 ? [] : this.state.datas
      datas = [].concat(datas, current)
      this.setState({ datas: datas, currentSize: current.length, currentActive: [] })
    })
  }

  reload(type) {
    this.loadData(type, 1)
  }

  getSelected() {
    const s = this.state.currentActive
    if ((s || []).length === 0) RbHighbar.create($L('未选中任何审批'))
    else return s
  }

  _handleApprve(item, state, e) {
    $stopEvent(e, true)
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

  renderRbcomp(<ApprovalList type={type} />, $('.approval-viewport'), function () {
    _ApprovalList = this
  })
})
