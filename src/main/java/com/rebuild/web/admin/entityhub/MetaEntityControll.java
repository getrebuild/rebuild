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

package com.rebuild.web.admin.entityhub;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.rbstore.MetaSchemaGenerator;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.task.TaskExecutors;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.MetadataSorter;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.metadata.entity.Entity2Schema;
import com.rebuild.server.service.base.QuickCodeReindexTask;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;
import com.rebuild.web.common.FileDownloader;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
@Controller
@RequestMapping("/admin/")
public class MetaEntityControll extends BasePageControll {

	@RequestMapping("entities")
	public ModelAndView pageList(HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/admin/entityhub/entity-grid.jsp");
		mv.getModel().put("isSuperAdmin", UserHelper.isSuperAdmin(getRequestUser(request)));
		return mv;
	}

	@RequestMapping("entity/{entity}/base")
	public ModelAndView pageEntityBase(@PathVariable String entity, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/admin/entityhub/entity-edit.jsp");
		setEntityBase(mv, entity);
		
		Entity entityMeta = MetadataHelper.getEntity(entity);
		mv.getModel().put("nameField", MetadataHelper.getNameField(entityMeta).getName());
		
		if (entityMeta.getMasterEntity() != null) {
			mv.getModel().put("masterEntity", entityMeta.getMasterEntity().getName());
			mv.getModel().put("slaveEntity", entityMeta.getName());
		} else if (entityMeta.getSlaveEntity() != null) {
			mv.getModel().put("masterEntity", entityMeta.getName());
			mv.getModel().put("slaveEntity", entityMeta.getSlaveEntity().getName());
		}
		
		return mv;
	}
	@RequestMapping("entity/{entity}/advanced")
	public ModelAndView pageEntityDanger(@PathVariable String entity, HttpServletRequest request) throws IOException {
		ModelAndView mv = createModelAndView("/admin/entityhub/entity-advanced.jsp");
		mv.getModel().put("isSuperAdmin", UserHelper.isSuperAdmin(getRequestUser(request)));
		setEntityBase(mv, entity);
		return mv;
	}

	@RequestMapping("entity/entity-list")
	public void listEntity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		List<Map<String, Object>> ret = new ArrayList<>();
		for (Entity entity : MetadataSorter.sortEntities()) {
			if (entity.getMasterEntity() != null) {
				continue;
			}
			
			EasyMeta easyMeta = new EasyMeta(entity);
			Map<String, Object> map = new HashMap<>();
			map.put("entityName", easyMeta.getName());
			map.put("entityLabel", easyMeta.getLabel());
			map.put("comments", easyMeta.getComments());
			map.put("icon", easyMeta.getIcon());
			map.put("builtin", easyMeta.isBuiltin());
			if (entity.getSlaveEntity() != null) {
				map.put("slaveEntity", entity.getSlaveEntity().getName());
			}
			ret.add(map);
		}
		writeSuccess(response, ret);
	}

	@RequestMapping("entity/entity-new")
	public void entityNew(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSONObject reqJson = (JSONObject) ServletUtils.getRequestJson(request);

		String label = reqJson.getString("label");
		String comments = reqJson.getString("comments");
		String masterEntity = reqJson.getString("masterEntity");
		if (StringUtils.isNotBlank(masterEntity)) {
			if (!MetadataHelper.containsEntity(masterEntity)) {
				writeFailure(response, "无效主实体 : " + masterEntity);
				return;
			}
			
			Entity master = MetadataHelper.getEntity(masterEntity);
			if (master.getMasterEntity() != null) {
				writeFailure(response, "明细实体不能作为主实体");
				return;
			} else if (master.getSlaveEntity() != null) {
				writeFailure(response, "选择的主实体已被 " + EasyMeta.getLabel(master.getSlaveEntity()) + " 使用");
				return;
			}
		}
		
		try {
			String entityName = new Entity2Schema(user)
					.createEntity(label, comments, masterEntity, getBoolParameter(request, "nameField"));
			writeSuccess(response, entityName);
		} catch (Exception ex) {
			LOG.error(null, ex);
			writeFailure(response, ex.getLocalizedMessage());
		}
	}

	@RequestMapping("entity/entity-update")
	public void entityUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSON formJson = ServletUtils.getRequestJson(request);
		Record record = EntityHelper.parse((JSONObject) formJson, user);
		
		// 修改了名称字段
		String needReindex = null;
		String nameField = record.getString("nameField");
		if (nameField != null) {
			Object[] nameFieldOld = Application.createQueryNoFilter(
					"select nameField,entityName from MetaEntity where entityId = ?")
					.setParameter(1, record.getPrimary())
					.unique();
			if (!nameField.equalsIgnoreCase((String) nameFieldOld[0])) {
				needReindex = (String) nameFieldOld[1];
			}
		}
		
		Application.getCommonService().update(record);
		Application.getMetadataFactory().refresh(false);
		
		if (needReindex != null) {
			Entity entity = MetadataHelper.getEntity(needReindex);
			QuickCodeReindexTask reindexTask = new QuickCodeReindexTask(entity);
			TaskExecutors.submit(reindexTask);
		}
		
		writeSuccess(response);
	}
	
	@RequestMapping("entity/entity-drop")
	public void entityDrop(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		Entity entity = getEntityById(getIdParameterNotNull(request, "id"));
		boolean force = getBoolParameter(request, "force", false);

		boolean drop = new Entity2Schema(user).dropEntity(entity, force);
		if (drop) {
			writeSuccess(response);
		} else {
			writeFailure(response, "删除失败，请确认该实体是否可被删除");
		}
	}

	@RequestMapping("entity/entity-export")
	public void entityExport(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		Entity entity = getEntityById(getIdParameterNotNull(request, "id"));

		File dest = SysConfiguration.getFileOfTemp("schema-" + entity.getName() + ".json");
		if (dest.exists()) {
			dest.delete();
		}
		new MetaSchemaGenerator(entity).generate(dest);

		if (ServletUtils.isAjaxRequest(request)) {
			writeSuccess(response, JSONUtils.toJSONObject("file", dest.getName()));
		} else {
			FileDownloader.setDownloadHeaders(response, dest.getName());
			FileDownloader.writeLocalFile(dest.getName(), true, response);
		}
	}

	/**
	 * @param metaId
	 * @return
	 */
	private Entity getEntityById(ID metaId) {
		Object[] entityRecord = Application.createQueryNoFilter(
				"select entityName from MetaEntity where entityId = ?")
				.setParameter(1, metaId)
				.unique();
		String entityName = (String) entityRecord[0];
		return MetadataHelper.getEntity(entityName);
	}

	/**
	 * @param mv
	 * @param entity
	 * @return
	 */
	static EasyMeta setEntityBase(ModelAndView mv, String entity) {
		EasyMeta entityMeta = EasyMeta.valueOf(entity);
		mv.getModel().put("entityMetaId", entityMeta.getMetaId());
		mv.getModel().put("entityName", entityMeta.getName());
		mv.getModel().put("entityLabel", entityMeta.getLabel());
		mv.getModel().put("icon", entityMeta.getIcon());
		mv.getModel().put("comments", entityMeta.getComments());
		return entityMeta;
	}
}
