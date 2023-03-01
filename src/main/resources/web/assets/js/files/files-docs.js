/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global filesList */

// 文件
const __DEFAULT_ALL = 'ALL'

let __FolderData = []
let currentFolder

// 目录树
const FolderTree = {
  _filesNav: null,

  load: function () {
    $.get('/files/tree-folder', (res) => {
      __FolderData = [{ id: __DEFAULT_ALL, text: $L('全部') }, ...res.data]

      if (FolderTree._filesNav) {
        ReactDOM.unmountComponentAtNode(document.getElementById('navTree'))
        FolderTree._filesNav = null
      }

      renderRbcomp(
        <AsideTree
          data={__FolderData}
          activeItem={__DEFAULT_ALL}
          onItemClick={(item) => {
            filesList && filesList.loadData(item.id)
            currentFolder = item.id

            const paths = []
            FolderTree._findPaths($('#navTree li.active'), paths)
            const $ol = $('.file-path ol').empty()
            $(paths).each((idx, item) => {
              const $li = $('<li class="breadcrumb-item active"></li>').appendTo($ol)
              $li.text(item[0])
            })
            location.hash = `!/Folder/${item.id}`
          }}
          extrasAction={(item) => {
            return item.self ? (
              <RF>
                <span className="action" onClick={() => FolderTree.handleEdit(item)}>
                  <i className="zmdi zmdi-edit" />
                </span>
                <span className="action" onClick={() => FolderTree.handleDelete(item)}>
                  <i className="zmdi zmdi-delete" />
                </span>
              </RF>
            ) : null
          }}
        />,
        'navTree',
        function () {
          FolderTree._filesNav = this
        }
      )
    })
  },

  handleEdit: function (item) {
    let scope = item.private ? 'SELF' : 'ALL'
    if (item.specUsers) scope = item.specUsers
    renderRbcomp(
      <FolderEditDlg
        call={() => {
          FolderTree.load()
          filesList && filesList.loadData(__DEFAULT_ALL)
        }}
        id={item.id}
        name={item.text}
        scope={scope}
        parent={item.parent}
      />
    )
  },

  handleDelete: function (item) {
    RbAlert.create($L('如果目录内有文件或子目录则不允许删除。确认删除吗？'), {
      type: 'danger',
      confirmText: $L('删除'),
      confirm: function () {
        this.disabled(true)
        $.post(`/app/entity/common-delete?id=${item.id}`, (res) => {
          if (res.error_code === 0) {
            this.hide()
            FolderTree.load()
          } else {
            RbHighbar.error(res.error_msg)
            this.disabled()
          }
        })
      },
    })
  },

  _findPaths: function (active, into) {
    const $a = active.find('>a')
    into.unshift([$a.text(), $a.data('id')])
    const $li = active.parent('ul').prev('li')
    if ($li.length > 0) FolderTree._findPaths($li, into)
  },
}

// 渲染目录
const _renderOption = function (item, idx, disabledItem) {
  idx = idx || 0
  if (item.id === __DEFAULT_ALL) item = { text: $L('无') }

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
  constructor(props) {
    super(props)
    this.state = { scope: 'ALL', ...props }

    if (props.scope && props.scope.length >= 20) {
      this.state.scope = 'SPEC'
      this.state.specUsers = props.scope.split(',')
    }
  }

  render() {
    return (
      <RbModal title={this.props.id ? $L('修改目录') : $L('新建目录')} ref={(c) => (this._dlg = c)} disposeOnHide={true}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('目录名称')}</label>
            <div className="col-sm-7">
              <input type="text" className="form-control form-control-sm" name="name" value={this.state.name || ''} onChange={this.handleChange} />
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('可见范围')}</label>
            <div className="col-sm-7 pt-1">
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                <input className="custom-control-input" type="radio" name="scope" checked={this.state.scope === 'ALL'} value="ALL" onChange={this.handleChange} />
                <span className="custom-control-label">{$L('公开')}</span>
              </label>
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                <input className="custom-control-input" type="radio" name="scope" checked={this.state.scope === 'SPEC'} value="SPEC" onChange={this.handleChange} />
                <span className="custom-control-label">{$L('指定用户')}</span>
              </label>
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                <input className="custom-control-input" type="radio" name="scope" checked={this.state.scope === 'SELF'} value="SELF" onChange={this.handleChange} />
                <span className="custom-control-label">{$L('私有 (仅自己可见)')}</span>
              </label>

              <div className={`mt-1 mb-2 ${this.state.scope !== 'SPEC' && 'hide'}`}>
                <UserSelector ref={(c) => (this._UserSelector = c)} defaultValue={this.state.specUsers} />
              </div>
              <div className="form-text mb-1">{$L('目录可见范围将影响子目录以及目录内的文件')}</div>
            </div>
          </div>
          <div className="form-group row pt-1">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('上级目录')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" name="parent" defaultValue={this.props.parent || null} onChange={this.handleChange}>
                {__FolderData.map((item) => {
                  return _renderOption(item, 0, this.props.id)
                })}
              </select>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3">
              <button className="btn btn-primary" type="button" onClick={this._post}>
                {$L('确定')}
              </button>
              <a className="btn btn-link" onClick={this.hide}>
                {$L('取消')}
              </a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  _post = () => {
    const _data = {
      name: this.state.name,
      parent: this.state.parent,
      scope: this.state.scope,
    }
    if (!_data.name) return RbHighbar.create($L('请输入目录名称'))
    if (_data.scope === 'SPEC') {
      const s = this._UserSelector.val()
      if (s.length === 0) return RbHighbar.create($L('请选择指定用户'))
      _data.scope = s.join(',')
    }

    _data.metadata = {
      entity: 'AttachmentFolder',
      id: this.props.id || null,
    }

    this.disabled(true)
    $.post('/app/entity/common-save', JSON.stringify(_data), () => {
      this.hide()
      typeof this.props.call === 'function' && this.props.call()
    })
  }
}

// ~ 上传
class FileUploadDlg extends RbFormHandler {
  state = { ...this.props }

  render() {
    const files = this.state.files || {}

    return (
      <RbModal title={$L('上传文件')} ref={(c) => (this._dlg = c)} disposeOnHide={true}>
        <div className="form" ref={(c) => (this._dropArea = c)}>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('上传目录')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" name="inFolder" defaultValue={this.props.inFolder} onChange={this.handleChange}>
                {__FolderData.map((item) => {
                  return _renderOption(item)
                })}
              </select>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('文件')}</label>
            <div className="col-sm-7">
              <div className="file-field files">
                {Object.keys(files).map((file) => {
                  let s = files[file]

                  return (
                    <div key={file} className="img-thumbnail" title={file}>
                      <i className="file-icon" data-type={$fileExtName(file)} />
                      <span>{file}</span>
                      <span className="status" style={{ width: `${isNaN(s) ? 100 : Math.max(s, 0)}%` }}>
                        {isNaN(s) && <i className="zmdi zmdi-check text-success" />}
                      </span>
                      <b title={$L('移除')} onClick={() => this._removeFile(file)}>
                        <span className="zmdi zmdi-close" />
                      </b>
                    </div>
                  )
                })}
              </div>
              <label className="upload-box">
                {$L('点击选择或拖动文件至此')}
                <input type="file" ref={(c) => (this._$upload = c)} className="hide" multiple />
              </label>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary" type="button" onClick={this._post}>
                {$L('上传')}
              </button>
              <a className="btn btn-link" onClick={this.hide}>
                {$L('取消')}
              </a>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    const that = this

    let fixConcurrency = 0
    function fn(file, s) {
      if (fixConcurrency === 1) return
      const files = that.state.files || {}
      files[file.name] = s
      that.setState({ files: files }, () => (fixConcurrency = 0))
    }

    $createUploader(
      this._$upload,
      (res) => fn(res.file, res.percent),
      (res) => fn(res.file, res.key)
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

    $da.on('drop', function (e) {
      e.preventDefault()
      const files = e.originalEvent.dataTransfer.files
      if (!files || files.length === 0) return false
      that._$upload.files = files
      $(that._$upload).trigger('change')
      $da.find('.upload-box').removeClass('active')
    })

    // Ctrl+V 上传
    document.addEventListener('paste', (e) => {
      const data = e.clipboardData || window.clipboardData
      if (data && data.items && data.files && data.files.length > 0) {
        that._$upload.files = data.files
        $(that._$upload).trigger('change')
      }
    })
  }

  componentWillUnmount() {
    super.componentWillUnmount()
    $(document).off('paste')
  }

  _removeFile(file) {
    const files = this.state.files || {}
    delete files[file]
    this.setState({ files: files })
  }

  _post = () => {
    if (!this.state.files || Object.keys(this.state.files).length === 0) return RbHighbar.create($L('请选择文件'))

    let files = []
    for (let k in this.state.files) {
      const file = this.state.files[k]
      if (file && isNaN(file)) files.push(file)
    }

    this.disabled(true)
    $.post(`/files/post-files?folder=${this.state.inFolder || ''}`, JSON.stringify(files), (res) => {
      if (res.error_code === 0) {
        this.hide()
        this.props.call && this.props.call()
      } else {
        RbHighbar.error(res.error_msg)
        this.disabled()
      }
    })
  }
}

// ~ 移动目录
class FileMoveDlg extends RbFormHandler {
  state = { ...this.props }

  render() {
    return (
      <RbModal title={$L('移动文件')} ref={(c) => (this._dlg = c)} disposeOnHide={true}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('更改至新目录')}</label>
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
                {$L('确定')}
              </button>
              <a className="btn btn-link" onClick={this.hide}>
                {$L('取消')}
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
      } else {
        RbHighbar.error(res.error_msg)
        this.disabled()
      }
    })
  }
}

// eslint-disable-next-line no-undef
class FilesList4Docs extends FilesList {
  state = { ...this.props }
  __lastEntry = __DEFAULT_ALL

  renderExtras(item) {
    return (
      <RF>
        <span className="fop">
          <a title={$L('下载')} className="fs-15" onClick={(e) => $stopEvent(e)} href={`${rb.baseUrl}/filex/download/${item.filePath}?attname=${$fileCutName(item.filePath)}`} target="_blank">
            <i className="icon zmdi zmdi-download" />
          </a>
          {rb.fileSharable && (
            <a title={$L('分享')} onClick={(e) => this.handleShare(item, e)}>
              <i className="icon zmdi zmdi-share" />
            </a>
          )}
        </span>
      </RF>
    )
  }

  handleShare(item, e) {
    $stopEvent(e)
    // eslint-disable-next-line react/jsx-no-undef
    renderRbcomp(<FileShare file={item.filePath} />)
  }
}

$(document).ready(() => {
  FolderTree.load()

  renderRbcomp(<FilesList4Docs />, $('.file-viewport'), function () {
    // eslint-disable-next-line no-global-assign
    filesList = this
  })

  $('.J_add-folder').on('click', () => renderRbcomp(<FolderEditDlg call={() => FolderTree.load()} />))
  $('.J_upload-file').on('click', () => renderRbcomp(<FileUploadDlg call={() => filesList && filesList.loadData()} inFolder={currentFolder} />))

  $('.J_delete').on('click', () => {
    const s = filesList.getSelected()
    if (!s) return
    RbAlert.create($L('确认删除此文件？'), {
      type: 'danger',
      confirmText: $L('删除'),
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

  $('.J_move').on('click', () => {
    const s = filesList.getSelected()
    if (!s) return
    renderRbcomp(<FileMoveDlg files={[s]} call={() => filesList && filesList.loadData()} />)
  })
})
