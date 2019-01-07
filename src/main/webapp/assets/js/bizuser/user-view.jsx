const user_id = window.__PageConfig.recordId
$(document).ready(function(){
    if (rb.isAdminUser != true || rb.isAdminVerified == false) $('.view-action').remove()

    $('.J_delete').off('click').click(function(){
        rb.alert('<b>暂不支持删除用户</b><br>我们建议你停用用户，而非将其删除', { type: 'warning', html: true, confirmText: '停用', confirm:()=>{ toggleDisabled(true) } })
    })
    $('.J_disable').click(()=>{
        rb.alert('确定要停用此用户吗？', { type: 'warning', confirm:()=>{ toggleDisabled(true) } })
    })
    $('.J_enable').click(()=>{ toggleDisabled(false) })
    
    $('.J_changeRole').click(()=>{ renderRbcomp(<DlgChangeRole user={user_id} />) })
    $('.J_changeDept').click(()=>{ renderRbcomp(<DlgChangeDept user={user_id} />) })
    
    if (rb.isAdminVerified == true){
        $.get(rb.baseUrl + '/admin/bizuser/check-user-status?id=' + user_id, (res) => {
            if (res.data.system == true){
                $('.J_tips').removeClass('hide').find('.message p').text('系统内建用户，不允许修改')
                $('.view-action').remove()
                return
            }

            if (res.data.disabled == true) $('.J_disable').remove()
            else $('.J_enable').remove()

            if (res.data.active == true) return
            let reason = []
            if (!res.data.role) reason.push('未指定角色')
            if (!res.data.dept) reason.push('未指定部门')
            if (res.data.disabled == true) reason.push('已停用')
            $('.J_tips').removeClass('hide').find('.message p').text('当前用户处于未激活状态，因为其 ' + reason.join(' / '))
        })
    }
})

const toggleDisabled = function(disabled){
    let _data = { isDisabled: disabled }
    _data.metadata = { entity: 'User', id: user_id }
    $.post(rb.baseUrl + '/app/entity/record-save', JSON.stringify(_data), function(res){
        if (res.error_code == 0){
            rb.highbar('用户已' + (disabled ? '停用' : '启用'), 'success')
            setTimeout(()=>{ location.reload() }, 500)
        }
    })
}

// 变更部门
class DlgChangeDept extends RbModalHandler {
    constructor(props) {
        super(props)
        this.type = 'Department'
        this.typeName = '部门'
    }
    render(){
        return (<RbModal title={'变更' + this.typeName} ref="dlg" disposeOnHide={true}>
            <form>
                <div className="form-group row">
                    <label className="col-sm-3 col-form-label text-sm-right">选择新{this.typeName}</label>
                    <div className="col-sm-7">
                        <select className="form-control form-control-sm" ref="idNew" />
                    </div>
                 </div>
                <div className="form-group row footer">
                    <div className="col-sm-7 offset-sm-3" ref="btns">
                        <button className="btn btn-primary btn-space" type="button" data-loading-text="请稍后" onClick={()=>this.post()}>确定</button>
                        <a className="btn btn-link btn-space" onClick={()=>this.hide()}>取消</a>
                    </div>
                </div>
            </form>
        </RbModal>)
    }
    componentDidMount() {
        let that = this
        this.__select2 = $(this.refs['idNew']).select2({
            language: 'zh-CN',
            placeholder: '选择' + this.typeName,
            width: '100%',
            allowClear: true,
            minimumInputLength: 1,
            ajax: {
                url: rb.baseUrl + '/app/entity/search',
                delay: 300,
                data: function(params) {
                    return { entity: that.type, q: params.term }
                },
                processResults: function(data){
                    let rs = data.data.map((item) => { return item })
                    return { results: rs }
                }
            }
        })
    }
    post() {
        let dept = this.__select2.val()
        if (!!!dept){ rb.highbar('请选择新部门'); return }
        let btns = $(this.refs['btns']).find('.btn').button('loading')
        $.post(rb.baseUrl + '/admin/bizuser/change-dept?dept=' + dept + '&user=' + this.props.user, (res)=>{
            if (res.error_code == 0) location.reload()
            else rb.hberror(res.error_msg)
            btns.button('reset')
        })
    }
}

// 变更角色
class DlgChangeRole extends DlgChangeDept {
    constructor(props) {
        super(props)
        this.type = 'Role'
        this.typeName = '角色'
    }
    post() {
        let role = this.__select2.val()
        if (!!!role){ rb.highbar('请选择新角色'); return }
        let btns = $(this.refs['btns']).find('.btn').button('loading')
        $.post(rb.baseUrl + '/admin/bizuser/change-role?role=' + role + '&user=' + this.props.user, (res)=>{
            if (res.error_code == 0) location.reload()
            else rb.hberror(res.error_msg)
            btns.button('reset')
        })
    }
}
