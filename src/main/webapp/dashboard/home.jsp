<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@ include file="/_include/Head.jsp"%>
<title>Rebuild</title>
<style type="text/css">
.demo-layout .mdl-layout__header .mdl-layout__drawer-button {
  color: rgba(0, 0, 0, 0.54);
}
</style>
</head>
<body>
<div class="mdl-layout__container">
<div class="rb-layout mdl-layout mdl-js-layout mdl-layout--fixed-drawer mdl-layout--fixed-header">
	<header class="mdl-layout__header mdl-color--grey-100 mdl-color-text--grey-600">
		<div class="mdl-layout__header-row">
			<span class="mdl-layout-title">首页</span>
			<div class="mdl-layout-spacer"></div>
			<button class="mdl-button mdl-js-button mdl-js-ripple-effect mdl-button--icon" id="hdrbtn">
				<i class="material-icons">more_vert</i>
			</button>
			<ul class="mdl-menu mdl-js-menu mdl-js-ripple-effect mdl-menu--bottom-right" for="hdrbtn">
				<li class="mdl-menu__item mdl-menu__item--full-bleed-divider"><a href="<%=baseUrl%>/admin/">系统设置</a></li>
				<li class="mdl-menu__item"><a href="https://github.com/devezhao/re-build/" target="_blank">fork on Github</a></li>
			</ul>
		</div>
	</header>
	<div class="mdl-layout__drawer mdl-color--blue-grey-900 mdl-color-text--blue-grey-50">
		<header class="rb-drawer-header">
			REBUILD
		</header>
		<nav class="rb-navigation mdl-navigation mdl-color--blue-grey-800">
			<a class="mdl-navigation__link" href="<%=baseUrl%>/dashboard/home"><i class="mdl-color-text--blue-grey-400 material-icons">home</i>首页</a>
			<div class="mdl-layout-spacer"></div>
		</nav>
	</div>
	<main class="mdl-layout__content mdl-color--grey-100">
		<div class="mdl-grid">
		</div>
	</main>
</div>
</div>
<script type="text/babel">
</script>
</body>
</html>
