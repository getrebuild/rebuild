<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/bootstrap-slider.min.css">
<%@ include file="/_include/Head.jsp"%>
<title>字段信息</title>
<style type="text/css">
.sortable-box{height:268px}
.sortable-box .dd-list{height:260px}
.sortable-box .dd-list .no-item{padding:9px;text-align:center;color:#999}
.dd-item.default .dd3-content{background-color:#5a95f5 !important;border-color:#5a95f5;color:#fff}
.dd-item.default .J_default{display:none !important;}
</style>
</head>
<body>
<div class="rb-wrapper rb-fixed-sidebar rb-collapsible-sidebar rb-collapsible-sidebar-hide-logo rb-aside">
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
							<li><a href="../base"><i class="icon mdi mdi-inbox"></i>基本信息</a></li>
							<li class="active"><a href="../fields"><i class="icon mdi mdi-inbox"></i>管理字段</a></li>
							<li><a href="../form-design"><i class="icon mdi mdi-inbox"></i>配置布局</a></li>
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
					<br>
					<form>
						<div class="form-group row">
							<label class="col-sm-2 col-form-label text-sm-right">字段名称</label>
							<div class="col-lg-4 col-sm-10">
								<input class="form-control form-control-sm" type="text" id="fieldLabel" value="${fieldLabel}" data-o="${fieldLabel}">
							</div>
						</div>
						<div class="form-group row">
							<label class="col-sm-2 col-form-label text-sm-right">内部标识</label>
							<div class="col-lg-4 col-sm-10">
								<input class="form-control form-control-sm" type="text" readonly="readonly" value="${fieldName}">
							</div>
						</div>
						<div class="form-group row">
							<label class="col-sm-2 col-form-label text-sm-right">类型</label>
							<div class="col-lg-4 col-sm-10">
								<input class="form-control form-control-sm" type="text" readonly="readonly" value="${fieldType}">
							</div>
						</div>
						<div class="form-group row J_for-DECIMAL hide">
							<label class="col-sm-2 col-form-label text-sm-right">小数位长度</label>
							<div class="col-lg-4 col-sm-10">
								<select class="form-control form-control-sm" id="decimalPrecision">
									<option value="1">1位</option>
									<option value="2" selected="selected">2位</option>
									<option value="3">3位</option>
									<option value="4">4位</option>
									<option value="5">5位</option>
									<option value="6">6位</option>
								</select>
							</div>
						</div>
						<div class="form-group row J_for-REFERENCE hide">
							<label class="col-sm-2 col-form-label text-sm-right">引用实体</label>
							<div class="col-lg-4 col-sm-10">
								<div class="form-control-plaintext"><a href="../../${fieldRefentity}/base">${fieldRefentityLabel} (${fieldRefentity})</a></div>
							</div>
						</div>
						<div class="form-group row J_for-DATETIME hide">
							<label class="col-sm-2 col-form-label text-sm-right">格式</label>
							<div class="col-lg-4 col-sm-10">
								<select class="form-control form-control-sm" id="dateFormat">
									<option value="yyyy-MM">YYYY-MM</option>
									<option value="yyyy-MM-dd">YYYY-MM-DD</option>
									<option value="yyyy-MM-dd HH:mm" selected="selected">YYYY-MM-DD HH:MM</option>
									<option value="yyyy-MM-dd HH:mm:ss">YYYY-MM-DD HH:MM:SS</option>
								</select>
							</div>
						</div>
						<div class="form-group row J_for-IMAGE J_for-FILE hide">
							<label class="col-sm-2 col-form-label text-sm-right">允许上传数量</label>
							<div class="col-lg-4 col-sm-10" style="padding-top:6px">
								<input class="bslider form-control" id="uploadNumber" type="text" data-slider-value="[1,5]" data-slider-step="1" data-slider-max="10" data-slider-min="0" data-slider-tooltip="show">
								<div class="form-text J_minmax">最少上传 <b>1</b> 个，最多上传 <b>5</b> 个</div>
							</div>
						</div>
						<div class="form-group row J_for-PICKLIST hide">
							<label class="col-sm-2 col-form-label text-sm-right">选项</label>
							<div class="col-lg-4 col-sm-10">
								<div class="rb-scroller sortable-box">
									<ol class="dd-list" id="picklist-items">
										<li class="no-item">加载中</li>
									</ol>
								</div>
								<button type="button" class="btn btn-secondary btn-sm J_picklist-edit" style="line-height:28px"><i class="zmdi zmdi-settings"></i> 添加/编辑选项</button>
							</div>
						</div>
						<div class="form-group row">
							<label class="col-sm-2 col-form-label text-sm-right">备注</label>
							<div class="col-lg-4 col-sm-10">
								<textarea class="form-control form-control-sm row2x" id="comments" data-o="${fieldComments}">${fieldComments}</textarea>
							</div>
						</div>
						<div class="form-group row">
							<div class="col-sm-10 offset-sm-2">
								<label class="custom-control custom-checkbox custom-control-inline" style="margin-bottom:0">
									<input class="custom-control-input" type="checkbox" id="fieldNullable" data-o="${fieldNullable}"><span class="custom-control-label"> 允许空值</span>
								</label>
								<label class="custom-control custom-checkbox custom-control-inline" style="margin-bottom:0">
									<input class="custom-control-input" type="checkbox" id="fieldUpdatable" data-o="${fieldUpdatable}"><span class="custom-control-label"> 允许修改值</span>
								</label>
							</div>
						</div>
						<div class="form-group row footer">
							<div class="col-lg-4 col-sm-10 offset-sm-2">
								<button class="btn btn-primary" type="button">保存</button>
								<div class="alert alert-warning alert-icon hide">
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
<script type="text/plain" id="picklist-temp">
<li class="dd-item dd3-item">
	<div class="dd3-content text-3dot">HOLD</div>
	<div class="dd-handle dd3-handle"></div>
	<div class="dd3-action"><a href="javascript:;" class="J_default" title="设为默认">[默认]</a></div>
</li>
</script>
<%@ include file="/_include/Foot.jsp"%>
<script type="text/javascript" src="${baseUrl}/assets/lib/bootstrap-slider.min.js"></script>
<script type="text/javascript">
$(document).ready(function(){
	const metaId = '${fieldMetaId}';
	let dt = '${fieldType}';
	if (dt.indexOf('(') > -1) dt = dt.match('\\((.+?)\\)')[1];
	const extConfigOld = JSON.parse('${fieldExtConfig}' || '{}');
	
	const btn = $('.btn-primary').click(function(){
		if (!!!metaId) return;
		let label = $val('#fieldLabel'),
			comments = $val('#comments'),
			nullable = $val('#fieldNullable'),
			updatable = $val('#fieldUpdatable');
		let _data = { fieldLabel:label, comments:comments, nullable:nullable, updatable:updatable };
		_data = $cleanMap(_data)
		
		let extConfig = {};
		$('.J_for-' + dt + ' .form-control').each(function(){
			let k = $(this).attr('id');
			let v = $val(this);
			if (extConfigOld[k] != v) extConfig[k] = v;
		});
		
		if (Object.keys(extConfig).length > 0){
			_data['extConfig'] = JSON.stringify(extConfig);
		}
		
		_data = $cleanMap(_data)
		if (Object.keys(_data).length == 0){
			location.reload();
			return;
		}
		
		_data.metadata = { entity:'MetaField', id:metaId||null };
		_data = JSON.stringify(_data);
		console.log(_data);
		btn.button('loading');
		$.post(rb.baseUrl +  '/admin/entity/field-update', _data, function(res){
			if (res.error_code == 0){
				location.reload();
			} else alert(res.error_msg)
		});
	});
	if (!!!metaId){
		btn.next().removeClass('hide');
		btn.remove();
	}
	
	$('#fieldNullable').attr('checked', $('#fieldNullable').data('o') == true)
	$('#fieldUpdatable').attr('checked', $('#fieldUpdatable').data('o') == true)
	
	$('.J_for-' + dt).removeClass('hide');
	
	let uploadNumber = [1, 5];
	for (let k in extConfigOld) {
		if (k == 'uploadNumber'){
			uploadNumber = extConfigOld[k].split(',');
			uploadNumber[0] = ~~uploadNumber[0];
			uploadNumber[1] = ~~uploadNumber[1];
			$('.J_minmax b').eq(0).text(uploadNumber[0])
			$('.J_minmax b').eq(1).text(uploadNumber[1])
		} else $('#' + k).val(extConfigOld[k]);
	}
	$('input.bslider').slider({ value:uploadNumber }).on('change', function(e){
		let v = e.value.newValue;
		$('.J_minmax b').eq(0).text(v[0])
		$('.J_minmax b').eq(1).text(v[1])
	})
	
	if (dt == 'PICKLIST'){
		$.get(rb.baseUrl + '/admin/field/picklist-gets?entity=${entityName}&field=${fieldName}&isAll=false', function(res){
			if (res.data.length == 0){
				$('#picklist-items li').text('请添加选项'); return;
			}
			$('#picklist-items').empty();
			$(res.data).each(function(){
				picklistItemRender(this)
			});
			
			$('.dd-list').sortable({
				cursor: 'move',
				placeholder: 'dd-placeholder',
				axis: 'y',
			}).disableSelection()
		});
		
		picklistModal = null;
		$('.J_picklist-edit').click(function(){
			if (picklistModal) picklistModal.show()
			else picklistModal = rb.modal(rb.baseUrl + '/admin/entity/picklist-config.htm?entity=${entityName}&field=${fieldName}', '配置列表选项')
		})
	}
});
const picklistItemRender = function(data){
	let item = $($('#picklist-temp').html()).appendTo('#picklist-items');
	item.find('.dd3-content').text(data.text)
	item.attr('attr-id', data.id);
	item.find('.dd3-action .J_default').off('click').click(function(){
		$('#picklist-items li').removeClass('default')
		$(this).parent().parent().addClass('default')
	});
	if (data['default'] === true) item.find('.dd3-action .J_default').trigger('click')
};
</script>
</body>
</html>
