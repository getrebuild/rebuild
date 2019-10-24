<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<link rel="stylesheet" type="text/css" href="${baseUrl}/assets/lib/widget/bootstrap-slider.min.css">
<%@ include file="/_include/Head.jsp"%>
<title>字段信息</title>
<style type="text/css">
.sortable-box {
	height: 208px;
	max-width: 300px;
}
.sortable-box .dd-list {
	height: 200px
}
.sortable-box .dd-list .dd-item, .sortable-box .dd-list .dd-handle {
	background-color: #fff !important;
	color: #404040 !important;
	border-color: #dedede !important;
}
.sortable-box .no-item {
	padding: 9px;
	text-align: center;
	color: #999
}
.sortable-box.autoh, .sortable-box.autoh .dd-list {
	height: auto;
}
.sortable-box .default .dd-handle {
	background-color: #dedede !important
}
.sortable-box .default .dd-handle::before {
	color: #4285f4;
}
.dt-MULTISELECT .dd-handle, .dt-PICKLIST .dd-handle {
	padding-left: 31px !important;
}
.dt-MULTISELECT .dd-handle::before, .dt-PICKLIST .dd-handle::before {
	position: absolute;
	font-family: 'Material-Design-Iconic-Font';
	font-size: 20px;
	color: #bbb;
	left: 10px;
	content: '\f279';
}
.dt-MULTISELECT .default .dd-handle::before {
	content: '\f26a';
}
.dt-PICKLIST .dd-handle::before {
	content: '\f273';
	font-size: 17px;
	font-weight: bold;
}
.dt-PICKLIST .default .dd-handle::before {
	content: '\f26b';
}
.form-text.help code {
	cursor: help;
	font-weight: bold;
}
.calc-expr {
	border-radius: 3px;
	padding: 10px 0;
	background-color: #eee
}
.calc-expr .col-4 {
	padding-left: 10px;
	padding-right: 10px;
}
#stateClass:empty::before {
	content: '无效状态类';
	color: #ea4335;
}
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
							<li><a href="../base">基本信息</a></li>
							<li class="active"><a href="../fields">管理字段</a></li>
							<li><a href="../form-design">表单布局</a></li>
							<li><a href="../advanced">高级配置</a></li>
						</ul>
					</div>
				</div>
			</div>
        </aside>
		<div class="page-head">
			<div class="page-head-title">字段信息</div>
		</div>
		<div class="main-content container-fluid pt-1">
			<c:if test="${fieldType == 'REFERENCE' && fieldBuildin != true}">
			<ul class="nav nav-tabs nav-tabs-classic">
				<li class="nav-item"><a href="./${fieldName}" class="nav-link active">字段信息</a></li>
				<li class="nav-item"><a href="./${fieldName}/auto-fillin" class="nav-link">表单回填配置</a></li>
			</ul>
			</c:if>
			<div class="card mb-0">
				<div class="card-body pt-4">
					<form class="simple">
						<div class="form-group row">
							<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">字段名称</label>
							<div class="col-md-12 col-xl-6 col-lg-8">
								<input class="form-control form-control-sm" type="text" id="fieldLabel" value="${fieldLabel}" data-o="${fieldLabel}">
							</div>
						</div>
						<div class="form-group row">
							<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">内部标识</label>
							<div class="col-md-12 col-xl-6 col-lg-8">
								<input class="form-control form-control-sm" type="text" readonly="readonly" value="${fieldName}">
							</div>
						</div>
						<div class="form-group row">
							<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">类型</label>
							<div class="col-md-12 col-xl-6 col-lg-8">
								<input class="form-control form-control-sm" type="text" readonly="readonly" value="${fieldTypeLabel}">
							</div>
						</div>
						<div class="form-group row J_for-DECIMAL hide">
							<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">小数位长度</label>
							<div class="col-md-12 col-xl-6 col-lg-8">
								<select class="form-control form-control-sm" id="decimalFormat">
									<option value="##,##0.0">1位</option>
									<option value="##,##0.00" selected="selected">2位</option>
									<option value="##,##0.000">3位</option>
									<option value="##,##0.0000">4位</option>
									<option value="##,##0.00000">5位</option>
									<option value="##,##0.000000">6位</option>
								</select>
							</div>
						</div>
						<div class="form-group row J_for-REFERENCE hide pt-0 pb-0">
							<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">引用实体</label>
							<div class="col-md-12 col-xl-6 col-lg-8">
								<div class="form-control-plaintext"><a href="../../${fieldRefentity}/base">${fieldRefentityLabel} (${fieldRefentity})</a></div>
							</div>
						</div>
						<div class="form-group row J_for-DATE hide">
							<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">格式</label>
							<div class="col-md-12 col-xl-6 col-lg-8">
								<select class="form-control form-control-sm" id="dateFormat">
									<option value="yyyy">YYYY</option>
									<option value="yyyy-MM">YYYY-MM</option>
									<option value="yyyy-MM-dd" selected="selected">YYYY-MM-DD</option>
								</select>
							</div>
						</div>
						<div class="form-group row J_for-DATETIME hide">
							<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">格式</label>
							<div class="col-md-12 col-xl-6 col-lg-8">
								<select class="form-control form-control-sm" id="datetimeFormat">
									<option value="yyyy-MM-dd">YYYY-MM-DD</option>
									<option value="yyyy-MM-dd HH:mm">YYYY-MM-DD HH:II</option>
									<option value="yyyy-MM-dd HH:mm:ss" selected="selected">YYYY-MM-DD HH:II:SS</option>
								</select>
							</div>
						</div>
						<div class="form-group row J_for-IMAGE J_for-FILE hide">
							<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">允许上传数量</label>
							<div class="col-md-12 col-xl-6 col-lg-8 pt-1">
								<input class="bslider form-control" id="uploadNumber" type="text" data-slider-value="[0,9]" data-slider-step="1" data-slider-max="9" data-slider-min="0" data-slider-tooltip="show">
								<div class="form-text J_minmax">最少上传 <b>0</b> 个，最多上传 <b>9</b> 个</div>
							</div>
						</div>
						<div class="form-group row J_for-PICKLIST J_for-MULTISELECT hide">
							<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">选项列表</label>
							<div class="col-md-12 col-xl-6 col-lg-8">
								<div class="rb-scroller sortable-box autoh dt-${fieldType}">
									<ol class="dd-list" id="picklist-items">
										<li class="no-item">加载中</li>
									</ol>
								</div>
								<button type="button" class="btn btn-secondary btn-sm J_picklist-edit"><i class="zmdi zmdi-settings"></i> 配置选项</button>
							</div>
						</div>
						<div class="J_for-SERIES hide">
							<div class="form-group row">
								<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">编号规则</label>
								<div class="col-md-12 col-xl-6 col-lg-8">
									<input class="form-control form-control-sm" type="text" id="seriesFormat" value="{YYYYMMDD}-{0000}">
									<p class="form-text mb-0 help">内置变量使用 <code>{}</code> 包裹，如 <code title="年月日 20191231">{YYYYMMDD}</code> <code title="时分秒 235959">{HHIISS}</code> <code title="4位自增序号">{0000}</code></p>
								</div>
							</div>
							<div class="form-group row">
								<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">自增序号归零</label>
								<div class="col-md-12 col-xl-6 col-lg-8">
									<select class="form-control form-control-sm" id="seriesZero">
										<option value="N" selected="selected">不归零</option>
										<option value="D">每天归零</option>
										<option value="M">每月归零</option>
										<option value="Y">每年归零</option>
									</select>
								</div>
							</div>
						</div>
						<div class="form-group row J_for-CLASSIFICATION pt-0 pb-0 hide">
							<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">分类数据</label>
							<div class="col-md-12 col-xl-6 col-lg-8">
								<div class="form-control-plaintext" id="useClassification">
									<a title="查看/编辑分类数据" target="_blank" href="${baseUrl}/admin/entityhub/classifications">加载中</a>
									<i class="zmdi zmdi-layers fs-14 ml-1 text-muted"></i>
								</div>
							</div>
						</div>
						<div class="form-group row J_for-TEXT J_for-NTEXT J_for-EMAIL J_for-PHONE J_for-URL J_for-NUMBER J_for-DECIMAL J_for-DATE J_for-DATETIME hide">
							<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">默认值</label>
							<div class="col-md-12 col-xl-6 col-lg-8">
								<div class="input-group">
									<input class="form-control form-control-sm" type="text" id="defaultValue" value="${fieldDefaultValue}" data-o="${fieldDefaultValue}" placeholder="无" autocomplete="off">
        							<div class="input-group-append hide">
          								<button class="btn btn-primary mw-auto" title="设置高级默认值" type="button"><i class="icon zmdi zmdi-hdr-strong"></i></button>
									</div>
								</div>
							</div>
						</div>
						<div class="form-group row J_for-DECIMAL J_for-NUMBER hide">
                            <label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right pt-1">是否允许负数</label>
                            <div class="col-md-12 col-xl-6 col-lg-8">
                                <label class="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
                                    <input class="custom-control-input" type="checkbox" id="notNegative"><span class="custom-control-label"> 不允许</span>
                                </label>
                            </div>
                        </div>
                        <div class="form-group row J_for-STATE pt-0 pb-0 hide">
                            <label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">状态类</label>
                            <div class="col-md-12 col-xl-6 col-lg-8">
                                <div class="form-control-plaintext code" id="stateClass"></div>
                            </div>
                        </div>
						<div class="form-group row">
							<label class="col-md-12 col-xl-3 col-lg-4 col-form-label text-lg-right">备注</label>
							<div class="col-md-12 col-xl-6 col-lg-8">
								<textarea class="form-control form-control-sm row2x" id="comments" data-o="${fieldComments}">${fieldComments}</textarea>
							</div>
						</div>
						<div class="form-group row">
							<div class="col-md-12 col-xl-6 col-lg-8 offset-xl-3 offset-lg-4">
								<label class="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
									<input class="custom-control-input" type="checkbox" id="fieldNullable" data-o="${fieldNullable}"><span class="custom-control-label"> 允许为空</span>
								</label>
								<label class="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
									<input class="custom-control-input" type="checkbox" id="fieldUpdatable" data-o="${fieldUpdatable}"><span class="custom-control-label"> 允许修改</span>
								</label>
								<label class="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0 hide">
									<input class="custom-control-input" type="checkbox" id="fieldRepeatable" data-o="${fieldRepeatable}"><span class="custom-control-label"> 允许重复值</span>
								</label>
							</div>
						</div>
						<div class="form-group row footer">
							<div class="col-md-12 col-xl-6 col-lg-8 offset-xl-3 offset-lg-4">
								<div class="J_action hide">
									<button class="btn btn-primary btn-space J_save" type="button" data-loading-text="请稍后">保存</button>
									<button class="btn btn-danger bordered btn-space J_del" type="button" data-loading-text="请稍后"><i class="zmdi zmdi-delete icon"></i> 删除</button>
								</div>
								<div class="alert alert-warning alert-icon hide mb-0">
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
<script type="text/plain" id="picklist-tmpl">
<li class="dd-item dd3-item">
	<div class="dd3-content text-dots">HOLD</div>
	<div class="dd-handle dd3-handle"></div>
	<div class="dd3-action"><a href="javascript:;" class="J_default" title="设为默认">[默认]</a></div>
</li>
</script>
<%@ include file="/_include/Foot.jsp"%>
<script src="${baseUrl}/assets/lib/widget/bootstrap-slider.min.js"></script>
<script>
window.__PageConfig = {
	metaId: '${fieldMetaId}',
	fieldType: '${fieldType}',
	extConfig: $.parseJSON('${fieldExtConfig}' || '{}'),
	entityName: '${entityName}',
	fieldName: '${fieldName}',
	fieldBuildin: ${fieldBuildin},
	isSuperAdmin: ${isSuperAdmin}
}
</script>
<script type="text/babel" src="${baseUrl}/assets/js/entityhub/field-edit.jsx"></script>
</body>
</html>
