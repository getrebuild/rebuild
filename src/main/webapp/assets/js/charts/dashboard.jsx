/* eslint-disable react/prop-types */
/* eslint-disable react/no-string-refs */
let dashid = null
let dash_editable = false
$(document).ready(function () {
  $('.chart-grid').height($(window).height() - 120)

  let d = $urlp('d')
  if (d) $storage.set('DashDefault', d)

  let dash_list = null
  $.get(rb.baseUrl + '/dashboard/dash-gets', ((res) => {
    dash_list = res.data
    let d = dash_list[0]  // default
    if (res.data.length > 1) {
      let dset = $storage.get('DashDefault')
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
    dash_editable = d[3]
    render_dashboard(d[2])
    $('.dash-list h4').text(d[1])

    if (location.hash && location.hash.length > 20) {
      if (location.hash.substr(0, 5) === '#del=') {
        rb.hbsuccess('仪表盘已删除')
        location.hash = ''
      } else {
        let high = $('#chart-' + location.hash.substr(1) + ' > .chart-box').addClass('high')
        high.on('mouseleave', () => {
          high.removeClass('high')
        })
      }
    }

    if (dash_editable !== true) $('.J_dash-edit, .J_chart-adds').remove()

    $('.J_dash-new').click(() => { show_dlg('DlgDashAdd') })
    $('.J_dash-edit').click(() => { show_dlg('DlgDashSettings', { title: d[1], shareToAll: d[4] === 'ALL' }) })
    $('.J_chart-new').click(() => { show_dlg('DlgAddChart') })
    $('.J_dash-select').click(() => { show_dlg('DashSelect', { dashList: dash_list }) })
    $('.J_chart-select').click(() => { show_dlg('ChartSelect', { dlgClazz: 'dlg-chart-select', dlgTitle: '选择图表' }) })
  }))
})
let rendered_charts = []
$(window).resize(() => {
  $setTimeout(() => {
    $('.chart-grid').height($(window).height() - 120)
    $(rendered_charts).each((idx, item) => { item.resize() })
  }, 200, 'resize-charts')
})

const dlg_cached = {}
const show_dlg = (t, props) => {
  props = props || {}
  props.dashid = props.dashid || dashid
  if (dlg_cached[t]) dlg_cached[t].show()
  else if (t === 'DlgAddChart') dlg_cached[t] = renderRbcomp(<DlgAddChart {...props} />)
  else if (t === 'DlgDashAdd') dlg_cached[t] = renderRbcomp(<DlgDashAdd {...props} />)
  else if (t === 'DlgDashSettings') dlg_cached[t] = renderRbcomp(<DlgDashSettings {...props} />)
  else if (t === 'DashSelect') dlg_cached[t] = renderRbcomp(<DashSelect {...props} />)
  else if (t === 'ChartSelect') dlg_cached[t] = renderRbcomp(<ChartSelect {...props} />)
}

let gridster = null
let gridster_undata = true
let render_dashboard = function (cfg) {
  gridster = $('.gridster ul').gridster({
    widget_base_dimensions: ['auto', 100],
    autogenerate_stylesheet: true,
    min_cols: 1,
    max_cols: 12,
    widget_margins: [10, 10],
    resize: {
      enabled: true,
      min_size: [2, 2],
      // eslint-disable-next-line no-unused-vars
      stop: function (e, ui, $widget) {
        $(window).trigger('resize')
        save_dashboard()
      }
    },
    draggable: {
      handle: '.chart-title',
      // eslint-disable-next-line no-unused-vars
      stop: function (e, ui, $widget) {
        save_dashboard()
      }
    },
    serialize_params: function ($w, wgd) {
      return {
        col: wgd.col,
        row: wgd.row,
        size_x: wgd.size_x,
        size_y: wgd.size_y,
        chart: $w.data('chart')
      }
    }
  }).data('gridster')

  gridster.remove_all_widgets()
  rendered_charts = []
  $(cfg).each((idx, item) => {
    let elid = 'chart-' + item.chart
    let el = '<li data-chart="' + item.chart + '"><div id="' + elid + '"></div><span class="handle-resize"></span></li>'
    gridster.add_widget(el, item.size_x || 2, item.size_y || 2, item.col || null, item.row || null)
    // eslint-disable-next-line no-undef
    let c = renderRbcomp(detectChart(item, item.chart, dash_editable), elid)
    rendered_charts.push(c)
  })
  if (rendered_charts.length === 0) {
    let el = '<li><a class="chart-add" onclick="show_dlg(\'DlgAddChart\')"><i class="zmdi zmdi-plus"></i><p>添加图表</p></a></li>'
    gridster.add_widget(el, 2, 2)
    gridster.disable_resize()
  } else {
    gridster_undata = false
  }

  $('.chart-grid').removeClass('invisible')
  $('.J_dash-load').remove()
}

let save_dashboard = function () {
  if (gridster_undata === true || dash_editable !== true) return
  $setTimeout(() => {
    let s = gridster.serialize()
    // eslint-disable-next-line no-undef
    s = Gridster.sort_by_row_and_col_asc(s)
    $.post(rb.baseUrl + '/dashboard/dash-config?id=' + dashid, JSON.stringify(s), (() => {
    }))
  }, 500, 'save-dashboard')
}

class DlgAddChart extends RbFormHandler {
  constructor(props) {
    super(props)
  }
  render() {
    return (<RbModal title="添加图表" ref="dlg">
      <form>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">图表数据来源</label>
          <div className="col-sm-7">
            <select className="form-control form-control-sm" ref="entity" />
          </div>
        </div>
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3">
            <button className="btn btn-primary" type="button" onClick={() => this.next()}>下一步</button>
          </div>
        </div>
      </form>
    </RbModal>)
  }
  componentDidMount() {
    let entity_el = $(this.refs['entity'])
    $.get(rb.baseUrl + '/commons/metadata/entities', (res) => {
      $(res.data).each(function () {
        $('<option value="' + this.name + '">' + this.label + '</option>').appendTo(entity_el)
      })
      this.__select2 = entity_el.select2({
        placeholder: '选择数据来源'
      })
    })
  }
  next() {
    let e = this.__select2.val()
    if (!e) return
    location.href = rb.baseUrl + '/dashboard/chart-design?source=' + e + '&dashid=' + this.props.dashid
  }
}


class DlgDashSettings extends RbFormHandler {
  constructor(props) {
    super(props)
  }
  render() {
    return (<RbModal title="仪表盘设置" ref="dlg">
      <form>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">名称</label>
          <div className="col-sm-7">
            <input className="form-control form-control-sm" value={this.state.title || ''} placeholder="默认仪表盘" data-id="title" onChange={this.handleChange} maxLength="40" />
          </div>
        </div>
        {rb.isAdminUser !== true ? null :
          <div className="form-group row">
            <label className="col-sm-3 col-form-label text-sm-right"></label>
            <div className="col-sm-7">
              <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mt-0 mb-0">
                <input className="custom-control-input" type="checkbox" checked={this.state.shareToAll === true} data-id="shareToAll" onChange={this.handleChange} />
                <span className="custom-control-label">共享此仪表盘给全部用户</span>
              </label>
            </div>
          </div>
        }
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3">
            <button className="btn btn-primary btn-space" type="button" onClick={() => this.save()}>确定</button>
            <button className="btn btn-secondary btn-space" type="button" onClick={() => this.delete()}><i className="zmdi zmdi-delete icon" /> 删除</button>
          </div>
        </div>
      </form>
    </RbModal >)
  }
  save() {
    let _data = { shareTo: this.state.shareToAll === true ? 'ALL' : 'SELF', title: this.state.title || '默认仪表盘' }
    _data.metadata = { id: this.props.dashid, entity: 'DashboardConfig' }
    $.post(rb.baseUrl + '/dashboard/dash-update', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        // rb.hbsuccess('设置已保存')
        $('.dash-head h4').text(_data.title)
        if (dlg_cached['DashSelect']) {
          dlg_cached['DashSelect'].setState({ 'dashTitle': _data.title })
        }
        this.hide()
      } else rb.hberror(res.error_msg)
    })
  }
  delete() {
    rb.alert('确认删除此仪表盘？', {
      confirm: function () {
        $.post(rb.baseUrl + '/dashboard/dash-delete?id=' + dashid, function (res) {
          if (res.error_code === 0) location.replace('home#del=' + dashid)
          else rb.hberror(res.error_msg)
        })
      }
    })
  }
}

class DlgDashAdd extends RbFormHandler {
  constructor(props) {
    super(props)
  }
  render() {
    return (<RbModal title="添加仪表盘" ref="dlg">
      <form>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">名称</label>
          <div className="col-sm-7">
            <input className="form-control form-control-sm" value={this.state.title || ''} placeholder="我的仪表盘" data-id="title" onChange={this.handleChange} maxLength="40" />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right"></label>
          <div className="col-sm-7">
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mt-0 mb-0">
              <input className="custom-control-input" type="checkbox" checked={this.state.copy === true} data-id="copy" onChange={this.handleChange} />
              <span className="custom-control-label">复制当前仪表盘</span>
            </label>
          </div>
        </div>
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3">
            <button className="btn btn-primary" type="button" onClick={() => this.save()}>确定</button>
          </div>
        </div>
      </form>
    </RbModal>)
  }
  save() {
    let _data = { title: this.state.title || '我的仪表盘' }
    _data.metadata = { entity: 'DashboardConfig' }
    if (this.state.copy === true) _data.__copy = gridster.serialize()

    $.post(rb.baseUrl + '/dashboard/dash-new', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) {
        location.href = '?d=' + res.data.id
      } else rb.hberror(res.error_msg)
    })
  }
}

class DashPanel extends React.Component {
  constructor(props) {
    super(props)
  }
  render() {
    return (
      <div className={'modal ' + (this.props.dlgClazz || 'dlg-dash-select')} ref="dlg" tabIndex="-1">
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <h4>{this.props.dlgTitle || ''}</h4>
              <button className="close" type="button" onClick={() => this.hide()}><span className="zmdi zmdi-close" /></button>
            </div>
            <div className="modal-body">
              {this.renderPanel()}
            </div>
          </div>
        </div>
      </div>
    )
  }
  renderPanel() {
    return (<ul className="list-unstyled">
      {(this.props.dashList || []).map((item) => {
        let title = item[1]
        if (item[0] === dashid) title = this.state.dashTitle || $('.dash-head h4').text() || title
        return <li key={'dash-' + item[0]}><a href={'?d=' + item[0]}>{title}<i className="icon zmdi zmdi-arrow-right"></i></a></li>
      })}
    </ul>)
  }
  componentDidMount() {
    this.show()
  }
  hide() {
    $(this.refs['dlg']).modal('hide')
  }
  show() {
    $(this.refs['dlg']).modal({ show: true, keyboard: true })
  }
}

class DashSelect extends DashPanel {
  constructor(props) {
    super(props)
    this.state = { dashTitle: null }
  }
  renderPanel() {
    return (
      <ul className="list-unstyled">
        {(this.props.dashList || []).map((item) => {
          let title = item[1]
          if (item[0] === dashid) title = this.state.dashTitle || $('.dash-head h4').text() || title
          return <li key={'dash-' + item[0]}><a href={'?d=' + item[0]}>{title}<i className="icon zmdi zmdi-arrow-right"></i></a></li>
        })}
      </ul>
    )
  }
}

// TODO 从已有图表中选择图表
// 添加的图表会在多个仪表盘共享（本身就是一个），修改时会同步修改
class ChartSelect extends DashPanel {
  constructor(props) {
    super(props)
  }
  renderPanel() {
    return (<a>TODO</a>)
  }
  componentDidMount() {
    super.componentDidMount()
  }
}