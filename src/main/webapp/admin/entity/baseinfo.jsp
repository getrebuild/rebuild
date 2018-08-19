<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>实体管理</title>
<style type="text/css">
a#entityIcon{display:inline-block;width:36px;height:36px;background-color:rgb(213, 216, 222);text-align:center;border-radius:2px;}
a#entityIcon .icon{font-size:26px;color:#555;line-height:36px;}
a#entityIcon:hover{opacity:0.8}
</style>
</head>
<body>
<div class="rb-wrapper rb-collapsible-sidebar rb-fixed-sidebar rb-aside">
	<jsp:include page="/_include/NavTop.jsp">
		<jsp:param value="实体管理" name="pageTitle"/>
	</jsp:include>
	<jsp:include page="/_include/NavLeftAdmin.jsp">
		<jsp:param value="entity-list" name="activeNav"/>
	</jsp:include>
	<div class="rb-content">
		<aside class="page-aside">
			<div class="rb-scroller">
				<div class="aside-content">
					<div class="content">
						<div class="aside-header">
							<span class="title">${entityLabel}</span>
							<p class="description">${comments}</p>
						</div>
					</div>
					<div class="aside-nav collapse">
						<ul class="nav">
							<li class="active"><a href="base"><i class="icon mdi mdi-inbox"></i>基本信息</a></li>
							<li><a href="fields"><i class="icon mdi mdi-inbox"></i>管理字段</a></li>
							<li><a href="layouts"><i class="icon mdi mdi-inbox"></i>管理布局</a></li>
						</ul>
					</div>
				</div>
			</div>
        </aside>
		<div class="page-head">
			<div class="page-head-title">基本信息</div>
		</div>
		<div class="main-content container-fluid" style="padding-top:3px">
			<div class="card">
				<div class="card-body">
					<form>
						<div class="form-group row">
							<label class="col-12 col-sm-2 col-form-label text-sm-right">图标</label>
							<div class="col-12 col-sm-8 col-lg-4">
								<a id="entityIcon" data-o="${icon}" title="更换图标" onclick="rbModal.show('../../search-icon.htm')"><i class="icon zmdi zmdi-${icon}"></i></a>
							</div>
						</div>
						<div class="form-group row">
							<label class="col-12 col-sm-2 col-form-label text-sm-right">实体名称</label>
							<div class="col-12 col-sm-8 col-lg-4">
								<input class="form-control form-control-sm" type="text" id="entityLabel" value="${entityLabel}" data-o="${entityLabel}">
							</div>
						</div>
						<div class="form-group row">
							<label class="col-12 col-sm-2 col-form-label text-sm-right">内部标识</label>
							<div class="col-12 col-sm-8 col-lg-4">
								<input class="form-control form-control-sm" type="text" readonly="readonly" id="entityName" value="${entityName}">
							</div>
						</div>
						<div class="form-group row">
							<label class="col-12 col-sm-2 col-form-label text-sm-right">备注</label>
							<div class="col-12 col-sm-8 col-lg-4">
								<textarea class="form-control form-control-sm row2" id="comments" data-o="${comments}">${comments}</textarea>
							</div>
						</div>
						<div class="form-group row footer">
							<label class="col-12 col-sm-2 col-form-label text-sm-right"></label>
							<div class="col-12 col-sm-8 col-lg-4">
								<button class="btn btn-primary" type="button">确定</button>
								<div class="alert alert-warning alert-icon" style="display:none">
									<div class="icon"><span class="zmdi zmdi-alert-triangle"></span></div>
									<div class="message">系统内建实体，不允许修改</div>
								</div>
							</div>
						</div>
					</form>
				</div>
			</div>
		</div>
	</div>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/babel">
const rbModal = ReactDOM.render(<RbModal title="选择图标" />, $('<div id="react-comps"></div>').appendTo(document.body)[0]);
</script>
<script type="text/javascript">
icon_call = function(icon){
	$('#entityIcon').attr('data-val', icon)
			.find('i').attr('class', 'icon zmdi zmdi-' + icon);
	rbModal.hide();
};
$(document).ready(function(){
	const metaId = '${entityMetaId}';
	const btn = $('.btn-primary').click(function(){
		if (!!!metaId) return;
		let icon = $val('#entityIcon');
		let label = $val('#entityLabel');
		let comments = $val('#comments');
		let _data = { icon:icon, label:label, comments:comments };
		if (JSON.stringify(_data) == '{}'){
			location.reload();
			return;
		}
		
		_data.metadata = { entity:'MetaEntity', id:metaId||null };
		_data = JSON.stringify(_data);
		console.log(_data);
		btn.button('loading');
		$.post('../entity-update', _data, function(res){
			if (res.error_code == 0) location.reload();
			else alert(res.error_msg)
		});
	});
	
	if (!!!metaId){
		btn.next().show();
		btn.remove();
	}
});
</script>
</body>
</html>
