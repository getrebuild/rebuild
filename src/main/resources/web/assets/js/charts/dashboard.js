/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable react/no-string-refs */

let dashid = null
let dash_editable = false

let refresh_timeout = 0
let refresh_timer = null

let on_resizestart = false
let rendered_charts = []

$(document).ready(function () {
  const d = $urlp('d')
  if (d) $storage.set('DashDefault', d)

  let dash_list = null
  $.get('/dashboard/dash-gets', (res) => {
    typeof window.startTour === 'function' && window.startTour(1000)

    dash_list = res.data
    if (!dash_list || dash_list.length === 0) {
      $('.chart-grid').removeClass('invisible')
      $('.J_dash-load').remove()

      renderRbcomp(<RbAlertBox message={$L('暂无仪表盘')} />, $('.chart-grid')[0])
      return
    }

    let d = dash_list[0] // default
    if (dash_list.length > 1) {
      const dset = $storage.get('DashDefault')
      if (dset) {
        for (let i = 0; i < res.data.length; i++) {
          if (res.data[i][0] === dset) {
            d = res.data[i]
            break
          }
        }
      }
    }

    dashid = d[0]
    dash_editable = d[2]
    render_dashboard(d[3])
    $('.dash-list h4').text(d[4])

    if (location.hash && location.hash.length > 20) {
      if (location.hash.substr(0, 5) === '#del=') {
        RbHighbar.success($L('仪表盘已删除'))
        location.hash = ''
      } else {
        const high = $('#chart-' + location.hash.substr(1)).addClass('high')
        if (high.length > 0) {
          high.on('mouseleave', () => {
            high.removeClass('high').off('mouseleave')
          })
          $gotoSection(high.offset().top - 65)
        }
      }
    }

    if (dash_editable !== true) {
      $('.J_dash-edit, .J_chart-adds').remove()
      $('.chart-grid').addClass('uneditable')
    }

    $('.J_dash-new').on('click', () => dlgShow('DlgDashAdd'))
    $('.J_dash-edit').on('click', () => dlgShow('DlgDashSettings', { title: d[4], shareTo: d[1] }))
    $('.J_chart-new').on('click', () => dlgShow('DlgAddChart'))
    $('.J_dash-select').on('click', () => dlgShow('DashSelect', { dashList: dash_list }))

    $('.J_dash-refresh .dropdown-item').on('click', function () {
      const $this = $(this)
      $('.J_dash-refresh .btn span').text($this.text())
      refresh_timeout = ~~$this.data('time')

      if (refresh_timer) {
        clearInterval(refresh_timer)
        refresh_timer = null
      }

      if (refresh_timeout > 0) {
        refresh_timer = setInterval(() => {
          rendered_charts.forEach((x) => x.loadChartData())
        }, refresh_timeout * 1000)
      }
    })

    let dlgChartSelect
    $('.J_chart-select').on('click', () => {
      const appended = []
      $('.grid-stack-item-content').each(function () {
        appended.push($(this).attr('id').substr(6))
      })

      if (dlgChartSelect) {
        dlgChartSelect.show()
        dlgChartSelect.setState({ appended: appended })
        return
      }

      const _select = function (chart) {
        chart.w = chart.h = 4
        add_widget(chart)
      }
      // eslint-disable-next-line react/jsx-no-undef
      renderRbcomp(<ChartSelect key="ChartSelect" select={_select} />, null, function () {
        dlgChartSelect = this
        this.setState({ appended: appended })
      })
    })
  })

  $('.J_dash-fullscreen').on('click', () => {
    const $body = $(document.body)
    if ($body.hasClass('fullscreen')) $fullscreen.exit()
    else $fullscreen.open()
    $body.toggleClass('fullscreen')
  })

  $addResizeHandler(() => {
    if (on_resizestart === true) return
    console.log('Resize dashboard ...')

    rendered_charts.forEach((x) => x.resize())
    // eslint-disable-next-line no-undef
    BaseChart.currentFullscreen && BaseChart.currentFullscreen.toggleFullscreen(true)
  })
})

// 全屏工具
const $fullscreen = {
  open: function () {
    const element = document.documentElement
    if (element.requestFullscreen) element.requestFullscreen()
    else if (element.msRequestFullscreen) element.msRequestFullscreen()
    else if (element.mozRequestFullScreen) element.mozRequestFullScreen()
    else if (element.webkitRequestFullscreen) element.webkitRequestFullscreen()
  },
  exit: function () {
    if (document.exitFullscreen) document.exitFullscreen()
    else if (document.msExitFullscreen) document.msExitFullscreen()
    else if (document.mozCancelFullScreen) document.mozCancelFullScreen()
    else if (document.webkitExitFullscreen) document.webkitExitFullscreen()
  },
  is: function () {
    return !!(document.fullscreenElement || document.msFullscreenElement || document.mozFullScreenElement || document.webkitFullscreenElement)
  },
}

const dlgRefs = {}
const dlgShow = (t, props) => {
  props = props || {}
  props.dashid = props.dashid || dashid
  if (dlgRefs[t]) {
    dlgRefs[t].show()
  } else if (t === 'DlgAddChart') {
    if (dash_editable) {
      renderRbcomp(<DlgAddChart {...props} />, null, function () {
        dlgRefs[t] = this
      })
    } else {
      RbHighbar.create($L('你无权添加图表到此仪表盘'))
    }
  } else if (t === 'DlgDashAdd') {
    renderRbcomp(<DlgDashAdd {...props} />, null, function () {
      dlgRefs[t] = this
    })
  } else if (t === 'DlgDashSettings') {
    renderRbcomp(<DlgDashSettings {...props} />, null, function () {
      dlgRefs[t] = this
    })
  } else if (t === 'DashSelect') {
    renderRbcomp(<DashSelect {...props} />, null, function () {
      dlgRefs[t] = this
    })
  }
}

let gridstack
let gridstack_serialize
const render_dashboard = function (init) {
  gridstack = $('.grid-stack')
    .gridstack({
      cellHeight: 60,
      handleClass: 'chart-title',
      animate: true,
      auto: false,
      verticalMargin: 10,
      disableDrag: !dash_editable,
      disableResize: !dash_editable,
    })
    .data('gridstack')

  gridstack_serialize = init
  $(init).each((idx, item) => add_widget(item))
  if (rendered_charts.length === 0) {
    const gsi = `<div class="grid-stack-item"><div id="chart-add" class="grid-stack-item-content"><a class="chart-add"><i class="zmdi zmdi-plus"></i><p>${$L('添加图表')}</p></a></div></div>`
    const $gsi = gridstack.addWidget(gsi, 0, 0, 2, 2)
    $gsi.find('a').on('click', () => {
      if ($('.J_chart-new').length === 0) $('.J_chart-select').trigger('click')
      else dlgShow('DlgAddChart')
    })
    gridstack.disable()
  }

  // When resize/re-postion/remove
  $('.grid-stack')
    .on('change', function () {
      $setTimeout(save_dashboard, 500, 'save_dashboard')
    })
    .on('resizestart', function () {
      on_resizestart = true
    })
    .on('gsresizestop', function () {
      $(rendered_charts).each((idx, item) => item.resize())
      on_resizestart = false
    })

  $('.chart-grid').removeClass('invisible')
  $('.J_dash-load').remove()
}

const add_widget = function (item) {
  const chid = 'chart-' + item.chart
  if ($('#' + chid).length > 0) return false

  const chart_add = $('#chart-add')
  if (chart_add.length > 0) gridstack.removeWidget(chart_add.parent())

  const gsi = `<div class="grid-stack-item"><div id="${chid}" class="grid-stack-item-content"></div></div>`
  // Use gridstar
  if (item.size_x || item.size_y) {
    gridstack.addWidget(gsi, (item.col || 1) - 1, (item.row || 1) - 1, item.size_x || 2, item.size_y || 2, 2, 12, 2, 12)
  } else {
    gridstack.addWidget(gsi, item.x, item.y, item.w, item.h, item.x === undefined, 2, 12, 2, 12)
  }

  item.editable = dash_editable
  // eslint-disable-next-line no-undef
  renderRbcomp(detectChart(item, item.chart), chid, function () {
    rendered_charts.push(this)
  })
}

const save_dashboard = function () {
  if (dash_editable !== true) return
  const s = []
  $('.chart-grid .grid-stack-item').each(function () {
    const $this = $(this)
    const chid = $this.find('.grid-stack-item-content').attr('id')
    if (chid && chid.length > 20) {
      s.push({
        x: $this.attr('data-gs-x'),
        y: $this.attr('data-gs-y'),
        w: $this.attr('data-gs-width'),
        h: $this.attr('data-gs-height'),
        chart: chid.substr(6),
      })
    }
  })
  gridstack_serialize = s
  $setTimeout(
    () => {
      $.post(`/dashboard/dash-config?id=${dashid}`, JSON.stringify(gridstack_serialize), () => {
        if (rb.env === 'dev') console.log('Saved dashboard : ' + JSON.stringify(gridstack_serialize))
      })
    },
    500,
    'save-dashboard'
  )
}

// 添加图表
class DlgAddChart extends RbFormHandler {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <RbModal title={$L('添加图表')} ref="dlg">
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('图表数据来源')}</label>
            <div className="col-sm-7">
              <select className="form-control form-control-sm" ref="entity" />
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3">
              <button className="btn btn-primary" type="button" onClick={() => this.next()}>
                {$L('下一步')}
              </button>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    const $entity = $(this.refs['entity'])
    $.get('/commons/metadata/entities?detail=true', (res) => {
      $(res.data).each(function () {
        $('<option value="' + this.name + '">' + this.label + '</option>').appendTo($entity)
      })
      this.__select2 = $entity.select2({
        allowClear: false,
        placeholder: $L('选择数据来源'),
        matcher: $select2MatcherAll,
      })
    })
  }

  next() {
    const e = this.__select2.val()
    if (!e) return
    location.href = `${rb.baseUrl}/dashboard/chart-design?source=${e}&dashid=${this.props.dashid}`
  }
}

// 仪表盘设置
class DlgDashSettings extends RbFormHandler {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <RbModal title={$L('设置')} ref="dlg">
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('名称')}</label>
            <div className="col-sm-7">
              <input className="form-control form-control-sm" value={this.state.title || ''} placeholder={$L('默认仪表盘')} data-id="title" onChange={this.handleChange} maxLength="40" />
            </div>
          </div>
          {rb.isAdminUser && (
            <div className="form-group row">
              <label className="col-sm-3 col-form-label text-sm-right" />
              <div className="col-sm-7">
                <div className="shareTo--wrap">
                  <Share2 ref={(c) => (this._Share2 = c)} noSwitch={true} shareTo={this.props.shareTo} />
                </div>
              </div>
            </div>
          )}
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3">
              <button className="btn btn-primary btn-space" type="button" onClick={() => this.save()}>
                {$L('确定')}
              </button>
              <button className="btn btn-danger btn-outline btn-space" type="button" onClick={() => this.delete()}>
                <i className="zmdi zmdi-delete icon" /> {$L('删除')}
              </button>
            </div>
          </div>
        </div>
      </RbModal>
    )
  }

  save() {
    const _data = {
      shareTo: this._Share2 ? this._Share2.getData().shareTo : 'SELF',
      title: this.state.title || $L('默认仪表盘'),
    }
    _data.metadata = {
      id: this.props.dashid,
      entity: 'DashboardConfig',
    }

    $.post('/app/entity/common-save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        $('.dash-head h4').text(_data.title)
        if (dlgRefs['DashSelect']) {
          dlgRefs['DashSelect'].setState({ dashTitle: _data.title })
        }
        this.hide()
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }

  delete() {
    RbAlert.create($L('确认删除此仪表盘？'), {
      type: 'danger',
      confirmText: $L('删除'),
      confirm: function () {
        this.disabled(true)
        $.post(`/app/entity/common-delete?id=${dashid}`, function (res) {
          // if (res.error_code === 0) location.replace('home#del=' + dashid)  // Chrome no refresh?
          if (res.error_code === 0) location.reload()
          else RbHighbar.error(res.error_msg)
        })
      },
    })
  }
}

// 添加仪表盘
class DlgDashAdd extends RbFormHandler {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <RbModal title={$L('添加仪表盘')} ref="dlg">
        <div className="form">
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right">{$L('名称')}</label>
            <div className="col-sm-7">
              <input className="form-control form-control-sm" value={this.state.title || ''} placeholder={$L('我的仪表盘')} data-id="title" onChange={this.handleChange} maxLength="40" />
            </div>
          </div>
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right" />
            <div className="col-sm-7">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mt-0 mb-0">
                <input className="custom-control-input" type="checkbox" checked={this.state.copy === true} data-id="copy" onChange={this.handleChange} />
                <span className="custom-control-label">{$L('复制当前仪表盘')}</span>
              </label>
            </div>
          </div>
          <div className="form-group row footer">
            <div className="col-sm-7 offset-sm-3">
              <button className="btn btn-primary" type="button" onClick={this.save}>
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

  save = () => {
    const _data = {
      title: this.state.title || $L('我的仪表盘'),
      metadata: { entity: 'DashboardConfig' },
    }
    if (this.state.copy === true) _data.__copy = gridstack_serialize

    $.post('/dashboard/dash-new', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) location.href = '?d=' + res.data.id
      else RbHighbar.error(res.error_msg)
    })
  }
}

// 选择默认仪表盘
class DashSelect extends React.Component {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <div className="modal select-list" ref={(c) => (this._dlg = c)} tabIndex="-1">
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={this.hide}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <div>
                <ul className="list-unstyled">
                  {(this.props.dashList || []).map((item) => {
                    return (
                      <li key={'dash-' + item[0]}>
                        <a href={'?d=' + item[0]}>
                          {item[4]}
                          <i className="icon zmdi zmdi-arrow-right" />
                        </a>
                      </li>
                    )
                  })}
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount = () => $(this._dlg).modal({ show: true, keyboard: true })
  hide = () => $(this._dlg).modal('hide')
  show = () => $(this._dlg).modal('show')
}
