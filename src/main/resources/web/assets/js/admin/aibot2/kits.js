/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global autosize, ShowEnable */

$(document).ready(() => {
  if ($('#skillsList').length) SkillList.load()
  if ($('#kbList').length) KbList.load()
  if ($('#toolsList').length) ToolList.load()
  $('.J_addSkill').on('click', (e) => {
    $stopEvent(e, true)
    _editSkill()
  })
  $('.J_addKb').on('click', (e) => {
    $stopEvent(e, true)
    renderRbcomp(<DlgKbEdit title={$L('添加知识库')} />)
  })

  $('.J_batchUploadKb').on('click', (e) => {
    $stopEvent(e, true)

    const $file = $('<input type="file" multiple data-maxsize="52428800" data-updir="AIBOT_KBS" />').appendTo('body').hide()

    $multipleUploader($file[0], (res) => {
      const data = {
        name: res.file.name.replace(/\.[^.]+$/, ''),
        type: 'KNOWLEDGE',
        config: JSON.stringify({
          sourceType: 'FILE',
          sourceConfig: JSON.stringify({ file: res.key }),
          chunkCount: 0,
        }),
        metadata: { entity: 'AibotConfig', id: null },
      }
      $.post('/app/entity/common-save', JSON.stringify(data), (saveRes) => {
        if (saveRes.error_code === 0) {
          $.post(`/admin/integration/aibot/kb-build?id=${saveRes.data.id}`)
          $setTimeout(() => KbList.load(), 2000, 'KbList.load')
        } else {
          RbHighbar.error(saveRes.error_msg)
        }
      })
    })

    setTimeout(() => $file.click(), 20)
  })

  $('.J_rebuildKb').on('click', (e) => {
    $stopEvent(e, true)
    RbAlert.create($L('确认重建全部知识库索引？'), {
      confirmText: $L('重建'),
      confirm: function () {
        this.hide()
        $.get('/admin/integration/aibot/kb-list', (res) => {
          const data = res.data || []
          data.forEach((item) => {
            $.post(`/admin/integration/aibot/kb-build?id=${item.id}`)
          })

          $setTimeout(() => KbList.load(), 3000, 'KbList.load')
        })
      },
    })
  })
})

// ~~ Knowledge

// 列表图标（与新版管理端一致）
const KitIconBook = () => (
  <span className="kit-tile">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7">
      <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16H6.5A2.5 2.5 0 0 0 4 21z" />
      <path d="M4 5.5V21M8 7h8M8 11h6" />
    </svg>
  </span>
)

const KitIconStar = () => (
  <span className="kit-tile">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7">
      <path d="M12 3l2.2 5.4L20 10l-4.3 4.1L17 20l-5-2.9L7 20l1.3-5.9L4 10l5.8-1.6z" />
    </svg>
  </span>
)

const KitIconTool = () => (
  <span className="kit-tile">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round">
      <path d="M14.5 6.5a4 4 0 0 0-5.4 5.4L3 18l3 3 6.1-6.1a4 4 0 0 0 5.4-5.4l-2.4 2.4-2.6-2.6z" />
    </svg>
  </span>
)

class KbList extends React.Component {
  static load() {
    const $body = $('#kbList').closest('.card-body')
    let loaded = false
    $.get('/admin/integration/aibot/kb-list', (res) => {
      loaded = true
      const data = res.data || []
      const isEmpty = data.length === 0
      $body.toggleClass('card-body-loading', isEmpty)
      $body.children('.list-nodata').toggleClass('hide', !isEmpty)
      renderRbcomp(<KbList data={data} />, 'kbList')

      // 有构建中的知识库时自动刷新
      if (data.some((item) => item.chunkCount === -1)) {
        $setTimeout(() => KbList.load(), 5000, 'KbList.load')
      }
    }).always(() => {
      $body.children('.spinner').addClass('hide')
      if (!loaded) $body.removeClass('card-body-loading')
    })
  }

  render() {
    return this.props.data.map((item) => {
      // -1 构建中，0 构建失败，>0 分片数
      let chunk
      if (item.chunkCount > 0) chunk = item.chunkCount
      else if (item.chunkCount === -1) chunk = <span className="badge badge-warning">{$L('构建中')}</span>
      else chunk = <span className="badge badge-danger">{$L('构建失败')}</span>

      let source = <NoValue />
      if (item.sourceType === 'FILE') source = $L('文件')
      else if (item.sourceType === 'TEXT') source = $L('文本')

      return (
        <tr key={item.id}>
          <td>
            <div className="kit-cell">
              <KitIconBook />
              <div className="kit-text">
                <div className="kit-name">{item.name}</div>
                <div className="kit-sub text-muted" title={item.description || null}>
                  {item.description || $L('暂无描述')}
                </div>
              </div>
            </div>
          </td>
          <td className="text-muted">{source}</td>
          <td>{chunk}</td>
          <td className="actions kit-actions">
            {ShowEnable(item.isDisabled, item.id)}
            <a title={$L('修改')} className="icon" onClick={() => _editKb(item)}>
              <i className="zmdi zmdi-edit" />
            </a>
            <a title={$L('删除')} className="icon danger-hover" onClick={() => _deleteKb(item)}>
              <i className="zmdi zmdi-delete" />
            </a>
          </td>
        </tr>
      )
    })
  }
}

// 获取知识库源文件
const _getKbFile = function (item) {
  try {
    return (JSON.parse(item.sourceConfig || '{}') || {}).file || ''
  } catch (e) {
    return ''
  }
}

const _editKb = function (item) {
  renderRbcomp(<DlgKbEdit item={item} title={item ? $L('修改知识库') : $L('添加知识库')} />)
}

const _deleteKb = function (item) {
  RbAlert.create($L('确认删除此知识库？'), {
    type: 'danger',
    confirmText: $L('删除'),
    confirm: function () {
      this.disabled(true)
      $.post(`/app/entity/common-delete?id=${item.id}`, (res) => {
        if (res.error_code === 0) {
          this.hide()
          KbList.load()
        } else {
          RbHighbar.error(res.error_msg)
          this.disabled()
        }
      })
    },
  })
}

class DlgKbEdit extends RbModalHandler {
  constructor(props) {
    super(props)
    this.state = { ...props, fileKey: null, fileName: null }
  }

  render() {
    const item = this.props.item || {}
    const isFile = item.sourceType === 'FILE'
    return (
      <RbModal ref={(c) => (this._dlg = c)} title={this.props.title} disposeOnHide>
        <div>
          <form>
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('知识库名称')}</label>
              <div className="col-sm-7">
                <input className="form-control form-control-sm" type="text" maxLength="200" ref={(c) => (this._$name = c)} defaultValue={item.name || ''} autoFocus />
              </div>
            </div>
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('描述')}</label>
              <div className="col-sm-7">
                <input className="form-control form-control-sm" type="text" maxLength="500" ref={(c) => (this._$desc = c)} defaultValue={item.description || ''} placeholder={$L('选填')} />
              </div>
            </div>
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('内容')}</label>
              <div className="col-sm-7">
                <div className="mb-1 file-select">
                  <input type="file" className="inputfile" id="DlgKbEdit__file" data-maxsize="52428800" data-updir="AIBOT_KBS" ref={(c) => (this._$file = c)} />
                  <label htmlFor="DlgKbEdit__file" className="btn-secondary">
                    <span className="zmdi zmdi-upload" />
                    <span className="ml-1">{$L('上传文件')}</span>
                  </label>
                  {this.state.fileName ? <b className="text-underline ml-2">{this.state.fileName}</b> : null}
                  {!this.state.fileName && isFile && _getKbFile(item) && (
                    <a className="ml-2 text-bold text-dark text-underline" title={$L('下载文件')} href={`${rb.baseUrl}/filex/download/${_getKbFile(item)}`} target="_blank">
                      {$fileCutName(_getKbFile(item))}
                    </a>
                  )}
                </div>
                <textarea
                  className="form-control form-control-sm"
                  ref={(c) => (this._$text = c)}
                  defaultValue={!isFile ? this._getSourceConfig('text', item.sourceConfig) : ''}
                  placeholder={$L('输入内容，或上传文件自动解析')}
                />
              </div>
            </div>
            <div className="form-group row footer">
              <div className="col-sm-7 offset-sm-3" ref={(c) => (this._$btn = c)}>
                <button className="btn btn-primary" type="button" onClick={() => this._onSave()}>
                  {$L('确定')}
                </button>
                <button className="btn btn-link" type="button" onClick={() => this.hide()}>
                  {$L('取消')}
                </button>
              </div>
            </div>
          </form>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    super.componentDidMount && super.componentDidMount()

    $multipleUploader(this._$file, (res) => {
      this.setState({ fileKey: res.key, fileName: res.file.name })
      if (!$(this._$name).val()) $(this._$name).val(res.file.name.replace(/\.[^.]+$/, ''))
    })

    setTimeout(() => autosize(this._$text), 100)
  }

  _getSourceConfig(key, sourceConfig) {
    if (!sourceConfig) return ''
    try {
      const cfg = JSON.parse(sourceConfig)
      return cfg[key] || ''
    } catch (e) {
      return ''
    }
  }

  _onSave() {
    const name = $(this._$name).val()
    if (!name) {
      RbHighbar.create($L('请输入名称'))
      return
    }

    const item = this.props.item || {}
    const text = $(this._$text).val()
    const fileKey = this.state.fileKey

    let sourceType,
      sourceConfig,
      needBuild = false
    if (fileKey) {
      sourceType = 'FILE'
      sourceConfig = JSON.stringify({ file: fileKey })
      needBuild = true
    } else if (text) {
      sourceType = 'TEXT'
      sourceConfig = JSON.stringify({ text: text })
      // 仅当文本内容变化时才需要重新构建
      const oldText = item.id ? this._getSourceConfig('text', item.sourceConfig) : null
      needBuild = text !== oldText
    } else if (item.id && item.sourceConfig) {
      // 仅修改名称/描述，沿用原内容
      sourceType = item.sourceType
      sourceConfig = item.sourceConfig
      needBuild = false
    } else {
      RbHighbar.create($L('请输入内容或上传文件'))
      return
    }

    const itemId = item.id || null
    const config = {
      description: $(this._$desc).val(),
      sourceType: sourceType,
      sourceConfig: sourceConfig,
      chunkCount: item.chunkCount || 0,
    }
    const data = {
      name: name,
      type: 'KNOWLEDGE',
      config: JSON.stringify(config),
      metadata: {
        entity: 'AibotConfig',
        id: itemId,
      },
    }

    const $btn = $(this._$btn).find('.btn').button('loading')
    $.post('/app/entity/common-save', JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        const newId = itemId || res.data.id
        const next = () => {
          $btn.button('reset')
          this.hide()
          KbList.load()
        }

        // 内容未变化无需重新构建
        if (needBuild) {
          $.post(`/admin/integration/aibot/kb-build?id=${newId}`, next)
        } else {
          next()
        }
      } else {
        RbHighbar.error(res.error_msg)
        $btn.button('reset')
      }
    })
  }
}

// ~~ Skills

class SkillList extends React.Component {
  static load() {
    const $body = $('#skillsList').closest('.card-body')
    let loaded = false
    $.get('/admin/integration/aibot/skill-list', (res) => {
      loaded = true
      const data = res.data || []
      const isEmpty = data.length === 0
      $body.toggleClass('card-body-loading', isEmpty)
      $body.children('.list-nodata').toggleClass('hide', !isEmpty)
      renderRbcomp(<SkillList data={data} />, 'skillsList')
    }).always(() => {
      $body.children('.spinner').addClass('hide')
      if (!loaded) $body.removeClass('card-body-loading')
    })
  }

  render() {
    return this.props.data.map((item) => {
      const cfg = item.config || {}
      return (
        <tr key={item.id}>
          <td>
            <div className="kit-cell">
              <KitIconStar />
              <div className="kit-text">
                <div className="kit-name">{item.name}</div>
                <div className="kit-sub text-muted" title={cfg.description || null}>
                  {cfg.description || $L('暂无描述')}
                </div>
              </div>
            </div>
          </td>
          <td className="text-muted">
            {cfg.prompt ? (
              <span className="kit-prompt" title={cfg.prompt}>
                {cfg.prompt}
              </span>
            ) : (
              <NoValue />
            )}
          </td>
          <td className="actions kit-actions">
            {ShowEnable(item.isDisabled, item.id)}
            <a title={$L('修改')} className="icon" onClick={() => _editSkill(item)}>
              <i className="zmdi zmdi-edit" />
            </a>
            <a title={$L('删除')} className="icon danger-hover" onClick={() => _deleteSkill(item)}>
              <i className="zmdi zmdi-delete" />
            </a>
          </td>
        </tr>
      )
    })
  }
}

const _editSkill = function (item) {
  renderRbcomp(<DlgSkillEdit item={item} title={item ? $L('修改技能') : $L('添加技能')} />)
}

const _deleteSkill = function (item) {
  RbAlert.create($L('确认删除此技能？'), {
    type: 'danger',
    confirmText: $L('删除'),
    confirm: function () {
      this.disabled(true)
      $.post(`/app/entity/common-delete?id=${item.id}`, (res) => {
        if (res.error_code === 0) {
          this.hide()
          SkillList.load()
        } else {
          RbHighbar.error(res.error_msg)
          this.disabled()
        }
      })
    },
  })
}

class DlgSkillEdit extends RbModalHandler {
  constructor(props) {
    super(props)
    this.state = { ...props }
    this._$refs = {}
  }

  render() {
    const conf = (this.props.item || {}).config || {}

    // 修改
    if (this.props.item) {
      return (
        <RbModal ref={(c) => (this._dlg = c)} title={this.props.title} disposeOnHide>
          {this._renderFormContent(conf)}
        </RbModal>
      )
    }

    const skills = this.state.skills || []
    return (
      <RbModal
        ref={(c) => (this._dlg = c)}
        title={
          <RF>
            {this.props.title}
            <AiCreateButton presetContent={$L('帮我做一个技能。先向我确认技能用途和使用场景，确认后再创建。')} />
          </RF>
        }
        disposeOnHide>
        <div className="tab-container" style={{ marginTop: -10 }} ref={(c) => (this._$container = c)}>
          <ul className="nav nav-tabs">
            <li className="nav-item">
              <a className="nav-link active" href="#SKILL_MANUAL" data-toggle="tab">
                {$L('手动')}
              </a>
            </li>
            <li className="nav-item">
              <a className="nav-link" href="#SKILL_IMPORT" data-toggle="tab">
                {$L('从 RB 仓库导入')}
              </a>
            </li>
          </ul>
          <div className="tab-content m-0 pb-0">
            <div className="tab-pane active" id="SKILL_MANUAL">
              {this._renderFormContent(conf)}
            </div>
            <div className="tab-pane" id="SKILL_IMPORT">
              <div className="init-models" ref={(c) => (this._$skills = c)}>
                <fieldset>
                  <legend>
                    <div className="row">
                      <div className="col">
                        <strong>{$L('共 %d 个可用技能', skills.length)}</strong>
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
                    {skills.map((item, idx) => {
                      return (
                        <div key={idx}>
                          <label className="custom-control custom-checkbox m-0" title={item.description} ref={(c) => (this._$refs[item.name] = c)}>
                            <input className="custom-control-input" type="checkbox" value={item.name} />
                            <span className="custom-control-label text-bold">{item.name}</span>
                            <p>{item.description}</p>
                          </label>
                        </div>
                      )
                    })}
                  </form>
                </fieldset>
              </div>
              <div className="dialog-footer">
                <div className="float-right">
                  <button type="button" className="btn btn-primary" ref={(c) => (this._$importBtn = c)} onClick={() => this._onImport()}>
                    {$L('开始导入')}
                  </button>
                </div>
                <div className="float-right">
                  <p className="protips mt-2 pr-2">{$L('可在导入后根据自身需求做适当调整/修改')}</p>
                </div>
                <div className="clearfix" />
              </div>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  // 表单内容（新建/修改共用）
  _renderFormContent(conf) {
    return (
      <div className="form">
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('技能名称')}</label>
          <div className="col-sm-7">
            <input className="form-control form-control-sm" type="text" maxLength="40" ref={(c) => (this._$name = c)} defaultValue={conf.name || ''} autoFocus />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('描述')}</label>
          <div className="col-sm-7">
            <input className="form-control form-control-sm" type="text" maxLength="100" ref={(c) => (this._$desc = c)} defaultValue={conf.description || ''} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('提示词')}</label>
          <div className="col-sm-7">
            <textarea className="form-control form-control-sm" ref={(c) => (this._$prompt = c)} defaultValue={conf.prompt || ''} />
          </div>
        </div>
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3" ref={(c) => (this._$btn = c)}>
            <button className="btn btn-primary" type="button" onClick={() => this._onSave()}>
              {$L('确定')}
            </button>
            <button className="btn btn-link" type="button" onClick={() => this.hide()}>
              {$L('取消')}
            </button>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount && super.componentDidMount()

    setTimeout(() => autosize(this._$prompt), 100)

    if (!this.props.item) this._loadSkills()
  }

  _loadSkills() {
    $.get('/admin/rbstore/load-index?type=skills', (res) => {
      if (res.error_code === 0) this.setState({ skills: res.data })
      else RbHighbar.error(res.error_msg)
    })
  }

  _handleSelectAll(e) {
    const chk = $(e.currentTarget).prop('checked')
    $(this._$skills)
      .find('form input')
      .each(function () {
        $(this).prop('checked', chk)
      })
  }

  _onImport() {
    const selected = []
    $(this._$skills)
      .find('form input')
      .each(function () {
        const $chk = $(this)
        if ($chk.prop('checked')) selected.push($chk.val())
      })

    if (selected.length === 0) {
      RbHighbar.create($L('请选择要导入的技能'))
      return
    }

    const $btn = $(this._$importBtn).button('loading')
    $.post(`/admin/integration/aibot/import-skills?names=${encodeURIComponent(selected.join(','))}`, (res) => {
      if (res.error_code === 0) {
        this.hide()
        RbHighbar.success($L('成功导入 %d 个技能', selected.length))
        SkillList.load()
      } else {
        RbHighbar.error(res.error_msg)
        $btn.button('reset')
      }
    })
  }

  _onSave() {
    const item = this.props.item || {}
    const name = $(this._$name).val()
    const desc = $(this._$desc).val()
    const prompt = $(this._$prompt).val()
    if (!name || !desc || !prompt) {
      RbHighbar.create($L('请输入技能名称、描述和提示词'))
      return
    }

    const data = {
      name: name,
      type: 'SKILL',
      config: JSON.stringify({
        name: name,
        description: desc,
        prompt: prompt,
      }),
      metadata: {
        entity: 'AibotConfig',
        id: item.id || null,
      },
    }

    const $btn = $(this._$btn).find('.btn').button('loading')
    $.post('/app/entity/common-save', JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        this.hide()
        SkillList.load()
      } else {
        RbHighbar.error(res.error_msg)
        $btn.button('reset')
      }
    })
  }
}

// ~~ Tools

let _toolsData = []
class ToolList extends React.Component {
  static load() {
    const $body = $('#toolsList').closest('.card-body')
    let loaded = false
    $.get('/admin/integration/aibot/tool-list', (res) => {
      loaded = true
      _toolsData = res.data || []
      const isEmpty = _toolsData.length === 0
      $body.toggleClass('card-body-loading', isEmpty)
      $body.children('.list-nodata').toggleClass('hide', !isEmpty)
      renderRbcomp(<ToolList data={_toolsData} />, 'toolsList')
    }).always(() => {
      $body.children('.spinner').addClass('hide')
      if (!loaded) $body.removeClass('card-body-loading')
    })
  }

  render() {
    return this.props.data.map((item) => {
      let desc = item.userDescription || item.description || ''
      const sysTool = desc.includes('[系统工具]')
      const commercialTool = desc.includes('[商业工具]')
      desc = desc.replace(/\s*\[(系统工具|商业工具)]/g, '').trim()

      const _saveFn = (checked, e) => {
        if (rb.commercial < 1) {
          RbAlertFree43.create()
          return
        }

        if (checked) {
          this._saveToolsDisabled(item.name, checked)
          return
        }

        const input = e ? e.target : null
        const doSave = () => this._saveToolsDisabled(item.name, false)
        let confirmed = false
        RbAlert.create($L('关闭后 Rex AI 将失去此能力。确定关闭吗？'), {
          onConfirm: function () {
            confirmed = true
            this.hide()
            doSave()
          },
          onHide: () => {
            if (!confirmed && input) input.checked = true
          },
        })
      }

      return (
        <tr key={item.name}>
          <td>
            <div className="kit-cell">
              <KitIconTool />
              <div className="kit-text">
                <div className="kit-name">{item.name}</div>
                <div className="kit-sub text-muted" title={desc || null}>
                  {desc || $L('暂无说明')}
                </div>
              </div>
            </div>
          </td>
          <td>
            {sysTool && <span className="badge badge-secondary">{$L('系统')}</span>}
            {commercialTool && <span className="badge badge-info ml-1">{$L('商业')}</span>}
            {!sysTool && !commercialTool && <NoValue />}
          </td>
          <td>{sysTool ? <span className="badge badge-secondary">{$L('内置')}</span> : ShowEnable(item.disabled, item.name, _saveFn)}</td>
        </tr>
      )
    })
  }

  _saveToolsDisabled(name, checked) {
    const item = _toolsData.find((x) => x.name === name)
    if (item) item.disabled = !checked

    const disabled = _toolsData.filter((x) => x.disabled).map((x) => x.name)
    $.post('/admin/integration/aibot', JSON.stringify({ AibotToolsDisabled: disabled.join(',') }), (res) => {
      if (res.error_code !== 0) RbHighbar.error(res.error_msg)
    })
  }
}
