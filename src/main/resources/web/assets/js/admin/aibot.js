/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// eslint-disable-next-line no-undef, react/display-name
useEditComp = function (name) {
  if ('AibotBasePrompt' === name) {
    return <textarea className="form-control form-control-sm row2x" maxLength="2048" />
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

  _loadSkills()
  _loadTools()
  $('.J_addSkill').on('click', (e) => {
    $stopEvent(e, true)
    _editSkill()
  })
  $('.J_importSkill').on('click', (e) => {
    $stopEvent(e, true)
    renderRbcomp(<DlgSkillImport title={$L('导入技能')} />)
  })
})

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
                <textarea className="form-control prompt" maxLength="6000" rows="3" ref={(c) => (this._$prompt = c)} defaultValue={conf.prompt || ''} />
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

    // eslint-disable-next-line no-undef
    autosize(this._$prompt)
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

// ~~ Tools

let _toolsData = []

const _loadTools = function () {
  $.get('./aibot/tools', (res) => {
    _toolsData = res.data || []
    const $tbody = $('#toolsList').empty()

    _toolsData.forEach((item) => {
      if (['SuggestCustom', 'SearchHelp'].includes(item.name)) return

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

// ~~ Import Skills

class DlgSkillImport extends RbModalHandler {
  constructor(props) {
    super(props)
    this.state = { ...props, data: null }
    this._$refs = {}
  }

  render() {
    const skills = this.state.skills || []

    return (
      <RbModal ref={(c) => (this._dlg = c)} title={this.props.title} disposeOnHide>
        <div>
          <div className="form-group row mb-2">
            <div className="col">
              <label className="custom-control custom-checkbox custom-control-inline custom-control-sm" title={$L('全选')}>
                <input className="custom-control-input" type="checkbox" onClick={(e) => this._handleSelectAll(e)} />
                <span className="custom-control-label" />
                <span className="text-bold fs-14">{$L('全选')}</span>
              </label>
              {skills.length > 0 && <span className="text-muted fs-12 ml-2">{$L('共 %d 个可用技能', skills.length)}</span>}
            </div>
          </div>

          {skills.length === 0 ? (
            <div className="text-muted text-center mt-4 mb-4">{$L('暂无可导入的技能')}</div>
          ) : (
            <div className="skill-import-list">
              {skills.map((item, idx) => (
                <div key={idx}>
                  <label className="custom-control custom-checkbox m-0" title={item.description} ref={(c) => (this._$refs[item.name] = c)}>
                    <input className="custom-control-input" type="checkbox" value={item.name} />
                    <span className="custom-control-label text-bold">{item.name}</span>
                    <p>{item.description}</p>
                  </label>
                </div>
              ))}
            </div>
          )}

          <div className="form-group row footer mt-3">
            <div className="col-sm-7 offset-sm-3" ref={(c) => (this._$btn = c)}>
              <button className="btn btn-primary" type="button" onClick={() => this._onImport()}>
                {$L('导入')}
              </button>
              <button className="btn btn-link" type="button" onClick={() => this.hide()}>
                {$L('取消')}
              </button>
            </div>
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
      // const s = {}
      // if (res.error_code > 0 || !Array.isArray(res)) {
      //   s.hasError = $L('暂无可用技能')
      // } else {
      //   s.data = res
      // }
      this.setState({ skills: res.data || [] })
    })
  }

  _handleSelectAll(e) {
    const chk = $(e.currentTarget).prop('checked')
    $(e.currentTarget)
      .closest('.rb-modal')
      .find('.skill-import-list input')
      .each(function () {
        $(this).prop('checked', chk)
      })
  }

  _onImport() {
    const selected = []
    for (let k in this._$refs) {
      const $chk = $(this._$refs[k]).find('input')
      if ($chk.prop('checked')) selected.push($chk.val())
    }

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
