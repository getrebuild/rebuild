/*!
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
    const currentActive = this.state.currentActive || []
    return (
      <div className="file-list file-list-striped">
        {(this.state.files || []).map((item) => {
          const checked = currentActive.includes(item.id)
          return (
            <div key={`file-${item.id}`} className={`file-list-item ${checked ? 'active' : ''}`} onClick={(e) => this._handleClick(e, item.id)}>
              <div className="check">
                <div className="custom-control custom-checkbox m-0">
                  <input className="custom-control-input" type="checkbox" checked={checked === true} readOnly />
                  <label className="custom-control-label" />
                </div>
              </div>
              <div className="type">
                <i className="file-icon" data-type={item.fileType || '?'} />
              </div>
              <div className="detail">
                <a onClick={(e) => previewFile(e, item.filePath, item.relatedRecord ? item.relatedRecord[0] : null)} title={$L('预览')}>
                  {$fileCutName(item.filePath)}
                </a>
                <div className="extras">
                  <span className="fsize">{item.fileSize}</span>
                  {this.renderExtras(item)}
                </div>
              </div>
              {this.renderExtras34(item)}
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
          <div className="list-nodata">
            <i className="zmdi zmdi-folder-outline" />
            <p>{$L('暂无数据')}</p>
          </div>
        )}
      </div>
    )
  }

  _handleClick(e, id) {
    $stopEvent(e, true)
    let currentActiveNew = this.state.currentActive || []
    currentActiveNew.toggle(id)
    this.setState({ currentActive: currentActiveNew })
  }

  renderExtras(item) {
    return null
  }

  renderExtras34(item) {
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
      this.setState({ files: files, currentLen: current.length, currentActive: [] })
    })
  }

  getSelected() {
    const s = this.state.currentActive
    if ((s || []).length === 0) RbHighbar.create($L('未选中任何文件'))
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

// ~~ 共享列表
class SharedFiles extends RbModalHandler {
  render() {
    return (
      <RbModal ref={(c) => (this._dlg = c)} title={$L('查看分享文件')} disposeOnHide>
        <div className="sharing-list ml-1 mr-1">
          {this.state.data && this.state.data.length === 0 ? (
            <div className="list-nodata pt-5">
              <i className="zmdi mdi mdi-share-variant-outline" />
              <p>{$L('没有分享文件')}</p>
            </div>
          ) : (
            <table className="table table-hover">
              <thead>
                <tr>
                  <th>{$L('分享文件')}</th>
                  <th width="100" className="text-right">
                    {$L('过期时间')}
                  </th>
                  <th width="130"></th>
                </tr>
              </thead>
              <tbody ref={(c) => (this._$tbody = c)}>
                {this.state.data &&
                  this.state.data.map((item, idx) => {
                    return (
                      <tr key={idx}>
                        <td className="position-relative">
                          <a href={item[0]} target="_blank" className="link">
                            {$fileCutName(item[1])}
                          </a>
                          <div className="fop-action">
                            <a className="link J_copy" title={$L('复制分享链接')} data-url={item[0]}>
                              <i className="icon zmdi zmdi-copy fs-14" />
                            </a>
                            <a
                              title={$L('取消分享')}
                              onClick={(e) => {
                                const $tr = $(e.currentTarget).parents('tr')
                                $.post(`/filex/del-make-share?id=${item[5]}`, (res) => {
                                  if (res.error_code === 0) {
                                    $tr.animate({ opacity: 0 }, 400)
                                    setTimeout(() => $tr.remove(), 400)
                                    RbHighbar.success($L('已取消分享'))
                                  } else {
                                    RbHighbar.error(res.error_msg)
                                  }
                                })
                              }}>
                              <i className="icon zmdi zmdi-delete fs-16" />
                            </a>
                          </div>
                        </td>
                        <td title={item[2]} className="text-right">
                          <span>{$fromNow(item[2])}</span>
                        </td>
                        <td title={item[3]} className="text-muted text-right">
                          <span>{$L('分享于 %s', $fromNow(item[3]))}</span>
                        </td>
                        <td className="p-0"></td>
                      </tr>
                    )
                  })}
              </tbody>
            </table>
          )}
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    $.get('/filex/all-make-share', (res) => {
      this.setState({ data: res.data || [] }, () => {
        const $tbody = $(this._$tbody)
        const initCopy = function () {
          $tbody.find('.J_copy').each(function () {
            const $copy = $(this)
            // eslint-disable-next-line no-undef
            new ClipboardJS($copy[0], {
              text: function () {
                return $copy.data('url')
              },
            }).on('success', () => $copy.addClass('copied-check'))
            $copy.on('mouseenter', () => $copy.removeClass('copied-check'))
          })
        }
        if (window.ClipboardJS) {
          initCopy()
        } else {
          // eslint-disable-next-line no-undef
          $getScript('/assets/lib/clipboard.min.js', initCopy)
        }
      })
    })
  }
}

var currentSearch
var currentSort
var filesList

$(document).ready(() => {
  $('.side-toggle').on('click', () => {
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
    // eslint-disable-next-line no-undef
    // _showGlobalSearch(gs)
    currentSearch = $decode(gs)
    $('.input-search input').val(currentSearch)
  }

  $('.J_sort .dropdown-item').on('click', function () {
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

  $('.J_view-share').on('click', () => renderRbcomp(<SharedFiles />))
})
