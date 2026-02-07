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
          icon="mdi mdi-folder down-1"
          data={__FolderData}
          activeItem={__DEFAULT_ALL}
          onItemClick={(item) => {
            filesList && filesList.loadData(item.id)
            currentFolder = item.id

            const paths = []
            FolderTree._findPaths($('#navTree li.active'), paths)

            const $ol = $('.file-path ol').empty()
            paths.forEach((item) => {
              const $li = $('<li class="breadcrumb-item"></li>').appendTo($ol)
              $li.text(item[0])
            })
            location.hash = `!/Folder/${item.id}`
          }}
          extrasAction={(item) => {
            return item.self ? (
              <RF>
                {rb.isAdminUser && (
                  <span
                    className="action"
                    title={$L('分享')}
                    onClick={() => {
                      // eslint-disable-next-line react/jsx-no-undef
                      renderRbcomp(<FileShare file={item.id} title={$L('分享目录')} />)
                    }}>
                    <i className="zmdi zmdi-share" />
                  </span>
                )}
                <span className="action" onClick={() => FolderTree.handleEdit(item)} title={$L('修改')}>
                  <i className="zmdi zmdi-edit" />
                </span>
                <span className="action" onClick={() => FolderTree.handleDelete(item)} title={$L('删除')}>
                  <i className="zmdi zmdi-delete" />
                </span>
              </RF>
            ) : null
          }}
        />,
        'navTree',
        function () {
          FolderTree._filesNav = this
          // be:v4.0
          const e = (location.hash || '').split('Folder/')[1]
          if (e) this.triggerClick(e)
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
    RbAlert.create(
      <RF>
        <b>{$L('确认删除此目录？')} </b>
        <div>{$L('如果目录内有文件或子目录则不允许删除')}</div>
      </RF>,
      {
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
      }
    )
  },

  _findPaths: function (active, into) {
    const $a = active.find('>a')
    into.unshift([$a.text(), $a.data('id')])
    const $li = active.parent('ul').prev('li')
    if ($li.length > 0) FolderTree._findPaths($li, into)
  },
}

// 渲染目录
const _renderFolder = function (item, idx, disabledItem) {
  idx = idx || 0
  if (item.id === __DEFAULT_ALL) item = { text: $L('无') }

  let options = [
    <option
      key={`opt-${item.id || '0'}`}
      value={item.id || ''}
      disabled={disabledItem && item.id === disabledItem}
      dangerouslySetInnerHTML={{ __html: idx === 0 ? item.text : `${'&nbsp;'.repeat(idx * 3)}${item.text}` }}
    />,
  ]

  if (item.children) {
    item.children.forEach((item) => {
      options = options.concat(_renderFolder(item, idx + 1, disabledItem))
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
      <RbModal title={this.props.id ? $L('修改目录') : $L('新建目录')} ref={(c) => (this._dlg = c)} disposeOnHide>
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
              {rb.isAdminUser && (
                <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                  <input className="custom-control-input" type="radio" name="scope" checked={this.state.scope === 'SPEC'} value="SPEC" onChange={this.handleChange} />
                  <span className="custom-control-label">{$L('指定用户')}</span>
                </label>
              )}
              <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-1">
                <input className="custom-control-input" type="radio" name="scope" checked={this.state.scope === 'SELF'} value="SELF" onChange={this.handleChange} />
                <span className="custom-control-label">{$L('私有 (仅自己可见)')}</span>
              </label>

              <div className={`mt-1 mb-2 ${this.state.scope !== 'SPEC' && 'hide'}`}>
                <UserSelector ref={(c) => (this._UserSelector = c)} defaultValue={this.state.specUsers} />
              </div>
              <div className="form-text mt-0 mb-1">{$L('目录可见范围将影响子目录以及目录内的文件')}</div>
            </div>
          </div>
          <div className="form-group row pt-1">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('上级目录')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" name="parent" defaultValue={this.props.parent || null} onChange={this.handleChange}>
                {__FolderData.map((item) => {
                  return _renderFolder(item)
                })}
              </select>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3">
              <button className="btn btn-primary" type="button" onClick={() => this._post()}>
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

  _post() {
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
  state = { ...this.props, uploadState: 0 }

  render() {
    const files = this.state.files || {}

    return (
      <RbModal title={$L('上传文件')} ref={(c) => (this._dlg = c)} disposeOnHide>
        <div className="form" ref={(c) => (this._$dropArea = c)}>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('上传目录')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" name="inFolder" defaultValue={this.props.inFolder} onChange={this.handleChange}>
                {__FolderData.map((item) => {
                  return _renderFolder(item)
                })}
              </select>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('文件')}</label>
            <div className="col-sm-7">
              <div className="file-field files">
                {Object.keys(files).map((file) => {
                  const state = files[file]
                  return (
                    <div
                      key={file}
                      className="img-thumbnail"
                      title={file}
                      onClick={() => {
                        state.key && RbPreview.create(state.key)
                      }}>
                      <i className="file-icon" data-type={$fileExtName(file)} />
                      <span>{file}</span>
                      <span className="status" style={{ width: `${state.key ? 100 : Math.max(state.percent, 0)}%` }}>
                        {state.key && <i className="zmdi zmdi-check text-success" />}
                        {state.error && <i className="zmdi zmdi-close-circle-o text-danger" />}
                      </span>
                      <b title={$L('移除')} onClick={(e) => this._removeFile(file, e)}>
                        <span className="zmdi zmdi-close" />
                      </b>
                    </div>
                  )
                })}
              </div>
              <label className={`upload-box ${this.state.uploadState === 0 ? '' : 'hide'}`}>
                {$L('粘贴、拖动或点击选择文件')}
                <input type="file" ref={(c) => (this._$upload = c)} className="hide" multiple />
              </label>
              <input type="file" ref={(c) => (this._$uploadForUploader43 = c)} className="hide" multiple />
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className={`btn btn-primary ${this.state.uploadState <= 1 ? '' : 'hide'}`} type="button" onClick={() => this._post()}>
                {$L('开始上传')}
              </button>
              <button className={`btn btn-primary ${this.state.uploadState === 2 ? '' : 'hide'}`} type="button" onClick={() => this._reset()}>
                {$L('继续上传')}
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
    function _FN(file, s) {
      if (fixConcurrency === 1) return
      const files = that.state.files || {}
      if (files[file.name]) {
        files[file.name] = { ...files[file.name], ...s }
        fixConcurrency = 1
        that.setState({ files: files }, () => {
          fixConcurrency = 0
          that._postIfUploaded()
        })
      }
    }

    $createUploader(
      this._$uploadForUploader43,
      (res) => _FN(res.file, { percent: res.percent }),
      (res) => _FN(res.file, { key: res.key }),
      (res) => _FN(res.file, { error: res.error })
    )

    $(this._$upload).on('change', (e) => {
      let files = {}
      for (let i = 0; i < (e.target.files || []).length; i++) {
        let file = e.target.files[i]
        files[file.name] = { file: file, key: null, error: null, percent: 0 } // All keys
      }
      this.setState({ files })
    })

    // 粘贴/拖动
    $dropUpload(this._$dropArea, document, (files) => {
      if (files && files.length) {
        that._$upload.files = files
        $(that._$upload).trigger('change')
      }
    })
  }

  componentWillUnmount() {
    super.componentWillUnmount()
    $(document).off('paste.file')
  }

  _reset() {
    this.setState({ files: {}, uploadState: 0 })
    this._$upload.value = null
    this._$uploadForUploader43.value = null
  }

  _removeFile(fileName, e) {
    e && $stopEvent(e, true)
    const files = this.state.files || {}
    delete files[fileName]
    this.setState({ files: files }, () => this._postIfUploaded())
  }

  _post(notip) {
    if ($empty(this.state.files)) {
      if (!notip) RbHighbar.create($L('请选择文件'))
      return
    }

    let fileNames = []
    let dt = new DataTransfer()
    for (let name in this.state.files) {
      const file = this.state.files[name]
      if (file) {
        dt.items.add(file.file)
        fileNames.push(file.file.name)
      }
    }

    const that = this
    function _FN() {
      that._$uploadForUploader43.files = dt.files
      $(that._$uploadForUploader43).trigger('change')
      that.disabled(true)
      that.setState({ uploadState: 1 })
    }

    this.__lastExistsFiles = null
    $.post(`/files/check-files?folder=${this.state.inFolder || ''}`, JSON.stringify(fileNames), (res) => {
      if (res.error_code === 0) {
        const existsFiles = res.data || {}
        if (Object.keys(existsFiles).length) {
          _showExists43(
            existsFiles,
            () => {
              // 覆盖
              this.__lastExistsFiles = existsFiles
              _FN()
            },
            () => {
              // 跳过
              const filesNew = this.state.files || {}
              Object.keys(existsFiles).forEach((fileName) => {
                delete filesNew[fileName]
              })
              this.setState({ files: filesNew }, () => {
                this._post(true)
                if ($empty(filesNew)) this._reset()
              })
            }
          )
        } else {
          _FN()
        }
      } else {
        RbHighbar.error(res.error_msg)
        this.disabled()
      }
    })
  }

  _postIfUploaded() {
    let fileKeys = []
    for (let k in this.state.files) {
      let file = this.state.files[k]
      if (file && file.key) {
        fileKeys.push(file.key)
      } else {
        fileKeys = null
        break
      }
    }
    if (!fileKeys) return

    if (fileKeys.length === 0) {
      this.disabled()
      this.setState({ uploadState: 0 })
      RbHighbar.create($L('请选择文件'))
      return
    }

    let url = `/files/post-files?folder=${this.state.inFolder || ''}`
    if (this.__lastExistsFiles) url += '&deletes=' + Object.values(this.__lastExistsFiles).join(',')
    $.post(url, JSON.stringify(fileKeys), (res) => {
      this.disabled()
      this.setState({ uploadState: 2 })
      if (res.error_code === 0) {
        typeof this.props.call === 'function' && this.props.call()
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

function _showExists43(existsFiles, onConfirm, onCancel) {
  const checkMsg = (
    <div>
      <h5 className="text-bold">{$L('存在同名文件，是否覆盖？')}</h5>
      <table className="table table-sm table-bordered" style={{ margin: '0 auto', width: 'auto', minWidth: '60%' }}>
        <tbody>
          {Object.keys(existsFiles).map((item, idx) => {
            return (
              <tr key={idx}>
                <td className="text-break">{item}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )

  RbAlert.create(checkMsg, {
    type: 'warning',
    confirmText: $L('覆盖'),
    onConfirm: function () {
      onConfirm && onConfirm()
      this.hide()
    },
    cancelText: $L('跳过'),
    onCancel: function () {
      onCancel && onCancel()
      this.hide()
    },
  })
}

// ~ 移动目录
class FileMoveDlg extends RbFormHandler {
  state = { ...this.props }

  render() {
    return (
      <RbModal title={$L('移动文件')} ref={(c) => (this._dlg = c)} disposeOnHide>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('更改至新目录')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" name="inFolder" onChange={this.handleChange}>
                {__FolderData.map((item) => {
                  return _renderFolder(item)
                })}
              </select>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary" type="button" onClick={() => this._post()}>
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

  _post() {
    const that = this
    function _FN(files, deletes) {
      if ($empty(files)) {
        that.hide()
        return
      }

      let url = `/files/move-files?folder=${that.state.inFolder || ''}&ids=${files.join(',')}`
      if (deletes) url += '&deletes=' + deletes.join(',')
      $.post(url, (res) => {
        if (res.error_code === 0) {
          that.hide()
          that.props.call && that.props.call()
        } else {
          RbHighbar.error(res.error_msg)
          that.disabled()
        }
      })
    }

    this.disabled(true)
    $.post(`/files/check-files?folder=${this.state.inFolder || ''}`, JSON.stringify(this.props.files), (res) => {
      if (res.error_code === 0) {
        const existsFiles = res.data || {}
        if (Object.keys(existsFiles).length) {
          const existsFilesId = Object.values(existsFiles)
          _showExists43(
            existsFiles,
            () => {
              // 覆盖
              _FN(this.props.files, existsFilesId)
            },
            () => {
              // 跳过
              const filesNew = []
              for (let k in this.props.filesName) {
                const name = this.props.filesName[k]
                if (!existsFiles[name]) filesNew.push(k)
              }

              _FN(filesNew, null)
            }
          )
        } else {
          _FN(this.props.files)
        }
      } else {
        RbHighbar.error(res.error_msg)
        this.disabled()
      }
    })
  }
}

// eslint-disable-next-line no-undef
class FilesList4Docs extends FilesList {
  constructor(props) {
    super(props)
    this._lastEntry = __DEFAULT_ALL
  }

  renderExtras34(item) {
    return (
      <div className="info position-relative">
        <span className="fop-action">
          <a title={$L('修改')} onClick={(e) => this._handleEdit(item, e)}>
            <i className="icon mdi mdi-square-edit-outline" />
          </a>
          <a title={$L('下载')} onClick={(e) => $stopEvent(e)} href={`${rb.baseUrl}/files/download?id=${item.id}`} target="_blank">
            <i className="icon zmdi zmdi-download" />
          </a>
          {rb.fileSharable && (
            <a title={$L('分享')} onClick={(e) => this._handleShare(item, e)}>
              <i className="icon zmdi zmdi-share fs-16 up-1" />
            </a>
          )}
        </span>
      </div>
    )
  }

  _handleEdit(item, e) {
    $stopEvent(e, true)
    renderRbcomp(
      <FileRename
        fileId={item.id}
        fileKey={item.fileName}
        onConfirm={(newName, dlg) => {
          if (!newName || !dlg) return
          dlg.disabled(true)
          $.post(`/files/file-edit?newName=${$encode(newName)}&id=${item.id}`, (res) => {
            if (res.error_code === 0) {
              dlg.hide()
              filesList && filesList.loadData()
            } else {
              RbHighbar.error(res.error_msg)
              dlg.disabled()
            }
          })
        }}
      />
    )
  }

  _handleShare(item, e) {
    $stopEvent(e)
    // eslint-disable-next-line react/jsx-no-undef
    renderRbcomp(<FileShare file={item.id} />)
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
    RbAlert.create(<b>{$L('确认删除选中的 %d 个文件？', s.length)}</b>, {
      type: 'danger',
      confirmText: $L('删除'),
      confirm: function () {
        this.disabled(true)
        $.post(`/files/delete-files?ids=${s.join(',')}`, (res) => {
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

    const filesName43 = {}
    filesList.state.files.forEach((item) => {
      if (s.includes(item.id)) filesName43[item.id] = item.fileName
    })
    renderRbcomp(<FileMoveDlg files={s} filesName={filesName43} call={() => filesList.loadData()} />)
  })

  $('a.J_dl').on('click', () => {
    const s = filesList.getSelected()
    if (!s) return

    const $form = $('form.J_dl')
    $form.find('input').val(s.join(','))
    $form[0].submit()
  })
})
