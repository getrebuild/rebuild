/*
Copyright 2018 DEVEZHAO(zhaofang123@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cn.devezhao.rebuild.web.entitymanage;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.server.metadata.EntityHelper;
import cn.devezhao.rebuild.server.service.entitymanage.LayoutManager;
import cn.devezhao.rebuild.web.commons.BaseControll;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/19/2018
 */
@Controller
@RequestMapping("/admin/entity/")
public class EntityLayoutControll extends BaseControll {
	
	@RequestMapping("{entity}/form-design")
	public String pageFormDesign(@PathVariable String entity, HttpServletRequest request) throws IOException {
		MetaEntityControll.setEntityBase(request, entity);
		
		JSON ll = LayoutManager.getFormLayout(entity);
		if (ll != null) {
			request.setAttribute("config", ll);
		}
		return "/admin/entity/form-design.jsp";
	}
	
	@RequestMapping("{entity}/form-update")
	public void formUpdate(@PathVariable String entity, HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSON formJson = ServletUtils.getRequestJson(request);
		Record record = EntityHelper.parse((JSONObject) formJson, user);
		Application.getCommonService().createOrUpdate(record);
		
		writeSuccess(response);
	}
}
