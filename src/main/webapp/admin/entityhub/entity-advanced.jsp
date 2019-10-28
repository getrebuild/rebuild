<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>高级配置</title>
<style type="text/css">
a#entityIcon{display:inline-block;width:36px;height:36px;background-color:#e3e3e3;text-align:center;border-radius:2px;}
a#entityIcon .icon{font-size:26px;color:#555;line-height:36px;}
a#entityIcon:hover{opacity:0.8}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-aside rb-color-header">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="实体管理" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="entities" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<aside class="page-aside">
			<div class="rb-scroller-aside rb-scroller">
				<div class="aside-content">
					<div class="content">
						<div class="aside-header">
							<button class="navbar-toggle collapsed" type="button"><span class="icon zmdi zmdi-caret-down"></span></button>
							<span class="title">${entityLabel}</span>
							<p class="description">${comments}</p>
						</div>
					</div>
					<div class="aside-nav collapse">
						<ul class="nav">
							<li><a href="base">基本信息</a></li>
							<li><a href="fields">管理字段</a></li>
							<li><a href="form-design">表单布局</a></li>
							<li class="active"><a href="advanced">高级配置</a></li>
						</ul>
					</div>
				</div>
			</div>
        </aside>
		<div class="page-head">
			<div class="page-head-title">高级配置</div>
		</div>
		<div class="main-content container-fluid pt-1">
			<div class="card">
				<div class="card-header">删除${entityLabel}实体</div>
				<div class="card-body">
					<p><strong>实体删除后将无法恢复，请务必谨慎操作。</strong>删除前，必须将该实体下的记录全部清空。如果这是一个主实体，则需要先将明细实体删除。</p>
					<div>
						<label class="custom-control custom-control-sm custom-checkbox custom-control-inline mb-2">
							<input class="custom-control-input J_drop-check" type="checkbox"><span class="custom-control-label"> 我已知晓风险</span>
						</label>
						<label class="custom-control custom-control-sm custom-checkbox custom-control-inline mb-2 bosskey-show">
							<input class="custom-control-input J_drop-force" type="checkbox"><span class="custom-control-label"> 强制删除</span>
						</label>
					</div>
					<div class="mb-1">
						<button type="button" class="btn btn-danger J_drop-confirm" disabled="disabled" data-loading-text="删除中"><i class="zmdi zmdi-delete icon"></i> 确认删除</button>
						<div class="alert alert-warning alert-icon hide col-sm-6 mb-0">
							<div class="icon"><span class="zmdi zmdi-alert-triangle"></span></div>
							<div class="message">系统内建实体，不允许删除</div>
						</div>
					</div>
				</div>
			</div>
			<div class="card bosskey-show">
				<div class="card-header">导出${entityLabel}实体</div>
				<div class="card-body">
					<p>将实体的元数据导出，方便与其他实例间共享。你也可以将导出文件发布到 <a class="link" href="https://github.com/getrebuild/rebuild-datas" target="_blank">元数据市场</a>。</p>
					<div class="mb-1">
						<a href="../entity-export?id=${entityMetaId}" target="_blank" class="btn btn-primary"><i class="zmdi zmdi-cloud-download icon"></i> 导出</a>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script>
window.__PageConfig = { isSuperAdmin: ${isSuperAdmin} }
</script>
<script type="text/babel">
$(document).ready(function(){
	const metaId = '${entityMetaId}'
	if (!!!metaId){
		$('.J_drop-confirm').next().removeClass('hide')
		$('.J_drop-confirm').remove()
		$('.J_drop-check').parent().parent().remove()
		return
	}
	
	$('.J_drop-check').click(function(){
		$('.J_drop-confirm').attr('disabled', $(this).prop('checked') == false)
	})
	
	let btnDrop = $('.J_drop-confirm').click(() => {
		if ($('.J_drop-check').prop('checked') == false) return
		if (!window.__PageConfig.isSuperAdmin){ RbHighbar.error('仅超级管理员可删除实体'); return }

		let alertExt = { type: 'danger', confirmText: '删除' }
        alertExt.confirm = function() {
            btnDrop.button('loading')
            this.disabled(true)
            $.post('../entity-drop?id=' + metaId + '&force=' + $('.J_drop-force').prop('checked'), (res) => {
                if (res.error_code == 0) {
                    RbHighbar.success('实体已删除')
                    setTimeout(function() { location.replace('../../entities') }, 1500)
                } else RbHighbar.error(res.error_msg)
            })
        }
        alertExt.call = function () { $countdownButton($(this._dlg).find('.btn-danger')) }
		RbAlert.create('实体删除后将无法恢复，请务必谨慎操作。确认删除吗？', '删除实体', alertExt)
	})
})
</script>
</body>
</html>
