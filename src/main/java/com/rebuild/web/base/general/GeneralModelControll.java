/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.base.general;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.FormsBuilder;
import com.rebuild.server.configuration.portals.ViewAddonsManager;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.web.BaseEntityControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 表单/视图
 * 
 * @author zhaofang123@gmail.com
 * @since 08/22/2018
 */
@Controller
@RequestMapping("/app/{entity}/")
public class GeneralModelControll extends BaseEntityControll {

	@RequestMapping("view/{id}")
	public ModelAndView pageView(@PathVariable String entity, @PathVariable String id,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		final ID user = getRequestUser(request);
		Entity thatEntity = MetadataHelper.getEntity(entity);
		
 		if (!Application.getSecurityManager().allowRead(user, thatEntity.getEntityCode())) {
 			response.sendError(403, "你没有访问此实体的权限");
 			return null;
 		}
		
		ID record = ID.valueOf(id);
		ModelAndView mv;
		if (thatEntity.getMasterEntity() != null) {
			mv = createModelAndView("/general-entity/slave-view.jsp", record, user);
		} else {
			mv = createModelAndView("/general-entity/record-view.jsp", record, user);
			
			JSON vtab = ViewAddonsManager.instance.getViewTab(entity, user);
			mv.getModel().put("ViewTabs", vtab);
			JSON vadd = ViewAddonsManager.instance.getViewAdd(entity, user);
			mv.getModel().put("ViewAdds", vadd);
		}
		mv.getModel().put("id", record);
		
		return mv;
	}
	
	@RequestMapping("form-model")
	public void entityForm(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		ID record = getIdParameter(request, "id");  // New or Update
		
		JSON initialVal = null;
		if (record == null) {
			initialVal = ServletUtils.getRequestJson(request);
			if (initialVal != null) {
				// 创建明细实体必须指定主实体，以便验证权限
				String master = ((JSONObject) initialVal).getString(FormsBuilder.DV_MASTER);
				if (ID.isId(master)) {
					FormsBuilder.setCurrentMasterId(ID.valueOf(master));
				}
			}
		}

		try {
			JSON model = FormsBuilder.instance.buildForm(entity, user, record);
			// 填充前端设定的初始值
			if (record == null && initialVal != null) {
				FormsBuilder.instance.setFormInitialValue(MetadataHelper.getEntity(entity), model, (JSONObject) initialVal);
			}
			writeSuccess(response, model);
		} finally {
			FormsBuilder.setCurrentMasterId(null);
		}
	}
	
	@RequestMapping("view-model")
	public void entityView(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) {
		ID user = getRequestUser(request);
		ID record = getIdParameterNotNull(request, "id");
		JSON modal = FormsBuilder.instance.buildView(entity, user, record);
		writeSuccess(response, modal);
	}
}
