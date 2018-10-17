var opType = opType || ['assign', '分派']
$(document).ready(function(){
	let selected = parent.RbListPage._RbList.getSelectedRows()
	const ids = selected.map((item) => { return item[0] })
	const entity = $urlp('entity')
	
	$('#records').text('选中的记录 (' + ids.length + '条)')
	
	$('#toUser').select2({
		language: 'zh-CN',
		placeholder: '选择用户',
		minimumInputLength: 1,
		ajax: {
			url: rb.baseUrl + '/commons/search',
			delay: 300,
			data: function(params) {
				let query = {
					entity: 'User',
					qfields: 'loginName,fullName,email',
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
	
	$('.J_click-cass a').click(function(){
		$('.J_click-cass').remove();
		$('.J_cass').removeClass('hide');
		parent.RbListPage._ModalShare.resize()
		
		$.get(rb.baseUrl + '/commons/metadata/references?entity=' + entity, function(res){
			$(res.data).each(function(){
				$('<option value="' + this[0] + '">' + this[1] + '</option>').appendTo('#cascades')
			})
			$('#cascades').select2({
	            language: 'zh-CN',
	            placeholder: '选择关联实体 (可选)',
	        })
		})
	});
	
	$('.J_submit').click(function() {
		let to = $('#toUser').val()
		if (!to || to.length < 1){ rb.notice('请选择' + opType[1] + '给谁'); return }
		let cascades = $('#cascades').val()
		
		let url = rb.baseUrl + '/app/entity/record-' + opType[0] + '?id=' + ids.join(',') + '&cascades=' + cascades.join(',') + '&to=' + to.join(',')
		$.post(url, function(res) {
		    if (res.error_code == 0){
		        rb.notice('已成功' + opType[1] + ' ' + (res.data.assigned || res.data.shared) + ' 条记录', 'success')
		        if (parent.RbListPage) {
		            if (parent.RbListPage._RbList) parent.RbListPage._RbList.reload()
		            $('.J_cancel').trigger('click')
		        }
            } else {
                rb.notice(res.error_msg || (opType[1] + '失败，请稍后重试'), 'danger')
            }
		})
	})
	
	$('.J_cancel').click(function(){
	    if (parent.RbListPage) {
	    	if (parent.RbListPage._ModalAssign) parent.RbListPage._ModalAssign.hide()
	        if (parent.RbListPage._ModalShare) parent.RbListPage._ModalShare.hide()
	    }
	})
});