/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// eslint-disable-next-line no-undef
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
  $('.J_addSkill').on('click', () => _editSkill())
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
          <td>${item.name || ''}</td>
          <td class="text-muted">${cfg.description || ''}</td>
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
  renderRbcomp(<DlgSkillEdit item={item} title={item ? $L('编辑技能') : $L('添加技能')} />)
}

class DlgSkillEdit extends RbModal {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  renderContent() {
    const item = this.props.item || {}
    const cfg = item.config || {}
    return (
      <div className="form">
        <div className="form-group">
          <label className="text-bold">{$L('技能名称')}</label>
          <input type="text" className="form-control form-control-sm" maxLength="40" defaultValue={cfg.name || item.name || ''} ref={(c) => (this._$name = c)} autoFocus />
        </div>
        <div className="form-group">
          <label className="text-bold">{$L('描述')}</label>
          <input type="text" className="form-control form-control-sm" maxLength="100" defaultValue={cfg.description || ''} ref={(c) => (this._$desc = c)} />
        </div>
        <div className="form-group">
          <label className="text-bold">{$L('提示词')}</label>
          <textarea className="form-control form-control-sm row3" maxLength="4000" defaultValue={cfg.prompt || ''} ref={(c) => (this._$prompt = c)} />
        </div>
        <div className="form-group mb-2">
          <button className="btn btn-primary" type="button" onClick={() => this._onSave()}>{$L('保存')}</button>
        </div>
      </div>
    )
  }

  _onSave() {
    const item = this.props.item || {}
    const isEdit = !!item.id
    const name = $(this._$name).val()
    if (!name) {
      RbHighbar.create($L('请输入技能名称'))
      return
    }

    const data = {
      name: name,
      type: 'AIBOT_SKILL',
      belongEntity: 'N',
      shareTo: 'ALL',
      config: JSON.stringify({
        name: name,
        description: $(this._$desc).val(),
        prompt: $(this._$prompt).val(),
      }),
      metadata: {
        entity: 'CommonsConfig',
        id: isEdit ? item.id : null,
      },
    }

    this.disabled(true)
    $.post('/app/entity/common-save', JSON.stringify(data), (res) => {
      if (res.error_code === 0) {
        this.hide()
        _loadSkills()
      } else {
        RbHighbar.error(res.error_msg)
        this.disabled()
      }
    })
  }
}

const _deleteSkill = function (item) {
  RbAlert.create($L('确认删除技能「%s」？', item.name || ''), {
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
