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

package com.rebuild.web.admin.audit;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.web.BaseEntityControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * TODO 修改历史
 *
 * @author devezhao
 * @since 11/01/2018
 */
@Controller
@RequestMapping("/admin/audit/")
public class RevisionHistoryControll extends BaseEntityControll {

	@RequestMapping("revision-history")
	public ModelAndView pageLogging() throws IOException {
		return createModelAndView("/admin/audit/revision-history.jsp");
	}

    @RequestMapping("revision-history/details")
    public void details(HttpServletRequest request, HttpServletResponse response) throws IOException {
	    ID id = getIdParameterNotNull(request, "id");
	    Object[] rev = Application.createQueryNoFilter(
                "select revisionContent,belongEntity from RevisionHistory where revisionId = ?")
                .setParameter(1, id)
                .unique();

        JSONArray data = JSON.parseArray((String) rev[0]);

        // 字段名称
        if (MetadataHelper.containsEntity((String) rev[1])) {
            Entity entity = MetadataHelper.getEntity((String) rev[1]);
            for (Object o : data) {
                JSONObject item = (JSONObject) o;
                String field = item.getString("field");
                if (entity.containsField(field)) {
                    field = EasyMeta.getLabel(entity.getField(field));
                } else {
                    field = "[" + field.toUpperCase() + "]";
                }
                item.put("field", field);
            }
        }

        writeSuccess(response, data);
    }
}
