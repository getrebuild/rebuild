<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>新建图表</title>
<style type="text/css">
.chart-types>a{width:60px;height:42px;line-height:40px;display:inline-block;border:1px solid #eee;text-align:center;font-size:0;margin-right:6px}
.chart-types>a>img{height:28px;width:auto;font-size:0}
.chart-types>a:hover,.chart-types>a.active{border-color:#4285f4;}
.chart-types>a.active{outline:1px solid #4285f4;}
</style>
</head>
<body class="dialog">
<div class="main-content">
	<form>
		<div class="form-group row">
			<label class="col-sm-3 col-form-label text-sm-right">数据来源</label>
			<div class="col-sm-7">
				<select class="form-control form-control-sm" id="sourceEntity">
				</select>
			</div>
		</div>
		<div class="form-group row">
			<label class="col-sm-3 col-form-label text-sm-right">图表名称</label>
			<div class="col-sm-7">
				<input type="text" class="form-control form-control-sm" id="chartTitle" placeholder="可选">
			</div>
		</div>
		<div class="form-group row footer">
			<div class="col-sm-7 offset-sm-3">
				<button class="btn btn-primary" type="button">下一步</button>
				<a class="btn btn-link" onclick="parent.rb.modalHide()">取消</a>
			</div>
		</div>
	</form>
</div>
<%@ include file="/_include/Foot.jsp"%>
<script>
$(document).ready(function(){
	$.get(rb.baseUrl + '/commons/metadata/entities', function(res){
		$(res.data).each(function(){
			$('<option value="' + this.name + '">' + this.label + '</option>').appendTo('#sourceEntity')
		})
		$('#sourceEntity').select2({
            language: 'zh-CN',
            placeholder: '选择数据源',
            width: '100%'
        })
	})
	$('.btn-primary').click(function(){
		let source = $val('#sourceEntity'),
			title = $val('#chartTitle')
		parent.location.href = rb.baseUrl + '/dashboard/chart-design?source=' + source + '&title=' + title + '&das'
	})
})
</script>
</body>
</html>
