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

package com.rebuild.web.base.general;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.configuration.portals.DataListManager;
import com.rebuild.server.helper.datalist.DataList;
import com.rebuild.server.helper.datalist.DefaultDataList;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.privileges.ZeroEntry;
import com.rebuild.web.BaseEntityControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * 数据列表
 * 
 * @author zhaofang123@gmail.com
 * @since 08/22/2018
 */
@Controller
@RequestMapping("/app/")
public class GeneralDataListControll extends BaseEntityControll {

	@RequestMapping("{entity}/list")
	public ModelAndView pageList(@PathVariable String entity, 
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		final ID user = getRequestUser(request);
		if (!MetadataHelper.containsEntity(entity) || MetadataHelper.isBizzEntity(entity)) {
			response.sendError(404);
			return null;
		}
		
		Entity thatEntity = MetadataHelper.getEntity(entity);
		if (!Application.getSecurityManager().allowedR(user, thatEntity.getEntityCode())) {
			response.sendError(403, "你没有访问此实体的权限");
			return null;
		}
		
		ModelAndView mv;
		if (thatEntity.getMasterEntity() != null) {
			mv = createModelAndView("/general-entity/slave-list.jsp", entity, user);
		} else {
			mv = createModelAndView("/general-entity/record-list.jsp", entity, user);
		}
		
		JSON config = DataListManager.instance.getFieldsLayout(entity, getRequestUser(request));
		mv.getModel().put("DataListConfig", JSON.toJSONString(config));
		mv.getModel().put(ZeroEntry.AllowCustomDataList.name(),
				Application.getSecurityManager().allowed(user, ZeroEntry.AllowCustomDataList));

		String asideCollapsed = ServletUtils.readCookie(request, "rb.asideCollapsed");
		if (!"false".equals(asideCollapsed)) {
			mv.getModel().put("asideCollapsed", true);
		}

		return mv;
	}
	
	@RequestMapping("{entity}/data-list")
	public void dataList(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		JSONObject query = (JSONObject) ServletUtils.getRequestJson(request);

        DataList control = new DefaultDataList(query, getRequestUser(request));
		if ("TheSpecEntity".equalsIgnoreCase(entity)) {
		    // Use spec
        }

		JSON result = control.getJSONResult();
		writeSuccess(response, result);
	}

    @RequestMapping("list-and-view")
    public void quickPageList(HttpServletRequest request, HttpServletResponse response) throws IOException {
	    ID id = getIdParameterNotNull(request, "id");
	    String entity = MetadataHelper.getEntityName(id);
	    String url = MessageFormat.format("{0}/list#!/View/{0}/{1}", entity, id);
	    response.sendRedirect(url);
    }
}
