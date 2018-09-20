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

package cn.devezhao.rebuild.web.base;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.fastjson.JSON;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import cn.devezhao.rebuild.server.Application;
import cn.devezhao.rebuild.server.metadata.EntityHelper;
import cn.devezhao.rebuild.server.service.base.LayoutManager;
import cn.devezhao.rebuild.server.service.base.NavManager;
import cn.devezhao.rebuild.web.BaseControll;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 09/19/2018
 */
@Controller
@RequestMapping("/app/")
public class NavSettings extends BaseControll {

	@RequestMapping(value = "common/nav-settings", method = RequestMethod.POST)
	public void navsSet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		
		JSON config = ServletUtils.getRequestJson(request);
		ID configId = getIdParameter(request, "cfgid");
		
		Record record = null;
		if (configId == null) {
			record = EntityHelper.forNew(EntityHelper.LayoutConfig, user);
			record.setString("belongEntity", "");
			record.setString("type", LayoutManager.TYPE_NAVI);
		} else {
			record = EntityHelper.forUpdate(configId, user);
		}
		record.setString("config", config.toJSONString());
		Application.getCommonService().createOrUpdate(record);
		
		writeSuccess(response);
	}
	
	@RequestMapping(value = "common/nav-settings", method = RequestMethod.GET)
	public void navsGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		JSON config = NavManager.getNav(user);
		writeSuccess(response, config);
	}
}
