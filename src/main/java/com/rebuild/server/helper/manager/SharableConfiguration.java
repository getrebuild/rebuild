/*
rebuild - Building your system freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.helper.manager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.RoleService;
import com.rebuild.server.service.bizz.UserHelper;

import cn.devezhao.persist4j.engine.ID;

/**
 * 可共享配置。配置实体需遵循一致的标准，即包含以下字段
 * - configId 主键
 * - config JSON 配置
 * - [belongEntity] 隶属实体
 * - [shareTo] 共享给哪些人
 * - [applyType] 配置类型，因为可能存在共享配置，如表单和视图
 * 
 * @author devezhao
 * @since 01/07/2019
 */
public class SharableConfiguration implements PortalsConfiguration {
	
	protected static final Log LOG = LogFactory.getLog(SharableConfiguration.class);
	
	// 共享给全部
	public static final String SHARE_ALL = "ALL";
	// 私有
	public static final String SHARE_SELF = "SELF";
	
	/**
	 * @param user
	 * @param configEntity
	 * @return
	 * @see #detectUseConfig(ID, String, String, String)
	 */
	protected static ID detectUseConfig(ID user, String configEntity) {
		return detectUseConfig(user, configEntity, null, null);
	}
	
	/**
	 * 确定使用哪个配置，规则如下
	 * 1.管理员（角色）使用同一配置
	 * 2.非管理员优先使用自己的配置，无自己的配置则使用管理员的
	 * 
	 * @param user
	 * @param configEntity
	 * @param belongEntity
	 * @param applyType
	 * @return
	 */
	protected static ID detectUseConfig(ID user, String configEntity, String belongEntity, String applyType) {
		Assert.isTrue(MetadataHelper.containsEntity(configEntity), "configEntity");
		
		String sqlBase = String.format("select configId from %s where (1=1)", configEntity);
		if (belongEntity != null) {
			sqlBase += String.format(" and belongEntity = '%s'", belongEntity);
		}
		if (applyType != null) {
			sqlBase += String.format(" and applyType = '%s'", applyType);
		}
		
		// 目前只有一个配置的实体
		if ("ViewAddonsConfig".equalsIgnoreCase(configEntity)
				|| ("LayoutConfig".equalsIgnoreCase(configEntity) && LayoutManager.TYPE_FORM.equals(applyType))) {
			Object[] o = Application.createQueryNoFilter(sqlBase).unique();
			return o == null ? null : (ID) o[0];
		}
		
		sqlBase += " and ";
		
		if (UserHelper.isAdmin(user)) {
			sqlBase += String.format("createdBy.roleId = '%s'", RoleService.ADMIN_ROLE.toLiteral());
			Object[] o = Application.createQueryNoFilter(sqlBase).unique();
			return o == null ? null : (ID) o[0];
		}
		
		String sql4self = sqlBase + String.format("createdBy = '%s'", user.toLiteral());
		Object[] o = Application.createQueryNoFilter(sql4self).unique();
		if (o == null && MetadataHelper.containsField(configEntity, "shareTo")) {
			sql4self = sqlBase + "shareTo = 'ALL'";
			o = Application.createQueryNoFilter(sql4self).unique();
		}
		return o == null ? null : (ID) o[0];
	}
	
	/**
	 * 是否是自己的配置
	 * 
	 * @param user
	 * @param configOrUser 配置ID 或 用戶ID
	 * @return
	 */
	public static boolean isSelf(ID user, ID configOrUser) {
		if (configOrUser.getEntityCode() == EntityHelper.User) {
			boolean self = user.equals(configOrUser);
			if (!self && UserHelper.isAdmin(user)) {
				self = UserHelper.isAdmin(configOrUser);
			}
			return self;
		}
		
		String sql = String.format(
				"select createdBy from %s where configId = ?", MetadataHelper.getEntityName(configOrUser));
		Object[] c = Application.createQueryNoFilter(sql).setParameter(1, configOrUser).unique();
		if (c == null) {
			return false;
		}
		return isSelf(user, (ID) c[0]);
	}
}
