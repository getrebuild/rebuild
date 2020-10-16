/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global filesList */

// 文档
const __DEFAULT_ALL = 'ALL'
let __FolderData = []

// 渲染目录
const _renderOption = function (item, idx, disabledItem) {
  idx = idx || 0
  if (item.id === 1) item = { text: $L('None') }
  let options = [
    <option
      key={`opt-${item.id}`}
      value={item.id || ''}
      disabled={disabledItem && item.id === disabledItem}
      dangerouslySetInnerHTML={{ __html: idx === 0 ? item.text : `${'&nbsp;'.repeat(idx * 3)}${item.text}` }}
    />,
  ]

  if (item.children) {
    item.children.forEach((item) => {
      options = options.concat(_renderOption(item, idx + 1, disabledItem))
    })
  }
  return options
}

// ~ 目录
class FolderEditDlg extends RbFormHandler {
  state = { scope: 'ALL', ...this.props }

  render() {
    return (
      <RbModal title={`${$L(this.props.id ? 'ModifySome,Folder' : 'NewSome,Folder')}`} ref={(c) => (this._dlg = c)} disposeOnHide={true}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('FolderName')}</label>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="name" value={this.state.name || ''} onChange={this.handleChange} />
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('ViewScope')}</label>
            <div className="col-sm-7 pt-1 down-1">
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                <input className="custom-control-input" type="radio" name="scope" checked={this.state.scope === 'ALL'} value="ALL" onChange={this.handleChange} />
                <span className="custom-control-label">{$L('Public')}</span>
              </label>
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                <input className="custom-control-input" type="radio" name="scope" checked={this.state.scope === 'SELF'} value="SELF" onChange={this.handleChange} />
                <span className="custom-control-label">{$L('PrivateTips')}</span>
              </label>
              <div className="form-text mb-1">{$L('ViewScopeTips')}</div>
            </div>
          </div>
          <div className="form-group row pt-1">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('ParentFolder')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" name="parent" onChange={this.handleChange}>
                {__FolderData.map((item) => {
                  return _renderOption(item, 0, this.props.id)
                })}
              </select>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3">
              <button className="btn btn-primary" type="button" onClick={this._post}>
                {$L('Confirm')}
              </button>
              <a className="btn btn-link" onClick={this.hide}>
                {$L('Cancel')}
              </a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  _post = () => {
    let _data = { name: this.state.name, parent: this.state.parent, scope: this.state.scope }
    if (!_data.name) {
      RbHighbar.create($L('PlsInputSome,FolderName'))
      return
    }
    _data.metadata = { entity: 'AttachmentFolder', id: this.props.id || null }

    this.disabled(true)
    $.post('/app/entity/record-save', JSON.stringify(_data), () => {
      this.hide()
      typeof this.props.call === 'function' && this.props.call()
    })
  }
}

// ~ 上传
class FileUploadDlg extends RbFormHandler {
  state = { ...this.props }

  render() {
    return (
      <RbModal title={$L('UploadSome,File')} ref={(c) => (this._dlg = c)} disposeOnHide={true}>
        <div className="form" ref={(c) => (this._dropArea = c)}>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('UploadSome,Folder')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" name="inFolder" defaultValue={this.props.inFolder} onChange={this.handleChange}>
                {__FolderData.map((item) => {
                  return _renderOption(item)
                })}
              </select>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('File')}</label>
            <div className="col-sm-7">
              <div className="file-field files">
                {(this.state.files || []).map((item) => {
                  let fileName = $fileCutName(item)
                  return (
                    <div key={'file-' + item} className="img-thumbnail" title={fileName}>
                      <i className="file-icon" data-type={$fileExtName(fileName)} />
                      <span>{fileName}</span>
                      <b title={$L('Remove')} onClick={() => this._removeFile(item)}>
                        <span className="zmdi zmdi-close"></span>
                      </b>
                    </div>
                  )
                })}
              </div>
              <label className="upload-box">
                {$L('DragToUpload')}
                <input type="file" ref={(c) => (this._upload = c)} className="hide" data-maxsize="102400000" />
              </label>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary" type="button" onClick={this._post}>
                {$L('Upload')}
              </button>
              <a className="btn btn-link" onClick={this.hide}>
                {$L('Cancel')}
              </a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    let mp
    const mp_end = function () {
      if (mp) mp.end()
      mp = null
    }
    $createUploader(
      this._upload,
      (res) => {
        if (!mp) mp = new Mprogress({ template: 1, start: true })
        mp.set(res.percent / 100)
      },
      (res) => {
        const files = this.state.files || []
        files.push(res.key)
        this.setState({ files: files })
        mp_end()
      },
      () => mp_end()
    )

    // 拖拽上传
    const $da = $(this._dropArea)
    $da.on('dragenter', (e) => e.preventDefault())
    $da.on('dragleave', (e) => {
      e.preventDefault()
      $da.find('.upload-box').removeClass('active')
    })
    $da.on('dragover', (e) => {
      e.preventDefault()
      $da.find('.upload-box').addClass('active')
    })
    const that = this
    $da.on('drop', function (e) {
      e.preventDefault()
      const files = e.originalEvent.dataTransfer.files
      if (!files || files.length === 0) return false
      that._upload.files = files
      $(that._upload).trigger('change')
      $da.find('.upload-box').removeClass('active')
    })
  }

  _removeFile(file) {
    const files = this.state.files
    files.remove(file)
    this.setState({ files: files })
  }

  _post = () => {
    if ((this.state.files || []).length === 0) return RbHighbar.create($L('PlsSelectSome,File'))

    this.disabled(true)
    $.post(`/files/post-files?folder=${this.state.inFolder || ''}`, JSON.stringify(this.state.files), (res) => {
      if (res.error_code === 0) {
        this.hide()
        this.props.call && this.props.call()
      } else RbHighbar.error(res.error_msg)
    })
  }
}

// ~ 移动目录
class FileMoveDlg extends RbFormHandler {
  state = { ...this.props }

  render() {
    return (
      <RbModal title={$L('ModifySome,Folder')} ref={(c) => (this._dlg = c)} disposeOnHide={true}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('ChangToFolder')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" name="inFolder" onChange={this.handleChange}>
                {__FolderData.map((item) => {
                  return _renderOption(item)
                })}
              </select>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary" type="button" onClick={this._post}>
                {$L('Confirm')}
              </button>
              <a className="btn btn-link" onClick={this.hide}>
                {$L('Cancel')}
              </a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  _post = () => {
    this.disabled(true)
    $.post(`/files/move-files?folder=${this.state.inFolder || ''}&ids=${this.props.files.join(',')}`, (res) => {
      if (res.error_code === 0) {
        this.hide()
        this.props.call && this.props.call()
      } else RbHighbar.error(res.error_msg)
    })
  }
}

// ~ 目录树
class FolderTree extends React.Component {
  state = { activeItem: __DEFAULT_ALL, ...this.props }

  render() {
    return (
      <div className="aside-tree p-0">
        <ul className="list-unstyled">
          {(this.state.list || []).map((item) => {
            return this._renderItem(item)
          })}
        </ul>
      </div>
    )
  }

  _renderItem(item) {
    return (
      <li key={`folder-${item.id}`} className={this.state.activeItem === item.id ? 'active' : ''}>
        <a data-id={item.id} onClick={() => this._clickItem(item)} href={`#!/Folder/${item.id}`}>
          {item.text}
          {item.private && <i title={$L('Private')} className="icon zmdi zmdi-lock" />}
        </a>
        {item.self && (
          <div className="action">
            <a onClick={() => this._handleEdit(item)}>
              <i className="zmdi zmdi-edit"></i>
            </a>
            <a onClick={() => this._handleDelete(item.id)}>
              <i className="zmdi zmdi-delete"></i>
            </a>
          </div>
        )}
        {item.children && (
          <ul className="list-unstyled">
            {item.children.map((item) => {
              return this._renderItem(item)
            })}
          </ul>
        )}
      </li>
    )
  }

  _clickItem(item) {
    this.setState({ activeItem: item.id }, () => {
      this.props.call && this.props.call(item)
    })
  }

  _handleEdit(item) {
    event.preventDefault()
    renderRbcomp(<FolderEditDlg call={() => filesNav && filesNav.loadData()} id={item.id} name={item.text} scope={item.private ? 'SELF' : 'ALL'} parent={item.parent} />)
  }

  _handleDelete(id) {
    event.preventDefault()
    const that = this
    RbAlert.create($L('DelFolderTips'), {
      type: 'danger',
      confirmText: $L('Delete'),
      confirm: function () {
        this.disabled(true)
        $.post(`/app/entity/record-delete?id=${id}`, (res) => {
          if (res.error_code === 0) {
            this.hide()
            that.loadData()
          } else RbHighbar.error(res.error_msg)
        })
      },
    })
  }

  componentDidMount = () => this.loadData()

  loadData() {
    $.get('/files/list-folder', (res) => {
      const data = res.data || []
      data.unshift({ id: __DEFAULT_ALL, text: $L('All') })
      this.setState({ list: data })
      __FolderData = data
    })
  }
}

// eslint-disable-next-line no-undef
class FilesList2 extends FilesList {
  state = { ...this.props }
  __lastEntry = __DEFAULT_ALL

  renderExtras(item) {
    return (
      <React.Fragment>
        {super.renderExtras(item)}
        <span className="op">
          <a title={$L('Download')} onClick={(e) => $stopEvent(e)} href={`${rb.baseUrl}/filex/download/${item.filePath}?attname=${$fileCutName(item.filePath)}`} target="_blank">
            <i className="icon zmdi zmdi-download fs-15 down-2"></i>
          </a>
          {rb.fileSharable && (
            <a title={$L('Share')} onClick={(e) => this._share(item, e)}>
              <span>
                <i className="icon zmdi zmdi-share fs-14 down-1"></i>
              </span>
            </a>
          )}
        </span>
      </React.Fragment>
    )
  }

  _share(item, e) {
    $stopEvent(e)
    // eslint-disable-next-line react/jsx-no-undef
    renderRbcomp(<FileShare file={item.filePath} />)
  }
}

const __findPaths = function (active, push) {
  const a = active.find('>a')
  push.unshift([a.text(), a.data('id')])
  const li = active.parent('ul').parent('li')
  if (li.length > 0) __findPaths(li, push)
}

let filesNav
let currentFolder

$(document).ready(() => {
  const clickNav = function (item) {
    filesList && filesList.loadData(item.id)
    currentFolder = item.id

    const paths = []
    __findPaths($('#navTree li.active'), paths)
    const ol = $('.file-path ol').empty()
    $(paths).each((idx, item) => {
      $(`<li class="breadcrumb-item active">${item[0]}</li>`).appendTo(ol)
    })
  }
  renderRbcomp(<FolderTree call={clickNav} />, 'navTree', function () {
    filesNav = this
  })
  renderRbcomp(<FilesList2 />, $('.file-viewport'), function () {
    // eslint-disable-next-line no-global-assign
    filesList = this
  })

  $('.J_add-folder').click(() => renderRbcomp(<FolderEditDlg call={() => filesNav && filesNav.loadData()} />))
  $('.J_upload-file').click(() => renderRbcomp(<FileUploadDlg call={() => filesList && filesList.loadData()} inFolder={currentFolder} />))

  $('.J_delete').click(() => {
    const s = filesList.getSelected()
    if (!s) return
    RbAlert.create($L('DeleteSomeConfirm,File'), {
      type: 'danger',
      confirmText: $L('Delete'),
      confirm: function () {
        this.disabled(true)
        $.post(`/files/delete-files?ids=${s}`, (res) => {
          if (res.error_code === 0) {
            this.hide()
            filesList.loadData()
          } else {
            this.disabled()
            RbHighbar.error(res.error_msg)
          }
        })
      },
    })
  })

  $('.J_move').click(() => {
    const s = filesList.getSelected()
    if (!s) return
    renderRbcomp(<FileMoveDlg files={[s]} call={() => filesList && filesList.loadData()} />)
  })
})
