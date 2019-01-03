// $Id$
let dashid = null
let dashid_self = false
$(document).ready(function(){
    $('.chart-grid').height($(window).height() - 120)

    let d = $urlp('d')
    if (!!d) $storage.set('DashDefault', d)
    
    $.get(rb.baseUrl + '/dashboard/dash-gets', ((res) => {
        let d = res.data[0]  // default
        if (res.data.length > 1){
            let dset = $storage.get('DashDefault')
            if (dset) {
                for (let i = 0; i < res.data.length; i++){
                    if (res.data[i][0] == dset) {
                        d = res.data[i]
                        break
                    }
                }
            }
        }

        dashid = d[0]
        dashid_self = d[3]
        render_dashboard(d[2])
        $('.dash-list h4').text(d[1])

        if (location.hash){
            let high = $('#chart-' + location.hash.substr(1) + ' > .chart-box').addClass('high')
            high.on('mouseleave', ()=>{
                high.removeClass('high')
            })
        }

        // 仅开放一个仪表盘
        if (dashid_self) $('.J_dash-new').remove()
        else $('.J_dash-edit').remove()
        
        $('.J_dash-select').click( ()=>{  })
        $('.J_dash-new').click( ()=>{ renderRbcomp(<DlgDashAdd />) })
        $('.J_dash-edit').click(()=>{ renderRbcomp(<DlgDashSettings dashid={dashid} title={d[1]} shareToAll={d[4] == 'ALL'} />) })
        $('.J_chart-new').click(()=>{ add_chart() })
        $('.J_chart-select').click(()=>{  })
    }))
})
let rendered_charts = []
$(window).resize(() => {
    $setTimeout(()=>{
        $('.chart-grid').height($(window).height() - 120)
        $(rendered_charts).each((idx, item)=>{ item.resize() })
    }, 200, 'resize-charts')
})

const add_chart = ()=>{
    renderRbcomp(<DlgAddChart dashid={dashid} />)
}

let gridster = null
let gridster_undata = true
let render_dashboard = function(cfg){
    gridster = $('.gridster ul').gridster({
        widget_base_dimensions: ['auto', 100],
        autogenerate_stylesheet: true,
        min_cols: 1,
        max_cols: 12,
        widget_margins: [10, 10],
        resize: {
            enabled: true,
            min_size: [2, 2],
            stop: function(e, ui, $widget){
                $(window).trigger('resize')
                save_dashboard()
            }
        },
        draggable: {
            handle: '.chart-title',
            stop: function(e, ui, $widget){
                save_dashboard()
            }
        },
        serialize_params: function($w, wgd) {
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
    $(cfg).each((idx, item)=>{
        let elid = 'chart-' + item.chart
        let el = '<li data-chart="' + item.chart + '"><div id="' + elid + '"></div><span class="handle-resize"></span></li>'
        gridster.add_widget(el, item.size_x || 2, item.size_y || 2, item.col || null, item.row || null)
        let c = renderRbcomp(detectChart(item, item.chart), elid)
        rendered_charts.push(c)
    })
    if (rendered_charts.length == 0){
        let el = '<li><a class="chart-add" onclick="add_chart()"><i class="zmdi zmdi-plus"></i><p>添加图表</p></a></li>'
        gridster.add_widget(el, 2, 2)
        gridster.disable_resize()
    } else{
        gridster_undata = false
    }
    
    $('.chart-grid').removeClass('invisible')
    $('.J_dash-load').remove()
}

let save_dashboard = function(){
    if (gridster_undata == true || dashid_self == false) return
    $setTimeout(()=>{
        let s = gridster.serialize()
        s = Gridster.sort_by_row_and_col_asc(s)
        $.post(rb.baseUrl + '/dashboard/dash-config?id=' + dashid, JSON.stringify(s), ((res) => {
            console.log(JSON.stringify(s) + ' > ' + JSON.stringify(res))
        }))
    }, 500, 'save-dashboard')
}

class DlgAddChart extends RbFormHandler {
    constructor(props) {
        super(props)
    }
    render() {
        return (<RbDialog title="添加图表" ref="dlg">
                <form>
                <div className="form-group row">
                    <label className="col-sm-3 col-form-label text-sm-right">图表数据来源</label>
                    <div className="col-sm-7">
                        <select className="form-control form-control-sm" ref="entity" />
                    </div>
                </div>
                <div className="form-group row footer">
                    <div className="col-sm-7 offset-sm-3">
                        <button className="btn btn-primary" type="button" onClick={()=>this.next()}>下一步</button>
                    </div>
                </div>
            </form>
            </RbDialog>)
    }
    componentDidMount() {
        let that = this
        let entity_el = $(this.refs['entity'])
        $.get(rb.baseUrl + '/commons/metadata/entities', (res)=>{
            $(res.data).each(function(){
                $('<option value="' + this.name + '">' + this.label + '</option>').appendTo(entity_el)
            })
            this.__select2 = entity_el.select2({
                language: 'zh-CN',
                placeholder: '选择数据来源',
                width: '100%'
            })
        })
    }
    next() {
        let e = this.__select2.val()
        if (!!!e) return
        location.href = rb.baseUrl + '/dashboard/chart-design?source=' + e + '&dashid=' + this.props.dashid
    }
}

class DlgDashSettings extends RbFormHandler {
    constructor(props) {
        super(props)
    }
    render() {
        return (<RbDialog title="仪表盘设置" ref="dlg">
                <form>
                <div className="form-group row">
                    <label className="col-sm-3 col-form-label text-sm-right">名称</label>
                    <div className="col-sm-7">
                        <input className="form-control form-control-sm" value={this.state.title || ''} placeholder="默认仪表盘" data-id="title" onChange={this.handleChange} maxLength="40" />
                    </div>
                </div>
                <div className="form-group row">
                    <label className="col-sm-3 col-form-label text-sm-right"></label>
                    <div className="col-sm-7">
                        <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mt-0 mb-0">
                            <input className="custom-control-input" type="checkbox" checked={this.state.shareToAll == true} data-id="shareToAll" onChange={this.handleChange} />
                            <span className="custom-control-label">共享此仪表盘给全部用户</span>
                        </label>
                    </div>
                </div>
                <div className="form-group row footer">
                    <div className="col-sm-7 offset-sm-3">
                        <button className="btn btn-primary" type="button" onClick={()=>this.save()}>确定</button>
                    </div>
                </div>
            </form>
            </RbDialog>)
    }
    save() {
        let _data = { shareTo: this.state.shareToAll == true ? 'ALL' : 'SELF', title: this.state.title || '默认仪表盘' }
        _data.metadata = { id: this.props.dashid, entity: 'DashboardConfig' }
        $.post(rb.baseUrl + '/dashboard/dash-update', JSON.stringify(_data), (res)=>{
            if (res.error_code == 0){
                rb.notice('设置已保存', 'success')
                this.hide()
            } else rb.notice(res.error_msg, 'danger')
        })
    }
}

class DlgDashAdd extends RbFormHandler {
    constructor(props) {
        super(props)
    }
    render() {
        return (<RbDialog title="添加仪表盘" ref="dlg">
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
                            <input className="custom-control-input" type="checkbox" checked={this.state.copy == true} data-id="copy" onChange={this.handleChange} />
                            <span className="custom-control-label">复制当前仪表盘</span>
                        </label>
                    </div>
                </div>
                <div className="form-group row footer">
                    <div className="col-sm-7 offset-sm-3">
                        <button className="btn btn-primary" type="button" onClick={()=>this.save()}>确定</button>
                    </div>
                </div>
            </form>
            </RbDialog>)
    }
    save() {
        let _data = { title: this.state.title || '我的仪表盘' }
        _data.metadata = { entity: 'DashboardConfig' }
        if (this.state.copy == true) _data.__copy = gridster.serialize()
        
        $.post(rb.baseUrl + '/dashboard/dash-new', JSON.stringify(_data), (res)=>{
            if (res.error_code == 0){
                location.href = '?d=' + res.data.id
            } else rb.notice(res.error_msg, 'danger')
        })
    }
}