let clickIcon = function(icon){
	$('#entityIcon').attr('value', icon).find('i').attr('class', 'icon zmdi zmdi-' + icon)
	rb.modalHide()
}

const wpc = window.__PageConfig
$(document).ready(function(){
	if (!!!wpc.metaId) $('.footer .alert').removeClass('hide')
	else $('.footer .J_action').removeClass('hide')

	$('.J_tab-' + wpc.entity + ' a').addClass('active')
	
	let _btn = $('.J_save').click(function(){
		if (!!!wpc.metaId) return
		let icon = $val('#entityIcon'),
			label = $val('#entityLabel'),
			comments = $val('#comments'),
			nameField = $val('#nameField')
		let _data = { entityLabel:label, comments:comments, nameField:nameField }
		if (!!icon) _data.icon = icon
		_data = $cleanMap(_data)
		if (Object.keys(_data) == 0){ location.reload(); return }
		
		_data.metadata = { entity: 'MetaEntity', id: wpc.metaId }
		_btn.button('loading')
		$.post('../entity-update', JSON.stringify(_data), function(res){
			if (res.error_code == 0) location.reload()
			else rb.notice(res.error_msg, 'danger')
		})
	})
	
	$('#entityIcon').click(function(){
		rb.modal(rb.baseUrl + '/p/commons/search-icon', '选择图标')
	})
	
	$.get(rb.baseUrl + '/commons/metadata/fields?entity=' + wpc.entity, function(d){
		let rs = d.data.map((item) => {
			let unName = item.type == 'REFERENCE' || item.type == 'NTEXT'
			return {
				id: item.name,
				text: item.label,
				disabled: unName,
				title: unName ? '此字段（类型）不能作为主显字段' : ''
			}
		})
		$('#nameField').select2({
			language: 'zh-CN',
			placeholder: '选择字段',
			data: rs
		}).val(wpc.nameField).trigger('change')
	})
})