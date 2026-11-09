/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// 初始导入实体

// eslint-disable-next-line no-unused-vars
class InitModels extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, installMode: props.rbsystemPackage ? 'package' : 'modules' }
    this._$refs = {}
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="mb-7">
          <RbAlertBox message={this.state.hasError} />
        </div>
      )
    } else if (!this.state.data) {
      return null
    }

    const types = this.state.data.types || {}
    const schemas = this.state.data.schemas || []
    const mode = this.state.installMode

    return (
      <div className="init-models">
        <ul className="nav nav-tabs nav-tabs-segment mx-auto">
          <li className="nav-item">
            <a className={`nav-link${mode === 'modules' ? ' active' : ''}`} role="button" onClick={() => this.setState({ installMode: 'modules' })}>
              {$L('按业务模块安装')}
            </a>
          </li>
          <li className="nav-item">
            <a className={`nav-link${mode === 'package' ? ' active' : ''}`} role="button" onClick={() => this.setState({ installMode: 'package' })}>
              {$L('从应用商店安装')}
            </a>
          </li>
        </ul>
        <div className="tab-content p-0">
          <div className={`tab-pane${mode === 'modules' ? ' active' : ''}`}>{this._renderModels(types, schemas)}</div>
          <div className={`tab-pane${mode === 'package' ? ' active' : ''}`}>{this._renderPackage()}</div>
        </div>
      </div>
    )
  }

  _renderModels(types, schemas) {
    const groups = Object.keys(types)
    if (groups.length === 0) {
      return <RbAlertBox message={$L('暂无可用的业务实体，你仍可以继续安装，稍后在后台创建')} />
    }

    const _find = (s) => {
      return schemas.find((x) => s === x.key)
    }

    return (
      <div>
        {groups.map((t) => {
          return (
            <fieldset key={t}>
              <legend>
                <div className="row">
                  <div className="col">
                    <strong>{t}</strong>
                  </div>
                  <div className="col text-right">
                    <label className="custom-control custom-checkbox custom-control-inline custom-control-sm" title={$L('全选')}>
                      <input className="custom-control-input" type="checkbox" onClick={(e) => this._handleSelectAll(e)} />
                      <span className="custom-control-label" />
                    </label>
                  </div>
                </div>
              </legend>
              <form>
                {types[t].map((s) => {
                  const item = _find(s)
                  if (!item) return null

                  return (
                    <div key={item.key}>
                      <label className="custom-control custom-checkbox m-0" title={item.desc} ref={(c) => (this._$refs[item.key] = c)}>
                        <input className="custom-control-input" type="checkbox" value={item.key} data-refs={item.refs} onClick={(e) => this._handleSelect(e)} disabled={item.exists} />
                        <span className="custom-control-label text-bold">
                          {item.name}
                          {item.exists && <span className="text-muted font-weight-normal ml-1">{$L('已安装')}</span>}
                          {item.rbv && <i className="zmdi zmdi-info-outline text-warning zicon ml-1 J_rbv-tip" title={$L('部分功能可能需要商业版才能正常运行')} />}
                        </span>
                        <p>{item.desc}</p>
                      </label>
                    </div>
                  )
                })}
              </form>
              <div className="clearfix" />
            </fieldset>
          )
        })}
      </div>
    )
  }

  _renderPackage() {
    const packageLabel = $L('选择从 REBUILD [应用商店](https://getrebuild.com/store/) 下载的应用资源包，整套业务系统一次装好，不用从零搭建。')
    return (
      <form>
        <div className="w-100 p-4 mt-4">
          <label className="text-dark">{WrapHtml(packageLabel)}</label>
          <div className="file-select">
            <input
              type="file"
              className="inputfile"
              id="upload-rbsystem-package"
              accept=".rbspkg"
              data-local="temp"
              ref={(c) => (this._$upload = c)}
              disabled={!!(this.state.uploading || rb.currentUser)}
            />
            <label htmlFor="upload-rbsystem-package" className="btn-secondary" disabled={!!(this.state.uploading || rb.currentUser)}>
              <i className="zmdi zmdi-upload" />
              {this.state.uploading ? <span className="text-muted">{$L('上传中...')}</span> : <span>{$L('选择文件')}</span>}
            </label>
            {this.state.rbsystemPackage && (
              <span className="ml-2 text-bold">
                <i className="mdi-package-check mdi mr-1" />
                {$fileCutName(this.state.rbsystemPackage)}
                <a className="ml-2 text-primary font-weight-normal" role="button" onClick={this._removePackage}>
                  {$L('移除')}
                </a>
              </span>
            )}
          </div>
          {rb.currentUser && <RbAlertBox message={$L('应用资源包仅支持全新安装时使用')} className="mb-0" />}
        </div>{' '}
      </form>
    )
  }

  componentDidMount() {
    $.get(rb.isAdminVerified ? '/admin/rbstore/load-metaschemas' : '/setup/init-models', (res) => {
      const s = {}
      if (res.error_code > 0) s.hasError = $L('暂无可用的业务实体，你仍可以继续安装，稍后在后台创建')
      else s.data = res.data
      this.setState(s, () => {
        $('i.J_rbv-tip').tooltip()
        typeof this.props.onLoad === 'function' && this.props.onLoad()

        if (this._$upload) {
          $createUploader(
            this._$upload,
            () => this.setState({ uploading: true }),
            (result) => this.setState({ rbsystemPackage: result.key, uploading: false }),
            () => this.setState({ uploading: false }),
          )
        }
      })
    })
  }

  _checkRefs($chk) {
    const refs = ($chk.data('refs') || '').split(',')
    refs.forEach((s) => {
      if (!s) return
      const $label = $(this._$refs[s])
      if (!$label.length) return

      const $ref = $label.find('input')
      if ($ref.attr('disabled') || $ref.prop('checked')) return

      $ref.prop('checked', true)
      this._syncGroupAll($ref)
    })
  }

  _handleSelect(e) {
    const $el = $(e.currentTarget)
    if ($el.prop('checked')) this._checkRefs($el)
    this._syncGroupAll($el)
  }

  _handleSelectAll(e) {
    const chk = $(e.currentTarget).prop('checked')
    const that = this
    $(e.currentTarget)
      .parents('legend')
      .next()
      .find('input')
      .each(function () {
        const $chk = $(this)
        if ($chk.attr('disabled')) return
        $chk.prop('checked', chk)
        if (chk) that._checkRefs($chk)
        that._syncGroupAll($chk)
      })
  }

  _syncGroupAll($chk) {
    const $group = $chk.closest('fieldset')
    if (!$group.length) return

    const $all = $group.find('legend input')
    if (!$all.length) return

    const $items = $group.find('form input')
    const total = $items.filter(':not(:disabled)').length
    const checked = $items.filter(':checked').length
    $all.prop('checked', total > 0 && checked === total)
    $all.prop('indeterminate', checked > 0 && checked < total)
  }

  _removePackage = () => {
    if (this._$upload) this._$upload.value = ''
    this.setState({ rbsystemPackage: null })
  }

  getMode() {
    return this.state.installMode
  }

  getSelected() {
    const ss = []
    for (let k in this._$refs) {
      const $chk = $(this._$refs[k]).find('input')
      if ($chk.prop('checked')) ss.push($chk.val())
    }
    return ss
  }

  getPackage() {
    return this.state.rbsystemPackage || null
  }
}
