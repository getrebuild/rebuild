<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>改变部门</title>
<style type="text/css">
</style>
</head>
<body class="dialog">
<div class="main-content">
	<form>
		<div class="form-group row">
			<label class="col-sm-3 col-form-label text-sm-right">选择新部门</label>
			<div class="col-sm-7 col-lg-4">
				<select class="form-control form-control-sm" id="newDept">
				</select>
			</div>
		</div>
		<div class="form-group row footer">
			<div class="col-sm-7 offset-sm-3">
            	<button class="btn btn-primary" type="button" data-loading-text="请稍后">确定</button>
            	<a class="btn btn-link" onclick="parent.RbViewPage.hideModal()">取消</a>
			</div>
		</div>
	</form>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript">
$(document).ready(function(){
	let user = $urlp('user')
	
    let select2 = $('#newDept').select2({
        language: 'zh-CN',
        placeholder: '选择部门',
        width: '100%',
        minimumInputLength: 1,
        maximumSelectionLength: 1,
        allowClear: true,
        ajax: {
            url: rb.baseUrl + '/commons/search',
            delay: 300,
            data: function(params) {
                let query = {
                    entity: 'Department',
                    fields: 'name',
                    q: params.term,
                }
                return query
            },
            processResults: function(data){
                let rs = data.data.map((item) => { return item })
                return { results: rs }
            }
        }
    })
	
	let btn = $('.btn-primary').click(function(){
		let dept = select2.val()
		if (!!!dept){ rb.notice('请选择新部门'); return }
		
		btn.button('loading')
		$.post(rb.baseUrl + '/admin/bizuser/change-dept?dept=' + dept + '&user=' + user, function(res){
			if (res.error_code == 0) parent.location.reload()
			else rb.notice(res.error_msg, 'danger')
			btn.button('reset')
		});
	});
});
</script>
</body>
</html>