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

package com.rebuild.web.feeds;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 操作相关
 *
 * @author devezhao
 * @since 2019/11/1
 */
@Controller
@RequestMapping("/feeds/post/")
public class FeedsPostControll extends BaseControll {

    @RequestMapping({"publish", "comment"})
    public void publish(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        JSON formJson = ServletUtils.getRequestJson(request);
        Record record = EntityHelper.parse((JSONObject) formJson, user);
        String content = record.getString("content");
        record.setString("content", CommonsUtils.escapeHtml(content));

        Application.getService(record.getEntity().getEntityCode()).createOrUpdate(record);
        JSON ret = JSONUtils.toJSONObject("id", record.getPrimary());
        writeSuccess(response, ret);
    }

    @RequestMapping("like")
    public void like(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID user = getRequestUser(request);
        ID source = getIdParameterNotNull(request, "id");

        Object[] liked = Application.createQueryNoFilter(
                "select likeId from FeedsLike where source = ? and createdBy = ?")
                .setParameter(1, source)
                .setParameter(2, user)
                .unique();
        if (liked == null) {
            Record record = EntityHelper.forNew(EntityHelper.FeedsLike, user);
            record.setID("source", source);
            Application.getCommonService().create(record);
        } else {
            Application.getCommonService().delete((ID) liked[0]);
        }

        writeSuccess(response, liked == null);
    }

    @RequestMapping("delete")
    public void delete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ID anyId = getIdParameterNotNull(request, "id");
        Application.getService(anyId.getEntityCode()).delete(anyId);
        writeSuccess(response);
    }
}
