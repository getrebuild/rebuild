/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

/* global RbForm, loadDeptTree */

let formPostType = 1

const RbForm_postAfter = RbForm.postAfter
RbForm.postAfter = function (data, next) {
  if (formPostType === 1) RbForm_postAfter(data, next)
  else loadDeptTree()
}

RbList.queryBefore = function (query) {
  if (!$val('#showAllUsers')) {
    query.filterAnd = {
      items: [{ field: 'isDisabled', op: 'EQ', value: 'F' }],
    }
  }
  return query
}

$(document).ready(() => {
  $('.J_new').on('click', () => {
    formPostType = 1
  })
  $('.J_new-dept').on('click', () => {
    formPostType = 2
    RbFormModal.create({ title: $L('新建部门'), entity: 'Department', icon: 'accounts' })
  })

  $('.J_imports').on('click', () => renderRbcomp(<UserImport />))
  $('.J_resign').on('click', () => renderRbcomp(<UserResigntion />))

  $('#showAllUsers').on('change', () => {
    RbListPage._RbList.reload()
  })
})

// 用户导入
class UserImport extends RbModalHandler {
  render() {
    return (
      <RbModal title={$L('导入用户')} ref={(c) => (this._dlg = c)} disposeOnHide>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('上传文件')}</label>
            <div className="col-sm-9">
              <div className="float-left">
                <div className="file-select">
                  <input type="file" className="inputfile" id="upload-input" accept=".xlsx,.xls" data-local="temp" ref={(c) => (this._$upload = c)} />
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
                <input className="custom-control-input" type="checkbox" ref={(c) => (this._$notify = c)} />
                <span className="custom-control-label">
                  {$L('导入成功后发送邮件通知用户')} {window.__PageConfig.serviceMail !== 'true' && <span>({$L('不可用')})</span>}
                </span>
              </label>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3">
              <button className="btn btn-primary" type="button" onClick={() => this.imports()} ref={(c) => (this._$btn = c)}>
                {$L('开始导入')}
              </button>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    $createUploader(
      this._$upload,
      () => $mp.start(),
      (res) => {
        $mp.end()
        this.setState({ uploadFile: res.key })
      }
    )
  }

  imports() {
    if (!this.state.uploadFile) return RbHighbar.create($L('请上传文件'))

    $.post(`/admin/bizuser/user-imports?file=${$encode(this.state.uploadFile)}&notify=${$val(this._$notify)}`, (res) => {
      if (res.error_code === 0) {
        $(this._$btn).button('loading')

        this.__taskid = res.data
        this._checkState()
      } else {
        RbHighbar.create(res.error_msg)
      }
    })
  }

  _checkState() {
    $.get(`/commons/task/state?taskid=${this.__taskid}`, (res) => {
      if (res.data && res.data.isCompleted) {
        // $(this._$btn).button('reset')
        this.hide()
        RbListPage.reload()
        RbHighbar.success($L('成功导入 %d 用户', res.data.succeeded))
      } else {
        setTimeout(() => this._checkState(), 1000)
      }
    })
  }
}

// 离职继任
class UserResigntion extends RbModalHandler {
  render() {
    const title = (
      <RF>
        {$L('离职继任')}
        <sup className="rbv" title={$L('增值功能')} />
      </RF>
    )

    return (
      <RbModal title={title} ref={(c) => (this._dlg = c)} disposeOnHide>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('离职用户')}</label>
            <div className="col-sm-7">
              <UserSelector hideDepartment hideRole hideTeam multiple={false} ref={(c) => (this._UserSelector1 = c)} />
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('继任用户')}</label>
            <div className="col-sm-7">
              <UserSelector hideDepartment hideRole hideTeam multiple={false} ref={(c) => (this._UserSelector2 = c)} />
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right"></label>
            <div className="col-sm-7">
              <label>
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                  <input className="custom-control-input" type="checkbox" ref={(c) => (this._$disabled = c)} />
                  <span className="custom-control-label">{$L('同时禁用离职用户')}</span>
                </label>
              </label>
              <p className="form-text mt-1">{$L('离职继任将把离职用户的业务数据 (所属用户、共享用户)、审批中的记录 (审批人) 转移至继任用户')}</p>
            </div>
          </div>

          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3">
              <button className="btn btn-primary" type="button" onClick={() => this.start()} ref={(c) => (this._$btn = c)}>
                {$L('开始转移')}
              </button>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  start() {
    if (rb.commercial < 10) {
      RbHighbar.error(WrapHtml($L('免费版不支持离职继任功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }

    const post = {
      oldUser: this._UserSelector1.val()[0],
      newUser: this._UserSelector2.val()[0],
      disabledOldUser: $val(this._$disabled),
    }
    if (!post.oldUser) return RbHighbar.create($L('请选择离职用户'))
    if (!post.newUser) return RbHighbar.create($L('请选择继任用户'))
    if (post.oldUser === post.newUser) return RbHighbar.create($L('不能是同一个用户'))

    const that = this
    RbAlert.create($L('如数据较多耗时会较长，请耐心等待。确定转移吗？'), {
      onConfirm: function () {
        this.hide()

        const $btn = $(that._$btn).button('loading')
        $mp.start()

        $.post('/admin/bizuser/user-resignation', JSON.stringify(post), (res) => {
          $mp.end()

          if (res.error_code === 0) {
            RbHighbar.success($L('数据转移完成'))
            setTimeout(() => that.hide(), 1500)
          } else {
            RbHighbar.error(res.error_msg)
            $btn.button('reset')
          }
        })
      },
    })
  }

  _checkState() {}
}
