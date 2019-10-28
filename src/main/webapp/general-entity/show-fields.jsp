<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>列显示</title>
</head>
<body class="dialog">
<div class="main-content">
	<div class="row m-0">
		<div class="col-6 sortable-swap">
			<h5 class="sortable-box-title">已显示</h5>
			<div class="sortable-box h380 rb-scroller">
				<ol class="dd-list J_config"></ol>
			</div>
			<i class="zmdi zmdi-swap"></i>
		</div>
		<div class="col-6">
			<h5 class="sortable-box-title">未显示</h5>
			<div class="sortable-box h380 rb-scroller">
				<ol class="dd-list unset-list"></ol>
			</div>
		</div>
	</div>
	<div class="dialog-footer">
		<div class="float-left">
			<div id="shareTo" class="shareTo--wrap"></div>
		</div>
		<button class="btn btn-primary J_save" type="button">保存</button>
		<button class="btn btn-secondary" onclick="parent.RbModal.hide()" type="button">取消</button>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/js/sortable.js"></script>
<script src="${baseUrl}/assets/js/settings-share2.jsx" type="text/babel"></script>
<script type="text/babel">
$(document).ready(function () {
	const entity = $urlp('entity')
	const baseUrl = rb.baseUrl + '/app/' + entity + '/list-fields'

	let shareTo
	let cfgid = $urlp('id')
	$.get(baseUrl + '?id=' + cfgid, function (res) {
		let _data = res.data || {}
		$(_data.fieldList).each(function () {
			render_unset([this.field, this.label])
		})
		$(_data.configList).each(function () {
			$('.unset-list li[data-key="' + this.field + '"]').trigger('click')
		})
		cfgid = _data.configId || ''

		if (rb.isAdminUser) {
			$.get(baseUrl + '/alist', (res) => {
				let configName = null
				$(res.data).each(function () {
					if (this[0] === _data.configId) {
						configName = this[1]
						return false
					}
				})
				// eslint-disable-next-line react/jsx-no-undef
				renderRbcomp(<Share2 title="列显示" list={res.data} configName={configName} shareTo={_data.shareTo} entity={entity} id={_data.configId}/>, 'shareTo', function () { shareTo = this })
			})
		}
	})

	$('.J_save').click(function () {
		let config = [];
		$('.J_config>li').each(function () {
			config.push({field: $(this).data('key')})
		});
		if (config.length == 0) {
			RbHighbar.create('请至少设置一个显示列');
			return
		}

		let btn = $(this).button('loading')
		let shareToData = shareTo ? shareTo.getData() : {}
		$.post(baseUrl + '?id=' + cfgid + '&configName=' + $encode(shareToData.configName || '') + '&shareTo=' + shareToData.shareTo, JSON.stringify(config), function (res) {
			if (res.error_code == 0) parent.location.reload()
			btn.button('reset')
		})
	})
})
</script>
</body>
</html>
