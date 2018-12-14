$(document).ready(() => {
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
	
	let ctas = $('.chart-type > a').click(function(){
		let that = $(this)
		if (that.hasClass('active') == false) return
		ctas.removeClass('select')
		that.addClass('select')
	})
	
	$('.rb-toggle-left-sidebar').attr('title', '保存并返回').off('click').on('click', () => {
	    let cfg = build_config()
	    if (!!!cfg) return
	    let _data = { config: JSON.stringify(cfg), title: cfg.title, belongEntity: cfg.entity }
	    _data.metadata = { entity: 'ChartConfig', id: window.__chartId }
	    
	    console.log(JSON.stringify(_data))
        $.post(rb.baseUrl + '/app/entity/record-save', JSON.stringify(_data), function(res){
            if (res.error_code == 0){
                window.__chartId = res.data.id
            }
        })
	    
	}).find('.zmdi').addClass('zmdi-arrow-left')
	
	if (window.__chartConfig) {
	    let _axis = window.__chartConfig.axis
	    $(_axis.dimension).each((idx, item) => {
	        add_axis('.J_axis-dim', item)
	    })
	    $(_axis.numerical).each((idx, item) => {
	        add_axis('.J_axis-num', item)
	    })
	}
})

const CTs = { SUM:'求和', AVG:'平均值', MAX:'最大值', MIN:'最小值', COUNT:'计数', Y:'按年', Q:'按季', M:'按月', D:'按日', H:'按时' }
let add_axis = ((target, axis) => {
	let el = $($('#axis-ietm').html()).appendTo($(target))
	let fName = null
	let fLabel = null
	let fType = null
	let ct = null  // calc-type
	
	let isNum = $(target).hasClass('J_axis-num')
	
	if (!!axis.field){
	    let field = $('.fields [data-field="' + axis.field + '"]')
	    fName = axis.field
	    fLabel = field.text()
	    fType = field.data('type')
	    ct = CTs[axis.calc]
	    el.attr({ 'data-calc': axis.calc, 'data-sort': axis.sort })
	} else{
	    fName = axis.data('field')
	    fLabel = axis.text()
	    fType = axis.data('type')
	    
    	if (isNum) {
    		if (fType == 'text' || fType == 'date') ct = '计数'
    		else ct = '求和'
    	} else {
    		if (fType == 'date') ct = '按日'
    	}
	}
	
	if (isNum) {
        if (fType == 'date' || fType == 'text') el.find('.J_date, .J_num').remove()
        else el.find('.J_date').remove()
    } else {
        if (fType == 'date') el.find('.J_text, .J_num').remove()
        else el.find('.J_text, .J_num, .J_date, .dropdown-divider').remove()
    }
	
	el.find('.dropdown-item').click(function(){
		let that = $(this)
		let calc = that.data('calc')
		let sort = that.data('sort')
		if (calc){
			el.find('span').text(fLabel + (' (' + that.text() + ')'))
			el.attr('data-calc', calc)
		} else if (sort){
			el.attr('data-sort', sort)
		}
	})
	
	el.attr({ 'data-type': fType, 'data-field': fName })
	el.find('span').text(fLabel + (ct ? (' (' + ct + ')') : ''))
	el.find('a.del').click(()=>{
		el.remove()
		render_option()
	})
	render_option()
})

let render_option = (() => {
	$('.chart-type>a').removeClass('active')
	let txtAxis = $('.J_axis-dim .item').length
	let numAxis = $('.J_axis-num .item').length
	
	if (txtAxis > 0 || numAxis > 0) $('.chart-type>a').addClass('active')
	
	let select = $('.chart-type>a.select')
	if (!select.hasClass('active')) select.removeClass('select')
	$('.chart-type>a.active').eq(0).addClass('active')
})

let render_preview = (() => {
    
})

let build_config = (() => {
    let cfg = { entity: window.__sourceEntity }
    cfg.type = $('.chart-type>a.select').data('type')
    cfg.title = $val('#chart-title') || '未命名图表'
    
    let dimension = []
    let numerical = []
    $('.J_axis-dim>span').each((idx, item) => {
        dimension.push(build_axis_item(item, false))
    })
    $('.J_axis-num>span').each((idx, item) => {
        numerical.push(build_axis_item(item, true))
    })
    cfg.axis = { dimension: dimension, numerical: numerical }
    
    return cfg
})

let build_axis_item = ((item, isNum) => {
    item = $(item)
    let x = { field: item.data('field'), sort: item.attr('data-sort') || '' }
    if (isNum){
        x.calc = item.attr('data-calc') || 'SUM'
    } else if (item.data('type') == 'date'){
        x.calc = item.attr('data-calc') || 'D'
    }
    return x
})