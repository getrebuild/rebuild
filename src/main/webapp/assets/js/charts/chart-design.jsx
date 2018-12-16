$(document).ready(() => {
    $(window).trigger('resize')
    
    $('.chart-type>a').tooltip({ html:true, container:'.config-aside' })
    
	let dragIsNum = false
    $('.fields a').draggable({
    	helper: 'clone',
    	appendTo: 'body',
    	cursor: 'move',
    	cursorAt: { top: 14, left: 75 },
    	start: function(){
    		dragIsNum = $(this).data('type') == 'num'
    	}
    })
	$('.axis-target').droppable({
		accept: function(){
			if ($(this).hasClass('J_axis-dim')) return !dragIsNum
			else return true
		},
		drop: function(event, ui){
			add_axis(this, $(ui.draggable[0]))
		}
	})
	
	let cts = $('.chart-type > a').click(function(){
		let _this = $(this)
		if (_this.hasClass('active') == false) return
		cts.removeClass('select')
		_this.addClass('select')
		render_option()
	})
	
	$('.rb-toggle-left-sidebar').attr('title', '保存并返回').off('click').on('click', () => {
	    let cfg = build_config()
	    if (!!!cfg){
	        rb.notice('当前图表无数据')
	        return
	    }
	    let _data = { config: JSON.stringify(cfg), title: cfg.title, belongEntity: cfg.entity, type: cfg.type }
	    _data.metadata = { entity: 'ChartConfig', id: window.__chartId }
	    
	    console.log(JSON.stringify(_data))
        $.post(rb.baseUrl + '/dashboard/chart-save?dashid=' + $urlp('dashid'), JSON.stringify(_data), function(res){
            if (res.error_code == 0){
                window.__chartId = res.data.id
            }
        })
	    
	}).find('.zmdi').addClass('zmdi-arrow-left')
	
	if (window.__chartConfig && window.__chartConfig.axis) {
	    let cfg = window.__chartConfig
	    $(cfg.axis.dimension).each((idx, item) => { add_axis('.J_axis-dim', item) })
	    $(cfg.axis.numerical).each((idx, item) => { add_axis('.J_axis-num', item) })
	    $('.chart-type>a[data-type="' + cfg.type + '"]').trigger('click')
	}
    if (!window.__chartId) $('<h4 class="chart-undata must-center">当前图表无数据</h4>').appendTo('#chart-preview')
})
$(window).resize(() => {
    $setTimeout(()=>{
        $('#chart-preview').height($(window).height() - 170)
        if (render_preview_chart) render_preview_chart.resize()
    }, 200, 'resize-chart-preview')
})

const CTs = { SUM:'求和', AVG:'平均值', MAX:'最大值', MIN:'最小值', COUNT:'计数', Y:'按年', Q:'按季', M:'按月', D:'按日', H:'按时' }
let axis_props = null
let add_axis = ((target, axis) => {
	let el = $($('#axis-ietm').html()).appendTo($(target))
	let fName = null
	let fLabel = null
	let fType = null
	let calc = null
	let sort = null
	
	let isNumAxis = $(target).hasClass('J_axis-num')
	// in-load
	if (!!axis.field){
	    let field = $('.fields [data-field="' + axis.field + '"]')
	    fName = axis.field
	    fLabel = field.text()
	    fType = field.data('type')
	    sort = axis.sort
	    calc = axis.calc
	    el.attr({ 'data-label': axis.label, 'data-scale': axis.scale })
	} else {
	    fName = axis.data('field')
	    fLabel = axis.text()
	    fType = axis.data('type')
	    sort = 'NONE'
    	if (isNumAxis) {
    		if (fType == 'text' || fType == 'date') calc = 'COUNT'
    		else calc = 'SUM'
    	} else {
    		if (fType == 'date') calc = 'D'
    	}
	}
	el.attr({ 'data-calc': calc, 'data-sort': sort })
	
	if (isNumAxis) {
        if (fType == 'date' || fType == 'text') el.find('.J_date, .J_num').remove()
        else el.find('.J_date').remove()
    } else {
        if (fType == 'date') el.find('.J_text, .J_num').remove()
        else el.find('.J_text, .J_num, .J_date, .dropdown-divider').remove()
    }
	let aopts = el.find('.dropdown-menu .dropdown-item').click(function(){
		let _this = $(this)
		let calc = _this.data('calc')
		let sort = _this.data('sort')
		if (calc){
			el.find('span').text(fLabel + (' (' + _this.text() + ')'))
			el.attr('data-calc', calc)
			aopts.each(function(){ if (!!$(this).data('calc')) $(this).removeClass('text-primary') })
			_this.addClass('text-primary')
			render_option()
		} else if (sort){
			el.attr('data-sort', sort)
			aopts.each(function(){ if (!!$(this).data('sort')) $(this).removeClass('text-primary') })
            _this.addClass('text-primary')
			render_option()
		} else {
		    let state = { axisEl: el, isNumAxis: isNumAxis, label: el.attr('data-label'), scale: el.attr('data-scale') }
		    console.log(JSON.stringify(state))
		    if (axis_props) axis_props.show(state)
		    else axis_props = renderRbcomp(<DlgAxisProps { ...state }  />)
		}
	})
	if (!!calc) el.find('.dropdown-menu li[data-calc="' + calc + '"]').addClass('text-primary')
	if (!!sort) el.find('.dropdown-menu li[data-sort="' + sort + '"]').addClass('text-primary')
	
	el.attr({ 'data-type': fType, 'data-field': fName })
	el.find('span').text(fLabel + (calc ? (' (' + CTs[calc] + ')') : ''))
	el.find('a.del').click(()=>{
		el.remove()
		render_option()
	})
	render_option()
})

// 图表选项
let render_option = (() => {
	let cts = $('.chart-type>a').removeClass('active')
	let dimsAxis = $('.J_axis-dim .item').length
	let numsAxis = $('.J_axis-num .item').length
	
	cts.each(function(){
	    let _this = $(this)
	    let dims = (_this.data('allow-dims') || '0|0').split('|')
	    let nums = (_this.data('allow-nums') || '0|0').split('|')
	    if (dimsAxis >= ~~dims[0] && dimsAxis <= ~~dims[1] && numsAxis >= ~~nums[0] && numsAxis <= ~~nums[1]) _this.addClass('active')
	})
	
	let select = $('.chart-type>a.select')
	if (!select.hasClass('active')) select.removeClass('select')
	
	select = $('.chart-type>a.select')
	if (select.length == 0) $('.chart-type>a.active').eq(0).addClass('select')
	
	render_preview()
})

// 生成预览
let render_preview_chart = null
let render_preview = (() => {
    $setTimeout(()=>{
        if (!!render_preview_chart){
            ReactDOM.unmountComponentAtNode(document.getElementById('chart-preview'))
            render_preview_chart = null
        }
        
        let cfg = build_config()
        if (!cfg){
            $('#chart-preview').html('<h4 class="chart-undata must-center">当前图表无数据</h4>')
            return
        }
        console.log(JSON.stringify(cfg))
        
        $('#chart-preview').empty()
        let c = detectChart(cfg)
        if (!!c) render_preview_chart = renderRbcomp(c, 'chart-preview')
        else $('#chart-preview').html('<h4 class="chart-undata must-center">不支持的图表类型</h4>')
        
    }, 400, 'chart-preview')
})

let build_config = (() => {
    let cfg = { entity: window.__sourceEntity, title: $val('#chart-title') || '未命名图表' }
    cfg.type = $('.chart-type>a.select').data('type')
    if (!cfg.type) return
    
    let dims = []
    let nums = []
    $('.J_axis-dim>span').each((idx, item) => { dims.push(__build_axisItem(item, false)) })
    $('.J_axis-num>span').each((idx, item) => { nums.push(__build_axisItem(item, true)) })
    if (dims.length == 0 && nums.length == 0) return
    cfg.axis = { dimension: dims, numerical: nums }
    
    return cfg
})
let __build_axisItem = ((item, isNum) => {
    item = $(item)
    let x = { field: item.data('field'), sort: item.attr('data-sort') || '', label: item.attr('data-label') || '' }
    if (isNum){
        x.calc = item.attr('data-calc')
        x.scale = item.attr('data-scale')
    } else if (item.data('type') == 'date'){
        x.calc = item.attr('data-calc')
    }
    return x
})

class DlgAxisProps extends React.Component {
    constructor(props) {
        super(props)
        this.state = { ...props }
        this.changeVal = this.changeVal.bind(this)
    }
    render() {
        return (<RbModal title="显示样式" destroyOnHide={false} ref="dlg">
                <form>
                <div className="form-group row">
                    <label className="col-sm-3 col-form-label text-sm-right">別名</label>
                    <div className="col-sm-7">
                        <input className="form-control form-control-sm" data-id="label" placeholder="默认" value={this.state.label || ''} onChange={this.changeVal} />
                    </div>
                </div>
                {this.state.isNumAxis !== true ? null :
                <div className="form-group row">
                    <label className="col-sm-3 col-form-label text-sm-right">小数位</label>
                    <div className="col-sm-7">
                        <select className="form-control form-control-sm" data-id="scale" value={this.state.scale || 2} onChange={this.changeVal}>
                            <option value="0">0</option>
                            <option value="1">1</option>
                            <option value="2">2</option>
                            <option value="3">3</option>
                            <option value="4">4</option>
                        </select>
                    </div>
                </div>
                }
                <div className="form-group row footer">
                    <div className="col-sm-7 offset-sm-3">
                        <button className="btn btn-primary" type="button" onClick={()=>this.saveProps()}>确定</button>
                    </div>
                </div>
            </form>
            </RbModal>)
    }
    changeVal(e) {
        let id = e.target.dataset.id
        let vvv = {}
        vvv[id] = e.target.value
        this.setState({ ...vvv  })
    }
    saveProps() {
        this.state.axisEl.attr({ 'data-label': this.state['label'], 'data-scale': this.state['scale'] })
        this.refs['dlg'].hide()
        render_preview()
    }
    show(state) {
        this.setState({ ...state }, () => {
            this.refs['dlg'].show()
        })
    }
}