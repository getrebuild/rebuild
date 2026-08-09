/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global autosize */

// eslint-disable-next-line no-undef, react/display-name
useEditComp = function (name) {
  if ('AibotBasePrompt' === name) {
    return <textarea className="form-control form-control-sm row2x" maxLength="2000" />
  } else if ('AibotSuggestQuestions' === name) {
    return <textarea className="form-control form-control-sm row2x" maxLength="2000" />
  } else if ('AibotBaseDefModel' === name) {
    setTimeout(() => {
      let models = 'deepseek-v4-flash qwen3.6-flash hy3-preview gpt-5 gemini-2.5-pro'.split(' ')
      $autoComplete($('input[name="AibotBaseDefModel"]'), null, {
        options: models,
        onSelect: (v) => {
          // eslint-disable-next-line no-undef
          changeValue({ target: { value: v, name: 'AibotBaseDefModel' } })
        },
      })
    }, 500)
  }
}

$(document).ready(() => {
  $.get('./aibot/stats', (res) => {
    let $el = $('.J_stats-aibot')
    $el.find('strong').text(res.data.aibotCount || 0)
    _renderStats(res.data.aibot, $el)
  })

  _renderMcpConfig()

  _loadSkills()
  _loadKnowledge()
  _loadTools()
  $('.J_addSkill').on('click', (e) => {
    $stopEvent(e, true)
    _editSkill()
  })
  $('.J_addKb').on('click', (e) => {
    $stopEvent(e, true)
    renderRbcomp(<DlgKbEdit title={$L('添加知识库')} />)
  })
  $('.J_importSkill').on('click', (e) => {
    $stopEvent(e, true)
    renderRbcomp(<DlgSkillImport title={$L('导入技能')} />)
  })
})

// ~~ Knowledge

const _loadKnowledge = function () {
  $.get('./aibot/kb-list', (res) => {
    const data = res.data || []
    const $tbody = $('#kbList').empty()
    $('.J_kbEmpty').toggle(data.length === 0)

    data.forEach((item) => {
      // -1 构建中，0 构建失败，>0 分片数
      let chunkBadge
      if (item.chunkCount > 0) chunkBadge = `<span class="badge badge-light ml-1">${item.chunkCount}</span>`
      else if (item.chunkCount === -1) chunkBadge = `<span class="badge badge-warning ml-1">${$L('构建中')}</span>`
      else chunkBadge = `<span class="badge badge-danger ml-1">${$L('构建失败')}</span>`

      const $tr = $(
        `<tr>
          <td>${item.name} ${chunkBadge}</td>
          <td>${item.description || $L('无')}</td>
          <td class="actions">
            <a title="${$L('修改')}" class="icon"><i class="zmdi zmdi-edit"></i></a>
            <a title="${$L('删除')}" class="icon danger-hover"><i class="zmdi zmdi-delete"></i></a>
          </td>
        </tr>`,
      ).appendTo($tbody)

      $tr.find('a:eq(0)').on('click', () => _editKb(item))
      $tr.find('a:eq(1)').on('click', () => _deleteKb(item))
    })
  })
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
          _loadKnowledge()
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
                <input className="form-control form-control-sm" type="text" maxLength="500" ref={(c) => (this._$desc = c)} defaultValue={item.description || ''} />
              </div>
            </div>
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right">{$L('内容')}</label>
              <div className="col-sm-7">
                <div className="mb-1 file-select">
                  <input type="file" className="inputfile" id="DlgKbEdit__file" data-local="temp" ref={(c) => (this._$file = c)} />
                  <label htmlFor="DlgKbEdit__file" className="btn-secondary">
                    <span className="zmdi zmdi-upload" />
                    <span className="ml-1">{$L('上传文件')}</span>
                  </label>
                  {this.state.fileName ? <b className="text-underline ml-2">{this.state.fileName}</b> : null}
                </div>
                <textarea className="form-control form-control-sm" ref={(c) => (this._$text = c)} defaultValue={!isFile ? this._getSourceConfig('text', item.sourceConfig) : ''} />
                <p className="form-text">{WrapHtml($L('输入内容，或上传文件自动解析'))}</p>
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
    setTimeout(() => autosize(this._$text), 400)
    $multipleUploader(this._$file, (res) => {
      this.setState({ fileKey: res.key, fileName: res.file.name })
      // 名称为空时用文件名（去后缀）回填
      if (!$(this._$name).val()) $(this._$name).val(res.file.name.replace(/\.[^.]+$/, ''))
    })
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

    let sourceType, sourceConfig
    if (fileKey) {
      sourceType = 'FILE'
      sourceConfig = JSON.stringify({ file: fileKey })
    } else if (text) {
      sourceType = 'TEXT'
      sourceConfig = JSON.stringify({ text: text })
    } else if (item.id && item.sourceConfig) {
      // 仅修改名称/描述，沿用原内容
      sourceType = item.sourceType
      sourceConfig = item.sourceConfig
    } else {
      RbHighbar.create($L('请输入内容或上传文件'))
      return
    }

    const itemId = item.id || null
    const data = {
      name: name,
      description: $(this._$desc).val(),
      sourceType: sourceType,
      sourceConfig: sourceConfig,
      metadata: {
        entity: 'AibotKnowledge',
        id: itemId,
      },
    }

    const $btn = $(this._$btn).find('.btn').button('loading')
    $.post('/app/entity/common-save', JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        const newId = itemId || res.data.id
        $.post(`./aibot/kb-build?id=${newId}`, () => {
          $btn.button('reset')
          this.hide()
          _loadKnowledge()
        })
      } else {
        RbHighbar.error(res.error_msg)
        $btn.button('reset')
      }
    })
  }
}

// ~~ Skills

const _loadSkills = function () {
  $.get('/admin/commons-config/list?type=AIBOT_SKILL', (res) => {
    const data = res.data || []
    const $tbody = $('#skillsList').empty()
    $('.J_skillsEmpty').toggle(data.length === 0)

    data.forEach((item) => {
      const cfg = item.config || {}
      const $tr = $(
        `<tr>
          <td>${item.name}</td>
          <td>${cfg.description || $L('无')}</td>
          <td class="actions">
            <a title="${$L('修改')}" class="icon"><i class="zmdi zmdi-edit"></i></a>
            <a title="${$L('删除')}" class="icon danger-hover"><i class="zmdi zmdi-delete"></i></a>
          </td>
        </tr>`,
      ).appendTo($tbody)

      $tr.find('a:eq(0)').on('click', () => _editSkill(item))
      $tr.find('a:eq(1)').on('click', () => _deleteSkill(item))
    })
  })
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
          _loadSkills()
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
  }

  render() {
    const conf = (this.props.item || {}).config || {}
    return (
      <RbModal ref={(c) => (this._dlg = c)} title={this.props.title} disposeOnHide>
        <div>
          <form>
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
          </form>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    super.componentDidMount && super.componentDidMount()
    setTimeout(() => autosize(this._$prompt), 400)
  }

  _onSave() {
    const item = this.props.item || {}
    const name = $(this._$name).val()
    const prompt = $(this._$prompt).val()
    if (!name || !prompt) {
      RbHighbar.create($L('请输入技能名称和提示词'))
      return
    }

    const data = {
      name: name,
      type: 'AIBOT_SKILL',
      belongEntity: 'N',
      shareTo: 'ALL',
      config: {
        name: name,
        description: $(this._$desc).val(),
        prompt: prompt,
      },
      metadata: {
        entity: 'CommonsConfig',
        id: item.id || null,
      },
    }

    const $btn = $(this._$btn).find('.btn').button('loading')
    $.post('/app/entity/common-save', JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        this.hide()
        _loadSkills()
      } else {
        RbHighbar.error(res.error_msg)
        $btn.button('reset')
      }
    })
  }
}

class DlgSkillImport extends RbModalHandler {
  constructor(props) {
    super(props)
    this.state = { ...props, data: null }
    this._$refs = {}
  }

  render() {
    if (this.state.hasError) {
      return (
        <RbModal ref={(c) => (this._dlg = c)} title={this.props.title} disposeOnHide>
          <div className="mb-7">
            <RbAlertBox message={this.state.hasError} />
          </div>
        </RbModal>
      )
    }

    const skills = this.state.skills || []

    return (
      <RbModal ref={(c) => (this._dlg = c)} title={this.props.title} disposeOnHide>
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
            <div className="clearfix" />
          </fieldset>

          <div className="dialog-footer">
            <div className="float-right">
              <button type="button" className="btn btn-primary" onClick={() => this._onImport()}>
                {$L('开始导入')}
              </button>
            </div>
            <div className="float-right">
              <p className="protips mt-2 pr-2">{$L('可在导入后根据自身需求做适当调整/修改')}</p>
            </div>
            <div className="clearfix" />
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    super.componentDidMount && super.componentDidMount()
    this._loadSkills()
  }

  _loadSkills() {
    $.get('/admin/rbstore/load-index?type=skills', (res) => {
      let hasError = res.error_code > 0 ? res.error_msg || $L('暂无可用技能') : null
      this.setState({ skills: res.data || [], hasError })
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

    const $btn = $(this._$btn).find('.btn').button('loading')
    $.post(`/admin/rbstore/import-skills?names=${encodeURIComponent(selected.join(','))}`, (res) => {
      if (res.error_code === 0) {
        this.hide()
        RbHighbar.success($L('成功导入 %d 个技能', selected.length))
        _loadSkills()
      } else {
        RbHighbar.error(res.error_msg)
        $btn.button('reset')
      }
    })
  }
}

// ~~ Tools

let _toolsData = []

const _loadTools = function () {
  $.get('./aibot/tools', (res) => {
    _toolsData = res.data || []
    const $tbody = $('#toolsList').empty()

    _toolsData.forEach((item) => {
      if (['SuggestCustom', 'SuggestQuestions'].includes(item.name)) return

      const htmlid = `tool-enable-${item.name}`
      $(
        `<tr>
          <td>${item.name}</td>
          <td>${item.description || $L('无')}</td>
          <td>
            <div class="switch-button switch-button-xs switch-button-success">
              <input type="checkbox" id="${htmlid}" ${item.disabled ? '' : 'checked'} />
              <span><label for="${htmlid}"></label></span>
            </div>
          </td>
        </tr>`,
      ).appendTo($tbody)

      $(`#${htmlid}`).on('change', function () {
        _saveToolsDisabled()
      })
    })
  })
}

const _saveToolsDisabled = function () {
  const disabled = []
  _toolsData.forEach((item) => {
    const $input = $(`#tool-enable-${item.name}`)
    if ($input[0] && !$input[0].checked) disabled.push(item.name)
  })

  $.post(location.href, JSON.stringify({ AibotToolsDisabled: disabled.join(',') }), (res) => {
    if (res.error_code !== 0) RbHighbar.error(res.error_msg)
  })
}

// eslint-disable-next-line no-undef
postBefore = function (data) {
  const $ds = $('td[data-id="AibotDSSecret"]')
  if (!data['AibotDSSecret'] && !$ds.data('value')) {
    RbHighbar.create($L('%s不能为空', $ds.prev().text()))
    return false
  }

  return data
}

// ~~

const _renderStats = function (data, $el) {
  const xAxis = []
  const series = []
  data.forEach((item) => {
    xAxis.push(item[0])
    series.push(item[1])
  })

  const option = {
    grid: { left: 0, right: 0, top: 4, bottom: 4 },
    animation: true,
    tooltip: {
      trigger: 'axis',
      formatter: '{b} : <b>{c}</b>',
      textStyle: {
        fontSize: 12,
        lineHeight: 1.3,
        color: '#333',
      },
      axisPointer: {
        lineStyle: { color: '#ddd' },
      },
      backgroundColor: '#fff',
      extraCssText: 'border-radius:0;box-shadow:0 0 6px 0 rgba(0, 0, 0, .1), 0 8px 10px 0 rgba(170, 182, 206, .2);',
      confine: true,
      position: 'top',
    },
    textStyle: {
      fontFamily: '"Hiragina Sans GB", San Francisco, "Helvetica Neue", Helvetica, Arial, PingFangSC-Light, "WenQuanYi Micro Hei", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif',
    },
    xAxis: {
      show: false,
      type: 'category',
      data: xAxis,
    },
    yAxis: {
      show: false,
      type: 'value',
      splitLine: { show: false },
      cursor: 'default',
    },
    series: [
      {
        data: series,
        areaStyle: { opacity: 0.2 },
        itemStyle: {
          normal: {
            color: '#4285f4',
            lineStyle: { color: '#4285f4' },
          },
        },
        type: 'line',
        smooth: true,
        connectNulls: true,
      },
    ],
  }

  const c = echarts.init($el.find('span')[0])
  c.setOption(option)
}

const _renderMcpConfig = function () {
  const $mcp = $('.J_mcpConfig')
  const homeUrl = $mcp.data('home-url') || ''
  const code = {
    mcpServers: {
      rebuild: {
        url: homeUrl + 'gw/mcp/sse',
        headers: {
          Authorization: 'Bearer',
        },
        disabled: false,
      },
    },
  }
  renderRbcomp(<CodeViewport code={code} type="json" />, $mcp[0], function () {
    const $pre = $mcp.find('pre')
    const $a = $('<a>', { href: '../../settings/user#secure', target: '_blank', text: `<${$L('你的个人秘钥')}>` })
    $pre.html($pre.html().replace('Bearer', 'Bearer ' + $a[0].outerHTML))
  })
}
