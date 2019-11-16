/* eslint-disable react/jsx-no-target-blank */
/* eslint-disable react/prop-types */
/* eslint-disable no-unused-vars */

// ~ 文件列表
class FilesList extends React.Component {
  state = { ...this.props }
  __lastEntry = 1

  render() {
    return <div className="file-list">
      {(this.state.files || []).map((item) => {
        let checked = this.state.currentActive === item.id
        return <div key={`file-${item.id}`} className={`file-list-item ${checked ? 'active' : ''}`} onClick={() => this._handleClick(item.id)}>
          <div className="check">
            <div className="custom-control custom-checkbox m-0">
              <input className="custom-control-input" type="checkbox" checked={checked} onChange={() => this._handleClick(item.id)} />
              <label className="custom-control-label"></label>
            </div>
          </div>
          <div className="type"><i className="file-icon" data-type={item.fileType}></i></div>
          <div className="detail">
            <a title="点击查看文件" onClick={() => previewFile(item.filePath, item.relatedRecord ? item.relatedRecord[0] : null)}>{$fileCutName(item.filePath)}</a>
            <div className="extras">{this.renderExtras(item)}</div>
          </div>
          <div className="info">{item.uploadOn}</div>
          <div className="info">{item.uploadBy[1]}</div>
        </div>
      })}
      {(this.state.files && this.state.files.length === 0) && <div className="list-nodata pt-8 pb-8">
        <i className="zmdi zmdi-folder-outline"></i>
        <p>暂无相关文件</p>
      </div>}
    </div>
  }

  _handleClick(id) {
    event.preventDefault()
    if (this.state.currentActive === id) this.setState({ currentActive: null })
    else this.setState({ currentActive: id })
  }

  renderExtras(item) {
    return <React.Fragment >
      <span>{item.fileSize}</span>
      {item.relatedRecord && <span><a target="_blank" title="点击查看相关记录" onClick={(e) => $stopEvent(e)} href={`${rb.baseUrl}/app/list-and-view?id=${item.relatedRecord[0]}`}>{item.relatedRecord[1]}</a></span>}
    </React.Fragment>
  }

  componentDidMount = () => this.loadData()
  loadData(entry) {
    this.__lastEntry = entry = entry || this.__lastEntry
    $.get(`${rb.baseUrl}/files/list-file?entry=${entry}&sort=${currentSort || ''}&q=${$encode(currentSearch || '')}`, (res) => {
      this.setState({ files: res.data || [] })
    })
  }

  getSelected() {
    let s = this.state.currentActive
    if (!s) RbHighbar.create('未选中任何文件')
    else return s
  }
}

// 文件预览
const previewFile = function (path, checkId) {
  if (checkId) {
    $.get(`${rb.baseUrl}/files/check-readable?id=${checkId}`, (res) => {
      if (res.data) RbPreview.create(path)
      else RbHighbar.error('你没有读取/查看此文件的权限')
    })
  } else RbPreview.create(path)
}

var currentSearch
var currentSort
var filesList

$(document).ready(() => {
  $('.side-toggle').click(() => $('.rb-aside').toggleClass('rb-aside-collapsed'))
  let $content = $('.page-aside .tab-content')
  let hold = window.resize_handler
  window.resize_handler = function () {
    typeof hold === 'function' && hold()
    $content.height($(window).height() - 147)
    $content.perfectScrollbar('update')
  }
  window.resize_handler()

  $('.J_sort .dropdown-item').click(function () {
    let $this = $(this)
    currentSort = $this.data('sort')
    $('.J_sort > .btn').find('span').text($this.text())
    filesList && filesList.loadData()
  })
  let btn = $('.input-search .btn').click(() => {
    currentSearch = $('.input-search input').val()
    filesList && filesList.loadData()
  })
  $('.input-search input').keydown((event) => { if (event.which === 13) btn.trigger('click') })
})