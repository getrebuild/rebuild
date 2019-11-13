/* global __NavTreeData */
// 文档

// 渲染目录树
const _renderOption = function (item, idx) {
  idx = idx || 0
  if (item.id === 1) item = { text: '无' }
  let options = [<option key={`opt-${item.id}`} value={item.id}
    dangerouslySetInnerHTML={{ __html: idx === 0 ? item.text : `${'&nbsp;'.repeat(idx * 3)}${item.text}` }}
  />]
  if (item.children) {
    item.children.forEach((item) => {
      options = options.concat(_renderOption(item, idx + 1))
    })
  }
  return options
}

// ~ 目录
class FolderEditDlg extends RbFormHandler {
  state = { scope: 'ALL', ...this.props }
  render() {
    return <RbModal title={`${this.props.id ? '修改' : '新建'}目录`} ref={(c) => this._dlg = c} disposeOnHide={true}>
      <div className="form">
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">名称</label>
          <div className="col-sm-7">
            <input type="text" className="form-control form-control-sm" name="name" value={this.state.name || ''} onChange={this.handleChange} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">上级目录</label>
          <div className="col-sm-7">
            <select className="form-control form-control-sm" name="parent" defaultValue={this.props.parent} onChange={this.handleChange}>
              {__NavTreeData.map((item) => { return _renderOption(item) })}
            </select>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">可见范围</label>
          <div className="col-sm-7 pt-1 down-1">
            <label className="custom-control custom-control-sm custom-radio custom-control-inline">
              <input className="custom-control-input" type="radio" name="scope" checked={this.state.scope === 'ALL'} value="ALL" onChange={this.handleChange} />
              <span className="custom-control-label">公开</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio custom-control-inline">
              <input className="custom-control-input" type="radio" name="scope" checked={this.state.scope === 'SELF'} value="SELF" onChange={this.handleChange} />
              <span className="custom-control-label">私有 (仅自己可见)</span>
            </label>
          </div>
        </div>
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3">
            <button className="btn btn-primary" type="button" onClick={this._post}>确定</button>
            <a className="btn btn-link" onClick={this.hide}>取消</a>
          </div>
        </div>
      </div>
    </RbModal>
  }

  _post = () => {
    let _data = { name: this.state.name, parent: this.state.parent, scope: this.state.scope }
    _data.metadata = { entity: 'AttachmentFolder' }
    this.disabled(true)
    $.post(`${rb.baseUrl}/app/entity/record-save`, JSON.stringify(_data), () => {
      this.hide()
      this.props.call && this.props.call()
    })
  }
}

// ～ 上传
class FileUploadDlg extends RbFormHandler {
  state = { ...this.props }
  render() {
    return <RbModal title="上传文件" ref={(c) => this._dlg = c} disposeOnHide={true}>
      <div className="form">
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">上传目录</label>
          <div className="col-sm-7">
            <select className="form-control form-control-sm" name="inFolder" value={this.state.inFolder || ''} onChange={this.handleChange}>
              {__NavTreeData.map((item) => { return _renderOption(item) })}
            </select>
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">文件</label>
          <div className="col-sm-7">
            <div className="file-field files">
              {(this.state.files || []).map((item) => {
                let fileName = $fileCutName(item)
                return (<div key={'file-' + item} className="img-thumbnail" title={fileName}>
                  <i className="file-icon" data-type={$fileExtName(fileName)} />
                  <span>{fileName}</span>
                  <b title="移除" onClick={() => this._removeFile(item)}><span className="zmdi zmdi-close"></span></b>
                </div>)
              })}
            </div>
            <label className="upload-box">
              点击选择或拖动文件至此
              <input type="file" ref={(c) => this._upload = c} className="hide" />
            </label>
          </div>
        </div>
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3" ref={(c) => this._btns = c}>
            <button className="btn btn-primary" type="button" onClick={this._post}>上传</button>
            <a className="btn btn-link" onClick={this.hide}>取消</a>
          </div>
        </div>
      </div>
    </RbModal>
  }

  componentDidMount() {
    let mp
    $createUploader(this._upload, (res) => {
      if (!mp) mp = new Mprogress({ template: 1, start: true })
      mp.set(res.percent / 100)
    }, (res) => {
      if (mp) mp.end()
      let files = this.state.files || []
      files.push(res.key)
      this.setState({ files: files })
    })
  }

  _removeFile(file) {
    let files = this.state.files
    files.remove(file)
    this.setState({ files: files })
  }

  _post = () => {
    if ((this.state.files || []).length === 0) return

    this.disabled(true)
    $.post(`${rb.baseUrl}/files/post-files?folder=${this.state.inFolder || ''}`, JSON.stringify(this.state.files), (res) => {
      if (res.error_code === 0) {
        this.hide()
        this.props.call && this.props.call()
      } else RbHighbar.error(res.error_msg)
    })
  }
}

// eslint-disable-next-line no-undef
class FilesList2 extends FilesList {

  constructor(props) {
    super(props)
  }

  buildDataUrl(folder) {
    return `${rb.baseUrl}/files/list-file?folder=${folder === 1 ? '' : folder}`
  }
}

const __findPaths = function (active, push) {
  let a = active.find('>a')
  push.unshift([a.text(), a.data('id')])
  let li = active.parent('ul').parent('li')
  if (li.length > 0) __findPaths(li, push)
}

let filesList
let filesNav
let currentFolderId

$(document).ready(() => {
  let clickNav = function (item) {
    filesList && filesList.loadData(item.id)
    currentFolderId = item.id

    let paths = []
    __findPaths($('#navTree li.active'), paths)
    let ol = $('.file-path ol').empty()
    $(paths).each((idx, item) => {
      let active = idx === paths.length - 1
      let li = $(`<li class="breadcrumb-item ${active ? 'active' : ''}"></li>`).appendTo(ol)
      if (active) li.text(item[0])
      else $(`<a href="#!/Folder/${item[1]}">${item[0]}</a>`).appendTo(li)
    })
  }

  // eslint-disable-next-line react/jsx-no-undef
  renderRbcomp(<NavTree call={clickNav} dataUrl={`${rb.baseUrl}/files/list-folder`} />, 'navTree', function () { filesNav = this })
  renderRbcomp(<FilesList2 />, $('.file-viewport'), function () { filesList = this })

  $('.J_add-folder').click(() => renderRbcomp(<FolderEditDlg call={() => filesNav && filesNav.loadData()} />))
  $('.J_upload-file').click(() => renderRbcomp(<FileUploadDlg call={() => filesList && filesList.loadData()} inFolder={currentFolderId} />))
})