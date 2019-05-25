/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.web.admin.robot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.EasyMeta;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/05/25
 */
@Controller
@RequestMapping("/admin/robot/")
public class SendNotificationControll extends BaseControll {

	@RequestMapping("trigger/send-notification-sendtos")
	public void parseSendTo(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String sourceEntity = getParameterNotNull(request, "entity");
		JSON sendTo = ServletUtils.getRequestJson(request);
		Entity entity = MetadataHelper.getEntity(sourceEntity);
		
		List<JSON> formatted = new ArrayList<>();
		String[] keys = new String[] { "id","text"};
		for (Object item : (JSONArray) sendTo) {
			String idOrField = (String) item;
			if (ID.isId(idOrField)) {
				String name = UserHelper.getName(ID.valueOf(idOrField));
				formatted.add(JSONUtils.toJSONObject(keys, new String[] { idOrField, name }));
			} else if (entity.containsField(idOrField.split("//.")[0])) {
				String fullLabel = EasyMeta.getLabel(entity, idOrField);
				formatted.add(JSONUtils.toJSONObject(keys, new String[] { idOrField, fullLabel }));
			}
		}
		writeSuccess(response, formatted);
	}
}
