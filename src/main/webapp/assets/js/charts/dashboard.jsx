let gridster = null
$(document).ready(function(){
    $('.J_add-chart').click(function(){
        rb.modal(rb.baseUrl + '/p/dashboard/chart-new', '添加图表')
        //gridster.add_widget.apply(gridster, ['<li>0</li>', 4, 2])
    })
    $(window).trigger('resize')
    
    gridster = $('.gridster ul').gridster({
        widget_base_dimensions: ['auto', 100],
        autogenerate_stylesheet: true,
        min_cols: 1,
        max_cols: 12,
        widget_margins: [5, 5],
        resize: {
            enabled: true,
            min_size: [2, 2]
        },
        draggable: {
            handle: '.chart-title'
        }
    }).data('gridster')
})

$(window).resize(() => {
    $('.chart-grid').height($(window).height() - 131)
})