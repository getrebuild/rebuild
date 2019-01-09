$(document).ready(()=>{
    init_upload()
    
    $('.steps li:eq(0)').click(step_upload)
    $('.steps li:eq(1)').click(step_preview)
    $('.steps li:eq(2)').click(step_imports)
})

let upload_file = '213923818__dataimports-test.csv'
const init_upload = ()=>{
    $('#upload-input').html5Uploader({
        postUrl: rb.baseUrl + '/filex/upload',
        onClientLoad: function(e, file){
            console.log(file.size)
            if (!(file.type.contains('excel'))){
                rb.highbar('请上传 Excel/CSV 文件')
                return false
            } else if (file.size > 1024 * 1024 * 20){
                rb.highbar('文件大小不能超过 20M')
                return false
            }
        },
        onSuccess: function(d){
            d = JSON.parse(d.currentTarget.response)
            if (d.error_code == 0){
                upload_file = d.data
                step_preview()
            } else rb.hberror('上传失败，请稍后重试')
        }
    })
}

let step_imports_in = false
const step_upload = () =>{
    //if (step_imports_in == true) return
    $('.steps li, .step-content .step-pane').removeClass('active complete')
    $('.steps li[data-step=1], .step-content .step-pane[data-step=1]').addClass('active')
}
const step_preview = () =>{
    //if (step_imports_in == true) return
    //if (!upload_file){ rb.highbar('请先上传文件'); return }
    $('.steps li, .step-content .step-pane').removeClass('active complete')
    $('.steps li[data-step=1]').addClass('complete')
    $('.steps li[data-step=2], .step-content .step-pane[data-step=2]').addClass('active')
    
    $.get(rb.baseUrl + '/admin/datas/imports-preview?file=' + $encode(upload_file), (res)=>{
    })
}
const step_imports = () =>{
    //if (step_imports_in == true) return
    $('.steps li, .step-content .step-pane').removeClass('active complete')
    $('.steps li[data-step=1], .steps li[data-step=2]').addClass('complete')
    $('.steps li[data-step=3], .step-content .step-pane[data-step=3]').addClass('active')
    
    alert(upload_file)
}