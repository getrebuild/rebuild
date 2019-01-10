let upload_file = '204648874__dataimports-test.csv'
let to_entity
let fields_cached
let repeat_opt = 3
let repeat_fields
let owning_user
$(document).ready(()=>{
    init_upload()
    
    let fileds_render = (entity)=>{
        if (!!!entity) return
        let el = $('#repeatFields').empty()
        $.get(rb.baseUrl + '/admin//datas/imports-fields?entity=' + entity, (res)=>{
            $(res.data).each(function(){
                if (this.name == 'createdBy' || this.name == 'createdOn' || this.name == 'modifiedOn' || this.name == 'modifiedBy') return
                $('<option value="' + this.name + '">' + this.label + '</option>').appendTo(el)
            })
            el.select2({
                maximumSelectionLength: 3,
                placeholder: '选择判断字段'
            }).on('change', function(){
                repeat_fields = $(this).val()
            })
            
            fields_cached = res.data
            to_entity = entity
        })
    }
    $.get(rb.baseUrl + '/commons/metadata/entities', (res)=>{
        $(res.data).each(function(){
            $('<option value="' + this.name + '">' + this.label + '</option>').appendTo('#toEntity')
        })
        $('#toEntity').select2({
            allowClear: false
        }).on('change', function(){
            fileds_render($(this).val())
        }).trigger('change')
    })
    
    $('input[name=repeatOpt]').click(function(){
        repeat_opt = $(this).val()
        if (repeat_opt == 3) $('.J_repeatFields').hide()
        else $('.J_repeatFields').show()
    })
    
    $('.J_step1-btn').click(step_mapping)
    $('.J_step2-btn').click(step_imports)
    $('.J_step2-return').click(step_upload)
    
})

const init_upload = ()=>{
    $('#upload-input').html5Uploader({
        postUrl: rb.baseUrl + '/filex/upload',
        onSelectError: function(field, error){
            if (error == 'ErrorType') rb.highbar('请上传 Excel/CSV 文件')
            else if (error == 'ErrorMaxSize') rb.highbar('文件不能大于 20M')
        },
        onSuccess: function(d){
            d = JSON.parse(d.currentTarget.response)
            if (d.error_code == 0){
                upload_file = d.data
                $('.J_upload-input').text($fileCutName(upload_file))
            } else rb.hberror('上传失败，请稍后重试')
        }
    })
}

const step_upload = () =>{
    $('.steps li, .step-content .step-pane').removeClass('active complete')
    $('.steps li[data-step=1], .step-content .step-pane[data-step=1]').addClass('active')
}
const step_mapping = () =>{
    if (!to_entity){ rb.highbar('请选择导入实体'); return }
    if (!upload_file){ rb.highbar('请上传数据文件'); return }
    if (repeat_opt != 3 && (!repeat_fields || repeat_fields.length == 0)){ rb.highbar('请指定重复判断字段'); return }
    
    $.get(rb.baseUrl + '/admin/datas/imports-preview?file=' + $encode(upload_file), (res)=>{
        if (res.error_code > 0) { rb.highbar(res.error_msg); return }
        let _data = res.data
        if (_data.rows_count < 2 || _data.rows_preview[0].length == 0) { rb.highbar('上传的文件无有效数据'); return }
        
        render_fieldsMapping(_data.rows_preview[0], fields_cached)
        $('.steps li, .step-content .step-pane').removeClass('active complete')
        $('.steps li[data-step=1]').addClass('complete')
        $('.steps li[data-step=2], .step-content .step-pane[data-step=2]').addClass('active')
    })
}
const step_imports = () =>{
    let fields_mapping = {}
    $('#fieldsMapping tbody>tr').each(function(){
        let _this = $(this)
        let col = _this.data('col')
        let field = _this.find('select').val()
        if (!!field) fields_mapping[field] = col
    })
    $(fields_cached).each((idx, item)=>{
        if (item.isNullable == true || !!item.defaultValue){
            // Not be must
        }else if (fields_mapping[item.name] == undefined){
            rb.highbar(item.label + ' 为必填字段，请选择')
            fields_mapping = null
            return false
        }
    })
    if (!!!fields_mapping) return
    
    let data = {
        file: upload_file, entity: to_entity, 
        repeat_opt: repeat_opt, repeat_fields: repeat_fields, owning_user: owning_user,
        fields_mapping: fields_mapping
    }
    console.log(JSON.stringify(data))
    return
    
    $('.steps li, .step-content .step-pane').removeClass('active complete')
    $('.steps li[data-step=1], .steps li[data-step=2]').addClass('complete')
    $('.steps li[data-step=3], .step-content .step-pane[data-step=3]').addClass('active')
}

const render_fieldsMapping = (columns, fields) =>{
    let fields_map = {}
    let fields_select = $('<select><option value="">无</option></select>')
    $(fields).each((idx, item)=>{
        let canNull = item.isNullable == false ? ' [必填]' : ''
        if (!!item.defaultValue) canNull = ''
        $('<option value="' + item.name + '">' + item.label + canNull + '</option>').appendTo(fields_select)
        fields_map[item.name] = item
    })
    
    let tbody = $('#fieldsMapping tbody').empty()
    $(columns).each(function(idx, item){
        let tr = $('<tr data-col="' + idx + '"></tr>').appendTo(tbody)
        $('<td><em>#' + (idx + 1) + '</em> ' + item + '<i class="zmdi zmdi-arrow-right"></i></td>').appendTo(tr)
        let td = $('<td></td>').appendTo(tr)
        fields_select.clone().appendTo(td)
        $('<td class="pl-3"></td>').appendTo(tr)
    })
    $('#fieldsMapping tbody select').select2({
        placeholder: '无'
    }).on('change', function(){
        let val = $(this).val()
        let toel = $(this).parents('td').next()
        if (val){
            toel.parent().addClass('table-active')
            let meta = fields_map[val]
            if (!!meta.defaultValue) toel.text('默认 : ' + meta.defaultValue)
            else toel.text('')
        } else {
            toel.parent().removeClass('table-active')
            toel.text('')
        }
    })
}