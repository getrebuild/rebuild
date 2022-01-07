/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */

const PAGE_SIZE = 40

// ~ 文件列表
class FilesList extends React.Component {
  state = { ...this.props }
  __lastEntry = 1
  __pageNo = 1

  render() {
    return (
      <div className="file-list file-list-striped">
        {(this.state.files || []).map((item) => {
          const checked = this.state.currentActive === item.id
          return (
            <div key={`file-${item.id}`} className={`file-list-item ${checked ? 'active' : ''}`} onClick={(e) => this._handleClick(e, item.id)}>
              <div className="check">
                <div className="custom-control custom-checkbox m-0">
                  <input className="custom-control-input" type="checkbox" checked={checked === true} readOnly />
                  <label className="custom-control-label" />
                </div>
              </div>
              <div className="type">
                <i className="file-icon" data-type={item.fileType} />
              </div>
              <div className="detail">
                <a onClick={(e) => previewFile(e, item.filePath, item.relatedRecord ? item.relatedRecord[0] : null)}>{$fileCutName(item.filePath)}</a>
                <div className="extras">
                  <span className="fsize">{item.fileSize}</span>
                  {this.renderExtras(item)}
                </div>
              </div>
              <div className="info">
                <DateShow date={item.uploadOn} />
              </div>
              <div className="info">{item.uploadBy[1]}</div>
            </div>
          )
        })}
        {this.state.currentLen >= PAGE_SIZE && (
          <div className="text-center mt-3 pb-3">
            <a
              href="#"
              onClick={(e) => {
                this.loadData(null, this.__pageNo + 1)
                e.preventDefault()
              }}>
              {$L('显示更多')}
            </a>
          </div>
        )}
        {this.__pageNo > 1 && this.state.currentLen > 0 && this.state.currentLen < PAGE_SIZE && <div className="text-center mt-3 pb-3 text-muted">{$L('已显示全部')}</div>}
        {this.__pageNo === 1 && this.state.files && this.state.files.length === 0 && (
          <div className="list-nodata pt-8 pb-8">
            <i className="zmdi zmdi-folder-outline" />
            <p>{$L('暂无数据')}</p>
          </div>
        )}
      </div>
    )
  }

  _handleClick(e, id) {
    $stopEvent(e, true)
    if (id === this.state.currentActive) this.setState({ currentActive: null })
    else this.setState({ currentActive: id })
  }

  renderExtras(item) {
    return null
  }

  componentDidMount = () => this.loadData()

  loadData(entry, pageNo) {
    this.__lastEntry = entry || this.__lastEntry
    this.__pageNo = pageNo || 1
    const url = `/files/list-file?entry=${this.__lastEntry}&sort=${currentSort || ''}&q=${$encode(currentSearch || '')}&pageNo=${this.__pageNo}&pageSize=${PAGE_SIZE}`
    $.get(url, (res) => {
      const current = res.data || []
      let files = this.__pageNo === 1 ? [] : this.state.files
      files = [].concat(files, current)
      this.setState({ files: files, currentLen: current.length })
    })
  }

  getSelected() {
    const s = this.state.currentActive
    if (!s) RbHighbar.create($L('未选中任何文件'))
    else return s
  }
}

// 文件预览
const previewFile = function (e, path, checkId) {
  $stopEvent(e)
  if (checkId) {
    $.get(`/files/check-readable?id=${checkId}`, (res) => {
      if (res.data) RbPreview.create(path)
      else RbHighbar.error($L('你没有查看此文件的权限'))
    })
  } else {
    RbPreview.create(path)
  }
}

var currentSearch
var currentSort
var filesList

$(document).ready(() => {
  $('.side-toggle').click(() => {
    const $el = $('.rb-aside').toggleClass('rb-aside-collapsed')
    $.cookie('rb.asideCollapsed', $el.hasClass('rb-aside-collapsed'), { expires: 180 })
  })

  const $content = $('.page-aside .tab-content')
  $addResizeHandler(() => {
    $content.height($(window).height() - 147)
    $content.perfectScrollbar('update')
  })()

  const gs = $urlp('gs', location.hash)
  if (gs) {
    currentSearch = $decode(gs)
    $('.search-input-gs, .input-search input').val(currentSearch)
  }

  $('.J_sort .dropdown-item').click(function () {
    const $this = $(this)
    currentSort = $this.data('sort')
    $('.J_sort > .btn').find('span').text($this.text())
    filesList && filesList.loadData()
  })

  // 搜索
  const $btn = $('.input-search .input-group-btn .btn').on('click', () => {
    currentSearch = $('.input-search input').val()
    filesList && filesList.loadData()
  })
  const $input = $('.input-search input').on('keydown', (e) => (e.which === 13 ? $btn.trigger('click') : true))
  $('.input-search .btn-input-clear').on('click', () => {
    $input.val('')
    $btn.trigger('click')
  })
})
