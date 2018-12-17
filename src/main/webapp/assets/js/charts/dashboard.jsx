// $Id$
let dashid = null
$(document).ready(function(){
    $('.chart-grid').height($(window).height() - 131)
    
    let dlg = null
    $.get(rb.baseUrl + '/dashboard/dash-gets', ((res) => {
        let d = res.data[0]  // default
        dashid = d[0]
        render_dashboard(d[2])
        $('.J_add-chart').click(add_chart)
    }))
})
let rendered_charts = []
$(window).resize(() => {
    $setTimeout(()=>{
        $('.chart-grid').height($(window).height() - 131)
        $(rendered_charts).each((idx, item)=>{ item.resize() })
    }, 200, 'resize-charts')
})

let add_chart_dlg = null
let add_chart = function(){
    if (add_chart_dlg) add_chart_dlg.show()
    else add_chart_dlg = renderRbcomp(<DlgAddChart dashid={dashid} />)
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
    if (gridster_undata == true) return
    $setTimeout(()=>{
        let s = gridster.serialize()
        s = Gridster.sort_by_row_and_col_asc(s)
        $.post(rb.baseUrl + '/dashboard/dash-save?id=' + dashid, JSON.stringify(s), ((res) => {
            console.log(JSON.stringify(s) + ' > ' + JSON.stringify(res))
        }))
    }, 500, 'save-dashboard')
}

class DlgAddChart extends React.Component {
    constructor(props) {
        super(props)
    }
    render() {
        return (<RbModal title="添加图表" ref="dlg" destroyOnHide={false}>
                <form>
                <div className="form-group row">
                    <label className="col-sm-3 col-form-label text-sm-right">图表数据来源</label>
                    <div className="col-sm-7">
                        <select className="form-control form-control-sm" ref="sentity" />
                    </div>
                </div>
                <div className="form-group row footer">
                    <div className="col-sm-7 offset-sm-3">
                        <button className="btn btn-primary" type="button" onClick={()=>this.next()}>下一步</button>
                    </div>
                </div>
            </form>
            </RbModal>)
    }
    componentDidMount() {
        let that = this
        let sentity = $(this.refs['sentity'])
        $.get(rb.baseUrl + '/commons/metadata/entities', function(res){
            $(res.data).each(function(){
                $('<option value="' + this.name + '">' + this.label + '</option>').appendTo(sentity)
            })
            that.select2 = sentity.select2({
                language: 'zh-CN',
                placeholder: '选择数据源',
                width: '100%'
            })
        })
    }
    show() {
        this.refs['dlg'].show()
    }
    next() {
        location.href = rb.baseUrl + '/dashboard/chart-design?source=' + this.select2.val() + '&dashid=' + this.props.dashid
    }
}