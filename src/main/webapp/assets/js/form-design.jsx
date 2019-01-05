const wpc = window.__PageConfig
$(document).ready(function(){
    $.get('../list-field?entity=' + wpc.entityName, function(res){
        let validFields = {}, configFields = []
        $(wpc.formConfig.elements).each(function(){ configFields.push(this.field) })
        $(res.data).each(function(){
            validFields[this.fieldName] = this
            if (configFields.contains(this.fieldName) == false) render_unset(this, '.field-list')
        })
        
        $(wpc.formConfig.elements).each(function(){
            let field = validFields[this.field]
            if (this.field == '$DIVIDER$'){
                render_item({ fieldName: this.field, fieldLabel: this.label || '分栏', isFull: true }, '.form-preview')
            } else if (!!!field){
                let item = $('<div class="dd-item"><div class="dd-handle J_field J_missed"><span class="text-danger">[' + this.field + '] 字段已被删除</span></div></div>').appendTo('.form-preview')
                let action = $('<div class="dd-action"><a>[移除]</a></div>').appendTo(item.find('.dd-handle'))
                action.find('a').click(function(){
                    item.remove()
                    check_empty()
                })
            } else {
                render_item({ ...field, isFull: this.isFull || false }, '.form-preview')
            }
        })
        
        check_empty()
        $('.form-preview').sortable({
            cursor: 'move',
            placeholder: 'dd-placeholder',
            cancel: '.nodata',
            stop: check_empty
        }).disableSelection()
    });
    
    $('.J_add-divider').click(function(){
        render_item({ fieldName: '$DIVIDER$', fieldLabel: '分栏', isFull: true }, '.form-preview')
    });
    
    let btn = $('.J_save').click(function(){
        let elements = []
        $('.form-preview .J_field').each(function(){
            let _this = $(this)
            if (!!!_this.data('field')) return
            let item = { field: _this.data('field'), isFull: _this.parent().hasClass('w-100') }
            if (item.field == '$DIVIDER$') item.label = _this.find('span').text()
            elements.push(item)
        })
        if (elements.length == 0) { rb.highbar('请至少布局1个字段'); return }
        
        let _data = { belongEntity: wpc.entityName, type: 'FORM', config: JSON.stringify(elements) }
        _data.metadata = { entity:'LayoutConfig', id: wpc.formConfig.id || null }
        
        btn.button('loading')
        $.post('form-update', JSON.stringify(_data), function(res){
            if (res.error_code == 0) location.reload();
            else rb.hberror(res.error_msg)
        });
    });
});
const render_item = function(data) {
    let item = $('<div class="dd-item"></div>').appendTo('.form-preview')
    if (data.isFull == true) item.addClass('w-100')
    
    let handle = $('<div class="dd-handle J_field" data-field="' + data.fieldName + '"><span>' + data.fieldLabel + '</span></div>').appendTo(item)
    if (data.creatable == false) handle.addClass('readonly')
    else if (data.nullable == false) handle.addClass('not-nullable')
    
    let action = $('<div class="dd-action"></div>').appendTo(handle)
    if (data.displayType){
        $('<span class="ft">' + data.displayType.split('(')[0].trim() + '</span>').appendTo(item)
        $('<a class="rowspan">[双列]</a>').appendTo(action).click(function(){ item.removeClass('w-100') })
        $('<a class="rowspan">[单列]</a>').appendTo(action).click(function(){ item.addClass('w-100') })
        $('<a>[移除]</a>').appendTo(action).click(function(){
            render_unset(data)
            item.remove()
            check_empty()
        })
    }
    
    if (data.fieldName == '$DIVIDER$'){
        item.addClass('divider')
        $('<a title="修改分栏名称">[修改]</a>').appendTo(action).click(function(){ modify_divider(handle) })
        $('<a>[移除]</a>').appendTo(action).click(function(){
            item.remove()
            check_empty()
        })
    }
}
const render_unset = function(data){
    let item = $('<li class="dd-item"><div class="dd-handle">' + data.fieldLabel + '</div></li>').appendTo('.field-list')
    $('<span class="ft">' + data.displayType.split('(')[0].trim() + '</span>').appendTo(item)
    if (data.creatable == false) item.find('.dd-handle').addClass('readonly')
    else if (data.nullable == false) item.find('.dd-handle').addClass('not-nullable')
    item.click(function() {
        render_item(data)
        item.remove()
        check_empty()
    })
    return item
}
const check_empty = function(){
    if ($('.field-list .dd-item').length == 0) $('.field-list .nodata').show()
    else $('.field-list .nodata').hide()
    if ($('.form-preview .dd-item').length == 0) $('.form-preview .nodata').show()
    else $('.form-preview .nodata').hide()
}
const modify_divider = function(handle){
    let input = '<div class="divider-name"><input type="text" class="form-control form-control-sm" placeholder="输入分栏名称"/></div>'
    rb.alert(input, '修改分栏名称', { html: true, confirm: function(){
        this.hide()
        let name = $(this.refs['rbalert']).find('input').val() || '分栏';
        if (name) handle.find('span').text(name)
    } })
}