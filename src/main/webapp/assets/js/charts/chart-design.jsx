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
			if ($(this).hasClass('J_axis-txt')) return !dragIsNum
			else return true
		},
		drop: function(event, ui){
			add_axis(this, ui)
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
	    console.log(JSON.stringify(cfg))
	}).find('.zmdi').addClass('zmdi-arrow-left')
})

let add_axis = ((target, ui) => {
	let axis = $(ui.draggable[0])
	let el = $($('#axis-ietm').html()).appendTo($(target))
	let ft = axis.data('type')
	let calcType = null
	if ($(target).hasClass('J_axis-num')) {
		if (ft == 'text' || ft == 'date') calcType = '计数'
		else calcType = '求和'
		if (ft == 'date' || ft == 'text') el.find('.J_date, .J_num').remove()
		else el.find('.J_date').remove()
	} else {
		if (ft == 'date'){
			calcType = '按日'
			el.find('.J_text, .J_num').remove()
		} else {
			el.find('.J_text, .J_num, .J_date, .dropdown-divider').remove()
		}
	}
	
	el.find('.dropdown-item').click(function(){
		let that = $(this)
		let calc = that.data('calc')
		let sort = that.data('sort')
		if (calc){
			el.find('span').text(axis.text() + (' (' + that.text() + ')'))
			el.attr('data-calc', calc)
		} else if (sort){
			el.attr('data-sort', sort)
		}
	})
	
	el.attr({ 'data-type': ft, 'data-field': axis.data('field') })
	el.find('span').text(axis.text() + (calcType ? (' (' + calcType + ')') : ''))
	el.find('a.del').click(()=>{
		el.remove()
		render_config()
	})
	render_config()
})

let render_config = (() => {
	$('.chart-type>a').removeClass('active')
	let txtAxis = $('.J_axis-txt .item').length
	let numAxis = $('.J_axis-num .item').length
	
	if (txtAxis > 0 || numAxis > 0) $('.chart-type>a').addClass('active')
})

let build_config = (() => {
    let cfg = { entity: window.__sourceEntity }
    cfg.type = $('.chart-type>a.select').data('type')
    return cfg
})

