/*
rebuild - Building your business-systems freely.
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

package com.rebuild.server.configuration.portals;

import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.ConfigManager;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.UserHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
public abstract class SharableManager<T> implements ConfigManager<T> {
	
	protected static final Log LOG = LogFactory.getLog(SharableManager.class);
	
	// 共享给全部
	public static final String SHARE_ALL = "ALL";
	// 私有
	public static final String SHARE_SELF = "SELF";
	
	/**
	 * 确定使用哪个配置，规则如下：
	 * 1.管理员（角色）使用同一配置；
	 * 2.非管理员优先使用自己的配置，无自己的配置则使用管理员共享的；
     * 3.如果存在多个共享，优先返回最近修改的
	 * 
	 * @param user
	 * @param configEntity
	 * @param belongEntity
	 * @param applyType
	 * @return
	 */
	protected ID detectUseConfig(ID user, String configEntity, String belongEntity, String applyType) {
		Assert.isTrue(MetadataHelper.containsEntity(configEntity), "Unknow configEntity : " + configEntity);

        String cacheKey = String.format("%s-%s-%s", configEntity, belongEntity, applyType);
        Object[][] cached = (Object[][]) Application.getCommonCache().getx(cacheKey);
//		cached = null;
		if (cached == null) {
		    List<String> sqlWhere = new ArrayList<>();
            if (belongEntity != null) {
                sqlWhere.add(String.format("belongEntity = '%s'", belongEntity));
            }
            if (applyType != null) {
                sqlWhere.add(String.format("applyType = '%s'", applyType));
            }

            String sql = String.format("select configId,shareTo,createdBy from %s where (1=1) order by modifiedOn desc", configEntity);
            if (!sqlWhere.isEmpty()) {
                sql = sql.replace("(1=1)", StringUtils.join(sqlWhere.iterator(), " and "));
            }

            cached = Application.createQueryNoFilter(sql).array();
            Application.getCommonCache().putx(cacheKey, cached);
        }

		if (cached == null || cached.length == 0) {
		    return null;
        }

		if (isSingleConfig()) {
		    return (ID) cached[0][0];
        }

		// 优先自己
        boolean isAdmin = UserHelper.isAdmin(user);
		for (Object[] d : cached) {
		    if (user.equals(d[2]) || (isAdmin && UserHelper.isAdmin((ID) d[2]))) {
		        return (ID) d[0];
            }
        }

		// 共享的
        for (Object[] d : cached) {
            String shareTo = (String) d[1];
            if (SHARE_ALL.equals(shareTo)) {
                return (ID) d[0];
            } else if (shareTo.length() >= 20) {
                Set<String> userDefs = new HashSet<>();
                CollectionUtils.addAll(userDefs, shareTo.split(","));
                Set<ID> sharedUsers = UserHelper.parseUsers(userDefs, null);
                if (sharedUsers.contains(user)) {
                    return (ID) d[0];
                }
            }
        }

        return null;

//		String sqlBase = String.format("select configId,createdBy from %s where (1=1)", configEntity);
//		if (belongEntity != null) {
//			sqlBase += String.format(" and belongEntity = '%s'", belongEntity);
//		}
//		if (applyType != null) {
//			sqlBase += String.format(" and applyType = '%s'", applyType);
//		}
//
//		if (isSingleConfig()) {
//			Object[] o = Application.createQueryNoFilter(sqlBase).unique();
//			return o == null ? null : (ID) o[0];
//		}
//
//		sqlBase += " and ";
//
//		if (UserHelper.isAdmin(user)) {
//			sqlBase += String.format("createdBy.roleId = '%s'", RoleService.ADMIN_ROLE.toLiteral());
//			Object[] o = Application.createQueryNoFilter(sqlBase).unique();
//			return o == null ? null : (ID) o[0];
//		}
//
//		// 使用自己的
//		String sql4self = sqlBase + String.format("createdBy = '%s'", user.toLiteral());
//		Object[] o = Application.createQueryNoFilter(sql4self).unique();
//		// 使用管理员共享的
//		if (o == null && MetadataHelper.containsField(configEntity, "shareTo")) {
//			sql4self = sqlBase + ("shareTo = '" + SHARE_ALL + "'");
//			o = Application.createQueryNoFilter(sql4self).unique();
//		}
//		return o == null ? null : (ID) o[0];
	}
	
	/**
	 * 只会有一个配置（由管理员配置的）
	 * 
	 * @return
	 */
	protected boolean isSingleConfig() {
		return false;
	}
	
	/**
	 * 是否是自己的配置（不是自己的不能改）
	 * 
	 * @param user
	 * @param configOrUser 配置ID 或 用戶ID
	 * @return
	 */
	public boolean isSelf(ID user, ID configOrUser) {
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
