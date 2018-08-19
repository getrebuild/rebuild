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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import cn.devezhao.rebuild.server.metadata.EasyMeta;
import cn.devezhao.rebuild.server.metadata.EntityHelper;
import cn.devezhao.rebuild.web.commons.BaseControll;
import cn.devezhao.rebuild.web.commons.PageForward;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/19/2018
 */
@Controller
@RequestMapping("/admin/entity/")
public class EntityLayoutControll extends BaseControll {
	
	@RequestMapping("{entity}/layouts")
	public String pageEntityLayouts(@PathVariable String entity, HttpServletRequest request) throws IOException {
		EasyMeta entityMeta = new EasyMeta(EntityHelper.getEntity(entity));
		request.setAttribute("entityName", entityMeta.getName());
		request.setAttribute("entityLabel", entityMeta.getLabel());
		request.setAttribute("comments", entityMeta.getComments());
		
		PageForward.setPageAttribute(request);
		return "/admin/entity/layouts.jsp";
	}
}
