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
import org.springframework.web.bind.annotation.RequestMapping;

import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.rebuild.server.service.entitymanage.Entity2Schema;
import cn.devezhao.rebuild.web.commons.BaseControll;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
@Controller
@RequestMapping("/admin/entity/")
public class MetaEntityControll extends BaseControll {

	@RequestMapping("entity-new")
	public void entityNew(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		String label = getParameterNotNull(request, "label");
		String desc = getParameter(request, "desc");
		
		String entityName = new Entity2Schema(user).create(label, desc);
		if (entityName != null) {
			writeSuccess(response, entityName);
		} else {
			writeFailure(response);
		}
	}
}
