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

package com.rebuild.web;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.Privileges;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.utils.JSONUtils;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

/**
 * 页面上需要某个实体信息的  Controll
 * 
 * @author devezhao
 * @since 01/10/2019
 */
public abstract class BaseEntityControll extends BasePageControll {

	/**
	 * @param page
	 * @param entity
	 * @param user
	 * @return
	 */
	protected ModelAndView createModelAndView(String page, String entity, ID user) {
		ModelAndView mv = createModelAndView(page);
		Entity entityMeta = MetadataHelper.getEntity(entity);
		putEntityMeta(mv, entityMeta);
		
		if (EntityHelper.hasPrivilegesField(entityMeta)) {
			Privileges priv = Application.getSecurityManager().getPrivileges(user, entityMeta.getEntityCode());
			Permission[] actions = new Permission[] {
					BizzPermission.CREATE,
					BizzPermission.DELETE,
					BizzPermission.UPDATE,
					BizzPermission.READ,
					BizzPermission.ASSIGN,
					BizzPermission.SHARE,
			};
			Map<String, Boolean> actionMap = new HashMap<>();
			for (Permission act : actions) {
				actionMap.put(act.getName(), priv.allowed(act));
			}
			mv.getModel().put("entityPrivileges", JSON.toJSONString(actionMap));
		} else {
			mv.getModel().put("entityPrivileges", JSONUtils.EMPTY_OBJECT_STR);
		}
		return mv;
	}
	
	/**
	 * @param page
	 * @param record
	 * @param user
	 * @return
	 */
	protected ModelAndView createModelAndView(String page, ID record, ID user) {
		ModelAndView mv = createModelAndView(page);
		Entity entity = MetadataHelper.getEntity(record.getEntityCode());
		putEntityMeta(mv, entity);
		
		// 使用主实体权限
		if (entity.getMasterEntity() != null) {
			entity = entity.getMasterEntity();
		}
		if (EntityHelper.hasPrivilegesField(entity)) {
			Permission[] actions = new Permission[] {
					BizzPermission.CREATE,
					BizzPermission.DELETE,
					BizzPermission.UPDATE,
					BizzPermission.READ,
					BizzPermission.ASSIGN,
					BizzPermission.SHARE,
			};
			Map<String, Boolean> actionMap = new HashMap<>();
			for (Permission act : actions) {
				actionMap.put(act.getName(), Application.getSecurityManager().allowed(user, record, act));
			}
			mv.getModel().put("entityPrivileges", JSON.toJSONString(actionMap));
		} else {
			mv.getModel().put("entityPrivileges", JSONUtils.EMPTY_OBJECT_STR);
		}
		return mv;
	}
	
	/**
	 * @param into
	 * @param entity
	 */
	protected void putEntityMeta(ModelAndView into, Entity entity) {
		EasyMeta easyMeta = EasyMeta.valueOf(entity);
		into.getModel().put("entityName", easyMeta.getName());
		into.getModel().put("entityLabel", easyMeta.getLabel());
		into.getModel().put("entityIcon", easyMeta.getIcon());
		
		EasyMeta master = null;
		EasyMeta slave = null;
		if (entity.getMasterEntity() != null) {
			master = EasyMeta.valueOf(entity.getMasterEntity());
			slave = EasyMeta.valueOf(entity);
		} else if (entity.getSlaveEntity() != null) {
			master = EasyMeta.valueOf(entity);
			slave = EasyMeta.valueOf(entity.getSlaveEntity());
		} else {
			into.getModel().put("masterEntity", easyMeta.getName());
		}
		
		if (master != null && slave != null) {
			into.getModel().put("masterEntity", master.getName());
			into.getModel().put("masterEntityLabel", master.getLabel());
			into.getModel().put("masterEntityIcon", master.getIcon());
			into.getModel().put("slaveEntity", slave.getName());
			into.getModel().put("slaveEntityLabel", slave.getLabel());
			into.getModel().put("slaveEntityIcon", slave.getIcon());
		}
	}
}
