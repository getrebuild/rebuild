/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * 任务凭论
 *
 * @author devezhao
 * @since 2020/6/29
 */
@RequestMapping("/project/comments/")
@Controller
public class TaskCommentControll extends BaseControll {

    @GetMapping("list")
    public void commentList(HttpServletRequest request, HttpServletResponse response) {
        ID user = getRequestUser(request);
        ID taskId = getIdParameterNotNull(request, "task");

        Object[][] array = Application.createQueryNoFilter(
                "select commentId,content,attachments,createdOn,createdBy,createdBy from ProjectTaskComment where taskId = ?")
                .setParameter(1, taskId)
                .array();

        JSONArray ret = new JSONArray();
        for (Object[] o : array) {
            if (o[2] != null) o[2] = JSON.parse((String) o[2]);
            o[3] = ProjectTaskControll.formatUTCWithZone((Date) o[3]);
            o[4] = new Object[]{ o[4], UserHelper.getName((ID) o[4]) };
            o[5] = user.equals(o[5]);

            JSONObject item = JSONUtils.toJSONObject(
                    new String[] { "id", "content", "attachments", "createdOn", "createdBy", "self" },
                    o);
            ret.add(item);
        }
        writeSuccess(response, ret);
    }
}
