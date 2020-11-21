/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.project;

import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.IdParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * 任务评论
 *
 * @author devezhao
 * @since 2020/6/29
 */
@RestController
public class TaskCommentController extends BaseController {

    @GetMapping("/project/comments/list")
    public JSON commentList(@IdParam(name = "task") ID taskId, HttpServletRequest request) {
        final ID user = getRequestUser(request);

        Object[][] array = Application.createQueryNoFilter(
                "select commentId,content,attachments,createdOn,createdBy,createdBy" +
                        " from ProjectTaskComment where taskId = ? order by createdOn desc")
                .setParameter(1, taskId)
                .setMaxResults(200)
                .array();

        JSONArray ret = new JSONArray();
        for (Object[] o : array) {
            if (o[2] != null) o[2] = JSON.parse((String) o[2]);
            o[3] = I18nUtils.formatDate((Date) o[3]);
            o[4] = new Object[]{o[4], UserHelper.getName((ID) o[4])};
            o[5] = user.equals(o[5]);

            JSONObject item = JSONUtils.toJSONObject(
                    new String[]{"id", "content", "attachments", "createdOn", "createdBy", "self"},
                    o);
            ret.add(item);
        }
        return ret;
    }
}
