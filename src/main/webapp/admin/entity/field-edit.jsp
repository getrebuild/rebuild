<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>字段信息</title>
<style type="text/css">
.card.entity .card-body{padding:14px 20px}
.card.entity .icon{font-size:40px;}
.card.entity h5,.card.entity p{margin:3px 0;}
.card.entity p{color:#777;font-size:0.9rem;}
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
							<li><a href="../base"><i class="icon mdi mdi-inbox"></i>基本信息</a></li>
							<li class="active"><a href="../fields"><i class="icon mdi mdi-inbox"></i>管理字段</a></li>
							<li><a href="../layouts"><i class="icon mdi mdi-inbox"></i>管理布局</a></li>
						</ul>
					</div>
				</div>
			</div>
        </aside>
		<div class="page-head">
			<div class="page-head-title">字段信息</div>
		</div>
		<div class="main-content container-fluid" style="padding-top:3px">
			<div class="card">
				<div class="card-body">
					<form>
						<div class="form-group row">
							<label class="col-12 col-sm-2 col-form-label text-sm-right">字段名称</label>
							<div class="col-12 col-sm-8 col-lg-4">
								<input class="form-control form-control-sm" type="text" id="fieldLabel" value="${fieldLabel}" data-o="${fieldLabel}">
							</div>
						</div>
						<div class="form-group row">
							<label class="col-12 col-sm-2 col-form-label text-sm-right">内部标识</label>
							<div class="col-12 col-sm-8 col-lg-4">
								<input class="form-control form-control-sm" type="text" readonly="readonly" value="${fieldName}">
							</div>
						</div>
						<div class="form-group row">
							<label class="col-12 col-sm-2 col-form-label text-sm-right">类型</label>
							<div class="col-12 col-sm-8 col-lg-4">
								<input class="form-control form-control-sm" type="text" readonly="readonly" value="${fieldType}">
							</div>
						</div>
						<div class="form-group row J_for-DECIMAL hide">
							<label class="col-12 col-sm-2 col-form-label text-sm-right">小数位长度</label>
							<div class="col-12 col-sm-8 col-lg-4">
								<select class="form-control form-control-sm" id="precision" data-o="${fieldPrecision}">
									<option value="1">1位 (1,234.1)</option>
									<option value="2">2位 (1,234.12)</option>
									<option value="3">3位 (1,234.123)</option>
									<option value="4">4位 (1,234.1234)</option>
									<option value="5">5位 (1,234.12345)</option>
									<option value="6">6位 (1,234.123456)</option>
								</select>
							</div>
						</div>
						<div class="form-group row J_for-REFERENCE hide">
							<label class="col-12 col-sm-2 col-form-label text-sm-right">引用实体</label>
							<div class="col-12 col-sm-8 col-lg-4">
								<div class="form-control-plaintext"><a href="../../${fieldRefentity}/base">${fieldRefentityLabel} (${fieldRefentity})</a></div>
							</div>
						</div>
						
						<div class="form-group row">
							<label class="col-12 col-sm-2 col-form-label text-sm-right">备注</label>
							<div class="col-12 col-sm-8 col-lg-4">
								<textarea class="form-control form-control-sm row2" id="comments" data-o="${fieldComments}">${fieldComments}</textarea>
							</div>
						</div>
						<div class="form-group row footer">
							<label class="col-12 col-sm-2 col-form-label text-sm-right"></label>
							<div class="col-12 col-sm-8 col-lg-4">
								<button class="btn btn-primary" type="button">更新</button>
								<div class="alert alert-warning alert-icon" style="display:none">
									<div class="icon"><span class="zmdi zmdi-alert-triangle"></span></div>
									<div class="message">系统内建字段，不允许修改</div>
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
<script type="text/javascript">
$(document).ready(function(){
	const metaId = '${fieldMetaId}';
	let dt = '${fieldType}';
	if (dt.indexOf('(') > -1) dt = dt.match('\\((.+?)\\)')[1];
	
	const btn = $('.btn-primary').click(function(){
		if (!!!metaId) return;
		let label = $val('#fieldLabel');
		let comments = $val('#comments');
		let _data = { fieldLabel:label, comments:comments };
		
		$('.J_for-' + dt + ' .form-control').each(function(){
			let id = $(this).attr('id');
			_data[id] = $val(this);
		});
		
		if (JSON.stringify(_data) == '{}'){
			location.reload();
			return;
		}
		
		_data.metadata = { entity:'MetaField', id:metaId||null };
		_data = JSON.stringify(_data);
		console.log(_data);
		btn.button('loading');
		$.post('../../field-update', _data, function(res){
			if (res.error_code == 0) location.reload();
			else alert(res.error_msg)
		});
	});
	
	if (!!!metaId){
		btn.next().show();
		btn.remove();
	}
	
	$('.J_for-' + dt).removeClass('hide');
	$('#precision').val($('#precision').data('o'));
	
});
</script>
</body>
</html>
