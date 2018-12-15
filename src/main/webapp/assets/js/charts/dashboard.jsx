// $Id$
let dashid = null
$(document).ready(function(){
    $(window).trigger('resize')
    
    $.get(rb.baseUrl + '/dashboard/dash-gets', ((res) => {
        let d = res.data[0]
        $('.J_add-chart').click(() => {
            renderRbcomp(<DlgAddChart dashid={d[0]} />)
        })
        dashid = d[0]
        render_dashboard((d[2]))
    }))
})
$(window).resize(() => {
    $('.chart-grid').height($(window).height() - 131)
})

class DlgAddChart extends React.Component {
    constructor(props) {
        super(props)
    }
    render() {
        return (<RbModal title="添加图表">
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
    next() {
        location.href = rb.baseUrl + '/dashboard/chart-design?source=' + this.select2.val() + '&dashid=' + this.props.dashid
    }
}

let gridster = null
let render_dashboard = function(cfg){
    gridster = $('.gridster ul').gridster({
        widget_base_dimensions: ['auto', 100],
        autogenerate_stylesheet: true,
        min_cols: 1,
        max_cols: 12,
        widget_margins: [5, 5],
        resize: {
            enabled: true,
            min_size: [2, 2],
            stop: function(e, ui, $widget){
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
    //cfg = Gridster.sort_by_row_and_col_asc(cfg)
    $(cfg).each((idx, item)=>{
        let el_id = 'chart-' + item.chart
        let el = '<li data-chart="' + item.chart + '"><div id="' + el_id + '"></div><span class="handle-resize"></span></li>'
        gridster.add_widget(el, item.size_x || 2, item.size_y || 2, item.col || null, item.row || null)
        renderRbcomp(detectChart(item, item.chart), el_id)
    })
}

let save_dashboard = function(){
    let s = gridster.serialize()
    console.log(s)
    $.post(rb.baseUrl + '/dashboard/dash-save?id=' + dashid, JSON.stringify(s), ((res) => {
        console.log(res)
    }))
}