/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.entityhub;

import cn.devezhao.persist4j.Field;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.base.SeriesReindexTask;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseControll;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author devezhao
 * @since 2020/4/30
 */
@Controller
@RequestMapping("/admin/field/")
public class SeriesControll extends BaseControll {

    @RequestMapping("series-reindex")
    public void seriesReindex(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String entity = getParameterNotNull(request, "entity");
        String field = getParameterNotNull(request, "field");

        Field metaField = MetadataHelper.getField(entity, field);
        SeriesReindexTask seriesReindexTask = new SeriesReindexTask(metaField);
        seriesReindexTask.setUser(getRequestUser(request));

        int reindex = seriesReindexTask.exec();
        writeJSON(response, JSONUtils.toJSONObject("reindex", reindex));
    }
}
