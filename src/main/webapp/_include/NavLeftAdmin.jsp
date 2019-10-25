<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<div class="rb-left-sidebar">
<div class="left-sidebar-wrapper">
	<a class="left-sidebar-toggle">MIN</a>
	<div class="left-sidebar-spacer">
		<div class="left-sidebar-scroll rb-scroller">
			<div class="left-sidebar-content">
				<ul class="sidebar-elements">
					<li class="divider">系统</li>
					<li class="${param['activeNav'] == 'systems' ? 'active' : ''}"><a href="${baseUrl}/admin/systems"><i class="icon zmdi zmdi-settings"></i><span>通用配置</span></a></li>
					<li class="parent">
						<a><i class="icon zmdi zmdi-cloud-outline-alt"></i><span>三方服务集成</span></a>
						<ul class="sub-menu">
							<li class="title">三方服务集成</li>
							<li class="nav-items">
								<div class="rb-scroller">
									<div class="content">
										<ul>
											<li class="${param['activeNav'] == 'integration-storage' ? 'active' : ''}"><a href="${baseUrl}/admin/integration/storage">云存储</a></li>
											<li class="${param['activeNav'] == 'integration-submail' ? 'active' : ''}"><a href="${baseUrl}/admin/integration/submail">短信/邮件</a></li>
											<li class="${param['activeNav'] == 'integration-cache' ? 'active' : ''}"><a href="${baseUrl}/admin/integration/cache">缓存系统</a></li>
										</ul>
									</div>
								</div>
							</li>
						</ul>
					</li>
					<li class="${param['activeNav'] == 'apis-manager' ? 'active' : ''} bosskey-show"><a href="${baseUrl}/admin/apis-manager"><i class="icon zmdi zmdi-key"></i><span>API 秘钥管理</span></a></li>
					<li class="divider">业务/实体</li>
					<li class="${param['activeNav'] == 'entities' ? 'active' : ''}"><a href="${baseUrl}/admin/entities"><i class="icon zmdi zmdi-widgets"></i><span>实体管理</span></a></li>
					<li class="${param['activeNav'] == 'classifications' ? 'active' : ''}"><a href="${baseUrl}/admin/entityhub/classifications"><i class="icon x21 zmdi zmdi-layers"></i><span>分类数据</span></a></li>
					<li class="${param['activeNav'] == 'robot-approval' ? 'active' : ''}"><a href="${baseUrl}/admin/robot/approvals"><i class="icon zmdi zmdi-assignment-check"></i><span>审批流程</span></a></li>
					<li class="${param['activeNav'] == 'robot-trigger' ? 'active' : ''}"><a href="${baseUrl}/admin/robot/triggers"><i class="icon zmdi zmdi-rotate-cw"></i><span>触发器</span></a></li>
					<li class="${param['activeNav'] == 'data-reports' ? 'active' : ''}"><a href="${baseUrl}/admin/datas/data-reports"><i class="icon zmdi zmdi-map"></i><span>报表模板</span></a></li>
					<li class="${param['activeNav'] == 'data-importer' ? 'active' : ''}"><a href="${baseUrl}/admin/datas/data-importer"><i class="icon zmdi zmdi-cloud-upload"></i><span>数据导入</span></a></li>
					<li class="divider">数据审计</li>
					<li class="${param['activeNav'] == 'revision-history' ? 'active' : ''}"><a href="${baseUrl}/admin/audit/revision-history"><i class="icon zmdi zmdi-wrap-text"></i><span>变更历史</span></a></li>
					<li class="${param['activeNav'] == 'recycle-bin' ? 'active' : ''}"><a href="${baseUrl}/admin/audit/recycle-bin"><i class="icon zmdi zmdi-delete fs-16"></i><span>回收站</span></a></li>
					<li class="divider">用户</li>
					<li class="${param['activeNav'] == 'users' ? 'active' : ''}"><a href="${baseUrl}/admin/bizuser/users"><i class="icon zmdi zmdi-accounts"></i><span>部门用户</span></a></li>
					<li class="${param['activeNav'] == 'role-privileges' ? 'active' : ''}"><a href="${baseUrl}/admin/bizuser/role-privileges"><i class="icon zmdi zmdi-lock"></i><span>角色权限</span></a></li>
					<li class="${param['activeNav'] == 'login-logs' ? 'active' : ''}"><a href="${baseUrl}/admin/bizuser/login-logs"><i class="icon zmdi zmdi-assignment-account"></i><span>登陆日志</span></a></li>
				</ul>
			</div>
		</div>
	</div>
</div>
</div>