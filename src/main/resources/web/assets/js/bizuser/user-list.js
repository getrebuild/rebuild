/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

/* global RbForm, loadDeptTree */

const RbForm_postAfter = RbForm.postAfter
let formPostType = 1
RbForm.postAfter = function (data, next) {
  if (formPostType === 1) RbForm_postAfter(data, next)
  else loadDeptTree()
}

$(document).ready(function () {
  $('.J_new').click(function () {
    formPostType = 1
  })
  $('.J_new-dept').click(function () {
    formPostType = 2
    RbFormModal.create({ title: $L('新建部门'), entity: 'Department', icon: 'accounts' })
  })

  $('.J_imports').click(() => renderRbcomp(<UserImport />))
})

// 用户导入
class UserImport extends RbModalHandler {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <RbModal title={$L('导入用户')} ref={(c) => (this._dlg = c)} disposeOnHide={true}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('上传文件')}</label>
            <div className="col-sm-9">
              <div className="float-left">
                <div className="file-select">
                  <input type="file" className="inputfile" id="upload-input" accept=".xlsx,.xls" data-local="temp" ref={(c) => (this._upload = c)} />
                  <label htmlFor="upload-input" className="btn-secondary">
                    <i className="zmdi zmdi-upload" />
                    <span>{$L('选择文件')}</span>
                  </label>
                </div>
              </div>
              <div className="float-left ml-2" style={{ paddingTop: 8 }}>
                {this.state.uploadFile && <u className="text-bold">{$fileCutName(this.state.uploadFile)}</u>}
              </div>
              <div className="clearfix" />
              <p
                className="form-text mt-0 mb-0 link"
                dangerouslySetInnerHTML={{
                  __html: $L(
                    '请按照 [模板文件](https://getrebuild.com/docs/images/USERS_TEMPLATE.xls) 要求填写并上传，更多说明请 [参考文档](https://getrebuild.com/docs/admin/users#2.%20%E6%89%B9%E9%87%8F%E5%AF%BC%E5%85%A5)'
                  ),
                }}
              />
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right" />
            <div className="col-sm-9">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" ref={(c) => (this._notify = c)} />
                <span className="custom-control-label">
                  {$L('导入成功后发送邮件通知用户')} {window.__PageConfig.serviceMail !== 'true' && <span>({$L('不可用')})</span>}
                </span>
              </label>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary" type="button" onClick={() => this.imports()} ref={(c) => (this._btn = c)}>
                {$L('开始导入')}
              </button>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    const that = this
    $createUploader(
      this._upload,
      () => $mp.start(),
      (res) => {
        $mp.end()
        that.setState({ uploadFile: res.key })
      }
    )
  }

  imports() {
    if (!this.state.uploadFile) return RbHighbar.create($L('请上传文件'))
    $.post(`/admin/bizuser/user-imports?file=${$encode(this.state.uploadFile)}&notify=${$(this._notify).prop('checked')}`, (res) => {
      if (res.error_code === 0) {
        this.__taskid = res.data
        $(this._btn).button('loading')
        this._checkState()
      } else {
        RbHighbar.create(res.error_msg)
      }
    })
  }

  _checkState() {
    $.get(`/commons/task/state?taskid=${this.__taskid}`, (res) => {
      if (res.data && res.data.isCompleted) {
        // $(this._btn).button('reset')
        this.hide()
        RbListPage.reload()
        RbHighbar.success($L('成功导入 %d 用户', res.data.succeeded))
      } else {
        setTimeout(() => this._checkState(), 1000)
      }
    })
  }
}
