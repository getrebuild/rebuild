/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

/* global RbForm, loadDeptTree */

const RbForm_postAfter = RbForm.postAfter

let formPostType = 1
RbForm.postAfter = function () {
  if (formPostType === 1) RbForm_postAfter()
  else loadDeptTree()
}

$(document).ready(function () {
  loadDeptTree()
  $('.J_new').click(function () {
    formPostType = 1
  })
  $('.J_new-dept').click(function () {
    formPostType = 2
    RbFormModal.create({ title: $L('NewSome,Department'), entity: 'Department', icon: 'accounts' })
  })
})

// eslint-disable-next-line no-undef
clickDept = function (depts) {
  if (depts[0] === '$ALL$') depts = []
  let exp = { items: [], values: {} }
  exp.items.push({ op: 'in', field: 'deptId', value: '{2}' })
  exp.values['2'] = depts
  RbListPage._RbList.search(depts.length === 0 ? {} : exp)
}

$(document).ready(() => {
  $('.J_imports').click(() => renderRbcomp(<UserImport />))
})

// 用户导入
class UserImport extends RbModalHandler {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <RbModal title={$L('ImportUser')} ref={(c) => (this._dlg = c)} disposeOnHide={true}>
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('UploadFile')}</label>
            <div className="col-sm-9">
              <div className="float-left">
                <div className="file-select">
                  <input type="file" className="inputfile" id="upload-input" accept=".xlsx,.xls" data-maxsize="5000000" data-temp="true" ref={(c) => (this._upload = c)} />
                  <label htmlFor="upload-input" className="btn-secondary">
                    <i className="zmdi zmdi-upload"></i>
                    <span>{$L('SelectFile')}</span>
                  </label>
                </div>
              </div>
              <div className="float-left ml-2" style={{ paddingTop: 8 }}>
                {this.state.uploadFile && <u className="text-bold">{$fileCutName(this.state.uploadFile)}</u>}
              </div>
              <div className="clearfix"></div>
              <p className="form-text mt-0 mb-0 link" dangerouslySetInnerHTML={{ __html: $L('ImportUserTips') }}></p>
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right"></label>
            <div className="col-sm-9">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                <input className="custom-control-input" type="checkbox" ref={(c) => (this._notify = c)} />
                <span className="custom-control-label">
                  {$L('ImportUserAndNotify')} {window.__PageConfig.serviceMail !== 'true' && <span>({$L('Unavailable')})</span>}
                </span>
              </label>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._btns = c)}>
              <button className="btn btn-primary" type="button" onClick={() => this.imports()} ref={(c) => (this._btn = c)}>
                {$L('StartImport')}
              </button>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    const that = this
    let mp = false
    $createUploader(
      this._upload,
      function () {
        if (!mp) {
          $mp.start()
          mp = true
        }
      },
      function (res) {
        $mp.end()
        mp = false
        that.setState({ uploadFile: res.key })
      }
    )
  }

  imports() {
    if (rb.commercial < 1) return RbHighbar.error($L('FreeVerNotSupportted,ImportUser'))
    if (!this.state.uploadFile) return RbHighbar.create($L('PlsUploadFile'))

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
        RbHighbar.success($L('ImportUserOkTips').replace('%d', res.data.succeeded))
      } else {
        setTimeout(() => this._checkState(), 1000)
      }
    })
  }
}
