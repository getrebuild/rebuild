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
})

var add_axis = ((target, ui) => {
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

var render_config = (() => {
	$('.chart-type>a').removeClass('active')
	let txtAxis = $('.J_axis-txt .item').length
	let numAxis = $('.J_axis-num .item').length
	
	if (numAxis == 1 || numAxis == 2) $('.J_ct-INDEX').addClass('active')
	if (numAxis >= 1) $('.J_ct-TABLE, .J_ct-LINE, .J_ct-BAR, .J_ct-PIE, .J_ct-FUNNEL').addClass('active')
	
	if (txtAxis <= 1) $('.J_ct-LINE, .J_ct-BAR').addClass('active')
	else $('.J_ct-LINE, .J_ct-BAR').removeClass('active')
	
})