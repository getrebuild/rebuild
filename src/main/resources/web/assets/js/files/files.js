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
    const hasFiles = (this.state.files || []).length > 0
    return (
      <div className="file-list">
        {(this.state.files || []).map((item) => {
          const checked = this.state.currentActive === item.id
          return (
            <div key={`file-${item.id}`} className={`file-list-item ${checked ? 'active' : ''}`} onClick={() => this._handleClick(item.id)}>
              <div className="check">
                <div className="custom-control custom-checkbox m-0">
                  <input className="custom-control-input" type="checkbox" checked={checked} onChange={() => this._handleClick(item.id)} />
                  <label className="custom-control-label"></label>
                </div>
              </div>
              <div className="type">
                <i className="file-icon" data-type={item.fileType}></i>
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
              {$L('LoadMore')}
            </a>
          </div>
        )}
        {this.__pageNo > 1 && this.state.currentLen > 0 && this.state.currentLen < PAGE_SIZE && <div className="text-center mt-3 pb-3 text-muted">{$L('AllLoaded')}</div>}
        {this.__pageNo === 1 && !hasFiles && (
          <div className="list-nodata pt-8 pb-8">
            <i className="zmdi zmdi-folder-outline"></i>
            <p>{$L('NoData')}</p>
          </div>
        )}
      </div>
    )
  }

  _handleClick(id) {
    event.preventDefault()
    if (this.state.currentActive === id) this.setState({ currentActive: null })
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
    if (!s) RbHighbar.create($L('UnselectAnySome,File'))
    else return s
  }
}

// 文件预览
const previewFile = function (e, path, checkId) {
  $stopEvent(e)
  if (checkId) {
    $.get(`/files/check-readable?id=${checkId}`, (res) => {
      if (res.data) RbPreview.create(path)
      else RbHighbar.error($L('NoPermissionReadFile'))
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

  const $btn = $('.input-search .btn').click(() => {
    currentSearch = $('.input-search input').val()
    filesList && filesList.loadData()
  })
  $('.input-search input').keydown((e) => (e.which === 13 ? $btn.trigger('click') : true))
})
